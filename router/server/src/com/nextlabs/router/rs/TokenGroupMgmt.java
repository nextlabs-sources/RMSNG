package com.nextlabs.router.rs;

import com.google.gson.JsonParseException;
import com.nextlabs.common.codec.Base64Codec;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.security.KeyUtils;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.util.CertificateAuthority;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.router.security.KeyStoreManagerImpl;
import com.nextlabs.router.servlet.LogConstants;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/tokenGroup")
public class TokenGroupMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.ROUTER_SERVER_LOG_NAME);

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String addTokenGroup(@Context HttpServletRequest request, String json) {
        String name = null;
        try {
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing parameters.").toJson();
            }
            name = req.getParameter("name");
            String signature = req.getParameter("signature");
            String icaKey = req.getParameter("icaPublicKey");
            String dhKey = req.getParameter("dhPublicKey");
            if (!StringUtils.hasText(name) || !StringUtils.hasText(signature) || !StringUtils.hasText(icaKey) || !StringUtils.hasText(dhKey)) {
                return new JsonResponse(400, "Missing required parameter.").toJson();
            } else if (name.length() > 250) {
                return new JsonResponse(4001, "Tenant name is too long.").toJson();
            }
            KeyManager km = new KeyManager(new KeyStoreManagerImpl());
            X509Certificate rootCA = (X509Certificate)km.getCertificate(null, IKeyStoreManager.ROOT_RSA);
            X509Certificate rootDh = (X509Certificate)km.getCertificate(null, IKeyStoreManager.ROOT_DH);
            byte[] buf = Base64Codec.decode(req.getParameter("certificate"));
            Certificate pubCert = KeyUtils.readCertificate(buf);
            if (!KeyManager.verify(rootCA, pubCert, signature, name)) {
                LOGGER.error(" Verify signature for tenant '{}' failed", name);
                return new JsonResponse(403, "Access denied").toJson();
            }

            PrivateKey caKey = (PrivateKey)km.getKey(null, IKeyStoreManager.ROOT_RSA);

            buf = Base64Codec.decode(icaKey);
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
            resp.putResult("hsk", Hex.toHexString(KeyManager.randomBytes(IKeyStoreManager.KEY_BYTES)));

            return resp.toJson();

        } catch (JsonParseException e) {
            return new JsonResponse(400, "Malformed request").toJson();
        } catch (Throwable e) {
            LOGGER.error("Error occurred during token group creation (name: {}): {}", name, e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        }
    }
}
