    package org.helioviewer.gl3d.shader;

import java.nio.FloatBuffer;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class GL3DImageCoronaFragmentShaderProgram extends GLFragmentShaderProgram {

	private int h;
    public GL3DImageCoronaFragmentShaderProgram() {
    }

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            String program = "\toutput.a *= alpha;" + GLShaderBuilder.LINE_SEP;
            
            program += "\tfloat2 texture;" + GLShaderBuilder.LINE_SEP;
            program += "\ttexture.x = textureCoordinate.z - 0.5;" + GLShaderBuilder.LINE_SEP;
            program += "\ttexture.y = textureCoordinate.w - 0.5;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.a *= step(cutOffRadius, length(texture));" + GLShaderBuilder.LINE_SEP;
            shaderBuilder.addEnvParameter("float cutOffRadius");
            shaderBuilder.addEnvParameter("float alpha");
            
            program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
            program = program.replace("textureCoordinate", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
            shaderBuilder.addMainFragment(program);
            
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }

    }
        
    public void changeAlpha(double alpha){
    	this.alpha = alpha;
    }
    
    public void setCutOffRadius(double cutOffRadius){
    	this.cutOffRadius = cutOffRadius;
    }
}
