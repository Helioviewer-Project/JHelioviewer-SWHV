package org.helioviewer.viewmodel.view.opengl.shader;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class GL3DImageFragmentShaderProgram extends GLFragmentShaderProgram {

    private GLShaderBuilder builder;

    private int cutOffRadiusRef;
    private final double[] cutOffRadius = new double[4];
    private int phiParamRef;
    private final double[] phiParamFloat = new double[4];
    private int thetaParamRef;
    private final double[] thetaParamFloat = new double[4];

    /**
     * Binds (= activates it) the shader, if it is not active so far.
     *
     * @param gl
     *            Valid reference to the current gl object
     */
    @Override
    public final void bind(GL2 gl) {
        super.bind(gl);

        this.bindEnvVars(gl, cutOffRadiusRef, cutOffRadius);
        this.bindEnvVars(gl, phiParamRef, phiParamFloat);
        this.bindEnvVars(gl, thetaParamRef, thetaParamFloat);
    }

    /**
     * Pushes the shader currently in use onto a stack.
     *
     * This is useful to load another shader but still being able to restore the
     * old one, similar to the very common pushMatrix() in OpenGL2.
     *
     * @param gl
     *            Valid reference to the current gl object
     * @see #popShader(GL2)
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
     * @see #pushShader(GL2)
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
            String program = "\tif(texcoord0.x<0.0||texcoord0.y<0.0||texcoord0.x>1.0||texcoord0.y>1.0) {" + "discard;" + GLShaderBuilder.LINE_SEP + "\t}" + GLShaderBuilder.LINE_SEP;
            program += "float dotpos = dot(position.xyz, position.xyz);" + GLShaderBuilder.LINE_SEP;
            program += "\tif(dotpos<cutOffRadius.x*cutOffRadius.x ||dotpos>cutOffRadius.y*cutOffRadius.y ){discard;}" + GLShaderBuilder.LINE_SEP;
            program += "\tif((position.z==0.0 && dotpos<0.99)){" + "\t\tdiscard;" + GLShaderBuilder.LINE_SEP + "\t}" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat3x3 mat = float3x3( cos(phi), -sin(theta)*sin(phi), -cos(theta)*sin(phi), 0., cos(theta), -sin(theta), sin(phi), cos(phi)*sin(theta), cos(theta)*cos(phi));" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat3 zaxisrot = mul(mat,float3(0.,0.,1.));" + GLShaderBuilder.LINE_SEP;
            program += "\tfloat projectionn = dot(position.xyz,zaxisrot);" + GLShaderBuilder.LINE_SEP;
            program += "\tif((position.z!=0.0 && projectionn<-0.0)){" + "\t\tdiscard;" + GLShaderBuilder.LINE_SEP + "\t}" + GLShaderBuilder.LINE_SEP;

            cutOffRadiusRef = shaderBuilder.addEnvParameter("float4 cutOffRadius");

            phiParamRef = shaderBuilder.addEnvParameter("float phi");
            thetaParamRef = shaderBuilder.addEnvParameter("float theta");

            program = program.replace("position", shaderBuilder.useStandardParameter("float4", "TEXCOORD3"));
            program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));

            shaderBuilder.addMainFragment(program);
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }
    }

    public void setCutOffRadius(double innerCutOff, double outerCutOff) {
        cutOffRadius[0] = innerCutOff;
        cutOffRadius[1] = outerCutOff;
    }

    public void changeAngles(double theta, double phi) {
        thetaParamFloat[0] = theta;
        phiParamFloat[0] = phi;
    }

}
