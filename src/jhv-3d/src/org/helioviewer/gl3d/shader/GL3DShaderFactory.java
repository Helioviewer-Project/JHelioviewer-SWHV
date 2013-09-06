package org.helioviewer.gl3d.shader;

import javax.media.opengl.GL;

import org.helioviewer.gl3d.model.image.GL3DImageMesh;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLMinimalVertexShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

/**
 * The {@link GL3DShaderFactory} is used to create {@link GL3DImageMesh} nodes.
 * A Factory pattern is used, because the underlying image layer determines what
 * image meshes need to be created. Depending on the image, several image meshes
 * are grouped created.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DShaderFactory {

    /*
     * public static GL3DNode[] createImageMesh(GL gl, GL3DImageTextureView
     * imageTextureView) { MetaData metaData =
     * imageTextureView.getAdapter(MetaDataView.class).getMetaData();
     * 
     * if (metaData instanceof HelioviewerOcculterMetaData) { // LASCO //
     * Log.debug("GL3DShaderFactory: Creating LASCO Image Mesh");
     * HelioviewerOcculterMetaData hvMetaData = (HelioviewerOcculterMetaData)
     * metaData; return createLASCOImageMeshes(gl, imageTextureView,
     * hvMetaData); } else if (metaData instanceof HelioviewerMetaData) {
     * HelioviewerMetaData hvMetaData = (HelioviewerMetaData) metaData;
     * 
     * if(metaData instanceof HelioviewerPositionedMetaData) {
     * HelioviewerPositionedMetaData hvPosMetaData =
     * (HelioviewerPositionedMetaData) hvMetaData; return
     * createStereoImageMeshes(gl, imageTextureView, hvPosMetaData); } else if
     * (hvMetaData.getInstrument().equalsIgnoreCase("MDI") ||
     * hvMetaData.getInstrument().equalsIgnoreCase("HMI")) { // MDI and HMI //
     * Log.debug("GL3DShaderFactory: Creating MDI/HMI Image Mesh"); return
     * createMDIOrHMIImageMeshes(gl, imageTextureView); } else { // EIT and AIA
     * // Log.debug("GL3DShaderFactory: Creating EIT/AIA Image Mesh"); return
     * createEITorAIAImageMeshes(gl, imageTextureView); } } else { Log.error(
     * "GL3DShaderFactory: Cannot create ImageMesh for given ImageTextureView, not recognized underlying data"
     * ); return null; } }
     * 
     * private static GL3DNode[] createLASCOImageMeshes(GL gl,
     * GL3DImageTextureView imageTextureView, HelioviewerOcculterMetaData
     * hvMetaData) { GL3DImageCorona imageMesh = new GL3DImageCorona("LASCO",
     * imageTextureView, createVertexShaderProgram(gl, new
     * GL3DImageVertexShaderProgram()), createFragmentShaderProgram(gl, new
     * GL3DLASCOImageFragmentShaderProgram(hvMetaData))); //
     * Log.debug("GL3DShaderFactory: Creating LASCO Image Mesh!"); return new
     * GL3DNode[] {imageMesh}; }
     * 
     * private static GL3DNode[] createMDIOrHMIImageMeshes(GL gl,
     * GL3DImageTextureView imageTextureView) { GL3DImageMesh imageMesh = new
     * GL3DImageSphere(imageTextureView, createVertexShaderProgram(gl, new
     * GL3DImageVertexShaderProgram()), createFragmentShaderProgram(gl, new
     * GL3DMDIorHMIImageFragmentShaderProgram())); //
     * Log.debug("GL3DShaderFactory: Creating MDI or HMI Image Mesh!"); return
     * new GL3DNode[] {imageMesh}; }
     * 
     * private static GL3DNode[] createEITorAIAImageMeshes(GL gl,
     * GL3DImageTextureView imageTextureView) { GL3DImageSphere sphere = new
     * GL3DImageSphere(imageTextureView, createVertexShaderProgram(gl, new
     * GL3DImageVertexShaderProgram()), createFragmentShaderProgram(gl, new
     * GL3DAIAorEITImageFragmentShaderProgram())); GL3DImageCorona corona = new
     * GL3DImageCorona(imageTextureView, createVertexShaderProgram(gl, new
     * GL3DImageVertexShaderProgram()), createFragmentShaderProgram(gl, new
     * GL3DImageCoronaFragmentShaderProgram())); //
     * Log.debug("GL3DShaderFactory: Creating EIT or AIA Image Mesh!"); return
     * new GL3DNode[] {corona, sphere}; }
     * 
     * private static GL3DNode[] createStereoImageMeshes(GL gl,
     * GL3DImageTextureView imageTextureView, HelioviewerPositionedMetaData
     * metaData) {
     * 
     * GL3DImageSphere sphere = new GL3DPositionedImageSphere(metaData,
     * imageTextureView, createVertexShaderProgram(gl, new
     * GL3DImageVertexShaderProgram()), createFragmentShaderProgram(gl, new
     * GL3DImageSphereFragmentShaderProgram())); GL3DImageCorona corona = new
     * GL3DPositionedImageCorona(metaData, imageTextureView,
     * createVertexShaderProgram(gl, new GL3DImageVertexShaderProgram()),
     * createFragmentShaderProgram(gl, new
     * GL3DImageCoronaFragmentShaderProgram()));
     * Log.debug("GL3DShaderFactory: Creating STEREO Image Mesh!"); return new
     * GL3DNode[] {corona, sphere}; }
     */

    public static GLFragmentShaderProgram createFragmentShaderProgram(GL gl, GLFragmentShaderProgram fragmentShaderProgram) {
        // create new shader builder
        GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl, GL.GL_FRAGMENT_PROGRAM_ARB, true);

        // fill with standard values
        GLMinimalFragmentShaderProgram minimalProgram = new GLMinimalFragmentShaderProgram();
        minimalProgram.build(newShaderBuilder);
        fragmentShaderProgram.build(newShaderBuilder);

        // fill with other filters and compile
        newShaderBuilder.compile();
        // Log.debug("GL3DShaderFactory.createFragmentShaderProgram");
        return fragmentShaderProgram;
    }

    public static GLVertexShaderProgram createVertexShaderProgram(GL gl, GLVertexShaderProgram vertexShaderProgram) {
        // create new shader builder
        GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl, GL.GL_VERTEX_PROGRAM_ARB, true);

        // fill with standard values
        GLMinimalVertexShaderProgram minimalProgram = new GLMinimalVertexShaderProgram();
        minimalProgram.build(newShaderBuilder);
        vertexShaderProgram.build(newShaderBuilder);

        // fill with other filters and compile
        newShaderBuilder.compile();
        // Log.debug("GL3DShaderFactory.createVertexShaderProgram");
        return vertexShaderProgram;
    }
}
