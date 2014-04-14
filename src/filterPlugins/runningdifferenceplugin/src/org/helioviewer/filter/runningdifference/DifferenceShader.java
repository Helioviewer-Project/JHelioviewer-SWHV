package org.helioviewer.filter.runningdifference;

import javax.media.opengl.GL;

import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLTextureCoordinate;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class DifferenceShader extends GLFragmentShaderProgram {
    private GLTextureCoordinate differenceContrast;
	private static int ID = 0;
    int mode = -1;

    private void setDifferenceContrast(GL gl, float contrast) {
        	differenceContrast.setValue(gl, contrast);
    }

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {

        try {
            String program = "\toutput.r = (output.r - tex2D(differenceImage, texcoord0.xy).r)/output.r;";
            program += "\tvec4 tr = vec4(0.05f,0.05f,0.05f,0.05f);";
            program += "output.r = (output.r + 1.0f)/2.0f;";
            program = program.replaceAll("output", shaderBuilder.useOutputValue("float4", "COLOR"));
            
            mode = shaderBuilder.addTextureParameter("sampler2D differenceImage" + 3);
            program = program.replaceAll("differenceImage", "differenceImage" + 3);
            ID = (ID + 1) & 15;
            //program = program.replace("differenceContrast", differenceContrast.getIdentifier(1));

            shaderBuilder.addMainFragment(program);
            System.out.println("SHADERDIFF: " + shaderBuilder.getCode());
            //System.exit(1);
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }
    }

	public void activateDifferenceTexture(GL gl) {
	}
}	
