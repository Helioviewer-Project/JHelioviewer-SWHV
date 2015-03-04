package org.helioviewer.gl3d.model.image;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.HelioviewerOcculterMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.opengl.GL3DView;

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

        if (metaData instanceof HelioviewerOcculterMetaData || metaData instanceof HelioviewerMetaData) {
            imageLayer = new GL3DImageLayer(metaData.toString(), mainView);
        } else {
            Log.error("GL3DShaderFactory: Cannot create ImageMesh for given ImageTextureView, not recognized underlying data " + metaData);
        }

        return imageLayer;
    }
}
