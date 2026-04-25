package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opengles.GLES30;

public final class GL {
    public static final int SAMPLES = 1; // values greater than 1 request MSAA.
    public static String version = "";
    public static int maxTextureSize;

    public static final int ARRAY_BUFFER = GLES30.GL_ARRAY_BUFFER;
    public static final int BACK = GLES30.GL_BACK;
    public static final int BLEND = GLES30.GL_BLEND;
    public static final int CLAMP_TO_EDGE = GLES30.GL_CLAMP_TO_EDGE;
    public static final int COLOR_ATTACHMENT0 = GLES30.GL_COLOR_ATTACHMENT0;
    public static final int COLOR_BUFFER_BIT = GLES30.GL_COLOR_BUFFER_BIT;
    public static final int COMPILE_STATUS = GLES30.GL_COMPILE_STATUS;
    public static final int CULL_FACE = GLES30.GL_CULL_FACE;
    public static final int DEPTH_ATTACHMENT = GLES30.GL_DEPTH_ATTACHMENT;
    public static final int DEPTH_BUFFER_BIT = GLES30.GL_DEPTH_BUFFER_BIT;
    public static final int DEPTH_COMPONENT16 = GLES30.GL_DEPTH_COMPONENT16;
    public static final int DEPTH_COMPONENT24 = GLES30.GL_DEPTH_COMPONENT24;
    // Keep the 32-bit integer depth format candidate for renderbuffer allocation because it seems to work on ANGLE.
    public static final int DEPTH_COMPONENT32 = 0x81A7;
    public static final int DEPTH_TEST = GLES30.GL_DEPTH_TEST;
    public static final int DRAW_FRAMEBUFFER = GLES30.GL_DRAW_FRAMEBUFFER;
    public static final int DYNAMIC_DRAW = GLES30.GL_DYNAMIC_DRAW;
    public static final int FLOAT = GLES30.GL_FLOAT;
    public static final int FRAGMENT_SHADER = GLES30.GL_FRAGMENT_SHADER;
    public static final int FRAMEBUFFER = GLES30.GL_FRAMEBUFFER;
    public static final int FRAMEBUFFER_COMPLETE = GLES30.GL_FRAMEBUFFER_COMPLETE;
    public static final int FUNC_ADD = GLES30.GL_FUNC_ADD;
    public static final int INFO_LOG_LENGTH = GLES30.GL_INFO_LOG_LENGTH;
    public static final int INVALID_ENUM = GLES30.GL_INVALID_ENUM;
    public static final int INVALID_FRAMEBUFFER_OPERATION = GLES30.GL_INVALID_FRAMEBUFFER_OPERATION;
    public static final int INVALID_OPERATION = GLES30.GL_INVALID_OPERATION;
    public static final int INVALID_VALUE = GLES30.GL_INVALID_VALUE;
    public static final int LEQUAL = GLES30.GL_LEQUAL;
    public static final int LINEAR = GLES30.GL_LINEAR;
    public static final int LINEAR_MIPMAP_LINEAR = GLES30.GL_LINEAR_MIPMAP_LINEAR;
    public static final int LINK_STATUS = GLES30.GL_LINK_STATUS;
    public static final int MAX_SAMPLES = GLES30.GL_MAX_SAMPLES;
    public static final int MAX_TEXTURE_SIZE = GLES30.GL_MAX_TEXTURE_SIZE;
    public static final int NEAREST = GLES30.GL_NEAREST;
    public static final int NO_ERROR = GLES30.GL_NO_ERROR;
    public static final int ONE = GLES30.GL_ONE;
    public static final int ONE_MINUS_SRC_ALPHA = GLES30.GL_ONE_MINUS_SRC_ALPHA;
    public static final int OUT_OF_MEMORY = GLES30.GL_OUT_OF_MEMORY;
    public static final int PACK_ALIGNMENT = GLES30.GL_PACK_ALIGNMENT;
    public static final int PIXEL_UNPACK_BUFFER = GLES30.GL_PIXEL_UNPACK_BUFFER;
    public static final int POINTS = GLES30.GL_POINTS;
    public static final int R8 = GLES30.GL_R8;
    // ANGLE accepts this single-channel 16-bit normalized format, though LWJGL does not expose it on GLES30.
    public static final int R16 = 0x822A;
    public static final int READ_FRAMEBUFFER = GLES30.GL_READ_FRAMEBUFFER;
    public static final int RED = GLES30.GL_RED;
    public static final int RENDERBUFFER = GLES30.GL_RENDERBUFFER;
    public static final int RGB = GLES30.GL_RGB;
    public static final int RGB8 = GLES30.GL_RGB8;
    public static final int RGBA = GLES30.GL_RGBA;
    public static final int STATIC_DRAW = GLES30.GL_STATIC_DRAW;
    public static final int STREAM_DRAW = GLES30.GL_STREAM_DRAW;
    public static final int TEXTURE0 = GLES30.GL_TEXTURE0;
    public static final int TEXTURE_2D = GLES30.GL_TEXTURE_2D;
    public static final int TEXTURE_BASE_LEVEL = GLES30.GL_TEXTURE_BASE_LEVEL;
    public static final int TEXTURE_MAG_FILTER = GLES30.GL_TEXTURE_MAG_FILTER;
    public static final int TEXTURE_MAX_LEVEL = GLES30.GL_TEXTURE_MAX_LEVEL;
    public static final int TEXTURE_MIN_FILTER = GLES30.GL_TEXTURE_MIN_FILTER;
    public static final int TEXTURE_WRAP_S = GLES30.GL_TEXTURE_WRAP_S;
    public static final int TEXTURE_WRAP_T = GLES30.GL_TEXTURE_WRAP_T;
    public static final int TRIANGLES = GLES30.GL_TRIANGLES;
    public static final int TRIANGLE_STRIP = GLES30.GL_TRIANGLE_STRIP;
    public static final int UNIFORM_BUFFER = GLES30.GL_UNIFORM_BUFFER;
    public static final int UNPACK_ALIGNMENT = GLES30.GL_UNPACK_ALIGNMENT;
    public static final int UNPACK_ROW_LENGTH = GLES30.GL_UNPACK_ROW_LENGTH;
    public static final int UNSIGNED_BYTE = GLES30.GL_UNSIGNED_BYTE;
    public static final int UNSIGNED_SHORT = GLES30.GL_UNSIGNED_SHORT;
    public static final int VERSION = GLES30.GL_VERSION;
    public static final int VERTEX_SHADER = GLES30.GL_VERTEX_SHADER;

    private GL() {}

    public static String formatVersionString(String version) {
        return version != null && version.startsWith("OpenGL ") ? version : "OpenGL " + version;
    }

    public static void initInfo() {
        version = formatVersionString(glGetString(VERSION));
        maxTextureSize = glGetInteger(MAX_TEXTURE_SIZE);
    }

    public static void glActiveTexture(int unit) {
        GLES30.glActiveTexture(unit);
    }

    public static void glAttachShader(int program, int shader) {
        GLES30.glAttachShader(program, shader);
    }

    public static void glBindBuffer(int target, int id) {
        GLES30.glBindBuffer(target, id);
    }

    public static void glBindBufferBase(int target, int binding, int buffer) {
        GLES30.glBindBufferBase(target, binding, buffer);
    }

    public static void glBindFramebuffer(int target, int framebuffer) {
        GLES30.glBindFramebuffer(target, framebuffer);
    }

    public static void glBindRenderbuffer(int target, int renderbuffer) {
        GLES30.glBindRenderbuffer(target, renderbuffer);
    }

    public static void glBindTexture(int target, int texture) {
        GLES30.glBindTexture(target, texture);
    }

    public static void glBindVertexArray(int id) {
        GLES30.glBindVertexArray(id);
    }

    public static void glBlendEquation(int mode) {
        GLES30.glBlendEquation(mode);
    }

    public static void glBlendFunc(int sfactor, int dfactor) {
        GLES30.glBlendFunc(sfactor, dfactor);
    }

    public static void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    public static void glBufferData(int target, int size, int usage) {
        GLES30.glBufferData(target, size, usage);
    }

    public static void glBufferSubData(int target, long offset, ByteBuffer buffer) {
        GLES30.glBufferSubData(target, offset, buffer);
    }

    public static void glBufferSubData(int target, long offset, FloatBuffer buffer) {
        GLES30.glBufferSubData(target, offset, buffer);
    }

    public static void glBufferSubData(int target, long offset, IntBuffer buffer) {
        GLES30.glBufferSubData(target, offset, buffer);
    }

    public static void glBufferSubData(int target, long offset, ShortBuffer buffer) {
        GLES30.glBufferSubData(target, offset, buffer);
    }

    public static int glCheckFramebufferStatus(int target) {
        return GLES30.glCheckFramebufferStatus(target);
    }

    public static void glClear(int mask) {
        GLES30.glClear(mask);
    }

    public static void glClearColor(float red, float green, float blue, float alpha) {
        GLES30.glClearColor(red, green, blue, alpha);
    }

    public static void glCompileShader(int shader) {
        GLES30.glCompileShader(shader);
    }

    public static int glCreateProgram() {
        return GLES30.glCreateProgram();
    }

    public static int glCreateShader(int type) {
        return GLES30.glCreateShader(type);
    }

    public static void glCullFace(int mode) {
        GLES30.glCullFace(mode);
    }

    public static void glDeleteBuffer(int id) {
        GLES30.glDeleteBuffers(id);
    }

    public static void glDeleteFramebuffer(int framebuffer) {
        GLES30.glDeleteFramebuffers(framebuffer);
    }

    public static void glDeleteProgram(int program) {
        GLES30.glDeleteProgram(program);
    }

    public static void glDeleteRenderbuffer(int renderbuffer) {
        GLES30.glDeleteRenderbuffers(renderbuffer);
    }

    public static void glDeleteShader(int shader) {
        GLES30.glDeleteShader(shader);
    }

    public static void glDeleteTexture(int id) {
        GLES30.glDeleteTextures(id);
    }

    public static void glDeleteVertexArray(int id) {
        GLES30.glDeleteVertexArrays(id);
    }

    public static void glDepthFunc(int func) {
        GLES30.glDepthFunc(func);
    }

    public static void glDetachShader(int program, int shader) {
        GLES30.glDetachShader(program, shader);
    }

    public static void glDisable(int cap) {
        GLES30.glDisable(cap);
    }

    public static void glDrawArrays(int mode, int first, int count) {
        GLES30.glDrawArrays(mode, first, count);
    }

    public static void glDrawArraysInstanced(int mode, int first, int count, int primcount) {
        GLES30.glDrawArraysInstanced(mode, first, count, primcount);
    }

    public static void glEnable(int cap) {
        GLES30.glEnable(cap);
    }

    public static void glEnableVertexAttribArray(int index) {
        GLES30.glEnableVertexAttribArray(index);
    }

    public static void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        GLES30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    public static void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        GLES30.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    public static int glGenBuffer() {
        return GLES30.glGenBuffers();
    }

    public static int glGenFramebuffer() {
        return GLES30.glGenFramebuffers();
    }

    public static int glGenRenderbuffer() {
        return GLES30.glGenRenderbuffers();
    }

    public static int glGenTexture() {
        return GLES30.glGenTextures();
    }

    public static int glGenVertexArray() {
        return GLES30.glGenVertexArrays();
    }

    public static void glGenerateMipmap(int target) {
        GLES30.glGenerateMipmap(target);
    }

    public static int glGetError() {
        return GLES30.glGetError();
    }

    public static int glGetInteger(int pname) {
        return GLES30.glGetInteger(pname);
    }

    public static String glGetProgramInfoLog(int program, int length) {
        return GLES30.glGetProgramInfoLog(program, length);
    }

    public static int glGetProgrami(int program, int pname) {
        return GLES30.glGetProgrami(program, pname);
    }

    public static String glGetShaderInfoLog(int shader, int length) {
        return GLES30.glGetShaderInfoLog(shader, length);
    }

    public static int glGetShaderi(int shader, int pname) {
        return GLES30.glGetShaderi(shader, pname);
    }

    public static String glGetString(int name) {
        return GLES30.glGetString(name);
    }

    public static int glGetUniformBlockIndex(int program, String blockName) {
        return GLES30.glGetUniformBlockIndex(program, blockName);
    }

    public static int glGetUniformLocation(int program, String name) {
        return GLES30.glGetUniformLocation(program, name);
    }

    public static void glLinkProgram(int program) {
        GLES30.glLinkProgram(program);
    }

    public static void glPixelStorei(int pname, int value) {
        GLES30.glPixelStorei(pname, value);
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer buffer) {
        GLES30.glReadPixels(x, y, width, height, format, type, buffer);
    }

    public static void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        GLES30.glRenderbufferStorage(target, internalformat, width, height);
    }

    public static void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height) {
        GLES30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }

    public static void glShaderSource(int shader, String source) {
        GLES30.glShaderSource(shader, source);
    }

    public static void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int inputFormat, int inputType, ByteBuffer buffer) {
        GLES30.glTexImage2D(target, level, internalFormat, width, height, border, inputFormat, inputType, buffer);
    }

    public static void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int inputFormat, int inputType, ShortBuffer buffer) {
        GLES30.glTexImage2D(target, level, internalFormat, width, height, border, inputFormat, inputType, buffer);
    }

    public static void glTexParameteri(int target, int pname, int value) {
        GLES30.glTexParameteri(target, pname, value);
    }

    public static void glTexSubImage2D(int target, int level, int x, int y, int width, int height, int inputFormat, int inputType, ByteBuffer buffer) {
        GLES30.glTexSubImage2D(target, level, x, y, width, height, inputFormat, inputType, buffer);
    }

    public static void glTexSubImage2D(int target, int level, int x, int y, int width, int height, int inputFormat, int inputType, long offset) {
        GLES30.glTexSubImage2D(target, level, x, y, width, height, inputFormat, inputType, offset);
    }

    public static void glUniform1f(int location, float value) {
        GLES30.glUniform1f(location, value);
    }

    public static void glUniform1fv(int location, float[] values) {
        GLES30.glUniform1fv(location, values);
    }

    public static void glUniform1i(int location, int value) {
        GLES30.glUniform1i(location, value);
    }

    public static void glUniform3fv(int location, float[] values) {
        GLES30.glUniform3fv(location, values);
    }

    public static void glUniform4fv(int location, float[] values) {
        GLES30.glUniform4fv(location, values);
    }

    public static void glUniformBlockBinding(int program, int blockIndex, int binding) {
        GLES30.glUniformBlockBinding(program, blockIndex, binding);
    }

    public static void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        GLES30.glUniformMatrix4fv(location, transpose, value);
    }

    public static void glUseProgram(int program) {
        GLES30.glUseProgram(program);
    }

    public static void glVertexAttribDivisor(int index, int divisor) {
        GLES30.glVertexAttribDivisor(index, divisor);
    }

    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset) {
        GLES30.glVertexAttribPointer(index, size, type, normalized, stride, offset);
    }

    public static void glViewport(int x, int y, int width, int height) {
        GLES30.glViewport(x, y, width, height);
    }
}
