package com.nextlabs.rms.abac;

import com.nextlabs.common.shared.JsonABACMembershipObligation;
import com.nextlabs.common.util.GsonUtils;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.Constants;
import com.nextlabs.rms.cache.TokenGroupCacheManager;
import com.nextlabs.rms.cc.pojos.ControlCenterDelegationObligation;
import com.nextlabs.rms.cc.pojos.ControlCenterObligation;
import com.nextlabs.rms.cc.pojos.ControlCenterObligationParameters;
import com.nextlabs.rms.eval.Attribute;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.EvaluationAdapterFactory;
import com.nextlabs.rms.eval.Obligation;
import com.nextlabs.rms.eval.User;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.KeyStoreEntry;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.ProjectTag;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.security.KeyStoreManagerImpl;
import com.nextlabs.rms.service.SystemBucketManagerImpl;
import com.nextlabs.rms.util.MembershipUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

public final class MembershipPoliciesEvaluationHandler {

    private static final String ACCESSIBLE_PROJECTS = "Accessible Projects";
    private static final String ACCESSIBLE_PROJECTS_SHORTNAME = "projects";

    private MembershipPoliciesEvaluationHandler() {

    }

    public static boolean isDynamicallyAccessible(DbSession session, UserSession us, String tokenGroupName) {
        Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
        if (new SystemBucketManagerImpl().isSystemBucket(tokenGroupName, loginTenant.getName())) {
            return true;
        }
        KeyStoreEntry keyStoreEntry = new KeyStoreManagerImpl().getKeyStore(tokenGroupName);
        if (keyStoreEntry != null) {
            Criteria criteria = session.createCriteria(Project.class);
            criteria.add(Restrictions.eq("keystore.id", keyStoreEntry.getId()));
            criteria.add(Restrictions.eq("status", Project.Status.ACTIVE));
            @SuppressWarnings("unchecked")
            List<Project> projects = criteria.list();
            return (projects != null && !projects.isEmpty() && MembershipPoliciesEvaluationHandler.isProjectAccessible(session, us, projects.get(0).getId()));
        }
        return false;
    }

    public static boolean isProjectAccessible(DbSession session, UserSession us, Integer projectId) {
        List<?> projectIds = getIdList(session, us, "id");
        return projectIds != null && projectIds.contains(projectId);
    }

    private static List<?> getIdList(DbSession session, UserSession us, String propertyName) {
        User userEval = new User.Builder().id(String.valueOf(us.getUser().getId())).clientId(us.getClientId()).email(us.getUser().getEmail()).build();
        Tenant loginTenant = session.get(Tenant.class, us.getLoginTenant());
        Set<Integer> abacProjects = MembershipPoliciesEvaluationHandler.evalABACMembershipPolicies(userEval, loginTenant.getName());
        if (abacProjects.isEmpty()) {
            return null;
        }
        Criteria criteria = session.createCriteria(Project.class);
        criteria.add(Restrictions.in("id", abacProjects.toArray()));
        criteria.setProjection(Projections.property(propertyName));
        return criteria.list();
    }

    public static Set<Integer> evalABACMembershipPolicies(User user, String tenantName) {
        Set<Integer> projectIds = new HashSet<>();
        if (!EvaluationAdapterFactory.isInitialized()) {
            return projectIds;
        }
        String tokenGroup = MembershipUtil.getTokenGroup(tenantName);
        String resourceType = TokenGroupCacheManager.getInstance().getResourceType(tokenGroup + Constants.MEMBERSHIP_MODEL_SUFFIX);
        EvalResponse evalResponse = CentralPoliciesEvaluationHandler.evaluateMembershipPolicy(user, tenantName, resourceType);
        List<Rights> rights = Arrays.asList(evalResponse.getRights());
        if (rights.contains(Rights.ACCESS_PROJECT)) {
            List<Obligation> obligations = evalResponse.getObligations();
            Set<Integer> abacProjects = getAccessibleProjectIds(obligations);
            if (!abacProjects.isEmpty()) {
                projectIds.addAll(abacProjects);
            }
        }
        return projectIds;
    }

    public static Set<Integer> getAccessibleProjectIds(List<Obligation> obligations) {
        Set<Integer> projectIds = new HashSet<>();
        try (DbSession session = DbSession.newSession()) {
            for (Obligation obligation : obligations) {
                Set<Integer> pIds = getAccessibleProjects(obligation, session);
                if (pIds != null) {
                    projectIds.addAll(pIds);
                }
            }
        }
        return projectIds;
    }

    @SuppressWarnings("unchecked")
    private static Set<Integer> getAccessibleProjects(Obligation obligation, DbSession session) {
        if (!obligation.getName().equals(ACCESSIBLE_PROJECTS_SHORTNAME)) {
            return null;
        }
        Set<Integer> projectIds = new HashSet<>();
        for (Attribute attr : obligation.getAttributes()) {
            if (attr.getName().equals(ACCESSIBLE_PROJECTS_SHORTNAME)) {
                JsonABACMembershipObligation jsonAbac = GsonUtils.GSON.fromJson(attr.getValue(), JsonABACMembershipObligation.class);
                if (jsonAbac.getTagIds() != null && jsonAbac.getTagIds().length > 0) {
                    Criteria criteria = session.createCriteria(ProjectTag.class);
                    Integer[] tagArray = ArrayUtils.toObject(jsonAbac.getTagIds());
                    criteria.add(Restrictions.in("tag.id", tagArray));
                    criteria.setProjection(Projections.property("project.id"));
                    List<Integer> pIds = criteria.list();
                    projectIds.addAll(pIds);
                }
                if (jsonAbac.getProjectIds() != null && jsonAbac.getProjectIds().length > 0) {
                    Integer[] pIds = ArrayUtils.toObject(jsonAbac.getProjectIds());
                    projectIds.addAll(new HashSet<>(Arrays.asList(pIds)));
                }
            }
        }
        return projectIds;
    }

    public static ControlCenterObligation getABACPolicyModelObligation() {
        ControlCenterObligation obligation = new ControlCenterObligation();
        obligation.setName(ACCESSIBLE_PROJECTS);
        obligation.setShortName(ACCESSIBLE_PROJECTS_SHORTNAME);
        obligation.setRunAt("PEP");
        ControlCenterObligationParameters parameter = new ControlCenterObligationParameters();
        parameter.setName(ACCESSIBLE_PROJECTS);
        parameter.setType(ControlCenterObligationParameters.ControlCenterObligationParamType.SINGLE_ROW.getTypeValue());
        parameter.setHidden(false);
        parameter.setEditable(true);
        parameter.setMandatory(false);
        parameter.setSortOrder(0);
        parameter.setShortName(ACCESSIBLE_PROJECTS_SHORTNAME);
        obligation.setParameters(new ControlCenterObligationParameters[] { parameter });
        return obligation;
    }

    public static ControlCenterDelegationObligation getABACPolicyObligation(long policyModelId,
        JsonABACMembershipObligation jsonObligation) {
        ControlCenterDelegationObligation policyObligation = new ControlCenterDelegationObligation();
        policyObligation.setPolicyModelId(policyModelId);
        policyObligation.setName(ACCESSIBLE_PROJECTS_SHORTNAME);
        Map<String, String> params = new HashMap<>();
        params.put(ACCESSIBLE_PROJECTS_SHORTNAME, GsonUtils.GSON.toJson(jsonObligation));
        policyObligation.setParams(params);
        return policyObligation;
    }

    public static JsonABACMembershipObligation getJsonABACMembershipObligation(
        ControlCenterDelegationObligation controlCenterDelegationObligation) {
        return GsonUtils.GSON.fromJson(controlCenterDelegationObligation.getParams().get(ACCESSIBLE_PROJECTS_SHORTNAME), JsonABACMembershipObligation.class);
    }
}
