package com.nextlabs.rms.mail;

import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.io.QuotedPrintableOutputStream;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.shared.LogConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Mail {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    public static final String CRLF = "\r\n";

    public static final String KEY_RECIPIENT = "recipient";
    public static final String KEY_RECIPIENT_ENCODED = "recipientEncoded";
    public static final String KEY_DATE = "date";
    public static final String KEY_UID = "uid";
    public static final String KEY_WEB_URL = "web";
    public static final String KEY_FULL_NAME = "fullName";
    public static final String KEY_ACCOUNT = "accountId";
    public static final String KEY_OTP = "otp";
    public static final String FEEDBACK_TYPE = "feedbackType";
    public static final String FEEDBACK_SUMMARY = "feedbackSummary";
    public static final String FEEDBACK_DESCRIPTION = "description";
    public static final String FEEDBACK_USEREMAIL = "userEmail";
    public static final String PROJECT_NAME = "projectName";
    public static final String DECLINE_REASON = "declineReason";
    public static final String INVITATION_MSG = "invitationMsg";
    public static final String INVITATION_MSG_PREFIX = "messagePrefix";
    public static final String INVITATION_ID = "invitationId";
    public static final String INVITATION_CODE = "invitationCode";
    public static final String BASE_URL = "baseUrl";
    public static final String EMAIL_DOMAIN_NAME = "domain";
    public static final String EMAIL_SENDER_NAME = "sender";
    private static final String RFC822_DATEFORMAT = "EEE, d MMM yyyy HH:mm:ss Z";
    private static final Pattern PATTERN_BODY = Pattern.compile("(\\[\\[(B:|Q:)?(.+?)\\]\\])", Pattern.DOTALL);
    private static final Pattern PATTERN_REPLACE = Pattern.compile("(\\{\\{(B:|Q:)?(.+?)\\}\\})");

    private static HashMap<String, String> templates = new HashMap<String, String>();

    private String body;
    private Properties prop;
    private SimpleDateFormat df;

    public Mail(Properties prop, String templateName, Locale locale) {
        this.prop = prop;
        body = loadTemplate(templateName, locale);
        df = new SimpleDateFormat(RFC822_DATEFORMAT, Locale.US);
    }

    public String getRecipient() {
        return prop.getProperty(KEY_RECIPIENT);
    }

    public void writeTo(PrintStream ps) {
        try {
            prop.setProperty(KEY_DATE, df.format(new Date()));
            prop.setProperty(KEY_UID, UUID.randomUUID().toString());

            Matcher m = PATTERN_BODY.matcher(body);
            int index = 0;
            while (m.find()) {
                replace(ps, body.substring(index, m.start()));

                String transferEncoding = m.group(2);
                PrintStream os;
                if ("B:".equals(transferEncoding)) {
                    os = new PrintStream(new Base64OutputStream(ps), false, StandardCharsets.UTF_8.name());
                } else if ("Q:".equals(transferEncoding)) {
                    os = new PrintStream(new QuotedPrintableOutputStream(ps), false, StandardCharsets.UTF_8.name());
                } else {
                    os = ps;
                }

                replace(os, m.group(3));
                os.flush();
                index = m.end();
            }
            replace(ps, body.substring(index));
            ps.flush();
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static synchronized String loadTemplate(String name, Locale locale) {
        String body = templates.get(name);
        if (body == null) {
            StringBuilder sb = new StringBuilder(1024);
            InputStream is = null;
            BufferedReader reader = null;
            try {
                String file = "template/" + name + '_' + locale.toString() + ".txt";
                is = Mail.class.getResourceAsStream(file);
                if (is == null) {
                    file = "template/" + name + ".txt";
                    is = Mail.class.getResourceAsStream(file);
                }
                String line;
                reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append(CRLF);
                }
                body = sb.toString();
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(is);
            }
        }
        return body;
    }

    private void replace(PrintStream ps, String text) {
        Matcher m = PATTERN_REPLACE.matcher(text);
        int index = 0;
        while (m.find()) {
            ps.append(text.subSequence(index, m.start()));
            String codec = m.group(2);
            String key = m.group(3);
            String value = prop.getProperty(key, key);
            if (StringUtils.isAscii(value)) {
                ps.append(value);
            } else {
                if ("B:".equals(codec)) {
                    ps.append(bEncode(value, StandardCharsets.UTF_8.name()));
                } else if ("Q:".equals(codec)) {
                    ps.append(qEncode(value, StandardCharsets.UTF_8.name()));
                } else {
                    ps.append(value);
                }
            }
            index = m.end();
        }
        ps.append(text.substring(index));
    }

    private static String bEncode(String value, String charset) {
        StringBuilder sb = new StringBuilder();
        sb.append("=?").append(charset).append("?B?");
        sb.append(Base64Codec.encodeAsString(StringUtils.toBytesQuietly(value, charset)));
        sb.append("?=");
        return sb.toString();
    }

    private static String qEncode(String value, String charset) {
        StringBuilder sb = new StringBuilder();
        sb.append("=?").append(charset).append("?Q?");
        QuotedPrintableCodec codec = new QuotedPrintableCodec(charset);
        try {
            sb.append(codec.encode(value, charset));
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        sb.append("?=");
        return sb.toString();
    }
}
