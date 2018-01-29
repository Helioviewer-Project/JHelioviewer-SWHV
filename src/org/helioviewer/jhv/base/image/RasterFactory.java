package org.helioviewer.jhv.base.image;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;

public interface RasterFactory {

    WritableRaster createRaster(SampleModel model, DataBuffer buffer, Point origin);

    RasterFactory factory = createRasterFactory();

    static RasterFactory createRasterFactory() {
        try {
            // Try to instantiate, will throw LinkageError if it fails
            return new SunRasterFactory();
        } catch (LinkageError e) {
            e.printStackTrace();
            System.err.println("Could not instantiate SunWritableRaster, falling back to GenericWritableRaster.");
        }
        // Fall back
        return new GenericRasterFactory();
    }

    /**
     * Generic implementation that should work for any JRE, and creates a custom subclass of {@link WritableRaster}.
     */
    final class GenericRasterFactory implements RasterFactory {
        @Override
        public WritableRaster createRaster(SampleModel model, DataBuffer buffer, Point origin) {
            return new GenericWritableRaster(model, buffer, origin);
        }
    }

    /**
     * Sun/Oracle JRE-specific implementation that creates {@code sun.awt.image.SunWritableRaster}.
     * Callers must catch {@link LinkageError}.
     */
    final class SunRasterFactory implements RasterFactory {
        private final Constructor<WritableRaster> factoryMethod = getFactoryMethod();

        @SuppressWarnings("unchecked")
        private static Constructor<WritableRaster> getFactoryMethod() {
            try {
                Class<?> cls = Class.forName("sun.awt.image.SunWritableRaster");

                if (Modifier.isAbstract(cls.getModifiers())) {
                    throw new IncompatibleClassChangeError("sun.awt.image.SunWritableRaster has become abstract and can't be instantiated");
                }

                return (Constructor<WritableRaster>) cls.getConstructor(SampleModel.class, DataBuffer.class, Point.class);
            } catch (ClassNotFoundException e) {
                throw new NoClassDefFoundError(e.getMessage());
            } catch (NoSuchMethodException e) {
                throw new NoSuchMethodError(e.getMessage());
            }
        }

        @Override
        public WritableRaster createRaster(SampleModel model, DataBuffer buffer, Point origin) {
            try {
                return factoryMethod.newInstance(model, buffer, origin);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new Error("Could not create SunWritableRaster: ", e); // Should never happen, as we test for abstract class
            } catch (InvocationTargetException e) {
                // Unwrap to allow normal exception flow
                Throwable cause = e.getCause();

                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else if (cause instanceof Error) {
                    throw (Error) cause;
                }

                throw new UndeclaredThrowableException(cause);
            }
        }
    }

}
