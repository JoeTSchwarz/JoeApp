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
import java.util.ArrayList;
import java.util.List;
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
//
import org.bridj.Pointer;
/**
 Webcam<br>
 AutoClose in case of Application Exit.<br>
 Joe Schwarz (C)
*/
public class Webcam {
  /**
  Constructor with Resolution res<br>
  Note: wrong resolution (width x height) may cause OpenIMAJGrabber aborted 
  @param device Device (i.e. camera), null will be the 1st device
  @param res Dimension, resolution width x height
  @exception Exception thrown by JAVA
  */
  @SuppressWarnings("deprecation")
  public Webcam(Device device, Dimension res) throws Exception {
    if (res == null || device == null) throw new Exception("Invalid D or device");
    if (!list.contains(device)) throw new Exception("Unknown Device: "+device.toString());
    //
    this.res = res;
    this.device = device;
    isOpen = grabber.startSession(res.width, res.height, 10, Pointer.pointerTo(device));
    if (!isOpen) throw new Exception("Unable to start "+device.toString());
    for (int i = 0; i < 3; ++i) { // try to initialize
      grabber.nextFrame();
      try {
        TimeUnit.MILLISECONDS.sleep(50);
      } catch (InterruptedException e) {}
    }
    smodel = new ComponentSampleModel(DataBuffer.TYPE_BYTE,
                                      res.width,
                                      res.height,
                                      3,
                                      res.width * 3,
                                      new int[] { 0, 1, 2 }
                                     );
    cmodel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                     new int[] { 8, 8, 8 },
                                     false,
                                     false,
                                     Transparency.OPAQUE,
                                     DataBuffer.TYPE_BYTE
                                    );
    length = res.width * res.height*3;
    // hooking watchdog for Application exit
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        close();
      }
    });
  }
  /**
  getDeviceList: List of availabe devices (cameras)
  @return List of Device
  */
  public static List<Device> getVideoList() {
    return list;
  }
  /**
  isOpen
  @return boolean true if Webcam is instantiated
  */
  public boolean isOpen() {
    return isOpen;
  }
  /**
  getImage: take an Image at the moment of invocation (snapshot)
  @return BufferedImage the snapshot image
  */
  public synchronized BufferedImage getImage() {
    if (!isOpen) return null;
    byte[] buf = new byte[length];
    grabber.nextFrame();
    grabber.getImage().getByteBuffer(length).get(buf);
    BufferedImage bImg = new BufferedImage
                            (
                             cmodel, 
                             Raster.createWritableRaster
                                   (
                                    smodel,
                                    new DataBufferByte(buf,length),
                                    null
                                   ),
                             false,
                             null
                            );
		bImg.flush();
		return bImg;
  }
  /**
  getResolution returns the Resolution of device (width x height)
  @return Dimension of Resolution
  */
  public Dimension getResolution() {
    return res;
  }
  /**
  getDeviceName returns the name of current of device
  @return BufferedImage the snapshot
  */
  public String getDeviceName() {
    return device.toString();
  }
  /**
  getDevice returns the current active device (camera)
  @return Device
  */
  public Device getDevice() {
    return device;
  }
  /**
  close Webcam session
  */
  public void close() {
    if (isOpen) {
      grabber.stopSession();
      try {
        TimeUnit.MILLISECONDS.sleep(10);
      } catch (Exception ex) { }
      isOpen = false;
      device = null;
    }
  }
  // ------------------------------------------------------------------------------
  protected Dimension res;  
  protected volatile boolean isOpen = false;
  // ------------------------------------------------------------------------------
  private static OpenIMAJGrabber grabber = new OpenIMAJGrabber();
  private static List<Device> list = grabber.getVideoDevices().get().asArrayList();
  // ------------------------------------------------------------------------------
  private int length;
  private Device device;
  private ColorModel cmodel;
  private ComponentSampleModel smodel;
}