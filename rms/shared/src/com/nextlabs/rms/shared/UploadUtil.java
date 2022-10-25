package com.nextlabs.rms.shared;

import com.nextlabs.common.shared.Constants.SPACETYPE;
import com.nextlabs.common.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.CRC32;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class UploadUtil {

    public static final String FILE = "file";
    public static final String CONFLICT_FILE_NAME = "conflictFileName";
    public static final String USER_NAME = "userName";
    public static final String OFFSET = "offset";
    public static final String TENANT_ID = "tenantId";
    public static final String CLIENT_ID = "clientId";
    public static final String TENANT_NAME = "tenantName";
    public static final String TICKET = "ticket";
    public static final String UID = "uid";
    public static final String PARAM_SHARE_WITH = "shareWith";
    public static final String RIGHTS_JSON = "rightsJSON";
    public static final String REPOID = "repoId";
    public static final String FILEPATH_ID = "filePathId";
    public static final String FILEPATH_DISPLAY = "filePathDisplay";
    public static final String PARAM_IS_FILE_ATTACHED = "isFileAttached";
    public static final String PARAM_USER_FILE_OVERWRITE = "userConfirmedFileOverwrite";
    public static final String PARAM_COMMENT = "comment";
    public static final String PARAM_WATERMARK = "watermark";
    public static final String PARAM_EXPIRY = "expiry";
    public static final int THRESHOLD_SIZE = 1024 * 100; // 100KB
    public static final long REQUEST_SIZE = 1024 * 1024 * 150L; // 150MB

    private static final Logger LOGGER = LogManager.getLogger(UploadUtil.class);

    private UploadUtil() {

    }

    public static void sendErrorResponse(String originalName, String errorMessage, String value,
        HttpServletResponse response) {
        sendErrorResponse(originalName, errorMessage, value, 200, response);
    }

    public static void sendErrorResponse(String originalName, String errorMessage, String value, int statusCode,
        HttpServletResponse response) {
        UploadFileResponse fileResponse = new UploadFileResponse();
        fileResponse.setName(originalName);
        fileResponse.setError(errorMessage);
        fileResponse.setStatusCode(statusCode);
        JsonUtil.writeJsonToResponse(fileResponse, response);
        if ("".equals(value) || !value.contains("application/json")) {
            response.setContentType("text/plain");
        }
    }

    public static UploadFileRequest readFile(HttpServletRequest request, int thresholdSize,
        long requestSize) throws FileUploadException, UnsupportedEncodingException {
        if (thresholdSize <= 0) {
            thresholdSize = THRESHOLD_SIZE;
        }
        if (requestSize <= 0) {
            requestSize = REQUEST_SIZE;
        }
        if (!StringUtils.hasText(request.getCharacterEncoding())) {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(thresholdSize);
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setHeaderEncoding("UTF-8");
        upload.setSizeMax(requestSize);
        List<FileItem> list = upload.parseRequest(request);
        Iterator<FileItem> iter = list.iterator(); // parses the request's content to extract file data
        String originalName = "";
        String conflictFileName = "";
        String userName = "";
        String offset = "";
        String tenantId = "";
        String tenantName = "";
        String ticket = "";
        String rightsJSON = "[]";
        String shareWithJSON = "[]";
        String repoId = "";
        String filePathId = "";
        String displayPath = "";
        String uid = "";
        String isFileAttached = "";
        String userConfirmedFileOverwrite = "";
        String clientId = "";
        byte[] bytes = null;
        String comment = null;
        String watermark = null;
        String expiry = null;
        try {
            while (iter.hasNext()) {
                FileItem item = iter.next();
                if (item.isFormField()) {
                    if (USER_NAME.equals(item.getFieldName())) {
                        userName = item.getString("UTF-8");
                    } else if (OFFSET.equals(item.getFieldName())) {
                        offset = item.getString("UTF-8");
                    } else if (TENANT_ID.equals(item.getFieldName())) {
                        tenantId = item.getString("UTF-8");
                    } else if (TICKET.equals(item.getFieldName())) {
                        ticket = item.getString("UTF-8");
                    } else if (UID.equals(item.getFieldName())) {
                        uid = item.getString("UTF-8");
                    } else if (TENANT_NAME.equals(item.getFieldName())) {
                        tenantName = item.getString("UTF-8");
                    } else if (RIGHTS_JSON.equals(item.getFieldName())) {
                        rightsJSON = item.getString("UTF-8");
                    } else if (PARAM_SHARE_WITH.equals(item.getFieldName())) {
                        shareWithJSON = item.getString("UTF-8");
                    } else if (REPOID.equals(item.getFieldName())) {
                        repoId = item.getString("UTF-8");
                    } else if (FILEPATH_ID.equals(item.getFieldName())) {
                        filePathId = item.getString("UTF-8");
                    } else if (FILEPATH_DISPLAY.equals(item.getFieldName())) {
                        displayPath = item.getString("UTF-8");
                    } else if (CONFLICT_FILE_NAME.equals(item.getFieldName())) {
                        conflictFileName = item.getString("UTF-8");
                    } else if (PARAM_IS_FILE_ATTACHED.equals(item.getFieldName())) {
                        isFileAttached = item.getString("UTF-8");
                    } else if (PARAM_USER_FILE_OVERWRITE.equals(item.getFieldName())) {
                        userConfirmedFileOverwrite = item.getString("UTF-8");
                    } else if (CLIENT_ID.equals(item.getFieldName())) {
                        clientId = item.getString("UTF-8");
                    } else if (PARAM_COMMENT.equals(item.getFieldName())) {
                        comment = item.getString("UTF-8");
                    } else if (PARAM_WATERMARK.equals(item.getFieldName())) {
                        watermark = item.getString("UTF-8");
                    } else if (PARAM_EXPIRY.equals(item.getFieldName())) {
                        expiry = item.getString("UTF-8");
                    }
                } else {
                    if (FILE.equals(item.getFieldName())) {
                        bytes = new byte[(int)item.getSize()];
                        try {
                            byte[] bs = item.get();
                            System.arraycopy(bs, 0, bytes, 0, bs.length);
                            StringTokenizer fileTokenizer = new StringTokenizer(item.getName(), "\\");
                            while (fileTokenizer.hasMoreTokens()) {
                                originalName = fileTokenizer.nextToken();
                            }

                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }
                }
            }
        } finally {
            if (!list.isEmpty()) {
                for (FileItem item : list) {
                    item.delete();
                }
            }
            list.clear();
        }
        UploadFileRequest upRequest = new UploadFileRequest(originalName, bytes, userName, offset, tenantId);
        upRequest.setTenantName(tenantName);
        upRequest.setTicket(ticket);
        upRequest.setUid(uid);
        upRequest.setRightsJSON(rightsJSON);
        upRequest.setShareWithJSON(shareWithJSON);
        upRequest.setRepoId(repoId);
        upRequest.setFilePathId(filePathId);
        upRequest.setFilePathDisplay(displayPath);
        upRequest.setConflictFileName(conflictFileName);
        upRequest.setClientId(clientId);
        upRequest.setFileAttached(!StringUtils.hasText(isFileAttached) || Boolean.parseBoolean(isFileAttached));
        upRequest.setUserConfirmedFileOverwrite(StringUtils.hasText(userConfirmedFileOverwrite) && Boolean.parseBoolean(userConfirmedFileOverwrite));
        upRequest.setComment(comment);
        upRequest.setWatermark(watermark);
        upRequest.setExpiry(expiry);
        return upRequest;
    }

    public static String getUniqueResourceId(SPACETYPE spaceType, String spaceId, String fileParentPathId,
        String fileName)
            throws UnsupportedEncodingException {
        String resourceId = spaceType + spaceId + fileParentPathId + fileName;
        CRC32 crc = new CRC32();
        crc.update(resourceId.getBytes(StandardCharsets.UTF_8.name()));
        long crcValue = crc.getValue();
        return String.valueOf(crcValue);
    }

    public static File writeFileToDisk(byte[] bytes, String destFilePath) throws IOException {
        File file = null;
        FileOutputStream fos = null;
        try {
            file = new File(destFilePath);
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                boolean created = parentFile.mkdirs();
                if (!created) {
                    LOGGER.warn("Unable to create folder: {}", parentFile.getAbsolutePath());
                }
            }
            fos = new FileOutputStream(file);
            fos.write(bytes);
        } catch (IOException e) {
            LOGGER.error("Error occurred while writing encrypted file to disk: {}", e.getMessage(), e);
            throw e;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    LOGGER.error("Error occurred while closing stream", e);
                }
            }
        }
        return file;
    }
}
