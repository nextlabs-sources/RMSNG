package com.nextlabs.rms.util;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.Operations;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.builder.OrderBuilder;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.NxlMetadata;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.rs.RemoteLoggingMgmt;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

public class DownloadUtil {

    public boolean checkUserFileRights(Constants.DownloadType downloadType, Rights[] rightsList) {

        switch (downloadType) {
            case OFFLINE:
                return ArrayUtils.contains(rightsList, Rights.DOWNLOAD) || ArrayUtils.contains(rightsList, Rights.VIEW) || ArrayUtils.contains(rightsList, Rights.EDIT);
            case FOR_VIEWER:
                return ArrayUtils.contains(rightsList, Rights.VIEW);
            case NORMAL:
            case FOR_SYSTEMBUCKET:
                return ArrayUtils.contains(rightsList, Rights.DOWNLOAD);
            default:
                return false;
        }
    }

    public String getUserId(String owner) {
        User user;
        String userId;
        if (owner == null) {
            return null;
        }
        try (DbSession session = DbSession.newSession()) {
            Membership ownerMembership = session.get(Membership.class, owner);
            if (ownerMembership == null) {
                user = new User();
                user.setId(Integer.parseInt(owner.substring("user".length(), owner.indexOf('@'))));
                userId = Integer.toString(user.getId());
            } else {
                Criteria criteria = session.createCriteria(Membership.class, "m");
                criteria.add(Restrictions.eq("name", owner));
                Membership m = (Membership)criteria.uniqueResult();
                int createdByUserId = m.getUser().getId();
                userId = Integer.toString(createdByUserId);
            }
        }
        return userId;
    }

    public Operations getDownloadOps(Constants.DownloadType downloadType, boolean isPartialDownload) {
        Operations ops;
        if (isPartialDownload) {
            if (downloadType == Constants.DownloadType.OFFLINE) {
                ops = Operations.OFFLINE_PARTIAL_DOWNLOAD;
            } else if (downloadType == Constants.DownloadType.FOR_VIEWER) {
                ops = Operations.VIEW_PARTIAL_DOWNLOAD;
            } else if (downloadType == Constants.DownloadType.FOR_SYSTEMBUCKET) {
                ops = Operations.SYSTEMBUCKET_PARTIAL_DOWNLOAD;
            } else {
                ops = Operations.DOWNLOAD_PARTIAL_DOWNLOAD;
            }
        } else {
            if (downloadType == Constants.DownloadType.OFFLINE) {
                ops = Operations.OFFLINE;
            } else if (downloadType == Constants.DownloadType.FOR_VIEWER) {
                ops = Operations.VIEW;
            } else if (downloadType == Constants.DownloadType.FOR_SYSTEMBUCKET) {
                ops = Operations.SYSTEMBUCKET_DOWNLOAD;
            } else {
                ops = Operations.DOWNLOAD;
            }
        }

        return ops;
    }

    public void saveDownloadActivityLog(Operations ops,
        RemoteLoggingMgmt.Activity activity) {
        if (ops != null) {
            activity.setOperation(ops);
            RemoteLoggingMgmt.saveActivityLog(activity);
        }
    }

    public Tenant getParentTenant(String id) {
        Tenant tenant;
        try (DbSession session = DbSession.newSession()) {
            tenant = session.get(Tenant.class, id);
        }
        return tenant;
    }

    public EvalResponse getAdhocEvaluationResponse(NxlFile nxlMetadata)
            throws GeneralSecurityException, IOException, NxlException {
        EvalResponse evalResponse = new EvalResponse();
        FilePolicy policy = DecryptUtil.getFilePolicy(nxlMetadata, null);
        List<FilePolicy.Policy> adhocPolicies = null;
        if (policy != null) {
            adhocPolicies = policy.getPolicies();
        }
        if (adhocPolicies != null && !adhocPolicies.isEmpty()) {
            evalResponse = AdhocEvalAdapter.evaluate(policy, true);
        }
        return evalResponse;
    }

    public JsonExpiry getValidity(FilePolicy policy) {
        JsonExpiry validity = AdhocEvalAdapter.getFirstPolicyExpiry(policy);
        if (validity.getStartDate() == null && validity.getEndDate() == null) {
            validity.setOption(0);
        } else if (validity.getStartDate() == null && validity.getEndDate() != null) {
            validity.setOption(2);
        } else if (validity.getStartDate() != null && validity.getEndDate() != null) {
            validity.setOption(3);
        }
        return validity;
    }

    public String getFileNameWithoutNXL(String fileName) {
        if (fileName == null || fileName.length() == 0) {
            return fileName;
        }
        if (!fileName.toLowerCase().endsWith(com.nextlabs.nxl.Constants.NXL_FILE_EXTN)) {
            return fileName;
        }
        int index = fileName.toLowerCase().lastIndexOf(com.nextlabs.nxl.Constants.NXL_FILE_EXTN);
        return fileName.substring(0, index);
    }

    public List<Order> getOrdersList(String orderBy) {
        List<Order> orders = Collections.emptyList();
        if (StringUtils.hasText(orderBy)) {
            Map<String, String> supportedFields = new HashMap<>(2);
            supportedFields.put("creationTime", "p.creationTime");
            supportedFields.put("lastModified", "p.lastModified");
            supportedFields.put("name", "p.filePathSearchSpace");
            supportedFields.put("size", "p.size");
            supportedFields.put("folder", "p.directory");
            OrderBuilder builder = new OrderBuilder(supportedFields);
            List<String> list = StringUtils.tokenize(orderBy, ",");
            for (String s : list) {
                builder.add(s);
            }
            orders = builder.build();
        }
        return orders;
    }

    public String getFileOwner(String duid) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(NxlMetadata.class);
            criteria.add(Restrictions.and(Restrictions.eq("duid", duid)));
            NxlMetadata metadata = (NxlMetadata)criteria.uniqueResult();
            return metadata.getOwner();
        }
    }

}
