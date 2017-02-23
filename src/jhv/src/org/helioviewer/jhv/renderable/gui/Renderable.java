package org.helioviewer.jhv.renderable.gui;

import java.awt.Component;

import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.opengl.GLSLSolarShader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.jogamp.opengl.GL2;

public interface Renderable {

    void render(@NotNull Camera camera, @NotNull Viewport vp, @NotNull GL2 gl);

    void renderScale(@NotNull Camera camera, @NotNull Viewport vp, @NotNull GL2 gl, @NotNull GLSLSolarShader shader, @NotNull GridScale scale);

    void renderFloat(@NotNull Camera camera, @NotNull Viewport vp, @NotNull GL2 gl);

    void renderFullFloat(@NotNull Camera camera, @NotNull Viewport vp, @NotNull GL2 gl);

    void renderMiniview(@NotNull Camera camera, @NotNull Viewport vp, @NotNull GL2 gl);

    void prerender(@NotNull GL2 gl);

    void remove(@NotNull GL2 gl);

    @Nullable Component getOptionsPanel();

    @NotNull String getName();

    boolean isVisible(int i);

    boolean isVisible();

    int isVisibleIdx();

    void setVisible(boolean b);

    @Nullable String getTimeString();

    boolean isDeletable();

    boolean isDownloading();

    void init(@NotNull GL2 gl);

    void dispose(@NotNull GL2 gl);

    void setVisible(int ctImages);

}
