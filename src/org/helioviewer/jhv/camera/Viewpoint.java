package org.helioviewer.jhv.camera;

public abstract class Viewpoint implements UpdateViewpoint {

        protected PositionLoad positionLoad;

        @Override
        public void setPositionLoad(PositionLoad _positionLoad) {
            positionLoad = _positionLoad;
        }

}
