package org.helioviewer.jhv.opengl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL33;
import org.lwjgl.opengles.GLES30;

public final class GL {
    public static final int SAMPLES = 4;
    public static String version = "";
    public static int maxTextureSize;

    public static final int ARRAY_BUFFER = GL33.GL_ARRAY_BUFFER;
    public static final int PIXEL_UNPACK_BUFFER = GL33.GL_PIXEL_UNPACK_BUFFER;
    public static final int UNIFORM_BUFFER = GL33.GL_UNIFORM_BUFFER;
    public static final int STATIC_DRAW = GL33.GL_STATIC_DRAW;
    public static final int DYNAMIC_DRAW = GL33.GL_DYNAMIC_DRAW;
    public static final int STREAM_DRAW = GL33.GL_STREAM_DRAW;
    public static final int TEXTURE_2D = GL33.GL_TEXTURE_2D;
    public static final int FRAMEBUFFER = GL33.GL_FRAMEBUFFER;
    public static final int READ_FRAMEBUFFER = GL33.GL_READ_FRAMEBUFFER;
    public static final int DRAW_FRAMEBUFFER = GL33.GL_DRAW_FRAMEBUFFER;
    public static final int RENDERBUFFER = GL33.GL_RENDERBUFFER;
    public static final int COLOR_ATTACHMENT0 = GL33.GL_COLOR_ATTACHMENT0;
    public static final int DEPTH_ATTACHMENT = GL33.GL_DEPTH_ATTACHMENT;
    public static final int FRAMEBUFFER_COMPLETE = GL33.GL_FRAMEBUFFER_COMPLETE;
    public static final int BLEND = GL33.GL_BLEND;
    public static final int CULL_FACE = GL33.GL_CULL_FACE;
    public static final int DEPTH_TEST = GL33.GL_DEPTH_TEST;
    public static final int MULTISAMPLE = GL33.GL_MULTISAMPLE;
    public static final int ONE = GL33.GL_ONE;
    public static final int ONE_MINUS_SRC_ALPHA = GL33.GL_ONE_MINUS_SRC_ALPHA;
    public static final int FUNC_ADD = GL33.GL_FUNC_ADD;
    public static final int LEQUAL = GL33.GL_LEQUAL;
    public static final int BACK = GL33.GL_BACK;
    public static final int COLOR_BUFFER_BIT = GL33.GL_COLOR_BUFFER_BIT;
    public static final int DEPTH_BUFFER_BIT = GL33.GL_DEPTH_BUFFER_BIT;
    public static final int NO_ERROR = GL33.GL_NO_ERROR;
    public static final int INVALID_ENUM = GL33.GL_INVALID_ENUM;
    public static final int INVALID_VALUE = GL33.GL_INVALID_VALUE;
    public static final int INVALID_OPERATION = GL33.GL_INVALID_OPERATION;
    public static final int INVALID_FRAMEBUFFER_OPERATION = GL33.GL_INVALID_FRAMEBUFFER_OPERATION;
    public static final int OUT_OF_MEMORY = GL33.GL_OUT_OF_MEMORY;
    public static final int TEXTURE0 = GL33.GL_TEXTURE0;
    public static final int TEXTURE_BASE_LEVEL = GL33.GL_TEXTURE_BASE_LEVEL;
    public static final int TEXTURE_MAX_LEVEL = GL33.GL_TEXTURE_MAX_LEVEL;
    public static final int TEXTURE_MIN_FILTER = GL33.GL_TEXTURE_MIN_FILTER;
    public static final int TEXTURE_MAG_FILTER = GL33.GL_TEXTURE_MAG_FILTER;
    public static final int TEXTURE_WRAP_S = GL33.GL_TEXTURE_WRAP_S;
    public static final int TEXTURE_WRAP_T = GL33.GL_TEXTURE_WRAP_T;
    public static final int CLAMP_TO_EDGE = GL33.GL_CLAMP_TO_EDGE;
    public static final int LINEAR = GL33.GL_LINEAR;
    public static final int LINEAR_MIPMAP_LINEAR = GL33.GL_LINEAR_MIPMAP_LINEAR;
    public static final int NEAREST = GL33.GL_NEAREST;
    public static final int UNPACK_ALIGNMENT = GL33.GL_UNPACK_ALIGNMENT;
    public static final int UNPACK_ROW_LENGTH = GL33.GL_UNPACK_ROW_LENGTH;
    public static final int RGBA = GL33.GL_RGBA;
    public static final int RGB = GL33.GL_RGB;
    public static final int RED = GL33.GL_RED;
    public static final int FLOAT = GL33.GL_FLOAT;
    public static final int R8 = GL33.GL_R8;
    public static final int R16 = GL33.GL_R16;
    public static final int RGB8 = GL33.GL_RGB8;
    public static final int DEPTH_COMPONENT16 = GL33.GL_DEPTH_COMPONENT16;
    public static final int DEPTH_COMPONENT24 = GL33.GL_DEPTH_COMPONENT24;
    public static final int DEPTH_COMPONENT32 = GL33.GL_DEPTH_COMPONENT32;
    public static final int UNSIGNED_BYTE = GL33.GL_UNSIGNED_BYTE;
    public static final int UNSIGNED_SHORT = GL33.GL_UNSIGNED_SHORT;
    public static final int TRIANGLE_STRIP = GL33.GL_TRIANGLE_STRIP;
    public static final int TRIANGLES = GL33.GL_TRIANGLES;
    public static final int POINTS = GL33.GL_POINTS;
    public static final int VERTEX_SHADER = GL33.GL_VERTEX_SHADER;
    public static final int FRAGMENT_SHADER = GL33.GL_FRAGMENT_SHADER;
    public static final int COMPILE_STATUS = GL33.GL_COMPILE_STATUS;
    public static final int LINK_STATUS = GL33.GL_LINK_STATUS;
    public static final int INFO_LOG_LENGTH = GL33.GL_INFO_LOG_LENGTH;
    public static final int VERSION = GL33.GL_VERSION;
    public static final int MAX_TEXTURE_SIZE = GL33.GL_MAX_TEXTURE_SIZE;
    public static final int MAX_SAMPLES = GL33.GL_MAX_SAMPLES;
    public static final int DEPTH_BITS = GL33.GL_DEPTH_BITS;
    public static final int PACK_ALIGNMENT = GL33.GL_PACK_ALIGNMENT;

    private static final Backend DESKTOP = new DesktopBackend();
    private static final Backend GLES = new GlesBackend();

    private static Backend backend = DESKTOP;

    private GL() {
        assertFacadeMatchesBackend();
    }

    public static void useGles(boolean enabled) {
        backend = enabled ? GLES : DESKTOP;
    }

    public static boolean isGles() {
        return backend == GLES;
    }

    public static String adaptShaderSource(String source, boolean vertex) {
        if (!isGles())
            return source;

        return vertex
                ? source.replace("#version 330 core", "#version 300 es")
                : source.replace("#version 330 core", "#version 300 es\nprecision highp float;");
    }

    public static String formatVersionString(String version) {
        return version != null && version.startsWith("OpenGL ") ? version : "OpenGL " + version;
    }

    public static void initInfo() {
        version = formatVersionString(glGetString(VERSION));
        maxTextureSize = glGetInteger(MAX_TEXTURE_SIZE);
    }

    public static void glActiveTexture(int unit) {
        backend.glActiveTexture(unit);
    }

    public static void glAttachShader(int program, int shader) {
        backend.glAttachShader(program, shader);
    }

    public static void glBindBuffer(int target, int id) {
        backend.glBindBuffer(target, id);
    }

    public static void glBindBufferBase(int target, int binding, int buffer) {
        backend.glBindBufferBase(target, binding, buffer);
    }

    public static void glBindFramebuffer(int target, int framebuffer) {
        backend.glBindFramebuffer(target, framebuffer);
    }

    public static void glBindRenderbuffer(int target, int renderbuffer) {
        backend.glBindRenderbuffer(target, renderbuffer);
    }

    public static void glBindTexture(int target, int texture) {
        backend.glBindTexture(target, texture);
    }

    public static void glBindVertexArray(int id) {
        backend.glBindVertexArray(id);
    }

    public static void glBlendEquation(int mode) {
        backend.glBlendEquation(mode);
    }

    public static void glBlendFunc(int sfactor, int dfactor) {
        backend.glBlendFunc(sfactor, dfactor);
    }

    public static void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        backend.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    public static void glBufferData(int target, int size, int usage) {
        backend.glBufferData(target, size, usage);
    }

    public static void glBufferSubData(int target, long offset, ByteBuffer buffer) {
        backend.glBufferSubData(target, offset, buffer);
    }

    public static void glBufferSubData(int target, long offset, FloatBuffer buffer) {
        backend.glBufferSubData(target, offset, buffer);
    }

    public static void glBufferSubData(int target, long offset, IntBuffer buffer) {
        backend.glBufferSubData(target, offset, buffer);
    }

    public static void glBufferSubData(int target, long offset, ShortBuffer buffer) {
        backend.glBufferSubData(target, offset, buffer);
    }

    public static int glCheckFramebufferStatus(int target) {
        return backend.glCheckFramebufferStatus(target);
    }

    public static void glClear(int mask) {
        backend.glClear(mask);
    }

    public static void glClearColor(float red, float green, float blue, float alpha) {
        backend.glClearColor(red, green, blue, alpha);
    }

    public static void glCompileShader(int shader) {
        backend.glCompileShader(shader);
    }

    public static int glCreateProgram() {
        return backend.glCreateProgram();
    }

    public static int glCreateShader(int type) {
        return backend.glCreateShader(type);
    }

    public static void glCullFace(int mode) {
        backend.glCullFace(mode);
    }

    public static void glDeleteBuffer(int id) {
        backend.glDeleteBuffer(id);
    }

    public static void glDeleteFramebuffer(int framebuffer) {
        backend.glDeleteFramebuffer(framebuffer);
    }

    public static void glDeleteProgram(int program) {
        backend.glDeleteProgram(program);
    }

    public static void glDeleteRenderbuffer(int renderbuffer) {
        backend.glDeleteRenderbuffer(renderbuffer);
    }

    public static void glDeleteShader(int shader) {
        backend.glDeleteShader(shader);
    }

    public static void glDeleteTexture(int id) {
        backend.glDeleteTexture(id);
    }

    public static void glDeleteVertexArray(int id) {
        backend.glDeleteVertexArray(id);
    }

    public static void glDepthFunc(int func) {
        backend.glDepthFunc(func);
    }

    public static void glDetachShader(int program, int shader) {
        backend.glDetachShader(program, shader);
    }

    public static void glDisable(int cap) {
        backend.glDisable(cap);
    }

    public static void glDrawArrays(int mode, int first, int count) {
        backend.glDrawArrays(mode, first, count);
    }

    public static void glDrawArraysInstanced(int mode, int first, int count, int primcount) {
        backend.glDrawArraysInstanced(mode, first, count, primcount);
    }

    public static void glEnable(int cap) {
        backend.glEnable(cap);
    }

    public static void glEnableVertexAttribArray(int index) {
        backend.glEnableVertexAttribArray(index);
    }

    public static void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        backend.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    public static void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        backend.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    public static int glGenBuffer() {
        return backend.glGenBuffer();
    }

    public static int glGenFramebuffer() {
        return backend.glGenFramebuffer();
    }

    public static int glGenRenderbuffer() {
        return backend.glGenRenderbuffer();
    }

    public static int glGenTexture() {
        return backend.glGenTexture();
    }

    public static int glGenVertexArray() {
        return backend.glGenVertexArray();
    }

    public static void glGenerateMipmap(int target) {
        backend.glGenerateMipmap(target);
    }

    public static int glGetError() {
        return backend.glGetError();
    }

    public static int glGetInteger(int pname) {
        return backend.glGetInteger(pname);
    }

    public static String glGetProgramInfoLog(int program, int length) {
        return backend.glGetProgramInfoLog(program, length);
    }

    public static int glGetProgrami(int program, int pname) {
        return backend.glGetProgrami(program, pname);
    }

    public static String glGetShaderInfoLog(int shader, int length) {
        return backend.glGetShaderInfoLog(shader, length);
    }

    public static int glGetShaderi(int shader, int pname) {
        return backend.glGetShaderi(shader, pname);
    }

    public static String glGetString(int name) {
        return backend.glGetString(name);
    }

    public static int glGetUniformBlockIndex(int program, String blockName) {
        return backend.glGetUniformBlockIndex(program, blockName);
    }

    public static int glGetUniformLocation(int program, String name) {
        return backend.glGetUniformLocation(program, name);
    }

    public static void glLinkProgram(int program) {
        backend.glLinkProgram(program);
    }

    public static void glPixelStorei(int pname, int value) {
        backend.glPixelStorei(pname, value);
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer buffer) {
        backend.glReadPixels(x, y, width, height, format, type, buffer);
    }

    public static void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        backend.glRenderbufferStorage(target, internalformat, width, height);
    }

    public static void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height) {
        backend.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }

    public static void glShaderSource(int shader, String source) {
        backend.glShaderSource(shader, source);
    }

    public static void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int inputFormat, int inputType, ByteBuffer buffer) {
        backend.glTexImage2D(target, level, internalFormat, width, height, border, inputFormat, inputType, buffer);
    }

    public static void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int inputFormat, int inputType, ShortBuffer buffer) {
        backend.glTexImage2D(target, level, internalFormat, width, height, border, inputFormat, inputType, buffer);
    }

    public static void glTexParameteri(int target, int pname, int value) {
        backend.glTexParameteri(target, pname, value);
    }

    public static void glTexSubImage2D(int target, int level, int x, int y, int width, int height, int inputFormat, int inputType, ByteBuffer buffer) {
        backend.glTexSubImage2D(target, level, x, y, width, height, inputFormat, inputType, buffer);
    }

    public static void glTexSubImage2D(int target, int level, int x, int y, int width, int height, int inputFormat, int inputType, long offset) {
        backend.glTexSubImage2D(target, level, x, y, width, height, inputFormat, inputType, offset);
    }

    public static void glUniform1fv(int location, float[] values) {
        backend.glUniform1fv(location, values);
    }

    public static void glUniform1i(int location, int value) {
        backend.glUniform1i(location, value);
    }

    public static void glUniform3fv(int location, float[] values) {
        backend.glUniform3fv(location, values);
    }

    public static void glUniform4fv(int location, float[] values) {
        backend.glUniform4fv(location, values);
    }

    public static void glUniformBlockBinding(int program, int blockIndex, int binding) {
        backend.glUniformBlockBinding(program, blockIndex, binding);
    }

    public static void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        backend.glUniformMatrix4fv(location, transpose, value);
    }

    public static void glUseProgram(int program) {
        backend.glUseProgram(program);
    }

    public static void glValidateProgram(int program) {
        backend.glValidateProgram(program);
    }

    public static void glVertexAttribDivisor(int index, int divisor) {
        backend.glVertexAttribDivisor(index, divisor);
    }

    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset) {
        backend.glVertexAttribPointer(index, size, type, normalized, stride, offset);
    }

    public static void glViewport(int x, int y, int width, int height) {
        backend.glViewport(x, y, width, height);
    }

    private interface Backend {
        void glActiveTexture(int unit);

        void glAttachShader(int program, int shader);

        void glBindBuffer(int target, int id);

        void glBindBufferBase(int target, int binding, int buffer);

        void glBindFramebuffer(int target, int framebuffer);

        void glBindRenderbuffer(int target, int renderbuffer);

        void glBindTexture(int target, int texture);

        void glBindVertexArray(int id);

        void glBlendEquation(int mode);

        void glBlendFunc(int sfactor, int dfactor);

        void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter);

        void glBufferData(int target, int size, int usage);

        void glBufferSubData(int target, long offset, ByteBuffer buffer);

        void glBufferSubData(int target, long offset, FloatBuffer buffer);

        void glBufferSubData(int target, long offset, IntBuffer buffer);

        void glBufferSubData(int target, long offset, ShortBuffer buffer);

        int glCheckFramebufferStatus(int target);

        void glClear(int mask);

        void glClearColor(float red, float green, float blue, float alpha);

        void glCompileShader(int shader);

        int glCreateProgram();

        int glCreateShader(int type);

        void glCullFace(int mode);

        void glDeleteBuffer(int id);

        void glDeleteFramebuffer(int framebuffer);

        void glDeleteProgram(int program);

        void glDeleteRenderbuffer(int renderbuffer);

        void glDeleteShader(int shader);

        void glDeleteTexture(int id);

        void glDeleteVertexArray(int id);

        void glDepthFunc(int func);

        void glDetachShader(int program, int shader);

        void glDisable(int cap);

        void glDrawArrays(int mode, int first, int count);

        void glDrawArraysInstanced(int mode, int first, int count, int primcount);

        void glEnable(int cap);

        void glEnableVertexAttribArray(int index);

        void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer);

        void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level);

        int glGenBuffer();

        int glGenFramebuffer();

        int glGenRenderbuffer();

        int glGenTexture();

        int glGenVertexArray();

        void glGenerateMipmap(int target);

        int glGetError();

        int glGetInteger(int pname);

        String glGetProgramInfoLog(int program, int length);

        int glGetProgrami(int program, int pname);

        String glGetShaderInfoLog(int shader, int length);

        int glGetShaderi(int shader, int pname);

        String glGetString(int name);

        int glGetUniformBlockIndex(int program, String blockName);

        int glGetUniformLocation(int program, String name);

        void glLinkProgram(int program);

        void glPixelStorei(int pname, int value);

        void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer buffer);

        void glRenderbufferStorage(int target, int internalformat, int width, int height);

        void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height);

        void glShaderSource(int shader, String source);

        void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int inputFormat, int inputType, ByteBuffer buffer);

        void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int inputFormat, int inputType, ShortBuffer buffer);

        void glTexParameteri(int target, int pname, int value);

        void glTexSubImage2D(int target, int level, int x, int y, int width, int height, int inputFormat, int inputType, ByteBuffer buffer);

        void glTexSubImage2D(int target, int level, int x, int y, int width, int height, int inputFormat, int inputType, long offset);

        void glUniform1fv(int location, float[] values);

        void glUniform1i(int location, int value);

        void glUniform3fv(int location, float[] values);

        void glUniform4fv(int location, float[] values);

        void glUniformBlockBinding(int program, int blockIndex, int binding);

        void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value);

        void glUseProgram(int program);

        void glValidateProgram(int program);

        void glVertexAttribDivisor(int index, int divisor);

        void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset);

        void glViewport(int x, int y, int width, int height);
    }

    private static final class DesktopBackend implements Backend {
        @Override
        public void glActiveTexture(int unit) {
            GL33.glActiveTexture(unit);
        }

        @Override
        public void glAttachShader(int program, int shader) {
            GL33.glAttachShader(program, shader);
        }

        @Override
        public void glBindBuffer(int target, int id) {
            GL33.glBindBuffer(target, id);
        }

        @Override
        public void glBindBufferBase(int target, int binding, int buffer) {
            GL33.glBindBufferBase(target, binding, buffer);
        }

        @Override
        public void glBindFramebuffer(int target, int framebuffer) {
            GL33.glBindFramebuffer(target, framebuffer);
        }

        @Override
        public void glBindRenderbuffer(int target, int renderbuffer) {
            GL33.glBindRenderbuffer(target, renderbuffer);
        }

        @Override
        public void glBindTexture(int target, int texture) {
            GL33.glBindTexture(target, texture);
        }

        @Override
        public void glBindVertexArray(int id) {
            GL33.glBindVertexArray(id);
        }

        @Override
        public void glBlendEquation(int mode) {
            GL33.glBlendEquation(mode);
        }

        @Override
        public void glBlendFunc(int sfactor, int dfactor) {
            GL33.glBlendFunc(sfactor, dfactor);
        }

        @Override
        public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
            GL33.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        }

        @Override
        public void glBufferData(int target, int size, int usage) {
            GL33.glBufferData(target, size, usage);
        }

        @Override
        public void glBufferSubData(int target, long offset, ByteBuffer buffer) {
            GL33.glBufferSubData(target, offset, buffer);
        }

        @Override
        public void glBufferSubData(int target, long offset, FloatBuffer buffer) {
            GL33.glBufferSubData(target, offset, buffer);
        }

        @Override
        public void glBufferSubData(int target, long offset, IntBuffer buffer) {
            GL33.glBufferSubData(target, offset, buffer);
        }

        @Override
        public void glBufferSubData(int target, long offset, ShortBuffer buffer) {
            GL33.glBufferSubData(target, offset, buffer);
        }

        @Override
        public int glCheckFramebufferStatus(int target) {
            return GL33.glCheckFramebufferStatus(target);
        }

        @Override
        public void glClear(int mask) {
            GL33.glClear(mask);
        }

        @Override
        public void glClearColor(float red, float green, float blue, float alpha) {
            GL33.glClearColor(red, green, blue, alpha);
        }

        @Override
        public void glCompileShader(int shader) {
            GL33.glCompileShader(shader);
        }

        @Override
        public int glCreateProgram() {
            return GL33.glCreateProgram();
        }

        @Override
        public int glCreateShader(int type) {
            return GL33.glCreateShader(type);
        }

        @Override
        public void glCullFace(int mode) {
            GL33.glCullFace(mode);
        }

        @Override
        public void glDeleteBuffer(int id) {
            GL33.glDeleteBuffers(id);
        }

        @Override
        public void glDeleteFramebuffer(int framebuffer) {
            GL33.glDeleteFramebuffers(framebuffer);
        }

        @Override
        public void glDeleteProgram(int program) {
            GL33.glDeleteProgram(program);
        }

        @Override
        public void glDeleteRenderbuffer(int renderbuffer) {
            GL33.glDeleteRenderbuffers(renderbuffer);
        }

        @Override
        public void glDeleteShader(int shader) {
            GL33.glDeleteShader(shader);
        }

        @Override
        public void glDeleteTexture(int id) {
            GL33.glDeleteTextures(id);
        }

        @Override
        public void glDeleteVertexArray(int id) {
            GL33.glDeleteVertexArrays(id);
        }

        @Override
        public void glDepthFunc(int func) {
            GL33.glDepthFunc(func);
        }

        @Override
        public void glDetachShader(int program, int shader) {
            GL33.glDetachShader(program, shader);
        }

        @Override
        public void glDisable(int cap) {
            GL33.glDisable(cap);
        }

        @Override
        public void glDrawArrays(int mode, int first, int count) {
            GL33.glDrawArrays(mode, first, count);
        }

        @Override
        public void glDrawArraysInstanced(int mode, int first, int count, int primcount) {
            GL33.glDrawArraysInstanced(mode, first, count, primcount);
        }

        @Override
        public void glEnable(int cap) {
            GL33.glEnable(cap);
        }

        @Override
        public void glEnableVertexAttribArray(int index) {
            GL33.glEnableVertexAttribArray(index);
        }

        @Override
        public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
            GL33.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
        }

        @Override
        public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
            GL33.glFramebufferTexture2D(target, attachment, textarget, texture, level);
        }

        @Override
        public int glGenBuffer() {
            return GL33.glGenBuffers();
        }

        @Override
        public int glGenFramebuffer() {
            return GL33.glGenFramebuffers();
        }

        @Override
        public int glGenRenderbuffer() {
            return GL33.glGenRenderbuffers();
        }

        @Override
        public int glGenTexture() {
            return GL33.glGenTextures();
        }

        @Override
        public int glGenVertexArray() {
            return GL33.glGenVertexArrays();
        }

        @Override
        public void glGenerateMipmap(int target) {
            GL33.glGenerateMipmap(target);
        }

        @Override
        public int glGetError() {
            return GL33.glGetError();
        }

        @Override
        public int glGetInteger(int pname) {
            return GL33.glGetInteger(pname);
        }

        @Override
        public String glGetProgramInfoLog(int program, int length) {
            return GL33.glGetProgramInfoLog(program, length);
        }

        @Override
        public int glGetProgrami(int program, int pname) {
            return GL33.glGetProgrami(program, pname);
        }

        @Override
        public String glGetShaderInfoLog(int shader, int length) {
            return GL33.glGetShaderInfoLog(shader, length);
        }

        @Override
        public int glGetShaderi(int shader, int pname) {
            return GL33.glGetShaderi(shader, pname);
        }

        @Override
        public String glGetString(int name) {
            return GL33.glGetString(name);
        }

        @Override
        public int glGetUniformBlockIndex(int program, String blockName) {
            return GL33.glGetUniformBlockIndex(program, blockName);
        }

        @Override
        public int glGetUniformLocation(int program, String name) {
            return GL33.glGetUniformLocation(program, name);
        }

        @Override
        public void glLinkProgram(int program) {
            GL33.glLinkProgram(program);
        }

        @Override
        public void glPixelStorei(int pname, int value) {
            GL33.glPixelStorei(pname, value);
        }

        @Override
        public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer buffer) {
            GL33.glReadPixels(x, y, width, height, format, type, buffer);
        }

        @Override
        public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
            GL33.glRenderbufferStorage(target, internalformat, width, height);
        }

        @Override
        public void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height) {
            GL33.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
        }

        @Override
        public void glShaderSource(int shader, String source) {
            GL33.glShaderSource(shader, source);
        }

        @Override
        public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int inputFormat, int inputType, ByteBuffer buffer) {
            GL33.glTexImage2D(target, level, internalFormat, width, height, border, inputFormat, inputType, buffer);
        }

        @Override
        public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int inputFormat, int inputType, ShortBuffer buffer) {
            GL33.glTexImage2D(target, level, internalFormat, width, height, border, inputFormat, inputType, buffer);
        }

        @Override
        public void glTexParameteri(int target, int pname, int value) {
            GL33.glTexParameteri(target, pname, value);
        }

        @Override
        public void glTexSubImage2D(int target, int level, int x, int y, int width, int height, int inputFormat, int inputType, ByteBuffer buffer) {
            GL33.glTexSubImage2D(target, level, x, y, width, height, inputFormat, inputType, buffer);
        }

        @Override
        public void glTexSubImage2D(int target, int level, int x, int y, int width, int height, int inputFormat, int inputType, long offset) {
            GL33.glTexSubImage2D(target, level, x, y, width, height, inputFormat, inputType, offset);
        }

        @Override
        public void glUniform1fv(int location, float[] values) {
            GL33.glUniform1fv(location, values);
        }

        @Override
        public void glUniform1i(int location, int value) {
            GL33.glUniform1i(location, value);
        }

        @Override
        public void glUniform3fv(int location, float[] values) {
            GL33.glUniform3fv(location, values);
        }

        @Override
        public void glUniform4fv(int location, float[] values) {
            GL33.glUniform4fv(location, values);
        }

        @Override
        public void glUniformBlockBinding(int program, int blockIndex, int binding) {
            GL33.glUniformBlockBinding(program, blockIndex, binding);
        }

        @Override
        public void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
            GL33.glUniformMatrix4fv(location, transpose, value);
        }

        @Override
        public void glUseProgram(int program) {
            GL33.glUseProgram(program);
        }

        @Override
        public void glValidateProgram(int program) {
            GL33.glValidateProgram(program);
        }

        @Override
        public void glVertexAttribDivisor(int index, int divisor) {
            GL33.glVertexAttribDivisor(index, divisor);
        }

        @Override
        public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset) {
            GL33.glVertexAttribPointer(index, size, type, normalized, stride, offset);
        }

        @Override
        public void glViewport(int x, int y, int width, int height) {
            GL33.glViewport(x, y, width, height);
        }
    }

    private static final class GlesBackend implements Backend {
        @Override
        public void glActiveTexture(int unit) {
            GLES30.glActiveTexture(unit);
        }

        @Override
        public void glAttachShader(int program, int shader) {
            GLES30.glAttachShader(program, shader);
        }

        @Override
        public void glBindBuffer(int target, int id) {
            GLES30.glBindBuffer(target, id);
        }

        @Override
        public void glBindBufferBase(int target, int binding, int buffer) {
            GLES30.glBindBufferBase(target, binding, buffer);
        }

        @Override
        public void glBindFramebuffer(int target, int framebuffer) {
            GLES30.glBindFramebuffer(target, framebuffer);
        }

        @Override
        public void glBindRenderbuffer(int target, int renderbuffer) {
            GLES30.glBindRenderbuffer(target, renderbuffer);
        }

        @Override
        public void glBindTexture(int target, int texture) {
            GLES30.glBindTexture(target, texture);
        }

        @Override
        public void glBindVertexArray(int id) {
            GLES30.glBindVertexArray(id);
        }

        @Override
        public void glBlendEquation(int mode) {
            GLES30.glBlendEquation(mode);
        }

        @Override
        public void glBlendFunc(int sfactor, int dfactor) {
            GLES30.glBlendFunc(sfactor, dfactor);
        }

        @Override
        public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
            GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        }

        @Override
        public void glBufferData(int target, int size, int usage) {
            GLES30.glBufferData(target, size, usage);
        }

        @Override
        public void glBufferSubData(int target, long offset, ByteBuffer buffer) {
            GLES30.glBufferSubData(target, offset, buffer);
        }

        @Override
        public void glBufferSubData(int target, long offset, FloatBuffer buffer) {
            GLES30.glBufferSubData(target, offset, buffer);
        }

        @Override
        public void glBufferSubData(int target, long offset, IntBuffer buffer) {
            GLES30.glBufferSubData(target, offset, buffer);
        }

        @Override
        public void glBufferSubData(int target, long offset, ShortBuffer buffer) {
            GLES30.glBufferSubData(target, offset, buffer);
        }

        @Override
        public int glCheckFramebufferStatus(int target) {
            return GLES30.glCheckFramebufferStatus(target);
        }

        @Override
        public void glClear(int mask) {
            GLES30.glClear(mask);
        }

        @Override
        public void glClearColor(float red, float green, float blue, float alpha) {
            GLES30.glClearColor(red, green, blue, alpha);
        }

        @Override
        public void glCompileShader(int shader) {
            GLES30.glCompileShader(shader);
        }

        @Override
        public int glCreateProgram() {
            return GLES30.glCreateProgram();
        }

        @Override
        public int glCreateShader(int type) {
            return GLES30.glCreateShader(type);
        }

        @Override
        public void glCullFace(int mode) {
            GLES30.glCullFace(mode);
        }

        @Override
        public void glDeleteBuffer(int id) {
            GLES30.glDeleteBuffers(id);
        }

        @Override
        public void glDeleteFramebuffer(int framebuffer) {
            GLES30.glDeleteFramebuffers(framebuffer);
        }

        @Override
        public void glDeleteProgram(int program) {
            GLES30.glDeleteProgram(program);
        }

        @Override
        public void glDeleteRenderbuffer(int renderbuffer) {
            GLES30.glDeleteRenderbuffers(renderbuffer);
        }

        @Override
        public void glDeleteShader(int shader) {
            GLES30.glDeleteShader(shader);
        }

        @Override
        public void glDeleteTexture(int id) {
            GLES30.glDeleteTextures(id);
        }

        @Override
        public void glDeleteVertexArray(int id) {
            GLES30.glDeleteVertexArrays(id);
        }

        @Override
        public void glDepthFunc(int func) {
            GLES30.glDepthFunc(func);
        }

        @Override
        public void glDetachShader(int program, int shader) {
            GLES30.glDetachShader(program, shader);
        }

        @Override
        public void glDisable(int cap) {
            GLES30.glDisable(cap);
        }

        @Override
        public void glDrawArrays(int mode, int first, int count) {
            GLES30.glDrawArrays(mode, first, count);
        }

        @Override
        public void glDrawArraysInstanced(int mode, int first, int count, int primcount) {
            GLES30.glDrawArraysInstanced(mode, first, count, primcount);
        }

        @Override
        public void glEnable(int cap) {
            GLES30.glEnable(cap);
        }

        @Override
        public void glEnableVertexAttribArray(int index) {
            GLES30.glEnableVertexAttribArray(index);
        }

        @Override
        public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
            GLES30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
        }

        @Override
        public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
            GLES30.glFramebufferTexture2D(target, attachment, textarget, texture, level);
        }

        @Override
        public int glGenBuffer() {
            return GLES30.glGenBuffers();
        }

        @Override
        public int glGenFramebuffer() {
            return GLES30.glGenFramebuffers();
        }

        @Override
        public int glGenRenderbuffer() {
            return GLES30.glGenRenderbuffers();
        }

        @Override
        public int glGenTexture() {
            return GLES30.glGenTextures();
        }

        @Override
        public int glGenVertexArray() {
            return GLES30.glGenVertexArrays();
        }

        @Override
        public void glGenerateMipmap(int target) {
            GLES30.glGenerateMipmap(target);
        }

        @Override
        public int glGetError() {
            return GLES30.glGetError();
        }

        @Override
        public int glGetInteger(int pname) {
            return GLES30.glGetInteger(pname);
        }

        @Override
        public String glGetProgramInfoLog(int program, int length) {
            return GLES30.glGetProgramInfoLog(program, length);
        }

        @Override
        public int glGetProgrami(int program, int pname) {
            return GLES30.glGetProgrami(program, pname);
        }

        @Override
        public String glGetShaderInfoLog(int shader, int length) {
            return GLES30.glGetShaderInfoLog(shader, length);
        }

        @Override
        public int glGetShaderi(int shader, int pname) {
            return GLES30.glGetShaderi(shader, pname);
        }

        @Override
        public String glGetString(int name) {
            return GLES30.glGetString(name);
        }

        @Override
        public int glGetUniformBlockIndex(int program, String blockName) {
            return GLES30.glGetUniformBlockIndex(program, blockName);
        }

        @Override
        public int glGetUniformLocation(int program, String name) {
            return GLES30.glGetUniformLocation(program, name);
        }

        @Override
        public void glLinkProgram(int program) {
            GLES30.glLinkProgram(program);
        }

        @Override
        public void glPixelStorei(int pname, int value) {
            GLES30.glPixelStorei(pname, value);
        }

        @Override
        public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer buffer) {
            GLES30.glReadPixels(x, y, width, height, format, type, buffer);
        }

        @Override
        public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
            GLES30.glRenderbufferStorage(target, internalformat, width, height);
        }

        @Override
        public void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height) {
            GLES30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
        }

        @Override
        public void glShaderSource(int shader, String source) {
            GLES30.glShaderSource(shader, source);
        }

        @Override
        public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int inputFormat, int inputType, ByteBuffer buffer) {
            GLES30.glTexImage2D(target, level, internalFormat, width, height, border, inputFormat, inputType, buffer);
        }

        @Override
        public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int inputFormat, int inputType, ShortBuffer buffer) {
            GLES30.glTexImage2D(target, level, internalFormat, width, height, border, inputFormat, inputType, buffer);
        }

        @Override
        public void glTexParameteri(int target, int pname, int value) {
            GLES30.glTexParameteri(target, pname, value);
        }

        @Override
        public void glTexSubImage2D(int target, int level, int x, int y, int width, int height, int inputFormat, int inputType, ByteBuffer buffer) {
            GLES30.glTexSubImage2D(target, level, x, y, width, height, inputFormat, inputType, buffer);
        }

        @Override
        public void glTexSubImage2D(int target, int level, int x, int y, int width, int height, int inputFormat, int inputType, long offset) {
            GLES30.glTexSubImage2D(target, level, x, y, width, height, inputFormat, inputType, offset);
        }

        @Override
        public void glUniform1fv(int location, float[] values) {
            GLES30.glUniform1fv(location, values);
        }

        @Override
        public void glUniform1i(int location, int value) {
            GLES30.glUniform1i(location, value);
        }

        @Override
        public void glUniform3fv(int location, float[] values) {
            GLES30.glUniform3fv(location, values);
        }

        @Override
        public void glUniform4fv(int location, float[] values) {
            GLES30.glUniform4fv(location, values);
        }

        @Override
        public void glUniformBlockBinding(int program, int blockIndex, int binding) {
            GLES30.glUniformBlockBinding(program, blockIndex, binding);
        }

        @Override
        public void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
            GLES30.glUniformMatrix4fv(location, transpose, value);
        }

        @Override
        public void glUseProgram(int program) {
            GLES30.glUseProgram(program);
        }

        @Override
        public void glValidateProgram(int program) {
        }

        @Override
        public void glVertexAttribDivisor(int index, int divisor) {
            GLES30.glVertexAttribDivisor(index, divisor);
        }

        @Override
        public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset) {
            GLES30.glVertexAttribPointer(index, size, type, normalized, stride, offset);
        }

        @Override
        public void glViewport(int x, int y, int width, int height) {
            GLES30.glViewport(x, y, width, height);
        }
    }

    private static void assertFacadeMatchesBackend() {
        Set<String> facade = Arrays.stream(GL.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getName().startsWith("gl"))
                .map(GL::signature)
                .collect(Collectors.toSet());

        Set<String> iface = Arrays.stream(Backend.class.getDeclaredMethods())
                .map(GL::signature)
                .collect(Collectors.toSet());

        if (!facade.equals(iface))
            throw new IllegalStateException("GL facade and backend are out of sync");
    }

    private static String signature(Method method) {
        return method.getName() + Arrays.toString(method.getParameterTypes()) + method.getReturnType().getTypeName();
    }
}
