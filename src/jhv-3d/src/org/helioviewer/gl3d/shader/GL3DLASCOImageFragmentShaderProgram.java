package org.helioviewer.gl3d.shader;

import org.helioviewer.viewmodel.metadata.HelioviewerOcculterMetaData;
import org.helioviewer.viewmodel.view.HelioviewerGeometryView;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class GL3DLASCOImageFragmentShaderProgram extends GLFragmentShaderProgram {

    private HelioviewerOcculterMetaData metaData;

    public GL3DLASCOImageFragmentShaderProgram(HelioviewerOcculterMetaData metaData) {
        this.metaData = metaData;
    }

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {

        try {
            String program = "\tfloat geometryRadius = length(physicalPosition.zw);" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.a = output.a * step(innerRadius, geometryRadius) * step(-outerRadius, -geometryRadius);" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.a *= alpha;";

            program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
            program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
            program = program.replace("innerRadius", Double.toString(metaData.getInnerPhysicalOcculterRadius() * HelioviewerGeometryView.roccInnerFactor).replace(',', '.'));
            program = program.replace("outerRadius", Double.toString(metaData.getOuterPhysicalOcculterRadius() * HelioviewerGeometryView.roccOuterFactor).replace(',', '.'));
            program = program.replace("alpha", shaderBuilder.useStandardParameter("float", "TEXCOORD1"));

            shaderBuilder.addMainFragment(program);
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }

    }
}
