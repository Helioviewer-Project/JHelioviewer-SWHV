package org.helioviewer.jhv.opengl.model;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

import org.lwjgl.assimp.AIMetaData;
import org.lwjgl.assimp.AIMetaDataEntry;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryStack;

public final class AssimpMetaDataTest {

    public static void main(String[] args) throws Exception {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            AIString.Buffer keys = AIString.calloc(8, stack);
            AIMetaDataEntry.Buffer values = AIMetaDataEntry.calloc(8, stack);
            put(stack, keys, values, 0, "int32", Assimp.AI_INT32, Integer.BYTES).putInt(0, -2);
            put(stack, keys, values, 1, "uint32", Assimp.AI_UINT32, Integer.BYTES).putInt(0, -1);
            put(stack, keys, values, 2, "int64", Assimp.AI_INT64, Long.BYTES).putLong(0, -5_000_000_000L);
            put(stack, keys, values, 3, "uint64", Assimp.AI_UINT64, Long.BYTES).putLong(0, -1);
            put(stack, keys, values, 4, "float", Assimp.AI_FLOAT, Float.BYTES).putFloat(0, 1.25f);
            put(stack, keys, values, 5, "double", Assimp.AI_DOUBLE, Double.BYTES).putDouble(0, 2.5);
            put(stack, keys, values, 6, "boolean", Assimp.AI_BOOL, 1).put(0, (byte) 1);
            put(stack, keys, values, 7, "nan", Assimp.AI_DOUBLE, Double.BYTES).putDouble(0, Double.NaN);

            AIMetaData metadata = AIMetaData.calloc(stack).set(keys.remaining(), keys, values);
            AssimpMetaData source = new AssimpMetaData(URI.create("file:/metadata.gltf"), metadata);
            check(source.getDouble("int32") == -2);
            check(source.getDouble("uint32") == 4_294_967_295d);
            check(source.getDouble("int64") == -5_000_000_000d);
            check(source.getDouble("uint64") == 0x1p64);
            check(source.getDouble("float") == 1.25);
            check(source.getDouble("double") == 2.5);
            checkRejected(source, "boolean", "must be a JSON number");
            checkRejected(source, "nan", "must be finite");
        }
    }

    private static ByteBuffer put(MemoryStack stack, AIString.Buffer keys, AIMetaDataEntry.Buffer values,
            int index, String key, int type, int bytes) {
        keys.get(index).data(stack.UTF8(key));
        ByteBuffer data = stack.calloc(bytes);
        values.get(index).set(type, data);
        return data;
    }

    private static void checkRejected(AssimpMetaData source, String key, String message) throws Exception {
        try {
            source.getDouble(key);
            throw new AssertionError(key + " was accepted");
        } catch (IOException e) {
            check(e.getMessage().contains(message));
        }
    }

    private static void check(boolean condition) {
        if (!condition)
            throw new AssertionError();
    }

    private AssimpMetaDataTest() {}
}
