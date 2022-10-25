package com.nextlabs.rms.viewer.repository;

import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.rms.eval.User;
import com.nextlabs.rms.viewer.conversion.WaterMark;
import com.nextlabs.rms.viewer.json.RepoFile;
import com.nextlabs.rms.viewer.json.SharedFile;

import java.io.Serializable;
import java.util.Map;

import org.apache.tika.mime.MediaType;

public class CachedFile implements Serializable {

    private static final long serialVersionUID = -6960898640377591668L;

    private RepoFile repoFile;
    private SharedFile sharedFile;
    private String efsId;
    private String projectId;

    private byte[] fileContent;
    private String fileName;
    private String originalFileName;
    private WaterMark waterMark;

    private boolean isOwner;
    private String duid;
    private String membership;
    private int rights;
    private Map<String, String[]> tagMap;
    private User viewer;

    private long lastModifiedDate;
    private long fileSize;
    private JsonExpiry validity;

    private ContentType type;
    private MediaType mediaType;
    private int protectionType = -1;

    private BucketName bucketName;
    private byte[] watermarkPNG;

    public BucketName getBucketName() {
        return bucketName;
    }

    public void setBucketName(BucketName bucketName) {
        this.bucketName = bucketName;
    }

    public enum BucketName {
        LOCAL,
        PROJECT,
        WORKSPACE
    }

    public enum ContentType {
        _2D,
        _3D
    }

    public CachedFile(byte[] fileContent, String fileName, String originalFileName, WaterMark waterMarkObj,
        String duid, String membership,
        int rights, Map<String, String[]> tagMap, User viewer, int protectionType) {
        this.fileContent = fileContent;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.waterMark = waterMarkObj;
        this.duid = duid;
        this.membership = membership;
        this.rights = rights;
        this.tagMap = tagMap;
        this.viewer = viewer;
        this.protectionType = protectionType;
    }

    public RepoFile getRepoFile() {
        return repoFile;
    }

    public void setRepoFile(RepoFile repoFile) {
        this.repoFile = repoFile;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    public String getFileName() {
        return fileName;
    }

    public WaterMark getWaterMark() {
        return waterMark;
    }

    public void setWaterMark(WaterMark waterMark) {
        this.waterMark = waterMark;
    }

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setContentType(ContentType type) {
        this.type = type;
    }

    public ContentType getContentType() {
        return type;
    }

    public Map<String, String[]> getTagMap() {
        return tagMap;
    }

    public void setTagMap(Map<String, String[]> tagMap) {
        this.tagMap = tagMap;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public int getRights() {
        return rights;
    }

    public void setRights(int rights) {
        this.rights = rights;
    }

    public String getDuid() {
        return duid;
    }

    public String getMembership() {
        return membership;
    }

    public String getEfsId() {
        return efsId;
    }

    public void setEfsId(String efsId) {
        this.efsId = efsId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public SharedFile getSharedFile() {
        return sharedFile;
    }

    public void setSharedFile(SharedFile sharedFile) {
        this.sharedFile = sharedFile;
    }

    public User getViewer() {
        return viewer;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean isOwner) {
        this.isOwner = isOwner;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public JsonExpiry getValidity() {
        return validity;
    }

    public void setValidity(JsonExpiry validity) {
        this.validity = validity;
    }

    public int getProtectionType() {
        return protectionType;
    }

    public void setProtectionType(int protectionType) {
        this.protectionType = protectionType;
    }

    public byte[] getWatermarkPNG() {
        return watermarkPNG;
    }

    public void setWatermarkPNG(byte[] watermarkPNG) {
        this.watermarkPNG = watermarkPNG;
    }
}
