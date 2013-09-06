package org.helioviewer.gl3d.plugin.vectors.data.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.helioviewer.gl3d.plugin.vectors.data.VectorData;

/**
 * This parser can read an text file that is exported from IDL with a print
 * command The parser works actually only for one File. The reason it is still
 * used, is to load the example file vectordata.txt.
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * @date 27.06.2011
 * 
 */
public class IDLVectorParser {

    public VectorData[][] getVectorData(String file_path, int width, int height, int time) {

        // count of variables in IDL
        int varCount = 4;

        int SAZM = 2;
        int SFLD = 0;
        int SPSI = 1;

        float[][][][] input = new float[varCount][width][height][time];

        // PARSE THE WHOLE FILE IN A 4-DIMENSIONAL DOUBLE ARRAY
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(file_path)));
            int x = 0, y = 0, z = 0, var = 0;
            String line = null;
            float parsedValue = 0;

            while ((line = reader.readLine()) != null && var < varCount) {

                String[] values = line.split("\\s+");

                for (String value : values) {
                    try {
                        parsedValue = Float.parseFloat(value);
                        input[var][x][y][z] = parsedValue;

                        x++;
                        if (x == width) {
                            y++;
                            x = 0;
                        }

                        if (y == height) {
                            z++;
                            y = 0;
                        }

                        if (z == time) {
                            var++;
                            x = y = z = 0;
                        }
                    } catch (NumberFormatException e) {
                        // exception only thrown when curly braces are parsed
                        // at the head and tail of the file
                    }
                }
            }
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        VectorData[][] ret = new VectorData[time][width * height];

        for (int i = 0; i < time; i++) {
            for (int w = 0; w < width; w++) {
                for (int h = 0; h < height; h++) {
                    VectorData v = ret[i][h * width + w] = new VectorData();
                    v.azimuth = input[SAZM][w][h][i];
                    v.length = input[SFLD][w][h][i];
                    v.inclination = input[SPSI][w][h][i];
                    v.x = w + 0.5f;
                    v.y = h + 0.5f;
                }
            }
        }
        return ret;
    }
}
