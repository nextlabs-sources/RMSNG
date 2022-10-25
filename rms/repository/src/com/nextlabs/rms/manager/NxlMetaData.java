package com.nextlabs.rms.manager;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;

import java.util.Map;

public class NxlMetaData {

    private String duid;
    private String ownerMembership;
    private Rights[] rights;
    private Map<String, String[]> tags;
    private Constants.TokenGroupType tgType;
    private boolean isRevoked;
    private boolean allowedToShare;
    private boolean isOwner;
    private boolean isExpired;
    private boolean isNotYetValid;
    private JsonExpiry validity;
    private int protectionType;
    private FilePolicy policy;
    private boolean isTenantMembershipInvalid;

    public NxlMetaData(String duid, String ownerMembership, Constants.TokenGroupType tgType, Map<String, String[]> tags,
        boolean isRevoked,
        boolean allowedToShare, boolean isOwner, JsonExpiry validity) {
        this.duid = duid;
        this.ownerMembership = ownerMembership;
        this.tgType = tgType;
        this.tags = tags;
        this.allowedToShare = allowedToShare;
        this.isRevoked = isRevoked;
        this.isOwner = isOwner;
        this.isExpired = false;
        this.validity = validity;
    }

    public NxlMetaData(String duid, String ownerMembership, Constants.TokenGroupType tgType, boolean isRevoked,
        boolean isOwner, FilePolicy policy, Map<String, String[]> tags) {
        EvalResponse evalResponse = AdhocEvalAdapter.evaluate(policy, isOwner);
        this.rights = evalResponse.getRights();
        if (policy != null) {
            this.validity = AdhocEvalAdapter.getFirstPolicyExpiry(policy);
            this.isExpired = AdhocEvalAdapter.isFileExpired(policy);
            this.isNotYetValid = AdhocEvalAdapter.isNotYetValid(policy);
        }
        this.allowedToShare = (isOwner && !isExpired) || (!isExpired && hasShareRights(rights));
        this.policy = policy;
        this.tags = tags;
        this.duid = duid;
        this.ownerMembership = ownerMembership;
        this.tgType = tgType;
        this.isRevoked = isRevoked;
        this.isOwner = isOwner;
    }

    private boolean hasShareRights(Rights[] rights) {
        if (rights != null) {
            for (Rights r : rights) {
                if (r == Rights.SHARE) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getDuid() {
        return duid;
    }

    public void setDuid(String duid) {
        this.duid = duid;
    }

    public String getOwnerMembership() {
        return ownerMembership;
    }

    public void setOwnerMembership(String ownerMembership) {
        this.ownerMembership = ownerMembership;
    }

    public Rights[] getRights() {
        return rights;
    }

    public void setRights(Rights[] rights) {
        this.rights = rights;
    }

    public Map<String, String[]> getTags() {
        return tags;
    }

    public void setTags(Map<String, String[]> tags) {
        this.tags = tags;
    }

    public Constants.TokenGroupType getTgType() {
        return tgType;
    }

    public void setTgType(Constants.TokenGroupType tgType) {
        this.tgType = tgType;
    }

    public boolean isRevoked() {
        return isRevoked;
    }

    public void setRevoked(boolean isRevoked) {
        this.isRevoked = isRevoked;
    }

    public boolean isAllowedToShare() {
        return allowedToShare;
    }

    public void setAllowedToShare(boolean allowedToShare) {
        this.allowedToShare = allowedToShare;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean isOwner) {
        this.isOwner = isOwner;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public void setExpired(boolean isExpired) {
        this.isExpired = isExpired;
    }

    public JsonExpiry getValidity() {
        return validity;
    }

    public void setValidity(JsonExpiry validity) {
        this.validity = validity;
    }

    public int getProtectionType() {
        return protectionType;
    }

    public void setProtectionType(int protectionType) {
        this.protectionType = protectionType;
    }

    public FilePolicy getPolicy() {
        return policy;
    }

    public void setPolicy(FilePolicy policy) {
        this.policy = policy;
    }

    public boolean isNotYetValid() {
        return isNotYetValid;
    }

    public void setNotYetValid(boolean isNotYetValid) {
        this.isNotYetValid = isNotYetValid;
    }

    public boolean isTenantMembershipInvalid() {
        return isTenantMembershipInvalid;
    }

    public void setTenantMembershipInvalid(boolean isTenantMembershipInvalid) {
        this.isTenantMembershipInvalid = isTenantMembershipInvalid;
    }

}
