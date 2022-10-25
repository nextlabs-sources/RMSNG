package com.nextlabs.rms.migration;

import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.rms.Constants;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicy;
import com.nextlabs.rms.cc.service.ControlCenterPolicyService;
import com.nextlabs.rms.cc.service.ControlCenterRestClient;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.shared.LogConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/***
 * This migration sets SkipAddingTrueAllowAttribute=true to all existing domain, abac and project policies.
 */
public class V195SkipAddingTrueAllowAttributeMigration implements Migration {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void migrate() {
        // migrate old policies with skipAddingTrueAllowAttribute value

        String defaultTenant = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
        LOGGER.info("Migrating policies with skipAddingTrueAllowAttribute value");

        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", defaultTenant));
            Tenant tenant = (Tenant)criteria.uniqueResult();

            if (tenant == null) {
                return;
            }

            ControlCenterRestClient rsClient = new ControlCenterRestClient(tenant.getName());
            ControlCenterPolicyService policyService = new ControlCenterPolicyService(rsClient);

            ControlCenterPolicy[] domainPolicies = policyService.list(null, tenant.getName(), null, null, null);
            ControlCenterPolicy[] membershipPolicies = policyService.list(null, tenant.getName() + Constants.MEMBERSHIP_MODEL_SUFFIX, null, null, null);
            List<ControlCenterPolicy> projectPolicies = new ArrayList<>();

            Criteria projectCriteria = session.createCriteria(Project.class);
            List<Project> projects = projectCriteria.list();
            for (Project project : projects) {
                String tokenGroupName = project.getKeystore().getTokenGroupName();
                projectPolicies.addAll(Arrays.asList(policyService.list(null, tokenGroupName, null, null, null)));
            }

            List<ControlCenterPolicy> allPolicies = Stream.of(domainPolicies, membershipPolicies).flatMap(Stream::of).collect(Collectors.toList());
            allPolicies.addAll(projectPolicies);
            for (ControlCenterPolicy p : allPolicies) {
                ControlCenterPolicy policy = policyService.get(p.getId());
                if (policy.isSkipAddingTrueAllowAttribute()) {
                    continue;
                }
                policy.setSkipAddingTrueAllowAttribute(true);
                LOGGER.debug("Migrating policy: {}", policy.getName());
                policyService.modify(policy);
            }
        } catch (ControlCenterRestClient.ControlCenterServiceException
                | ControlCenterRestClient.ControlCenterRestClientException e) {
            LOGGER.error("Error in skipAddingTrueAllowAttribute migration", e);
        }
    }
}
