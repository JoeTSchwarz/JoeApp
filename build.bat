REM set CLASSPATH and PATH
set CLASSPATH=./bridj-0.6.2.jar;%CLASSPATH%
set PATH=./bin;%PATH%
REM compile Webcam APIs
cd webcam
javac -g:none -d ./classes *.java
cd classes
REM create a JAR file joe.webcam.jar
jar -cvfm ../../joe.webcam.jar ../resources/MANIFEST.MF joe/schwarz/webcam/*.class > ../o.txt
cd ../..
set CLASSPATH=./joe.Webcam.jar;%CLASSPATH%
REM compile the examples
cd examples
javac -g:none -d ./classes *.java
cd classes
REM create server.jar, jfx.jar and swing.jar
jar -cvfm ../../server.jar ../resources/manifest_Server.txt WebcamServer.class ../icons/theWeb.png > ../o1.txt
jar -cvfm ../../swing.jar ../resources/manifest_Swing.txt SwingVideo.class Utility.class ../icons/Camcorder.png > ../o2.txt
jar -cvfm ../../jfx.jar ../resources/manifest_Jfx.txt JfxVideo.class Utility.class video.css ../icons/Camcorder.png > ../o3.txt
cd ..
REM creare Javadoc
cd ..
javadoc -d doc ./webcam/*.java 
REM run test jfx.jar
javaw -jar jfx.jar

