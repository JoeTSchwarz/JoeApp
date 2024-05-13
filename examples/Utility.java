//
import java.io.File;
import java.util.Optional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.image.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Orientation;
import javafx.scene.control.Alert.AlertType;
import javafx.embed.swing.SwingFXUtils;

// Joe Schwarz  (C)
// Utility

public class Utility {
  /**
  getSlider
  @param w int width
  @param h int height
  @return double the selected value
  */
  public static Slider getSlider(double min, double max, int w, int h) {
    Slider slider = new Slider(min, max, 1);
    slider.setShowTickMarks(true);
    slider.setShowTickLabels(true);
    slider.setMajorTickUnit(0.1);
    slider.setBlockIncrement(0.1);
    slider.setSnapToTicks(true);
    slider.setPrefSize(w, h);
    slider.setValue(1.0d);
    return slider;
  }
  /**
   @param bImg String, BufferedImage to be saved
   @param fName String, the file name
  */
  public static void saveImage(BufferedImage bImg, String fName) {
    try {
      ImageIO.write(bImg, "PNG", new File(fName));
    } catch (Exception ex) {      
      System.err.println("Unable to create Image file:"+fName);
    }
  }
  //
  public static void saveImage(BufferedImage bImg, File file) {
    saveImage(bImg, file.getAbsolutePath());
  }
  /**
   @param txt1 String, Header text
   @param txt2 String, content message
  */
  public static void info(String txt1, String txt2) {
    Alert alert = new Alert(AlertType.INFORMATION);
    alert.setTitle(txt1);
    alert.setHeaderText("WEBCAM");
    alert.setContentText(txt2);
    alert.showAndWait();
  }
  /**
   @param txt String, error message
  */
  public static void error(String txt) {
    Alert alert = new Alert(AlertType.ERROR);
    alert.setTitle("ERROR");
    alert.setHeaderText("WEBCAM");
    alert.setContentText(txt);
    alert.showAndWait();
  }
  /**
  @param txt String, the JButton name
  */
  public static javax.swing.JButton getJButton(String txt) {
    javax.swing.JButton button = new javax.swing.JButton(txt);
    button.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 10));
    button.setForeground(java.awt.Color.BLUE);
    return button;
  }
  /**
   @param icon JFX Image or String
   @param txt String as tooltip or button name (if icon = null)
   @return String abs. path of selected file
  */
  public static Button getButton(Object icon, String txt) {
    Button button = null;
    if (icon == null || icon instanceof String) {
      button = new Button(icon == null? txt:(String)icon);
      button.setStyle("-fx-font-size:10; -fx-font-weight: bold;");
    } else try {
      button = new Button( );
      button.setGraphic(new ImageView((Image) icon));
    } catch (Exception ex) { 
      button = new Button(txt);
      button.setStyle("-fx-font-weight: bold;");
    }
    Tooltip tt = new Tooltip();
    tt.setText(txt);
    tt.setStyle("-fx-base: #AE3522; -fx-text-fill: orange; -fx-font-cb: bold;");
    button.setTooltip(tt);
    return button;
  }
  //
  public static String getFile(String dir) {
    String fName = dir+File.separator+String.format("image%06X.png", System.currentTimeMillis());
    Dialog<ButtonType> dia = new Dialog<>();
    dia.setTitle("WebcamUtility");
    dia.setHeaderText("Get a File");
    dia.setContentText("File name:");
    DialogPane dp = dia.getDialogPane();
    dp.getStylesheets().add("video.css");
    dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    //
    TextField tf = new TextField(fName);
    tf.setPrefSize(fName.length(), 10);
    tf.setOnAction(e -> {
      if (tf.getText().length() == 0) { 
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setInitialDirectory(new File(dir));
        File file = dirChooser.showDialog(null);
        if (file != null) {
          String dn = file.getAbsolutePath();
          tf.setPrefSize(dn.length(), 10);
          tf.setText(dn+File.separator+String.format("image%06X.png", System.currentTimeMillis()));         
        }
      }
    });
    VBox vbox = new VBox(5);
    vbox.setPadding(new Insets(5, 5, 5, 5));
    vbox.getChildren().addAll(tf);
    vbox.setAlignment(Pos.CENTER);

    dp.setContent(vbox);
    dp.setPrefSize(fName.length()+300, 150);
    Platform.runLater(() -> {
      tf.requestFocus();
    });
    Optional<ButtonType> result = dia.showAndWait();
    fName = tf.getText().trim();
    if (result.get() == ButtonType.CANCEL || fName.length() == 0) return null;
    return fName;
  }
  /**
   @param dir Directory name
   @return FileChooser
  */
  public static FileChooser fileChooser(String dir) {
    FileChooser fChooser = new FileChooser();
    fChooser.setInitialDirectory(new File(dir));
    fChooser.setTitle("JPG/JPEG/PNG/GIF");
    fChooser.getExtensionFilters().
             add(new FileChooser.ExtensionFilter("Image",
                                                 "*.jpg",  "*.JPG",
                                                 "*.png",  "*.PNG",
                                                 "*.gif",  "*.GIF"));
    return fChooser;
  }
  /**
  input
  @param header string
  @param def String, default text
  @return String the input String
  */
  public static String input(String header, String def) {
    Dialog<ButtonType> dia = new Dialog<ButtonType>();
    dia.setTitle("JFXOptions");
    dia.setHeaderText(header);
    dia.setResizable(true);

    // Set up dialog pane
    VBox box = new VBox();
    box.setPadding(new Insets(5, 5, 0, 5));
    TextField txt = new TextField(def);
    txt.setOnAction(a -> {
      if (def instanceof String) return;
      try {
        Integer.parseInt(txt.getText().trim());
      } catch (Exception ex) {
        txt.setText(def);
      }
    });
    box.getChildren().add(txt);
    //
    DialogPane dp = dia.getDialogPane();    
    dp.getButtonTypes().addAll(ButtonType.OK);
    dp.setContent(box);
    Platform.runLater(() -> txt.requestFocus());
    dp.getStylesheets().add("video.css");
    dp.setPrefSize(def.length()+300, 150);
    Optional<ButtonType> B = dia.showAndWait();
    if (B.get() == ButtonType.OK) return txt.getText().trim();
    return "0";
  }
}