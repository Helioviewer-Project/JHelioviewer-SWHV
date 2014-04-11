package org.helioviewer.gl3d.shader;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DImageCoronaVertexShaderProgram extends GLVertexShaderProgram {

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            String program = "\tphysicalPosition = physicalPosition;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.z = output.x-offset.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.w = output.y-offset.y;" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x -= rect.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y -= rect.y;" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x *= rect.z;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= rect.w;" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x *= textureScale.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= textureScale.y;" + GLShaderBuilder.LINE_SEP;

            program += "\tscaledTexture.x -= offset.x;" + GLShaderBuilder.LINE_SEP;
            program += "\tscaledTexture.y -= offset.y;" + GLShaderBuilder.LINE_SEP;
            program += "\talpha = color.a;";

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
