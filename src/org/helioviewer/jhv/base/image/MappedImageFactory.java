/*
 * Copyright (c) 2010, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.helioviewer.jhv.base.image;

import javax.imageio.ImageTypeSpecifier;
import java.awt.*;
import java.awt.image.*;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A factory for creating {@link BufferedImage}s backed by memory mapped files.
 * The data buffers will be allocated outside the normal JVM heap, allowing more efficient
 * memory usage for large images.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: MappedImageFactory.java,v 1.0 May 26, 2010 5:07:01 PM haraldk Exp$
 */
public final class MappedImageFactory {

    // TODO: Create a way to do ColorConvertOp (or other color space conversion) on these images. 
    // - Current implementation of CCOp delegates to internal sun.awt classes that assumes java.awt.DataBufferByte for type byte buffers :-/
    // - Might be possible (but slow) to copy parts to memory and do CCOp on these copies

    /* Constants for DirectColorModel masks, from BufferedImage. */
/*
    private static final int DCM_RED_MASK = 0x00ff0000;
    private static final int DCM_GREEN_MASK = 0x0000ff00;
    private static final int DCM_BLUE_MASK = 0x000000ff;
    private static final int DCM_ALPHA_MASK = 0xff000000;
    private static final int DCM_565_RED_MASK = 0xf800;
    private static final int DCM_565_GRN_MASK = 0x07E0;
    private static final int DCM_565_BLU_MASK = 0x001F;
    private static final int DCM_555_RED_MASK = 0x7C00;
    private static final int DCM_555_GRN_MASK = 0x03E0;
    private static final int DCM_555_BLU_MASK = 0x001F;
    private static final int DCM_BGR_RED_MASK = 0x0000ff;
    private static final int DCM_BGR_GRN_MASK = 0x00ff00;
    private static final int DCM_BGR_BLU_MASK = 0xff0000;
*/

    private MappedImageFactory() {
    }

    public static BufferedImage createCompatibleMappedImage(int width, int height, int type) throws IOException {
        BufferedImage temp = new BufferedImage(1, 1, type);
        return createCompatibleMappedImage(width, height, temp.getSampleModel().createCompatibleSampleModel(width, height), temp.getColorModel());
    }

    public static BufferedImage createCompatibleMappedImage(int width, int height, GraphicsConfiguration configuration, int transparency) throws IOException {
        return createCompatibleMappedImage(width, height, configuration.getColorModel(transparency));
    }

    public static BufferedImage createCompatibleMappedImage(int width, int height, ImageTypeSpecifier type) throws IOException {
        return createCompatibleMappedImage(width, height, type.getSampleModel(width, height), type.getColorModel());
    }

    static BufferedImage createCompatibleMappedImage(int width, int height, ColorModel cm) throws IOException {
        return createCompatibleMappedImage(width, height, cm.createCompatibleSampleModel(width, height), cm);
    }

    static BufferedImage createCompatibleMappedImage(int width, int height, SampleModel sm, ColorModel cm) throws IOException {
        DataBuffer buffer = MappedFileBuffer.create(sm.getTransferType(), width * height * sm.getNumDataElements(), 1);
        return new BufferedImage(cm, RasterFactory.factory.createRaster(sm, buffer, new Point()), cm.isAlphaPremultiplied(), null);
    }

    public static ByteBuffer getByteBuffer(BufferedImage img) {
        DataBuffer buffer = img.getRaster().getDataBuffer();
        if (buffer instanceof MappedFileBuffer.DataBufferByte)
            return (ByteBuffer) ((MappedFileBuffer.DataBufferByte) buffer).getBuffer();
        else
            throw new IncompatibleClassChangeError("Not a MappedFileBuffer byte backed image");
    }

}
