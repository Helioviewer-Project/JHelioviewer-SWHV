package org.helioviewer.jhv.internal_plugins.filter.difference;

import javax.media.opengl.GL;

import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;
import org.helioviewer.viewmodel.view.opengl.shader.GLTextureCoordinate;

public class DifferenceShader extends GLFragmentShaderProgram {
    private GLTextureCoordinate isDifference;
    private GLTextureCoordinate truncationValue;

    private static int ID = 0;
    int mode = -1;

    public void setIsDifference(GL gl, float isDifference) {
        this.isDifference.setValue(gl, isDifference);
    }

    public void setTruncationValue(GL gl, float truncationValue) {
        this.truncationValue.setValue(gl, truncationValue);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildImpl(GLShaderBuilder shaderBuilder) {

        try {
            isDifference = shaderBuilder.addTexCoordParameter(1);
            truncationValue = shaderBuilder.addTexCoordParameter(1);


            shaderBuilder.getParameterList().add("float4 " + "texcoord4" + " : TEXCOORD4");
            String program = "";
            program = "if(isdifference>0.5){\toutput.r = output.r - tex2D(differenceImage, texcoord4.xy).r;";
            program +="\toutput.r = clamp(output.r,-truncationValue,truncationValue)/truncationValue;";
            program +="\toutput.r = (output.r + 1.0f)/2.0f;";
            program += "}";

            program = program.replaceAll("output", shaderBuilder.useOutputValue("float4", "COLOR"));

            mode = shaderBuilder.addTextureParameter("sampler2D differenceImage");
            ID = (ID + 1) & 15;
            program = program.replace("isdifference", isDifference.getIdentifier(1));
            program = program.replace("truncationValue", truncationValue.getIdentifier(1));


            shaderBuilder.addMainFragment(program);
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }
    }

    public void activateDifferenceTexture(GL gl) {
    }
}
