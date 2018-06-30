package org.helioviewer.jhv.math;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.Pair;

public class IcoSphere {

    public static final Pair<FloatBuffer, IntBuffer> IcoSphere = makeIcoSphere(3);

    private static Pair<FloatBuffer, IntBuffer> makeIcoSphere(int level) {
        float t = (float) ((Math.sqrt(5) - 1) / 2);
        float[][] icosahedronVertexList = { { -1, -t, 0 }, { 0, 1, t }, { 0, 1, -t }, { 1, t, 0 }, { 1, -t, 0 }, { 0, -1, -t }, { 0, -1, t }, { t, 0, 1 }, { -t, 0, 1 }, { t, 0, -1 }, { -t, 0, -1 }, { -1, t, 0 }, };
        int[][] icosahedronFaceList = {{3, 7, 1}, {4, 7, 3}, {6, 7, 4}, {8, 7, 6}, {7, 8, 1}, {9, 4, 3}, {2, 9, 3}, {2, 3, 1}, {11, 2, 1}, {10, 2, 11}, {10, 9, 2}, {9, 5, 4}, {6, 4, 5}, {0, 6, 5}, {0, 11, 8}, {11, 1, 8}, {10, 0, 5}, {10, 5, 9}, {0, 8, 6}, {0, 10, 11},};

        for (float[] v : icosahedronVertexList) {
            float length = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            v[0] /= length;
            v[1] /= length;
            v[2] /= length;
        }

        ArrayList<Float> vertices = new ArrayList<>();
        for (float[] v : icosahedronVertexList) {
            vertices.add(v[0]);
            vertices.add(v[2]);
            vertices.add(v[1]);
            vertices.add(1f);
        }

        ArrayList<Integer> faceIndices = new ArrayList<>();
        for (int[] f : icosahedronFaceList) {
            subdivide(f[0], f[1], f[2], vertices, faceIndices, level);
        }

        int beginPositionNumberCorona = vertices.size() / 4;
        float r = 40f;
        vertices.add(-r);
        vertices.add(r);
        vertices.add(0f);
        vertices.add(1f);

        vertices.add(r);
        vertices.add(r);
        vertices.add(0f);
        vertices.add(1f);

        vertices.add(r);
        vertices.add(-r);
        vertices.add(0f);
        vertices.add(1f);

        vertices.add(-r);
        vertices.add(-r);
        vertices.add(0f);
        vertices.add(1f);

        faceIndices.add(beginPositionNumberCorona);
        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona + 1);

        faceIndices.add(beginPositionNumberCorona + 2);
        faceIndices.add(beginPositionNumberCorona);
        faceIndices.add(beginPositionNumberCorona + 3);

        FloatBuffer positionBuffer = BufferUtils.newFloatBuffer(vertices.size());
        for (float vert : vertices) {
            if (vert == 0f)
                vert = Math.nextAfter(vert, vert + 1.0f);
            positionBuffer.put(vert);
        }
        positionBuffer.rewind();

        IntBuffer indexBuffer = BufferUtils.newIntBuffer(faceIndices.size());
        for (int i : faceIndices) {
            indexBuffer.put(i);
        }
        indexBuffer.rewind();

        return new Pair<>(positionBuffer, indexBuffer);
    }

    private static void subdivide(int vx, int vy, int vz, ArrayList<Float> vertexList, ArrayList<Integer> faceList, int level) {
        if (level != 0) {
            float x1 = vertexList.get(4 * vx) + vertexList.get(4 * vy);
            float y1 = vertexList.get(4 * vx + 1) + vertexList.get(4 * vy + 1);
            float z1 = vertexList.get(4 * vx + 2) + vertexList.get(4 * vy + 2);
            float length = (float) Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
            x1 /= length;
            y1 /= length;
            z1 /= length;

            int firstIndex = vertexList.size() / 4;
            vertexList.add(x1);
            vertexList.add(y1);
            vertexList.add(z1);
            vertexList.add(1f);

            float x2 = vertexList.get(4 * vz) + vertexList.get(4 * vy);
            float y2 = vertexList.get(4 * vz + 1) + vertexList.get(4 * vy + 1);
            float z2 = vertexList.get(4 * vz + 2) + vertexList.get(4 * vy + 2);
            length = (float) Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2);
            x2 /= length;
            y2 /= length;
            z2 /= length;

            int secondIndex = vertexList.size() / 4;
            vertexList.add(x2);
            vertexList.add(y2);
            vertexList.add(z2);
            vertexList.add(1f);

            float x3 = vertexList.get(4 * vx) + vertexList.get(4 * vz);
            float y3 = vertexList.get(4 * vx + 1) + vertexList.get(4 * vz + 1);
            float z3 = vertexList.get(4 * vx + 2) + vertexList.get(4 * vz + 2);
            length = (float) Math.sqrt(x3 * x3 + y3 * y3 + z3 * z3);
            x3 /= length;
            y3 /= length;
            z3 /= length;

            int thirdIndex = vertexList.size() / 4;
            vertexList.add(x3);
            vertexList.add(y3);
            vertexList.add(z3);
            vertexList.add(1f);

            subdivide(vx, firstIndex, thirdIndex, vertexList, faceList, level - 1);
            subdivide(firstIndex, vy, secondIndex, vertexList, faceList, level - 1);
            subdivide(thirdIndex, secondIndex, vz, vertexList, faceList, level - 1);
            subdivide(firstIndex, secondIndex, thirdIndex, vertexList, faceList, level - 1);
        } else {
            faceList.add(vx);
            faceList.add(vy);
            faceList.add(vz);
        }
    }

}
