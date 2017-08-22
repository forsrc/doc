

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.CreateStreamRequest;
import com.amazonaws.services.kinesis.model.DescribeStreamRequest;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.ListStreamsRequest;
import com.amazonaws.services.kinesis.model.ListStreamsResult;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.services.kinesis.model.StreamDescription;


public class AmazonKinesisUtils {

    public static interface PutRecordRequestHandler {
        public void handle(AmazonKinesisClient kinesis, String streamName);
    }

    public static AmazonKinesisClient getAmazonKinesisClient(ClientConfiguration configuration) {
        /*
         * The ProfileCredentialsProvider will return your [default] credential profile by reading from the
         * credentials file located at (~/.aws/credentials).
         */
        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
                    + "Please make sure that your credentials file is at the correct "
                    + "location (~/.aws/credentials), and is in valid format.", e);
        }
        AmazonKinesisClient kinesis = new AmazonKinesisClient(credentials, configuration);
        return kinesis;
    }

    public static AmazonKinesisClient getAmazonKinesisClient() {

        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setMaxErrorRetry(3);
        configuration.setConnectionTimeout(501000);
        configuration.setSocketTimeout(501000);
        configuration.setProtocol(Protocol.HTTPS);
        configuration.setProxyHost("192.168.10.3");
        configuration.setProxyPort(3128);
        return getAmazonKinesisClient(configuration);
    }

    public static void put(AmazonKinesisClient kinesis, String streamName, PutRecordRequestHandler handler)
            throws InterruptedException {
        System.out.println("[START]");
        DescribeStreamRequest describeStreamRequest = new DescribeStreamRequest().withStreamName(streamName);
        StreamDescription streamDescription = null;
        try {
            streamDescription = kinesis.describeStream(describeStreamRequest).getStreamDescription();
        } catch (ResourceNotFoundException e) {
            createStream(kinesis, streamName, 100);
            streamDescription = kinesis.describeStream(describeStreamRequest).getStreamDescription();
        }
        System.out.printf("Stream %s has a status of %s.\n", streamName, streamDescription.getStreamStatus());
        if ("DELETING".equals(streamDescription.getStreamStatus())) {
            throw new RuntimeException("Stream is being deleted: " + streamName);
        }

        ListStreamsRequest listStreamsRequest = new ListStreamsRequest();
        listStreamsRequest.setLimit(10);
        ListStreamsResult listStreamsResult = kinesis.listStreams(listStreamsRequest);
        List<String> streamNames = listStreamsResult.getStreamNames();
        while (listStreamsResult.isHasMoreStreams()) {
            if (streamNames.size() > 0) {
                listStreamsRequest.setExclusiveStartStreamName(streamNames.get(streamNames.size() - 1));
            }
            listStreamsResult = kinesis.listStreams(listStreamsRequest);
            streamNames.addAll(listStreamsResult.getStreamNames());
        }

        System.out.println("List of streams: ");
        for (int i = 0; i < streamNames.size(); i++) {
            System.out.println("\t--> " + streamNames.get(i));
        }

        handler.handle(kinesis, streamName);

        System.out.println("[END]");
    }

    public static void createStream(AmazonKinesisClient kinesis, String streamName, int streamSize)
            throws InterruptedException {
        System.out.printf("Create Stream: ", streamName);
        CreateStreamRequest createStreamRequest = new CreateStreamRequest();
        createStreamRequest.setStreamName(streamName);
        createStreamRequest.setShardCount(streamSize);
        kinesis.createStream(createStreamRequest);
        // The stream is now being created. Wait for it to become active.
        waitForStreamToBecomeAvailable(kinesis, streamName);
    }

    private static void waitForStreamToBecomeAvailable(AmazonKinesisClient kinesis, String streamName)
            throws InterruptedException {
        System.out.printf("Waiting for %s to become ACTIVE...\n", streamName);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + TimeUnit.MINUTES.toMillis(10);
        while (System.currentTimeMillis() < endTime) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(20));

            try {
                DescribeStreamRequest describeStreamRequest = new DescribeStreamRequest();
                describeStreamRequest.setStreamName(streamName);
                // ask for no more than 10 shards at a time -- this is an optional parameter
                describeStreamRequest.setLimit(10);
                DescribeStreamResult describeStreamResponse = kinesis.describeStream(describeStreamRequest);

                String streamStatus = describeStreamResponse.getStreamDescription().getStreamStatus();
                System.out.printf("\t- current state: %s\n", streamStatus);
                if ("ACTIVE".equals(streamStatus)) {
                    return;
                }
            } catch (ResourceNotFoundException ex) {
                // ResourceNotFound means the stream doesn't exist yet,
                // so ignore this error and just keep polling.
            } catch (AmazonServiceException ase) {
                throw ase;
            }
        }

        throw new RuntimeException(String.format("Stream %s never became active", streamName));
    }

    public static void main(String[] args) throws Exception {
        AmazonKinesisClient kinesis = AmazonKinesisUtils.getAmazonKinesisClient();
        AmazonKinesisUtils.put(kinesis, "DENO", new PutRecordRequestHandler() {

            @Override
            public void handle(AmazonKinesisClient kinesis, String streamName) {
                long time = System.currentTimeMillis();
                PutRecordRequest putRecordRequest = new PutRecordRequest();
                putRecordRequest.setStreamName(streamName);
                String word = String.format("%d", time);
                putRecordRequest.setData(ByteBuffer.wrap(word.getBytes()));
                putRecordRequest.setPartitionKey(String.format("partitionKey-%d", time));
                PutRecordResult putRecordResult = kinesis.putRecord(putRecordRequest);
                System.out.printf(
                        "Successfully put record : %s , partition key : %s, ShardID : %s, SequenceNumber : %s.\n", word,
                        putRecordRequest.getPartitionKey(), putRecordResult.getShardId(),
                        putRecordResult.getSequenceNumber());
            }
        });
        kinesis.shutdown();
    }
}
