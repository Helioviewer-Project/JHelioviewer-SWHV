package org.helioviewer.gl3d.shader;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DImageVertexShaderProgram extends GLVertexShaderProgram {
    private double theta;
    private double phi;
    private double xxTextureScale;
    private double yyTextureScale;
    private double differenceXTextureScale;
    private double differenceYTextureScale;
    private double differenceXOffset;
    private double differenceYOffset;
    private double differenceTheta;
    private double differencePhi;
    private double differenceXScale;
    private double differenceYScale;

    /**
     * {@inheritDoc}
     */
    @Override
    public final void bind(GL2 gl) {
        bind(gl, shaderID, xOffset, yOffset, xScale, yScale, xxTextureScale, yyTextureScale, defaultXOffset, defaultYOffset, theta, phi);
    }

    private void bind(GL2 gl, int shader, double xOffset, double yOffset, double xScale, double yScale, double xTextureScale, double yTextureScale, double defaultXOffset, double defaultYOffset, double theta, double phi) {
        if (shader != shaderCurrentlyUsed) {
            shaderCurrentlyUsed = shader;
            gl.glBindProgramARB(target, shader);
            gl.glProgramLocalParameter4dARB(target, 0, xOffset, yOffset, xScale, yScale);
            gl.glProgramLocalParameter4dARB(target, 1, xTextureScale, yTextureScale, theta, phi);
            gl.glProgramLocalParameter4dARB(target, 2, defaultXOffset, defaultYOffset, 0, 0);
            gl.glProgramLocalParameter4dARB(target, 3, differenceXOffset, differenceYOffset, differenceXScale, differenceYScale);
            gl.glProgramLocalParameter4dARB(target, 4, differenceXTextureScale, differenceYTextureScale, differenceTheta, differencePhi);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            String program = "";

            program += "\tif(abs(position.x)>1.1){" + GLShaderBuilder.LINE_SEP;
            //Corona
            program += "\tfloat theta = textureScaleThetaPhi.z;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat phi = textureScaleThetaPhi.w;" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x = position.x - rect.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y = -position.y - rect.y;" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x *= rect.z;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= rect.w;" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x *= textureScaleThetaPhi.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= textureScaleThetaPhi.y;" + GLShaderBuilder.LINE_SEP;
            //Difference Image

            program += "\tdifferenceOutput.x = position.x - differenceRect.x;" + GLShaderBuilder.LINE_SEP;
            program += "\tdifferenceOutput.y = -position.y - differenceRect.y;" + GLShaderBuilder.LINE_SEP;

            program += "\tdifferenceOutput.x *= differenceRect.z;" + GLShaderBuilder.LINE_SEP;
            program += "\tdifferenceOutput.y *= differenceRect.w;" + GLShaderBuilder.LINE_SEP;

            program += "\tdifferenceOutput.x *= diffTextureScaleThetaPhi.x;" + GLShaderBuilder.LINE_SEP;
            program += "\tdifferenceOutput.y *= diffTextureScaleThetaPhi.y;" + GLShaderBuilder.LINE_SEP;

            program += "\tpositionPass = position;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat3x3 mat = float3x3(cos(phi), -sin(theta)*sin(phi), -sin(phi)*cos(theta), 0, cos(theta), -sin(theta), sin(phi), cos(phi)*sin(theta), cos(theta)*cos(phi));" + GLShaderBuilder.LINE_SEP;
            program += "\tphysicalPosition.xyz = mul(mat, physicalPosition.xyz);" + GLShaderBuilder.LINE_SEP;
            program += "\t OUT.position = mul(state_matrix_mvp, physicalPosition);" + GLShaderBuilder.LINE_SEP;
            program += "\t}" + GLShaderBuilder.LINE_SEP;
            program += "\telse{" + GLShaderBuilder.LINE_SEP;
            //Solar disk
            //Image
            program += "\tfloat theta = -textureScaleThetaPhi.z;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat phi = -textureScaleThetaPhi.w;" + GLShaderBuilder.LINE_SEP;
            program += "\tpositionPass = physicalPosition;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat3x3 mat = float3x3(cos(phi), 0, -sin(phi), -sin(theta)*sin(phi), cos(theta), -sin(theta)*cos(phi), cos(theta)*sin(phi), sin(theta), cos(theta)*cos(phi));" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat3 rot = mul(mat, physicalPosition.xyz);" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x = rot.x - rect.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y = -rot.y - rect.y;" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x *= rect.z;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= rect.w;" + GLShaderBuilder.LINE_SEP;

            program += "\toutput.x *= textureScaleThetaPhi.x;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.y *= textureScaleThetaPhi.y;" + GLShaderBuilder.LINE_SEP;
            //Difference Image

            program += "\tfloat differencetheta = -textureScaleThetaPhi.z;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat differencephi = -diffTextureScaleThetaPhi.w;" + GLShaderBuilder.LINE_SEP;
            program += "\tmat = float3x3(cos(differencephi), 0, -sin(differencephi), -sin(differencetheta)*sin(differencephi), cos(differencetheta), -sin(differencetheta)*cos(differencephi), cos(differencetheta)*sin(differencephi), sin(differencetheta), cos(differencetheta)*cos(differencephi));" + GLShaderBuilder.LINE_SEP;
            program += "\trot = mul(mat, physicalPosition.xyz);" + GLShaderBuilder.LINE_SEP;

            program += "\tdifferenceOutput.x = rot.x - differenceRect.x;" + GLShaderBuilder.LINE_SEP;
            program += "\tdifferenceOutput.y = -rot.y - differenceRect.y;" + GLShaderBuilder.LINE_SEP;

            program += "\tdifferenceOutput.x *= differenceRect.z;" + GLShaderBuilder.LINE_SEP;
            program += "\tdifferenceOutput.y *= differenceRect.w;" + GLShaderBuilder.LINE_SEP;

            program += "\tdifferenceOutput.x *= diffTextureScaleThetaPhi.x;" + GLShaderBuilder.LINE_SEP;
            program += "\tdifferenceOutput.y *= diffTextureScaleThetaPhi.y;" + GLShaderBuilder.LINE_SEP;
            program += "}" + GLShaderBuilder.LINE_SEP;

            shaderBuilder.addEnvParameter("float4 rect");
            shaderBuilder.addEnvParameter("float4 textureScaleThetaPhi");
            shaderBuilder.addEnvParameter("float4 offset");
            shaderBuilder.addEnvParameter("float4 differenceRect");
            shaderBuilder.addEnvParameter("float4 diffTextureScaleThetaPhi");

            program = program.replace("output", shaderBuilder.useOutputValue("float4", "TEXCOORD0"));
            program = program.replace("physicalPosition", shaderBuilder.useStandardParameter("float4", "POSITION"));
            program = program.replace("differenceOutput", shaderBuilder.useOutputValue("float4", "TEXCOORD4"));

            program = program.replace("positionPass", shaderBuilder.useOutputValue("float4", "TEXCOORD3"));
            program = program.replace("color", shaderBuilder.useStandardParameter("float4", "COLOR"));
            shaderBuilder.addMainFragment(program);

            // System.out.println("VertexShader:\n" + shaderBuilder.getCode());
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }
    }

    public void changeRect(double xOffset, double yOffset, double xScale, double yScale) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.xScale = xScale;
        this.yScale = yScale;
    }

    public void changeAngles(double theta, double phi) {
        this.theta = theta;
        this.phi = phi;
    }

    public void changeTextureScale(double scaleX, double scaleY) {
        this.xxTextureScale = scaleX;
        this.yyTextureScale = scaleY;
    }

    public void changeDifferenceTextureScale(double scaleX, double scaleY) {
        this.differenceXTextureScale = scaleX;
        this.differenceYTextureScale = scaleY;
    }

    public void changeDifferenceAngles(double theta, double phi) {
        this.differenceTheta = theta;
        this.differencePhi = phi;
    }

    public void setDifferenceRect(double differenceXOffset, double differenceYOffset, double differenceXScale, double differenceYScale) {
        this.differenceXOffset = differenceXOffset;
        this.differenceYOffset = differenceYOffset;
        this.differenceXScale = differenceXScale;
        this.differenceYScale = differenceYScale;
    }
}
