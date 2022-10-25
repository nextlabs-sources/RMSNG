/**
 *
 */
package com.nextlabs.rms.services.resource;

import com.nextlabs.rms.auth.AuthManager;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.BadRequestException;
import com.nextlabs.rms.exception.DuplicateRepositoryNameException;
import com.nextlabs.rms.exception.ForbiddenOperationException;
import com.nextlabs.rms.exception.RepositoryAlreadyExists;
import com.nextlabs.rms.exception.RepositoryNotFoundException;
import com.nextlabs.rms.exception.UnSupportedStorageProviderException;
import com.nextlabs.rms.exception.UnauthorizedOperationException;
import com.nextlabs.rms.exception.UserNotFoundException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.repository.exception.FileNotFoundException;
import com.nextlabs.rms.rmc.StatusTypeEnum;
import com.nextlabs.rms.rmc.types.StatusType;
import com.nextlabs.rms.shared.LogConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

/**
 * @author nnallagatla
 *
 */
public abstract class AbstractSyncResource extends ServerResource {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Post
    public final Representation doPost(Representation entity) throws IOException {
        Representation response = null;
        try {
            response = handlePost(entity);
        } catch (XmlException e) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            response = getErrorResponse(StatusTypeEnum.BAD_REQUEST.getCode(), StatusTypeEnum.BAD_REQUEST.getMessageLabel());
            LOGGER.error("Error parsing request for {}", getResourceName(), e);
        } catch (UserNotFoundException e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            response = getErrorResponse(StatusTypeEnum.USER_NOT_FOUND.getCode(), StatusTypeEnum.USER_NOT_FOUND.getMessageLabel());
            LOGGER.error("User not found for {} (userId: {}).", getResourceName(), e.getUserId(), e);
        } catch (RepositoryNotFoundException e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            response = getErrorResponse(StatusTypeEnum.REPO_NOT_FOUND.getCode(), StatusTypeEnum.REPO_NOT_FOUND.getMessageLabel());
            LOGGER.error("Repository not found for {} (repoId:{}).", getResourceName(), e.getRepoId(), e);
        } catch (RepositoryAlreadyExists e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            response = getErrorResponse(StatusTypeEnum.REPO_ALREADY_EXISTS.getCode(), StatusTypeEnum.REPO_ALREADY_EXISTS.getMessageLabel());
            LOGGER.error("Repository Already exists {}", getResourceName(), e);
        } catch (DuplicateRepositoryNameException e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            response = getErrorResponse(StatusTypeEnum.DUPLICATE_REPO_NAME.getCode(), StatusTypeEnum.DUPLICATE_REPO_NAME.getMessageLabel());
            LOGGER.error("Duplicate Repository Name {}", getResourceName(), e);
        } catch (UnSupportedStorageProviderException e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            response = getErrorResponse(StatusTypeEnum.UNSUPPORTED_STORAGE_PROVIDER.getCode(), StatusTypeEnum.UNSUPPORTED_STORAGE_PROVIDER.getMessageLabel());
            LOGGER.error("Unsupported Storage Provider {}", getResourceName(), e);
        } catch (Exception e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            response = getErrorResponse(StatusTypeEnum.UNKNOWN.getCode(), StatusTypeEnum.UNKNOWN.getMessageLabel());
            LOGGER.error("Error occurred when handling POST request for {}: {}", getResourceName(), e.getMessage(), e);
        }
        return response;
    }

    /**
     *
     * @param entity
     * @return
     * @throws XmlException
     * @throws UserNotFoundException
     * @throws RepositoryNotFoundException
     * @throws IOException
     * @throws FileNotFoundException
     * @throws RepositoryAlreadyExists
     * @throws DuplicateRepositoryNameException
     * @throws UnSupportedStorageProviderException
     */
    protected abstract Representation handlePost(Representation entity) throws XmlException, UserNotFoundException,
            RepositoryNotFoundException, IOException, FileNotFoundException, RepositoryAlreadyExists,
            UnauthorizedOperationException, DuplicateRepositoryNameException, UnSupportedStorageProviderException,
            BadRequestException, ForbiddenOperationException;

    protected abstract Representation getErrorResponse(int errCode, String errMsg) throws IOException;

    protected String getResourceName() {
        return this.getClass().getName();
    }

    protected StatusType getStatus(int statusCode, String statusMsgLabel) {
        StatusType status = StatusType.Factory.newInstance();
        status.setCode(statusCode);
        status.setMessage(RMSMessageHandler.getClientString(statusMsgLabel));
        return status;
    }

    protected StringRepresentation getStringRepresentation(XmlObject doc) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return new StringRepresentation(baos.toString(StandardCharsets.UTF_8.name()), MediaType.TEXT_PLAIN);
    }

    protected RMSUserPrincipal authenticateRequest() throws UserNotFoundException {
        Request restletRequest = getRequest();
        HttpServletRequest servletRequest = ServletUtils.getRequest(restletRequest);

        DbSession session = DbSession.newSession();
        try {
            RMSUserPrincipal userPrincipal = AuthManager.authenticate(session, servletRequest);

            if (userPrincipal == null) {
                throw new UserNotFoundException();
            }

            return userPrincipal;
        } finally {
            session.close();
        }
    }

}
