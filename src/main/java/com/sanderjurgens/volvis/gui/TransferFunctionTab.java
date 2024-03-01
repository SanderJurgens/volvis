package com.sanderjurgens.volvis.gui;

import com.sanderjurgens.volvis.volume.Volume;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The TransferFunctionTab is a Swing JPanel designed to adjust the color
 * settings of some transfer function volume renderers.
 *
 * @author sanderjurgens
 */
public class TransferFunctionTab extends JPanel {

    /**
     * Panel containing the histogram
     */
    private histPanel histPanel;

    /**
     * Panel containing the color legend of the histogram
     */
    private legendPanel legendPanel;

    /**
     * Panel containing the color chooser
     */
    private JPanel colorPanel;

    /**
     * Histogram values (voxel values on x-axis, occurrences on y-axis)
     */
    private int[] histogram;

    /**
     * Size of histogram array (2D)
     */
    private int[] histSize;

    /**
     * Minimum density cutoff to reduce noise
     */
    private int minDensity;

    /**
     * The transfer function
     */
    private final ArrayList<double[]> points = new ArrayList<>();

    /**
     * The radius of points on the transfer function
     */
    private final int pointRadius = 5;

    /**
     * The current point being dragged
     */
    private double[] draggedPoint = null;

    /**
     * Whether a renderer has been notified of a transfer function change
     */
    private final Map<String, Boolean> rendererNotified = new HashMap<>();

    /**
     * The currently selected color
     */
    private Color selectedColor = Color.BLACK;

    /**
     * Constructs a new HistogramTab.
     */
    public TransferFunctionTab() {
        initComponents();
    }

    /**
     * Adjust the settings to a newly loaded volume.
     *
     * @param volume a newly loaded volume
     */
    public void setVolume(Volume volume) {
        // Re-populate the histogram with the number of occurences per voxel value
        histSize = new int[]{volume.getMaximum() + 1, 0};
        histogram = new int[histSize[0]];
        for (short s : volume.getData()) {
            histogram[s]++;
        }

        // Compact the y-axis by taking the square root and find the highest peak
        int peak = 0;
        for (int i = 0; i < histSize[0]; i++) {
            histogram[i] = (int) Math.sqrt(histogram[i]);
            if (histogram[i] > histSize[1]) {
                histSize[1] = histogram[i];
                peak = i;
            }
        }

        // Set minimum density at first low density relative to the highest peak
        for (int i = peak + 1; i < histSize[0]; i++) {
            if (histogram[i] < (0.2 * histogram[peak])) {
                minDensity = i;
                break;
            }
        }

        // Rescale the y-axis from minimum density onward
        histSize[1] = 0;
        for (int i = minDensity; i < histSize[0]; i++) {
            histSize[1] = Math.max(histSize[1], histogram[i]);
        }

        // Repaint the panel
        repaint();
    }

    /**
     * Initialize the graphical user interface components.
     */
    private void initComponents() {
        // Initiate panels
        histPanel = new histPanel();
        legendPanel = new legendPanel();
        colorPanel = new JPanel();

        // Set size of the tab
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setMinimumSize(new Dimension(300, 600));
        setPreferredSize(new Dimension(300, 600));
        setLayout(new BoxLayout(this, javax.swing.BoxLayout.PAGE_AXIS));

        // Add label for the color chooser
        JPanel histLabel = new JPanel();
        histLabel.setMaximumSize(new Dimension(2147483647, 20));
        histLabel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        histLabel.add(new JLabel("Click box to add, move, recolor or delete points:"));
        add(histLabel);

        // Set size and listeners for the histogram panel
        histPanel.setMaximumSize(new Dimension(2147483647, 200));
        histPanel.setMinimumSize(new Dimension(280, 200));
        histPanel.setPreferredSize(new Dimension(280, 200));
        histPanel.setBackground(Color.WHITE);
        histPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        histPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                histogramMouseClicked(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                histogramMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                histogramMouseReleased(e);
            }
        });
        histPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                histogramMouseDragged(e);
            }
        });
        add(histPanel);

        // Set size of the histogram legend panel
        legendPanel.setMaximumSize(new Dimension(2147483647, 20));
        legendPanel.setMinimumSize(new Dimension(280, 20));
        legendPanel.setPreferredSize(new Dimension(280, 20));
        add(legendPanel);

        // Add vertical spacing
        add(Box.createVerticalStrut(10));

        // Add label for the color chooser
        JPanel colorLabel = new JPanel();
        colorLabel.setMaximumSize(new Dimension(2147483647, 20));
        colorLabel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        colorLabel.add(new JLabel("Select a color by clicking the box below:"));
        add(colorLabel);

        // Set size and listeners for the color chooser panel
        colorPanel.setMaximumSize(new Dimension(2147483647, 200));
        colorPanel.setMinimumSize(new Dimension(280, 200));
        colorPanel.setPreferredSize(new Dimension(280, 200));
        colorPanel.setBackground(selectedColor);
        colorPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        colorPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                colorMouseClicked(e);
            }
        });
        add(colorPanel);
    }
    
    /**
     * Return the list of points in range of the mouse location.
     *
     * @param e mouse event
     * @return the list of points in range of the mouse location
     */
    public ArrayList<double[]> pointsInRange(MouseEvent e) {
        ArrayList<double[]> pointsInRange = new ArrayList<>();
        for (double[] p : points) {
            // Based on 2D Euclidean distance
            if (Math.sqrt(Math.pow(e.getX() - (p[0] * histPanel.getWidth()), 2)
                    + Math.pow((histPanel.getHeight() - e.getY()) - (p[1] * histPanel.getHeight()), 2))
                    <= pointRadius) {
                pointsInRange.add(p);
            }
        }
        return pointsInRange;
    }

    /**
     * Returns the transfer function point at the given index.
     *
     * @param index the given index
     */
    private double[] getPoint(int index) {
        try {
            if (0 <= index && index < points.size()) {
                double[] p = points.get(index);
                if (p != null) {
                    return p;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // Due to concurrence we explicitely check this case
        }

        // If index is outside of range or point is null return a 0-point
        return new double[]{0, 0, 0, 0, 0};
    }

    /**
     * Add a given point to the transfer function.
     *
     * @param point a given point
     */
    public void addPoint(double[] point) {
        // Keep points ordered based on x-position
        boolean added = false;
        for (int i = 0; i < points.size(); i++) {
            if (point[0] < getPoint(i)[0]) {
                points.add(i, point);
                added = true;
                break;
            }
        }

        // If the point has not been added yet, add it to the end
        if (!added) {
            points.add(point);
        }

        // Store whether the points have changed
        pointsChanged();
    }

    /**
     * Recolor or remove the clicked transfer function points.
     *
     * @param evt mouse clicked event
     */
    private void histogramMouseClicked(MouseEvent e) {
        if (e.getButton() == 2) {
            // If the middle mouse button is clicked, recolor all clicked points
            ArrayList<double[]> pointsToColor = pointsInRange(e);
            for (double[] p : pointsToColor) {
                p[2] = selectedColor.getRed();
                p[3] = selectedColor.getGreen();
                p[4] = selectedColor.getBlue();
            }

            // Store whether the points have changed and repaint
            if (!pointsToColor.isEmpty()) {
                pointsChanged();
            }
            repaint();
        } else if (e.getButton() == 3) {
            // If the right mouse button is clicked, remove all clicked points 
            ArrayList<double[]> pointsToRemove = pointsInRange(e);
            for (double[] p : pointsToRemove) {
                points.remove(p);
            }

            // Store whether the points have changed and repaint
            if (!pointsToRemove.isEmpty()) {
                pointsChanged();
            }
            repaint();
        }
    }

    /**
     * Add and/or start dragging the pressed transfer function point.
     *
     * @param evt mouse pressed event
     */
    private void histogramMousePressed(MouseEvent e) {
        // If the left mouse button is pressed
        if (e.getButton() == 1) {
            // Decide whether to create a new point, or move an existing point
            ArrayList<double[]> pointsPressed = pointsInRange(e);
            if (pointsPressed.isEmpty()) {
                // No points in range, so create a new one
                double[] point = new double[5];
                point[0] = (double) e.getX() / (double) histPanel.getWidth();
                point[1] = (double) (e.getY() - histPanel.getHeight()) / (double) (-1 * histPanel.getHeight());
                point[2] = (double) selectedColor.getRed();
                point[3] = (double) selectedColor.getGreen();
                point[4] = (double) selectedColor.getBlue();

                // Add point to transfer function, start dragging it and repaint
                addPoint(point);
                draggedPoint = point;

                // Store whether the points have changed and repaint
                pointsChanged();
                repaint();
            } else {
                // Pick first point in range to start dragging, ignore others
                draggedPoint = pointsPressed.get(0);
            }
        }
    }

    /**
     * Drag the selected transfer function point, if any.
     *
     * @param evt mouse dragged event
     */
    private void histogramMouseDragged(MouseEvent e) {
        // Update the location of a point if one is being dragged
        if (draggedPoint != null) {
            points.remove(draggedPoint);
            draggedPoint[0] = Math.min(Math.max(0, (double) e.getX() / (double) histPanel.getWidth()), 1);
            draggedPoint[1] = Math.min(Math.max(0, (double) (e.getY() - histPanel.getHeight()) / (double) (-1 * histPanel.getHeight())), 1);
            addPoint(draggedPoint);

            // Store whether the points have changed and repaint
            pointsChanged();
            repaint();
        }
    }

    /**
     * Stop dragging the selected transfer function point, if any.
     *
     * @param evt mouse release event
     */
    private void histogramMouseReleased(MouseEvent e) {
        // Finalize the location of a point if one is being dragged
        if (draggedPoint != null) {
            draggedPoint = null;
            pointsChanged();
            repaint();
        }
    }

    /**
     * Change the selected color.
     *
     * @param evt mouse clicked event
     */
    private void colorMouseClicked(MouseEvent e) {
        // If the left button is pressed
        if (e.getButton() == 1) {
            // Select a new color for use in the transfer function
            Color c = JColorChooser.showDialog(colorPanel,
                    "Choose Color", selectedColor);

            // Update selected color and its visualization in the panel
            if (c != null) {
                selectedColor = c;
                colorPanel.setBackground(c);
                repaint();
            }
        }
    }

    /**
     * Add the given renderer to notification list for transfer function
     * changes.
     *
     * @param renderer the given renderer
     */
    public void addRenderer(String renderer) {
        rendererNotified.put(renderer, false);
    }

    /**
     * Set notification for each renderer that transfer function points have
     * been changed.
     */
    private void pointsChanged() {
        for (Map.Entry<String, Boolean> entry : rendererNotified.entrySet()) {
            entry.setValue(true);
        }
    }

    /**
     * Read whether there is a notification regarding a change in the transfer
     * function points for the given renderer.
     *
     * @param renderer the given renderer
     * @return whether there is a notification regarding a change in the
     * transfer function points for the given renderer
     */
    public boolean readPointsChanged(String renderer) {
        if (rendererNotified.containsKey(renderer)) {
            if (rendererNotified.get(renderer)) {
                rendererNotified.put(renderer, false);
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * Returns the color (including opacity) for a given voxel value.
     *
     * @param voxel a given voxel value
     * @return the color for a given voxel value
     */
    public Color getColor(short voxel) {
        // Transform voxel value to a position on the x axis of the histogram.
        return getColor((int) (((float) voxel / (float) (histSize[0] - 1)) * histPanel.getWidth()));
    }

    /**
     * Returns the color (including opacity) for a given x coordinate on the
     * histogram.
     *
     * @param x a given x coordinate on the histogram
     * @return the color for a given x coordinate on the histogram
     */
    private Color getColor(int x) {
        // Calculate x coordinate for minimum density cutoff
        int densX = (int) (((float) minDensity / (float) (histSize[0] - 1)) * histPanel.getWidth());

        // A transfer function must have at least 2 points
        if (points.size() >= 2) {
            // Check if the given x is with the transfer function bounds
            if ((getPoint(0)[0] * histPanel.getWidth()) <= x && x < (getPoint(points.size() - 1)[0] * histPanel.getWidth())) {
                // Find the two points between which the x position lies
                for (int i = 1; i < points.size(); i++) {
                    if (x <= (getPoint(i)[0] * histPanel.getWidth())) {
                        int x1 = (int) (getPoint(i - 1)[0] * histPanel.getWidth());
                        float op1 = (float) getPoint(i - 1)[1];
                        Color c1 = new Color((int) getPoint(i - 1)[2], (int) getPoint(i - 1)[3], (int) getPoint(i - 1)[4]);
                        int x2 = (int) (getPoint(i)[0] * histPanel.getWidth());
                        float op2 = (float) getPoint(i)[1];
                        Color c2 = new Color((int) getPoint(i)[2], (int) getPoint(i)[3], (int) getPoint(i)[4]);

                        // Calculate opacity and color based on distance to points
                        float dx = Math.max(((float) (x - x1) / (float) (x2 - x1)), 0);
                        float a = x < densX ? 0 : (op1 * (1 - dx)) + (op2 * dx);
                        float r = ((((float) c1.getRed() / 255) * (1 - dx)) + (((float) c2.getRed() / 255) * dx));
                        float g = ((((float) c1.getGreen() / 255) * (1 - dx)) + (((float) c2.getGreen() / 255) * dx));
                        float b = ((((float) c1.getBlue() / 255) * (1 - dx)) + (((float) c2.getBlue() / 255) * dx));
                        return new Color(r, g, b, a);
                    }
                }
            } else {
                // x is not within transfer function bounds and therefore invisible
                return new Color(0, 0, 0, 0);
            }
        }

        // If no transfer function exists, use rainbow gradiant and density
        Color rgb = Color.getHSBColor((float) x / histPanel.getWidth(), 1, 1);
        int a = (int) (255 * Math.sqrt((double) Math.max(0, x - densX)
                / (double) (histPanel.getWidth() - densX)));
        return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), a);
    }

    /**
     * Panel containing the histogram.
     */
    private class histPanel extends JPanel {

        /**
         * Paint the histogram.
         *
         * @param g the Graphics context in which to paint
         */
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Draws the histogram, if a volume is loaded
            if (histogram != null) {
                g.setColor(Color.LIGHT_GRAY);
                double dx = (double) histPanel.getWidth() / (double) histSize[0];
                double dy = (double) histPanel.getHeight() / (double) histSize[1];
                for (int i = 0; i < histSize[0]; i++) {
                    // Change histogram color once we enter the visible range
                    if (i == minDensity) {
                        g.setColor(Color.BLUE);
                    }
                    //(0,0) is top left corner, so height is inverted to draw up
                    g.fillRect((int) (i * dx),
                            histPanel.getHeight(),
                            (int) Math.ceil(dx),
                            (int) Math.min(histogram[i] * dy * -1, histPanel.getHeight()));
                }
            }

            // Draw the lines of the transfer function
            g.setColor(Color.RED);
            for (int i = 1; i < points.size(); i++) {
                g.drawLine((int) (getPoint(i - 1)[0] * histPanel.getWidth()),
                        histPanel.getHeight() - (int) (getPoint(i - 1)[1] * histPanel.getHeight()),
                        (int) (getPoint(i)[0] * histPanel.getWidth()),
                        histPanel.getHeight() - (int) (getPoint(i)[1] * histPanel.getHeight()));
            }

            // Draw the points of the transfer functions
            for (double[] coord : points) {
                g.setColor(new Color((int) coord[2], (int) coord[3], (int) coord[4]));
                g.fillOval((int) (coord[0] * histPanel.getWidth()) - pointRadius,
                        histPanel.getHeight() - (int) (coord[1] * histPanel.getHeight()) - pointRadius,
                        pointRadius * 2, pointRadius * 2);
                g.setColor(Color.BLACK);
                g.drawOval((int) (coord[0] * histPanel.getWidth()) - pointRadius,
                        histPanel.getHeight() - (int) (coord[1] * histPanel.getHeight()) - pointRadius,
                        pointRadius * 2, pointRadius * 2);
            }
        }
    }

    /**
     * Panel containing the color legend of the histogram.
     */
    private class legendPanel extends JPanel {

        /**
         * Paint the color legend.
         *
         * @param g the Graphics context in which to paint
         */
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int i = 0; i < legendPanel.getWidth(); i++) {
                g.setColor(getColor(i));
                g.drawLine(i, 0, i, legendPanel.getHeight());
            }
        }
    }
}
