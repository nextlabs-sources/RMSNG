package com.nextlabs.nxl;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.Constants.ProtectionType;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.RandomUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.FilePolicy.Policy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NxlFile implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger(NxlFile.class);
    public static final String SECTION_FILE_INFO = ".FileInfo";
    public static final String SECTION_FILE_POLICY = ".FilePolicy";
    public static final String SECTION_FILE_TAGS = ".FileTag";

    public static final int BASIC_HEADER_SIZE = 4096;
    public static final int FIXED_HEADER_SIZE = 4056;
    public static final int COMPLETE_HEADER_SIZE = 4 * 4096;
    public static final int EXTENDED_DATA_SIZE = 784;

    private Signature signature;
    private FileHeader fileHeader;
    private KeyHeader keyHeader;
    private SectionHeader sectionHeader;
    private ByteBuffer extendedData;
    private DynamicHeader dynamicHeader;
    private InputStream content;

    private byte[] basicHeader;

    NxlFile() {
        basicHeader = new byte[BASIC_HEADER_SIZE];
    }

    public static NxlFile parse(InputStream is) throws IOException, NxlException {
        NxlFile nxl = new NxlFile();
        int len = IOUtils.read(is, nxl.basicHeader);
        if (len < BASIC_HEADER_SIZE) {
            throw new NxlException("Invalid nxl file");
        }
        ByteBuffer bb = ByteBuffer.wrap(nxl.basicHeader).asReadOnlyBuffer();
        bb.order(ByteOrder.LITTLE_ENDIAN);
        Signature signature = new Signature(bb);

        if (signature.getMajor() != 3) {
            throw new UnsupportedNxlVersionException("Unsupported version: " + signature.getMajor() + '.' + signature.getMinor());
        }
        if (!signature.isValid()) {
            throw new NxlException("Invalid nxl file");
        }

        nxl.signature = signature;
        nxl.fileHeader = new FileHeader(bb);
        nxl.keyHeader = new KeyHeader(bb);
        nxl.sectionHeader = new SectionHeader(bb);
        nxl.extendedData = bb.slice();
        nxl.extendedData.limit(EXTENDED_DATA_SIZE);
        bb.position(bb.position() + EXTENDED_DATA_SIZE);
        nxl.dynamicHeader = new DynamicHeader(bb);

        for (int i = 0; i < 32; ++i) {
            Section section = nxl.sectionHeader.getSection(i);
            if (section != null) {
                int size;
                if (section.isCompressed()) {
                    size = section.getCompressedDataSize();
                } else {
                    size = section.getOriginalDatasize();
                }
                if (section.isCompressed()) {
                    int mod = size % 32;
                    if (mod != 0) {
                        size += 32 - mod;
                    }
                }
                byte[] data = new byte[size];
                len = IOUtils.read(is, data);
                if (len < data.length) {
                    throw new NxlException("Invalid nxl file");
                }
                len = section.getSectionSize() - data.length;
                if (is.skip(len) != len) {
                    throw new NxlException("Invalid nxl file");
                }
                section.setData(data);
            }
        }

        nxl.content = is;
        return nxl;
    }

    public void setContent(InputStream is) {
        content = is;
    }

    public static boolean isNxl(File file) throws IOException {
        try (InputStream fis = new FileInputStream(file)) {
            final int length = NxlFile.BASIC_HEADER_SIZE;
            ByteArrayOutputStream os = new ByteArrayOutputStream(length);
            IOUtils.copy(fis, os, 0, length, length);
            byte[] nxlHeader = os.toByteArray();
            return isNxl(nxlHeader);
        }
    }

    public static boolean isNxl(byte[] localFile) throws UnsupportedEncodingException {
        int len = localFile.length;
        if (len < BASIC_HEADER_SIZE) {
            return false;
        }
        ByteBuffer bb = ByteBuffer.wrap(localFile).asReadOnlyBuffer();
        bb.order(ByteOrder.LITTLE_ENDIAN);
        Signature signature = new Signature(bb);
        return signature.isValid();
    }

    public static NxlFile parse(byte[] file) throws IOException, NxlException {
        return parse(new ByteArrayInputStream(file));
    }

    public boolean isValid(byte[] token) {
        try {
            byte[] hmac = NxlUtils.getChecksum(token, basicHeader, 0, FIXED_HEADER_SIZE);
            if (!Arrays.equals(hmac, dynamicHeader.getChecksum())) {
                return false;
            }
            for (int i = 0; i < 32; ++i) {
                Section section = sectionHeader.getSection(0);
                if (section != null && !section.isValid(token)) {
                    return false;
                }
            }
            return true;
        } catch (GeneralSecurityException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(e.getMessage(), e);
            }
        }
        return false;
    }

    public String getDuid() {
        return Hex.toHexString(fileHeader.getDuid());
    }

    public byte[] getIv() {
        return keyHeader.getIv();
    }

    public String getOwner() {
        return fileHeader.getOwner();
    }

    public BigInteger getRootAgreement() {
        return new BigInteger(Hex.toHexString(keyHeader.getRootAgreement()), 16);
    }

    public BigInteger getIcaAgreement() {
        byte[] buf = keyHeader.getIcaAgreement();
        if (buf == null) {
            return null;
        }

        return new BigInteger(buf);
    }

    public int getMaintenanceLevel() {
        return keyHeader.getMaintenanceLevel();
    }

    public Section getSection(String name) {
        return sectionHeader.getSectionByName(name);
    }

    public long getContentLength() {
        return dynamicHeader.getContentLength();
    }

    public void writeHeader(OutputStream os, byte[] tokenOrChecksum, boolean isChecksum)
            throws IOException, GeneralSecurityException {
        updateHeader(tokenOrChecksum, isChecksum);
        os.write(basicHeader);
    }

    public void writeSectionData(OutputStream os) throws IOException, GeneralSecurityException {
        sectionHeader.writeSectionData(os);
    }

    public void writeContent(OutputStream os, byte[] token) throws IOException, GeneralSecurityException {
        byte[] iv = keyHeader.getIv();
        byte[] ecek = keyHeader.getEcek();
        byte[] cek = NxlUtils.decryptOneBlock(token, iv, ecek);

        EncryptOutputStream eos = new EncryptOutputStream(os, cek, iv);
        IOUtils.copy(content, eos);
        eos.close();
    }

    /**
     *
     * @param os
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public void writeContent(OutputStream os) throws IOException, GeneralSecurityException {
        IOUtils.copy(content, os);
    }

    public void writeFileHeader(File file, byte[] token) throws IOException, NxlException, GeneralSecurityException {
        NxlFile nxl = null;
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            nxl = NxlFile.parse(is);
        } finally {
            IOUtils.closeQuietly(nxl);
            IOUtils.closeQuietly(is);
        }
        int contentOffset = nxl.fileHeader.getContentOffset();

        if (fileHeader.getContentOffset() != contentOffset) {
            throw new NxlException("Incompatible header.");
        }

        updateHeader(token, false);
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            raf.write(nxl.basicHeader);

            sectionHeader.writeSectionData(raf);
        } finally {
            IOUtils.closeQuietly(raf);
        }
    }

    /***
     * This method return the encrypted content
     * @return InputStream to copy
     */
    public InputStream getContent() {
        return content;
    }

    public InputStream getContent(byte[] token) throws GeneralSecurityException, NxlException {
        byte[] iv = keyHeader.getIv();
        byte[] ecek = keyHeader.getEcek();
        byte[] cek = NxlUtils.decryptOneBlock(token, iv, ecek);
        byte[] hmac = keyHeader.getCekHmac();
        if (!Arrays.equals(NxlUtils.getChecksum(token, cek), hmac)) {
            throw new NxlException("Corrupted cek.");
        }

        long length = dynamicHeader.getContentLength();
        return new DecryptInputStream(content, cek, iv, length);
    }

    public InputStream getContent(InputStream fis, byte[] token) throws GeneralSecurityException, NxlException {
        byte[] iv = keyHeader.getIv();
        byte[] ecek = keyHeader.getEcek();
        byte[] cek = NxlUtils.decryptOneBlock(token, iv, ecek);
        byte[] hmac = keyHeader.getCekHmac();
        if (!Arrays.equals(NxlUtils.getChecksum(token, cek), hmac)) {
            throw new NxlException("Corrupted cek.");
        }

        long length = dynamicHeader.getContentLength();
        return new DecryptInputStream(fis, cek, iv, length);
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(content);
        content = null;
    }

    private void updateHeader(byte[] tokenOrChecksum, boolean isChecksum) throws GeneralSecurityException {
        ByteBuffer bb = ByteBuffer.wrap(basicHeader);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        signature.writeTo(bb);
        fileHeader.writeTo(bb);
        keyHeader.writeTo(bb);
        sectionHeader.writeTo(bb);
        if (extendedData == null) {
            bb.position(bb.position() + EXTENDED_DATA_SIZE);
        } else {
            bb.put(extendedData);
        }

        if (isChecksum) {
            dynamicHeader.setChecksum(tokenOrChecksum);
        } else {
            dynamicHeader.setChecksum(NxlUtils.getChecksum(tokenOrChecksum, basicHeader, 0, FIXED_HEADER_SIZE));
        }
        dynamicHeader.writeTo(bb);
    }

    public void updateSection(int idx, String name, String content, byte[] tokenOrChecksum, boolean isChecksum)
            throws GeneralSecurityException, IOException, NxlException {
        byte[] iv = keyHeader.getIv();
        Section section = NxlFile.Builder.buildSection(name, false, content, tokenOrChecksum, iv, isChecksum);
        int contentOffset = BASIC_HEADER_SIZE + idx * Section.DEFAULT_SECTION_SIZE;
        sectionHeader.setSection(idx, section, contentOffset);
    }

    public ProtectionType getProtectionType() throws GeneralSecurityException, IOException, NxlException {
        FilePolicy policy = DecryptUtil.getFilePolicy(this, null);
        List<Policy> adhocPolicies = policy.getPolicies();
        if (adhocPolicies != null && !adhocPolicies.isEmpty()) {
            return ProtectionType.ADHOC;
        } else {
            return ProtectionType.CENTRAL;
        }
    }

    public static final class Builder {

        private String fileInfo;
        private String filePolicy;
        private String fileTags;

        private NxlFile nxl = new NxlFile();

        public Builder() {
            nxl = new NxlFile();
            nxl.signature = new Signature();
            nxl.fileHeader = new FileHeader();
            nxl.keyHeader = new KeyHeader();
            nxl.sectionHeader = new SectionHeader();
            nxl.dynamicHeader = new DynamicHeader();
        }

        public Builder(NxlFile baseNxlFile) {
            this.nxl = baseNxlFile;
        }

        public Builder setDuid(byte[] duid) {
            nxl.fileHeader.setDuid(duid);
            return this;
        }

        public Builder setOwner(String owner) {
            nxl.fileHeader.setOwner(owner);
            return this;
        }

        public Builder setRootAgreement(BigInteger rootAgreement) {
            nxl.keyHeader.setRootAgreement(rootAgreement.toByteArray());
            return this;
        }

        public Builder setIcaAgreement(BigInteger icaAgreement) {
            nxl.keyHeader.setIcaAgreement(icaAgreement.toByteArray());
            return this;
        }

        public Builder setFileInfo(String fileInfo) {
            this.fileInfo = fileInfo;
            return this;
        }

        public Builder setFilePolicy(String filePolicy) {
            this.filePolicy = filePolicy;
            return this;
        }

        public Builder setFileTags(String fileTags) {
            this.fileTags = fileTags;
            return this;
        }

        public Builder setContentLength(long length) {
            nxl.dynamicHeader.setContentLength(length);
            return this;
        }

        public Builder setContent(InputStream is, long length) {
            nxl.close();
            nxl.content = is;
            nxl.dynamicHeader.setContentLength(length);
            return this;
        }

        /**
         * This method updates the section content offset with given token.
         * @param token token received from the SKYDRM Server
         * @param iv IV generated while building header
         * @throws GeneralSecurityException
         * @throws IOException
         */
        private void updateSectionContentOffset(byte[] token, byte[] iv) throws GeneralSecurityException, IOException {
            int contentOffset = BASIC_HEADER_SIZE;

            Section section = buildSection(SECTION_FILE_INFO, false, fileInfo, token, iv, false);
            nxl.sectionHeader.setSection(0, section, contentOffset);
            contentOffset += section.getSectionSize();

            section = buildSection(SECTION_FILE_POLICY, false, filePolicy, token, iv, false);
            nxl.sectionHeader.setSection(1, section, contentOffset);
            contentOffset += section.getSectionSize();

            section = buildSection(SECTION_FILE_TAGS, false, fileTags, token, iv, false);
            nxl.sectionHeader.setSection(2, section, contentOffset);
            contentOffset += section.getSectionSize();

            nxl.fileHeader.setContentOffset(contentOffset);
        }

        public NxlFile build(byte[] token, int maintenanceLevel) throws GeneralSecurityException, IOException,
                NxlException {
            byte[] ecek = nxl.keyHeader.getEcek();
            byte[] iv = nxl.keyHeader.getIv();
            if (ecek == null) {
                iv = new byte[16];
                byte[] cek = new byte[32];
                RandomUtils.nextBytes(iv);
                RandomUtils.nextBytes(cek);
                nxl.keyHeader.setCekHmac(NxlUtils.getChecksum(token, cek));

                ecek = NxlUtils.encryptOneBlock(token, iv, cek);
                nxl.keyHeader.setIv(iv);
                nxl.keyHeader.setEcek(ecek);
                nxl.keyHeader.setMaintenanceLevel(maintenanceLevel);
            } else {
                byte[] hmac = nxl.keyHeader.getCekHmac();
                byte[] cek = NxlUtils.decryptOneBlock(token, iv, ecek);
                if (!Arrays.equals(NxlUtils.getChecksum(token, cek), hmac)) {
                    throw new NxlException("Access denied.");
                }
                if (nxl.keyHeader.getMaintenanceLevel() != maintenanceLevel) {
                    throw new NxlException("MaintenanceLevel incompatible.");
                }
            }
            updateSectionContentOffset(token, iv);
            return nxl;
        }

        public static Section buildSection(String name, boolean encrypt, String data, byte[] tokenOrCheckSum, byte[] iv,
            boolean isChecksum)
                throws GeneralSecurityException, IOException {
            Section section = new Section(name);
            byte[] buf = StringUtils.toBytesQuietly(data);
            section.setOriginalDatasize((short)buf.length);
            if (buf.length > Section.DEFAULT_SECTION_SIZE) {
                section.setCompressed(true);
                buf = NxlUtils.compress(buf, 0, buf.length);
                if (buf.length > Section.DEFAULT_SECTION_SIZE) {
                    throw new IOException("Section data too large: " + buf.length);
                }
                section.setCompressedDataSize((short)buf.length);
            } else {
                section.setCompressed(false);
                section.setCompressedDataSize((short)0);
            }
            section.setEncrypted(encrypt);
            if (!isChecksum && encrypt) {
                buf = NxlUtils.encrypt(tokenOrCheckSum, iv, buf);
            }
            section.setData(buf);
            if (isChecksum) {
                section.setChecksum(tokenOrCheckSum);
            } else {
                section.setChecksum(NxlUtils.getChecksum(tokenOrCheckSum, buf));
            }
            return section;
        }

        /**
         * This method helps to decrypt key with old token and reencrypt with new token for destination copy. This method also updates the checksum of section data
         * @param oldtokenResponse  Token details of source file
         * @param newtokenResponse  New Token details for destination file
         * @param nxlFile return the new formed destination file with new token and duid.
         * @return
         * @throws GeneralSecurityException
         * @throws NxlException
         * @throws IOException
         */
        public NxlFile refreshHeader(DecryptUtil.TokenResponse oldtokenResponse,
            DecryptUtil.TokenResponse newtokenResponse, NxlFile nxlFile)
                throws GeneralSecurityException, NxlException, IOException {
            byte[] ecek = nxlFile.keyHeader.getEcek();
            byte[] iv = nxlFile.keyHeader.getIv();
            byte[] hmac = nxlFile.keyHeader.getCekHmac();
            byte[] oldTokenBuf = oldtokenResponse.getToken();
            byte[] cek = NxlUtils.decryptOneBlock(oldTokenBuf, iv, ecek);
            if (!Arrays.equals(NxlUtils.getChecksum(oldTokenBuf, cek), hmac)) {
                throw new NxlException("Access denied.");
            }
            byte[] newTokenBuf = newtokenResponse.getToken();
            nxl.keyHeader.setCekHmac(NxlUtils.getChecksum(newTokenBuf, cek));
            nxl.keyHeader.setIv(iv);
            ecek = NxlUtils.encryptOneBlock(newTokenBuf, iv, cek);
            nxl.keyHeader.setEcek(ecek);
            nxl.keyHeader.setMaintenanceLevel(newtokenResponse.getMaintenanceLevel());
            updateSectionContentOffset(newTokenBuf, iv);
            return nxl;
        }
    }
}
