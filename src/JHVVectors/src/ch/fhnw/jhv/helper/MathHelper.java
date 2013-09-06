package ch.fhnw.jhv.helper;

/**
 * MathHelper a helper class for all math functions
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * @author David Hostettler
 * 
 */
public class MathHelper {
    /**
     * Conversion from RAD to DEG
     */
    public static final float RAD2DEG = 180.0f / (float) Math.PI;

    /**
     * Conversion from DEG to RAD
     */
    public static final float DEG2RAD = (float) Math.PI / 180.0f;

    /**
     * CONVERSION FROM ARC SECONDS to DEG
     */
    public static final float ARCSECONDS2DEG = 90.0f / 1000.0f;
}
