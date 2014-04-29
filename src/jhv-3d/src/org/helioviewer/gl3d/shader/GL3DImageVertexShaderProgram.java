package org.helioviewer.gl3d.shader;

import javax.media.opengl.GL;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DImageVertexShaderProgram extends GLVertexShaderProgram {
	private double theta;
	private double phi;
	private double xxTextureScale;
	private double yyTextureScale;

	
    /**
     * {@inheritDoc}
     */
    public final void bind(GL gl) {
    	GLTextureHelper th =  new GLTextureHelper();
    	bind(gl, shaderID, xOffset, yOffset, xScale, yScale, xxTextureScale, yyTextureScale, defaultXOffset, defaultYOffset, theta, phi);
    }	
    private static void bind(GL gl, int shader, double xOffset, double yOffset, double xScale, double yScale, double xTextureScale, double yTextureScale, double defaultXOffset, double defaultYOffset, double theta, double phi) {
        if (shader != shaderCurrentlyUsed) {
            shaderCurrentlyUsed = shader;
            // Log.debug("GLVertexShaderProgram.bind shader="+shader);
            //gl.glActiveTexture(GL.GL_TEXTURE0);
            //gl.glBindTexture(GL.GL_TEXTURE_2D, 1);
            gl.glBindProgramARB(target, shader);
            gl.glProgramLocalParameter4dARB(target, 0, xOffset, yOffset, xScale, yScale);
            gl.glProgramLocalParameter4dARB(target, 1,xTextureScale, yTextureScale,theta, phi);
            gl.glProgramLocalParameter4dARB(target, 2,defaultXOffset, defaultYOffset, 0,0);

        }
    }
	
    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            String program = "\tphysicalPosition = physicalPosition;" + GLShaderBuilder.LINE_SEP;
            
            program += "\tif(abs(position.x)>1.1){"+ GLShaderBuilder.LINE_SEP;
            program += "\tfloat theta = -textureScaleThetaPhi.z;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat phi = textureScaleThetaPhi.w;" + GLShaderBuilder.LINE_SEP;
            
            program += "\toutput.x = position.x - rect.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y = -position.y - rect.y;" + GLShaderBuilder.LINE_SEP;            

            
            program += "\toutput.x *= rect.z;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= rect.w;" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x *= textureScaleThetaPhi.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= textureScaleThetaPhi.y;" + GLShaderBuilder.LINE_SEP;
            //program += "\toutput.y = 1.-output.y;" + GLShaderBuilder.LINE_SEP;

            program += "\tpositionPass = position;" + GLShaderBuilder.LINE_SEP;

            program += "\tfloat xrott = physicalPosition.x;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat yrott = physicalPosition.y*cos(theta) - physicalPosition.z*sin(theta);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat zrott = physicalPosition.y*sin(theta) + physicalPosition.z*cos(theta);" + GLShaderBuilder.LINE_SEP;
            
            program += "\t physicalPosition.x = xrott*cos(phi) - zrott*sin(phi);" + GLShaderBuilder.LINE_SEP;
            program += "\t physicalPosition.y = yrott;" + GLShaderBuilder.LINE_SEP;
            program += "\t physicalPosition.z = xrott*sin(phi) + zrott*cos(phi);" + GLShaderBuilder.LINE_SEP;
            program += "\t OUT.position = mul(state_matrix_mvp, physicalPosition);" + GLShaderBuilder.LINE_SEP;
            program += "\t OUT.position.y = OUT.position.y;" + GLShaderBuilder.LINE_SEP;          

            program += "\t}"+ GLShaderBuilder.LINE_SEP;
            program += "\telse{"+ GLShaderBuilder.LINE_SEP;
            program += "\tfloat theta = -textureScaleThetaPhi.z;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat phi = -textureScaleThetaPhi.w;" + GLShaderBuilder.LINE_SEP;
            program += "\tpositionPass = physicalPosition;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat xrot = physicalPosition.x*cos(phi) - physicalPosition.z*sin(phi);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat yrot = physicalPosition.y;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat zrot = physicalPosition.x*sin(phi) + physicalPosition.z*cos(phi);" + GLShaderBuilder.LINE_SEP;
            
            program += "\tfloat xrott = xrot;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat yrott = yrot*cos(theta) - zrot*sin(theta);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat zrott = yrot*sin(theta) + zrot*cos(theta);" + GLShaderBuilder.LINE_SEP;       
            program += "\toutput.x = xrott - rect.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y = -yrott - rect.y;" + GLShaderBuilder.LINE_SEP;            

            program += "\toutput.x *= rect.z;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= rect.w;" + GLShaderBuilder.LINE_SEP;
            //program += "\toutput.y = 1.-output.y;" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x *= textureScaleThetaPhi.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= textureScaleThetaPhi.y;" + GLShaderBuilder.LINE_SEP;    
            program += "\t OUT.position.y = OUT.position.y;" + GLShaderBuilder.LINE_SEP;          

            program += "}"+ GLShaderBuilder.LINE_SEP;

			            
            
            shaderBuilder.addEnvParameter("float4 rect");            
            shaderBuilder.addEnvParameter("float4 textureScaleThetaPhi");            
            shaderBuilder.addEnvParameter("float4 offset");            
            
            program = program.replace("output", shaderBuilder.useOutputValue("float4", "TEXCOORD0"));
            program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "POSITION"));
            program = program.replace("positionPass", shaderBuilder.useOutputValue("float4", "TEXCOORD3"));
            program = program.replace("color", shaderBuilder.useStandardParameter("float4", "COLOR"));
            shaderBuilder.addMainFragment(program);
            
            System.out.println("VertexShader:\n" + shaderBuilder.getCode());
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

	public void setDefaultOffset(double x, double y){
		this.defaultXOffset = x;
		this.defaultYOffset = y;
	}

	public void changeAngles(double theta, double phi) {
		this.theta = theta;
		this.phi = phi;
	}
	public void changeTextureScale(double scaleX, double scaleY) {
		this.xxTextureScale = scaleX;
		this.yyTextureScale = scaleY;
	}
}
