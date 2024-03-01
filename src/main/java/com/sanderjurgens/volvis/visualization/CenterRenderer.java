package com.sanderjurgens.volvis.visualization;

import com.jogamp.opengl.GL2;
import com.sanderjurgens.volvis.gui.RenderingTab;
import com.sanderjurgens.volvis.volume.Volume;

/**
 * A renderer that uses ray casting to compute a centered slice of the volume.
 *
 * @author sanderjurgens
 */
public class CenterRenderer extends RaycastingRenderer {

    /**
     * Constructs a renderer to visualize the volume based on ray casting to
     * compute a centered slice of the volume.
     *
     * @param name the name of the renderer
     * @param renderingTab the tab containing the rendering settings
     */
    public CenterRenderer(String name, RenderingTab renderingTab) {
        super(name, renderingTab);
    }
    
    /**
     * Set the given volume as the one to be rendered.
     *
     * @param volume the given volume
     */
    @Override
    public void setVolume(Volume volume) {
        stopSlicer();
        super.setVolume(volume);
        startSlicer(this, getRenderingTab().getNumThreads());
    }

    /**
     * Render the volume using the given graphical interface.
     *
     * @param gl the given graphical interface
     * @throws Exception if an error occurs while rendering
     */
    @Override
    public void render(GL2 gl) throws Exception {
        // Stop slicing with old amount of threads if new amount is selected
        int num = getRenderingTab().getNumThreads();
        if (num != getThreadCount()) {
            stopSlicer();            
        }
        
        // Start slicing, if not doing so already
        startSlicer(this, num);
        
        // Continue with regular ray casting render
        super.render(gl);
    }
    
    /**
     * Stop rendering the volume, if still relevant.
     */
    @Override
    public void stop() {
        stopSlicer();
    }
    
    /**
     * Produce a slice of the 2D image using a specific ray casting algorithm,
     * from a given starting height up to and including a given ending height
     * based on the number of compute threads.
     *
     * @param startHeight the given start height
     * @param endHeight the given end height
     * @return whether the slicing was completed successfully
     */
    @Override
    public boolean slice(int startHeight, int endHeight) {       
        // Vectors defining a plane through the origin, perpendicular to view vector
        double[] viewMat = getViewMatrix();
        double[] uVec = new double[]{viewMat[0], viewMat[4], viewMat[8]};
        double[] vVec = new double[]{viewMat[1], viewMat[5], viewMat[9]};
        
        // Only render the central slice of the volume
        double[] volumeCenter = new double[]{
            getVolume().getDimX() / 2, 
            getVolume().getDimY() / 2, 
            getVolume().getDimZ() / 2
        };

        // Center of resulting 2D image, in both directions since it's a square
        int imageCenter = getImage().getWidth() / 2;

        // Values used for scaling image
        int res = getRenderingTab().getRayWidth();
        double max = getVolume().getMaximum();

        // Cast ray for each pixel in the slice
        double[] volumeCoord = new double[3];
        for (int j = startHeight; j <= endHeight; j = j + res) {            
            for (int i = 0; i < getImage().getWidth(); i = i + res) {
                // Check if we should still be slicing
                if (!isComputingSlices() || shouldRestart()) {
                    return false;
                }
                
                // Find middle of pixel (with larger resolution)
                double mx = i + ((Math.min(i + res - 1, getImage().getWidth() - 1) - i) / 2);
                double my = j + ((Math.min(j + res - 1, endHeight) - j) / 2);

                // Transform ray into volume coordinate in central slice of volume
                volumeCoord[0] = uVec[0] * (mx - imageCenter) + vVec[0] * (my - imageCenter) + volumeCenter[0];
                volumeCoord[1] = uVec[1] * (mx - imageCenter) + vVec[1] * (my - imageCenter) + volumeCenter[1];
                volumeCoord[2] = uVec[2] * (mx - imageCenter) + vVec[2] * (my - imageCenter) + volumeCenter[2];

                // Retrieve value in 256 bit scale
                int val = getTriVoxel(volumeCoord);
                val = (int) Math.floor((val / max) * 255);

                // Set up RGB, make pixel opaque if relevant
                int red = 0, green = 0, blue = 0, alpha = 0;
                if (val > 0) {
                    alpha = 255;
                }

                // Pack monochrome color channels into integer
                red = green = blue = val;
                val = (alpha << 24) | (red << 16) | (green << 8) | blue;

                // Set color of all pixels in the chosen resolution
                for (int k = 0; k < (res * res); k++) {
                    // Check if we should still be slicing
                    if (!isComputingSlices() || shouldRestart()) {
                        return false;
                    }
                    
                    // Color slice from bottom left corner
                    int x = i + (k % res);
                    int y = j + (k / res);
                    if (x < getImage().getWidth() && y <= endHeight) {                        
                        getImage().setRGB(x, (getImage().getHeight()-1) - y, val);
                    }
                }
            }
        }
        
        // Slice computed succesfully
        return true;
    }
}
