package com.nextlabs.common.io;

import java.io.IOException;
import java.io.OutputStream;

public abstract class ProxyOutputStream extends OutputStream {

    protected OutputStream os;

    public ProxyOutputStream(OutputStream os) {
        this.os = os;
    }

    public void close() throws IOException {
        this.os.close();
    }

    public void flush() throws IOException {
        this.os.flush();
    }

    public void write(int b) throws IOException {
        this.os.write(b);
    }
}
