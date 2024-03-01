package com.sanderjurgens.volvis.util;

/**
 * Support class for basic 3D vector operations.
 *
 * @author sanderjurgens
 */
public class VectorMath {

    /**
     * Assign the given coefficients to the given vector.
     * 
     * @param v the given vector
     * @param c0 the first given coefficient
     * @param c1 the second given coefficient
     * @param c2 the third given coefficient
     */
    public static void setVector(double[] v, double c0, double c1, double c2) {
        v[0] = c0;
        v[1] = c1;
        v[2] = c2;
    }
    
    /**
     * Compute the length of the given vector.
     * 
     * @param v the given vector
     * @return the length of the given vector
     */
    public static double length(double[] v) {
        return Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
    }
    
    /**
     * Compute the dot product of the two given vectors.
     * 
     * @param v the first given vector
     * @param w the second given vector
     * @return the dot product of the two given vectors
     */
    public static double dotproduct(double[] v, double[] w) {
        double r = 0;
        for (int i=0; i<3; i++) {
            r += v[i] * w[i];
        }
        return r;
    }
    
    /**
     * Compute the distance between the two given vectors.
     * 
     * @param v the first given vector
     * @param w the second given vector
     * @return the distance between two given vectors
     */
    public static double distance(double[] v, double[] w) {
        double[] tmp = new double[3];
        VectorMath.setVector(tmp, v[0]-w[0], v[1]-w[1], v[2]-w[2]);
        return Math.sqrt(VectorMath.dotproduct(tmp, tmp));
    }

   /**
     * Compute the cross product of the two given vectors.
     * 
     * @param v the first given vector
     * @param w the second given vector
     * @return the cross product of the two given vectors
     */
    public static double[] crossproduct(double[] v, double[] w) {
        double[] r = new double[3];
        r[0] = v[1] * w[2] - v[2] * w[1];
        r[1] = v[2] * w[0] - v[0] * w[2];
        r[2] = v[0] * w[1] - v[1] * w[0];
        return r;
    } 
}
