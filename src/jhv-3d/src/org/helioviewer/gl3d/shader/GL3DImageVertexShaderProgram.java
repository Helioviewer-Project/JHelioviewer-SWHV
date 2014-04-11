package org.helioviewer.gl3d.shader;

import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DImageVertexShaderProgram extends GLVertexShaderProgram {

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {  	
        try {
            String program = "";//" OUT.position = mul(mat, position);" + GLShaderBuilder.LINE_SEP;
            program +=  "\tphysicalPosition = physicalPosition;" + GLShaderBuilder.LINE_SEP;
            program += "float xrot = position.x*cos(0.8) - position.z*sin(0.8);" + GLShaderBuilder.LINE_SEP;
            program += "float yrot = position.y;" + GLShaderBuilder.LINE_SEP;
            program += "float zrot = position.x*sin(0.8) + position.z*cos(0.8);" + GLShaderBuilder.LINE_SEP;
            
            program += "float zaxisxrot = 0.0*cos(0.8) - 1.0*sin(0.8);" + GLShaderBuilder.LINE_SEP;
            program += "float zaxisyrot = 0.0;" + GLShaderBuilder.LINE_SEP;
            program += "float zaxiszrot = 0.0*sin(0.8) + 1.0*cos(0.8);" + GLShaderBuilder.LINE_SEP;    
            
            program += "\tfloat4 v1 = float4(position.x, position.y, position.z, 0.0);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat4 v2 = float4(xrot, yrot, zrot, 0.0);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat4 v3 = float4(zaxisxrot, zaxisyrot, zaxiszrot, 0.0);" + GLShaderBuilder.LINE_SEP;
            program += "float projectionn = dot(v2,v3);" + GLShaderBuilder.LINE_SEP;

            
            program += "\toutput.z = xrot - offset.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.w = yrot - offset.y;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.x = xrot - rect.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y = yrot - rect.y;" + GLShaderBuilder.LINE_SEP;
            program += "\tif( zrot<-7000000 ){output.z=0.0;}else{output.z=1.0;}" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x *= rect.z;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= rect.w;" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x *= textureScale.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= textureScale.y;" + GLShaderBuilder.LINE_SEP;
			
            program += "\tscaledTexture.x -= offset.x;" + GLShaderBuilder.LINE_SEP;
            program += "\tscaledTexture.y -= offset.y;" + GLShaderBuilder.LINE_SEP;
            //program +="\tOUT.position = position;";
            
            shaderBuilder.addEnvParameter("float4 rect");            
            shaderBuilder.addEnvParameter("float4 textureScale");            
            shaderBuilder.addEnvParameter("float4 offset");            
            
            program = program.replace("output", shaderBuilder.useOutputValue("float4", "TEXCOORD0"));
            program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "POSITION"));
            program = program.replace("alpha", shaderBuilder.useOutputValue("float", "TEXCOORD1"));
            program = program.replace("scaledTexture", shaderBuilder.useOutputValue("float4", "TEXCOORD2"));
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

	public void changeTextureScale(double xTextureScale, double yTextureScale) {
		this.xTextureScale = xTextureScale;
		this.yTextureScale = yTextureScale;
	}

	public void setDefaultOffset(double x, double y){
		this.defaultXOffset = x;
		this.defaultYOffset = y;
	}
}
