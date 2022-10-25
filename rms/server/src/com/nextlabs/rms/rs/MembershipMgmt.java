package com.nextlabs.rms.rs;

import com.google.gson.JsonParseException;
import com.nextlabs.common.BuildConfig;
import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.security.KeyUtils;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.CertificateAuthority;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.exception.TokenGroupNotFoundException;
import com.nextlabs.rms.abac.MembershipPoliciesEvaluationHandler;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.KeyStoreEntry;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.security.KeyStoreManagerImpl;
import com.nextlabs.rms.service.SystemBucketManagerImpl;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

@Path("/membership")
public class MembershipMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String register(@Context HttpServletRequest request, String json) {
        boolean error = true;
        int userId = -1;
        String name = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }

            userId = req.getIntParameter("userId", -1);
            String ticket = req.getParameter("ticket");
            name = req.getParameter("membership");
            String pubKey = req.getParameter("publicKey");
            if (userId < 0 || !StringUtils.hasText(ticket)) {
                return new JsonResponse(401, "Missing login parameters").toJson();
            }
            if (!StringUtils.hasText(name) || !StringUtils.hasText(pubKey)) {
                return new JsonResponse(400, "Missing required parameter").toJson();
            }
            try (DbSession session = DbSession.newSession()) {
                UserSession us = UserMgmt.authenticate(session, userId, ticket, null, null);
                if (us == null) {
                    return new JsonResponse(401, "Authentication failed.").toJson();
                }

                Membership membership = session.get(Membership.class, name);
                if (membership == null) {
                    String tokenGroupName = StringUtils.substringAfter(name, "@");
                    KeyStoreEntry keyStoreEntry = new KeyStoreManagerImpl().getKeyStore(tokenGroupName);
                    if (keyStoreEntry == null) {
                        return new JsonResponse(403, "Access denied").toJson();
                    } else {
                        Tenant tenant = session.get(Tenant.class, us.getLoginTenant());
                        Criteria criteria = session.createCriteria(Project.class);
                        criteria.add(Restrictions.eq("keystore", keyStoreEntry));
                        Project project = (Project)criteria.uniqueResult();
                        if ((project == null && !new SystemBucketManagerImpl().isSystemBucket(tokenGroupName, tenant.getName())) || (project != null && !MembershipPoliciesEvaluationHandler.isProjectAccessible(session, us, project.getId()))) {
                            return new JsonResponse(403, "Access denied").toJson();
                        }
                    }
                } else if (membership.getUser().getId() != userId) {
                    return new JsonResponse(403, "Access denied").toJson();
                }
            }
            byte[] buf = Base64Codec.decode(pubKey);
            Certificate[] chain = getCertificateChain(name, buf);
            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("certficates", KeyUtils.toPEM(chain));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Membership: {} certificate issued.", name);
            }
            error = false;
            return resp.toJson();
        } catch (JsonParseException | InvalidKeySpecException e) {
            if (BuildConfig.DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error occurred: {}", json, e);
            }
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (TokenGroupNotFoundException e) {
            return new JsonResponse(404, "Tenant is not supported on this server.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            Audit.audit(request, "API", "MembershipMgmt", "register", error ? 0 : 1, userId, name);
        }
    }

    protected void dummy() {

    }

    public static Certificate[] getCertificateChain(String membership, byte[] pubKey)
            throws GeneralSecurityException, IOException, TokenGroupNotFoundException {
        String tokenGroupName = StringUtils.substringAfter(membership, "@");
        KeyManager km = new KeyManager(new KeyStoreManagerImpl());
        String icaAlias = IKeyStoreManager.PREFIX_ICA + tokenGroupName;
        PrivateKey icaKey = (PrivateKey)km.getKey(tokenGroupName, icaAlias);
        X509Certificate icaCert = (X509Certificate)km.getCertificate(tokenGroupName, icaAlias);
        Certificate[] icaDh = km.getCertificateChain(tokenGroupName, IKeyStoreManager.PREFIX_DH + tokenGroupName);
        if (icaKey == null) {
            throw new TokenGroupNotFoundException(tokenGroupName);
        }

        Certificate[] memberChain = new Certificate[icaDh.length + 1];

        PublicKey memberPubKey = KeyUtils.readPublicKey(pubKey, "DH");
        memberChain[0] = CertificateAuthority.sign(icaKey, icaCert, membership, memberPubKey);
        System.arraycopy(icaDh, 0, memberChain, 1, icaDh.length);
        return memberChain;
    }
}
