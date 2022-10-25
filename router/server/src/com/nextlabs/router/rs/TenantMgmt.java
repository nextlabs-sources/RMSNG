package com.nextlabs.router.rs;

import com.google.gson.JsonParseException;
import com.nextlabs.common.Environment;
import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.security.KeyStoreManager;
import com.nextlabs.common.security.KeyUtils;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.CertificateAuthority;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.router.hibernate.DbSession;
import com.nextlabs.router.hibernate.model.Tenant;
import com.nextlabs.router.security.KeyStoreManagerImpl;
import com.nextlabs.router.servlet.LogConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

@Path("/tenant")
public class TenantMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.ROUTER_SERVER_LOG_NAME);

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String addTenant(@Context HttpServletRequest request, @HeaderParam("cert") String cert, String json) {
        String name = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing parameters.").toJson();
            }
            name = req.getParameter("name");
            boolean defaultTenant = Boolean.valueOf(req.getParameter("defaultTenant"));
            if (defaultTenant && StringUtils.hasText(cert)) {

                KeyStoreManager ksm = new KeyStoreManager(new File(Environment.getInstance().getDataDir() + File.separator + KeyStoreManager.RMS_KEYSTORE_FILE_SECURE), KeyStoreManager.RMS_KEYSTORE_FILE_PASSWORD.toCharArray(), KeyStoreManager.KeyStoreType.BCFKS.name());
                Certificate routerCertificate = ksm.getCertificate(KeyStoreManager.RMS_KEYSTORE_SECURE_ALIAS);
                byte[] buf = Base64Codec.decode(cert);
                Certificate icaCertificate = KeyUtils.readCertificate(buf);
                routerCertificate.verify(icaCertificate.getPublicKey());

            } else {

                String signature = req.getParameter("signature");
                if (!StringUtils.hasText(name) || !StringUtils.hasText(signature)) {
                    return new JsonResponse(400, "Missing required parameter.").toJson();
                } else if (name.length() > 250) {
                    return new JsonResponse(4001, "Tenant name is too long.").toJson();
                }
                KeyManager km = new KeyManager(new KeyStoreManagerImpl());
                X509Certificate rootCA = (X509Certificate)km.getCertificate(null, IKeyStoreManager.ROOT_RSA);
                byte[] buf = Base64Codec.decode(req.getParameter("certificate"));
                Certificate pubCert = KeyUtils.readCertificate(buf);
                if (!KeyManager.verify(rootCA, pubCert, signature, name)) {
                    LOGGER.error(" Verify signature for tenant '{}' failed", name);
                    return new JsonResponse(403, "Access denied").toJson();
                }
            }

            try (DbSession session = DbSession.newSession()) {
                Criteria criteria = session.createCriteria(Tenant.class);
                criteria.add(Restrictions.eq("name", name));
                Tenant tenant = (Tenant)criteria.uniqueResult();
                if (tenant != null) {
                    LOGGER.error("Tenant: {} already exists.", name);
                    return null;
                }
                session.beginTransaction();
                tenant = new Tenant();
                tenant.setName(name);
                if (StringUtils.hasText(req.getParameter("server"))) {
                    tenant.setServer(req.getParameter("server"));
                } else {
                    tenant.setServer(WebConfig.getInstance().getProperty("rms_url"));
                }
                if (defaultTenant && !StringUtils.hasText(WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT))) {
                    try (Writer fw = new OutputStreamWriter(new FileOutputStream(new File(Environment.getInstance().getSharedConfDir(), "router.properties"), true), "UTF-8")) {
                        fw.write(System.lineSeparator());
                        fw.write("web." + WebConfig.PUBLIC_TENANT + "=" + name);
                    }
                    WebConfig.getInstance().setProperty(WebConfig.PUBLIC_TENANT, name);
                }
                tenant.setOtp(KeyManager.randomBytes(16));
                tenant.setHsk(KeyManager.randomBytes(IKeyStoreManager.KEY_BYTES));
                tenant.setDisplayName(req.getParameter("displayName"));
                tenant.setDescription(req.getParameter("description"));
                tenant.setEmail(req.getParameter("email"));
                tenant.setCreationTime(new Date());
                session.save(tenant);
                session.commit();
                LOGGER.info("Tenant: {} created, OTP: {}", name, Hex.toHexString(tenant.getOtp()));
                JsonResponse resp = new JsonResponse("OK");
                resp.putResult("otp", Hex.toHexString(tenant.getOtp()));
                resp.putResult("server", tenant.getServer());
                return resp.toJson();
            }
        } catch (JsonParseException e) {
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error("Error occurred during tenant creation (name: {}): {}", name, e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        }
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteTenant(@Context HttpServletRequest request, String json) {
        JsonRequest req = JsonRequest.fromJson(json);
        try {
            KeyManager km = new KeyManager(new KeyStoreManagerImpl());
            X509Certificate rootCA = (X509Certificate)km.getCertificate(null, IKeyStoreManager.ROOT_RSA);
            byte[] buf = Base64Codec.decode(req.getParameter("certificate"));
            Certificate icaCertificate = KeyUtils.readCertificate(buf);
            icaCertificate.verify(rootCA.getPublicKey());
            boolean verified = KeyManager.verify(rootCA, icaCertificate, req.getParameter("signature"), "");
            if (!verified) {
                LOGGER.error(" Unable to verify certificate");
                return new JsonResponse(403, "Access denied").toJson();
            }
        } catch (GeneralSecurityException e) {
            LOGGER.error("Unable to verify certificate", e);
            return new JsonResponse(403, "Access denied").toJson();
        } catch (IOException e) {
            LOGGER.error("Unable to verify certificate", e);
            return new JsonResponse(403, "Access denied").toJson();
        }
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            String name = req.getParameter("name");
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", name));
            Tenant tenant = (Tenant)criteria.uniqueResult();
            if (tenant == null) {
                LOGGER.error("Tenant: {} does not exist.", name);
                return new JsonResponse(404, "Tenant not found").toJson();
            }
            session.delete(tenant);
            session.commit();
            LOGGER.info("Tenant: {} deleted.", name);
            JsonResponse resp = new JsonResponse(204, "OK");
            return resp.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String handleCsr(String json) {
        DbSession session = DbSession.newSession();
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            String name = req.getParameter("name");
            String passwd = req.getParameter("otp");
            String icaKey = req.getParameter("icaPublicKey");
            String dhKey = req.getParameter("dhPublicKey");
            if (!StringUtils.hasText(name) || !StringUtils.hasText(passwd) || !StringUtils.hasText(icaKey) || !StringUtils.hasText(dhKey)) {
                return new JsonResponse(400, "Missing required parameter").toJson();
            }

            session.beginTransaction();
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", name));
            Tenant tenant = (Tenant)criteria.uniqueResult();
            if (tenant == null) {
                return new JsonResponse(403, "Invalid tenant").toJson();
            }
            byte[] otp = tenant.getOtp();
            if (otp == null) {
                return new JsonResponse(403, "otp expired").toJson();
            }

            if (!Arrays.equals(otp, Hex.toByteArray(passwd))) {
                return new JsonResponse(403, "Invalid otp").toJson();
            }

            KeyManager km = new KeyManager(new KeyStoreManagerImpl());

            PrivateKey caKey = (PrivateKey)km.getKey(null, IKeyStoreManager.ROOT_RSA);
            X509Certificate rootCA = (X509Certificate)km.getCertificate(null, IKeyStoreManager.ROOT_RSA);
            X509Certificate rootDh = (X509Certificate)km.getCertificate(null, IKeyStoreManager.ROOT_DH);

            byte[] buf = Base64Codec.decode(icaKey);
            PublicKey icaPubKey = KeyUtils.readPublicKey(buf, "RSA");
            Certificate icaCert = CertificateAuthority.sign(caKey, rootCA, name, icaPubKey);
            String icaChain = KeyUtils.toPEM(icaCert, rootCA);

            buf = Base64Codec.decode(dhKey);
            PublicKey dhPubKey = KeyUtils.readPublicKey(buf, "DH");
            Certificate dhCert = CertificateAuthority.sign(caKey, rootCA, name, dhPubKey);
            String dhChain = KeyUtils.toPEM(dhCert, rootDh);

            JsonResponse resp = new JsonResponse("OK");
            resp.putResult("icaCertificates", icaChain);
            resp.putResult("dhCertificates", dhChain);
            resp.putResult("hsk", Hex.toHexString(tenant.getHsk()));

            tenant.setOtp(null);
            session.commit();

            return resp.toJson();
        } catch (JsonParseException e) {
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            session.close();
        }
    }
}
