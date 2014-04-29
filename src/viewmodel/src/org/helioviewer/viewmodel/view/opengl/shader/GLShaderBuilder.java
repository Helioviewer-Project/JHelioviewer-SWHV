package org.helioviewer.viewmodel.view.opengl.shader;

import java.util.HashMap;
import java.util.LinkedList;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;

/**
 * Class to build new OpenGL shader build in Cg.
 * 
 * <p>
 * Since a graphics card only supports one active vertex shader and one active
 * fragment shader at a time, it is necessary to merge all operations that
 * should be applied to one image into one big, final shader.
 * 
 * <p>
 * Therefore, this class provides functions to add different input and output
 * parameter to the current shader as well as adding code pieces. After adding
 * all different parts of the shader, they can be compiled into one single
 * shader. For programming, Cg (C for Graphics, developed by NVIDIA) is used as
 * the shading language. To use this class, you have to be familiar with Cg.
 * 
 * <p>
 * When adding code pieces, it is highly recommended to use a specific prefix,
 * so that your names do not collide with names from other parts.
 * 
 * <p>
 * For further informations about programming shaders, refer to the Cg User
 * Manual.
 * 
 * @author Markus Langenberg
 * 
 */
public class GLShaderBuilder {

    public static final String LINE_SEP = System.getProperty("line.separator");

    private static GLShaderHelper shaderHelper = new GLShaderHelper();
    private static int maxTexCoords = 32;
    private static int maxTexUnits = 32;
    private static int maxConstantRegisters = Integer.MAX_VALUE;
    private static int maxVertexAttributes = Integer.MAX_VALUE;

    private static char[] coordinateDimension = { 'x', 'y', 'z', 'w' };

    private LinkedList<String> outputStruct = new LinkedList<String>();
    private LinkedList<String> standardParameterList = new LinkedList<String>();
    private LinkedList<String> parameterList = new LinkedList<String>();
    private HashMap<String, String> outputTypes = new HashMap<String, String>();
    private HashMap<String, String> outputInit = new HashMap<String, String>();
    private HashMap<String, String> standardParameterTypes = new HashMap<String, String>();

    private int[] componentsAvailableInTexCoord;

    private int nextTexUnit = 1;
    private int nextConstantRegister = 0;
    private int nextVertexAttribute = 0;
    private static int highestTexCoordEverUsed = 0;

    private GL gl;
    private int shaderID;
    private int type;

    private String functions = "";
    private String mainBody = "";

    /**
     * Initializes the static members of the shader builder. This has to happen
     * during runtime, since many of this values are driver dependent.
     * 
     * @param gl
     *            current GL object
     */
    public static void initShaderBuilder(GL gl) {
        Log.debug(">> GLShaderBuilder.initShaderBuilder(GL) > Initialize shader builder");
        int tmp[] = new int[1];

        tmp[0] = 0;
        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_IMAGE_UNITS, tmp, 0);
        maxTexUnits = tmp[0];
        Log.debug(">> GLShaderBuilder.initShaderBuilder(GL) > max texture image units: " + maxTexUnits);

        tmp[0] = 0;
        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_COORDS, tmp, 0);
        maxTexCoords = tmp[0];
        Log.debug(">> GLShaderBuilder.initShaderBuilder(GL) > max texture coords: " + maxTexCoords);

        tmp[0] = 0;
        gl.glGetIntegerv(GL.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS_ARB, tmp, 0);
        maxConstantRegisters = tmp[0];
        Log.debug(">> GLShaderBuilder.initShaderBuilder(GL) > max fragment uniform components arb: " + maxConstantRegisters);

        tmp[0] = 0;
        gl.glGetIntegerv(GL.GL_MAX_VERTEX_ATTRIBS_ARB, tmp, 0);
        maxVertexAttributes = tmp[0];
        Log.debug(">> GLShaderBuilder.initShaderBuilder(GL) > max vertex attributes arb: " + maxVertexAttributes);

        GLTextureCoordinate.init(gl);
    }

    /**
     * Generates a new GLShaderBuilder.
     * 
     * @param gl
     *            Valid reference to the current gl object
     * @param type
     *            the shader type, has to be GL_FRAGMENT_PROGRAM_ARB or
     *            GL_VERTEX_PROGRAM_ARB
     */
    public GLShaderBuilder(GL gl, int type) {
        this(gl, type, false);
    }

    public GLShaderBuilder(GL gl, int type, boolean standalone) {
        this.gl = gl;
        this.type = type;

        componentsAvailableInTexCoord = new int[maxTexCoords];

        // Reserved for actual texture coordinate, scaling and physical position
        componentsAvailableInTexCoord[0] = 0;

        // Rest is free
        for (int i = 1; i < maxTexCoords; i++) {
            componentsAvailableInTexCoord[i] = 4;
        }

        shaderID = !standalone ? shaderHelper.genShaderID(gl) : shaderHelper.genStandaloneShaderID(gl);
    }

    /**
     * Returns the GL object assigned to this ShaderBuilder. Warning: Only use
     * this function within a build*Shader-method, otherwise the object might
     * not be valid.
     * 
     * @return Reference to the current gl object
     */
    public GL getGL() {
        return gl;
    }

    /**
     * Returns OpenGL shader id assigned to this shader. To use this shader,
     * bind shader id.
     * 
     * @return assigned shader id
     */
    public int getShaderID() {
        return shaderID;
    }

    /**
     * Advises the GLShaderBuilder to provide this output value in its output.
     * Typical values are for example POSITION, COLOR or TEXCOORD0
     * 
     * @param type
     *            the type of the output value to use (since this is not a
     *            1:1-binding to the value)
     * @param value
     *            the output value to use
     * @return the name, how to address this value inside the code, usually
     *         "OUT." + lowerCase(value)
     * @throws GLBuildShaderException
     *             if the output value is already used, but with different type
     * @see #useOutputValue(String, String, String)
     */
    public String useOutputValue(String type, String value) throws GLBuildShaderException {
        value = value.toUpperCase();

        if (outputStruct.contains(value)) {
            if (!type.equalsIgnoreCase(outputTypes.get(value))) {
                throw new GLBuildShaderException("Conflict between output types for " + value + ": " + type + " vs. " + outputTypes.get(value));
            }
        } else {
            outputStruct.add(value);
            outputTypes.put(value, type);
        }

        return "OUT." + value.toLowerCase();
    }

    /**
     * Advises the GLShaderBuilder to provide this output value in its output,
     * initialing it the given value. Typical values are for example POSITION,
     * COLOR or TEXCOORD0
     * 
     * @param type
     *            the type of the output value to use (since this is not a
     *            1:1-binding to the value)
     * @param value
     *            the output value to use
     * @param init
     *            the value to initialize this variable
     * @return the name, how to address this value inside the code, usually
     *         "OUT." + lowerCase(value)
     * @throws GLBuildShaderException
     *             if the output value is already used, but with different type
     * @see #useOutputValue(String, String)
     */
    public String useOutputValue(String type, String value, String init) throws GLBuildShaderException {
        value = value.toUpperCase();

        if (init != null && init.trim() != "") {
            if (outputInit.containsKey(value)) {
                if (!outputInit.get(value).equalsIgnoreCase(init)) {
                    throw new GLBuildShaderException("Conflict between output initial values for " + value + ": " + init + " vs. " + outputInit.get(value));
                }
            } else {
                outputInit.put(value, init);
            }
        }

        return useOutputValue(type, value);
    }

    /**
     * Advises the GLShaderBuilder to provide this input parameter in its
     * parameter list. This function should be used for all kind of parameters,
     * which do not have a special function and may be used by multiple code
     * fragments, such as COLOR, TEXUNIT0, TEXCOORD0 or all members of state.
     * 
     * @param type
     *            the type of the standard parameter to use (since this is not a
     *            1:1-binding to the parameter)
     * @param parameter
     *            the standard parameter to use
     * @return the name of the input parameter, usually lowerCase(value)
     * @throws GLBuildShaderException
     *             if the standard parameter is already used, but with different
     *             type
     * @see #addTextureParameter
     * @see #addTexCoordParameter
     * @see #addEnvParameter
     * @see #addVertexAttribute
     */
    public String useStandardParameter(String type, String parameter) throws GLBuildShaderException {
        if (!parameter.contains(".")) {
            parameter = parameter.toUpperCase();
        }
        for (String param : parameterList) {
            if (param.contains(parameter)) {
                if (!param.toLowerCase().contains(type.toLowerCase())) {
                    throw new GLBuildShaderException("Conflict between parameter types for " + parameter + ": " + type + " vs. " + param.substring(0, param.indexOf(' ')));
                }

                return param.substring(param.indexOf(' '), param.indexOf(' ', param.indexOf(' ') + 1));
            }
        }

        if (standardParameterList.contains(parameter)) {
            if (!type.equalsIgnoreCase(standardParameterTypes.get(parameter))) {
                throw new GLBuildShaderException("Conflict between parameter types for " + parameter + ": " + type + " vs. " + standardParameterTypes.get(parameter));
            }
        } else {
            standardParameterList.add(parameter);
            standardParameterTypes.put(parameter, type);
        }

        if (parameter.contains(".")) {
            return parameter.replace('.', '_');
        } else {
            return parameter.toLowerCase();
        }
    }

    /**
     * Adds new values to a texture coordinate. If necessary, a new texture
     * coordinate is added to the parameter list. It returns an abstract
     * representation of the texture coordinate, which has be used to access the
     * values. The object wrappes the call to the actual OpenGL functions
     * 
     * @param numDimensions
     *            Number of values to add to texture coordinate
     * @return GLTextureCoordinate presenting the new values
     * @throws GLBuildShaderException
     *             if there is no free texture coordinate available
     */
    public GLTextureCoordinate addTexCoordParameter(int numDimensions) throws GLBuildShaderException {
        int texCoord = 0;
        while (texCoord < maxTexCoords) {
            if (componentsAvailableInTexCoord[texCoord] >= numDimensions) {
                if (type == GL.GL_FRAGMENT_PROGRAM_ARB && texCoord > highestTexCoordEverUsed) {
                    highestTexCoordEverUsed = texCoord;
                }

                String identifier = "texcoord" + texCoord;
                int offset = 4 - componentsAvailableInTexCoord[texCoord];

                String searchFor = "TEXCOORD" + texCoord;
                boolean alreadyUsed = standardParameterList.contains(searchFor);
                for (String parameter : parameterList) {
                    if (parameter.contains(searchFor)) {
                        alreadyUsed = true;
                        break;
                    }
                }
                if (!alreadyUsed) {
                    parameterList.add("float4 " + identifier + " : TEXCOORD" + texCoord);
                }

                identifier += ".";

                for (int i = offset; i < offset + numDimensions; i++) {
                    identifier += coordinateDimension[i];
                }

                GLTextureCoordinate output = new GLShaderTextureCoordinate(GL.GL_TEXTURE0 + texCoord, offset, numDimensions, identifier);

                componentsAvailableInTexCoord[texCoord] -= numDimensions;

                return output;
            }
            texCoord++;
        }
        throw new GLBuildShaderException("Number of available texture coordinates exceeded (Max: " + maxTexCoords + ")");
    }

    /**
     * Adds a new texture unit to the parameter list. It returns the OpenGL
     * constant, which has to be used by glActiveTexture to access this
     * parameter, for example GL_TEXTURE1 or GL_TEXTURE4.
     * 
     * @param declaration
     *            declaration of the new parameter, including type and name. for
     *            example "sampler2D myTexture".
     * @return constant to use with glActiveTexture, to bind correct texture.
     * @throws GLBuildShaderException
     *             if there is no free texture unit available
     */
    public int addTextureParameter(String declaration) throws GLBuildShaderException {
        if (nextTexUnit < maxTexUnits) {
            parameterList.add("uniform " + declaration.trim() + " : TEXUNIT" + nextTexUnit);
            return GL.GL_TEXTURE0 + nextTexUnit++;
        } else {
            throw new GLBuildShaderException("Number of available texture units exceeded (Max: " + maxTexUnits + ")");
        }
    }

    /**
     * Adds a new environment parameter to the parameter list. It returns the
     * index, which has to be used by glProgramEnvParameter*ARB to access this
     * parameter.
     * 
     * @param declaration
     *            declaration of the new parameter, including type and name. for
     *            example "float2 myParam".
     * @return index to use with glProgramEnvParameter*ARB, to access correct
     *         parameter.
     * @throws GLBuildShaderException
     *             if there is no free environment parameter left
     */
    public int addEnvParameter(String declaration) throws GLBuildShaderException {
        if (nextConstantRegister < maxConstantRegisters) {
            parameterList.add("uniform " + declaration.trim() + " : C" + nextConstantRegister);
            // parameterList.add("uniform " + declaration.trim() +
            // nextConstantRegister);
            return nextConstantRegister++;
        } else {
            throw new GLBuildShaderException("Number of available enviroment parameters exceeded (Max: " + maxConstantRegisters + ")");
        }
    }

    /**
     * Adds a new generic vertex attribute to the parameter list. It returns the
     * index, which has to be used by glVertexAttrib*ARB to access this
     * parameter.
     * 
     * @param declaration
     *            declaration of the new parameter, including type and name. for
     *            example "float2 myParam".
     * @return index to use with glVertexAttrib*ARB, to access correct
     *         parameter.
     * @throws GLBuildShaderException
     *             if there is no free vertex attribute left
     */
    public int addVertexAttribute(String declaration) throws GLBuildShaderException {
        if (nextVertexAttribute < maxVertexAttributes) {
            parameterList.add(declaration.trim() + " : ATTR" + nextVertexAttribute);
            return nextVertexAttribute++;
        } else {
            throw new GLBuildShaderException("Number of available vertex attributes exceeded (Max: " + maxVertexAttributes + ")");
        }
    }

    /**
     * Adds a new function to the program code. This function adds a single
     * stand-alone function to the code, including return value, name,
     * parameters and body. Make sure to add a code fragment to the main
     * function by calling addMainFragment(), where the new function is called
     * in some way.
     * 
     * @param code
     *            new code fragment
     */
    public void addSingleFunction(String code) {
        functions += code + LINE_SEP + LINE_SEP;
    }

    /**
     * Adds a new code fragment to the program code. Use the names returned by
     * useOutputValue and useStandardParameter or the ones assigned by yourself
     * calling a add*-method.
     * 
     * @param code
     *            new code fragment
     */
    public void addMainFragment(String code) {
        mainBody += code + LINE_SEP + LINE_SEP;
    }

    /**
     * Compiles all given parameters and code fragments into a shader program.
     * Use getShaderID() to get the shader id to use this program.
     */
    public void compile() {
        if (outputStruct.size() == 0) {
            shaderHelper.delShaderID(gl, shaderID);
            return;
        }
        if (type == GL.GL_VERTEX_PROGRAM_ARB) {
            for (int i = 0; i <= highestTexCoordEverUsed; i++) {
                String searchFor = "TEXCOORD" + i;
                boolean alreadyUsed = outputStruct.contains(searchFor);
                for (String parameter : parameterList) {
                    if (parameter.contains(searchFor)) {
                        alreadyUsed = true;
                        break;
                    }
                }
                if (!alreadyUsed) {
                    try {
                        useOutputValue("float4", searchFor, useStandardParameter("float4", searchFor));
                    } catch (GLBuildShaderException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // output struct
        String finalCode = "struct outputStruct {" + LINE_SEP;

        for (String output : outputStruct) {
            finalCode += '\t' + outputTypes.get(output) + ' ' + output.toLowerCase() + " : " + output + ';' + LINE_SEP;
        }

        finalCode += "};" + LINE_SEP + LINE_SEP;

        // other functions
        if (functions.length() > 0) {
            finalCode += functions + LINE_SEP + LINE_SEP;
        }

        // main function header
        finalCode += "outputStruct main(";
        for (String parameter : standardParameterList) {
            if (parameter.contains(".")) {
                finalCode += LINE_SEP + '\t' + standardParameterTypes.get(parameter) + ' ' + parameter.replace('.', '_') + " : " + parameter + ',';
            } else {
                finalCode += LINE_SEP + '\t' + standardParameterTypes.get(parameter) + ' ' + parameter.toLowerCase() + " : " + parameter + ',';
            }
        }

        for (String parameter : parameterList) {
            finalCode += LINE_SEP + '\t' + parameter + ',';
        }

        finalCode = finalCode.substring(0, finalCode.length() - 1);
        finalCode += ')' + LINE_SEP + '{' + LINE_SEP + "\toutputStruct OUT;" + LINE_SEP;

        for (String output : outputStruct) {
            if (outputInit.containsKey(output)) {
                finalCode += "\tOUT." + output.toLowerCase() + " = " + outputInit.get(output) + ";" + LINE_SEP;
            }
        }

        // main function body
        finalCode += LINE_SEP + mainBody + "\treturn OUT;" + LINE_SEP + '}';

        shaderHelper.compileProgram(gl, type, finalCode, shaderID);
    }

    /**
     * Compiles the given shaders and creates a new shader builder to continue.
     * 
     * The new shader builder is initialized with a GLMinimalXShaderProgram.
     * This function might be useful, if multiple shaders should be used on path
     * path through the view chain.
     * 
     * @param shaderBuilder
     * @return new shader builder
     */
    public static GLShaderBuilder compileAndCreateNew(GLShaderBuilder shaderBuilder) {
        shaderBuilder.compile();

        if (shaderBuilder.type == GL.GL_FRAGMENT_PROGRAM_ARB) {
            shaderBuilder = new GLShaderBuilder(shaderBuilder.getGL(), GL.GL_FRAGMENT_PROGRAM_ARB);
            GLMinimalFragmentShaderProgram minimalFragmentShaderProgram = new GLMinimalFragmentShaderProgram();
            minimalFragmentShaderProgram.build(shaderBuilder);
        } else {
            shaderBuilder = new GLShaderBuilder(shaderBuilder.getGL(), GL.GL_VERTEX_PROGRAM_ARB);
            GLMinimalVertexShaderProgram minimalVertexShaderProgram = new GLMinimalVertexShaderProgram();
            minimalVertexShaderProgram.build(shaderBuilder);
        }
        return shaderBuilder;
    }

    /**
     * Class representing exception during the build process.
     */
    public class GLBuildShaderException extends Exception {

        private static final long serialVersionUID = 1L;

        public GLBuildShaderException(String message) {
            super(message);
        }
    }

    /**
     * GLTextureCoordinate implementation for the all texture coordinates
     * managed by GLShaderBuilder
     * 
     * @see GLShaderBuilder#addTextureParameter(String)
     */
    private class GLShaderTextureCoordinate extends GLTextureCoordinate {

        /**
         * Default constructor.
         * 
         * @param target
         *            OpenGL constant, representing the texture coordinate, such
         *            as GL_TEXTURE0 or GL_TEXTURE3
         * @param offset
         *            The offset within the 4d-coordinate. Has to be within [0,
         *            3]
         * @param length
         *            The length of the subset. Has to be within [1, 4]
         * @param identifier
         *            The identifier for this coordinate, which has to be used
         *            in shader programs
         */
        protected GLShaderTextureCoordinate(int target, int offset, int length, String identifier) {
            super(target, offset, length, identifier);
        }
    }

    public String getCode() {
        String finalCode = "struct outputStruct {" + LINE_SEP;

        for (String output : outputStruct) {
            finalCode += '\t' + outputTypes.get(output) + ' ' + output.toLowerCase() + " : " + output + ';' + LINE_SEP;
        }

        finalCode += "};" + LINE_SEP + LINE_SEP;

        // other functions
        if (functions.length() > 0) {
            finalCode += functions + LINE_SEP + LINE_SEP;
        }

        // main function header
        finalCode += "outputStruct main(";
        for (String parameter : standardParameterList) {
            if (parameter.contains(".")) {
                finalCode += LINE_SEP + '\t' + standardParameterTypes.get(parameter) + ' ' + parameter.replace('.', '_') + " : " + parameter + ',';
            } else {
                finalCode += LINE_SEP + '\t' + standardParameterTypes.get(parameter) + ' ' + parameter.toLowerCase() + " : " + parameter + ',';
            }
        }

        for (String parameter : parameterList) {
            finalCode += LINE_SEP + '\t' + parameter + ',';
        }

        finalCode = finalCode.substring(0, finalCode.length() - 1);
        finalCode += ')' + LINE_SEP + '{' + LINE_SEP + "\toutputStruct OUT;" + LINE_SEP;

        for (String output : outputStruct) {
            if (outputInit.containsKey(output)) {
                finalCode += "\tOUT." + output.toLowerCase() + " = " + outputInit.get(output) + ";" + LINE_SEP;
            }
        }

        // main function body
        finalCode += LINE_SEP + mainBody + "\treturn OUT;" + LINE_SEP + '}';

        return finalCode;
    }

}
