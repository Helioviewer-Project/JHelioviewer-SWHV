package org.helioviewer.gl3d.shader;

import javax.media.opengl.GL;

import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class GL3DImageFragmentShaderProgram extends GLFragmentShaderProgram {

    private double cutOffRadius = 0.0f;
    private double theta;
    private double phi;
    private double xxTextureScale = 1.0;
    private double yyTextureScale = 1.0;
    private double differenceXTextureScale;
    private double differenceYTextureScale;
    private double differenceXOffset;
    private double differenceYOffset;
    private double differenceTheta;
    private double differencePhi;
    private double differenceXScale;
    private double differenceYScale;
    public GL3DImageFragmentShaderProgram() {
    }

    /**
     * Binds (= activates it) the shader, if it is not active so far.
     *
     * @param gl
     *            Valid reference to the current gl object
     */
    @Override
    public final void bind(GL gl) {
        bind(gl, shaderID, cutOffRadius, xxTextureScale, yyTextureScale, theta, phi);
    }

    private void bind(GL gl, int shader, double cutOffRadius, double xTextureScale, double yTextureScale, double theta, double phi) {
        shaderCurrentlyUsed = shader;
        gl.glBindProgramARB(target, shader);
        gl.glProgramLocalParameter4dARB(target, 0, cutOffRadius, 0.0f, 0.0f, 0.0f);
        gl.glProgramLocalParameter4dARB(target, 1, xTextureScale, yTextureScale, theta, phi);
        gl.glProgramLocalParameter4dARB(target, 2, differenceXTextureScale, differenceYTextureScale, differenceTheta, differencePhi);
    }

    /**
     * Pushes the shader currently in use onto a stack.
     *
     * This is useful to load another shader but still being able to restore the
     * old one, similar to the very common pushMatrix() in OpenGL.
     *
     * @param gl
     *            Valid reference to the current gl object
     * @see #popShader(GL)
     */
    public static void pushShader(GL gl) {
        shaderStack.push(shaderCurrentlyUsed);
        gl.glPushAttrib(GL.GL_CURRENT_BIT);
        // Log.debug("GL3DFragmentShaderProgram: pushShader, current="+shaderCurrentlyUsed);
    }

    /**
     * Takes the top of from the shader stack and binds it.
     *
     * This restores a shader pushed onto the stack earlier, similar to the very
     * common popMatrix() in OpenGL.
     *
     * @param gl
     *            Valid reference to the current gl object
     * @see #pushShader(GL)
     */
    public static void popShader(GL gl) {
        gl.glPopAttrib();
        Integer restoreShaderObject = shaderStack.pop();
        int restoreShader = restoreShaderObject == null ? 0 : restoreShaderObject.intValue();
        if (restoreShader >= 0) {
            //bind(gl, restoreShader, 0.0f, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            String program = "\tif(texcoord0.x<0.0||texcoord0.y<0.0||texcoord0.x>textureScaleThetaPhi.x||texcoord0.y>textureScaleThetaPhi.y){" + "\t\tOUT.color = float4(1.0,0.0,0.0,1.0);" + GLShaderBuilder.LINE_SEP + "\t}" + GLShaderBuilder.LINE_SEP;
            program = "\tif((texcoord4.x<0.0||texcoord4.y<0.0||texcoord4.x>diffTextureScaleThetaPhi.x||texcoord4.y>diffTextureScaleThetaPhi.y)){" + "\t\tOUT.color = float4(0.0,1.0,0.0,1.0);" + GLShaderBuilder.LINE_SEP + "\t}" + GLShaderBuilder.LINE_SEP;

            //program += "\tOUT.color.a=1.;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat theta = textureScaleThetaPhi.z;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat phi = textureScaleThetaPhi.w;" + GLShaderBuilder.LINE_SEP;

            program += "\tfloat zaxisxrott = 0.0;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat zaxisyrott = 0.0*cos(theta) - 1.0*sin(theta);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat zaxiszrott = 0.0*sin(theta) + 1.0*cos(theta);" + GLShaderBuilder.LINE_SEP;

            program += "\tfloat zaxisxrot = zaxisxrott*cos(phi) - zaxiszrott*sin(phi);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat zaxisyrot = zaxisyrott;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat zaxiszrot = zaxisxrott*sin(phi) + zaxiszrott*cos(phi);" + GLShaderBuilder.LINE_SEP;

            program += "\tfloat4 v1 = float4(position.x, position.y, position.z, 0.0);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat4 v2 = float4(zaxisxrot, zaxisyrot, zaxiszrot, 0.0);" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat projectionn = dot(v1,v2);" + GLShaderBuilder.LINE_SEP;

            program += "\tif((position.z!=0.0 && projectionn<-0.001) || (position.z==0.0 && position.x*position.x +position.y*position.y<0.9)){" + "\t\tdiscard;" + GLShaderBuilder.LINE_SEP + "\t}";

            shaderBuilder.addEnvParameter("float cutOffRadius");
            shaderBuilder.addEnvParameter("float4 textureScaleThetaPhi");
            shaderBuilder.addEnvParameter("float4 diffTextureScaleThetaPhi");

            program = program.replace("position", shaderBuilder.useStandardParameter("float4", "TEXCOORD3"));

            program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
            program = program.replace("textureCoordinate", shaderBuilder.useStandardParameter("float4", "TEXCOORD5"));
            shaderBuilder.useOutputValue("float4", "DEPTH");
            shaderBuilder.addMainFragment(program);
            System.out.println("GL3D Image Fragment Shader:\n" + shaderBuilder.getCode());
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }

    }

    public void changeTextureScale(double xTextureScale, double yTextureScale) {
        this.xxTextureScale = xTextureScale;
        this.yyTextureScale = yTextureScale;
    }

    public void setCutOffRadius(double cutOffRadius) {
        this.cutOffRadius = cutOffRadius;
    }

    public void changeAngles(double theta, double phi) {
        this.theta = theta;
        this.phi = phi;
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
