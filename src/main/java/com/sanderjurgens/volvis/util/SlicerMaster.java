package com.sanderjurgens.volvis.util;

import com.sanderjurgens.volvis.visualization.RaycastingRenderer;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Master of a set of slave threads, each computing a slice of a rendering
 * image.
 *
 * @author sanderjurgens
 */
public class SlicerMaster implements Runnable {

    /**
     * The renderer whose result is computed in slices
     */
    private final RaycastingRenderer rcr;

    /**
     * The slaves (threads) computing slices of the rendering image
     */
    private final Thread[] slaves;

    /**
     * The number of slaves (threads)
     */
    private final int numSlaves;

    /**
     * The semaphore controlling whether slaves may start computing
     */
    private final Semaphore semaphore;

    /**
     * Whether the task of each slaves was completed successfully
     */
    private boolean[] completed;
    
    /**
     * Constructs the master for a set of slice computing slaves.
     *
     * @param rcr the ray casting renderer
     * @param numSlaves the number of computing slaves to start
     */
    public SlicerMaster(RaycastingRenderer rcr, int numSlaves) {
        this.rcr = rcr;
        this.numSlaves = numSlaves;

        // Set up semaphore and acquire all initially
        semaphore = new Semaphore(numSlaves, true);
        try {
            semaphore.acquire(numSlaves);
        } catch (InterruptedException e) {
            Logger.getLogger(SlicerMaster.class.getName()).log(Level.SEVERE, null, e);
        }        

        // Create threads to compute each slice
        slaves = new Thread[numSlaves];
        int dh = (int) Math.ceil(rcr.getImage().getHeight() / numSlaves);
        for (int i = 0; i < numSlaves; i++) {
            slaves[i] = new Thread(new SlicerSlave(this.rcr, this, i, 
                    i * dh,
                    (int) Math.min(((i + 1) * dh) - 1, rcr.getImage().getHeight() - 1)
            ));
            slaves[i].setDaemon(true);
            slaves[i].start();
        }
    }
    
    /**
     * Returns the semaphore controlling whether slaves may start computing.
     * 
     * @return the semaphore controlling whether slaves may start computing
     */
    protected Semaphore getSemaphore() {
        return semaphore;
    }
    
    /**
     * Sets the completed state of a given slave's task to given value.
     * 
     * @param identifier a given slave
     * @param flag the given value of completeness
     */
    protected void setCompleted(int identifier, boolean flag) {
        if (0 <= identifier && identifier < completed.length) {
            completed[identifier] = flag;
        }
    }

    /**
     * The routine of the master, makes slaves compute slices if new image is
     * required by the renderer.
     */
    @Override
    public void run() {
        // Check if master should keep on running
        while (rcr.isSlicerRunning()) {
            
            // Skip computing slices if no new image is required
            try {
                if (!rcr.isComputingSlices()) {              
                    Thread.sleep(10);
                    continue;
                }       
            }
            catch (InterruptedException e) {
                continue;
            }
            
            // Notify renderer that computation of slices has (re)started
            rcr.restarted();
            
            // Clear old image 
            for (int i = 0; i < rcr.getImage().getWidth(); i++) {
                for (int j = 0; j < rcr.getImage().getHeight(); j++) {
                    rcr.getImage().setRGB(i, j, 0);
                }
            }
            
            // Release all semaphores, wait until all slaves started on their task
            completed = new boolean[numSlaves];
            semaphore.release(numSlaves);
            try {
                while (semaphore.availablePermits() > 0) {                
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                continue;
            }
            
            // Wait for all slaves to finish their task
            try {
                for (int i = 0; i < numSlaves; i++) {                
                    semaphore.acquire();                
                }
            } catch (InterruptedException e) {
                continue;
            }

            // Check whether all slices were computed successfully
            boolean allCompleted = true;
            for (int i = 0; i < completed.length; i++) {
                if (!completed[i]) {
                    allCompleted = false;
                    break;
                }
            }
            
            // If all slices were computed successfully, stop updating the rendered image
            if (allCompleted) {
                rcr.computeSlices(false);
            }
        }
        
        // Stop computing slices
        for (Thread slave : slaves) {
            slave.interrupt();
        }
    }
}
