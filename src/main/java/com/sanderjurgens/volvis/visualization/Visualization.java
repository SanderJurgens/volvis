package com.sanderjurgens.volvis.visualization;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.sanderjurgens.volvis.gui.RenderingTab;
import com.sanderjurgens.volvis.util.TrackballCamera;
import com.sanderjurgens.volvis.volume.Volume;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class in charge of updating the visualization, partially based on the
 * selected renderer.
 *
 * @author sanderjurgens
 */
public final class Visualization implements GLEventListener {

    /**
     * The canvas on which to draw the visualization
     */
    private final GLCanvas canvas;

    /**
     * The width and height of the canvas
     */
    private int width, height;

    /**
     * The tab containing the rendering settings
     */
    private final RenderingTab renderingTab;

    /**
     * The list of available renderers to visualize the volume
     */
    private final ArrayList<Renderer> renderers = new ArrayList<>();

    /**
     * The volume that is currently loaded
     */
    private Volume volume = null;

    /**
     * The camera determining the view matrix position
     */
    private final TrackballCamera trackball;

    /**
     * The field of view when looking from the camera position
     */
    private double fov = 20.0;

    /**
     * Additional graphical interface (OpenGL) utilities
     */
    private final GLU glu = new GLU();

    /**
     * The animator attempting to achieve a target frames-per-second
     */
    private final FPSAnimator animator;

    /**
     * Constructs a visualization to display the volume on the canvas.
     *
     * @param cv the canvas on which to display the volume
     * @param rt the tab in which the selected renderer is chosen
     */
    public Visualization(GLCanvas cv, RenderingTab rt) {
        // Assign canvas and add mouse events
        canvas = cv;
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                canvasMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                canvasMouseReleased(e);
            }
        });
        canvas.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                canvasMouseDragged(e);
            }
        });
        canvas.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                canvasMouseWheelMoved(e);
            }
        });

        // Assign rendering settings tab and set-up camera
        renderingTab = rt;
        trackball = new TrackballCamera(width, height);

        // Set animator to achieve a target frames-per-second
        animator = new FPSAnimator(canvas, 144);
        animator.start();
    }

    /**
     * Set the given volume as the one to be rendered.
     *
     * @param volume the given volume
     */
    public void setVolume(Volume volume) {
        this.volume = volume;
        for (Renderer r : renderers) {
            r.setVolume(volume);
        }
    }

    /**
     * Add the given renderer to the visualization.
     *
     * @param renderer the given renderer
     */
    public void addRenderer(Renderer renderer) {
        renderers.add(renderer);
    }

    /**
     * Update the canvas by repainting it.
     */
    public void update() {
        canvas.repaint(10);
    }

    /**
     * Initialize the graphical rendering context.
     *
     * @param drawable the graphical rendering context in which to draw
     */
    @Override
    public void init(GLAutoDrawable drawable) {
        // Empty
    }

    /**
     * Render the graphical context.
     *
     * @param drawable the graphical rendering context in which to draw
     */
    @Override
    public void display(GLAutoDrawable drawable) {
        // Only visualize if a volume is loaded
        if (volume == null) {
            return;
        }
        
        // Get the graphics interface
        GL2 gl = (GL2) drawable.getGL();

        // Set camera perspective and move camera back from origin
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(fov, ((float) width / ((float) height)), 
                volume.getDiagonal(), volume.getDiagonal() * 5);
        gl.glTranslated(0, 0, volume.getDiagonal()* -2.5);
        
        // Clear color and depth buffer settings
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        
        // Set drawing options to perform depth comparison and alpha blending
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LESS);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        
        // Rotate view towards position of trackball camera
        trackball.update(gl);
        gl.glMultMatrixd(trackball.getTransformationMatrix(), 0);      

        // Draw the bounding box for the given volume
        drawBoundingBox(gl);
        
        try {
            // Render the volume using the selected renderer, stop other renderers
            for (int i = 0; i < renderers.size(); i++) { 
                if (i == renderingTab.getSelectedRenderer()) {
                    renderers.get(i).render(gl);
                } else {
                    renderers.get(i).stop();
                }
            }
            gl.glFlush();
            
            // Throw possible graphical error that was encountered while rendering
            if (gl.glGetError() != GL2.GL_NO_ERROR) {
                throw new Exception("OpenGL error: " + gl.glGetError());
            }            
        } catch (Exception e) {
            Logger.getLogger(Visualization.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Reshape of the canvas on which the graphical rendering is rendered.
     * 
     * @param drawable he graphical rendering context in which to draw
     * @param x the x coordinate of the canvas (relative to the window)
     * @param y the y coordinate of the canvas (relative to the window)
     * @param width the width of the canvas
     * @param height the height of the canvas
     */
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        // Assign the global variables
        this.width = width;
        this.height = height;
        
        // Get the graphics interface and adjust its viewport
        GL2 gl = (GL2) drawable.getGL();
        gl.glViewport(0, 0, width, height);

        // Set the dimensions of view in the trackball camera
        trackball.setDimensions(width, height);
    }

    /**
     * Dispose of the graphical rendering context.
     *
     * @param drawable the graphical rendering context in which to draw
     */
    @Override
    public void dispose(GLAutoDrawable drawable) {
        // Empty
    }

    /**
     * Draw a bounding box.
     *
     * @param gl the graphics interface
     */
    private void drawBoundingBox(GL2 gl) {
        // Store current lighting, line and color settings
        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glPushAttrib(GL2.GL_LINE_BIT);
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);

        // Set visualization settings
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        gl.glLineWidth(1.5f);
        gl.glColor4d(1.0, 1.0, 1.0, 1.0);        

        // Left face
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        // Right face
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        // Bottom face
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        // Top face
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        // Rear face
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        // Front face
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glEnd();

        // Reset graphics settings
        gl.glPopAttrib();
        gl.glPopAttrib();
        gl.glPopAttrib();
    }

    /**
     * Start dragging the camera when the mouse is pressed.
     *
     * @param evt mouse pressed event
     */
    private void canvasMousePressed(MouseEvent e) {
        trackball.startDragging(e.getX(), e.getY());
        
        // Set renderers to interactive mode
        for (int i = 0; i < renderers.size(); i++) {
            renderers.get(i).setInteractiveMode(true);
        }
    }

    /**
     * Stop rotating the camera when the mouse is released.
     *
     * @param evt mouse released event
     */
    private void canvasMouseReleased(MouseEvent e) {
        trackball.stopRotating();
        
        // Set renderers back to display mode
        for (int i = 0; i < renderers.size(); i++) {
            renderers.get(i).setInteractiveMode(false);
        }        
    }

    /**
     * Move the camera when the mouse is dragged.
     *
     * @param evt mouse dragged event
     */
    private void canvasMouseDragged(MouseEvent e) {
        trackball.drag(e.getX(), e.getY());
    }

    /**
     * Update field of view when moving scroll wheel.
     *
     * @param evt mouse wheel moved event
     */
    private void canvasMouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() < 0) {
            // Decrease field of view when scrolling up
            fov = Math.max(fov - 1, 5);
        } else if (e.getWheelRotation() > 0) {
            // Increase field of view when scrolling down
            fov = Math.min(fov + 1, 50);
        }
    }
}
