install:
	mvn install:install-file -Dfile=./jars/commons-logging-1.1.jar -DgroupId=commons-logging -DartifactId=logging -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/emory-util-all.jar -DgroupId=emory-util -DartifactId=emory-util-all -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/janino.jar -DgroupId=janino -DartifactId=janino -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/join.jar -DgroupId=join -DartifactId=join -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/junit-4.5.jar -DgroupId=junit-4.5 -DartifactId=junit-4.5 -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/jwf-1.0.1.jar -DgroupId=jwf -DartifactId=jwf -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/libsvm.jar -DgroupId=libsvm -DartifactId=libsvm -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/log4j-1.2.13.jar -DgroupId=log4j-1.2.13 -DartifactId=log4j-1.2.13 -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/opencsv-2.0.jar -DgroupId=opencsv -DartifactId=opencsv -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/ostermillerutils_1_05_00_for_java_1_4.jar -DgroupId=ostermillerutils -DartifactId=ostermillerutils -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/ostermillerutils_1_06_01.jar -DgroupId=ostermillerutils_1_06_01 -DartifactId=ostermillerutils_1_06_01 -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/poi-3.1-FINAL-20080629.jar -DgroupId=poi-3.1-FINAL -DartifactId=poi-3.1-FINAL -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/poi-contrib-3.1-FINAL-20080629.jar -DgroupId=poi-contrib -DartifactId=poi-contrib -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/poi-scratchpad-3.1-FINAL-20080629.jar -DgroupId=poi-scratchpad -DartifactId=poi-scratchpad -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/rsyntaxtextarea.jar -DgroupId=rsyntaxtextarea -DartifactId=rsyntaxtextarea -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/secondstring-20060615.jar -DgroupId=secondstring -DartifactId=secondstring -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/serializer.jar -DgroupId=serializer -DartifactId=serializer -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/swing-layout-1.0.jar -DgroupId=swing-layout -DartifactId=swing-layout -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/swingx_v0203.jar -DgroupId=swingx_v0203 -DartifactId=swingx_v0203 -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/xercesImpl.jar -DgroupId=xercesImpl -DartifactId=xercesImpl -Dversion=1.1 -Dpackaging=jar
	mvn install:install-file -Dfile=./jars/xml-apis.jar -DgroupId=xml-apis -DartifactId=xml-apis -Dversion=1.1 -Dpackaging=jar
	
compile:
	mvn clean verify
	