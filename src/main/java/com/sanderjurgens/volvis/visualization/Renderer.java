package com.sanderjurgens.volvis.visualization;

import com.jogamp.opengl.GL2;
import com.sanderjurgens.volvis.volume.Volume;

/**
 * Renderer interface.
 *
 * @author sanderjurgens
 */
public interface Renderer {
    
    /**
     * Returns the name of the renderer.
     * 
     * @return the name of the renderer
     */
    public String getName();
    
    /**
     * Set the given volume as the one to be rendered.
     *
     * @param volume the given volume
     */
    public void setVolume(Volume volume);
    
    /**
     * Set whether the user is interacting with the volume through the canvas.
     * 
     * @param interacting whether the user is interacting with the volume
     */
    public void setInteractiveMode(boolean interacting);
    
    /**
     * Render the volume using the given graphical interface.
     * 
     * @param gl the given graphical interface
     * @throws Exception if an error occurs while rendering
     */
    public void render(GL2 gl) throws Exception;
    
    /**
     * Stop rendering the volume, if still relevant.
     */
    public void stop();
}
