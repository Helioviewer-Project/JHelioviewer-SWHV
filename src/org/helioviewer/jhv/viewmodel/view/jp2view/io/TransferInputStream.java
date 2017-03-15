package org.helioviewer.jhv.viewmodel.view.jp2view.io;

import java.io.InputStream;

public abstract class TransferInputStream extends InputStream {

    public abstract int getTotalLength();

}
