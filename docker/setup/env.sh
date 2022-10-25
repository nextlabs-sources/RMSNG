#!/bin/bash


### Fields necessary to start SkyDRM server ###

SKYDRM_DEFAULT_TENANT_NAME="tenant"

#Only 1 of the 2 Inbuilt Storage Providers will have values
SKYDRM_INBUILT_STORAGE_PROVIDER_NAME="S3"
SKYDRM_INBUILT_STORAGE_PROVIDER_S3_APP_ID=""
#APP_SECRET will be entered by asking user to type it in
SKYDRM_INBUILT_STORAGE_PROVIDER_S3_MYSPACE_BUCKET=""
SKYDRM_INBUILT_STORAGE_PROVIDER_S3_PROJECT_BUCKET=""
SKYDRM_INBUILT_STORAGE_PROVIDER_S3_ENTERPRISE_BUCKET=""


#SKYDRM_INBUILT_STORAGE_PROVIDER_NAME="LOCAL_DRIVE"
#SKYDRM_INBUILT_STORAGE_PROVIDER_LOCAL_DRIVE_MYSPACE_BUCKET=""
#SKYDRM_INBUILT_STORAGE_PROVIDER_LOCAL_DRIVE_PROJECT_BUCKET=""
#SKYDRM_INBUILT_STORAGE_PROVIDER_LOCAL_DRIVE_ENTERPRISE_BUCKET=""

SKYDRM_SMTP_SERVER=""
SKYDRM_SMTP_PORT=
SKYDRM_SMTP_IS_SSL=""
#NAME and PASSWORD will be entered by asking user to type it in

SKYDRM_DATABASE_IS_EMBEDDED=
#only if above is false, should the following db related fields be set
SKYDRM_DATABASE_PROVIDER=3
#acceptable values are 1 for PostgreSQL, 2 for Microsoft SQL Server or 3 for Oracle Database
SKYDRM_DATABASE_HOSTNAME=""
SKYDRM_DATABASE_PORT=
#db password will be entered by asking user to type it in
SKYDRM_DATABASE_ORACLE_SERVICENAME=""

SKYDRM_HOSTNAME=""
#Atleast one admin's IDP must be configured below in the optional list so the admin can login to admin console
SKYDRM_PUBLIC_TENANT_ADMINS_CSV=""

SKYDRM_CC_ADMIN_USERNAME=""
#PASSWORD will be entered by asking user to type it in
SKYDRM_CC_ICENET_URL=""
SKYDRM_CC_CONSOLE_URL=""

SKYDRM_VIEWER_STATELESS=""

SKYDRM_CACHING_MODE_SERVER=""


### Fields that are optional for starting SkyDRM server ###

SKYDRM_LOGGER_URL=""

#IDP values and the IDP of at least one member of tenant admin's csv must be provided here
SKYDRM_IDP_RMS_ATTRIBUTES="{}"

SKYDRM_IDP_LDAP_NUMBER_OF_SERVERS=1
#There must be as many LDAP IDP entries as number of servers with suffix used to differentiate them
SKYDRM_IDP_LDAP_DISPLAY_NAME_1=""
SKYDRM_IDP_LDAP_HOSTNAME_1=""
SKYDRM_IDP_LDAP_DOMAIN_NAME_1=""
SKYDRM_IDP_LDAP_SEARCHBASE_1=""
SKYDRM_IDP_LDAP_SEARCHQUERY_1=""
SKYDRM_IDP_LDAP_RMSGROUP_1=""
SKYDRM_IDP_LDAP_IS_SSL_1=""
SKYDRM_IDP_LDAP_DOES_SECURITY_PRINCIPAL_USE_USERID_1=""
SKYDRM_IDP_LDAP_GLOBAL_UNIQUE_ID_1=""
#second ldap server details ommitted for brevity

SKYDRM_IDP_SAML_NUMBER_OF_SERVERS=1
#Suffix is used to differentiate the count of SAML IDP providers
SKYDRM_IDP_SAML_DISPLAY_NAME_1=""
SKYDRM_IDP_SAML_SP_ENTITY_ID_1=""
SKYDRM_IDP_SAML_SP_ACS_URL_1=""
SKYDRM_IDP_SAML_IDP_ENTITY_ID_1=""
SKYDRM_IDP_SAML_IDP_SSO_URL_1=""
SKYDRM_IDP_SAML_SP_NAME_ID_FORMAT_1=""
SKYDRM_IDP_SAML_SIGN_ALGO_1=""
SKYDRM_IDP_SAML_AUTH_N_CONTEXT_1=""
SKYDRM_IDP_SAML_BUTTON_TEXT_1=""
SKYDRM_IDP_SAML_IDP_X509_CERT_1=""
SKYDRM_IDP_SAML_GLOBAL_UNIQUE_ID_1=""

SKYDRM_IDP_FACEBOOK_APP_ID=""
#APP_SECRET will be entered by user signing in

SKYDRM_IDP_GOOGLE_APP_ID=""
#APP_SECRET will be entered by user signing in



### Fields that can be used to optionally/further customize SkyDRM or configure for alternate deployment strategies but necessary to be documented as they are internal or on a per-request basis ###

#Optional and defaults to /opt/nextlabs/rms/
SKYDRM_RMS_DATA_DIR=""
