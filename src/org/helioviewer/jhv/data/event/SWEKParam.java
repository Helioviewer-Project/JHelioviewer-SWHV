package org.helioviewer.jhv.data.event;

public class SWEKParam {

    public final String param;
    public final String value;
    public final SWEKOperand operand;

    public SWEKParam(String _param, String _value, SWEKOperand _operand) {
        param = _param;
        value = _value;
        operand = _operand;
    }

}
