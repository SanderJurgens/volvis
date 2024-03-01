package com.sanderjurgens.volvis.visualization;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.sanderjurgens.volvis.gui.RenderingTab;
import com.sanderjurgens.volvis.util.SlicerMaster;
import com.sanderjurgens.volvis.volume.Volume;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * Abstract renderer based on a ray casting algorithm to render a 2D texture of
 * the volume.
 *
 * @author sanderjurgens
 */
public abstract class RaycastingRenderer implements Renderer {

    /**
     * The name of the renderer
     */
    private final String name;

    /**
     * The volume to be rendered
     */
    private Volume volume;

    /**
     * The tab containing the rendering settings
     */
    private final RenderingTab renderingTab;

    /**
     * The current view matrix of the visualization
     */
    private double[] viewMatrix = new double[4 * 4];

    /**
     * The current ray step size
     */
    private int stepSize;

    /**
     * The current image pixel resolution, determining the width of a ray
     */
    private int resolution;

    /**
     * The rendered 2D image of the volume
     */
    private BufferedImage image;

    /**
     * The thread in charge of computing the image from a number of slices, each
     * slice is done by one of its slaves
     */
    private Thread slicer;

    /**
     * The current number of slaves computing slices
     */
    private int threadCount = 0;

    /**
     * Whether the slicer thread should currently be running
     */
    private boolean slicerRunning = false;

    /**
     * Whether a new image is currently being computed from its slices
     */
    private boolean computingSlices = false;
    
    /**
     * Whether the computing of slices should be restarted
     */
    private boolean restartCompute = false;

    /**
     * Constructs a renderer to visualize the volume based on a ray casting
     * algorithm to render a 2D image of the volume.
     *
     * @param name the name of the renderer
     * @param renderingTab the tab containing the rendering settings
     */
    public RaycastingRenderer(String name, RenderingTab renderingTab) {
        this.renderingTab = renderingTab;
        this.name = name;
    }

    /**
     * Returns the name of the renderer.
     *
     * @return the name of the renderer
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the volume to be rendered.
     *
     * @return the volume to be rendered
     */
    protected Volume getVolume() {
        return volume;
    }

    /**
     * Set the given volume as the one to be rendered.
     *
     * @param volume the given volume
     */
    @Override
    public void setVolume(Volume volume) {
        this.volume = volume;

        // Determine the size that will fit each orientation of the rendered 2D image
        int size = (int) Math.floor(volume.getDiagonal());
        size = (size % 2 == 0) ? size : size + 1;
        image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        
        // Allow slicer to recompute slices of the rendered image
        computeSlices(true);
    }

    /**
     * Returns the value at the given coordinate by trilinear interpolation of
     * nearby voxels.
     *
     * @param coord the given coordinate
     * @return the trilinear interpolated value at the given coordinate
     */
    protected short getTriVoxel(double[] coord) {
        // Compute rounded values of the given coordinates
        int[][] rounded = new int[3][2];
        for (int i = 0; i < 3; i++) {
            rounded[i][0] = (int) Math.floor(coord[i]);
            rounded[i][1] = (int) Math.ceil(coord[i]);
        }

        // Return if coordinate is not within volume
        if (rounded[0][0] < 0 || volume.getDimX() <= rounded[0][1]
                || rounded[1][0] < 0 || volume.getDimY() <= rounded[1][1]
                || rounded[2][0] < 0 || volume.getDimZ() <= rounded[2][1]) {
            return 0;
        }

        // Get alpha, beta and gamma for trilinear interpolation
        double alpha = coord[0] - rounded[0][0];
        double beta = coord[1] - rounded[1][0];
        double gamma = coord[2] - rounded[2][0];

        // Perform trilinear interpolation
        double result = 0;
        for (int i = 0; i < 8; i++) {
            // Get value from corner of surrounding cube and add to total
            double value = volume.getVoxel(rounded[0][i % 2], rounded[1][(i / 2) % 2], rounded[2][i / 4]);
            value = value * ((i % 2) == 0 ? (1 - alpha) : alpha);
            value = value * (((i / 2) % 2) == 0 ? (1 - beta) : beta);
            value = value * ((i / 4) == 0 ? (1 - gamma) : gamma);
            result = result + value;
        }

        // Return the rounded result
        return (short) Math.round(result);
    }
    
    /**
     * Calculate the approximated gradient at the given coordinate.
     * 
     * @param coord the given coordinate
     * @return the approximated gradient at the given coordinate
     */
    protected double[] gradient(double[] coord) {
        // Round to closest integer coordinate
        int x = (int) Math.round(coord[0]);
        int y = (int) Math.round(coord[1]);
        int z = (int) Math.round(coord[2]);
        
        // Return if coordinate is not within volume
        if (x < 0 || volume.getDimX() <= x
                || y < 0 || volume.getDimY() <= y
                || z < 0 || volume.getDimZ() <= z) {
            return new double[]{0.0, 0.0, 0.0};
        }
        
        // Calculate gradient
        double[] gradient = new double[3];
        gradient[0] = 0.5 * (volume.getVoxel(Math.min(x + 1, volume.getDimX() - 1), y, z) - 
                volume.getVoxel(Math.max(x - 1, 0), y, z));
        gradient[1] = 0.5 * (volume.getVoxel(x, Math.min(y + 1, volume.getDimY() - 1), z) - 
                volume.getVoxel(x, Math.max(y - 1, 0), z));
        gradient[2] = 0.5 * (volume.getVoxel(x, y, Math.min(z + 1, volume.getDimZ() - 1)) - 
                volume.getVoxel(x, y, Math.max(z - 1, 0)));        
        return gradient;
    }

    /**
     * Returns the tab containing the rendering settings.
     *
     * @return the tab containing the rendering settings
     */
    protected RenderingTab getRenderingTab() {
        return renderingTab;
    }

    /**
     * Returns the current view matrix of the visualization.
     *
     * @return the current view matrix of the visualization
     */
    protected double[] getViewMatrix() {
        return viewMatrix;
    }

    /**
     * Returns the rendered 2D image of the volume.
     *
     * @return the rendered 2D image of the volume
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Set whether the user is interacting with the volume through the canvas.
     *
     * @param interacting whether the user is interacting with the volume
     */
    @Override
    public void setInteractiveMode(boolean interacting) {
        // Guarantee that a new image is computed on interaction, no matter the view matrix
        computeSlices(true);
    }

    /**
     * Render the volume using the given graphical interface.
     *
     * @param gl the given graphical interface
     * @throws Exception if an error occurs while rendering
     */
    @Override
    public void render(GL2 gl) throws Exception {
        // Check the need the recompute the rendered image, if settings have changed
        boolean computeSlices = false;

        // Store viewmatrix for usage in slicer, and check for change
        double[] vm = new double[4 * 4];
        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, vm, 0);
        for (int i = 0; i < (4 * 4); i++) {
            if (viewMatrix[i] != vm[i]) {
                computeSlices = true;
                viewMatrix = vm;
                break;
            }
        }

        // Store current ray step size to check the need for a recompute of the image
        if (stepSize != renderingTab.getStepSize()) {
            computeSlices = true;
            stepSize = renderingTab.getStepSize();
        }

        // Store current ray width to check the need for a recompute of the image
        if (resolution != renderingTab.getRayWidth()) {
            computeSlices = true;
            resolution = renderingTab.getRayWidth();
        }

        // Compute new image if necessary
        if (computeSlices) {
            // Note: Only the slicer can set it to false, by finishing the computation.
            // Or alternatively this class can also do so by stopping the computation.
            computeSlices(true);
        }

        // Store and disable lighting settings
        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glDisable(GL2.GL_LIGHTING);

        // Create texture from computed buffered image
        Texture texture;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        texture = TextureIO.newTexture(is, false, null);

        // Draw rendered image as billboard texture
        texture.enable(gl);
        texture.bind(gl);
        double halfWidth = image.getWidth() / 2.0;
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glTexCoord2d(0.0, 0.0);
        gl.glVertex3d(-halfWidth, -halfWidth, 0.0);
        gl.glTexCoord2d(0.0, 1.0);
        gl.glVertex3d(-halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 1.0);
        gl.glVertex3d(halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 0.0);
        gl.glVertex3d(halfWidth, -halfWidth, 0.0);
        gl.glEnd();
        texture.disable(gl);
        texture.destroy(gl);
        gl.glPopMatrix();

        // Reset graphics settings
        gl.glPopAttrib();
    }

    /**
     * Stop rendering the volume, if still relevant.
     */
    @Override
    public abstract void stop();

    /**
     * Start the slicer and allow it to compute slices using an amount of slaves
     * equal to the given number of threads and the slice method of the given
     * renderer.
     *
     * @param rcr the given renderer, whose slice method should be used
     * @param numThreads the given number of threads
     */
    protected void startSlicer(RaycastingRenderer rcr, int numThreads) {
        // Only start new slicer if no other slicer is active
        if (!isSlicerRunning()) {
            setSlicerRunning(true);
            // Only start (re)computing slices on start-up if needed
            if ((threadCount != numThreads && isComputingSlices()) || shouldRestart()) {                
                computeSlices(true);                
            }
            threadCount = numThreads;
            slicer = new Thread(new SlicerMaster(rcr, numThreads));
            slicer.setDaemon(true);
            slicer.start();
        }
    }

    /**
     * Stop the currently running slicer, if any.
     */
    protected void stopSlicer() {
        if (isSlicerRunning()) {
            setSlicerRunning(false);
            // If we are still computing, set restart for next time we start slicer
            if (isComputingSlices()) {
                restartCompute = true;
            }
            computeSlices(false);
            
            // Small wait to let each slicing slave detect that it needs to stop
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            
            slicer.interrupt();

            // Small wait to let interrupt close down the threads
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Return the current number of slaves computing slices.
     *
     * @return the current number of slaves computing slices
     */
    protected int getThreadCount() {
        return threadCount;
    }

    /**
     * Returns whether the slicer is currently running.
     *
     * @return whether the slicer is currently running
     */
    public boolean isSlicerRunning() {
        return slicerRunning;
    }

    /**
     * Sets whether the slicer should currently be running.
     *
     * @param flag whether the slicer should currently be running
     */
    private synchronized void setSlicerRunning(boolean flag) {
        slicerRunning = flag;
    }

    /**
     * Returns whether the slicer is currently computing slices for a new image.
     *
     * @return whether the slicer is currently computing slices for a new image
     */
    public boolean isComputingSlices() {
        return computingSlices;
    }

    /**
     * Set whether the slicer should be computing slices for a new image.
     *
     * @param flag whether the slicer should be computing slices for a new image
     */
    public synchronized void computeSlices(boolean flag) {
        // Restart if we are already computing a slice
        if (computingSlices && flag) {
            restartCompute = true;
        }
        computingSlices = flag;
    }
    
    /**
     * Returns whether the computing of slices should be restarted.
     * 
     * @return whether the computing of slices should be restarted
     */
    public boolean shouldRestart() {
        return restartCompute;
    }
    
    /**
     * Notify that computation of slices has restarted.
     */
    public synchronized void restarted() {
        restartCompute = false;
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
    public abstract boolean slice(int startHeight, int endHeight);
}
