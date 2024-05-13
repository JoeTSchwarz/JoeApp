package joe.schwarz.webcam;
//
import javax.swing.*;
//
import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.text.FontSmoothingType;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.*;
import javafx.geometry.Pos;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.Graphics;
import javax.imageio.ImageIO;

import java.util.concurrent.*;
import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.GraphicsContext;
/**
 Replay recorded files of Webcam in JavaFX or JavaSwing<br>
 It's depending on the instantiated Object Canvas.<br>
 Joe Schwarz (C)
*/
public class WebcamReplay implements Runnable {
  /**
  Constructor. Replay a list of images. A self-start Runnable
  @param canvas Object of type javafx.scene.canvas.Canvas or java.awt.Canvas
  @param iName String, Starting Image File name
  @param fps int, Frames per Second
  @exception Exception thrown by Java
  */
  public WebcamReplay(Object canvas, String iName, int fps) throws Exception {
    File file = new File(iName);
    int e = iName.indexOf(".gzip");
    if (!file.exists() || e < 0) throw new Exception(iName+" is unknown or has wrong format");
    images = new ArrayList<>(12000);
    try { // load the gzipped content
      int b = iName.lastIndexOf("_") + 1;
      if (b == 0) read(images, iName);
      else { // get file name root
        String root = iName.substring(0, b);
        b = Integer.parseInt(iName.substring(b, e));
        while(read(images, iName)) {
          ++b; // next sequence
          iName = root+b+".gzip";
        }
      }
    } catch (Exception ex) { }
    //
    if (images.size() == 0) {
      throw new Exception(iName+" is empty.");
    }
    int height = images.get(0).getHeight();
    int width = images.get(0).getWidth();
    this.canvas = canvas;
    // JavaFX
    if (canvas instanceof Canvas) {
      Stage popup = new Stage();
      popup.setOnCloseRequest(ev -> {
        closed = true;
        popup.close();
      });
      ((Canvas)canvas).setHeight(height);
      ((Canvas)canvas).setWidth(width);
      popup.setTitle("Replay Video");
      //
      Button next = getButton("NEXT", "Next Image");
      next.setOnAction(a -> {
        onNext();
      });
      next.setDisable(true);
      //
      Button prev = getButton("PREV", "Previous Image");
      prev.setOnAction(a -> {
        onPrev();
      });
      prev.setDisable(true);
      //
      Button act = getButton("STOP", "Stop/Start playing");
      act.setStyle("-fx-font-size:10; -fx-font-weight: bold;-fx-text-fill: red;");
      act.setOnAction(a -> {
        stop = !stop;
        step = 0;
        if (stop) {
          act.setStyle("-fx-font-size:10; -fx-font-weight: bold;-fx-text-fill: blue;");
          next.setDisable(false);
          prev.setDisable(false);
          act.setText("START");
          toggle = false;
        } else {
          act.setStyle("-fx-font-size:10; -fx-font-weight: bold;-fx-text-fill: red;");
          next.setDisable(true);
          prev.setDisable(true);
          act.setText("STOP");
          toggle = true;
          nxt = true;
        }
      });
      HBox hbox = new HBox(5);
      hbox.setAlignment(Pos.CENTER);
      hbox.setPadding(new Insets(5, 5, 5, 5));
      hbox.getChildren().addAll(prev, act, next);
      //
      VBox vbox = new VBox(5);
      vbox.setAlignment(Pos.CENTER);
      vbox.setPadding(new Insets(5, 5, 5, 5));
      vbox.getChildren().addAll((Canvas)canvas, hbox);
      vbox.setStyle(".root {-fx-font-size: 10pt;-fx-base: bisque;}"+
                    ".button:hover {-fx-background-color: bisque;-fx-text-fill: red;}"
                   );
      Scene scene = new Scene(vbox, width+10, height+60);
      scene.getStylesheets().add("video.css");
      popup.setScene(scene);
      popup.setY(0);    
      popup.setX(0);
      popup.show();
    } else if (canvas instanceof java.awt.Canvas) { 
      // Swing
      try {
        UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
      } catch (Exception ex) { }
      //
      JFrame popup = new JFrame("Replay Video");
      popup.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      popup.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent ew) {
          closed = true;
          popup.dispose();
        }       
      });
      ((java.awt.Canvas)canvas).setPreferredSize(new java.awt.Dimension(width, height));
      //
      JButton next = getJButton("NEXT");
      next.addActionListener(a -> {
        onNext();
      });
      next.setEnabled(false);
      next.setForeground(java.awt.Color.GRAY);
      //
      JButton prev = getJButton("PREV");
      prev.addActionListener(a -> {
        onPrev();
      });
      prev.setEnabled(false);
      prev.setForeground(java.awt.Color.GRAY);
      //
      JButton act = getJButton("STOP");
      act.setForeground(java.awt.Color.RED);
      act.addActionListener(a -> {
        stop = !stop;
        step = 0;
        if (stop) {
          next.setEnabled(true);
          prev.setEnabled(true);
          act.setText("START");
          act.setForeground(java.awt.Color.BLUE);
          prev.setForeground(java.awt.Color.BLUE);
          next.setForeground(java.awt.Color.BLUE);
          toggle = false;
        } else {
          prev.setForeground(java.awt.Color.GRAY);
          next.setForeground(java.awt.Color.GRAY);
          act.setForeground(java.awt.Color.RED);
          next.setEnabled(false);
          prev.setEnabled(false);
          act.setText("STOP");
          toggle = true;
          nxt = true;
        }
      });
      JPanel pB = new JPanel();
      pB.add(next); pB.add(act); pB.add(prev);
      //
      popup.add("Center", (java.awt.Canvas)canvas);
      popup.add("South", pB);
      popup.pack();
      popup.setLocation(100, 100);
      popup.setVisible(true);
    } else 
      throw new Exception("Object canvas is not javafx.scene.canvas.Canvas nor java.awt.Canvas");
    // time Frame / Second
    if (fps < 5) fps = 5;
    else if (fps > 200) fps = 200;
    tfps = 1000000/fps; // in micoSec.
    (new Thread(this)).start();
  }
  // Runnable
  public void run() {
    GraphicsContext gc = null;
    java.awt.Graphics g = null;
    if (canvas instanceof Canvas) {
      gc = ((Canvas)canvas).getGraphicsContext2D( );
      gc.setFontSmoothingType(FontSmoothingType.GRAY);
    } else {
      g = ((java.awt.Canvas)canvas).getGraphics();
    }
    BufferedImage image;
    // repeat until closed is set
    for (int i = 0, mx = images.size()-1; !closed; ) {
      image = images.get(i);
      if (g != null) g.drawImage(image, 0, 0, null);
      else gc.drawImage(SwingFXUtils.toFXImage(image, null), 0, 0);
      if (toggle) {
        if (step == 0) try {
          ++i; // next image
          TimeUnit.MICROSECONDS.sleep(tfps);
        } catch (Exception ex) { }
        else { // Stepping
          i += step;
          nxt = false;
          while (!nxt) ; // do nothing
        }
      } else {
        while (!toggle) ; // do nothing
      }
      if (i < 0) i = mx;
      else if (i > mx) i = 0;
    }
  }
  //
  private JButton getJButton(String txt) {
    JButton button = new JButton(txt);
    button.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 10));
    button.setForeground(java.awt.Color.BLUE);
    return button;
  }
  //
  private Button getButton(Object icon, String txt) {
    Button button = null;
    if (icon instanceof String) {
      button = new Button((String)icon);
      button.setStyle(".button{-fx-text-fill: blue;-fx-font-size:10;"+
                      "-fx-font-weight: bold;-fx-pref-width: 90;}"+
                      ".button:hover {-fx-background-color: bisque;"+
                      "-fx-text-fill: red;}"
                     );
    } else button = new Button(icon == null? txt:(String)icon);
    Tooltip tt = new Tooltip();
    tt.setText(txt);
    tt.setStyle("-fx-base: #AE3522; -fx-text-fill: orange; -fx-font-cb: bold;");
    button.setTooltip(tt);
    return button;
  } 
  //
  private void onNext() {
    toggle = true;
    nxt = true;
    step = 1;
  }
  //
  private void onPrev() {
    toggle = true;
    nxt = true;
    step = -1;
  }
  //
  private boolean read(List<BufferedImage> images, String fname) throws Exception {
    File file = new File(fname);
    if (!file.exists()) return false;
    byte[] buf = java.nio.file.Files.readAllBytes(file.toPath());
    if (buf.length == 0) return false;
    ByteArrayInputStream bis = new ByteArrayInputStream(buf); 
    GZIPInputStream gi = new GZIPInputStream(bis);
    //
    buf = new byte[65536]; // a must for an empty content
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    for (int n = gi.read(buf); n > 0; n = gi.read(buf)) bao.write(buf, 0, n);
    bao.flush();
    buf = bao.toByteArray();      
    bao.close();
    gi.close();
    //
    for (int i = 0, l; i < buf.length; i += (4+l)) {
      l = (int)((buf[i]&0xFF)<<24)+(int)((buf[i+1]&0xFF)<<16)+
          (int)((buf[i+2]&0xFF)<<8)+(int)(buf[i+3]&0xFF);
      bis = new ByteArrayInputStream(buf, i+4, l);
      images.add(ImageIO.read(bis));
    }
    return true;
  }
  //
  private Object canvas;
  private List<BufferedImage> images;
  private volatile int step = 0, tfps;
  private volatile boolean stop = false, closed = false, toggle = true, nxt = true;
}