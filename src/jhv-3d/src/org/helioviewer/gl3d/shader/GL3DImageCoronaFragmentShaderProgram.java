    package org.helioviewer.gl3d.shader;


import javax.media.opengl.GL;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class GL3DImageCoronaFragmentShaderProgram extends GLFragmentShaderProgram {

	private int h;
    protected double alpha = 1.0f;
    protected double cutOffRadius = 0.0f;
	private double xTextureScale;
	private double yTextureScale;
	
    /**
     * Binds (= activates it) the shader, if it is not active so far.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    public final void bind(GL gl) {
    	bind(gl, shaderID, alpha, cutOffRadius,  this.xTextureScale,  this.yTextureScale);
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
            bind(gl, restoreShader,0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Binds (= activates it) the given shader, if it is not active so far.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    private static void bind(GL gl, int shader, double alpha, double cutOffRadius, double xTextureScale, double yTextureScale) {
        if (shader != shaderCurrentlyUsed) {
            shaderCurrentlyUsed = shader;
            gl.glBindProgramARB(target, shader);
            gl.glProgramLocalParameter4dARB(target, 1, alpha, 0.0f, 0.0f, 0.0f);
            gl.glProgramLocalParameter4dARB(target, 0, cutOffRadius, 0.0f, 0.0f, 0.0f);
            gl.glProgramLocalParameter4dARB(target, 2, xTextureScale, yTextureScale, 0, 0);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
        	String program = "\tif(texcoord0.x<0.0||texcoord0.y<0.0||texcoord0.x>textureScale.x||texcoord0.y>textureScale.y){"+ GLShaderBuilder.LINE_SEP 
        			+ "\t\tOUT.color = float4(0.0,1.0,0.0,1.0);" + GLShaderBuilder.LINE_SEP
        			+ "\t}";        	
        	program += "if(position.x*position.x+position.y*position.y+position.z*position.z<=1.0){discard;}" + GLShaderBuilder.LINE_SEP; 
        	program += "\toutput.a = 1.0;" + GLShaderBuilder.LINE_SEP;            
            program += "\tfloat2 texture;" + GLShaderBuilder.LINE_SEP;
            //program += "\ttexture.x = textureCoordinate.z - 0.5;" + GLShaderBuilder.LINE_SEP;
            //program += "\ttexture.y = textureCoordinate.w - 0.5;" + GLShaderBuilder.LINE_SEP;
            //program += "\toutput.a *= 1.0;" + GLShaderBuilder.LINE_SEP;
            //program += "\toutput.a *= step(cutOffRadius, length(texture));" + GLShaderBuilder.LINE_SEP;
            shaderBuilder.addEnvParameter("float cutOffRadius");
            shaderBuilder.addEnvParameter("float alpha");
            shaderBuilder.addEnvParameter("float4 textureScale");            
            
            program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
            program = program.replace("textureCoordinate", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
            program = program.replace("position", shaderBuilder.useStandardParameter("float4", "TEXCOORD3"));
            shaderBuilder.addMainFragment(program);
            System.out.println("CORONA Fragment Shader:\n" + shaderBuilder.getCode());
            
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }

    }
        
    public void changeAlpha(double alpha){
    	this.alpha = alpha;
    }
    
    public void setCutOffRadius(double cutOffRadius){
    	this.cutOffRadius = cutOffRadius;
    }
    
	public void changeTextureScale(double xTextureScale, double yTextureScale) {
		this.xTextureScale = xTextureScale;
		this.yTextureScale = yTextureScale;
	}    
}
