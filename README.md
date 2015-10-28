Translator and Connector for reading and writing to a Coherence Cache.

The resource-adapter is designed to be used with a local (in VM) cache or a remote cache.

## What is this?

 This will discuss the following:
 	-	How to build the translator and resource-adapter
 	-  	How to deploy and configure to a jboss-as server
 	-  	How to test
 
## Prerequisites

1)	Before running the maven build, install the following 2 jar's into the local maven repo:

	-	install coherence jar, can use the following as an example: 

    mvn install:install-file -Dfile=coherence.jar -DgroupId=com.oracle.coherence -DartifactId=coherence -Dversion=12.1.3 -Dpackaging=jar

> NOTE:  the groupId and artifactId need to match what's defined in the pom.xml files.  The version needs to match
the property:  <version.coherence> in the root pom.xml.

	-	install the pojo jar, can use the following as an example:

    mvn install:install-file -Dfile={pojo-name}.jar -DgroupId=com.coherence.pojo -DartifactId=pojo -Dversion=0.0.1 -Dpackaging=jar

> NOTE: the groupId, artifactId and version match what is defined in pom.xml, and will enable it to added to the dist.zip for deployment. 
If you change them here, corresponding changes will need to be made the pom.xml files and possibly the property:  <version.pojo>.


2)  Before deploying to a JBoss AS/EAP server

	-	must have a JBoss AS/EAP server instance installed with Teiid
	

## BUILD:
---------
	run:  mvn clean install


Artifacts to look for:

-  teiid-coherence/translator-coherence/target:  translator-coherence-${version}-jboss-as7-dist.zip
-  teiid-coherence/connector-coherence/target:   connector-coherence-${version}-jboss-as7-dist.zip


## Deployment \ Configuration

1)  Server should be shutdown

2)  Deploy the translator and connector jboss-as7-dist.zip files

	-  unzip translator-coherence-${version}-jboss-as7-dist.zip into $JBOSS_HOME/modules directory
	-  unzip connector-coherence-${version}-jboss-as7-dist.zip into $JBOSS_HOME/modules directory
	
>Note:  the module:  org.jboss.teiid.translator.coherence.cache
contains the configuration file for connecting to coherence:  tangosol-coherence.xml
This one has been changed from the default so that logging is not written to "stderr".


3)  Start the server (if not already started)

	To start the server, open a command line and navigate to the "bin" directory under the root directory of the JBoss server and run:
	
	For Linux:   ./standalone.sh	
	for Windows: standalone.bat

	append the following to the command to indicate which configuration to use if Teiid isn't configured in the default configuration
		
	-c standalone-teiid.xml 

4) Configure the coherence translator

	Perform the following steps to configure the translator

	-	cd to the ${JBOSS_HOME}/bin directory
	
	-	execute:  ./jboss-cli.sh --connect --file=../docs/teiid/datasources/coherence/add-coherence-cache-translator.cli


5) Configure the coherence resource-adapter

	*  edit the CLI script, setting your values to configure cache:  docs/teiid/datasources/coherence/create-coherence-ds.cli 
	
	*	cd to the ${JBOSS_HOME}/bin directory
	
	-   execute:  ./jboss-cli.sh --connect --file=../docs/teiid/datasources/coherence/create-coherence-ds.cli


4) Teiid Deployment

Copy the following files to the "<jboss.home>/standalone/deployments" directory

     (1) vdb/coherence-vdb.xml
     (2) vdb/coherence-vdb.xml.dodeploy

The vdb has the JNDI name set to the jndi name specified in the create-coherence-ds.cli script.  If you change the script, you will need to update the vdb.


## Query Demonstrations

Use a SQL tool to view the metadata and issue sql queries.   If you don't have a tool, try SQuirreL (http://squirrel-sql.sourceforge.net/).

Use the JDBC url:   jdbc:teiid:Cache@mm://hostname:31000

>Note:   do not issue a sql query that returns the column with "Object" in the name, unless you have your pojo jar in the client application classpath.







