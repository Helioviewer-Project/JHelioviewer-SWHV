package org.helioviewer.gl3d.shader;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.viewmodel.view.HelioviewerGeometryView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class GL3DAIAorEITImageFragmentShaderProgram extends GLFragmentShaderProgram {

    public GL3DAIAorEITImageFragmentShaderProgram() {
    }

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            String program = "\tfloat geometryRadius = -length(physicalPosition.zw);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat fadeDisc = smoothstep(-fadedSunRadius, -sunRadius, geometryRadius);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat maxPixelValue = max(max(output.r, output.g), max(output.b, 0.001));" + GLShaderBuilder.LINE_SEP;
            // program += "\toutput.a = min(maxPixelValue, output.a);";
            program += "\toutput.a = output.a * (fadeDisc + (1-fadeDisc) * pow(maxPixelValue, 1-output.a));";

            program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
            program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
            program = program.replace("sunRadius", Double.toString(Constants.SunRadius).replace(',', '.'));
            program = program.replace("fadedSunRadius", Double.toString(Constants.SunRadius * HelioviewerGeometryView.discFadingFactor).replace(',', '.'));

            shaderBuilder.addMainFragment(program);
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }

    }
}
