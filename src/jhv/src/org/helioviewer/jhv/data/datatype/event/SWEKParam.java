package org.helioviewer.jhv.data.datatype.event;

import org.helioviewer.jhv.data.container.cache.SWEKOperand;

public class SWEKParam {
    public final String param;
    public final String value;
    public final SWEKOperand operand;

    public SWEKParam(String param, String value, SWEKOperand operand) {
        this.param = param;
        this.value = value;
        this.operand = operand;
    }
}
