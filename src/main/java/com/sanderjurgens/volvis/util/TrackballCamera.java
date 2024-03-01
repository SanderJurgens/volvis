package com.sanderjurgens.volvis.util;

import com.jogamp.opengl.GL2;

/**
 * Support class for trackball camera rotation.
 *
 * @author sanderjurgens
 */
public class TrackballCamera {

    /**
     * The dimensions of the view
     */
    private int width, height;

    /**
     * The view transformation matrix (column-major)
     */
    private final double[] transformation = new double[16];

    /**
     * The mouse sensitivity, a scaling factor for mouse movement
     */
    private final double sens = 1;
    
    /**
     * Whether the camera is rotating
     */
    private boolean rotating = false;
    
    /**
     * The angle of rotation, in degrees
     */
    private double angle;
    
    /**
     * The 3D vector to rotate around
     */
    private final double[] axis = new double[3];
    
    /**
     * The last known virtual position of the camera
     */
    private final double[] lastPos = new double[3];

    /**
     * Constructs a trackball camera given certain view dimensions.
     *
     * @param width the given view width
     * @param height the given view height
     */
    public TrackballCamera(int width, int height) {
        this.width = width;
        this.height = height;

        // Set view transformation matrix to be the identity matrix
        transformation[0] = 1.0;
        transformation[5] = 1.0;
        transformation[10] = 1.0;
        transformation[15] = 1.0;
    }

    /**
     * Returns the view transformation matrix.
     *
     * @return the view transformation matrix
     */
    public double[] getTransformationMatrix() {
        return transformation;
    }

    /**
     * Set the dimensions of the view to the given values.
     *
     * @param width the given view width
     * @param height the given view height
     */
    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Transform Physical mouse coordinates to a Virtual position.
     *
     * @param x the mouse x coordinate
     * @param y the mouse y coordinate
     * @param width the view width
     * @param height the view height
     * @param v the variable in which to store the resulting vector
     */
    private void ptov(int x, int y, int width, int height, double v[]) {
        double d, a;
        
        // project x,y onto a hemi-sphere centered within width, height 
        double radius = Math.min(width, height) - 20;
        v[0] = (2.0 * x - width) / radius;
        v[1] = (height - 2.0 * y) / radius;
        d = Math.sqrt(v[0] * v[0] + v[1] * v[1]);
        v[2] = Math.cos((Math.PI / 2.0) * ((d < 1.0) ? d : 1.0));
        a = 1.0 / Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] *= a;
        v[1] *= a;
        v[2] *= a;
    }
    
    /**
     * Start dragging the camera from the given mouse coordinates.
     *
     * @param x the given x coordinate
     * @param y the given y coordinate
     */
    public void startDragging(int x, int y) {
        ptov(x, y, width, height, lastPos);
    }
    
    /**
     * Update camera position based on new mouse coordinates.
     *
     * @param mx the new mouse x coordinate
     * @param my the new mouse y coordinate
     */
    public void drag(int mx, int my) {        
        // Get current virtual position based on new mouse coordinates
        double[] currPos = new double[3];
        ptov(mx, my, width, height, currPos);
        
        // Compute difference with old position
        double dx = currPos[0] - lastPos[0];
        double dy = currPos[1] - lastPos[1];
        double dz = currPos[2] - lastPos[2];        
        
        // Only compute transformation if the virtual position has changed
        if ((dx != 0) || (dy != 0) || (dz != 0)) {
            rotating = true;
            angle = sens * 90.0 * Math.sqrt(dx * dx + dy * dy + dz * dz);
            
            axis[0] = lastPos[1] * currPos[2] - lastPos[2] * currPos[1];
            axis[1] = lastPos[2] * currPos[0] - lastPos[0] * currPos[2];
            axis[2] = lastPos[0] * currPos[1] - lastPos[1] * currPos[0];
            
            lastPos[0] = currPos[0];
            lastPos[1] = currPos[1];
            lastPos[2] = currPos[2];
        }
    }
    
    /**
     * Returns whether the camera is currently rotating.
     *
     * @return whether the camera is currently rotating
     */
    public boolean isRotating() {
        return rotating;
    }

    /**
     * Stop the current camera rotation.
     */
    public void stopRotating() {
        rotating = false;
    }
    
    /**
     * Update view transformation matrix based on the camera rotation.
     * 
     * @param gl the graphics interface
     */
    public void update(GL2 gl) {
        // Only update when rotating
        if(isRotating()) {
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glRotated(angle, axis[0], axis[1], axis[2]);
            gl.glMultMatrixd(transformation, 0);
            gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, transformation, 0);
            gl.glPopMatrix();
            
            // Automatically stop rotation
            stopRotating();
        }
    }
}
