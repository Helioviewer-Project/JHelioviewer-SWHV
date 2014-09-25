package org.helioviewer.jhv.plugins.swek.download;

/**
 * Contains the combination param and value.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKParam {
    /** The param name */
    public final String param;
    /** The param value */
    public final String value;
    /** The operand */
    public final SWEKOperand operand;

    /**
     * Create default HEK param.
     * 
     */
    public SWEKParam() {
        param = "";
        value = "";
        operand = SWEKOperand.EQUALS;
    }

    /**
     * Create HEK param for given param, value and operand.
     * 
     * @param param
     *            the param
     * @param value
     *            the value
     * @param operand
     *            the operand
     */
    public SWEKParam(String param, String value, SWEKOperand operand) {
        this.param = param;
        this.value = value;
        this.operand = operand;
    }

    /**
     * Gets the param.
     * 
     * @return the param
     */
    public String getParam() {
        return param;
    }

    /**
     * Gets the value.
     * 
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the operand.
     * 
     * @return the operand
     */
    public SWEKOperand getOperand() {
        return operand;
    }
}
