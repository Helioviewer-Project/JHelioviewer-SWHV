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
            program += "float phi = 0.8;" + GLShaderBuilder.LINE_SEP;
            program += "float theta = 0.1;" + GLShaderBuilder.LINE_SEP;

            program += "float xrot = position.x*cos(phi) - position.z*sin(phi);" + GLShaderBuilder.LINE_SEP;
            program += "float yrot = position.y;" + GLShaderBuilder.LINE_SEP;
            program += "float zrot = position.x*sin(phi) + position.z*cos(phi);" + GLShaderBuilder.LINE_SEP;
            
            program += "float xrott = xrot;" + GLShaderBuilder.LINE_SEP;
            program += "float yrott = yrot*cos(theta) - zrot*sin(theta);" + GLShaderBuilder.LINE_SEP;
            program += "float zrott = yrot*sin(theta) + zrot*cos(theta);" + GLShaderBuilder.LINE_SEP;            
            
            
            program += "float zaxisxrot = 0.0*cos(phi) - 1.0*sin(phi);" + GLShaderBuilder.LINE_SEP;
            program += "float zaxisyrot = 0.0;" + GLShaderBuilder.LINE_SEP;
            program += "float zaxiszrot = 0.0*sin(phi) + 1.0*cos(phi);" + GLShaderBuilder.LINE_SEP;  
            
            program += "float zaxisxrott = zaxisxrot;" + GLShaderBuilder.LINE_SEP;
            program += "float zaxisyrott = zaxisyrot*cos(theta) - zaxisxrot*sin(theta);" + GLShaderBuilder.LINE_SEP;
            program += "float zaxiszrott = zaxisyrot*sin(theta) + zaxisxrot*cos(theta);" + GLShaderBuilder.LINE_SEP;             
            
            program += "\tfloat4 v1 = float4(position.x, position.y, position.z, 0.0);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat4 v2 = float4(xrot, yrot, zrot, 0.0);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat4 v3 = float4(zaxisxrott, zaxisyrott, zaxiszrott, 0.0);" + GLShaderBuilder.LINE_SEP;
            program += "float projectionn = dot(v1,v3);" + GLShaderBuilder.LINE_SEP;
			
            
            program += "\toutput.z = xrott - offset.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.w = yrott - offset.y;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.x = xrott - rect.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y = yrott - rect.y;" + GLShaderBuilder.LINE_SEP;
            //program += "float vv = (-zrott-6.957e);"+ GLShaderBuilder.LINE_SEP;
            program += "if( projectionn>-100 ){output.z=0.0;}else{output.z=1.0;}" + GLShaderBuilder.LINE_SEP;

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
