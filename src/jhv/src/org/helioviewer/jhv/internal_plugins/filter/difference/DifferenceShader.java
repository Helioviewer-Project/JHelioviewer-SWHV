package org.helioviewer.jhv.internal_plugins.filter.difference;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class DifferenceShader extends GLFragmentShaderProgram {

    private static int ID = 0;
    int mode = -1;

    private final double[] truncationValueFloat = new double[4];
    private final int truncationValueRef = 0;

    private final double[] isDifferenceValueFloat = new double[4];
    private final int isDifferenceValueRef = 1;

    private GLShaderBuilder builder;

    public void setIsDifference(GL2 gl, float isDifference) {
        this.isDifferenceValueFloat[0] = isDifference;
    }

    public void setTruncationValue(GL2 gl, float truncationValue) {
        this.truncationValueFloat[0] = truncationValue;
    }

    @Override
    public void bind(GL2 gl) {
        super.bind(gl);
        this.bindEnvVars(gl, this.truncationValueRef, truncationValueFloat);
        this.bindEnvVars(gl, this.isDifferenceValueRef, isDifferenceValueFloat);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        this.builder = shaderBuilder;
        try {
            shaderBuilder.getParameterList().add("float4 " + "texcoord4" + " : TEXCOORD4");
            String program = "";
            // program =
            // "\tif((texcoord4.x<0.0||texcoord4.y<0.0||texcoord4.x>diffTextureScaleThetaPhi.x||texcoord4.y>diffTextureScaleThetaPhi.y)){"
            // + "\t\tOUT.color = float4(0.0,1.0,0.0,1.0);" +
            // GLShaderBuilder.LINE_SEP + "\t}" + GLShaderBuilder.LINE_SEP;

            program += "if(isdifference>0.24 && isdifference<0.27){" + GLShaderBuilder.LINE_SEP + "\toutput.r = output.r - tex2D(differenceImage, texcoord0.xy).r;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.r = clamp(output.r,-truncationValue,truncationValue)/truncationValue;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.r = (output.r + 1.0f)/2.0f;" + GLShaderBuilder.LINE_SEP;
            program += "} else" + GLShaderBuilder.LINE_SEP;

            program += "if(isdifference>0.98 && isdifference<1.01){" + GLShaderBuilder.LINE_SEP + "\toutput.r = output.r - tex2D(differenceImage, texcoord4.xy).r;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.r = clamp(output.r,-truncationValue,truncationValue)/truncationValue;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.r = (output.r + 1.0f)/2.0f;" + GLShaderBuilder.LINE_SEP;
            program += "}" + GLShaderBuilder.LINE_SEP;

            program = program.replaceAll("output", shaderBuilder.useOutputValue("float4", "COLOR"));

            mode = shaderBuilder.addTextureParameter("sampler2D differenceImage");
            ID = (ID + 1) & 15;
            //program = program.replace("isdifference", isDifference.getIdentifier(1));
            // program = program.replace("truncationValue",
            // truncationValue.getIdentifier(1));

            shaderBuilder.addMainFragment(program);
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }
    }
}
