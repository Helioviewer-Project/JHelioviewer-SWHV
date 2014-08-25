package org.helioviewer.swhv;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import javax.media.opengl.GL3;

public class GLSLProgram {

    private final ArrayList<Integer> vertexShaders = new ArrayList<Integer>();
    private final ArrayList<Integer> fragmentShaders = new ArrayList<Integer>();
    private final ArrayList<Integer> geometryShaders = new ArrayList<Integer>();

    private Integer progId;

    public GLSLProgram(GL3 gl2) {
        progId = 0;
    }

    public GLSLProgram(GL3 gl2, URL vertexShader, URL fragmentShader) {

        this(gl2);

        attachVertexShader(gl2, vertexShader);
        attachFragmentShader(gl2, fragmentShader);

        initializeProgram(gl2, true);
    }

    public GLSLProgram(GL3 gl2, URL vertexShader, URL fragmentShader, URL geometryShader) {

        this(gl2);

        attachVertexShader(gl2, vertexShader);
        attachFragmentShader(gl2, fragmentShader);

        initializeProgram(gl2, true);
    }

    public void destroy(GL3 gl2) {
        for (int i = 0; i < vertexShaders.size(); i++) {
            gl2.glDeleteShader(vertexShaders.get(i));
        }
        for (int i = 0; i < fragmentShaders.size(); i++) {
            gl2.glDeleteShader(fragmentShaders.get(i));
        }
        for (int i = 0; i < geometryShaders.size(); i++) {
            gl2.glDeleteShader(geometryShaders.get(i));
        }
        if (progId != 0) {
            gl2.glDeleteProgram(progId);
        }
    }

    public void bind(GL3 gl2) {
        gl2.glUseProgram(progId);
    }

    public void unbind(GL3 gl2) {
        gl2.glUseProgram(0);
    }

    public void setUniform(GL3 gl2, String name, float[] val, int count) {
        int id = gl2.glGetUniformLocation(progId, name);
        if (id == -1) {
            System.err.println("Warning: Invalid uniform parameter " + name);
            return;
        }
        switch (count) {
        case 1:
            gl2.glUniform1fv(id, 1, val, 0);
            break;
        case 2:
            gl2.glUniform2fv(id, 1, val, 0);
            break;
        case 3:
            gl2.glUniform3fv(id, 1, val, 0);
            break;
        case 4:
            gl2.glUniform4fv(id, 1, val, 0);
            break;
        }
    }

    public void setTextureUnit(GL3 gl2, String texname, int texunit) {
        int[] params = new int[] { 0 };
        gl2.glGetProgramiv(progId, GL3.GL_LINK_STATUS, params, 0);
        if (params[0] != 1) {
            System.err.println("Error: setTextureUnit needs program to be linked.");
        }
        int id = gl2.glGetUniformLocation(progId, texname);
        if (id == -1) {
            System.err.println("Warning: Invalid texture " + texname);
            return;
        }
        gl2.glUniform1i(id, texunit);
    }

    public void bindTexture(GL3 gl2, int target, String texname, int texid, int texunit) {
        gl2.glActiveTexture(GL3.GL_TEXTURE0 + texunit);
        gl2.glBindTexture(target, texid);
        setTextureUnit(gl2, texname, texunit);
        gl2.glActiveTexture(GL3.GL_TEXTURE0);
    }

    public void bindTexture2D(GL3 gl2, String texname, int texid, int texunit) {
        bindTexture(gl2, GL3.GL_TEXTURE_2D, texname, texid, texunit);
    }

    public void bindTexture3D(GL3 gl2, String texname, int texid, int texunit) {
        bindTexture(gl2, GL3.GL_TEXTURE_3D, texname, texid, texunit);
    }

    public void bindTextureRECT(GL3 gl2, String texname, int texid, int texunit) {
        bindTexture(gl2, GL3.GL_TEXTURE_RECTANGLE, texname, texid, texunit);
    }

    public final void attachVertexShader(GL3 gl2, URL filename) {

        if (filename != null) {
            String content = "";
            BufferedReader input = null;

            try {
                input = new BufferedReader(new InputStreamReader(filename.openStream()));
                String line = null;

                while ((line = input.readLine()) != null) {
                    content += line + "\n";
                }
            } catch (FileNotFoundException fileNotFoundException) {
                System.err.println("Unable to find the shader file " + filename);
            } catch (IOException iOException) {
                System.err.println("Problem reading the shader file " + filename);
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException iOException) {
                    System.out.println("Problem closing the BufferedReader, " + filename);
                }
            }

            int iID = gl2.glCreateShader(GL3.GL_VERTEX_SHADER);

            String[] akProgramText = new String[1];
            // find and replace program name with "main"
            akProgramText[0] = content;

            int[] params = new int[] { 0 };

            int[] aiLength = new int[1];
            aiLength[0] = akProgramText[0].length();
            int iCount = 1;

            gl2.glShaderSource(iID, iCount, akProgramText, aiLength, 0);

            gl2.glCompileShader(iID);

            gl2.glGetShaderiv(iID, GL3.GL_COMPILE_STATUS, params, 0);

            if (params[0] != 1) {
                System.err.println(filename);
                System.err.println("compile status: " + params[0]);
                gl2.glGetShaderiv(iID, GL3.GL_INFO_LOG_LENGTH, params, 0);
                System.err.println("log length: " + params[0]);
                byte[] abInfoLog = new byte[params[0]];
                gl2.glGetShaderInfoLog(iID, params[0], params, 0, abInfoLog, 0);
                System.err.println(new String(abInfoLog));
                System.exit(-1);
            }
            vertexShaders.add(iID);
        } else {
            System.err.println("Unable to find the shader file " + filename);
        }
    }

    public final void attachFragmentShader(GL3 gl2, URL filename) {

        if (filename != null) {
            String content = "";
            BufferedReader input = null;

            try {
                input = new BufferedReader(new InputStreamReader(filename.openStream()));
                String line = null;

                while ((line = input.readLine()) != null) {
                    content += line + "\n";
                }
            } catch (FileNotFoundException fileNotFoundException) {
                System.err.println("Unable to find the shader file " + filename);
            } catch (IOException iOException) {
                System.err.println("Problem reading the shader file " + filename);
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException iOException) {
                    System.out.println("Problem closing the BufferedReader, " + filename);
                }
            }

            int iID = gl2.glCreateShader(GL3.GL_FRAGMENT_SHADER);

            String[] akProgramText = new String[1];
            // find and replace program name with "main"
            akProgramText[0] = content;

            int[] params = new int[] { 0 };

            int[] aiLength = new int[1];
            aiLength[0] = akProgramText[0].length();
            int iCount = 1;

            gl2.glShaderSource(iID, iCount, akProgramText, aiLength, 0);

            gl2.glCompileShader(iID);

            gl2.glGetShaderiv(iID, GL3.GL_COMPILE_STATUS, params, 0);

            if (params[0] != 1) {
                System.err.println(filename);
                System.err.println("compile status: " + params[0]);
                gl2.glGetShaderiv(iID, GL3.GL_INFO_LOG_LENGTH, params, 0);
                System.err.println("log length: " + params[0]);
                byte[] abInfoLog = new byte[params[0]];
                gl2.glGetShaderInfoLog(iID, params[0], params, 0, abInfoLog, 0);
                System.err.println(new String(abInfoLog));
                System.exit(-1);
            }
            fragmentShaders.add(iID);
        } else {
            System.err.println("Unable to find the shader file " + filename);
        }
    }

    public final void attachGeometryShader(GL3 gl2, URL filename) {

        if (filename != null) {
            String content = "";
            BufferedReader input = null;

            try {
                input = new BufferedReader(new InputStreamReader(filename.openStream()));
                String line = null;

                while ((line = input.readLine()) != null) {
                    content += line + "\n";
                }
            } catch (FileNotFoundException fileNotFoundException) {
                System.err.println("Unable to find the shader file " + filename);
            } catch (IOException iOException) {
                System.err.println("Problem reading the shader file " + filename);
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException iOException) {
                    System.out.println("Problem closing the BufferedReader, " + filename);
                }
            }

            int iID = gl2.glCreateShader(GL3.GL_GEOMETRY_SHADER_ARB);

            String[] akProgramText = new String[1];
            // find and replace program name with "main"
            akProgramText[0] = content;

            int[] params = new int[] { 0 };

            int[] aiLength = new int[1];
            aiLength[0] = akProgramText[0].length();
            int iCount = 1;

            gl2.glShaderSource(iID, iCount, akProgramText, aiLength, 0);

            gl2.glCompileShader(iID);

            gl2.glGetShaderiv(iID, GL3.GL_COMPILE_STATUS, params, 0);

            if (params[0] != 1) {
                System.err.println(filename);
                System.err.println("compile status: " + params[0]);
                gl2.glGetShaderiv(iID, GL3.GL_INFO_LOG_LENGTH, params, 0);
                System.err.println("log length: " + params[0]);
                byte[] abInfoLog = new byte[params[0]];
                gl2.glGetShaderInfoLog(iID, params[0], params, 0, abInfoLog, 0);
                System.err.println(new String(abInfoLog));
                System.exit(-1);
            }
            geometryShaders.add(iID);
        } else {
            System.err.println("Unable to find the shader file " + filename);
        }
    }

    public final void initializeProgram(GL3 gl2, boolean cleanUp) {
        progId = gl2.glCreateProgram();

        for (int i = 0; i < vertexShaders.size(); i++) {
            gl2.glAttachShader(progId, vertexShaders.get(i));
        }

        for (int i = 0; i < fragmentShaders.size(); i++) {
            gl2.glAttachShader(progId, fragmentShaders.get(i));
        }

        for (int i = 0; i < geometryShaders.size(); i++) {
            gl2.glAttachShader(progId, geometryShaders.get(i));
        }

        gl2.glLinkProgram(progId);

        int[] params = new int[] { 0 };
        gl2.glGetProgramiv(progId, GL3.GL_LINK_STATUS, params, 0);

        if (params[0] != 1) {

            System.err.println("link status: " + params[0]);
            gl2.glGetProgramiv(progId, GL3.GL_INFO_LOG_LENGTH, params, 0);
            System.err.println("log length: " + params[0]);

            byte[] abInfoLog = new byte[params[0]];
            gl2.glGetProgramInfoLog(progId, params[0], params, 0, abInfoLog, 0);
            System.err.println(new String(abInfoLog));
        }

        gl2.glValidateProgram(progId);

        if (cleanUp) {
            for (int i = 0; i < vertexShaders.size(); i++) {
                gl2.glDetachShader(progId, vertexShaders.get(i));
                gl2.glDeleteShader(vertexShaders.get(i));
            }

            for (int i = 0; i < fragmentShaders.size(); i++) {
                gl2.glDetachShader(progId, fragmentShaders.get(i));
                gl2.glDeleteShader(fragmentShaders.get(i));
            }
        }
    }

    public Integer getProgId() {
        return progId;
    }
};
