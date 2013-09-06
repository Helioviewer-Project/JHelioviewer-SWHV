package org.helioviewer.gl3d.plugin.pfss;

import java.io.IOException;

import org.helioviewer.gl3d.plugin.pfss.data.IncorrectPfssFileException;
import org.helioviewer.gl3d.plugin.pfss.data.PfssReader;

public class PfssReaderTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        PfssReader reader = new PfssReader();
        try {
            reader.readFile("/pfss/pfss_field_2011-08-04_00-04-00.dat");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IncorrectPfssFileException e) {
            e.printStackTrace();
        }

    }

}
