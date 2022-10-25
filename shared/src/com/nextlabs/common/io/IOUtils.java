package com.nextlabs.common.io;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class IOUtils {

    public static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static void close(URLConnection conn) {
        if (conn instanceof HttpURLConnection) {
            ((HttpURLConnection)conn).disconnect();
        }
    }

    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                closeable = null;
            }
        }
    }

    public static long copy(final InputStream input, final OutputStream output) throws IOException {
        return copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    public static long copy(final InputStream input, final OutputStream output, final int bufferSize)
            throws IOException {
        return copy(input, output, 0, -1, new byte[bufferSize]);
    }

    public static long copy(final InputStream input, final OutputStream output, final long offset, final int bufferSize)
            throws IOException {
        return copy(input, output, offset, -1, bufferSize);
    }

    public static long copy(final InputStream input, final OutputStream output, final long offset, final long length)
            throws IOException {
        return copy(input, output, offset, length, DEFAULT_BUFFER_SIZE);
    }

    public static long copy(final InputStream input, final OutputStream output, final long offset, final long length,
        final byte[] buffer)
            throws IOException {
        if (length == 0) {
            return 0;
        }
        if (offset > 0) {
            skip(input, offset);
        }
        final int bufferLength = buffer.length;
        int bytesToRead = bufferLength;
        if (length > 0 && length < bufferLength) {
            bytesToRead = (int)length;
        }
        long count = 0;
        int n;
        while (bytesToRead > 0 && (n = input.read(buffer, 0, bytesToRead)) != EOF) {
            output.write(buffer, 0, n);
            count += n;
            if (length > 0) {
                bytesToRead = (int)Math.min(length - count, bufferLength);
            }
        }
        return count;
    }

    public static long copy(final InputStream input, final OutputStream output, final long offset, final long length,
        final int bufferSize)
            throws IOException {
        return copy(input, output, offset, length, new byte[bufferSize]);
    }

    public static void copy(final InputStream input, final Writer output, final Charset inputEncoding)
            throws IOException {
        final InputStreamReader in = new InputStreamReader(input, inputEncoding);
        copy(in, output);
    }

    public static long copy(final Reader input, final Writer output) throws IOException {
        return copy(input, output, new char[DEFAULT_BUFFER_SIZE]);
    }

    public static long copy(Reader input, Writer output, char[] buffer) throws IOException {
        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static void flush(Flushable flushable) {
        try {
            if (flushable != null) {
                flushable.flush();
            }
        } catch (IOException e) {
            flushable = null;
        }
    }

    public static Reader getReader(File file, String charset) throws IOException {
        return new InputStreamReader(new FileInputStream(file), charset);
    }

    public static Writer getWriter(File file) throws IOException {
        return getWriter(file, "UTF-8", false);
    }

    public static Writer getWriter(File file, String charset) throws IOException {
        return getWriter(file, charset, false);
    }

    public static Writer getWriter(File file, String charset, boolean append) throws IOException {
        return new OutputStreamWriter(new FileOutputStream(file, append), charset);
    }

    public static Writer getWriter(String fileName) throws IOException {
        return getWriter(new File(fileName), "UTF-8", false);
    }

    public static int read(final InputStream input, final byte[] buffer) throws IOException {
        return read(input, buffer, 0, buffer.length);
    }

    public static int read(final InputStream input, final byte[] buffer, final int offset, final int length)
            throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException("Length must not be negative: " + length);
        }
        int remaining = length;
        while (remaining > 0) {
            final int location = length - remaining;
            final int count = input.read(buffer, offset + location, remaining);
            if (count == EOF) {
                break;
            }
            remaining -= count;
        }
        return length - remaining;
    }

    public static byte[] readFully(final InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(input, baos);
        return baos.toByteArray();
    }

    public static int readFully(final InputStream input, final byte[] buffer) throws IOException {
        int length = buffer.length;
        int actual = read(input, buffer, 0, length);
        if (actual != length) {
            throw new EOFException("Length to read: " + length + ", actual: " + actual);
        }
        return actual;
    }

    public static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c1;
        while ((c1 = input.read()) != EOF) {
            if (c1 == '\n') {
                break;
            } else if (c1 == '\r') {
                // Got CR, is the next char NL ?
                boolean twoCRs = false;
                if (input.markSupported()) {
                    input.mark(2);
                }
                int c2 = input.read();
                if (c2 == '\r') {
                    // discard extraneous CR
                    twoCRs = true;
                    c2 = input.read();
                }
                if (c2 != '\n') {
                    if (input.markSupported()) {
                        input.reset();
                    } else {
                        if (!(input instanceof PushbackInputStream)) {
                            input = new PushbackInputStream(input, 2);
                        }
                        if (c2 != -1) {
                            ((PushbackInputStream)input).unread(c2);
                        }
                        if (twoCRs) {
                            ((PushbackInputStream)input).unread('\r');
                        }
                    }
                }
                break;
            }
            baos.write(c1);
        }
        byte[] result = baos.toByteArray();
        return result.length == 0 ? null : new String(result, 0, result.length, "ISO-8859-1");
    }

    public static String readLine(final Reader input) throws IOException {
        BufferedReader reader = toBufferedReader(input);
        return reader.readLine();
    }

    public static List<String> readLines(final InputStream input) throws IOException {
        final InputStreamReader reader = new InputStreamReader(input, Charset.forName("UTF-8"));
        return readLines(reader);
    }

    public static List<String> readLines(final InputStream input, final Charset encoding) throws IOException {
        final InputStreamReader reader = new InputStreamReader(input, encoding);
        return readLines(reader);
    }

    public static List<String> readLines(final Reader input) throws IOException {
        BufferedReader reader = toBufferedReader(input);
        List<String> list = new ArrayList<String>();
        String line = reader.readLine();
        while (line != null) {
            list.add(line);
            line = reader.readLine();
        }
        return list;
    }

    public static long skipAll(InputStream is) {
        if (is == null) {
            return 0;
        }
        int nr = -1;
        long bytesSkipped = 0;
        byte[] skipBuffer = new byte[DEFAULT_BUFFER_SIZE];

        try {
            while ((nr = is.read(skipBuffer)) != EOF) {
                bytesSkipped += nr;
            }
        } catch (IOException e) { //NOPMD

        }
        return bytesSkipped;
    }

    public static long skip(InputStream is, long toSkip) throws IOException {
        long remaining = toSkip;
        int nr;
        if (toSkip <= 0) {
            return 0;
        }
        int size = (int)Math.min(DEFAULT_BUFFER_SIZE, remaining);
        byte[] skipBuffer = new byte[size];
        while (remaining > 0) {
            nr = is.read(skipBuffer, 0, (int)Math.min(size, remaining));
            if (nr < 0) {
                break;
            }
            remaining -= nr;
        }
        return toSkip - remaining;
    }

    private static BufferedReader toBufferedReader(final Reader reader) {
        return reader instanceof BufferedReader ? (BufferedReader)reader : new BufferedReader(reader);
    }

    public static byte[] toByteArray(final InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(input, baos);
        return baos.toByteArray();
    }

    public static String toString(File file) throws IOException {
        Reader reader = null;
        try {
            reader = getReader(file, "UTF-8");
            return toString(reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public static String toString(final InputStream input) throws IOException {
        return toString(input, Charset.forName("UTF-8"));
    }

    public static String toString(InputStream input, Charset encoding) throws IOException {
        StringWriter sw = new StringWriter();
        copy(input, sw, encoding);
        return sw.toString();
    }

    public static String toString(final Reader input) throws IOException {
        StringWriter sw = new StringWriter();
        copy(input, sw);
        return sw.toString();
    }

    private IOUtils() {
    }
}
