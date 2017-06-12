package org.helioviewer.jhv.camera;

public abstract class Viewpoint implements UpdateViewpoint {

        protected LoadPosition loadPosition;

        @Override
        public void setLoadPosition(LoadPosition _loadPosition) {
            loadPosition = _loadPosition;
        }

}
