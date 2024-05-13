//
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.Dimension;
import javax.imageio.ImageIO;
import java.util.concurrent.*;
import java.awt.image.BufferedImage;
//
import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.geometry.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.canvas.Canvas;

import joe.schwarz.webcam.*;
 
// Joe Schwarz (C)
// Example of WebcamCanvas with JavaFX

public class JfxVideo extends Application {
  
  public void start(Stage stage) {
    this.stage = stage;
		stage.setTitle("JFXWebcam");
    List<Device> dlist = WebcamCanvas.getVideoList();
    if (dlist.size() == 0) {
      Utility.error("No Webcam is found.");
      Platform.exit();
      System.exit(0);
    }
    // VGA Resolution
    Dimension D = new Dimension(640, 480);
    canvas = new Canvas();
    try {
      video = new WebcamCanvas(dlist.get(0), D, canvas, fps);
    } catch (Exception ex) {
      ex.printStackTrace();
      Utility.error("Unable to create JFXCanvas");
      Platform.exit();
      System.exit(0);
    }
    ArrayList<String> list = new ArrayList<>(dlist.size());
    for (Device d:dlist) list.add(d.toString());
    ComboBox<String> combo = new ComboBox<>();
    combo.getItems().addAll(list);
    combo.setValue(list.get(0));
    combo.setOnAction(ev -> {
      int idx = combo.getSelectionModel().getSelectedIndex();
      if (idx != old) try {
        video.close();
        video = new WebcamCanvas(dlist.get(idx), D, canvas, fps);
        old = idx; // replace
        video.show();
      } catch (Exception ex) {
        Utility.error(ex.toString());
      }
    }); 
    TextField tfps = new TextField("25");
    tfps.setPrefColumnCount(3);
    tfps.setOnAction(a -> {
      ofps = fps;
      try {
        fps = Integer.parseInt(tfps.getText().trim());
        if (fps < 5) fps = 5;
        else if (fps > 200) fps = 200;
        video.setFPS(fps);
        Utility.info("Frame Per Second", "Frames/Second is tet to "+fps);
      } catch (Exception ex) {
        tfps.setText(""+25);
        fps = ofps;
      }
    });      
    HBox htop = new HBox(5);
    htop.setAlignment(Pos.CENTER);
    // Insets(top, right, bottom, left)
    htop.setPadding(new Insets(0, 5, 0, 5));
    htop.getChildren().addAll(combo, new Label("Frames/Sec."), tfps);
    //
    Button but = Utility.getButton("SNAPSHOT", "Snapshot a Video Image");
    but.setOnAction(e -> {
      snapshot( ); 
    });
    Button zoom = Utility.getButton("ZOOM", "Zoom the Video");
    zoom.setOnAction(e -> {
      zooming(); 
    });
    Button record = Utility.getButton("RECORD",
                   "Record: 1x click: start, 2x Click: stop. OR 1x F2: start, 2x F2: stop");
    record.setOnAction(a -> {
      on = !on;
      if (on) record.setStyle("-fx-text-fill: red;-fx-font-size:10;-fx-font-weight: bold;");
      else record.setStyle("-fx-text-fill: blue;-fx-font-size:10;-fx-font-weight: bold;");
      recording();
    });
    Button play = Utility.getButton("REPLAY", "Play recorded Video OR F3");
    play.setOnAction(a -> {
      playing();
    });
    slider = Utility.getSlider(0.4, 2.0, 400, 40);
    slider.valueProperty().addListener((observable, oValue, nValue) -> {
      delta = nValue.doubleValue();
      video.setZoom(delta);
      stage.setWidth(canvas.getWidth()+30);
      stage.setHeight(canvas.getHeight()+120);
    });
    HBox hbox = new HBox(5);
    hbox.setAlignment(Pos.CENTER);
    // Insets(top, right, bottom, left)
    hbox.setPadding(new Insets(5, 5, 5, 5));
    hbox.getChildren().addAll(but, zoom, record, play);
    VBox vbox = new VBox(5);
    vbox.setAlignment(Pos.CENTER);
    // Insets(top, right, bottom, left)
    vbox.setPadding(new Insets(5, 5, 5, 5));
    vbox.getChildren().addAll(htop, canvas, hbox);
    Scene scene = new Scene(vbox, canvas.getWidth()+20, canvas.getHeight()+75);
    
    //stage.setResizable(false);
    stage.setScene(scene);
    stage.sizeToScene();
    scene.getStylesheets().add("video.css");
    stage.getIcons().add(new Image(JfxVideo.class.getResourceAsStream("Camcorder.png")));
    stage.show();
    try {
      video.show();
    } catch (Exception ex) {
      ex.printStackTrace();
      stop();
    }
	}
  //
  public void stop() {
    Platform.exit();
    System.exit(0);
  }
  //
  private void zooming() {
    double oDelta = delta;
    Dialog<ButtonType> dia = new Dialog<>();
    dia.setTitle("ZOOM IN/OUT");
    dia.setHeaderText("Zooming factor");
    DialogPane dp = dia.getDialogPane();
    dp.getStylesheets().add("video.css");
    dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    dp.setContent(slider);
    dp.setPrefSize(420, 50);
    if (dia.showAndWait().get() != ButtonType.OK) {
      slider.setValue(oDelta);
      delta = oDelta;
      video.setZoom(delta);
      stage.setWidth(canvas.getWidth()+30);
      stage.setHeight(canvas.getHeight()+120);
    }
  }
  //
  private void snapshot( ) {
    String fName = Utility.getFile(dir);
    if (fName == null) return;
    int idx = fName.lastIndexOf(".");
    String sfx = "jpg";
    if (idx < 0) fName += ".jpg";
    else sfx = fName.substring(idx+1);
    try {
      javax.imageio.ImageIO.write(video.getImage(), sfx, new FileOutputStream(fName));
    } catch (Exception ex) {
      Utility.error("Unable to create:"+fName);
    }
  }
  //
  private void recording() {
    if (on) try {
      String prefix = Utility.input("Prefix of File name", "video");
      video.startRecord(prefix, 10); // max 10 files or 20 minutes
      return;
    } catch (Exception ex) {
      Utility.error("Error: "+ex.toString());
    }
    video.stopRecord();
    List<String> flist = video.getFileList();
    if (flist.size() > 0) {
      StringBuffer sb = new StringBuffer();
      for (String fn : flist) sb.append(fn+"\n");
      Utility.info("Recording from "+video.getDeviceName(), sb.toString()); 
    } else Utility.info("Recording from "+video.getDeviceName(), "Nothing.");
  }
  //
  private void playing( ) {
    Platform.runLater(()->{
      FileChooser fC = new FileChooser();
      fC.setInitialDirectory(new File(dir));
      fC.setTitle("Webcam recoded file");
      fC.getExtensionFilters().add(new FileChooser.ExtensionFilter("image", "*.gzip"));
      File file = fC.showOpenDialog(stage);
      if (file != null) try {
        new WebcamReplay(new Canvas(), file.getAbsolutePath(), fps);
      } catch (Exception ex) {
        Utility.error("Unable to replay "+file.getAbsolutePath());
      }
    });
  }
  //
  private Stage stage;
  private Slider slider;
  private Canvas canvas;
  private double delta = 1d;
  private WebcamCanvas video;
  private int old = 0, fps = 25, ofps;
  private volatile boolean stop = false, on = false;
  private String dir = System.getProperty("user.dir");
}
