package org.helioviewer.filter.runningdifference;

import javax.media.opengl.GL;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLTextureCoordinate;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class DifferenceShader extends GLFragmentShaderProgram {
    private GLTextureCoordinate isDifference;
    private GLTextureCoordinate differenceAngle;

    private static int ID = 0;
    int mode = -1;

    public void setIsDifference(GL gl, float isDifference) {
        this.isDifference.setValue(gl, isDifference);
    }

    public void setDifferenceAngle(GL gl, float differenceAngle) {
        this.differenceAngle.setValue(gl, differenceAngle);
    }

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {

        try {
            isDifference = shaderBuilder.addTexCoordParameter(1);
            differenceAngle = shaderBuilder.addTexCoordParameter(1);
            String program = "";
            program = "if(isdifference>0.5){\toutput.r = ( output.r - tex2D(differenceImage, texcoord0.xy).r)/(output.r);";
            program += "\tvec4 tr = vec4(0.05f,0.05f,0.05f,0.05f);";
            program += "output.r = (output.r + 1.0f)/2.0f;}";
            // program =
            // "\toutput.rgb = tex2D(differenceImage, texcoord0.xy).rgb;";

            program = program.replaceAll("output", shaderBuilder.useOutputValue("float4", "COLOR"));

            mode = shaderBuilder.addTextureParameter("sampler2D differenceImage");
            ID = (ID + 1) & 15;
            program = program.replace("isdifference", isDifference.getIdentifier(1));
            program = program.replace("differenceAngle", differenceAngle.getIdentifier(1));

            shaderBuilder.addMainFragment(program);
            System.out.println("SHADERDIFF: " + shaderBuilder.getCode());
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }
    }

    public void activateDifferenceTexture(GL gl) {
    }
}
