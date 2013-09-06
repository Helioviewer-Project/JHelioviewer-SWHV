package org.helioviewer.gl3d.shader;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.viewmodel.view.HelioviewerGeometryView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class GL3DImageSphereFragmentShaderProgram extends GLFragmentShaderProgram {

    public GL3DImageSphereFragmentShaderProgram() {
    }

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            // String program =
            // "\tfloat geometryRadius = length(physicalPosition.zw);" +
            // GLShaderBuilder.LINE_SEP;
            // program +=
            // "\toutput.a = output.a * (1-step(sunRadius, geometryRadius));";
            //
            // program = program.replace("output",
            // shaderBuilder.useOutputValue("float4", "COLOR"));
            // program = program.replace("physicalPosition",
            // shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
            // program = program.replace("sunRadius",
            // Double.toString(Constants.SunRadius).replace(',', '.'));

            String program = "\tfloat geometryRadius = -length(physicalPosition.zw);" + GLShaderBuilder.LINE_SEP;
            // program +=
            // "\toutput.a = output.a * step(-sunRadius, geometryRadius);";

            program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
            program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
            program = program.replace("sunRadius", Double.toString(Constants.SunRadius * HelioviewerGeometryView.discFactor).replace(',', '.'));

            shaderBuilder.addMainFragment(program);
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }

    }
}
