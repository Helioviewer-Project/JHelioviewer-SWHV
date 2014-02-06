package org.helioviewer.gl3d.scenegraph.rt;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;

/**
 * The {@link GL3DRayTracer} can be used to cast {@link GL3DRay}s through the
 * scene graph. To be able to create a Ray a reference to a camera is required.
 * Also, the rayTracer does necessarily need to traverse the whole scene graph,
 * also subnodes can be used as root nodes for the Ray Tracer.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DRayTracer {

    private GL3DNode sceneRoot;
    private GL3DCamera camera;

    double hh;
    double hw;
    double pixelSize;

    public GL3DRayTracer(GL3DNode sceneRoot, GL3DCamera camera) {
        this.sceneRoot = sceneRoot;
        this.camera = camera;
        hh = Math.tan(Math.toRadians(camera.getFOV() / 2)) * camera.getClipNear();
        hw = hh * camera.getAspect();
        pixelSize = hw / camera.getWidth() * 2;
    }

    public synchronized GL3DRay castCenter() {
        GL3DRay ray = createCenterRay(this.camera);

        // isOutside flag set to true if the ray hit no object in the scene
        ray.isOutside = !this.sceneRoot.hit(ray);
        return ray;
    }

    public synchronized GL3DRay cast(int pixelX, int pixelY) {
    	GL3DRay ray = createPrimaryRay(this.camera, pixelX, pixelY);

        // isOutside flag set to true if the ray hit no object in the scene
        ray.isOutside = !this.sceneRoot.hit(ray);
        return ray;
    }

    private GL3DRay createCenterRay(GL3DCamera camera) {
        GL3DMat4d VM = camera.getVM();
        GL3DVec3d LA = new GL3DVec3d();
        GL3DVec3d LR = new GL3DVec3d();
        GL3DVec3d LU = new GL3DVec3d();
        GL3DVec3d EYE = new GL3DVec3d();

        VM.readLookAt(EYE, LA, LU, LR);
        LA.normalize();

        GL3DVec3d C = LA.multiply(camera.getClipNear());

        GL3DRay ray = GL3DRay.createPrimaryRay(EYE, C);
        return ray;
    }

    private GL3DRay createPrimaryRay(GL3DCamera camera, int x, int y) {
        GL3DMat4d VM = camera.getVM();
        GL3DVec3d LA = new GL3DVec3d();
        GL3DVec3d LR = new GL3DVec3d();
        GL3DVec3d LU = new GL3DVec3d();
        GL3DVec3d EYE = new GL3DVec3d();

        VM.readLookAt(EYE, LA, LU, LR);
        LA.normalize();
        LU.normalize();
        LR.normalize();
        GL3DVec3d C = LA.multiply(camera.getClipNear());
        GL3DVec3d TL = C.subtract(LR.copy().multiply(hw)).add(LU.copy().multiply(hh));

        GL3DVec3d dir = TL.copy().add(LR.copy().multiply(x).subtract(LU.copy().multiply(y)).multiply(pixelSize));
        // dir = VM.copy().mat3().inverse().multiply(dir);
        // EYE = VM.copy().inverse().multiply(EYE);

        // GL3DMat4d VM_Inv = VM.inverse();
        // dir = VM_Inv.mat3().multiply(dir);
        // EYE = VM_Inv.multiply(EYE);

        GL3DRay ray = GL3DRay.createPrimaryRay(EYE, dir);
        return ray;
    }

}
