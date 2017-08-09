
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
/**
 * TimeOut class - used for stopping a thread that is taking too long
 * @author Peter Goransson
 *
 */
public class TimeOut {

    Thread interrupter;
    Thread target;
    long timeout;
    boolean success;
    boolean forceStop;

    CyclicBarrier cyclicBarrier;

    /**
     * 
     * @param target The Runnable target to be executed
     * @param timeout The time in milliseconds before target will be interrupted or stopped
     * @param forceStop If true, will Thread.stop() this target instead of just interrupt() 
     */
    public TimeOut(Runnable target, long timeout, boolean forceStop) {      
        this.timeout = timeout;
        this.forceStop = forceStop;

        this.target = new Thread(target);       
        this.interrupter = new Thread(new Interrupter());

        cyclicBarrier = new CyclicBarrier(2); // There will always be just 2 threads waiting on this barrier
    }

    public boolean execute() throws InterruptedException {  

        // Start target and interrupter
        target.start();
        interrupter.start();

        // Wait for target to finish or be interrupted by interrupter
        target.join();  

        interrupter.interrupt(); // stop the interrupter    
        try {
            cyclicBarrier.await(); // Need to wait on this barrier to make sure status is set
        } catch (BrokenBarrierException e) {
            // Something horrible happened, assume we failed
            //e.printStackTrace();
            success = false;
        } 

        return success; // status is set in the Interrupter inner class
    }

    private class Interrupter implements Runnable {

        public void run() {
            try {
                TimeUnit.MILLISECONDS.sleep(timeout);; // Wait for timeout period and then kill this target
                
//                if (forceStop) {
//                  target.stop(); // Need to use stop instead of interrupt since we're trying to kill this thread
//                }
//                else {
//                    target.interrupt(); // Gracefully interrupt the waiting thread
//                }
                Thread t = target;
                target = null;
                t.interrupt();
                success = false;
            } catch (InterruptedException e) {
                success = true;
            }


            try {
                cyclicBarrier.await(); // Need to wait on this barrier
            } catch (InterruptedException e) {
                // If the Child and Interrupter finish at the exact same millisecond we'll get here
                // In this weird case assume it failed
                success = false;                
            } 
            catch (BrokenBarrierException e) {
                // Something horrible happened, assume we failed
                success = false;
            }

        }

    }
    public static void main(String[] args) {
        long timeout = 2000; // number of milliseconds before timeout
        TimeOut t = new TimeOut(new Runnable() {
            
            @Override
            public void run() {
                 try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        }, timeout, true);
        try {                       
          boolean sucess = t.execute(); // Will return false if this times out
          System.out.println(sucess);
          if (!sucess) {
            // This thread timed out

          }
          else {
            // This thread ran completely and did not timeout
          }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }  

    }
}
