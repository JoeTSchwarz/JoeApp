package joe.schwarz.webcam;
//
import java.awt.Dimension;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
//
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;
import javax.imageio.ImageIO;
import java.util.concurrent.*;
//
import javax.imageio.ImageIO;
import java.nio.ByteBuffer;
//
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
/**
  WebcamCanvas is the Webcam GUI interface for JavaFX or JavaSwing<br>
  AutoClose in case of Application Exit.<br>
 Joe Schwarz (C)
 */
public class WebcamCanvas extends Webcam implements Runnable {
  /**
  Constructor with Object Canvas which depends from Object canvas
  @param device Device (i.e. camera), null will be the 1st device
  @param res Dimension, resolution width x height
  @param canvas Object of java.awt.Canvas or javafx.scene.canvas.Canvas
  @param fps int Frames per Second (between 5 ... 200)
  @exception Exception thrown by Java
  */
  public WebcamCanvas(Device device, Dimension res, Object canvas, int fps) throws Exception {
    super(device, res);
    if (canvas instanceof java.awt.Canvas) {
      ((java.awt.Canvas)canvas).setSize(res);
    } else if (canvas instanceof Canvas){
      ((Canvas)canvas).setWidth(res.width);
      ((Canvas)canvas).setHeight(res.height);
    } else // Error: unknown Object
      throw new Exception("Object canvas must be either javafx.scene.canvas.Canvas or java.awt.Canvas");
    //  
    this.canvas = canvas;
    if (fps < 5) fps = 5;
    else if (fps > 200) fps = 200;
    tfps = 1000000/fps;
    fileList = new ArrayList<>();
    // hook watchdog for the case of application exit
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        close();
      }
    });
  }
  /**
  show() must be invoked after Stage.show() or JFrame.setVisible(true).
  */
  public void show() {
    if (isOpen) (new Thread(this)).start();
  }
  //
  public void run() {
    try {
      BufferedImage image;
      GraphicsContext gc = null;
      java.awt.Graphics g = null;
      if (canvas instanceof Canvas) {
        gc = ((Canvas)canvas).getGraphicsContext2D();
        gc.setFontSmoothingType(FontSmoothingType.GRAY);
      } else g = ((java.awt.Canvas)canvas).getGraphics();
      while (isOpen) {
        image = getImage();
        if (zoom != 1) { // zoom out
          BufferedImage img = new BufferedImage(wi, hi, BufferedImage.TYPE_INT_ARGB);
          java.awt.Graphics2D g2 = img.createGraphics();
          g2.setBackground(java.awt.Color.WHITE);
          g2.setPaint(java.awt.Color.BLACK);
          g2.fillRect(0, 0, wi, hi);
          g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                              java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
          g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                              java.awt.RenderingHints.VALUE_RENDER_QUALITY);
          g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                              java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
          g2.drawImage(image, 0, 0, wi, hi, null);
          g2.dispose();
          if (g != null) g.drawImage(img, 0, 0, null);
          else gc.drawImage(SwingFXUtils.toFXImage(img, null), 0, 0); 
        } else {
          if (g != null) g.drawImage(image, 0, 0, null);
          else gc.drawImage(SwingFXUtils.toFXImage(image, null), 0, 0); 
        }
        TimeUnit.MICROSECONDS.sleep(tfps);
      }
    } catch (Exception ex) { }
  }
  /**
  setFPS
  @param fps int, number of Frames Per Second
  */
  public void setFPS(int fps) {
    if (fps < 5) fps = 5;
    else if (fps > 200) fps = 200;
    tfps = 1000000/fps;
  }
  /**
  setZoom() zooms in (less than 1) or zooms out (greater than 1) on the canvas
  @param zoom double, zoom value between 0.4 and 2.0
  */
  public void setZoom(double zoom) {
    if (zoom < 0.4d) zoom = 0.4d;
    else if (zoom > 2d) zoom = 2d;
    //
    wi = (int)(Math.ceil(zoom * res.width));
    hi = (int)(Math.ceil(zoom * res.height));
    if (canvas instanceof Canvas) {
      ((Canvas)canvas).setWidth(wi);
      ((Canvas)canvas).setHeight(hi);
    } else {
      ((java.awt.Canvas)canvas).setSize(new Dimension(wi, hi));
    }
    this.zoom = zoom;
  }
  /**
  startRecord starts recording to files with prefix and sequence _xx.gzip (xx: 0 .. max)
  @param prefix String, prefix of recording files (e.g. myVideo)
  @param max int, max number of files to be recorded (where each file contains 6000 images)
  @exception Exception if Recording is in progress
  */
  public void startRecord(final String prefix, final int max) throws Exception {
    if (onRec) throw new Exception("Recoding is in Progress.");
    onRec = true;
    ForkJoinPool.commonPool().execute(() -> {
      on = true;
      long t = 0;
      int counter = 1;
      fileList.clear();
      List<BufferedImage> images = new ArrayList<>(12000);
      while (on && counter < max) { // max loop
        images.add(getImage());
        if (images.size() >= 6000) {
          long b = System.currentTimeMillis();
          fileList.add(save(images, prefix, counter++));
          images.clear(); // clear images
          t = System.currentTimeMillis() - b;
          if (t > 20) continue;
        } else t = 20;
        try {
          TimeUnit.MILLISECONDS.sleep(t);
        } catch (Exception e) { }
      }
      if (images.size() > 0) {
        fileList.add(save(images, prefix, counter));
      }
      onRec = false;
    });
  }
  /**
  onRecording 
  @return boolean, true: Recording in progress
  */
  public boolean onRecording() {
    return onRec;
  }
  /**
  stopRecord stops recording (see StartRecord)
  */
  public void stopRecord() {
    on = false;
  }
  /**
  getFileList of recorded files starting with Prefix_0.gzip (Prefix: see startRecord)
  @return List containing recorded file names or is empty if no recorded file
  */
  public List<String> getFileList() {
    while (onRec) ; // wait for completion
    return fileList;
  }
  /**
  close WebcamCanvas
  */
  @Override
  public void close() {
    super.close();
    on = false;
  }
  // ------------------------------------------------------------------------------------
  private String save(List<BufferedImage> images, String prefix, int counter) {
    try {
      String video = prefix+"_"+counter+".gzip";
      GZIPOutputStream go = new GZIPOutputStream(new FileOutputStream(video, false), true);
      ByteArrayOutputStream bos = new ByteArrayOutputStream( );
      ByteArrayOutputStream bao = new ByteArrayOutputStream( );
      // format: 4 bytes lenght of each image
      byte[] len = new byte[4];
      for (BufferedImage img : images) {
        ImageIO.write(img, "JPG", bao);
        int le = bao.size();
        len[0] = (byte)((le >> 24)&0xFF);
        len[1] = (byte)((le >> 16)&0xFF);
        len[2] = (byte)((le >> 8) &0xFF);
        len[3] = (byte)( le & 0xFF);
        bos.write(len);       
        bos.write(bao.toByteArray());
        bos.flush();
        bao.reset();
      }
      go.write(bos.toByteArray());
      go.flush( );
      go.close( );
      bos.close();
      return video;
    } catch (Exception ex) {  }
    return "";
  }
  // ------------------------------------------------------------------------------------
  private long tfps;
  private Object canvas;
  private volatile int hi, wi;
  private List<String> fileList;
  private volatile double zoom = 1d;
  private volatile boolean on = false, onRec = false;
}