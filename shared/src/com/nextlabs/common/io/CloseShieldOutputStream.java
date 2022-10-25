package com.nextlabs.common.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Proxy stream that prevents the underlying output stream from being closed.
 */
public class CloseShieldOutputStream extends ProxyOutputStream {

    public CloseShieldOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void close() throws IOException {
    }
}
