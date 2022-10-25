package com.nextlabs.rms.share;

import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.common.shared.JsonSharing.JsonRecipient;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.exception.RepositoryException;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

public interface IShareService {

    JsonResponse share(HttpServletRequest request, JsonSharing shareReq);

    JsonResponse reshare(String transactionId, String comment, List<JsonRecipient> recipientList,
        boolean validateOnly, HttpServletRequest request, Object origin);

    JsonResponse update(HttpServletRequest request, String duid,
        List<JsonSharing.JsonRecipient> newRecipients,
        List<JsonSharing.JsonRecipient> removedRecipients, String comment);

    JsonResponse revoke(HttpServletRequest request, String duid);

    JsonResponse listSharedWithMeFiles(HttpServletRequest request, Integer page, Integer size,
        String orderBy, String searchString, Object spaceId);

    JsonResponse getSharedWithMeFileMetadata(HttpServletRequest request, String transactionId, String spaceId);

    Response downloadSharedWithMeFile(HttpServletResponse response, String transactionId, int start, long length,
        boolean downloadForView, Object spaceId, HttpServletRequest request)
            throws InvalidDefaultRepositoryException, RepositoryException, IOException;

    Response downloadSharedWithMeFileHeader(HttpServletResponse response, String transactionId, int start, long length,
        Object spaceId, HttpServletRequest request)
            throws InvalidDefaultRepositoryException, RepositoryException, IOException;

    Response decryptSharedWithMeFile(HttpServletRequest request, HttpServletResponse response, String transactionId,
        String spaceId);
}
