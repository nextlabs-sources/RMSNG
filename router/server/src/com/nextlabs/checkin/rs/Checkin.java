package com.nextlabs.checkin.rs;

import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.router.hibernate.DbSession;
import com.nextlabs.router.hibernate.model.Client;

import java.util.Date;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

@Path("/")
public class Checkin {

    public Checkin() {
    }

    @POST
    @Path("/heartbeat")
    @Consumes(MediaType.APPLICATION_JSON)
    public void heartbeat(String message) {
        JsonRequest req = JsonRequest.fromJson(message);
        if (req == null || req.getClient() == null) {
            throw new BadRequestException("Missing parameters.");
        }
        com.nextlabs.common.shared.Client clientInfo = req.getClient();

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
            client.setLastModified(now);
            session.saveOrUpdate(client);

            session.commit();
        } catch (Throwable e) {
            throw new ServerErrorException("Internal Server Error", 500, e);
        } finally {
            session.close();
        }
    }
}
