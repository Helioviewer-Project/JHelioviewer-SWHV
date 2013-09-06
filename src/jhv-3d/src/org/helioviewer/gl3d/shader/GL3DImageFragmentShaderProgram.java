package org.helioviewer.gl3d.shader;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.HelioviewerOcculterMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.HelioviewerGeometryView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class GL3DImageFragmentShaderProgram extends GLFragmentShaderProgram {

    private MetaData metaData;

    public GL3DImageFragmentShaderProgram(MetaData metaData) {
        this.metaData = metaData;
    }

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {

        if (metaData instanceof HelioviewerOcculterMetaData) {
            // LASCO

            HelioviewerOcculterMetaData hvMetaData = (HelioviewerOcculterMetaData) metaData;

            try {
                String program = "\tfloat geometryRadius = length(physicalPosition.zw);" + GLShaderBuilder.LINE_SEP;
                program += "\toutput.a = output.a * step(innerRadius, geometryRadius) * step(-outerRadius, -geometryRadius);";

                program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
                program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
                program = program.replace("innerRadius", Double.toString(hvMetaData.getInnerPhysicalOcculterRadius() * HelioviewerGeometryView.roccInnerFactor).replace(',', '.'));
                program = program.replace("outerRadius", Double.toString(hvMetaData.getOuterPhysicalOcculterRadius() * HelioviewerGeometryView.roccOuterFactor).replace(',', '.'));

                shaderBuilder.addMainFragment(program);
            } catch (GLBuildShaderException e) {
                e.printStackTrace();
            }
        } else if (metaData instanceof HelioviewerMetaData) {

            HelioviewerMetaData hvMetaData = (HelioviewerMetaData) metaData;

            // MDI and HMI
            if (hvMetaData.getInstrument().equalsIgnoreCase("MDI") || hvMetaData.getInstrument().equalsIgnoreCase("HMI")) {

                try {
                    String program = "\tfloat geometryRadius = -length(physicalPosition.zw);" + GLShaderBuilder.LINE_SEP;
                    program += "\toutput.a = output.a * step(-sunRadius, geometryRadius);";

                    program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
                    program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
                    program = program.replace("sunRadius", Double.toString(Constants.SunRadius * HelioviewerGeometryView.discFactor).replace(',', '.'));

                    shaderBuilder.addMainFragment(program);
                } catch (GLBuildShaderException e) {
                    e.printStackTrace();
                }

            } else {
                // EIT and AIA

                try {
                    String program = "\tfloat geometryRadius = -length(physicalPosition.zw);" + GLShaderBuilder.LINE_SEP;
                    program += "\tfloat fadeDisc = smoothstep(-fadedSunRadius, -sunRadius, geometryRadius);" + GLShaderBuilder.LINE_SEP;
                    program += "\tfloat maxPixelValue = max(max(output.r, output.g), max(output.b, 0.001));" + GLShaderBuilder.LINE_SEP;
                    program += "\toutput.a = output.a * (fadeDisc + (1-fadeDisc) * pow(maxPixelValue, 1-output.a));";

                    program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
                    program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
                    // program = program.replace("sunRadius",
                    // Double.toString(Constants.SunRadius).replace(',', '.'));
                    // program = program.replace("fadedSunRadius",
                    // Double.toString(Constants.SunRadius *
                    // HelioviewerGeometryView.discFadingFactor).replace(',',
                    // '.'));
                    program = program.replace("sunRadius", Double.toString(1).replace(',', '.'));
                    program = program.replace("fadedSunRadius", Double.toString(1 * HelioviewerGeometryView.discFadingFactor).replace(',', '.'));

                    shaderBuilder.addMainFragment(program);
                } catch (GLBuildShaderException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
