package com.sanderjurgens.volvis.util;

import com.sanderjurgens.volvis.visualization.RaycastingRenderer;

/**
 * A slave thread computing a slice of the rendering image.
 *
 * @author sanderjurgens
 */
public class SlicerSlave implements Runnable {

    /**
     * The renderer whose slice this slave is working on
     */
    private final RaycastingRenderer rcr;

    /**
     * The master controlling this slave
     */
    private final SlicerMaster master;

    /**
     * The index of this slave in the masters set of slaves
     */
    private final int identifier;

    /**
     * The start height of the computed slice
     */
    private final int startHeight;

    /**
     * The end height of the computed slice
     */
    private final int endHeight;

    /**
     * Constructs a slave which computes a slice for a given renderer, under the
     * control of a master slicer.
     *
     * @param rcr the renderer whose slice this slave is computing
     * @param master the master controlling this slave
     * @param identifier the given index of this slave
     * @param startHeight the given start height of the slice
     * @param endHeight the given end height of the slice
     */
    public SlicerSlave(RaycastingRenderer rcr, SlicerMaster master, int identifier, int startHeight, int endHeight) {
        this.rcr = rcr;
        this.master = master;
        this.identifier = identifier;
        this.startHeight = startHeight;
        this.endHeight = endHeight;

    }

    /**
     * The routine of the slave, computing a single slice of the rendered image.
     */
    @Override
    public void run() {
        // Check if slave should keep on running
        while (rcr.isSlicerRunning()) {
            try {
                // Acquire permit from master to start computing slice
                master.getSemaphore().acquire();
                
                // Compute slice (and wait for master to cofirom that permit has been taken)
                master.setCompleted(identifier, rcr.slice(startHeight, endHeight));
                Thread.sleep(10);
                
                // Release permit (and wait before acquiring new one)
                master.getSemaphore().release();
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Stop computing slice
            }
        }
    }
}
