package org.helioviewer.gl3d.model.image;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.HelioviewerOcculterMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.MetaDataView;

/**
 * Factory to be used for creating GL3DImageLayer Objects. This class is used by
 * the GL3DSceneGraphView.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageLayerFactory {

    public static GL3DImageLayer createImageLayer(GL3DState state, GL3DView mainView) {
        MetaData metaData = mainView.getAdapter(MetaDataView.class).getMetaData();

        GL3DImageLayer imageLayer = null;

        if (metaData instanceof HelioviewerOcculterMetaData) {
            // LASCO
            Log.debug("GL3DImageLayerFactory: Creating LASCO Image Layer");
            return new GL3DLascoImageLayer(mainView);
        } else if (metaData instanceof HelioviewerMetaData) {

            HelioviewerMetaData hvMetaData = (HelioviewerMetaData) metaData;
            if (hvMetaData.getInstrument().equalsIgnoreCase("HMI")) {
                // HMI
                Log.debug("GL3DImageLayerFactory: Creating HMI Image Layer!");
                return new GL3DHMIImageLayer(mainView);
            } else if (hvMetaData.getInstrument().equalsIgnoreCase("EIT")) {
                // EIT
                return new GL3DEITImageLayer(mainView);
            } else if (hvMetaData.getInstrument().equalsIgnoreCase("AIA")) {
                // AIA
                return new GL3DAIAImageLayer(mainView);
            } else if (hvMetaData.getInstrument().equalsIgnoreCase("SECCHI")) {
                // STEREO
                return new GL3DStereoImageLayer(mainView);
            } else {
                // GENERIC
                return new GL3DAIAImageLayer(mainView);
            }
        } else {
            Log.error("GL3DShaderFactory: Cannot create ImageMesh for given ImageTextureView, not recognized underlying data " + metaData);
        }

        return imageLayer;
    }
}
