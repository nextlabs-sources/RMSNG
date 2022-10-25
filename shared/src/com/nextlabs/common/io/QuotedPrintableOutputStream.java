package com.nextlabs.common.io;

import com.nextlabs.common.util.ByteUtils;

import java.io.IOException;
import java.io.OutputStream;

public class QuotedPrintableOutputStream extends ProxyOutputStream {

    private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
        'E', 'F' };
    private int count;
    private int bytesPerLine;
    private boolean gotSpace;
    private boolean gotCR;

    public QuotedPrintableOutputStream(OutputStream out) {
        this(out, 76);
    }

    public QuotedPrintableOutputStream(OutputStream out, int bytesPerLine) {
        super(out);
        // Subtract 1 to account for the '=' in the soft-return
        // at the end of a line
        this.bytesPerLine = bytesPerLine - 1;
    }

    public void close() throws IOException {
        if (gotSpace) {
            output(' ', true);
            gotSpace = false;
        }
        os.close();
    }

    public void flush() throws IOException {
        os.flush();
    }

    protected void output(int c, boolean encode) throws IOException {
        if (encode) {
            if ((count += 3) > bytesPerLine) {
                os.write('=');
                os.write(ByteUtils.CR);
                os.write(ByteUtils.LF);
                count = 3; // set the next line's length
            }
            os.write('=');
            os.write(HEX[c >> 4]);
            os.write(HEX[c & 0xf]);
        } else {
            if (++count > bytesPerLine) {
                os.write('=');
                os.write(ByteUtils.CR);
                os.write(ByteUtils.LF);
                count = 1; // set the next line's length
            }
            os.write(c);
        }
    }

    private void outputCRLF() throws IOException {
        os.write(ByteUtils.CR);
        os.write(ByteUtils.LF);
        count = 0;
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            write(b[off + i]);
        }
    }

    public void write(int c) throws IOException {
        c = c & 0xff; // Turn off the MSB.
        if (gotSpace) { // previous character was <SPACE>
            if (c == ByteUtils.CR || c == ByteUtils.LF) {
                // if CR/LF, we need to encode the <SPACE> char
                output(' ', true);
            } else {
                // no encoding required, just output the char
                output(' ', false);
            }
            gotSpace = false;
        }

        if (c == ByteUtils.CR) {
            gotCR = true;
            outputCRLF();
        } else {
            if (c == ByteUtils.LF) {
                if (!gotCR) {
                    outputCRLF();
                }
                // This is a CRLF sequence, we already output the
                // corresponding CRLF when we got the CR, so ignore this
            } else if (c == ' ') {
                gotSpace = true;
            } else if (c < 0x20 || c >= 0x7f || c == '=') {
                // Encoding required.
                output(c, true);
            } else {
                // No encoding required
                output(c, false);
            }
            // whatever it was, it wasn't a CR
            gotCR = false;
        }
    }
}
