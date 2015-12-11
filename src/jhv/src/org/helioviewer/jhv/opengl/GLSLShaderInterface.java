package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.math.Quat;

import com.jogamp.opengl.GL2;

public interface GLSLShaderInterface {

    public void bind(GL2 gl);

    public void bindMatrix(GL2 gl, float[] matrix);

    public void bindCameraDifferenceRotationQuat(GL2 gl, Quat quat);

    public void bindDiffCameraDifferenceRotationQuat(GL2 gl, Quat quat);

    public void unbind(GL2 gl);

    public void setUniform(GL2 gl, int id, float[] val, int count);

    public void setTextureUnit(GL2 gl, String texname, int texunit);

    public void attachVertexShader(GL2 gl, String vertexText);

    public void attachFragmentShader(GL2 gl, String fragmentText);

    public void initializeProgram(GL2 gl, boolean cleanUp);

    public void changeRect(double xOffset, double yOffset, double xScale, double yScale);

    public void setDifferenceRect(double differenceXOffset, double differenceYOffset, double differenceXScale, double differenceYScale);

    public void filter(GL2 gl);

    public void bindIsDisc(GL2 gl, int isDisc);

    public void setCutOffRadius(double cutOffRadius, double outerCutOffRadius);

    public void setOuterCutOffRadius(double cutOffRadius);

    public void setAlpha(float alpha);

    public void setContrast(float contrast);

    public void setGamma(float gamma);

    public void setFactors(float weighting, float pixelWidth, float pixelHeight, float span);

    public void setIsDifference(int isDifference);

    public void setTruncationValue(float truncationValue);

    public void setViewport(float offsetX, float offsetY, float width, float height);

    public void setCutOffValue(float val);

    public void setCutOffDirection(float x, float y, float z);
}
