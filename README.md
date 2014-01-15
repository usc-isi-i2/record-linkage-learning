record-linkage-learning
=======================

NOTE from Pedro: copied the FRIL service code to its own repository. Once we verify that we can run it we should delete this branch (2013-12-27)

Record Linkage Project, learning FRIL configurations


How to compile:
Prerequisite: install maven and JDK 
1. make install: install the third-part libraries.
2. make compile: compile the project and generate the target file.


How to deploy:
1. Put the misc.properties in the Tomcat/Jetty's root directory.
2. Deploy the war file in the target directory.
