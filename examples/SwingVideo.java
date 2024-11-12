//
import java.io.*;
import java.awt.Dimension;
import java.awt.Canvas;
import java.awt.Color;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import java.util.concurrent.*;
import java.awt.image.BufferedImage;
import javax.swing.filechooser.FileNameExtensionFilter;
//
import javax.swing.*;
//
import joe.schwarz.webcam.*;
 
// Joe Schwarz (C)
// Example of WebcamCanvas with JavaSwing

public class SwingVideo extends JFrame {
  //
  public static void main(String... argv) throws Exception {
    UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
    new SwingVideo( );
  }
  //
  public SwingVideo() throws Exception {
    setTitle("SwingVideo");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setIconImage((new ImageIcon(ImageIO.read(SwingVideo.class.
                  getResourceAsStream("icons/Camcorder.png")))).getImage());

    List<Device> dlist = WebcamCanvas.getVideoList();
    if (dlist.size() == 0) {
      JOptionPane.showMessageDialog(this,"No Webcam is found.");
      System.exit(0);
    }
    // VGA Resolution 640 x 480
    Canvas canvas = new Canvas();
    final Dimension D = new Dimension(640, 480);
    video = new WebcamCanvas(dlist.get(0), D, canvas, fps);
    //
    JComboBox<String> combo = new JComboBox<>();
    for (Device d:dlist) {
      String s = d.toString();
      combo.addItem(s);
    }
    combo.setSelectedIndex(0);
    combo.addActionListener(e -> {
      int idx = combo.getSelectedIndex();
      if (idx != old) try {
        video.close();
        video = new WebcamCanvas(dlist.get(idx), D, canvas, fps);
        old = idx; // replace
        video.show();
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this,ex.toString());
      }
    }); 
    JTextField tfps = new JTextField("25", 3);
    tfps.addActionListener(a -> {
      ofps = fps;
      try {
        fps = Integer.parseInt(tfps.getText().trim());
        if (fps < 5) fps = 5;
        else if (fps > 200) fps = 200;
        video.setFPS(fps);
        JOptionPane.showMessageDialog(this,"Frames/Second is set to "+fps);
      } catch (Exception ex) {
        tfps.setText(""+25);
        fps = ofps;
      }
    });      
    JPanel pT = new JPanel();
    pT.add(combo); pT.add(new JLabel("Frames/Sec.")); pT.add(tfps);
    //
    JButton snap = Utility.getJButton("SNAPSHOT");
    snap.addActionListener(a -> {
      snapshot();
    });
    JButton zoom = Utility.getJButton("ZOOM");
    zoom.addActionListener(a -> {
      zooming();
    });
    JButton record = Utility.getJButton("RECORD");
    record.addActionListener(a -> {
      on = !on;
      if (on) record.setForeground(Color.RED);
      else record.setForeground(Color.BLUE);
      recording();
    });
    JButton play = Utility.getJButton("REPLAY");
    play.addActionListener(a -> {
      playing();
    });
    //
    JPanel pB = new JPanel();
    pB.add(snap); pB.add(zoom); pB.add(record); pB.add(play);
    //
    add("North", pT);
    add("Center", canvas);
    add("South", pB);
    //
    if (dlist.size() == 0) Y = 80; else Y = 115;
    setSize(D.width+16, D.height+Y);
    setResizable(false);
    setVisible(true);
    try {
      video.show();
    } catch (Exception ex) {
      ex.printStackTrace();
      video.close();
      dispose();
      System.exit(0);
    }
	}
  //
  private void snapshot( ) {
    String fName = dir+File.separator+String.format("image%06X.jpg", System.currentTimeMillis());
    fName = JOptionPane.showInputDialog(this, "File Name", fName);
    if (fName == null) return;
    int idx = fName.lastIndexOf(".");
    String sfx = "jpg";
    if (idx < 0) fName += ".jpg";
    else sfx = fName.substring(idx+1);
    try {
      ImageIO.write(video.getImage(), sfx, new FileOutputStream(fName));
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this,"Unable to create:"+fName);
    }
  }
  //
  private void zooming() {
    double oDelta = delta;
    String s = JOptionPane.showInputDialog(this,"Zoom (0.4 .. 2.0)", "1.0");
    try {
      delta = Double.parseDouble(s.trim());
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this,"Invalid value: "+s);
      return;
    }      
    if (delta > 2d) delta = 2d;
    else if (delta < 0.4d) delta = 0.4d;
    if (oDelta == delta) return;
    video.setZoom(delta);
    setSize(16+(int)(D.width*delta), (int)(D.height*delta)+Y);
  }
  //
  private void recording() {
    if (on) try {
      String prefix = JOptionPane.showInputDialog(this, "Prefix of File name", "video");
      video.startRecord(prefix, 10); // max 10 files or 20 minutes
      return;
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this,"Error: "+ex.toString());
      return;
    }
    video.stopRecord();
    List<String> list = video.getFileList();
    List<String> flist = video.getFileList();
    if (flist.size() > 0) {
      StringBuffer sb = new StringBuffer( );
      for (String fn : flist) sb.append("\n"+fn);
      JOptionPane.showMessageDialog(this,"Recording from "+video.getDeviceName()+":"+sb.toString()); 
    } else 
      JOptionPane.showMessageDialog(this,"Recording from "+video.getDeviceName()+"\nNothing.");
  }
  //
  private void playing( ) {
    SwingUtilities.invokeLater(()->{
      JFileChooser chooser = new JFileChooser(new File(dir));
      chooser.setFileFilter(new FileNameExtensionFilter("GZIP", "gzip"));
      chooser.setDialogTitle("GZIP ImageFile");
      int rt = chooser.showOpenDialog(this);
      if (rt != JFileChooser.APPROVE_OPTION) return;
      File file = chooser.getSelectedFile();
      if (file != null) try {
        new WebcamReplay(new Canvas(), file.getAbsolutePath(), fps);
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Unable to replay "+file.getAbsolutePath());
      }
    });
  }
  //
  private Dimension D;
  private double delta = 1d;
  private WebcamCanvas video;
  private int old = 0, Y, fps = 25, ofps;
  private volatile boolean stop = false, on = false;
  private String dir = System.getProperty("user.dir");
}
