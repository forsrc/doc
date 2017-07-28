
* Exception in thread "main" org.springframework.beans.factory.xml.XmlBeanDefinitionStoreException: Line 10 in XML document from class path resource [spring/spring-dataCleansing.xml] is
invalid; nested exception is org.xml.sax.SAXParseException: cvc-elt.1: Cannot find the declaration of element 'beans'.
```XML  
      <plugin>
            <groupid>org.apache.maven.plugins</groupid>
            <artifactid>maven-shade-plugin</artifactid>
            <version>1.4</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <transformers>
                            <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                <mainclass>com.some.package.Main</mainclass>
                            </transformer>
                            <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                <resource>META-INF/spring.handlers</resource>
                            </transformer>
                            <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                <resource>META-INF/spring.schemas</resource>
                            </transformer>
                        </transformers>
            <filters>
                <filter>
                <artifact>*:*</artifact>
                    <excludes>
                        <exclude>META-INF/*.SF</exclude>
                        <exclude>META-INF/*.DSA</exclude>
                        <exclude>META-INF/*.RSA</exclude>
                    </excludes>
                </filter>
            </filters>
                    </configuration>
                </execution>
            </executions>
        </plugin>
```
