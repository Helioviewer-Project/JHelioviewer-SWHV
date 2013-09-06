package org.helioviewer.viewmodel.view.opengl.shader;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;

/**
 * Class representing a part of a OpenGL texture coordinate.
 * 
 * <p>
 * This class provides the capability to set only a part of an actual texture
 * coordinate. Therefore, it manages a static array, shared by all
 * GLTextureCoordinates. Thus, to work correctly, pure OpenGL-calls to set the
 * texture coordinate are forbidden.
 * 
 * 
 * <p>
 * Also, this object is supposed to provide the capability to write the values
 * assigned to it. To prevent overlaps, only use the implementations returned by
 * other classes, such as {@link GLShaderBuilder} and
 * {@link org.helioviewer.viewmodel.view.opengl.GLTextureHelper}, which manage
 * the utilization of the entire set. Overriding this abstract class may lead to
 * unwanted behavior.
 * 
 * @author Markus Langenberg
 */
public abstract class GLTextureCoordinate {

    private int target;
    private int offset;
    private int length;
    private String identifier;

    private static float currentValues[][];

    /**
     * Default constructor.
     * 
     * @param target
     *            OpenGL constant, representing the texture coordinate, such as
     *            GL_TEXTURE0 or GL_TEXTURE3
     * @param offset
     *            The offset within the 4d-coordinate. Has to be within [0, 3]
     * @param length
     *            The length of the subset. Has to be within [1, 4]
     * @param identifier
     *            The identifier for this coordinate, which has to be used in
     *            shader programs
     */
    protected GLTextureCoordinate(int target, int offset, int length, String identifier) {
        this.target = target - GL.GL_TEXTURE0;
        this.offset = offset;
        this.length = length;
        this.identifier = identifier;

        if (this.target >= currentValues.length || target < 0) {
            throw new IllegalArgumentException("Target refers to an invalid texture coordinate: GL_TEXTURE" + this.target + ".");
        }

        if (offset < 0 || offset > 3) {
            throw new IllegalArgumentException("Offset out of bounds: " + offset + ".");
        }

        if (length < 1 || length > 4) {
            throw new IllegalArgumentException("Length out of bounds: " + length + ".");
        }

        if (offset + length > 4) {
            throw new IllegalArgumentException("Combination of offset and length out of bounds: " + offset + ", " + length + ".");
        }
    }

    /**
     * Initializes this class.
     * 
     * This functions has to be called before using the class.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    public static void init(GL gl) {
        Log.debug(">> GLTextureCoordinate.init(GL) > Initialize GLTextureCoordinate");
        int tmp[] = new int[1];
        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_COORDS, tmp, 0);

        currentValues = new float[tmp[0]][4];
    }

    /**
     * Sets the values managed by this object.
     * 
     * This function may only be called for 1d-subsets.
     * 
     * @param gl
     *            Valid reference to the current gl object
     * @param value1
     *            First component to set
     */
    public void setValue(GL gl, float value1) {
        if (length != 1) {
            throw new IllegalArgumentException("Calling this function is not valid for GLTextureCoordinate with lenght " + length + ".");
        }

        currentValues[target][offset] = value1;
        gl.glMultiTexCoord4fv(target + GL.GL_TEXTURE0, currentValues[target], 0);
    }

    /**
     * Sets the values managed by this object.
     * 
     * This function may only be called for 2d-subsets.
     * 
     * @param gl
     *            Valid reference to the current gl object
     * @param value1
     *            First component to set
     * @param value2
     *            Second component to set
     */
    public void setValue(GL gl, float value1, float value2) {
        if (length != 2) {
            throw new IllegalArgumentException("Calling this function is not valid for GLTextureCoordinate with lenght " + length + ".");
        }

        currentValues[target][offset] = value1;
        currentValues[target][offset + 1] = value2;
        gl.glMultiTexCoord4fv(target + GL.GL_TEXTURE0, currentValues[target], 0);
    }

    /**
     * Sets the values managed by this object.
     * 
     * This function may only be called for 3d-subsets.
     * 
     * @param gl
     *            Valid reference to the current gl object
     * @param value1
     *            First component to set
     * @param value2
     *            Second component to set
     * @param value3
     *            Third component to set
     */
    public void setValue(GL gl, float value1, float value2, float value3) {
        if (length != 3) {
            throw new IllegalArgumentException("Calling this function is not valid for GLTextureCoordinate with lenght " + length + ".");
        }

        currentValues[target][offset] = value1;
        currentValues[target][offset + 1] = value2;
        currentValues[target][offset + 2] = value3;
        gl.glMultiTexCoord4fv(target + GL.GL_TEXTURE0, currentValues[target], 0);
    }

    /**
     * Sets the values managed by this object.
     * 
     * This function may only be called for 4d-subsets.
     * 
     * @param gl
     *            Valid reference to the current gl object
     * @param value1
     *            First component to set
     * @param value2
     *            Second component to set
     * @param value3
     *            Third component to set
     * @param value4
     *            Forth component to set
     */
    public void setValue(GL gl, float value1, float value2, float value3, float value4) {
        if (length != 4) {
            throw new IllegalArgumentException("Calling this function is not valid for GLTextureCoordinate with lenght " + length + ".");
        }

        currentValues[target][offset] = value1;
        currentValues[target][offset + 1] = value2;
        currentValues[target][offset + 2] = value3;
        currentValues[target][offset + 3] = value4;
        gl.glMultiTexCoord4fv(target + GL.GL_TEXTURE0, currentValues[target], 0);
    }

    /**
     * Sets the values managed by this object.
     * 
     * The length of the vector has to be equal to the length of the subset
     * represented by the is object.
     * 
     * @param gl
     *            Valid reference to the current gl object
     * @param values
     *            Array containing all components to set
     */
    public void setValue(GL gl, float[] values) {
        if (length != values.length) {
            throw new IllegalArgumentException("'values' is supposed to have a length of " + length + ".");
        }

        for (int i = 0; i < length; i++) {
            currentValues[target][offset + i] = values[i];
        }
        gl.glMultiTexCoord4fv(target + GL.GL_TEXTURE0, currentValues[target], 0);
    }

    /**
     * Returns the identifier for this coordinate, which has to be used in
     * shader programs.
     * 
     * @return The identifier for this coordinate, which has to be used in
     *         shader programs
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the identifier for this coordinate, which has to be used in
     * shader programs.
     * 
     * <p>
     * In addition to the basic version of this function, it provides the
     * capability to repeat the dimension. e.g. for a 1d-subset, setting
     * repeatDimension to 3 will return <i>identifier.ddd</i> instead of
     * <i>identifier.d</i>. This might be useful for some shader programs.
     * 
     * @param repeatDimension
     *            Number of repetitions of the dimension
     * @return The identifier for this coordinate, which has to be used in
     *         shader programs
     */
    public String getIdentifier(int repeatDimension) {
        String output = identifier;
        String repeat = identifier.substring(identifier.length() - length);

        for (int i = 1; i < repeatDimension; i++) {
            output += repeat;
        }

        return output;
    }
}
