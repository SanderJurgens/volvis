package com.sanderjurgens.volvis.visualization;

import com.jogamp.opengl.GL2;
import com.sanderjurgens.volvis.gui.RenderingTab;
import com.sanderjurgens.volvis.util.VectorMath;
import com.sanderjurgens.volvis.volume.Volume;

/**
 * A renderer based on the marching cubes algorithm.
 *
 * @author sanderjurgens
 */
public class MarchingCubesRenderer implements Renderer {

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
     * Resolution of vertices, each vertex represent this number of voxels
     */
    private final int resolution = 3;

    /**
     * The vertices, representing the corners of the cubes
     */
    private int[][] vertices;

    /**
     * The cubes, which can be drawn in different ways depending on vertex
     * values
     */
    private int[][] cubes;

    /**
     * Constructs a renderer to visualize the volume based on the marching cubes
     * algorithm.
     *
     * @param name the name of the renderer
     * @param renderingTab the tab containing the rendering settings
     */
    public MarchingCubesRenderer(String name, RenderingTab renderingTab) {
        this.name = name;
        this.renderingTab = renderingTab;
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
     * Set the given volume as the one to be rendered.
     *
     * @param volume the given volume
     */
    @Override
    public void setVolume(Volume volume) {
        this.volume = volume;

        // Compute dimensions based on Marching Cubes resolution
        int dimX = (int) Math.floor(volume.getDimX() / resolution);
        int dimY = (int) Math.floor(volume.getDimY() / resolution);
        int dimZ = (int) Math.floor(volume.getDimZ() / resolution);

        // Scale down volume based on resolution and store the result as vertices
        vertices = new int[dimX * dimY * dimZ][4];
        for (int i = 0; i < dimX; i++) {
            for (int j = 0; j < dimY; j++) {
                for (int k = 0; k < dimZ; k++) {
                    // Position
                    vertices[i + dimX * (j + dimY * k)][0] = i * resolution;
                    vertices[i + dimX * (j + dimY * k)][1] = j * resolution;
                    vertices[i + dimX * (j + dimY * k)][2] = k * resolution;
                    // Value
                    vertices[i + dimX * (j + dimY * k)][3]
                            = volume.getVoxel(i * resolution, j * resolution, k * resolution);
                }
            }
        }

        // Build cubes from the indices of the stored vertices (corners)
        cubes = new int[(dimX - 1) * (dimY - 1) * (dimZ - 1)][8];
        int currentCube = 0;
        for (int i = 0; i < (dimX - 1); i++) {
            for (int j = 0; j < (dimY - 1); j++) {
                for (int k = 0; k < (dimZ - 1); k++) {
                    cubes[currentCube][0] = i + dimX * (j + dimY * k);
                    cubes[currentCube][1] = i + dimX * (j + dimY * (k + 1));
                    cubes[currentCube][2] = i + dimX * ((j + 1) + dimY * (k + 1));
                    cubes[currentCube][3] = i + dimX * ((j + 1) + dimY * k);
                    cubes[currentCube][4] = (i + 1) + dimX * (j + dimY * k);
                    cubes[currentCube][5] = (i + 1) + dimX * (j + dimY * (k + 1));
                    cubes[currentCube][6] = (i + 1) + dimX * ((j + 1) + dimY * (k + 1));
                    cubes[currentCube][7] = (i + 1) + dimX * ((j + 1) + dimY * k);
                    currentCube++;
                }
            }
        }
    }

    /**
     * Set whether the user is interacting with the volume through the canvas.
     *
     * @param interacting whether the user is interacting with the volume
     */
    @Override
    public void setInteractiveMode(boolean interacting) {
        // Nothing is done differently in interactive mode
    }

    /**
     * Render the volume using the given graphical interface.
     *
     * @param gl the given graphical interface
     * @throws Exception if an error occurs while rendering
     */
    @Override
    public void render(GL2 gl) throws Exception {
        // Get the isovalue to determine which isosurface to render
        int isovalue = renderingTab.getIsovalue();

        // Store current lighting settings      
        gl.glPushAttrib(GL2.GL_ENABLE_BIT);

        // Set lighting (ambient and spot)
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, new float[]{0.5f, 0.5f, 0.5f, 1}, 0);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION,
                new float[]{-volume.getDimX(), -volume.getDimY(), 0}, 0);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
        gl.glEnable(GL2.GL_NORMALIZE);

        // Start drawing white isosurface triangles inside each cube
        gl.glColor3f(1f, 1f, 1f);
        gl.glBegin(GL2.GL_TRIANGLES);        
        for (int[] cube : cubes) {
            // Calculate which vertices are inside the isosurface
            byte cubeIndexByte = 0;
            if (vertices[cube[0]][3] > isovalue) {
                cubeIndexByte |= 1;
            }
            if (vertices[cube[1]][3] > isovalue) {
                cubeIndexByte |= 2;
            }
            if (vertices[cube[2]][3] > isovalue) {
                cubeIndexByte |= 4;
            }
            if (vertices[cube[3]][3] > isovalue) {
                cubeIndexByte |= 8;
            }
            if (vertices[cube[4]][3] > isovalue) {
                cubeIndexByte |= 16;
            }
            if (vertices[cube[5]][3] > isovalue) {
                cubeIndexByte |= 32;
            }
            if (vertices[cube[6]][3] > isovalue) {
                cubeIndexByte |= 64;
            }
            if (vertices[cube[7]][3] > isovalue) {
                cubeIndexByte |= 128;
            }

            // The result is used to look up which edges should contain vertices
            int cubeIndexInt = Byte.toUnsignedInt(cubeIndexByte);
            if (cubeIndexInt == 0 || cubeIndexInt == 255) {
                // If the cube is entirely inside/outside the surface, no faces
                continue;
            }
            int usedEdges = MarchingCubesData.EDGE_TABLE[cubeIndexInt];
            
            // Place vertices on edges that should contain vertices
            float[][] edgeVertices = new float[12][3];
            for (int currentEdge = 0; currentEdge < 12; currentEdge++) {
                // Check if an edge should contain a vertex
                if ((usedEdges & (1 << currentEdge)) > 0) {

                    int v1 = cube[MarchingCubesData.VERTEX_TABLE[currentEdge * 2]];
                    int v2 = cube[MarchingCubesData.VERTEX_TABLE[currentEdge * 2 + 1]];

                    // Linear interpolation of location on edge based on (iso)value
                    float delta = (float) (isovalue - vertices[v1][3]) / (float) (vertices[v2][3] - vertices[v1][3]);
                    edgeVertices[currentEdge][0] = vertices[v1][0] + delta * (vertices[v2][0] - vertices[v1][0]);
                    edgeVertices[currentEdge][1] = vertices[v1][1] + delta * (vertices[v2][1] - vertices[v1][1]);
                    edgeVertices[currentEdge][2] = vertices[v1][2] + delta * (vertices[v2][2] - vertices[v1][2]);
                }
            }

            // Combine the (edge) vertices to form triangles for the isosurface
            for (int i = 0; MarchingCubesData.TRIANGLE_TABLE[cubeIndexInt][i] != -1; i += 3) {
                double[] p0 = {
                    edgeVertices[MarchingCubesData.TRIANGLE_TABLE[cubeIndexInt][i + 0]][0] - (volume.getDimX() / 2),
                    edgeVertices[MarchingCubesData.TRIANGLE_TABLE[cubeIndexInt][i + 0]][1] - (volume.getDimY() / 2),
                    edgeVertices[MarchingCubesData.TRIANGLE_TABLE[cubeIndexInt][i + 0]][2] - (volume.getDimZ() / 2)
                };

                double[] p1 = {
                    edgeVertices[MarchingCubesData.TRIANGLE_TABLE[cubeIndexInt][i + 2]][0] - (volume.getDimX() / 2),
                    edgeVertices[MarchingCubesData.TRIANGLE_TABLE[cubeIndexInt][i + 2]][1] - (volume.getDimY() / 2),
                    edgeVertices[MarchingCubesData.TRIANGLE_TABLE[cubeIndexInt][i + 2]][2] - (volume.getDimZ() / 2)
                };

                double[] p2 = {
                    edgeVertices[MarchingCubesData.TRIANGLE_TABLE[cubeIndexInt][i + 1]][0] - (volume.getDimX() / 2),
                    edgeVertices[MarchingCubesData.TRIANGLE_TABLE[cubeIndexInt][i + 1]][1] - (volume.getDimY() / 2),
                    edgeVertices[MarchingCubesData.TRIANGLE_TABLE[cubeIndexInt][i + 1]][2] - (volume.getDimZ() / 2)
                };
                
                // Compute normal by interpolating the gradient along the edges
                double[] p3 = new double[3];
                p3[0] = p1[0] - p0[0];
                p3[1] = p1[1] - p0[1];
                p3[2] = p1[2] - p0[2];

                double[] p4 = new double[3];
                p4[0] = p2[0] - p0[0];
                p4[1] = p2[1] - p0[1];
                p4[2] = p2[2] - p0[2];

                double[] normal = new double[3];
                normal = VectorMath.crossproduct(p3, p4);
                
                // Add points and normals to drawing queue
                gl.glNormal3d(normal[0], normal[1], normal[2]);
                gl.glVertex3d(p0[0], p0[1], p0[2]);
                gl.glNormal3d(normal[0], normal[1], normal[2]);
                gl.glVertex3d(p1[0], p1[1], p1[2]);
                gl.glNormal3d(normal[0], normal[1], normal[2]);
                gl.glVertex3d(p2[0], p2[1], p2[2]);
            }
        }
        gl.glEnd();

        // Reset graphics settings
        gl.glPopAttrib();
    }
    
    /**
     * Stop rendering the volume, if still relevant.
     */
    @Override
    public void stop() {
        // Empty
    }
}
