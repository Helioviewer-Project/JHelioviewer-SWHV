package org.helioviewer.gl3d.shader;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class GL3DImageCoronaFragmentShaderProgram extends GLFragmentShaderProgram {

    public GL3DImageCoronaFragmentShaderProgram() {
    }

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            // String program =
            // "\tfloat geometryRadius = length(physicalPosition.zw);" +
            // GLShaderBuilder.LINE_SEP;
            // program += "\toutput.a = output.a * 0.5;";
            //
            // program = program.replace("output",
            // shaderBuilder.useOutputValue("float4", "COLOR"));
            // program = program.replace("physicalPosition",
            // shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
            // program = program.replace("innerRadius",
            // Double.toString(hvMetaData.getInnerPhysicalOcculterRadius() *
            // roccInnerFactor).replace(',', '.'));
            // program = program.replace("outerRadius",
            // Double.toString(hvMetaData.getOuterPhysicalOcculterRadius() *
            // roccOuterFactor).replace(',', '.'));
            //
            String program = "\tfloat geometryRadius = length(physicalPosition.zw);" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.a = output.a * (step(sunRadius, geometryRadius));";

            program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
            program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
            program = program.replace("sunRadius", Double.toString(Constants.SunRadius).replace(',', '.'));
            // program = program.replace("fadedSunRadius",
            // Double.toString(Constants.SunRadius *
            // HelioviewerGeometryView.discFadingFactor).replace(',', '.'));

            shaderBuilder.addMainFragment(program);
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }

    }
}
