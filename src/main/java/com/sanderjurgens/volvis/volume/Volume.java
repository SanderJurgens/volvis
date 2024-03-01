package com.sanderjurgens.volvis.volume;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 * A volume consisting of density values in a 3 dimensional space.
 *
 * @author sanderjurgens
 */
public class Volume {

    /**
     * The x, y and z dimensions of the volume
     */
    private int dimX, dimY, dimZ;

    /**
     * The density values of the volume
     */
    private short[] data;

    /**
     * Constructs an empty volume given certain dimensions.
     *
     * @param xd the given x dimension
     * @param yd the given y dimension
     * @param zd the given z dimension
     */
    public Volume(int xd, int yd, int zd) {
        data = new short[xd * yd * zd];
        dimX = xd;
        dimY = yd;
        dimZ = zd;
    }

    /**
     * Constructs a volume based on a data stream.
     *
     * @param is the stream containing the volume data
     * @throws IOException if an error occurs while reading the stream
     */
    public Volume(InputStream is) throws IOException {
        // Parse input stream as AVS file file format, see also: 
        // https://dav.lbl.gov/archive/NERSC/Software/express/help6.2/help/reference/dvmac/Field_Fi.htm
        try (BufferedInputStream stream = new BufferedInputStream(is)) {

            // Mark the start of the header
            stream.mark(2000);

            // All field files must start with a specific line
            byte[] magicCode = new byte[5];
            if (stream.read(magicCode) == 5) {
                String headerText = new String(magicCode);
                if (!headerText.equals("# AVS")) {
                    throw new IOException("Not a valid file");
                }
            }

            // Return to start of the file and determine the total header length
            stream.reset();
            int headerLength = 1;
            while (stream.read() != '\f') {
                headerLength++;
            }
            // Also skip next form feed character (^L)
            headerLength++;

            // Return to start of the file and parse the complete header
            stream.reset();
            int byteSize = 0;
            byte[] h = new byte[headerLength];
            if (stream.read(h) == headerLength) {
                String header = new String(h);
                byteSize = parseHeader(header);
            }

            // Read all density values from the stream
            int byteCount = dimX * dimY * dimZ * byteSize;
            byte[] d = new byte[byteCount];
            stream.read(d);

            // Store all density values into data array
            data = new short[dimX * dimY * dimZ];
            if (byteSize == 1) { // bytes
                for (int i = 0; i < byteCount; i++) {
                    data[i] = (short) (d[i] & 0xFF);
                }
            } else if (byteSize == 2) { // shorts
                for (int i = 0; i < byteCount; i += 2) {
                    short value = (short) ((d[i] & 0xFF) + (d[i + 1] & 0xFF) * 256);
                    data[i / 2] = value;
                }
            }
        }
    }

    /**
     * Parses the given header of a field file (.fld).
     *
     * @param header the given header
     * @return the byte size of density values (1 = bytes, 2 = shorts)
     * @throws IOException if an error occurs while reading the header
     */
    private int parseHeader(String header) throws IOException {

        // Byte size of object storing density values (1 = bytes, 2 = shorts)
        int byteSize = 0;

        // The possible header tokens, all tokens after "field" are optional
        String[] tokens = {"ndim", "dim1", "dim2", "dim3", "nspace", "veclen",
            "data", "field", "min_ext", "max_ext", "variable", "#", "label", "unit",
            "min_val", "max_val"};

        // Read header line by line
        BufferedReader br = new BufferedReader(new StringReader(header));
        String line = null;
        while ((line = br.readLine()) != null) {
            // Determine if the line assigns a value to a token
            if (line.contains("=") && (!line.contains("#") || line.indexOf("=") < line.indexOf("#"))) {
                // Split line on '=' and '#' seperators, removing spaces
                String[] elem = line.split("\\s*=\\s*|\\s*#\\s*");

                // Determine which token is being set                
                int tokenIndex;
                for (tokenIndex = 0; tokenIndex < tokens.length; tokenIndex++) {
                    if (elem[0].equals(tokens[tokenIndex])) {
                        break;
                    }
                }

                // Parse given value for respective token
                switch (tokenIndex) {
                    case 0: // ndim
                        if (Integer.parseInt(elem[1]) != 3) {
                            throw new IOException("Only 3D files are supported");
                        }
                        break;
                    case 1: // dim1
                        dimX = Integer.parseInt(elem[1]);
                        break;
                    case 2: // dim2
                        dimY = Integer.parseInt(elem[1]);
                        break;
                    case 3: // dim3
                        dimZ = Integer.parseInt(elem[1]);
                        break;
                    case 4: // nspace
                        break;
                    case 5: // veclen
                        if (Integer.parseInt(elem[1]) != 1) {
                            throw new IOException("Only scalar data is supported");
                        }
                        break;
                    case 6: // data
                        byteSize = 0;
                        if (elem[1].equals("byte")) {
                            byteSize = 1;
                        } else if (elem[1].equals("short")) {
                            byteSize = 2;
                        } else {
                            throw new IOException("Data type not supported: '" + elem[1] + "'");
                        }
                        break;
                    case 7: // field
                        if (!elem[1].equals("uniform")) {
                            throw new IOException("Only uniform data is supported");
                        }
                    case 8: // min_ext
                    case 9: // max_ext
                    case 10: // variable
                    case 11: // #
                    case 12: // label
                    case 13: // unit
                    case 14: // min_val
                    case 15: // max_val
                        break;
                    default:
                        throw new IOException("Invalid AVS token in file: '" + elem[0] + "'");
                }
            }
        }
        return byteSize;
    }

    /**
     * The x dimension of the volume.
     *
     * @return x dimension of the volume.
     */
    public int getDimX() {
        return dimX;
    }

    /**
     * The y dimension of the volume.
     *
     * @return y dimension of the volume.
     */
    public int getDimY() {
        return dimY;
    }

    /**
     * The z dimension of the volume.
     *
     * @return z dimension of the volume.
     */
    public int getDimZ() {
        return dimZ;
    }

    /**
     * Returns the density data.
     *
     * @return the density data
     */
    public short[] getData() {
        return data;
    }

    /**
     * Get the density value for the given voxel coordinate.
     *
     * @param x the given x coordinate
     * @param y the given y coordinate
     * @param z the given z coordinate
     * @return the density value for given the voxel coordinate
     */
    public short getVoxel(int x, int y, int z) {
        if (0 <= x && x < dimX && 0 <= y && y < dimY && 0 <= z && z < dimZ) {
            return data[x + dimX * (y + dimY * z)];
        }
        return 0;
    }

    /**
     * Get the density value for the given index.
     *
     * @param i the given index
     * @return the density value for the given index
     */
    public short getVoxel(int i) {
        if (0 <= i && i < data.length) {
            return data[i];
        }
        return 0;
    }

    /**
     * Set the density value for the given voxel coordinate.
     *
     * @param x the given x coordinate
     * @param y the given y coordinate
     * @param z the given z coordinate
     * @param value the given density value
     */
    public void setVoxel(int x, int y, int z, short value) {
        if (0 <= x && x < dimX && 0 <= y && y < dimY && 0 <= z && z < dimZ) {
            data[x + dimX * (y + dimY * z)] = value;
        }
    }

    /**
     * Set the density value for the given index.
     *
     * @param i the given index
     * @param value the given density value
     */
    public void setVoxel(int i, short value) {
        if (0 <= i && i < data.length) {
            data[i] = value;
        }
    }

    /**
     * Returns the diagonal of the volume.
     *
     * @return the diagonal of the volume
     */
    public double getDiagonal() {
        return Math.sqrt(dimX * dimX + dimY * dimY + dimZ * dimZ);
    }

    /**
     * Return the minimum density in the volume.
     *
     * @return the minimum density in the volume
     */
    public short getMinimum() {
        short minimum = data[0];
        for (int i = 0; i < data.length; i++) {
            minimum = data[i] < minimum ? data[i] : minimum;
        }
        return minimum;
    }

    /**
     * Return the maximum density in the volume.
     *
     * @return the maximum density in the volume
     */
    public short getMaximum() {
        short maximum = data[0];
        for (int i = 0; i < data.length; i++) {
            maximum = data[i] > maximum ? data[i] : maximum;
        }
        return maximum;
    }

    /**
     * Rotate volume by 180 degrees along the x-axis.
     */
    public void rotate() {
        short[] copy = new short[dimX * dimY * dimZ];
        for (int i = 0; i < dimX; i++) {
            for (int j = 0; j < dimY; j++) {
                for (int k = 0; k < dimZ; k++) {
                    copy[i + dimX * (((dimY - 1) - j) + dimY * ((dimZ - 1) - k))] = 
                            data[i + dimX * (j + dimY * k)];
                }
            }
        }
        data = copy;
    }
}
