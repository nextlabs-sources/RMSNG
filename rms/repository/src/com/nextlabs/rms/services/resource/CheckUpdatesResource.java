package com.nextlabs.rms.services.resource;

import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.config.Constants;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.shared.LogConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import noNamespace.ArchitectureType;
import noNamespace.CheckUpdatesDocument;
import noNamespace.CheckUpdatesRequestType;
import noNamespace.CheckUpdatesResponseType;
import noNamespace.CheckUpdatesType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class CheckUpdatesResource extends ServerResource {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Post
    public Representation doPost(Representation entity) throws XmlException, IOException {
        StringRepresentation response = null;
        try {
            String xml = entity.getText();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("CheckUpdatesRequest: {}", xml);
            }
            CheckUpdatesDocument doc = CheckUpdatesDocument.Factory.parse(xml);
            CheckUpdatesType request = doc.getCheckUpdates();
            CheckUpdatesDocument checkUpdatesResponse = checkUpdates(request);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("CheckUpdatesResponse: {}", checkUpdatesResponse);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            checkUpdatesResponse.save(baos);
            response = new StringRepresentation(baos.toString(StandardCharsets.UTF_8.name()), MediaType.TEXT_PLAIN);
        } catch (XmlException e) {
            LOGGER.error("Error occurred when handling POST request for CheckUpdates: {}", e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Error occurred when handling POST request for CheckUpdates", e);
        }
        return response;
    }

    private CheckUpdatesDocument checkUpdates(CheckUpdatesType checkUpdatesType) {
        CheckUpdatesRequestType request = checkUpdatesType.getCheckUpdatesRequest();
        String currentVersion = request.getCurrentVersion();
        ArchitectureType.Enum clientArch = request.getArchitecture();
        CheckUpdatesResponseType response = CheckUpdatesResponseType.Factory.newInstance();
        CheckUpdatesDocument doc = CheckUpdatesDocument.Factory.newInstance();
        String tenantId = request.getTenantId();

        if (tenantId == null || !StringUtils.hasText(tenantId)) {
            doc.setCheckUpdates(checkUpdatesType);
            return doc;
        }

        DbSession session = DbSession.newSession();
        Tenant tenant = null;
        try {
            session.beginTransaction();
            String tenantName = "jt2go.plm.siemens.skydrm.com".equalsIgnoreCase(tenantId) ? "jt2go" : tenantId;
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", tenantName));
            tenant = (Tenant)criteria.uniqueResult();
        } finally {
            session.close();
        }

        if (tenant == null) {
            LOGGER.error("Unable to lookup tenant with name '{}'", tenantId);
            doc.setCheckUpdates(checkUpdatesType);
            return doc;
        }

        String preferences = tenant.getPreference();
        Map<String, Object> settings = GsonUtils.GSON.fromJson(preferences, GsonUtils.GENERIC_MAP_TYPE);
        String currentVersionFromDB = (String)settings.get(Constants.RMC_CURRENT_VERSION);
        Boolean force = (Boolean)settings.get(Constants.RMC_FORCE_DOWNGRADE);
        if (StringUtils.hasText(currentVersionFromDB) && (force != null && force && !currentVersion.equals(currentVersionFromDB) || isNewer(currentVersionFromDB, currentVersion))) {
            if (ArchitectureType.X_32_BIT.equals(clientArch)) {
                String checksumStr = (String)settings.get(Constants.RMC_CRC_CHECKSUM_32BITS);
                long checksum = -1L;

                try {
                    checksum = Long.parseLong(checksumStr);
                    response.setCheckSum(checksum);
                } catch (NumberFormatException nfe) {
                    LOGGER.error("32 bit RMC package checksum not specified or improper. tenant: {}", tenantId, nfe);
                    doc.setCheckUpdates(checkUpdatesType);
                    return doc;
                }
                response.setDownloadURL((String)settings.get(Constants.RMC_UPDATE_URL_32BITS));
            } else {
                String checksumStr = (String)settings.get(Constants.RMC_CRC_CHECKSUM_64BITS);
                long checksum = -1L;

                try {
                    checksum = Long.parseLong(checksumStr);
                    response.setCheckSum(checksum);
                } catch (NumberFormatException nfe) {
                    LOGGER.error("64 bit RMC package checksum not specified or improper. tenant: {}", tenantId, nfe);
                    doc.setCheckUpdates(checkUpdatesType);
                    return doc;
                }
                response.setDownloadURL((String)settings.get(Constants.RMC_UPDATE_URL_64BITS));
            }
            response.setNewVersion(currentVersionFromDB);
            checkUpdatesType.setCheckUpdatesResponse(response);
            checkUpdatesType.setCheckUpdatesRequest(null);
        }
        doc.setCheckUpdates(checkUpdatesType);
        return doc;
    }

    private static boolean isNewer(String targetVersion, String currentVersion) {
        String[] targetParts = targetVersion.split("\\.");
        String[] currentParts = currentVersion.split("\\.");
        int length = Math.max(targetParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int target = i < targetParts.length ? Integer.parseInt(targetParts[i]) : 0;
            int current = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            if (target < current) {
                return false;
            }
            if (target > current) {
                return true;
            }
        }
        return false;
    }
}
