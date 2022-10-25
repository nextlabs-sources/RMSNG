package com.nextlabs.common.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Proxy stream that prevents the underlying input stream from being closed.
 *
 */
public class CloseShieldInputStream extends ProxyInputStream {

    public CloseShieldInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {
    }
}
