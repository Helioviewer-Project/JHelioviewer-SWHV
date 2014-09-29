package org.helioviewer.gl3d.shader;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class GL3DImageFragmentShaderProgram extends GLFragmentShaderProgram {

    private double cutOffRadius = 0.0f;
    private double outerCutOffRadius = 40.f;

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
    private int cutOffRadiusRef;
    private int outerCutOffRadiusRef;

    private int textureScaleThetaPhiRef;
    private int diffTextureScaleThetaPhiRef;
    private GLShaderBuilder builder;

    public GL3DImageFragmentShaderProgram() {
    }

    /**
     * Binds (= activates it) the shader, if it is not active so far.
     *
     * @param gl
     *            Valid reference to the current gl object
     */
    @Override
    public final void bind(GL2 gl) {
        bind(gl, shaderID, outerCutOffRadius, cutOffRadius, xxTextureScale, yyTextureScale, theta, phi);
    }

    private void bind(GL2 gl, int shader, double outerCutOffRadius, double cutOffRadius, double xTextureScale, double yTextureScale, double theta, double phi) {
        super.bind(gl);

        double[] cutOffRadiusFloat = this.builder.getEnvParameter(this.cutOffRadiusRef);
        double[] outerCutOffRadiusFloat = this.builder.getEnvParameter(this.outerCutOffRadiusRef);
        cutOffRadiusFloat[0] = (float) cutOffRadius;
        cutOffRadiusFloat[1] = 0f;
        cutOffRadiusFloat[2] = 0f;
        cutOffRadiusFloat[3] = 0f;
        outerCutOffRadiusFloat[0] = (float) outerCutOffRadius;
        outerCutOffRadiusFloat[1] = 0f;
        outerCutOffRadiusFloat[2] = 0f;
        outerCutOffRadiusFloat[3] = 0f;
        this.bindEnvVars(gl, this.cutOffRadiusRef, cutOffRadiusFloat);
        this.bindEnvVars(gl, this.outerCutOffRadiusRef, outerCutOffRadiusFloat);

        double[] textureScaleThetaPhiFloat = this.builder.getEnvParameter(this.textureScaleThetaPhiRef);
        textureScaleThetaPhiFloat[0] = xTextureScale;
        textureScaleThetaPhiFloat[1] = yTextureScale;
        textureScaleThetaPhiFloat[2] = theta;
        textureScaleThetaPhiFloat[3] = phi;
        this.bindEnvVars(gl, this.textureScaleThetaPhiRef, textureScaleThetaPhiFloat);

        double[] diffTextureScaleThetaPhiFloat = this.builder.getEnvParameter(this.diffTextureScaleThetaPhiRef);
        diffTextureScaleThetaPhiFloat[0] = differenceXTextureScale;
        diffTextureScaleThetaPhiFloat[1] = differenceYTextureScale;
        diffTextureScaleThetaPhiFloat[2] = differenceTheta;
        diffTextureScaleThetaPhiFloat[3] = differencePhi;
        this.bindEnvVars(gl, this.diffTextureScaleThetaPhiRef, diffTextureScaleThetaPhiFloat);
    }

    /**
     * Pushes the shader currently in use onto a stack.
     *
     * This is useful to load another shader but still being able to restore the
     * old one, similar to the very common pushMatrix() in OpenGL2.
     *
     * @param gl
     *            Valid reference to the current gl object
     * @see #popShader(GL)
     */
    public static void pushShader(GL2 gl) {
        shaderStack.push(shaderCurrentlyUsed);
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        // Log.debug("GL3DFragmentShaderProgram: pushShader, current="+shaderCurrentlyUsed);
    }

    /**
     * Takes the top of from the shader stack and binds it.
     *
     * This restores a shader pushed onto the stack earlier, similar to the very
     * common popMatrix() in OpenGL2.
     *
     * @param gl
     *            Valid reference to the current gl object
     * @see #pushShader(GL)
     */
    public static void popShader(GL2 gl) {
        gl.glPopAttrib();
        Integer restoreShaderObject = shaderStack.pop();
        int restoreShader = restoreShaderObject == null ? 0 : restoreShaderObject.intValue();
        if (restoreShader >= 0) {
            // bind(gl, restoreShader, 0.0f, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        this.builder = shaderBuilder;
        try {
            String program = "\tif(texcoord0.x<0.0||texcoord0.y<0.0||texcoord0.x>textureScaleThetaPhi.x||texcoord0.y>textureScaleThetaPhi.y){" + "discard;" + GLShaderBuilder.LINE_SEP + "\t}" + GLShaderBuilder.LINE_SEP;
            program += "float dotpos = dot(position.xyz, position.xyz);" + GLShaderBuilder.LINE_SEP;
            program += "\tif(dotpos<cutOffRadius.x*cutOffRadius.x ||dotpos>outerCutOffRadius.x*outerCutOffRadius.x ){discard;}" + GLShaderBuilder.LINE_SEP;
            program += "\tif((position.z==0.0 && dotpos<0.99)){" + "\t\tdiscard;" + GLShaderBuilder.LINE_SEP + "\t}" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat theta = textureScaleThetaPhi.z;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat phi = textureScaleThetaPhi.w;" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat3x3 mat = float3x3( cos(phi), -sin(theta)*sin(phi), -cos(theta)*sin(phi), 0., cos(theta), -sin(theta), sin(phi), cos(phi)*sin(theta), cos(theta)*cos(phi));" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat3 zaxisrot = mul(mat,float3(0.,0.,1.));" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat projectionn = dot(position.xyz,zaxisrot);" + GLShaderBuilder.LINE_SEP;
            program += "\tif((position.z!=0.0 && projectionn<-0.0)){" + "\t\tdiscard;" + GLShaderBuilder.LINE_SEP + "\t}" + GLShaderBuilder.LINE_SEP;

            this.cutOffRadiusRef = shaderBuilder.addEnvParameter("float4 cutOffRadius");
            this.outerCutOffRadiusRef = shaderBuilder.addEnvParameter("float4 outerCutOffRadius");

            this.textureScaleThetaPhiRef = shaderBuilder.addEnvParameter("float4 textureScaleThetaPhi");
            this.diffTextureScaleThetaPhiRef = shaderBuilder.addEnvParameter("float4 diffTextureScaleThetaPhi");

            program = program.replace("position", shaderBuilder.useStandardParameter("float4", "TEXCOORD3"));

            program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));

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

    public void setOuterCutOffRadius(double outerCutOffRadius) {
        this.outerCutOffRadius = outerCutOffRadius;
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
