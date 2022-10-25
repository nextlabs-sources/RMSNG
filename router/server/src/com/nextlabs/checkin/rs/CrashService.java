package com.nextlabs.checkin.rs;

import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.util.AuthUtils;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.router.hibernate.DbSession;
import com.nextlabs.router.hibernate.model.Client;
import com.nextlabs.router.hibernate.model.Crash;
import com.nextlabs.router.hibernate.model.CrashLog;
import com.nextlabs.router.servlet.LogConstants;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Date;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

@Path("/crash")
public class CrashService {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.ROUTER_SERVER_LOG_NAME);

    public CrashService() {
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void upload(String message) {
        JsonRequest req = JsonRequest.fromJson(message);
        if (req == null || req.getClient() == null || req.getCrash() == null) {
            throw new BadRequestException("Missing parameters.");
        }
        com.nextlabs.common.shared.Client clientInfo = req.getClient();
        com.nextlabs.common.shared.Crash crashInfo = req.getCrash();

        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            Criteria criteria = session.createCriteria(Client.class);
            criteria.add(Restrictions.eq("clientId", clientInfo.getClientId()));
            Client client = (Client)criteria.uniqueResult();
            Date now = new Date();
            if (client == null) {
                client = new Client();
                client.setDeviceId(clientInfo.getDeviceId());
                client.setDeviceType(clientInfo.getDeviceType().ordinal());
                client.setModel(clientInfo.getModel());
                client.setOsVersion(clientInfo.getOsVersion());
                client.setManufacturer(clientInfo.getManufacturer());
                client.setCreationDate(now);
            }
            client.setPushToken(clientInfo.getPushToken());
            client.setLastModified(now);
            session.saveOrUpdate(client);

            String stacktrace = crashInfo.getStacktrace();
            String logData = crashInfo.getLog();
            String hash;
            if (StringUtils.hasText(stacktrace)) {
                hash = hash(stacktrace);

                Crash crash = (Crash)session.get(Crash.class, hash);
                if (crash == null) {
                    crash = new Crash();
                    crash.setHash(hash);
                    if (stacktrace.length() > 9000) {
                        LOGGER.warn("Stracktrace too long: {}", stacktrace);
                        stacktrace = stacktrace.substring(0, 8999);
                    }
                    crash.setStacktrace(stacktrace);
                    crash.setClientId(clientInfo.getClientId());
                    session.save(crash);
                    session.flush();
                    session.refresh(crash);
                }
            } else {
                hash = hash(logData);
            }

            if (!logData.isEmpty()) {
                CrashLog log = new CrashLog();
                log.setHash(hash);
                if (logData.length() > 9000) {
                    LOGGER.warn("Log too long: {}", logData);
                    logData = logData.substring(0, 8999);
                }
                log.setLog(logData);
                log.setCreationDate(now);
                session.save(log);
            }

            session.commit();
        } catch (Throwable e) {
            throw new ServerErrorException("Internal Server Error", 500, e);
        } finally {
            session.close();
        }
    }

    private String hash(String value) throws NoSuchAlgorithmException, NoSuchProviderException {
        byte[] hash = AuthUtils.md5(StringUtils.toBytesQuietly(value));
        return Hex.toHexString(hash);
    }
}
