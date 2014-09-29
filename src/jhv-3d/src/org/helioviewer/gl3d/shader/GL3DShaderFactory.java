package org.helioviewer.gl3d.shader;

import javax.media.opengl.GL2;

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
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DShaderFactory {
    public static GLFragmentShaderProgram createFragmentShaderProgram(GL2 gl, GLFragmentShaderProgram fragmentShaderProgram) {
        // create new shader builder
        GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, true);

        // fill with standard values
        GLMinimalFragmentShaderProgram minimalProgram = new GLMinimalFragmentShaderProgram();
        minimalProgram.build(newShaderBuilder);
        fragmentShaderProgram.build(newShaderBuilder);

        // fill with other filters and compile
        newShaderBuilder.compile();
        // Log.debug("GL3DShaderFactory.createFragmentShaderProgram");
        return fragmentShaderProgram;
    }

    public static GLVertexShaderProgram createVertexShaderProgram(GL2 gl, GLVertexShaderProgram vertexShaderProgram) {
        // create new shader builder
        GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl, GL2.GL_VERTEX_PROGRAM_ARB, true);

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
