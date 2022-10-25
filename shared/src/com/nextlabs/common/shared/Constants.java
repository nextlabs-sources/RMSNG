package com.nextlabs.common.shared;

import com.google.gson.annotations.SerializedName;

public interface Constants {

    String SYSTEM_BUCKET_NAME_SUFFIX = "_system";
    String ACTIVITY_DATA_ACTIVITY_DETAIL = "activityDetail";

    enum Status {
        ACTIVE,
        PENDING,
        DISABLED
    };

    enum LoginType {
        DB,
        SAML,
        GOOGLE,
        FACEBOOK,
        LDAP,
        TRUSTEDAPP,
        AZUREAD
    };

    enum AccessResult {
        DENY,
        ALLOW
    }

    enum AccountType {
        PERSONAL,
        PROJECT,
        ENTERPRISEWS,
        SHAREDWS,
        LOCALDRIVE
    }

    enum Roles {
        SYSTEM_ADMIN,
        TENANT_ADMIN,
        PROJECT_ADMIN
    }

    enum ProtectionType {
        ADHOC,
        CENTRAL
    }

    public static enum PolicyComponentType {
        USER,
        APPLICATION,
        ADVANCED_CONDITION
    }

    enum DownloadType {
        NORMAL("download"),
        FOR_VIEWER("downloadForView"),
        OFFLINE("downloadForOffline"),
        FOR_SYSTEMBUCKET("downloadForSystemBucket"),
        PARTIAL("partialDownload"),
        HEADER("headerDownload");

        private String displayName;

        DownloadType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

    }

    enum UploadType {
        UPLOAD_NORMAL,
        UPLOAD_OVERWRITE
    }

    enum PolicyModelType {
        RESOURCE,
        MEMBERSHIP;
    }

    enum TokenGroupType {
        TOKENGROUP_TENANT,
        TOKENGROUP_PROJECT,
        TOKENGROUP_SYSTEMBUCKET;
    }

    enum SHARESPACE {
        @SerializedName("0")
        MYSPACE,
        @SerializedName("1")
        PROJECTSPACE,
        @SerializedName("2")
        ENTERPRISESPACE;
    }

    enum SPACETYPE {
        @SerializedName("0")
        MYSPACE,
        @SerializedName("1")
        PROJECTSPACE,
        @SerializedName("2")
        ENTERPRISESPACE,
        @SerializedName("3")
        SHAREDWORKSPACE;
    }

    enum ProjectUploadType {
        @SerializedName("0")
        UPLOAD_NORMAL,
        @SerializedName("1")
        UPLOAD_VIEW,
        @SerializedName("2")
        UPLOAD_EDIT,
        @SerializedName("3")
        UPLOAD_PROJECT_SYSBUCKET,
        @SerializedName("4")
        UPLOAD_PROJECT
    }

    enum SharedWorkSpaceUploadType {
        @SerializedName("0")
        UPLOAD_NORMAL,
        @SerializedName("1")
        UPLOAD_VIEW,
        @SerializedName("2")
        UPLOAD_EDIT,
        @SerializedName("3")
        UPLOAD_SYSBUCKET,
        @SerializedName("4")
        UPLOAD_NO_REENCRYPTION
    }

    enum TransferSpaceType {
        PROJECT,
        ENTERPRISE_WORKSPACE,
        SHAREPOINT_ONLINE,
        LOCAL_DRIVE,
        MY_VAULT,
        SHARED_WITH_ME
    }
}
