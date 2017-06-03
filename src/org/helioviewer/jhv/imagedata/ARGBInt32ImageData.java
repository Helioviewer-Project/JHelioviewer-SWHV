package org.helioviewer.jhv.imagedata;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.Buffer;
import java.nio.IntBuffer;

import org.helioviewer.jhv.base.logging.Log;

public class ARGBInt32ImageData extends ImageData {

    private final ImageFormat format = ImageFormat.ARGB32;

    public ARGBInt32ImageData(int _width, int _height, Buffer _buffer) {
        super(_width, _height, 32, 1);
        buffer = _buffer;
    }

    public ARGBInt32ImageData(BufferedImage newImage) {
        super(newImage.getWidth(), newImage.getHeight(), 32, 1);
        image = newImage;
        readImageTransportFromBufferedImage(newImage);
    }

    private void readImageTransportFromBufferedImage(BufferedImage newImage) {
        int[] outputData = new int[1];

        DataBuffer dataBuffer = newImage.getRaster().getDataBuffer();
        if (dataBuffer instanceof DataBufferInt) {
            outputData = ((DataBufferInt) dataBuffer).getData();
        } else if (dataBuffer instanceof DataBufferByte) {
            outputData = new int[width * height];
            byte[] inputData = ((DataBufferByte) dataBuffer).getData();
            int bytesPerPixel = inputData.length / (width * height);

            for (int i = 0; i < width * height; i++) {
                outputData[i] = (inputData[i * bytesPerPixel] & 0xFF);
                for (int j = 1; j < bytesPerPixel; j++) {
                    outputData[i] |= (inputData[i * bytesPerPixel + j] & 0xFF) << (j * 8);
                }
                if (bytesPerPixel < 4) {
                    outputData[i] |= 0xFF000000;
                }
            }
        } else {
            Log.error("Unknown DataBuffer: " + dataBuffer);
        }
        buffer = IntBuffer.wrap(outputData);
    }

    @Override
    public ImageFormat getImageFormat() {
        return format;
    }

    @Override
    protected BufferedImage createBufferedImageFromImageTransport() {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        newImage.setRGB(0, 0, width, height, (int[]) buffer.array(), 0, width);
        return newImage;
    }

}
