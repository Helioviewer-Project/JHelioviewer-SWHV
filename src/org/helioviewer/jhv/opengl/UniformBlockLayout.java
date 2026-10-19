package org.helioviewer.jhv.opengl;

enum UniformBlockLayout {
    IMAGE("ImageBlock", 0, 96),
    SCREEN("ScreenBlock", 1, 24),
    DISPLAY("DisplayBlock", 2, 28),
    LINE("LineBlock", 3, 24),
    MESH_MATERIAL("MaterialBlock", 4, 8),
    MESH_FRAME("FrameBlock", 5, 20);

    final String glslName;
    final int binding;
    final int floatCount;

    UniformBlockLayout(String _glslName, int _binding, int _floatCount) {
        glslName = _glslName;
        binding = _binding;
        floatCount = _floatCount;
    }

    int byteSize() {
        return floatCount * Float.BYTES;
    }
}
