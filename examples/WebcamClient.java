//
import java.io.*;
import java.util.concurrent.*;
import java.awt.image.BufferedImage;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;

import java.nio.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.util.concurrent.*;
//
import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.geometry.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.GraphicsContext;

// Joe Schwarz (C)
// Example of Client with WebcamServer/WebcamService

public class WebcamClient extends Application {
  
  public void start(Stage stage) {
    this.stage = stage;
		stage.setTitle("WebcamClient");
    //
    lab = new Label("Check Dialog");
    canvas = new Canvas(640, 480);
    ScrollPane scroll = new ScrollPane();
    scroll.setPrefSize(650, 490);
    scroll.setContent(canvas);
    start = Utility.getButton("START", "Start Client");
    start.setDisable(true);
    start.setOnAction(e -> {
      if (!on) setStop();
      else setStart();   
    });
    txt = new TextField("/");
    txt.setPrefColumnCount(16);
    txt.setOnAction(a -> {
      setStop();
    });
    HBox hbot = new HBox(5);
    hbot.setAlignment(Pos.CENTER);
    // Insets(top, right, bottom, left)
    hbot.setPadding(new Insets(0, 5, 0, 5));
    hbot.getChildren().addAll(start,
                              new Label("Streaming with / or File with /../.."),
                              txt
                             );
    VBox vbox = new VBox(5);
    vbox.setAlignment(Pos.CENTER);
    // Insets(top, right, bottom, left)
    vbox.setPadding(new Insets(5, 5, 5, 5));
    vbox.getChildren().addAll(lab, scroll, hbot);
    Scene scene = new Scene(vbox, canvas.getWidth()+20, canvas.getHeight()+75);
    
    stage.setResizable(false);
    stage.setScene(scene);
    stage.sizeToScene();
    scene.getStylesheets().add("video.css");
    stage.getIcons().add(new Image(WebcamClient.class.getResourceAsStream("icons/Camcorder.png")));
    stage.show();
    getHost();
	}
  //
  private void getHost() {
    g = canvas.getGraphicsContext2D();
    Dialog<ButtonType> dia = new Dialog<>();
    dia.setTitle("WebcamClient");
    dia.setHeaderText("Pls. specify Host and Port");
    DialogPane dp = dia.getDialogPane();
    dp.getStylesheets().add("video.css");
    dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    TextField thost = new TextField("localhost");
    thost.setPrefColumnCount(10);
    thost.setOnAction(a -> {
      host = thost.getText().trim();
      if (host.length() == 0) thost.setText("localhost");
    });      
    TextField tport = new TextField("22222");
    tport.setPrefColumnCount(3);
    tport.setOnAction(a -> {
      try {
        port = Integer.parseInt(tport.getText().trim());
      } catch (Exception ex) {
        tport.setText(""+22222);
      }
    });      
    HBox h1 = new HBox(5);
    h1.setAlignment(Pos.CENTER_LEFT);
    // Insets(top, right, bottom, left)
    h1.setPadding(new Insets(0, 5, 0, 5));
    h1.getChildren().addAll(new Label("Host"), thost);
    
    HBox h2 = new HBox(5);
    h2.setAlignment(Pos.CENTER_LEFT);
    // Insets(top, right, bottom, left)
    h2.setPadding(new Insets(0, 5, 0, 5));
    h2.getChildren().addAll(new Label("Port"), tport);
    
    VBox box = new VBox(5);
    box.getChildren().addAll(h1, h2);
    dp.setContent(box);
    dp.setPrefSize(300, 130);
    dia.initOwner(stage);
    if (dia.showAndWait().get() == ButtonType.CANCEL) {
     Utility.error("Dialog was CANCELED.");
     stop();
    }
    port = Integer.parseInt(tport.getText().trim());
    host = thost.getText().trim();
    lab.setText("WebcamServer @"+host+":"+port);
    start.setDisable(false);
    txt.requestFocus();
    txt.positionCaret(1);
  }
  //
  public void stop() {
    Platform.exit();
    System.exit(0);
  }
  //
  private void setStart() {
    on = false;
    txt.setText("/");
    txt.setDisable(false);
    txt.requestFocus();
    txt.positionCaret(1);
    start.setText("START");
    start.setStyle("-fx-font-size:10; -fx-font-weight: bold;-fx-text-fill: blue;");
  }
  //
  private void setStop() {
    txt.setDisable(true);
    start.setText("STOP");
    start.setStyle("-fx-font-size:10; -fx-font-weight: bold;-fx-text-fill: red;");
    // start displaying the received data from Webcamerver
    ForkJoinPool.commonPool().execute(() -> {
      SocketChannel soc = null;
      try {
        soc = SocketChannel.open(new InetSocketAddress(host, port));
        soc.socket().setReceiveBufferSize(65536); // 64KB
        soc.socket().setTcpNoDelay(true);
        // send to Server telling that it's client
        String tmp = txt.getText().trim();
        soc.write(ByteBuffer.wrap(tmp.getBytes(), 0, tmp.length()));
        ByteArrayOutputStream bao = new ByteArrayOutputStream(65536);
        ByteBuffer bbuf = ByteBuffer.allocateDirect(65536);
        // Streaming with server...
        if (tmp.length() == 1) {
          canvas.setHeight(480);
          canvas.setWidth(640);
          on = true; // forever
          byte[] buf = null;
          int p;
          while (on) { // read next Image fom Server
            while ((p = soc.read(bbuf)) > 0) {
              buf = new byte[p];
              ((ByteBuffer)bbuf.flip()).get(buf, 0, p);
              bao.write(buf, 0, p);
              if (p < 65536) break;
              bbuf.clear();      
            }
            bao.flush();
            BufferedImage bImg = ImageIO.read(new ByteArrayInputStream(bao.toByteArray()));
            if (bImg != null) {
              g.drawImage(SwingFXUtils.toFXImage(bImg, null), 0, 0);
              try { // 25 Frames / second
                TimeUnit.MICROSECONDS.sleep(40);
              } catch (Exception t) { }
            }
            bbuf.clear();
            bao.reset();
          }
        }
        else { // Access something
          int p;
          byte[] buf = null;
          messaging("Retrieving "+tmp+" from WebcamServer");
          while ((p = soc.read(bbuf)) > 0) {
            buf = new byte[p];
            ((ByteBuffer)bbuf.flip()).get(buf, 0, p);
            bao.write(buf, 0, p);
            bbuf.clear();      
          }
          bao.flush();
          buf = bao.toByteArray();
          String rep = new String(buf);
          if (rep.indexOf("not found") < 0 && (tmp.endsWith(".jpg") || tmp.endsWith(".jpeg") ||
              tmp.endsWith(".png") || tmp.endsWith(".gif") || tmp.endsWith(".bmp"))) {
            if (tmp.endsWith(".gif")) {
              List<BufferedImage> list = readGIF(buf);
              canvas.setWidth(list.get(0).getWidth()); 
              canvas.setHeight(list.get(0).getHeight());
              on = true; // repeat playing GIF image
              for (int i = 0, mx = list.size(); on; ++i) {
                if (i == mx) i = 0;
                g.drawImage(SwingFXUtils.toFXImage(list.get(i), null), 0, 0);
                try { // 25 Frames / second
                  TimeUnit.MILLISECONDS.sleep(40);
                } catch (Exception t) { }
              }
            } else {
              BufferedImage bImg = ImageIO.read(new ByteArrayInputStream(buf));
              canvas.setWidth(bImg.getWidth()); 
              canvas.setHeight(bImg.getHeight());
              g.drawImage(SwingFXUtils.toFXImage(bImg, null), 0, 0);
            }
          } else { // It's text
            //
            p = 0;
            int b = 0;
            int w = 0;
            int h = 0;
            while (true) {
              p = rep.indexOf("\n", p);
              if(p < 0) break;
              if ((p - b) > w) w = p-b;
              b = p;
              ++h;
              if (h > 581) { // too big
                rep = rep.substring(0, p)+"  ....\n  (shortened)";
                break;
              }
              ++p;
            }
            p = 13;
            // Height
            if (h > 1) h *= 14;
            else p = 30;
            if (h < 481) h = 480;
            // Width
            if (w > 120) { 
              w *= 10;
              if (w > 1700) w = 1700; 
            } else w = 640;
            //
            canvas.setWidth(w);
            canvas.setHeight(h);
            g.setFill(Color.BLACK);
            g.fillRect(0, 0, w, h);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
            g.fillText(rep, 1, p);
          }
        }
      } catch (Exception ex) {
        String err = ex.toString();
        if (err.length() > 600) err = err.substring(0, 600)+"...";
        messaging(err);        
      }
      Platform.runLater(()-> {
        setStart();
      });
      if (soc != null) try {
        soc.close();
      } catch (Exception e) { }
    });
  }
  //
  private void messaging(String msg) {
    canvas.setWidth(640);
    canvas.setHeight(480);
    g.setFill(Color.BLACK);
    g.fillRect(0, 0, 640, 480);
    g.setFill(Color.YELLOW);
    g.setFont(Font.font("Arial", FontWeight.BOLD, 15));
    int p = (int)((340 - 1.5 * msg.length())/2); // horizontal position
    g.fillText(msg, p > 0? p:0, 240);
  }
  // read GIF data
  private ArrayList<BufferedImage> readGIF(byte[] buf) {
    try {
      ImageReader reader = (ImageReader)ImageIO.getImageReadersByFormatName("gif").next();
      reader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(buf)), false);
      
      ArrayList<BufferedImage> list = new ArrayList<>();
      ByteArrayOutputStream bao = new ByteArrayOutputStream(65536);
      
      BufferedImage master = null;
      for (int i = 0, max = reader.getNumImages(true); i < max; ++i) {
        BufferedImage image = reader.read(i);

        NodeList children = reader.
                            getImageMetadata(i).
                            getAsTree("javax_imageio_gif_image_1.0").
                            getChildNodes();

        for (int j = 0, mx = children.getLength(); j < mx; ++j) {
          Node nodeItem = children.item(j);
          if(nodeItem.getNodeName().equals("ImageDescriptor")) {
            if(i == 0) {
              master = new BufferedImage(Integer.valueOf(nodeItem.
                                                         getAttributes().
                                                         getNamedItem("imageWidth").
                                                         getNodeValue()
                                                        ),
                                         Integer.valueOf(nodeItem.
                                                         getAttributes().
                                                         getNamedItem("imageHeight").
                                                         getNodeValue()
                                                        ),                                                   
                                         BufferedImage.TYPE_INT_ARGB
                                        );              
            }
            master.getGraphics().drawImage(image,
                                 Integer.valueOf(nodeItem.
                                                 getAttributes().
                                                 getNamedItem("imageLeftPosition").
                                                 getNodeValue()
                                                ),
                                 Integer.valueOf(nodeItem.
                                                 getAttributes().
                                                 getNamedItem("imageTopPosition").
                                                 getNodeValue()
                                                ),
                                 null
                                );
            ImageIO.write(master, "JPG", bao);
            bao.flush();
            list.add(ImageIO.read(new ByteArrayInputStream(bao.toByteArray())));
            bao.reset();
          }
        }
      }
      return list;
    } catch (Exception ex) { }
    return null;
  }
  //
  private int port;
  private Label lab;
  private String host;
  private Stage stage;
  private Button start;
  private Canvas canvas;
  private TextField txt;
  private GraphicsContext g;
  private volatile boolean on = false;
}
