package joe.schwarz.webcam;
//
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;
//
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Dimension;
//
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
/**
WebcamService with SocketChannel and ServerSocketChannel.<br>
Note: WebcamService self starts after instantiation and if WebcamService is run on LINUX, files/directories<br>
are case-sensitive.<br>

- If Client is not a browser it must start with sending a slash (/) or /anyFile<br>
- Streaming can be a html-page<br>
Joe Schwarz (C) 
*/
public class WebcamService extends Webcam implements Runnable {
  /**
  contructor. 
  @param device Device, webcam device
  @param res Dimension, Resolution (width x height)
  @param host String, e.g. localhost or 127.0. 0.1
  @param port int, Service port number,
  @exception Exception thrown by JAVA
  */
  public WebcamService(Device device, Dimension res,
                       String host, int port) throws Exception { 
    super(device, res);
    this.host = host; this.port = port;
    pool = Executors.newFixedThreadPool(2000);
    pool.execute(this); // self-start
    // hook on for monitoring exit
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        closeAll();
      }
    });
  }
  /**
  setLog
  @param on boolean, true: logging, false: no logging
  @exception IOException thrown by Java
  */
  public synchronized void setLog(boolean on) throws IOException {
    if (logging == on && on) return;
    if (on && log == null) {
      File file = new File(logDir);
      if (!file.exists()) file.mkdir();
      else if (file.isFile()) {
        logDir = file.getAbsolutePath();
        int e = logDir.lastIndexOf(File.separator);
        logDir = logDir.substring(0, e);        
      }
      log = new FileOutputStream(logDir+File.separator+"log_"+
                                 String.format("%6X", System.currentTimeMillis())+".txt");
      writeLog("Start Logging for WebcamService");
    }      
    logging = on;
  }
  /**
  Runnable implementation
  */
  public void run() {
    try {
      server = ServerSocketChannel.open();
      server.socket().bind(new InetSocketAddress(host, port));
      // waiting for requests from Browsers / Clients
      while(isOpen) {
        pool.execute((new Worker(server.accept())));
      }
    } catch (Exception ex) {
      writeLog("Cannot start Webcam Server "+host+":"+port);
    }
    closeAll();
  }
  /**
  closeAll. No further work with WbcamService is possible
  */
  public void closeAll() {
    super.close();
    try {
      server.close();
      if (logging || log != null) {
        writeLog("WebcamService is closed");
        log.flush();
        log.close();
      }
    } catch (Exception ex) { }
    if (pool != null) pool.shutdownNow();
  }
  //--------------------------------------------------------------------
  private class Worker implements Runnable {
    public Worker(SocketChannel socket) {
      this.socket = socket;
    }
    //
    private SocketChannel socket;
    //
    public void run() {
      try {
        Socket soc = socket.socket();
        soc.setSendBufferSize(65536);
        InetAddress ia = soc.getInetAddress();
        ByteArrayOutputStream bao = new ByteArrayOutputStream(65536);
        ByteBuffer rbuf = ByteBuffer.allocateDirect(1024);
        soc.setTcpNoDelay(true);
        //
        int n = socket.read(rbuf);
        byte[] buf = new byte[n];
        ((ByteBuffer)rbuf.flip()).get(buf, 0, n);
        String req = new String(buf).trim();
        String IP = ia.getHostAddress();
        // ClientApp
        if (buf[0] == '/') {
          writeLog("Client App (IP: "+IP+") requests \""+req+"\"");
          req = req.substring(1);
          if (n > 1) { // access files
            File file = new File(req);
            if (file.exists()) bao.write(Files.readAllBytes(file.toPath()));
            else {
              bao.write(("WebPage: \""+req+"\" not found.\r\n").getBytes());
              writeLog("....unknown \"/"+req+"\"");
            }
            bao.flush();
            buf = bao.toByteArray();
            socket.write(ByteBuffer.wrap(buf, 0, buf.length));
          } else while (!soc.isClosed()) { // Streaming
            ImageIO.write(getImage(), "JPG", bao);
            buf = bao.toByteArray();
            socket.write(ByteBuffer.wrap(buf, 0, buf.length));
            try {  // 100 Frames / Seconds
              TimeUnit.MILLISECONDS.sleep(10);
            } catch (Exception t) { }
            bao.reset();
          }
          writeLog("ClientApp (IP: "+IP+") quitted.");
          socket.close();
          return;
        }
        // ignore inputs 
        pool.execute(()-> {
          try { // do nothing...
            while (socket.read(rbuf) > 0) ;
          } catch (Exception e) { }
        });
        byte[] CRLF = {'\r','\n' };
        ByteArrayOutputStream bas = new ByteArrayOutputStream(65536);
        // direct access to Webcam
        if (req.indexOf(" / ") > 0) {
          while (!soc.isClosed()) {
            ImageIO.write(getImage(), "JPG", bas);
            bao.write(("HTTP/1.0 200 OK\r\n"+
                       "Connection: keep-alive\r\n"+
                       "Cache-Control: no-cache\r\n"+
                       "Cache-Control: no-store\r\n"+
                       "Pragma: no-cache\r\n"+
                       "Content-type: multipart/x-mixed-replace; boundary=--webcamstreaming\r\n"+
                       "--webcamstreaming\r\n"+
                       "Content-type: image/jpg\r\n"+
                       "Content-Length: "+bas.size()+"\r\n\r\n"
                      ).getBytes());
            bao.write(bas.toByteArray());
            bao.write(CRLF);
            bao.flush();
            buf = bao.toByteArray();
            socket.write(ByteBuffer.wrap(buf, 0, buf.length));
            try {  // 100 Frames / Seconds
              TimeUnit.MILLISECONDS.sleep(10);
            } catch (Exception t) { }
            bas.reset();
            bao.reset();
          }
        } else { // webpage
          String BWR = "Chrome";
          String OS = "(Unknown OS";
          n = req.indexOf("User-Agent: ");
          if (n > 0) OS = req.substring(req.indexOf("(", n), req.indexOf(")", n));
          OS += "; IP: "+IP+")";
          //
          if (req.indexOf("OPR/", n) > 0) BWR = "Opera";
          else if (req.indexOf("Edg/", n) > 0) BWR = "MS-Edge";
          else if (req.indexOf("Firefox/", n) > 0) BWR = "Firefox";
          
          n = req.indexOf(" /") + 2;
          String fName = req.substring(n, req.indexOf(" ", n));
          writeLog(BWR+" "+OS+" requests "+fName);
          File file = new File(fName);
          if (!file.exists()) {
            bas.write(("WebPage: \""+fName+"\" not found.\r\n").getBytes());
            writeLog("....unknown \"/"+fName+"\"");
            req = "text/html\r\n";
          } else {
            req = fName.toLowerCase();
            bas.write(Files.readAllBytes(file.toPath()));
            if (req.endsWith(".jpg") || req.endsWith(".png") || req.endsWith(".gif") ||
                req.endsWith(".jpeg") || req.endsWith(".bmp")) req = "image/*\r\n";
            else req = "text/html\r\n";
          }
          bao.write(("HTTP/1.1 200 OK\r\n"+
                     "Connection: keep-alive\r\n"+
                     "Cache-Control: no-cache\r\n"+
                     "Cache-Control: no-store\r\n"+
                     "Content-Type: "+req+
                     "Content-Length: "+bas.size()+"\r\n\r\n"                     
                    ).getBytes()
                   );
          bao.write(bas.toByteArray());
          bao.write(CRLF);
          bao.flush();
          buf = bao.toByteArray();
          socket.write(ByteBuffer.wrap(buf, 0, buf.length));
          writeLog(BWR+" "+OS+" quits");
        }
      } catch (Exception ex) { }
      try {
        socket.close();
      } catch (Exception e) { }
    }
  }
  //
  private synchronized void writeLog(String msg) {
    if (logging) try {
      log.write((msg+System.lineSeparator()).getBytes());
      log.flush();
    } catch (Exception e) {  }
  }
  //--------------------------------------------------------------------------
  private int port;
  private boolean logging;
  private ExecutorService pool;
  private FileOutputStream log;
  private ServerSocketChannel server;
  private String logDir = "log", host;
}
