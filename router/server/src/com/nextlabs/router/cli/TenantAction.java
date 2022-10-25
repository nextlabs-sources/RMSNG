package com.nextlabs.router.cli;

import com.nextlabs.common.cli.CLI;
import com.nextlabs.common.cli.CommandParser;
import com.nextlabs.common.cli.Parameter;
import com.nextlabs.common.security.IKeyStoreManager;
import com.nextlabs.common.util.Hex;
import com.nextlabs.common.util.KeyManager;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.router.Config;
import com.nextlabs.router.hibernate.DbSession;
import com.nextlabs.router.hibernate.model.Tenant;
import com.nextlabs.router.servlet.LogConstants;

import java.util.Date;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class TenantAction implements Action {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.ROUTER_SERVER_LOG_NAME);

    @Override
    public void execute(Config config, String[] args) {
        CommandParser<TenantConfig> p = new CommandParser<TenantConfig>(TenantConfig.class);
        try {
            TenantConfig tc = p.parse(args);
            String operation = tc.getOperation();
            if ("add".equals(operation)) {
                if (!StringUtils.hasText(tc.getServer())) {
                    LOGGER.error("server field is required.");
                    p.printHelp("java com.nextlabs.router.Main --cmd=Tenant");
                    return;
                }
                addTenant(tc);
            } else if ("reset-otp".equals(operation)) {
                resetOtp(tc.getName());
            } else {
                p.printHelp("java com.nextlabs.router.Main --cmd=Tenant");
            }
        } catch (ParseException e) {
            p.printHelp("java com.nextlabs.router.Main --cmd=Tenant");
        }
    }

    private void addTenant(TenantConfig config) {
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            String name = config.getName();
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", name));
            Tenant tenant = (Tenant)criteria.uniqueResult();
            if (tenant != null) {
                LOGGER.error("Tenant: {} already exists.", name);
                return;
            }
            tenant = new Tenant();
            tenant.setName(name);
            tenant.setServer(config.getServer());
            tenant.setOtp(KeyManager.randomBytes(16));
            tenant.setHsk(KeyManager.randomBytes(IKeyStoreManager.KEY_BYTES));
            tenant.setDisplayName(config.getDisplayName());
            tenant.setDescription(config.getDescription());
            tenant.setEmail(config.getEmail());
            tenant.setCreationTime(new Date());
            session.save(tenant);
            session.commit();
            LOGGER.info("Tenant: {} created, OTP: {}", name, Hex.toHexString(tenant.getOtp()));
        } finally {
            session.close();
        }
    }

    private void resetOtp(String name) {
        DbSession session = DbSession.newSession();
        try {
            session.beginTransaction();
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", name));
            Tenant tenant = (Tenant)criteria.uniqueResult();
            if (tenant == null) {
                LOGGER.error("Tenant: {} does not exist.", name);
                return;
            }
            tenant.setOtp(KeyManager.randomBytes(16));
            session.commit();
            LOGGER.info("Tenant: {}, new OTP is: {}", name, Hex.toHexString(tenant.getOtp()));
        } finally {
            session.close();
        }
    }

    @CLI
    public static final class TenantConfig {

        @Parameter(defaultValue = "", option = "o", description = "Operation: add/reset-otp", hasArgs = true, mandatory = true)
        private String operation;

        @Parameter(defaultValue = "", option = "n", description = "Tenant name", hasArgs = true, mandatory = true)
        private String name;

        @Parameter(defaultValue = "", option = "s", description = "Server name", hasArgs = true, mandatory = false)
        private String server;

        @Parameter(defaultValue = "", option = "d", description = "Tenant display name", hasArgs = true, mandatory = false)
        private String displayName;

        @Parameter(defaultValue = "", option = "desc", description = "Tenant description", hasArgs = true, mandatory = false)
        private String description;

        @Parameter(defaultValue = "", option = "e", description = "Contact email address", hasArgs = true, mandatory = false)
        private String email;

        public String getOperation() {
            return operation;
        }

        public String getName() {
            return name;
        }

        public String getServer() {
            return server;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public String getEmail() {
            return email;
        }
    }
}
