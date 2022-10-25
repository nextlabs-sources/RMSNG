package com.nextlabs.rms.repository.googledrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Get;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Constants;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.entity.setting.ServiceProviderType;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.pojo.ServiceProviderSetting;
import com.nextlabs.rms.repository.CreateFolderResult;
import com.nextlabs.rms.repository.DeleteFileMetaData;
import com.nextlabs.rms.repository.FilterOptions;
import com.nextlabs.rms.repository.IRepository;
import com.nextlabs.rms.repository.OperationResult;
import com.nextlabs.rms.repository.RepoPath;
import com.nextlabs.rms.repository.RepositoryContent;
import com.nextlabs.rms.repository.RepositoryFileManager;
import com.nextlabs.rms.repository.RepositoryManager;
import com.nextlabs.rms.repository.UploadedFileMetaData;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.MissingDependenciesException;
import com.nextlabs.rms.repository.exception.NonUniqueFileException;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.shared.Nvl;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GoogleDriveRepository implements IRepository {

    public static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    public static final String GOOGLE_NATIVE_FILES_MIME_TYPE_STARTS_WITH = "application/vnd.google-apps.";
    private final Logger logger = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final int MAX_RESULTS = 250;
    private static final String FILENAME_ATTR = "title";
    private static final String ROLE_READER = "reader";
    private static final String TYPE_ANYONE = "anyone";

    private RMSUserPrincipal userPrincipal;
    private String repoId;
    private static final ServiceProviderType REPO_TYPE = ServiceProviderType.GOOGLE_DRIVE;
    private String repoName;
    private String accountName;
    private final ServiceProviderSetting setting;
    private final Map<String, Object> attributes;
    private boolean isShared;
    private String accountId;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public GoogleDriveRepository(RMSUserPrincipal user, String repoId, ServiceProviderSetting setting) {
        this.repoId = repoId;
        this.userPrincipal = user;
        this.setting = setting;
        attributes = new HashMap<>();
    }

    @Override
    public RMSUserPrincipal getUser() {
        return userPrincipal;
    }

    @Override
    public void setUser(RMSUserPrincipal userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    @Override
    public String getRepoId() {
        return repoId;
    }

    @Override
    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    @Override
    public ServiceProviderType getRepoType() {
        return REPO_TYPE;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public List<RepositoryContent> getFileList(String path) throws RepositoryException {
        return getFileList(path, new FilterOptions());
    }

    @Override
    public List<RepositoryContent> getFileList(String path, FilterOptions filterOptions) throws RepositoryException {
        List<RepositoryContent> contentList = new ArrayList<>();
        boolean isRootFolder = false;
        try {
            if (!StringUtils.hasText(path)) {
                throw new IllegalArgumentException("Invalid path");
            }
            Drive service = getDrive();
            if ("/".equalsIgnoreCase(path)) {
                About about = service.about().get().execute();
                path = about.getRootFolderId();
                isRootFolder = true;
            }
            RepoPath repoPath = retrieveParentFromId(service, path);
            path = isRootFolder ? path : getBasePathId(path);
            Set<String> favoriteFileIdSet;
            try (DbSession session = DbSession.newSession()) {
                String parentFileId = RepositoryFileManager.getFileId(isRootFolder ? "/" : path);
                favoriteFileIdSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, getRepoId(), parentFileId);
            }
            List<com.google.api.services.drive.model.File> children = getChildren(service, path, filterOptions);
            for (com.google.api.services.drive.model.File file : children) {
                RepositoryContent content = new RepositoryContent();
                String mimeType = file.getMimeType();
                boolean isFolder = FOLDER_MIME_TYPE.equalsIgnoreCase(mimeType) && file.getFileExtension() == null;
                if (!isFolder) {
                    boolean isNxl = FileUtils.getRealFileExtension(file.getTitle()).equalsIgnoreCase(Constants.NXL_FILE_EXTN);
                    if (!isNxl && filterOptions.hideNonNxl() || isNxl && filterOptions.hideNxl()) {
                        continue;
                    }
                    String fileExtension = RepositoryFileUtil.getOriginalFileExtension(file.getTitle());
                    if (!StringUtils.hasText(fileExtension)) {
                        GoogleMimeType type = GoogleMimeType.toGoogleMimeType(mimeType);
                        if (type != null) {
                            fileExtension = type.getExtension();
                        }
                    }
                    content.setFileSize(file.getFileSize());
                    content.setFileType(fileExtension);
                    content.setProtectedFile(isNxl);
                    if (mimeType != null && mimeType.startsWith(GOOGLE_NATIVE_FILES_MIME_TYPE_STARTS_WITH)) {
                        content.setEncryptable(false);
                    }
                }
                content.setFileId(file.getId());
                content.setFavorited(favoriteFileIdSet.contains(file.getId()));
                content.setUsePathId(true);
                content.setLastModifiedTime(file.getModifiedDate().getValue());
                content.setFolder(isFolder);
                content.setName(file.getTitle());
                String rootPath = repoPath.getName() != null ? "/" + repoPath.getName() : "";
                String rootPathId = repoPath.getId() != null ? "/" + repoPath.getId() : "";
                content.setPath(rootPath + "/" + file.getTitle());
                content.setPathId(rootPathId + "/" + file.getId());
                content.setRepoId(getRepoId());
                content.setRepoName(getRepoName());
                content.setRepoType(ServiceProviderType.GOOGLE_DRIVE);
                contentList.add(content);
            }
        } catch (IOException e) {
            GoogleDriveOAuthHandler.handleException(e);
        }
        return contentList;
    }

    @Override
    public RepositoryContent getFileMetadata(String path) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    private Drive getDrive() throws InvalidTokenException {
        String refreshToken = (String)attributes.get(RepositoryManager.REFRESH_TOKEN);
        if (!StringUtils.hasText(refreshToken)) {
            throw new InvalidTokenException("Invalid token");
        }
        Credential credential = GoogleDriveOAuthHandler.buildCredential(setting);
        credential.setRefreshToken(refreshToken);
        return GoogleDriveOAuthHandler.getDrive(credential);
    }

    private RepoPath retrieveParentFromId(Drive drive, String path) throws RepositoryException {
        List<String> parentList = getParentFolderId(path);
        java.util.Collections.reverse(parentList);
        RepoPath repoPath = new RepoPath();
        try {
            for (String parent : parentList) {
                com.google.api.services.drive.model.File parentFolder = drive.files().get(parent).execute();
                if (!parentFolder.getParents().isEmpty()) {
                    String id = parentFolder.getId();
                    String title = parentFolder.getTitle();
                    repoPath.setId(repoPath.getId() != null ? id + "/" + Nvl.nvl(repoPath.getId()) : id);
                    repoPath.setName(repoPath.getName() != null ? title + "/" + Nvl.nvl(repoPath.getName()) : title);
                }
            }
        } catch (Exception e) {
            GoogleDriveOAuthHandler.handleException(e);
        }

        return repoPath;
    }

    private List<com.google.api.services.drive.model.File> getChildren(Drive service, String folderId,
        FilterOptions filterOptions)
            throws IOException {
        StringBuilder builder = new StringBuilder(50);
        builder.append('\'').append(folderId).append("' in parents and trashed = false");
        if (filterOptions.showOnlyFolders()) {
            builder.append(" and mimeType = '").append(FOLDER_MIME_TYPE).append('\'');
        }
        com.google.api.services.drive.Drive.Files.List request = service.files().list().setQ(builder.toString());
        request.setMaxResults(MAX_RESULTS);
        List<com.google.api.services.drive.model.File> fileList = new ArrayList<>();
        listFilesByPaginating(request, fileList);
        return fileList;
    }

    private void listFilesByPaginating(com.google.api.services.drive.Drive.Files.List request,
        List<com.google.api.services.drive.model.File> fileList) throws IOException {
        String npTok;
        do {
            FileList result = request.execute();

            if (result != null) {
                fileList.addAll(result.getItems());
                npTok = result.getNextPageToken(); // GET POINTER TO THE NEXT PAGE
                request.setPageToken(npTok);
            } else {
                break;
            }
        } while (npTok != null && npTok.length() > 0);
    }

    @Override
    public void refreshFileList(HttpServletRequest request, HttpServletResponse response, String path)
            throws RepositoryException {
        try {
            getFileList(path);
        } catch (Exception e) {
            GoogleDriveOAuthHandler.handleException(e);
        }
    }

    @Override
    public String getRepoName() {
        return repoName;
    }

    @Override
    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    @Override
    public File getFile(String fileId, String filePath, String outputPath) throws RepositoryException {
        File file = null;
        try {
            fileId = getBasePathId(fileId);
            Drive service = getDrive();
            Get repoFile = service.files().get(fileId);
            com.google.api.services.drive.model.File gFile = repoFile.execute();
            if (gFile == null || gFile.getExplicitlyTrashed()) {
                throw new FileNotFoundException("Unable to retrieve file in " + fileId);
            }
            String title = gFile.getTitle();
            StringBuilder builder = new StringBuilder(title.length() + 5);
            builder.append(title);
            InputStream inputStream;
            Map<String, String> exportLinks = gFile.getExportLinks();
            if (exportLinks != null) {
                String mimeTypeValue = gFile.getMimeType();
                GoogleMimeType googleMimeType = GoogleMimeType.toGoogleMimeType(mimeTypeValue);
                MimeType mimeType = MimeType.PDF;
                if (googleMimeType != null) {
                    mimeType = googleMimeType.getTargetMimeType();
                }
                if (!StringUtils.endsWithIgnoreCase(title, mimeType.getExtension())) {
                    builder.append('.');
                    builder.append(mimeType.getExtension());
                }
                String downloadURL = exportLinks.get(mimeType.getMimeType());
                inputStream = downloadFile(service, downloadURL);
            } else {
                inputStream = repoFile.executeMediaAsInputStream();
            }
            String fileName = builder.toString();
            file = new File(outputPath, fileName);
            org.apache.commons.io.FileUtils.copyInputStreamToFile(inputStream, file);
        } catch (IOException e) {
            GoogleDriveOAuthHandler.handleException(e);
        }
        return file;
    }

    @Override
    public UploadedFileMetaData uploadFile(String folderPathId, String folderPathDisplay, String srcFilePath,
        boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata)
            throws RepositoryException {
        List<RepositoryContent> result;
        com.google.api.services.drive.model.File uploadedfile;
        try {
            Drive service = getDrive();
            com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
            File file = new File(srcFilePath);
            String fileName = overwrite ? file.getName() : conflictFileName;
            body.setTitle(fileName);

            List<String> parentList = getParentFolderId(folderPathId);
            FileContent mediaContent = new FileContent(null, file);
            if (overwrite) {
                if (!parentList.isEmpty() && !folderPathId.isEmpty()) {
                    // a non-root folder
                    body.setParents(Arrays.asList(new ParentReference().setId(parentList.get(parentList.size() - 1))));
                    result = doSearch(parentList.get(parentList.size() - 1), new String[] { fileName }, true);
                } else {
                    // root folder
                    About about = service.about().get().execute();
                    result = doSearch(about.getRootFolderId(), new String[] { fileName }, true);
                }
                if (result.isEmpty()) {
                    logger.info("Adding the protected file to the repository {}", fileName);
                    uploadedfile = service.files().insert(body, mediaContent).execute();
                } else {
                    logger.info("Protected file already present. Updating the existing file {}", fileName);
                    List<String> fileIdList = getParentFolderId(result.get(0).getPathId());
                    uploadedfile = service.files().update(fileIdList.get(fileIdList.size() - 1), body, mediaContent).execute();
                }
            } else {
                if (!parentList.isEmpty() && !folderPathId.isEmpty()) {
                    body.setParents(Arrays.asList(new ParentReference().setId(parentList.get(parentList.size() - 1))));
                } else {
                    body.setParents(Arrays.asList(new ParentReference().setId(service.files().get("root").setFields("id").execute().getId())));
                }
                logger.info("Adding the protected file to the repository: {}", fileName);
                uploadedfile = service.files().insert(body, mediaContent).execute();
            }
            UploadedFileMetaData uploadedFileMetaData = new UploadedFileMetaData();
            uploadedFileMetaData.setPathDisplay((StringUtils.equals(folderPathDisplay, "/") ? "/" : folderPathDisplay + "/") + fileName);
            uploadedFileMetaData.setPathId((StringUtils.equals(folderPathId, "/") ? "/" : folderPathId + "/") + uploadedfile.getId());
            uploadedFileMetaData.setFileNameWithTimeStamp(conflictFileName);
            uploadedFileMetaData.setRepoId(getRepoId());
            uploadedFileMetaData.setRepoName(getRepoName());
            return uploadedFileMetaData;
        } catch (RepositoryException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Dropbox exception - invalid access token or network issue");
            }
            GoogleDriveOAuthHandler.handleException(e);
        } catch (IOException e) {
            logger.error("Unable to upload file (path: " + srcFilePath + "): " + e.getMessage(), e);
            GoogleDriveOAuthHandler.handleException(e);
        }
        return null;
    }

    @Override
    public DeleteFileMetaData deleteFile(String filePathId, String filePath) throws RepositoryException,
            ForbiddenOperationException {
        if ("/".equals(filePathId)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Deleting root pathId {} is forbidden", filePathId);
            }
            throw new ForbiddenOperationException("Invalid pathId: " + filePathId);
        }
        try {
            Drive service = getDrive();
            List<String> pathIdList = getParentFolderId(filePathId);
            service.files().delete(pathIdList.get(pathIdList.size() - 1)).execute();
        } catch (IOException e) {
            logger.error("Unable to delete file (path: " + filePath + "): " + e.getMessage(), e);
            GoogleDriveOAuthHandler.handleException(e);
        }
        return null;
    }

    @Override
    public CreateFolderResult createFolder(String folderName, String folderPathId, String folderPathDisplay,
        boolean autoRename)
            throws RepositoryException {
        CreateFolderResult result = null;
        try {
            if (!StringUtils.hasText(folderName) || !StringUtils.hasText(folderPathId)) {
                throw new IllegalArgumentException("Invalid folderName or folderPath");
            }
            Drive service = getDrive();
            if ("/".equalsIgnoreCase(folderPathId)) {
                About about = service.about().get().execute();
                folderPathId = about.getRootFolderId();
            } else if (folderPathId.charAt(0) == '/') {
                folderPathId = folderPathId.substring(folderPathId.lastIndexOf('/') + 1);
            }

            com.google.api.services.drive.model.File folderMetadata = new com.google.api.services.drive.model.File();
            folderMetadata.setMimeType("application/vnd.google-apps.folder");
            folderMetadata.setParents(Arrays.asList(new ParentReference().setId(folderPathId)));

            folderMetadata.setTitle(folderName);
            //            if (autoRename) {
            // TODO autoRename
            //            }
            folderMetadata = service.files().insert(folderMetadata).execute();
            if (logger.isDebugEnabled()) {
                logger.debug("Adding folder {} to the repository (repository ID: {}).", folderName, getRepoId());
            }
            result = new CreateFolderResult(folderMetadata.getId(), folderMetadata.getOriginalFilename(), folderName, new Date(folderMetadata.getModifiedDate().getValue()));
        } catch (IOException e) {
            logger.error("Unable to create folder (folder: {}, repository ID: {}): {}", folderName, getRepoId(), e.getMessage(), e);
            GoogleDriveOAuthHandler.handleException(e);
        }
        return result;
    }

    private String getParentPath(String path) {
        String parentPath = null;
        int index = path.lastIndexOf('/');
        if (path.contains("/")) {
            parentPath = path.substring(0, index);
        }
        return parentPath;
    }

    private List<String> getParentFolderId(String path) {
        List<String> parentList = new ArrayList<>();
        if (path.contains("/")) {
            String[] parentFromPath = path.split("/");
            for (String s : parentFromPath) {
                String parentPath = s.trim();
                if (parentPath.length() > 0) {
                    parentList.add(parentPath);
                }
            }
        } else {
            parentList.add(path.trim());
        }
        return parentList;
    }

    private InputStream downloadFile(Drive service, String downloadURL) {
        if (downloadURL != null && downloadURL.length() > 0) {
            try {
                HttpResponse resp = service.getRequestFactory().buildGetRequest(new GenericUrl(downloadURL)).execute();
                return resp.getContent();
            } catch (IOException e) {
                logger.error("An error occurred while trying to download a converted Google document.", e);
            }
        }
        return null;
    }

    @Override
    public byte[] downloadPartialFile(String fileId, String filePath, long startByte, long endByte)
            throws RepositoryException {
        if (endByte < startByte) {
            throw new IllegalArgumentException("Invalid range");
        }
        try {
            Drive drive = getDrive();
            fileId = getBasePathId(fileId);
            Get repoFile = drive.files().get(fileId);
            com.google.api.services.drive.model.File gFile = repoFile.execute();
            if (gFile == null || gFile.getExplicitlyTrashed()) {
                throw new FileNotFoundException("Unable to retrieve file in " + fileId);
            }
            String downloadUrl = repoFile.execute().getDownloadUrl();
            HttpRequest httpRequestGet = drive.getRequestFactory().buildGetRequest(new GenericUrl(downloadUrl));
            httpRequestGet.getHeaders().setRange("bytes=" + startByte + '-' + endByte);
            HttpResponse response = httpRequestGet.execute();
            InputStream is = null;
            ByteArrayOutputStream os = null;
            try {
                is = response.getContent();
                os = new ByteArrayOutputStream((int)(endByte - startByte + 1));
                IOUtils.copy(is, os);
                return os.toByteArray();
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
                response.disconnect();
            }
        } catch (IOException e) {
            GoogleDriveOAuthHandler.handleException(e);
        }
        return null;
    }

    @Override
    public OperationResult copyFile(String fromObject, String toObject) throws RepositoryException {
        return null;
    }

    @Override
    public List<OperationResult> copyFolder(String fromObject, String toObject) throws RepositoryException {
        return null;
    }

    @Override
    public OperationResult moveFile(String fromObject, String toObject) throws RepositoryException {
        return null;
    }

    @Override
    public List<OperationResult> moveFolder(String fromObject, String toObject) throws RepositoryException {
        return null;
    }

    public Map<String, List<String>> findPath(String currId,
        Map<String, com.google.api.services.drive.model.File> fileMap,
        Map<String, Map<String, List<String>>> map) {
        List<String> currPaths = map.get("paths").get(currId);
        List<String> currIds = map.get("ids").get(currId);

        if (currPaths == null) {
            currPaths = new ArrayList<>();
            currIds = new ArrayList<>();

            com.google.api.services.drive.model.File currFile = fileMap.get(currId);
            if (currFile == null) {
                return null;
            }

            String currPath = currFile.getTitle();

            List<ParentReference> parents = currFile.getParents();
            for (ParentReference parent : parents) {
                if (parent.getIsRoot()) {
                    currPaths.add("root");
                    currIds.add(parent.getId());
                } else {
                    Map<String, List<String>> result = findPath(parent.getId(), fileMap, map);
                    if (result != null) {
                        currPaths.addAll(result.get("paths"));
                        currIds.addAll(result.get("ids"));
                    }
                }
            }

            for (int i = 0; i < currPaths.size(); ++i) {
                String path = currPaths.get(i);
                String id = currIds.get(i);

                if ("root".equals(path)) {
                    currPaths.set(i, "/" + currPath);
                    currIds.set(i, "/" + currId);
                } else {
                    currPaths.set(i, path + "/" + currPath);
                    currIds.set(i, id + "/" + currId);
                }
            }
            map.get("paths").put(currId, currPaths);
            map.get("ids").put(currId, currIds);
        }

        Map<String, List<String>> result = new HashMap<>();
        result.put("paths", currPaths);
        result.put("ids", currIds);

        return result;
    }

    @Override
    public List<RepositoryContent> search(String searchString) throws RepositoryException {
        return doSearch(null, new String[] { searchString }, false);
    }

    private String getBasePathId(String pathId) {
        if (!StringUtils.hasText(pathId)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        int idx = pathId.lastIndexOf('/');
        pathId = idx >= 0 ? pathId.substring(idx + 1) : pathId;
        return pathId;
    }

    private void listItems(Drive service, String query, List<com.google.api.services.drive.model.File> files)
            throws IOException {
        Drive.Files.List list = service.files().list();
        if (StringUtils.hasText(query)) {
            list.setQ(query);
        }
        list.setMaxResults(MAX_RESULTS);
        listFilesByPaginating(list, files);
    }

    private List<RepositoryContent> doSearch(String parentPathId, String[] keywords, boolean exactMatch)
            throws RepositoryException {
        List<RepositoryContent> searchResults = new ArrayList<>();
        StringBuilder builder = new StringBuilder(80);
        builder.append("(((");
        for (int i = 0; i < keywords.length; i++) {
            if (i > 0) {
                builder.append(" or ");
            }
            builder.append(FILENAME_ATTR).append(" contains '").append(keywords[i]).append('\'');
        }
        builder.append(") ");
        if (StringUtils.hasText(parentPathId)) {
            String pathId = getBasePathId(parentPathId);
            builder.append("and '").append(pathId).append("' in parents");
        }
        builder.append(") or mimeType = '").append(FOLDER_MIME_TYPE).append("') and trashed = false");

        List<com.google.api.services.drive.model.File> files = new ArrayList<>();
        try {
            Drive service = getDrive();
            listItems(service, builder.toString(), files);
        } catch (IOException e) {
            GoogleDriveOAuthHandler.handleException(e);
        }

        Map<String, com.google.api.services.drive.model.File> fileMap = new HashMap<>();
        for (com.google.api.services.drive.model.File file : files) {
            fileMap.put(file.getId(), file);
        }

        Map<String, Map<String, List<String>>> pathsMap = new HashMap<>();
        pathsMap.put("ids", new HashMap<>());
        pathsMap.put("paths", new HashMap<>());

        HashSet<String> set = new HashSet<>();
        Locale locale = Locale.getDefault();
        for (String keyword : keywords) {
            set.add(keyword.toLowerCase(locale));
        }

        Set<String> favoriteFileIdSet;
        try (DbSession session = DbSession.newSession()) {
            favoriteFileIdSet = RepositoryFileManager.getSetOfFavoriteFileIds(session, getRepoId(), null);
        }

        for (com.google.api.services.drive.model.File file : files) {
            boolean match = false;
            String title = file.getTitle().toLowerCase(locale);
            if (exactMatch) {
                match = set.contains(title);
            } else {
                for (String keyword : set) {
                    if (title.contains(keyword)) {
                        match = true;
                        break;
                    }
                }
            }
            if (match) {
                Map<String, List<String>> paths = findPath(file.getId(), fileMap, pathsMap);
                for (int i = 0; i < paths.get("paths").size(); ++i) {
                    RepositoryContent sr = new RepositoryContent();
                    sr.setUsePathId(true);
                    sr.setFileId(file.getId());
                    sr.setPath(paths.get("paths").get(i));
                    sr.setPathId(paths.get("ids").get(i));
                    sr.setName(file.getTitle());
                    sr.setRepoId(repoId);
                    sr.setRepoType(REPO_TYPE);
                    sr.setRepoName(repoName);
                    sr.setFileSize(file.getFileSize());
                    sr.setLastModifiedTime(file.getModifiedDate().getValue());
                    sr.setFolder(file.getMimeType().equals(FOLDER_MIME_TYPE));
                    if (!sr.isFolder()) {
                        sr.setFileType(RepositoryFileUtil.getOriginalFileExtension(file.getTitle()));
                        sr.setProtectedFile(FileUtils.getRealFileExtension(file.getTitle()).equals(Constants.NXL_FILE_EXTN));
                        if (file.getMimeType() != null && file.getMimeType().startsWith(GOOGLE_NATIVE_FILES_MIME_TYPE_STARTS_WITH)) {
                            sr.setEncryptable(false);
                        }
                    }
                    sr.setFavorited(favoriteFileIdSet.contains(file.getId()));
                    searchResults.add(sr);
                }
            }
        }
        return searchResults;
    }

    private List<RepositoryContent> search(String parentPathId, String[] keywords) throws RepositoryException {
        return doSearch(parentPathId, keywords, true);
    }

    private List<RepositoryContent> searchWithNoDuplicate(String parentPathId,
        String[] filenames) throws RepositoryException {
        Set<String> filenameSet = new HashSet<>();
        List<RepositoryContent> search = search(parentPathId, filenames);
        if (!search.isEmpty()) {
            for (RepositoryContent result : search) {
                String name = result.getName();
                if (filenameSet.contains(name.toLowerCase())) {
                    throw new NonUniqueFileException(name + " is duplicated in repository", name);
                }
                filenameSet.add(name.toLowerCase());
            }
        }
        return search;
    }

    @Override
    public List<File> downloadFiles(String filePathId, String filePath, String[] filenames, String outputPath)
            throws RepositoryException, MissingDependenciesException {
        List<File> downloadedFiles = new ArrayList<File>();
        List<String> missingFiles = new ArrayList<String>();
        String parentPathId = filePathId.substring(0, filePathId.lastIndexOf('/'));
        try {
            List<RepositoryContent> list = searchWithNoDuplicate(parentPathId, filenames);
            for (RepositoryContent searchResult : list) {
                String filename = searchResult.getName();
                String pathId = searchResult.getPathId();
                String path = searchResult.getPath();
                File file = null;
                try {
                    file = getFile(pathId, path, outputPath);
                } catch (FileNotFoundException e) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(e.getMessage(), e);
                    }
                }
                if (file == null || !file.exists()) {
                    missingFiles.add(filename);
                } else {
                    downloadedFiles.add(file);
                }
            }
        } catch (Exception e) {
            GoogleDriveOAuthHandler.handleException(e);
        }
        if (!missingFiles.isEmpty()) {
            throw new MissingDependenciesException(missingFiles);
        }
        return downloadedFiles;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public void setShared(boolean shared) {
        this.isShared = shared;
    }

    @Override
    public boolean isShared() {
        return isShared;
    }

    @Override
    public String getCurrentFolderPathId(String objectPathId, String objectPathDisplay) throws RepositoryException {
        String currentFolderPathId = null;
        if (objectPathId.isEmpty()) {
            throw new IllegalArgumentException("Empty object pathID!");
        }
        try {
            // check if the object is a file
            Drive service = getDrive();
            Get repoFile = service.files().get(getBasePathId(objectPathId));
            com.google.api.services.drive.model.File gFile = repoFile.execute();
            if (!gFile.getMimeType().equals(FOLDER_MIME_TYPE)) {
                currentFolderPathId = objectPathId.substring(0, objectPathId.lastIndexOf('/'));
            } else {
                currentFolderPathId = objectPathId;
            }
        } catch (IOException e) {
            GoogleDriveOAuthHandler.handleException(e);
        }
        return currentFolderPathId;
    }

    @Override
    public String getCurrentFolderPathDisplay(String objectPathId, String objectPathDisplay)
            throws RepositoryException {
        String currentFoldPathId = getCurrentFolderPathId(objectPathId, objectPathDisplay);
        if (currentFoldPathId.equals(objectPathId)) {
            // current object is a folder
            return objectPathDisplay;
        } else {
            return getParentPath(objectPathDisplay);
        }
    }

    @Override
    public String getPublicUrl(String pathId) throws RepositoryException {
        Drive service = getDrive();
        PermissionList permissions;
        try {
            String fileId = getBasePathId(pathId);
            permissions = service.permissions().list(fileId).execute();
            boolean permissionExists = false;
            for (Permission permission : permissions.getItems()) {
                if (permission.getRole().equals(ROLE_READER)) {
                    permissionExists = true;
                    if (!permission.getType().equals(TYPE_ANYONE)) {
                        String permissionId = permission.getId();
                        permission.setType(TYPE_ANYONE).setWithLink(true);
                        Permission p = service.permissions().update(fileId, permissionId, permission).execute();
                        if (logger.isTraceEnabled()) {
                            logger.trace("Permission for {} updated from {} to {}", permission.getRole(), permission.getType(), p.getType());
                        }
                    }
                    break;
                }
            }
            if (!permissionExists) {
                Permission permission = service.permissions().insert(fileId, new Permission().setRole(ROLE_READER).setType(TYPE_ANYONE).setWithLink(true)).execute();
                if (logger.isTraceEnabled()) {
                    logger.trace("Permission added: {}={}", permission.getRole(), permission.getType());
                }
            }
            return service.files().get(fileId).execute().getWebContentLink();
        } catch (IOException e) {
            GoogleDriveOAuthHandler.handleException(e);
        }
        return null;
    }

    enum MimeType {
        XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
        DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
        PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
        PNG("image/png", "png"),
        SVG("image/svg+xml", "svg"),
        ZIP("application/zip", "zip"),
        PDF("application/pdf", "pdf");

        private final String mimeType;
        private final String extension;

        MimeType(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }

        public String getExtension() {
            return extension;
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    enum GoogleMimeType {
        GOOGLE_SHEETS("application/vnd.google-apps.spreadsheet", "gsheet", MimeType.XLSX),
        GOOGLE_PRESENTATION("application/vnd.google-apps.presentation", "gslides", MimeType.PPTX),
        GOOGLE_DOCUMENT("application/vnd.google-apps.document", "gdoc", MimeType.DOCX),
        GOOGLE_DRAWING("application/vnd.google-apps.drawing", "gdraw", MimeType.PNG);

        private final String sourceMimeType;
        private final String extension;
        private final MimeType targetMimeType;

        GoogleMimeType(String sourceMimeType, String extension, MimeType mimeType) {
            this.sourceMimeType = sourceMimeType;
            this.targetMimeType = mimeType;
            this.extension = extension;
        }

        public String getSourceMimeType() {
            return sourceMimeType;
        }

        public MimeType getTargetMimeType() {
            return targetMimeType;
        }

        public String getExtension() {
            return extension;
        }

        public static GoogleMimeType toGoogleMimeType(String src) {
            GoogleMimeType[] values = values();
            for (GoogleMimeType type : values) {
                if (StringUtils.equalsIgnoreCase(src, type.getSourceMimeType())) {
                    return type;
                }
            }
            return null;
        }
    }

    @Override
    public Usage calculateRepoUsage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleRepoException(Exception e) throws RepositoryException {
        GoogleDriveOAuthHandler.handleException(e);
    }

    @Override
    public ServiceProviderSetting getSetting() {
        return setting;
    }

    @Override
    public UploadedFileMetaData uploadFile(String filePathId, String filePath, String localFile, boolean overwrite,
        String conflictFileName, Map<String, String> customMetadata, boolean userConfirmedFileOverwrite)
            throws RepositoryException {
        return uploadFile(filePathId, filePath, localFile, overwrite, conflictFileName, customMetadata);
    }
}
