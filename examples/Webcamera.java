// Java
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.util.*;
import java.io.*;
//
import java.util.concurrent.*;
// JFX
import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.geometry.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.canvas.*;
import javafx.scene.text.*;
import javafx.scene.image.*;
import javafx.scene.control.Alert.*;
import javafx.scene.transform.Rotate;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
//
import joe.schwarz.webcam.*;
 
// Joe Schwarz (C)
// Example of Webcam with JavaFX as Camera

public class Webcamera extends Application {
  //
  public void start(Stage stage) {
		stage.setTitle("Webcamera");
    // VGA 640 x 480
    height = 480;
    width = 640;
    //
    Canvas canvas = new Canvas(width, height);
    try {
      webcam = new Webcam(Webcam.getVideoList().get(0), new Dimension(640, 480));
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(0);
    }
    gc = canvas.getGraphicsContext2D( );
    gc.setFontSmoothingType(FontSmoothingType.GRAY);
    canvas.addEventFilter(MouseEvent.ANY, (e) -> canvas.requestFocus());
    // +/- for right or left rotating
    canvas.setOnKeyTyped(e -> {
      String str = e.getCharacter();
      int len = str.length();
      if (len == 0) return;
      char ch = str.charAt(0);
      if (ch == '+') ++rot;      // rotate Left
      else if (ch == '-') --rot; // rotate Right
      else return;
      if (rot > 360) rot = 1;
      else if (rot < 0) rot = 359;
      //
      ImageView iv = new ImageView(SwingFXUtils.toFXImage(image, null ));
      Rotate rotate = new Rotate();
      rotate.setPivotX(xx);
      rotate.setPivotY(yy);
      rotate.setAngle(rot);
      iv.getTransforms().add(rotate);      
      SnapshotParameters params = new SnapshotParameters();
      params.setFill(Color.TRANSPARENT);
      rImg = SwingFXUtils.fromFXImage(iv.snapshot(params, null), null);
      draw(rImg);
      return;
    });
    canvas.setOnMouseMoved(e -> {
      if (!move) return;
      xx = (int)e.getX();
      yy = (int)e.getY();
      draw(image);
    });
    // cropping image
    canvas.setOnMouseReleased(e -> {
      if (!flag) return;
      xx = (int)e.getX(); 
      yy = (int)e.getY();
      if (x0 < 0) {
        x0 = xx;
        y0 = yy;
        move = true;
        return;
      }
      move = false;
      x1 = xx;
      y1 = yy;
      draw(image);
      // Cropping image
      if (x0 > x1) {
        int x = x0;
        x0 = x1;
        x1 = x;
      }
      if (y0 > y1) {
        int y = y0;
        y0 = y1;
        y1 = y;
      }
      //
      int xb = x0, xe = x1;
      int yb = y0, ye = y1;
      //
      int w = xe-xb, h = ye-yb;
      cImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      for(int y = yb, l = 0; l < h; y++, ++l) for(int x = xb, i = 0; i < w; ++x, ++i) {
        cImg.setRGB(i, l, image.getRGB(x, y)); // let it be
      }
      x0 = y0 = x1 = y1 = -1;
      flag = false;
      image = cImg;
      draw(cImg);
    });
    x0 = y0 = x1 = y1 = -1;
    final Image icon = new Image(Webcamera.class.getResourceAsStream("/crop.png"));
    Button crop = Utility.getButton(icon, "CROPPING");
    crop.setOnAction(a->{
      if (pause) {
        crop.setGraphic(new ImageView((Image) icon));
        canvas.setHeight(height);
        canvas.setWidth(width);
        pause = false;
        flag = false;
        return;
      }
      Image img = new Image(Webcamera.class.getResourceAsStream("/cropFocus.png")); 
      crop.setGraphic(new ImageView((Image) img));
      if (image != null) {
        rot = 0;
        flag = true;
        pause = true;
        sImg = iClone(image);
        rImg = iClone(image);
        //
        waiting();
        gc.drawImage(SwingFXUtils.toFXImage(image, null), 0, 0);
      }
    });
    //
    MenuButton mButton = new MenuButton("Choice",
                    new ImageView(new Image(Webcamera.class.getResourceAsStream("/cameraIcon.png"))));
    MenuItem save = new MenuItem("SAVE", 
                    new ImageView(new Image(Webcamera.class.getResourceAsStream("/save.png"))));
    save.setOnAction(a -> {
      String fName = Utility.getFile(dir);
      if (fName == null) {
        return;
      }
      crop.setGraphic(new ImageView((Image) icon));
      String sfx = fName.substring(fName.indexOf(".")+1).toUpperCase();
      if (!"JPG".equalsIgnoreCase(sfx) && !"PNG".equalsIgnoreCase(sfx)) {
        Utility.error("Expected file ends with .jpg, jpeg or .png but found: "+fName);
        return;
      }
      Utility.saveImage(image, fName);
    });
    MenuItem reset = new MenuItem("RESET");
    reset.setOnAction(a -> {
      crop.setGraphic(new ImageView((Image) icon));
      if (pause && sImg != null) {
        rot = 0;
        rImg = null;
        cImg = null;
        flag = true;
        move = false;
        image = sImg;
        x0 = y0 = x1 = y1 = -1;
        gc.drawImage(SwingFXUtils.toFXImage(image, null), 0, 0);
      }
    });
    //
    mButton.getItems().addAll(save, reset);
     
    HBox hbox = new HBox(5);
    hbox.setAlignment(Pos.CENTER);
    // Insets(double top, double right, double bottom, double left)
    hbox.setPadding(new Insets(5, 5, 5, 5));
    hbox.getChildren().addAll(crop, mButton);
    //
    VBox vbox = new VBox(5);
    vbox.setAlignment(Pos.CENTER);
    vbox.setPadding(new Insets(5, 5, 5, 5));
    vbox.getChildren().addAll(canvas, hbox);
    Scene scene = new Scene(vbox);
    scene.getStylesheets().add("video.css");
    // Background color for canvas
    stage.sizeToScene();
    stage.getIcons().add(new Image(getClass().getResourceAsStream("/qrcIcon.png")));
    stage.setResizable(false);
    stage.setScene(scene);
    stage.show();
    ForkJoinPool.commonPool().execute(() -> {
      while (running) {
        if (!pause) {
          image = webcam.getImage();
          gc.drawImage(SwingFXUtils.toFXImage(image, null), 0, 0);
        }
        try {
          // 100 Frames Per Second
          TimeUnit.MILLISECONDS.sleep(10);
        } catch (Exception e) { }
      }
    });
  }
  // clean up before terminated
  public void stop() {
    running = false;
    Platform.exit();
    System.exit(0);
  }
  // paint the Canvas
  private void draw(BufferedImage img) {
    int w = img.getWidth();
    int h = img.getHeight();
    // fill canvas with AZURE
    gc.setFill(Color.web("#F0FFFF"));
    gc.fillRect(0, 0, width, height);
    // cropping image
    int ix0 = (width-w)/2;
    int iy0 = (height-h)/2;
    int ix1 = ix0+w;  
    int iy1 = iy0+h;
    gc.drawImage(SwingFXUtils.toFXImage(img, null), ix0, iy0);
    if (!flag) return;
    //
    int X = move? xx:x1;
    int Y = move? yy:y1;
    
    if (x0 < ix0) x0 = ix0;
    if (y0 < iy0) y0 = iy0;
    if (X > ix1) X = ix1;
    if (Y > iy1) Y = iy1;
      
    gc.setStroke(Color.YELLOW);
    gc.setLineWidth(2);
    gc.strokeLine(x0, y0, X, y0);
    gc.strokeLine(x0, Y, X, Y);
    gc.strokeLine(x0, y0, x0, Y);
    gc.strokeLine(X, y0, X, Y);
  }
  //
  private void waiting() {
    try {
      TimeUnit.MILLISECONDS.sleep(20);
    } catch (Exception ex) { }
  }
  //
  private BufferedImage iClone(BufferedImage bImg) {
    int w = bImg.getWidth(), h = bImg.getHeight();
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < h; y++) for (int x = 0; x < w; ++x) img.setRGB(x, y, bImg.getRGB(x,y));
    return img;
  }
  //
  private Webcam webcam;
  private GraphicsContext gc;
  private BufferedImage image, rImg, cImg, sImg;
  private String dir = System.getProperty("user.dir");
  private volatile boolean move = false, flag = false;
  private int width, height, rot, x0, y0, x1, y1, xx, yy;
  private volatile boolean running = true, pause = false;
}
