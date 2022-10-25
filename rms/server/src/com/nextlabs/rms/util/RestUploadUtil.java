package com.nextlabs.rms.util;

import com.nextlabs.common.io.IOUtils;
import com.nextlabs.rms.Constants;
import com.nextlabs.rms.repository.RestUploadRequest;
import com.nextlabs.rms.shared.UploadUtil;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;

public final class RestUploadUtil {

    private RestUploadUtil() {
    }

    public static RestUploadRequest parseRestUploadRequest(HttpServletRequest request)
            throws FileUploadException, IOException {
        RestUploadRequest uploadReq = new RestUploadRequest();
        File uploadTmpDir = RepositoryFileUtil.getTempOutputFolder();
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(UploadUtil.THRESHOLD_SIZE);
        factory.setRepository(uploadTmpDir);
        ServletFileUpload upload = new ServletFileUpload();
        upload.setHeaderEncoding("UTF-8");
        upload.setFileItemFactory(factory);
        upload.setSizeMax(UploadUtil.REQUEST_SIZE);

        for (FileItem item : upload.parseRequest(request)) {
            if (Constants.API_INPUT.equals(item.getFieldName())) {
                uploadReq.setJson(item.getString("UTF-8"));
            } else if (Constants.FILE.equals(item.getFieldName())) {
                uploadReq.setFileStream(item.getInputStream());
                uploadReq.setFileName(item.getName());
            }
        }
        uploadReq.setUploadDir(uploadTmpDir);
        return uploadReq;
    }

    public static void cleanupRestUploadResources(RestUploadRequest uploadReq) {
        if (uploadReq == null) {
            return;
        }
        IOUtils.closeQuietly(uploadReq.getFileStream());
        FileUtils.deleteQuietly(uploadReq.getUploadDir());
    }
}
