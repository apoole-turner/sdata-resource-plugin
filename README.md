# SDATA Resource Plugin

This is maven plugin is for validation of your workflow and environment variables. It can also create a list of all of your environment variables that are inside of your resources.

## Getting Started
---
Add to your pom.xml inside the build section
```
<build>
    <plugins>
        <plugin>
        	<groupId>com.turner.sdata</groupId>
        	<artifactId>resource-plugin</artifactId>
        	<version>1.0.1</version>
        	<executions>
        		<execution>
        			<id>validateWorkflow</id>
        			<phase>compile</phase>
        			<goals>
        				<goal>validateWorkflow</goal>
        			</goals>
        			<configuration>
        				<failOnError>false</failOnError>
        				<workflowFile>workflow.xml</workflowFile>
        			</configuration>
        		</execution>
        		<execution>
        			<id>envValidate</id>
        			<goals>
        			    <goal>envValidate</goal>
        			</goals>
        			<configuration>
        				<failOnError>false</failOnError>
        			</configuration>
        		</execution>
        		<execution>
        			<id>generate</id>
        			<phase>compile</phase>
        			<goals>
        				<goal>generateEnvironmentStub</goal>
        			</goals>
        			<configuration>
        				<stubFileType>properties</stubFileType>
        				<showMetaData>false</showMetaData>
        			</configuration>
        		</execution>
        	</executions>
        </plugin>
    </plugins>
</build>
```
This contains all the the plugins goals.  
### Prerequisites
---
* Maven 3.x.x
* A Loki Project

### Goals
---
#### validateWorkflow
Parameters:

| Name        | type  | Default Value  |
| ------------- |:-------------:| -----:|
| workflowFile      | classPath file name | workflow.xml |
| failOnError      | boolean      |   true |


By default this will fail if it finds errors in your workflow.xml

#### envValidate
Parameters:

| Name        | type  | Default Value  |
| ------------- |:-------------:| -----:|
| failOnError| boolean|true |

Checks to ensure that all the environment variables the application is set before the main program starts up. It does this by recursively going through your resource folder for all erb files and collecting all the env variables within. The missing variables are printed out to console.

Ex:
```
[WARNING] Missing Property: LOG4J_APPENDER
[WARNING] Missing Property: ENVIRONMENT
[WARNING] Missing Property: DB_CONNECTION_DATABASE
[WARNING] Missing Property: DB_CONNECTION_URL
[WARNING] Missing Property: QUARTZ_DATABASE
```

#### generateEnvironmentStub
Parameters:

| Name | type | Default Value  
--- |--- | ---
 stubFileType      | bash  or properties | properties 
 stubFileName      | String      |   stub 
 showMetaData      | boolean      |   false 

This will generate the stub file inside the root of your project.

**stubFileType**
You have the options of making a key value pair properties file or a bash export file

**StubFileName**
You can choose the name of your file. But do not add an extension. If you name it "myExample.txt" it will generate a file called "myExample.txt.properties"

**showetaData**
When this is enabled it will inside of you stub file tell you in which file and which line continues an environment variable. Its useful if you are curious about where all these values are coming from.


### Running Inline
---
If don't want to add this to your pom file but would occasionally like to run this plugin you can run these commands in terminal in the root of your project.
```
mvn com.turner.sdata:resource-plugin:1.0.0:envValidate
mvn com.turner.sdata:resource-plugin:1.0.0:generateEnvironmentStub
mvn com.turner.sdata:resource-plugin:1.0.0:validateWorkflow
```
For more on maven plugins please go [HERE](https://maven.apache.org/guides/mini/guide-configuring-plugins.html)


## Authors
---
* **Austin Poole** - *Initial work* 

See also the list of [contributors](https://github.com/apoole-turner/sdata-resource-plugin/graphs/contributors) who participated in this project.
