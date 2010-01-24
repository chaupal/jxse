In order to generate the artifacts for Kenai:

a) Make sure the source have been imported from ant.

b) Perform the following:

 mvn clean

 mvn site:site -Dmaven.test.skip=false
 
 mvn -Dmaven.test.skip=true package
