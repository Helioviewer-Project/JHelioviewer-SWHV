package org.helioviewer.gl3d.shader;

import javax.media.opengl.GL;

import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DImageCoronaVertexShaderProgram extends GLVertexShaderProgram {
	private double theta;
	private double phi;
	
    /**
     * {@inheritDoc}
     */
    public final void bind(GL gl) {
    	bind(gl, shaderID, xOffset, yOffset, xScale, yScale, xTextureScale, yTextureScale, defaultXOffset, defaultYOffset, theta, phi);
    }	
    private static void bind(GL gl, int shader, double xOffset, double yOffset, double xScale, double yScale, double xTextureScale, double yTextureScale, double defaultXOffset, double defaultYOffset, double theta, double phi) {
        if (shader != shaderCurrentlyUsed) {
            shaderCurrentlyUsed = shader;
            gl.glBindProgramARB(target, shader);
            gl.glProgramLocalParameter4dARB(target, 0, xOffset, yOffset, xScale, yScale);
            gl.glProgramLocalParameter4dARB(target, 1, xTextureScale, yTextureScale,theta, phi);
            gl.glProgramLocalParameter4dARB(target, 2, defaultXOffset, defaultYOffset, 0,0);
        }
    }
    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            String program = "\tphysicalPosition = physicalPosition;" + GLShaderBuilder.LINE_SEP;
            
            program += "\tfloat theta = -textureScaleThetaPhi.z;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat phi = textureScaleThetaPhi.w;" + GLShaderBuilder.LINE_SEP;
            
            program += "\toutput.x = position.x - rect.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y = position.y - rect.y;" + GLShaderBuilder.LINE_SEP;            

            
            program += "\toutput.x *= rect.z;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= rect.w;" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x *= textureScaleThetaPhi.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= textureScaleThetaPhi.y;" + GLShaderBuilder.LINE_SEP;
			
            program += "\tscaledTexture.x -= offset.x;" + GLShaderBuilder.LINE_SEP;
            program += "\tscaledTexture.y -= offset.y;" + GLShaderBuilder.LINE_SEP;
            
            program += "\tpositionPass = position;" + GLShaderBuilder.LINE_SEP;

            program += "\talpha = color.a;";
            program += "\tfloat xrott = physicalPosition.x;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat yrott = physicalPosition.y*cos(theta) - physicalPosition.z*sin(theta);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat zrott = physicalPosition.y*sin(theta) + physicalPosition.z*cos(theta);" + GLShaderBuilder.LINE_SEP;
            
            program += "\t physicalPosition.x = xrott*cos(phi) - zrott*sin(phi);" + GLShaderBuilder.LINE_SEP;
            program += "\t physicalPosition.y = yrott;" + GLShaderBuilder.LINE_SEP;
            program += "\t physicalPosition.z = xrott*sin(phi) + zrott*cos(phi);" + GLShaderBuilder.LINE_SEP;
            program += "\t OUT.position = mul(state_matrix_mvp, physicalPosition);" + GLShaderBuilder.LINE_SEP;

            shaderBuilder.addEnvParameter("float4 rect");            
            shaderBuilder.addEnvParameter("float4 textureScaleThetaPhi");            
            shaderBuilder.addEnvParameter("float4 offset");            
            
            program = program.replace("output", shaderBuilder.useOutputValue("float4", "TEXCOORD0"));
            program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "POSITION"));
            program = program.replace("alpha", shaderBuilder.useOutputValue("float", "TEXCOORD1"));
            program = program.replace("scaledTexture", shaderBuilder.useOutputValue("float4", "TEXCOORD2"));
            program = program.replace("positionPass", shaderBuilder.useOutputValue("float4", "TEXCOORD3"));
            program = program.replace("color", shaderBuilder.useStandardParameter("float4", "COLOR"));
            shaderBuilder.addMainFragment(program);
            
            System.out.println("Corona VertexShader:\n" + shaderBuilder.getCode());
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }
    }
    
    public void changeRect(double xOffset, double yOffset, double xScale, double yScale){
    	this.xOffset = xOffset;
    	this.yOffset = yOffset;
    	this.xScale = xScale;
    	this.yScale = yScale;
    }

	public void changeTextureScale(double xTextureScale, double yTextureScale) {
		this.xTextureScale = xTextureScale;
		this.yTextureScale = yTextureScale;
	}

	public void setDefaultOffset(double x, double y){
		this.defaultXOffset = x;
		this.defaultYOffset = y;
	}
	public void changeAngles(double theta, double phi) {
		this.theta = theta;
		this.phi = phi;
	}	
}
