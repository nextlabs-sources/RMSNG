package com.nextlabs.router.rs;

import com.nextlabs.common.Environment;
import com.nextlabs.common.io.IOUtils;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.router.hibernate.DbSession;
import com.nextlabs.router.hibernate.model.Tenant;
import com.nextlabs.router.servlet.LogConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

@Path("/q")
public class Lookup {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.ROUTER_SERVER_LOG_NAME);

    @GET
    @Path("ping")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPing() {
        return new JsonResponse(200, "OK").toJson();
    }

    @GET
    @Path("tokenGroupName/{token_group_name: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTenant(@PathParam("token_group_name") String name) {
        DbSession session = DbSession.newSession();
        try {
            if (!StringUtils.hasText(name)) {
                name = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT);
                if (!StringUtils.hasText(name)) {
                    name = getDefaultTenantFromConfigFile();
                    if (!StringUtils.hasText(name)) {
                        return new JsonResponse(5001, "Get public tenant failed.").toJson();
                    }
                    WebConfig.getInstance().setProperty(WebConfig.PUBLIC_TENANT, name);
                }
                return getServerUrlResponse(session, name);
            }
            return getServerUrlResponse(session, URLDecoder.decode(name, StandardCharsets.UTF_8.name()));
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            session.close();
        }
    }

    @GET
    @Path("defaultTenant")
    @Produces(MediaType.APPLICATION_JSON)
    public String defaultTenant() {
        DbSession session = DbSession.newSession();
        try {
            String name = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT);
            if (!StringUtils.hasText(name)) {
                String defaultTenant = getDefaultTenantFromConfigFile();
                if (!StringUtils.hasText(defaultTenant)) {
                    return new JsonResponse(5001, "Get public tenant failed.").toJson();
                }
                WebConfig.getInstance().setProperty(WebConfig.PUBLIC_TENANT, defaultTenant);
                return getServerUrlResponse(session, defaultTenant);
            }
            return getServerUrlResponse(session, name);
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error").toJson();
        } finally {
            session.close();
        }
    }

    private String getServerUrlResponse(DbSession session, String name) {
        if (name.endsWith("_system")) {
            name = name.replace("_system", "");
        }
        Criteria criteria = session.createCriteria(Tenant.class);
        criteria.add(Restrictions.eq("name", name));
        Tenant tenant = (Tenant)criteria.uniqueResult();
        if (tenant == null) {
            int lastIdxOfUnderscore = name.lastIndexOf('_');
            if (lastIdxOfUnderscore != -1) {
                name = name.substring(0, lastIdxOfUnderscore);
                lastIdxOfUnderscore = name.lastIndexOf('_');
                if (lastIdxOfUnderscore != -1) {
                    name = name.substring(0, lastIdxOfUnderscore);
                    criteria = session.createCriteria(Tenant.class);
                    criteria.add(Restrictions.eq("name", name));
                    tenant = (Tenant)criteria.uniqueResult();
                }
            }
            if (tenant == null) {
                return new JsonResponse(400, "Invalid token group name").toJson();
            }
        }
        JsonResponse resp = new JsonResponse("OK");
        resp.putResult("server", tenant.getServer());
        return resp.toJson();
    }

    private String getDefaultTenantFromConfigFile() {
        String defaultTenantName = null;
        File baseDir = Environment.getInstance().getSharedConfDir();
        Properties prop = new Properties();
        File file = new File(baseDir, "router.properties");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            prop.load(fis);
            defaultTenantName = prop.getProperty("web." + WebConfig.PUBLIC_TENANT);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            defaultTenantName = null;
        } finally {
            IOUtils.closeQuietly(fis);
        }
        return defaultTenantName;
    }
}
