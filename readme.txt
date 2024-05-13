A simplified sarxos/webcam-capture

When I started studying the sarxos/webcam-capture package, I was a bit confused. The package is very verbose and has too many APIs and is completely intertwined. So, I decided to simplify this Sarxos/WebcamCapture package to suit my needs: Webcam for JavaFX, Swing and as a web server. Since Webcam only supplies images, Webcam is thread-safe. And here it is. The package is named "joe.schwarz.webcam" with the 7 following APIs in the webam folder:

- Device.java: copied unchanged from the sarxos/webcam-capture package.
- DeviceList.java: copied unchanged from sarxos/webcam-capture package.
- OpenIMAJGrabber.java: copied unchanged from the sarxos/webcam-capture package.
- Webcam.java: completely rewritten.
- WebcamCanvas.java: An extended Webcam. It's an interface for JavaFX or Swing application. Instead of JPanel, it returns a generic Object which can be either javafx.scene.canvas.Canvas or java.awt.Canvas (see JfxVideo and SwingVideo in examples folder). 
- WebcamReplay.java: new. An API for playing recorded videos (see JfxVideo and SwingVideo in examples folder).
- WebcamService: new. An extended Webcam. WebcamService can be run as webcam server (see WebcamServer in the examples folder), which can serves browsers and client apps as well. Client App must start to communicate with WebcamService with a request "/" (see WebcamClient in the examples folder). Browser accesses to WebcamService with http://host:port or http://host:port/<html-directory>/anypage.html (see html/js folder and some html-pages). Note: if WebcamService is run on LINUX, all files/directories are case-sensitive

These APIs are bundled in joe.webcam.jar. Along with the reduced webcam, I add three following folders to the package:

- bin: for OpenIMAJGrabber.dll (Windows 64) and OpenIMAJGrabber.so (Linux 64). If you use another operating system (e.g. MacOS), just look for the sarxos/webcam-capture package. Note: be careful to the new OpenIMAJGrabber version from the site "dllme.com" (reason it won't work properly).
- Examples: includes JfxVideo.java, SwingVideo.java, Webcamera.java, Utility.java, WebcamClient.java and WebcamServer.java. These Java-sources show you how to work with Webcam, WebcamCanvas, WebcamReplay, and WebcamService. A subdirectory classes with icon Camcorder.png and some manifest files.
- html: contains the HTML pages for checking functionality between browsers and webcam server. The js subdirectory contains refresh.js, which is called from refresh.html.
- bridj-0.6.2.jar or bridj-0.7.0.jar: copied from the sarxos/webcam-capture package, or you can download it HERE: http://www.java2s.com/Code/Jar/b/Downloadbridj062jar.htm or http://www.java2s.com/example/jar/b/download-bridj070jar-file.html. Note: After a March 23, 2024 Windows 10 update on my Acer laptop, OpenIMAJGrabber suddenly stopped working (JVM dump). After a long search for the mysterious causes, I found out that bridj-0.7.0.jar fixes the problems, not the newer version OpenIMAJGrabber.dll. However, I include both jar files for you to choose from.
- jfx.jar, swing.jar, joe.webcam.jar abd server.jar are the bundles compiled and jarred under Java 9.0.4

The sources are compiled and bundled (or jarred) under:

     Java version “9.0.4”
     Java(TM) SE Runtime Environment (Build 9.0.4+11)
     Java HotSpot(TM) 64-bit Server VM (Build 9.0.4+11, Mixed Mode)

If you have a higher version JDK, you should download the appropriate Open JavaFX package before you can run (or recompile and re-jar) the JAR files with the supplied manifest files.

Examples folder:
* WebcamServer: It is an implementation of WebcamService (server.jar) in Java SWING. WebcamServer supports remote client apps and browsers. Accessing formats:
- http://host:port for a single webcam image (Browser)
- http://host:port/anyDirectory/any.html (Browser custom web pages)
- /anyDirectory/anyFileName for direct access (remote Clients to any image or text file)
- / (slash) continuous Streaming (remote Clients) 
Note: anyDirectory must be a subdirectory of WebcamServer directory.
* WebcamClient: An example shows you how to access WebcamServer with SocketChannel. Formats:
- / (slash) continuous Streaming
- /anyDirectory/anyFileName to access remote file on server site.
* Webcamera: this app shows you how Webcam works with external Canvas. It allows you to take a snapshot and crop it (with the mouse to draw a rectangle on an area you want to keep) as you like to have.
* JfxVideo (jfx.jar) and SwingVideo (swing.jar): Both are examples that show you how to work with WebcamCanvas, WebcamReplay: snapshot, zoom in/out, recording and replaying.

HTML folder:
- webcam.html
- bigwebcam.html (zoom out of webcam.html)
- refresh.html (using external Java Script)
They all show you how to run webcam with any browser. Refresh.html calls an external JS script (refresh.js) from the JS folder.

doc folder: This folder contains the Javadoc style documents of the reduced webcam package.

Webcam.gif is the animation that shows you how a browser accesses WebcamServer.

Installation:
1) download webcam.zip to any directory of yoir choice and unzip it
2) open a CMD window on this unzipped directory
3) run build.bat 

If your OS is LINUX (Ubuntu or Mint): on a Terminal of directory where webcam.zip is unzipped.
1) rename build.sh.txt to build.sh
2) make it executable chmod 777 build.sh
3) run build.sh



































