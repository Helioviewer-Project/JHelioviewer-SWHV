package org.helioviewer.gl3d.shader;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.viewmodel.view.opengl.GLHelioviewerGeometryView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class GL3DMDIorHMIImageFragmentShaderProgram extends GLFragmentShaderProgram {

    public GL3DMDIorHMIImageFragmentShaderProgram() {
    }

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            String program = "\tfloat geometryRadius = -length(physicalPosition.zw);" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.a = output.a * step(-sunRadius, geometryRadius);" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.a *= alpha;";
            
            //program += "\toutput.rgb = output.gb * step(-sunRadius, geometryRadius);";
//            program += "\toutput.rgba = output.rgba * step(-sunRadius, geometryRadius);";

            program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
            program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
            program = program.replace("sunRadius", Double.toString(Constants.SunRadius * GLHelioviewerGeometryView.discFactor).replace(',', '.'));
//            program = program.replace("sunRadius", Double.toString(Constants.SunRadius).replace(',', '.'));
            program = program.replace("alpha", shaderBuilder.useStandardParameter("float", "TEXCOORD1"));

            shaderBuilder.addMainFragment(program);
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }

    }
}
