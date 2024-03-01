package com.sanderjurgens.volvis.gui;

import com.sanderjurgens.volvis.visualization.AverageRenderer;
import com.sanderjurgens.volvis.visualization.MarchingCubesRenderer;
import com.sanderjurgens.volvis.visualization.Renderer;
import com.sanderjurgens.volvis.visualization.CenterRenderer;
import com.sanderjurgens.volvis.visualization.IsovalueSurfacesRenderer;
import com.sanderjurgens.volvis.visualization.MaximumRenderer;
import com.sanderjurgens.volvis.visualization.TransferFunctionRenderer;
import com.sanderjurgens.volvis.visualization.Visualization;
import com.sanderjurgens.volvis.volume.Volume;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * The main GUI interface.
 *
 * @author sanderjurgens
 * @version 1.1
 */
public class MainFrame extends javax.swing.JFrame {

    /**
     * The volume that is currently loaded
     */
    private Volume volume;

    /**
     * The component in charge of updating the visualization
     */
    private final Visualization visualization;

    /**
     * The tab containing the rendering settings
     */
    private final RenderingTab renderingTab;

    /**
     * The tab containing color settings for transfer function renderers
     */
    private final TransferFunctionTab transferTab;
    
    /**
     * The list of available volume renderers
     */
    private final ArrayList<Renderer> renderers = new ArrayList<>();

    /**
     * Constructs a new MainFrame.
     */
    public MainFrame() {
        initComponents();
        
        // Create the rendering options tab
        renderingTab = new RenderingTab();
        tabbedPanel.addTab("Rendering", renderingTab);
        
        // Create the histogram colors options tab
        transferTab = new TransferFunctionTab();            
        tabbedPanel.addTab("Transfer Function", transferTab);
        
        // Create a new visualization for the OpenGL panel
        visualization = new Visualization(glCanvas, renderingTab);
        glCanvas.addGLEventListener(visualization);
        glCanvas.setMinimumSize(new java.awt.Dimension(300, 600));
        glCanvas.setPreferredSize(new java.awt.Dimension(500, 600));
        
        // Create new volume renders
        renderers.add(new CenterRenderer("RC - Center", renderingTab));
        renderers.add(new MaximumRenderer("RC - Maximum", renderingTab));
        renderers.add(new AverageRenderer("RC - Average", renderingTab));
        renderers.add(new TransferFunctionRenderer("RC - Transfer Function", renderingTab, transferTab));
        renderers.add(new IsovalueSurfacesRenderer("RC - Isovalue Surfaces", renderingTab, transferTab));
        renderers.add(new MarchingCubesRenderer("Marching Cubes", renderingTab));
        
        // Add all available renderers to the visualization and rendering options
        for (Renderer r : renderers) {
            renderingTab.addRenderer(r);
            visualization.addRenderer(r);
        }
        
        // Automatically try to load the "pig8" volume
        try {
            loadResource(getClass().getResource("pig8.fld"));
        } catch (IOException e) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, e);
        }

        // Set the application to be fullscreen, and stop renderers on close
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                for (Renderer r : renderers) {
                    r.stop();
                }
            }
        });
    }

    /**
     * Load a resource containing a volume, completely discards the current
     * loaded volume (if any).
     *
     * @param url the uniform locator to the resource containing the volume
     * @throws IOException if an error occurs while reading the resource
     */
    private void loadResource(URL url) throws IOException {
        // Load selected volume file and rotate it to move origin in line with graphics coordinate system (RHS)
        volume = new Volume(url.openStream());
        volume.rotate();

        // Show volume info in IO options tab
        String path = url.getPath();
        String infoText = "Volume data info:\n";
        infoText = infoText.concat("filename:\t\t" + path.substring(path.lastIndexOf('/') + 1, path.length()) + "\n");
        infoText = infoText.concat("dimensions:\t\t" + volume.getDimX() + " x " + volume.getDimY() + " x " + volume.getDimZ() + "\n");
        infoText = infoText.concat("voxel value range:\t" + volume.getMinimum() + " - " + volume.getMaximum());
        infoTextPane.setText(infoText);

        // Set-up visualization of volume
        renderingTab.setVolume(volume);
        transferTab.setVolume(volume);
        visualization.setVolume(volume);
        visualization.update();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        splitPane = new javax.swing.JSplitPane();
        tabbedPanel = new javax.swing.JTabbedPane();
        loadVolume = new javax.swing.JPanel();
        loadButtonPanel = new javax.swing.JPanel();
        loadButton = new javax.swing.JButton();
        infoScrollPane = new javax.swing.JScrollPane();
        infoTextPane = new javax.swing.JTextPane();
        visPanel = new javax.swing.JPanel();
        glCanvas = new com.jogamp.opengl.awt.GLCanvas();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("VolVis");
        setMinimumSize(new java.awt.Dimension(800, 600));
        setPreferredSize(new java.awt.Dimension(800, 600));

        splitPane.setResizeWeight(0.9);
        splitPane.setMinimumSize(new java.awt.Dimension(800, 600));
        splitPane.setPreferredSize(new java.awt.Dimension(800, 600));

        tabbedPanel.setMinimumSize(new java.awt.Dimension(300, 600));
        tabbedPanel.setPreferredSize(new java.awt.Dimension(300, 600));

        loadVolume.setMinimumSize(new java.awt.Dimension(300, 600));
        loadVolume.setPreferredSize(new java.awt.Dimension(300, 600));
        loadVolume.setLayout(new java.awt.BorderLayout());

        loadButtonPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        loadButtonPanel.setMinimumSize(new java.awt.Dimension(300, 50));
        loadButtonPanel.setPreferredSize(new java.awt.Dimension(300, 50));
        loadButtonPanel.setLayout(new java.awt.BorderLayout());

        loadButton.setText("Load volume");
        loadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadButtonActionPerformed(evt);
            }
        });
        loadButtonPanel.add(loadButton, java.awt.BorderLayout.WEST);

        loadVolume.add(loadButtonPanel, java.awt.BorderLayout.NORTH);

        infoScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 10, 10));

        infoTextPane.setEditable(false);
        infoScrollPane.setViewportView(infoTextPane);

        loadVolume.add(infoScrollPane, java.awt.BorderLayout.CENTER);

        tabbedPanel.addTab("Load", loadVolume);

        splitPane.setRightComponent(tabbedPanel);

        visPanel.setMinimumSize(new java.awt.Dimension(300, 600));
        visPanel.setPreferredSize(new java.awt.Dimension(500, 600));
        visPanel.setLayout(new java.awt.BorderLayout());
        visPanel.add(glCanvas, java.awt.BorderLayout.CENTER);

        splitPane.setLeftComponent(visPanel);

        getContentPane().add(splitPane, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Open volume from file, using a file chooser dialog.
     * 
     * @param evt the load button event
     */
    private void loadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadButtonActionPerformed
        // Open a modal file dialog
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        // Set file filter to only show accepted file types (and directories)
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter(new FileNameExtensionFilter("", "fld"));

        // Try to load the file into the program
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            // Do not accept a directory as a file
            if (chooser.getSelectedFile().isDirectory()) {
                return;
            }
            try {
                loadResource(chooser.getSelectedFile().toURI().toURL());
            } catch (IOException e) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }//GEN-LAST:event_loadButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    /** Canvas on which to draw the visualization */
    private com.jogamp.opengl.awt.GLCanvas glCanvas;
    /** Scrolling pane that contains the volume information panel */
    private javax.swing.JScrollPane infoScrollPane;
    /** Panel containing the volume information */
    private javax.swing.JTextPane infoTextPane;
    /** Button to lead a new volume */
    private javax.swing.JButton loadButton;
    /** Panel containing the load volume button */
    private javax.swing.JPanel loadButtonPanel;
    /** Setting tab for volume loading */
    private javax.swing.JPanel loadVolume;
    /** Main panel that splits the window between visualization and settings */
    private javax.swing.JSplitPane splitPane;
    /** Menu containing all setting tabs */
    private javax.swing.JTabbedPane tabbedPanel;
    /** Panel containing the visualization canvas */
    private javax.swing.JPanel visPanel;
    // End of variables declaration//GEN-END:variables
}
