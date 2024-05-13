//
import javax.swing.*;
import java.util.concurrent.*;
//
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
//
import joe.schwarz.webcam.*;
 
// Joe Schwarz (C)
// Example of WebcamService as WebcamServer
// Note: if WebcamServer is run on LINUX, files/directories are case-sensitive. 


public class WebcamServer extends JFrame {
  public static void main(String... argv) throws Exception {
    UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
    new WebcamServer();
  }
  public WebcamServer() throws Exception {
    java.util.List<Device> dlist = WebcamService.getVideoList();
    java.util.ArrayList<String> lst = new java.util.ArrayList<>(dlist.size());
    for (Device d:dlist) lst.add(d.toString());
    setTitle("WebcamServer");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setIconImage((new ImageIcon(javax.imageio.ImageIO.read(WebcamServer.class.
                  getResourceAsStream("/theWeb.png")))).getImage());
    
    //
    int wi = 256, he = 192;
    Canvas canvas = new Canvas();
    canvas.setSize(new Dimension(wi, he));
    JPanel pc = new JPanel(); pc.add(canvas);
    //
    JLabel lab = new JLabel("Check Dialog");
    lab.setHorizontalAlignment(JLabel.CENTER);
    JPanel pn = new JPanel(); pn.add(lab);
    //
    JButton but = new JButton("NO LOG");
    but.setForeground(java.awt.Color.BLUE);
    but.setEnabled(false);
    but.addActionListener(a -> {
      boolean log = "NO LOG".equals(but.getText());
      if (!log) {
        but.setText("NO LOG");
        but.setForeground(java.awt.Color.BLUE);
      } else try {
        but.setText("LOG");
        service.setLog(true);
        but.setForeground(java.awt.Color.RED);
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this,"Unable to set LOG:"+ex.toString());
      }
    });
    JPanel ps = new JPanel(); ps.add(but);
    //
    add("North", pn);
    add("Center", pc);
    add("South", ps);
    //
    addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent ew) {
        service.closeAll();
        running = false;
        dispose();
        System.exit(0);
      }       
    });
    setResizable(false);
    setSize(282, 294);
    setVisible(true);
    //
    String[] inp = getInputs(lst);
    if (inp == null) {
      JOptionPane.showMessageDialog(this,"Dialog was canceled.");
      System.exit(0);
    }
    try {
      but.setEnabled(true);
      int idx = lst.indexOf(inp[0]);
      lab.setText("WebcamService @"+inp[1]+":"+Integer.parseInt(inp[2]));
      //
      service = new WebcamService(dlist.get(idx),
                                 new Dimension(640, 480),
                                 inp[1],
                                 Integer.parseInt(inp[2])
                                );
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, ex.toString());
      System.exit(0);
    }
    ForkJoinPool.commonPool().execute(() -> {
      Graphics g = canvas.getGraphics();
      BufferedImage image, img;
      while (running) {
        img = new BufferedImage(wi, he, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        g2.setBackground(java.awt.Color.WHITE);
        g2.setPaint(java.awt.Color.BLACK);
        g2.fillRect(0, 0, wi, he);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(service.getImage(), 0, 0, wi, he, null);
        g2.dispose();
        //
        g.drawImage(img, 0, 0, null);
        try {
          TimeUnit.MILLISECONDS.sleep(10);
        } catch (Exception e) { }
      }
    });
  }
  @SuppressWarnings({"unchecked", "deprecation"})
  private String[] getInputs(java.util.ArrayList<String> lst) {
    GridBagLayout layout = new GridBagLayout();
    GridBagConstraints left = new GridBagConstraints();
    left.anchor = GridBagConstraints.EAST;
    GridBagConstraints right = new GridBagConstraints();
    right.anchor = GridBagConstraints.WEST;
    right.gridwidth = GridBagConstraints.REMAINDER;
    
    JPanel all = new JPanel(layout);
    JOptionPane jop = new JOptionPane(all);
    JDialog dia = jop.createDialog(this, "WebcamServer");
    //
    JLabel jlab0 = new JLabel("Device ");
    layout.setConstraints(jlab0, left);
    all.add(jlab0);
    JComboBox<Object> combo = new JComboBox<Object>(lst.toArray());
    combo.setSelectedItem(0);
    layout.setConstraints(combo, right);    
    all.add(combo);
    
    JLabel jlab1 = new JLabel("Host name or IP ");
    layout.setConstraints(jlab1, left);
    all.add(jlab1);
    JTextField thost = new JTextField(16);
    thost.setText("localhost");
    layout.setConstraints(thost, right);
    all.add(thost);
    
    JLabel jlab2 = new JLabel("Port ");
    layout.setConstraints(jlab2, left);
    all.add(jlab2);
    JTextField tport = new JTextField(6);
    tport.setText("22222");
    tport.addActionListener(a -> {
      try {
        Integer.parseInt(tport.getText().trim());
      } catch (Exception ex) {
        tport.setText("22222");
      }
    });
    layout.setConstraints(tport, right);    
    all.add(tport);
    //
    dia.pack();
    dia.setVisible(true);
    dia.dispose();
    // close clicked?
    if (jop.getValue() != null)
      return new String[] { 
                           (String)combo.getSelectedItem(),
                           thost.getText().trim(),
                           tport.getText().trim(),
                          };
     return null;                    
  }
  private volatile boolean running = true;
  private WebcamService service = null;
}