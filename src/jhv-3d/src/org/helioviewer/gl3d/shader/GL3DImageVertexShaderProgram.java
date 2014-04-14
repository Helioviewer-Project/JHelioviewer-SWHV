package org.helioviewer.gl3d.shader;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DImageVertexShaderProgram extends GLVertexShaderProgram {

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            String program = "\tphysicalPosition = physicalPosition;" + GLShaderBuilder.LINE_SEP;
            
            program += "\tfloat phi = -3.14159254/4.0;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat theta = 3.14/4;" + GLShaderBuilder.LINE_SEP;
 
            program += "\tfloat xrott = position.x;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat yrott = position.y*cos(theta) - position.z*sin(theta);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat zrott = position.y*sin(theta) + position.z*cos(theta);" + GLShaderBuilder.LINE_SEP;
            
            program += "\tfloat xrot = xrott*cos(phi) - zrott*sin(phi);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat yrot = yrott;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat zrot = xrott*sin(phi) + zrott*cos(phi);" + GLShaderBuilder.LINE_SEP;
            
            program += "\toutput.x = xrott - rect.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y = yrott - rect.y;" + GLShaderBuilder.LINE_SEP;            

            program += "\toutput.x *= rect.z;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= rect.w;" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x *= textureScale.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= textureScale.y;" + GLShaderBuilder.LINE_SEP;
			
            program += "\tscaledTexture.x -= offset.x;" + GLShaderBuilder.LINE_SEP;
            program += "\tscaledTexture.y -= offset.y;" + GLShaderBuilder.LINE_SEP;
            
            program += "\tpositionPass = position;" + GLShaderBuilder.LINE_SEP;

            program += "\talpha = color.a;";
            
            shaderBuilder.addEnvParameter("float4 rect");            
            shaderBuilder.addEnvParameter("float4 textureScale");            
            shaderBuilder.addEnvParameter("float4 offset");            
            
            program = program.replace("output", shaderBuilder.useOutputValue("float4", "TEXCOORD0"));
            program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "POSITION"));
            program = program.replace("alpha", shaderBuilder.useOutputValue("float", "TEXCOORD1"));
            program = program.replace("scaledTexture", shaderBuilder.useOutputValue("float4", "TEXCOORD2"));
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

	public void changeTextureScale(Vector2dDouble textureScale) {
		this.xTextureScale = textureScale.getX();
		this.yTextureScale = textureScale.getY();
	}
}
