package com.nextlabs.rms.mail;

import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.io.CloseShieldInputStream;
import com.nextlabs.common.io.CloseShieldOutputStream;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.security.SimpleSSLSocketFactory;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLSocket;

public class SimpleSmtp {

    private static final String EHLO = "EHLO";
    private static final String HELO = "HELO";
    private static final String MAIL = "MAIL FROM:";
    private static final String RCPT = "RCPT TO:";
    private static final String DATA = "DATA";
    private static final String RSET = "RSET";
    private static final String QUIT = "QUIT";
    private static final String AUTH = "AUTH";

    private static final String PLAIN = "PLAIN";

    private Socket socket;
    private InputStream is;
    private OutputStream os;
    private InputStream input;
    private PrintStream output;

    private String serverDomain;
    private String from;

    public SimpleSmtp() {
    }

    public void disconnect() {
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(os);
        IOUtils.closeQuietly(socket);
    }

    public boolean connect() throws IOException, GeneralSecurityException {
        SmtpConfig config = SmtpConfig.getIntance();
        if (config.isSsl()) {
            SimpleSSLSocketFactory factory = new SimpleSSLSocketFactory(SimpleSSLSocketFactory.TLS);
            socket = factory.createSocket(config.getServer(), config.getPort());
            ((SSLSocket)socket).startHandshake();
        } else {
            socket = new Socket(config.getServer(), config.getPort());
        }

        socket.setTcpNoDelay(true);
        socket.setSoTimeout(30000);

        is = socket.getInputStream();
        os = socket.getOutputStream();
        input = new CloseShieldInputStream(new BufferedInputStream(is));
        output = new PrintStream(new CloseShieldOutputStream(new BufferedOutputStream(os)), false, StandardCharsets.US_ASCII.name());

        int code = readResponse();
        if (code != 220) {
            disconnect();
            return false;
        }
        serverDomain = '[' + socket.getLocalAddress().getHostAddress() + ']';
        from = "webmaster@" + WebConfig.getInstance().getProperty(WebConfig.EMAIL_DOMAIN_NAME, "skydrm.com");

        code = ehlo();
        if (code != 250 && !helo()) {
            disconnect();
            throw new IOException("HELO is not supported.");
        }

        String userName = config.getUserName();
        String password = config.getPassword();
        if (StringUtils.hasText(userName) && StringUtils.hasText(password) && !authPlain(userName, password)) {
            disconnect();
            throw new IOException("Login failed.");
        }

        return true;
    }

    public boolean data() throws IOException {
        int code = sendCmd(DATA);
        return code == 354;
    }

    public int ehlo() throws IOException {
        return sendCmd(EHLO, serverDomain);
    }

    public boolean helo() throws IOException {
        int code = sendCmd(HELO, serverDomain);
        return code == 250;
    }

    public boolean authPlain(String userName, String passwd) throws IOException {
        // assume userName and passwd are all ansi
        byte[] buf = ("\0" + userName + "\0" + passwd).getBytes(StandardCharsets.US_ASCII);
        String enc = Base64Codec.encodeAsString(buf);
        int code = sendCmd(AUTH, PLAIN, enc);
        return code == 235;
    }

    public boolean mail(String reversePath) throws IOException {
        int code = sendCmd(MAIL, '<' + reversePath + '>');
        return code == 250;
    }

    public boolean quit() throws IOException {
        int code = sendCmd(QUIT);
        return code == 221;
    }

    public boolean rcpt(String forwardPath) throws IOException {
        int code = sendCmd(RCPT, '<' + forwardPath + '>');
        // TODO: do we need to handle 251 response?
        return code == 250 || code == 251;
    }

    public void rset() throws IOException {
        int code = sendCmd(RSET);
        if (code != 250) {
            quit();
            disconnect();
        }
    }

    public boolean send(Mail message) throws IOException {
        if (!mail(from)) {
            rset();
            return false;
        }

        String[] recipients = message.getRecipient().split(",");
        for (String recipient : recipients) {
            if (!rcpt(recipient)) {
                rset();
                return false;
            }
        }

        if (!data()) {
            rset();
            return false;
        }

        message.writeTo(output);

        output.append(Mail.CRLF).append('.').append(Mail.CRLF);
        output.flush();

        return readResponse() == 250;
    }

    public boolean getSendResult() throws IOException {
        return readResponse() == 250;
    }

    private int sendCmd(String cmd, String... params) throws IOException {
        output.append(cmd);
        for (String param : params) {
            output.append(' ').append(param);
        }
        output.append(Mail.CRLF);
        output.flush();

        return readResponse();
    }

    private int readResponse() throws IOException {
        try {
            String line = IOUtils.readLine(input);
            int code = Integer.parseInt(line.substring(0, 3));
            while (line.charAt(3) == '-') {
                line = IOUtils.readLine(input);
            }
            return code;
        } catch (RuntimeException e) {
            throw new IOException("Mail server drop the connection!", e);
        }
    }
}
