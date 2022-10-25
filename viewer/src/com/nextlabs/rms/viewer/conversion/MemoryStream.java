package com.nextlabs.rms.viewer.conversion;

import com.perceptive.documentfilters.IGRStream;
import com.perceptive.documentfilters.IGRStream_Data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MemoryStream extends IGRStream {

    private SeekableByteArrayOutputStream mOuts;

    public MemoryStream() {
        mOuts = new SeekableByteArrayOutputStream();
    }

    //CHECKSTYLE:OFF

    @Override
    public long Seek(long offset, int origin) {
        if (offset == 0 && origin == 0) {
            //--> Asking for current location
            return mOuts.position();
        } else if (origin == 0) {
            //-->Seek Set
            return mOuts.position((int)offset);
        } else if (origin == 1) {
            //-->Seek Current
            return mOuts.position(mOuts.position() + (int)offset);
        } else if (origin == 2) {
            //-->Seek End
            return mOuts.position(mOuts.size() + (int)offset);
        }
        return mOuts.position();
    }

    @Override
    public long Write(byte[] paramArrayOfByte) {
        mOuts.write(paramArrayOfByte, 0, paramArrayOfByte.length);
        return paramArrayOfByte.length;
    }

    @Override
    public long Read(long size, IGRStream_Data dest) {
        // Read the data from the in-memory array
        byte[] d = new byte[(int)size];
        long ret = mOuts.read(d, 0, (int)size);
        // If the amount read is different that what was asked for, make a copy
        if (ret != size) {
            byte[] arrDest = new byte[(int)ret];
            System.arraycopy(d, 0, arrDest, 0, (int)ret);
            dest.write(arrDest);
        } else {
            dest.write(d);
        }
        return ret;
    }

    //CHECKSTYLE:ON

    public byte[] toByteArray() {
        return mOuts.toByteArray();
    }

    public void writeTo(OutputStream stream) throws IOException {
        mOuts.writeTo(stream);
    }

    static class SeekableByteArrayOutputStream extends ByteArrayOutputStream {

        protected int mPosition;

        public synchronized void write(int b) {
            // Fill in any blanks
            while (mPosition > count) {
                super.write((byte)0);
            }
            // Now check if we're updating existing data or adding new data
            if (mPosition < count) {
                buf[mPosition] = (byte)b;
                mPosition++;
            } else {
                super.write(b);
                mPosition = count;
            }
        }

        public synchronized void write(byte[] b, int off, int len) {
            for (int i = 0; i < len; i++) {
                write(b[off + i]);
            }
        }

        public synchronized int position() {
            return mPosition;
        }

        public synchronized int position(int pos) {
            mPosition = pos;
            return mPosition;
        }

        public synchronized int read(byte[] b, int offset, int len) {
            int i = 0;
            while (len > 0 && mPosition < count) {
                b[offset + i] = buf[mPosition];
                mPosition++;
                len--;
                i++;
            }
            return i;
        }
    }

}
