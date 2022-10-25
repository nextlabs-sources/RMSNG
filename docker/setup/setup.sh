#!/bin/bash
# Header function to display the current section

function write_header() {
  local h="$@"
  echo "---------------------------------------------------------------"
  echo "     ${h}"
  echo "---------------------------------------------------------------"
}

# Variable to handle double quotes escape value
dqt='"'

function configure_default_tenant_name() {

  write_header "Check 1 - Configuring Tenant Name"

  if [ -n "$SKYDRM_DEFAULT_TENANT_NAME" ]; then
    if ! [[ $SKYDRM_DEFAULT_TENANT_NAME =~ ^[a-zA-Z0-9\ ._-]*$ ]]; then
      write_header "The tenant name can only contain alphanumeric characters, spaces, hyphens, and underscores. Please enter a valid tenant name: "
      exit 1
    else
      echo web.publicTenant="$SKYDRM_DEFAULT_TENANT_NAME" >>${RMS_CONF_DIR}/admin.properties
    fi
  else
    write_header "Default Tenant name will be automatically chosen as a unique string by SkyDRM"
  fi
}

# Function to setup the identity providers
function setup_idps() {

  configure_ldap
  echo
  configure_saml
  echo
  configure_fb
  echo
  configure_google
  echo
  configure_rmsidp
  echo
}

# Initial function called to configure Identity Providers
function configure_idp() {

  write_header "[Optional] Check 10 - IdP Configuration"  
  setup_idps
}

# Function to configure LDAP as IDP
function configure_ldap() {

  if [[ $SKYDRM_IDP_LDAP_NUMBER_OF_SERVERS =~ ^[0-9]+$ ]]; then
    echo "Number of LDAP Servers : $SKYDRM_IDP_LDAP_NUMBER_OF_SERVERS"
  else
    echo "***WARNING***: Number of LDAP servers not specified or invalid"
    return 0
  fi

  idp_ldap=()
  idp_ldap_count="$SKYDRM_IDP_LDAP_NUMBER_OF_SERVERS"
  for count in $(seq 1 $idp_ldap_count); do
    local ldaptype="AD"
    echo "Using LDAP server $count ldap type : $ldaptype"

    local ldap_name=$"SKYDRM_IDP_LDAP_DISPLAY_NAME_$count"
    ldap_name=$(eval echo "\$$ldap_name")
    if [[ -z "$ldap_name" ]]; then
      echo "***WARNING***: LDAP server $count display name is not specified (e.g. MyAD)"
      return 0
    else
      echo "Using LDAP server $count display name : $ldap_name"
    fi
    
    local ldap_hostName=$"SKYDRM_IDP_LDAP_HOSTNAME_$count"
    ldap_hostName=$(eval echo "\$$ldap_hostName")
    if [[ -z "$ldap_hostName" ]]; then
      echo "***WARNING***: LDAP server $count Host Name[:port] is not specified (e.g. adserver.lab.acme.com)"
      return 0
    else
      echo "Using LDAP server $count Host Name[:port] : $ldap_hostName"
    fi
    
    local ldap_domainName=$"SKYDRM_IDP_LDAP_DOMAIN_NAME_$count"
    ldap_domainName=$(eval echo "\$$ldap_domainName")
    if [[ -z "$ldap_domainName" ]]; then
      echo "***WARNING***: LDAP server $count Domain Name is not specified (e.g. lab.acme.com)"
      return 0
    else
      echo "Using LDAP server $count Domain Name : $ldap_domainName"
    fi

    local ldap_searchBase=$"SKYDRM_IDP_LDAP_SEARCHBASE_$count"
    ldap_searchBase=$(eval echo "\$$ldap_searchBase")
    if [[ -z "$ldap_searchBase" ]]; then
      echo "***WARNING***: LDAP server $count Search Base is not specified (e.g. DC=lab,DC=acme,DC=com)"
      return 0
    else
      echo "Using LDAP server $count Search Base : $ldap_searchBase"
    fi

    local ldap_searchQuery=$"SKYDRM_IDP_LDAP_SEARCHQUERY_$count"
    ldap_searchQuery=$(eval echo "\$$ldap_searchQuery")
    if [[ -z "$ldap_searchQuery" ]]; then
      echo "***WARNING***: LDAP server $count User Search Query is not specified (e.g. (&(objectClass=user)(sAMAccountName=\$USERID\$)) )"
      return 0
    else
      echo "Using LDAP server $count User Search Query : $ldap_searchQuery"
    fi

    local ldapSSL=$"SKYDRM_IDP_LDAP_IS_SSL_$count"
    ldapSSL=$(eval echo "\$$ldapSSL")
    if [ -n "$ldapSSL" ]; then
      case $ldapSSL in
        [yY][eE][sS] | [yY] | [tT][rR][uU][eE])
          ldapSSL=true
          echo "Whether using SSL for LDAPS for server $count : $ldapSSL"
          ;;

        [nN][oO] | [nN] | [fF][aA][lL][sS][eE])
          ldapSSL=false
          echo "Whether using SSL for LDAPS for server $count : $ldapSSL"
          ;;

        *)
          echo "***WARNING***: Invalid input for SSL for LDAPS for server $count instead of true/false : $ldapSSL"
          return 0
          ;;
      esac
    else
      echo "***WARNING***: Whether using SSL for LDAPS for server $count is not specified"
      return 0
    fi

    local ldap_userGroup=$"SKYDRM_IDP_LDAP_RMSGROUP_$count"
    ldap_userGroup=$(eval echo "\$$ldap_userGroup")
    if [[ -z "$ldap_userGroup" ]]; then
      echo "LDAP server $count User Group (e.g. Engineers) is optional so will be empty since not specified"
    else
      echo "Using LDAP server $count User Group : $ldap_userGroup"
    fi

    local ldap_securityPrincipalUseUserID=$"SKYDRM_IDP_LDAP_DOES_SECURITY_PRINCIPAL_USE_USERID_$count"
    ldap_securityPrincipalUseUserID=$(eval echo "\$$ldap_securityPrincipalUseUserID")
    if [ -n "$ldap_securityPrincipalUseUserID" ]; then
      case $ldap_securityPrincipalUseUserID in
        [yY][eE][sS] | [yY] | [tT][rR][uU][eE])
          ldap_securityPrincipalUseUserID=true
          echo "Whether using (optional) UserID for principal security  for LDAP for server $count : $ldap_securityPrincipalUseUserID"
          ;;

        [nN][oO] | [nN] | [fF][aA][lL][sS][eE])
          ldap_securityPrincipalUseUserID=false
          echo "Whether using (optional) UserID for principal security for LDAP for server $count : $ldap_securityPrincipalUseUserID"
          ;;

        *)
          echo "Invalid input for whether using (optional) UserID for principal security for server $count instead of true/false : $ldap_securityPrincipalUseUserID"
          ;;
      esac
    else
      echo "Whether using UserID for principal security for LDAP for server $count is optional so will be false as not specified"
    fi

    local ldap_globalUniqueID=$"SKYDRM_IDP_LDAP_GLOBAL_UNIQUE_ID_$count"
    ldap_globalUniqueID=$(eval echo "\$$ldap_globalUniqueID")
    if [[ -z "$ldap_globalUniqueID" ]]; then
      echo "***WARNING***: LDAP server $count Global Unique ID is not specified (e.g. objectSid )"
      return 0
    else
      echo "Using LDAP server $count Global Unique ID : $ldap_globalUniqueID"
    fi

    idp_ldap[count - 1]='web.idp.ldap.'${count}'.attributes={\'${dqt}'name'${dqt}':'${dqt}$ldap_name${dqt}',\'${dqt}'ldapType'${dqt}':'${dqt}$ldaptype${dqt}',\'${dqt}'hostName'${dqt}':'${dqt}$ldap_hostName${dqt}',\'${dqt}'domain'${dqt}':'${dqt}$ldap_domainName${dqt}',\'${dqt}'searchBase'${dqt}':'${dqt}$ldap_searchBase${dqt}',\'${dqt}'userSearchQuery'${dqt}':'${dqt}$ldap_searchQuery${dqt}',\'${dqt}'rmsGroup'${dqt}':'${dqt}$ldap_userGroup${dqt}',\'${dqt}'ldapSSL'${dqt}':'${dqt}${ldapSSL}${dqt}'\,\'${dqt}'securityPrincipalUseUserID'${dqt}':'${dqt}${ldap_securityPrincipalUseUserID}${dqt}'\,\'${dqt}'evalUserIdAttribute'${dqt}':'${dqt}${ldap_globalUniqueID}${dqt}'\}'
  done
  save_ldap
}

# Function to configure FACEBOOK login as IDP
function configure_fb() {

  if [[ -z "$SKYDRM_IDP_FACEBOOK_APP_ID" ]]; then
      echo "***WARNING***: Facebook IDP APP ID not specified"
      return 0
  else
    echo "Using APP ID for Facebook IDP : $SKYDRM_IDP_FACEBOOK_APP_ID"
  fi

  while true; do
    read -r -sp "Enter the App secret for Facebook : " fb_appSecret
    echo
    read -r -sp "Re-enter the App secret for Facebook : " re_fb_appSecret
    echo
    if compare "$fb_appSecret" "$re_fb_appSecret"; then
      break
    else
      echo "App secret does not match, please try again."
    fi
  done
  encrypted_fb_appSecret=$(encrypt "$fb_appSecret")
  encrypt_rms_secrets="$encrypt_rms_secrets web.idp.fb.attributes"
  echo web.idp.fb.attributes={\\${dqt}appId${dqt}:${dqt}$SKYDRM_IDP_FACEBOOK_APP_ID${dqt},\\${dqt}appSecret${dqt}:${dqt}$encrypted_fb_appSecret${dqt}\\ } >>${RMS_CONF_DIR}/admin.properties
}

# Function to configure GOOGLE login as IDP
function configure_google() {

  if [[ -z "$SKYDRM_IDP_GOOGLE_APP_ID" ]]; then
      echo "***WARNING***: Google IDP APP ID not specified"
      return 0
  else
    echo "Using APP ID for Google IDP : $SKYDRM_IDP_GOOGLE_APP_ID"
  fi
  
  while true; do
    read -r -sp "Enter the App secret for Google : " google_appSecret
    echo
    read -r -sp "Re-enter the App secret for Google : " re_google_appSecret
    echo
    if compare "$google_appSecret" "$re_google_appSecret"; then
      break
    else
      echo "App secret does not match, please try again."
    fi
  done
  encrypted_google_appSecret=$(encrypt "$google_appSecret")
  encrypt_rms_secrets="$encrypt_rms_secrets web.idp.google.attributes"
  echo web.idp.google.attributes={\\${dqt}appId${dqt}:${dqt}$SKYDRM_IDP_GOOGLE_APP_ID${dqt},\\${dqt}appSecret${dqt}:${dqt}$encrypted_google_appSecret${dqt}\\ } >>${RMS_CONF_DIR}/admin.properties
}

# Function to configure SkyDRM as IDP
function configure_rmsidp() {

  if [[ -z "$SKYDRM_IDP_RMS_ATTRIBUTES" ]]; then
      echo "***WARNING***: SkyDRM IDP attributes not specified"
      return 0
  else
    echo "Using attributes for SkyDRM IDP : $SKYDRM_IDP_RMS_ATTRIBUTES"
    echo web.idp.rms.attributes={} >>${RMS_CONF_DIR}/admin.properties
  fi
}

# Function to configure SAML as IDP
function configure_saml() {

  if [[ $SKYDRM_IDP_SAML_NUMBER_OF_SERVERS =~ ^[0-9]+$ ]]; then
    echo "Number of SAML providers : $SKYDRM_IDP_SAML_NUMBER_OF_SERVERS"
  else
    echo "***WARNING***: Please enter a valid number of SAML providers"
    return 0
  fi

  idp_saml=()
  idp_saml_count=$SKYDRM_IDP_SAML_NUMBER_OF_SERVERS
  for count in $(seq 1 $idp_saml_count); do
    local saml_name=$"SKYDRM_IDP_SAML_DISPLAY_NAME_$count"
    saml_name=$(eval echo "\$$saml_name")
    if [[ -z "$saml_name" ]]; then
      echo "***WARNING***: SAML provider $count display name is not specified"
      return 0
    else
      echo "Using SAML provider $count display name : $saml_name"
    fi

    local saml_spEntityId=$"SKYDRM_IDP_SAML_SP_ENTITY_ID_$count"
    saml_spEntityId=$(eval echo "\$$saml_spEntityId")
    if [[ -z "$saml_spEntityId" ]]; then
      echo "SAML provider $count Service Provider Entity Id is not specified"
      return 0
    else
      echo "Using SAML provider $count Service Provider Entity Id : $saml_spEntityId"
    fi

    local saml_spAcsUrl=$"SKYDRM_IDP_SAML_SP_ACS_URL_$count"
    saml_spAcsUrl=$(eval echo "\$$saml_spAcsUrl")
    if [[ -z "$saml_spAcsUrl" ]]; then
      echo "***WARNING***: SAML provider $count Service Provider ACS URL is not specified"
      return 0
    else
      echo "Using SAML provider $count Service Provider ACS URL : $saml_spAcsUrl"
    fi
    
    local saml_idpEntityId=$"SKYDRM_IDP_SAML_IDP_ENTITY_ID_$count"
    saml_idpEntityId=$(eval echo "\$$saml_idpEntityId")
    if [[ -z "$saml_idpEntityId" ]]; then
      echo "***WARNING***: SAML provider $count Identity Provider Entity Id is not specified"
      return 0
    else
      echo "Using SAML provider $count Identity Provider Entity Id : $saml_idpEntityId"
    fi
    
    local saml_idpSsoUrl=$"SKYDRM_IDP_SAML_IDP_SSO_URL_$count"
    saml_idpSsoUrl=$(eval echo "\$$saml_idpSsoUrl")
    if [[ -z "$saml_idpSsoUrl" ]]; then
      echo "***WARNING***: SAML provider $count Identity Provider SSO URL is not specified"
      return 0
    else
      echo "Using SAML provider $count Identity Provider SSO URL : $saml_idpSsoUrl"
    fi
    
    local saml_idpX509Cert=$"SKYDRM_IDP_SAML_IDP_X509_CERT_$count"
    saml_idpX509Cert=$(eval echo "\$$saml_idpX509Cert")
    if [[ -z "$saml_idpX509Cert" ]]; then
      echo "***WARNING***: SAML provider $count Identity Provider X509 Certificate is not specified"
      return 0
    else
      echo "Using SAML provider $count Identity Provider X509 Certificate : $saml_idpX509Cert"
    fi

    local saml_spNameIdFormat=$"SKYDRM_IDP_SAML_SP_NAME_ID_FORMAT_$count"
    saml_spNameIdFormat=$(eval echo "\$$saml_spNameIdFormat")
    if [[ -z "$saml_spNameIdFormat" ]]; then
      echo "SAML provider $count Service Provider Name ID Format is optional so will be empty as not specified"
    else
      echo "Using SAML provider $count Service Provider Name ID Format : $saml_spNameIdFormat"
    fi
    
    local saml_signAlgo=$"SKYDRM_IDP_SAML_SIGN_ALGO_$count"
    saml_signAlgo=$(eval echo "\$$saml_signAlgo")
    if [[ -z "$saml_signAlgo" ]]; then
      echo "SAML provider $count signature algorithm is optional so will be empty as not specified"
    else
      echo "Using SAML provider $count signature algorithm : $saml_signAlgo"
    fi
    
    local saml_authNContext=$"SKYDRM_IDP_SAML_AUTH_N_CONTEXT_$count"
    saml_authNContext=$(eval echo "\$$saml_authNContext")
    if [[ -z "$saml_authNContext" ]]; then
      echo "SAML provider $count autentication context is optional so will be empty as not specified"
    else
      echo "Using SAML provider $count autentication context : $saml_authNContext"
    fi

    local saml_buttonText=$"SKYDRM_IDP_SAML_BUTTON_TEXT_$count"
    saml_buttonText=$(eval echo "\$$saml_buttonText")
    if [[ -z "$saml_buttonText" ]]; then
      echo "SAML provider $count text to be displayed on the login button is optional so will be empty as not specified"
    else
      echo "Using SAML provider $count text to be displayed on the login button : $saml_buttonText"
    fi

    local saml_globalUniqueID=$"SKYDRM_IDP_SAML_GLOBAL_UNIQUE_ID_$count"
    saml_globalUniqueID=$(eval echo "\$$saml_globalUniqueID")
    if [[ -z "$saml_globalUniqueID" ]]; then
      echo "***WARNING***: SAML provider $count Global Unique ID is not specified (e.g. objectSid )"
      return 0
    else
      echo "Using SAML provider $count Global Unique ID : $saml_globalUniqueID"
    fi

    idp_saml[count - 1]='web.idp.saml.'${count}'.attributes={\'${dqt}'name'${dqt}':'${dqt}$saml_name${dqt}',\'${dqt}'spEntityId'${dqt}':'${dqt}$saml_spEntityId${dqt}',\'${dqt}'spAcsUrl'${dqt}':'${dqt}$saml_spAcsUrl${dqt}',\'${dqt}'idpEntityId'${dqt}':'${dqt}$saml_idpEntityId${dqt}',\'${dqt}'idpSsoUrl'${dqt}':'${dqt}$saml_idpSsoUrl${dqt}',\'${dqt}'spNameIdFormat'${dqt}':'${dqt}$saml_spNameIdFormat${dqt}',\'${dqt}'signAlgo'${dqt}':'${dqt}$saml_signAlgo${dqt}',\'${dqt}'authNContext'${dqt}':'${dqt}$saml_authNContext${dqt}',\'${dqt}'buttonText'${dqt}':'${dqt}$saml_buttonText${dqt}',\'${dqt}'idpX509Cert'${dqt}':'${dqt}$saml_idpX509Cert${dqt}'\,\'${dqt}'evalUserIdAttribute'${dqt}':'${dqt}${saml_globalUniqueID}${dqt}'\}'
  done
  save_saml
}

# Function to configure inbuilt service provider
function configure_default_storage_provider() {
  
  write_header "Check 2 - Configuring default storage provider"
  if [ -z "$SKYDRM_INBUILT_STORAGE_PROVIDER_NAME" ]; then
    write_header "Storage Provider name missing; please set variable and relaunch script" 
    exit 1 
  fi

  if [ "$SKYDRM_INBUILT_STORAGE_PROVIDER_NAME" != "S3" ]  && [ "$SKYDRM_INBUILT_STORAGE_PROVIDER_NAME" != "LOCAL_DRIVE" ] ; then
    write_header "Unknown Storage Provider name; please set correct variable and relaunch script"
  fi

  case $SKYDRM_INBUILT_STORAGE_PROVIDER_NAME in
    "S3") spType="S3"
          if [ -z "$SKYDRM_INBUILT_STORAGE_PROVIDER_S3_APP_ID" ]; then
            write_header "Storage Provider S3's AppId missing; please set variable and relaunch script" 
            exit 1 
          fi
          echo "Using inbuilt storage provider type : $SKYDRM_INBUILT_STORAGE_PROVIDER_NAME"
          
          echo "Using the S3 Access Key ID : $SKYDRM_INBUILT_STORAGE_PROVIDER_S3_APP_ID" 
          while true; do
            read -r -sp "Enter the Secret Access Key  : " spSecretAccess
            echo
            read -r -sp "Re-enter the Secret Access Key : " re_spSecretAccess
            echo
            if compare "$spSecretAccess" "$re_spSecretAccess"; then
              break
            else
              echo "S3's Secret Access Key does not match, please try again."
            fi
          done
          
          if [ -z "$SKYDRM_INBUILT_STORAGE_PROVIDER_S3_MYSPACE_BUCKET" ]; then
            write_header "Storage Provider S3's storage path for MySpace missing; please set variable and relaunch script" 
            exit 1 
          fi
          if [ -z "$SKYDRM_INBUILT_STORAGE_PROVIDER_S3_PROJECT_BUCKET" ]; then
            write_header "Storage Provider S3's storage path for Projects missing; please set variable and relaunch script" 
            exit 1 
          fi
          if [ -z "$SKYDRM_INBUILT_STORAGE_PROVIDER_S3_ENTERPRISE_BUCKET" ]; then
            write_header "Storage Provider S3's storage path for Enterprise Workspace missing; please set variable and relaunch script" 
            exit 1 
          fi

          echo "Using the storage path for MySpace (e.g., Documents/drm-mydrive) : $SKYDRM_INBUILT_STORAGE_PROVIDER_S3_MYSPACE_BUCKET"
          echo "Using the storage path for Projects (e.g., Documents/drm-projects) : $SKYDRM_INBUILT_STORAGE_PROVIDER_S3_PROJECT_BUCKET"
          echo "Using the storage path for Enterprise WorkSpace (e.g., Documents/drm-enterprise) : $SKYDRM_INBUILT_STORAGE_PROVIDER_S3_ENTERPRISE_BUCKET"
          (
            echo web.inbuilt_storage_provider=${spType}
            encrypted_spSecretAccess=$(encrypt "$spSecretAccess")
            echo web.inbuilt_storage_provider.attributes={\\${dqt}APP_ID${dqt}:${dqt}$SKYDRM_INBUILT_STORAGE_PROVIDER_S3_APP_ID${dqt},\\${dqt}APP_SECRET${dqt}:${dqt}$encrypted_spSecretAccess${dqt},\\${dqt}MYSPACE_BUCKET_NAME${dqt}:${dqt}$SKYDRM_INBUILT_STORAGE_PROVIDER_S3_MYSPACE_BUCKET${dqt},\\${dqt}PROJECT_BUCKET_NAME${dqt}:${dqt}$SKYDRM_INBUILT_STORAGE_PROVIDER_S3_PROJECT_BUCKET${dqt},\\${dqt}ENTERPRISE_BUCKET_NAME${dqt}:${dqt}$SKYDRM_INBUILT_STORAGE_PROVIDER_S3_ENTERPRISE_BUCKET${dqt}\\ } >>${RMS_CONF_DIR}/admin.properties
          ) >>${RMS_CONF_DIR}/admin.properties
          encrypt_rms_secrets="$encrypt_rms_secrets web.inbuilt_storage_provider.attributes"
          return 0
    ;;

    "ONEDRIVE_FORBUSINESS") spType="ONEDRIVE_FORBUSINESS"
          if [ -z "$SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_USERNAME" ]; then
            write_header "Storage Provider OD4B's Username missing; please set variable and relaunch script" 
            exit 1 
          fi
          echo "Using inbuilt storage provider type : $SKYDRM_INBUILT_STORAGE_PROVIDER_NAME"

          echo "Using the OD4B Username : $SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_USERNAME"
          while true; do
            read -r -sp "Enter the User Password  : " spUserPassword
            echo
            read -r -sp "Re-enter the User Password : " re_spUserPassword
            echo
            if compare "$spUserPassword" "$re_spUserPassword"; then
              break
            else
              echo "Password does not match, please try again."
            fi
          done
          
          if [ -z "$SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_SHAREPOINT_SITE" ]; then
            write_header "Storage Provider OD4B's sharepoint site missing; please set variable and relaunch script" 
            exit 1 
          fi
          if [ -z "$SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_MYSPACE_BUCKET" ]; then
            write_header "Storage Provider OD4B's storage path for MySpace missing; please set variable and relaunch script" 
            exit 1 
          fi
          if [ -z "$SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_PROJECT_BUCKET" ]; then
            write_header "Storage Provider OD4B's storage path for Projects missing; please set variable and relaunch script" 
            exit 1 
          fi
          if [ -z "$SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_ENTERPRISE_BUCKET" ]; then
            write_header "Storage Provider OD4B's storage path for Enterprise Workspace missing; please set variable and relaunch script" 
            exit 1 
          fi

          echo "Using the OneDrive for Business SharePoint site URL : $SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_SHAREPOINT_SITE"
          echo "Using the storage path for MySpace (e.g., Documents/drm-mydrive) : $SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_MYSPACE_BUCKET"
          echo "Using the storage path for Projects (e.g., Documents/drm-projects) : $SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_PROJECT_BUCKET"
          echo "Using the storage path for Enterprise WorkSpace (e.g., Documents/drm-enterprise) : $SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_ENTERPRISE_BUCKET"
          (
            echo web.inbuilt_storage_provider=${spType}
            encrypted_spUserPassword=$(encrypt "$spUserPassword")
            echo web.inbuilt_storage_provider.attributes={\\${dqt}USER_NAME${dqt}:${dqt}$SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_USERNAME${dqt},\\${dqt}USER_SECRET${dqt}:${dqt}$encrypted_spUserPassword${dqt},\\${dqt}SHAREPOINT_SITE${dqt}:${dqt}$SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_SHAREPOINT_SITE${dqt},\\${dqt}MYSPACE_BUCKET_NAME${dqt}:${dqt}$SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_MYSPACE_BUCKET${dqt},\\${dqt}PROJECT_BUCKET_NAME${dqt}:${dqt}$SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_PROJECT_BUCKET${dqt},\\${dqt}ENTERPRISE_BUCKET_NAME${dqt}:${dqt}$SKYDRM_INBUILT_STORAGE_PROVIDER_OD4B_ENTERPRISE_BUCKET${dqt}\\ } >>${RMS_CONF_DIR}/admin.properties
          ) >>${RMS_CONF_DIR}/admin.properties
          encrypt_rms_secrets="$encrypt_rms_secrets web.inbuilt_storage_provider.attributes"
          return 0
    ;;

    "LOCAL_DRIVE") spType="LOCAL_DRIVE"
          local localPath="/var/opt/nextlabs/rms/shared/"
          echo "Using inbuilt storage provider type : $SKYDRM_INBUILT_STORAGE_PROVIDER_NAME"

          if [ -z "$SKYDRM_INBUILT_STORAGE_PROVIDER_LOCAL_DRIVE_MYSPACE_BUCKET" ]; then
            write_header "Storage Provider LOCAL_DRIVE's storage path for MySpace missing; please set variable and relaunch script" 
            exit 1 
          fi
          if [ -z "$SKYDRM_INBUILT_STORAGE_PROVIDER_LOCAL_DRIVE_PROJECT_BUCKET" ]; then
            echo "Storage Provider LOCAL_DRIVE's storage path for Projects missing; please set variable and relaunch script" 
            exit 1 
          fi
          if [ -z "$SKYDRM_INBUILT_STORAGE_PROVIDER_LOCAL_DRIVE_ENTERPRISE_BUCKET" ]; then
            echo "Storage Provider LOCAL_DRIVE's storage path for Enterprise Workspace missing; please set variable and relaunch script" 
            exit 1 
          fi

          echo "Using the storage path for MySpace (e.g., Documents/drm-mydrive) : $SKYDRM_INBUILT_STORAGE_PROVIDER_LOCAL_DRIVE_MYSPACE_BUCKET"
          echo "Using the storage path for Projects (e.g., Documents/drm-projects) : $SKYDRM_INBUILT_STORAGE_PROVIDER_LOCAL_DRIVE_PROJECT_BUCKET"
          echo "Using the storage path for Enterprise WorkSpace (e.g., Documents/drm-enterprise) : $SKYDRM_INBUILT_STORAGE_PROVIDER_LOCAL_DRIVE_ENTERPRISE_BUCKET"
          (
            echo web.inbuilt_storage_provider=${spType}
            echo web.inbuilt_storage_provider.attributes={\\${dqt}LOCAL_PATH${dqt}:${dqt}$localPath${dqt},\\${dqt}MYSPACE_BUCKET_NAME${dqt}:${dqt}$SKYDRM_INBUILT_STORAGE_PROVIDER_LOCAL_DRIVE_MYSPACE_BUCKET${dqt},\\${dqt}PROJECT_BUCKET_NAME${dqt}:${dqt}$SKYDRM_INBUILT_STORAGE_PROVIDER_LOCAL_DRIVE_PROJECT_BUCKET${dqt},\\${dqt}ENTERPRISE_BUCKET_NAME${dqt}:${dqt}$SKYDRM_INBUILT_STORAGE_PROVIDER_LOCAL_DRIVE_ENTERPRISE_BUCKET${dqt}\\ } >>${RMS_CONF_DIR}/admin.properties
          ) >>${RMS_CONF_DIR}/admin.properties
          encrypt_rms_secrets="$encrypt_rms_secrets web.inbuilt_storage_provider.attributes"
          return 0
    ;;

    *) write_header "Unknown/Unsupported Service Provider; please check and relaunch script"
       exit 1
    ;;
  esac
}

# Function to configure mail service provider
function configure_smtp() {
  write_header "Check 3 - Configuring SMTP Server"

  if [ -z "$SKYDRM_SMTP_SERVER" ]; then
    write_header "SMTP Server Name missing; please set variable and relaunch script" 
    exit 1 
  fi
  if [ -z "$SKYDRM_SMTP_PORT" ]; then
    write_header "SMTP Port Number missing; please set variable and relaunch script" 
    exit 1 
  fi

  echo "Using the SMTP Server Name (e.g. MyEmailServer.acme.com): $SKYDRM_SMTP_SERVER"
  echo "Using the SMTP port (e.g. 465): $SKYDRM_SMTP_PORT"

  local smtpSSL=""
  if [ -n "$SKYDRM_SMTP_IS_SSL" ]; then
    case $SKYDRM_SMTP_IS_SSL in
      [yY][eE][sS] | [yY] | [tT][rR][uU][eE])
        isSmtpSSL=true
        echo "Whether using SSL for SMTP : $isSmtpSSL"
        ;;

      [nN][oO] | [nN] | [fF][aA][lL][sS][eE])
        isSmtpSSL=false
        echo "Whether using SSL for SMTP : $isSmtpSSL"
        ;;

      *)
        echo "Invalid input for SSL for SMTP instead of true/false : $SKYDRM_SMTP_IS_SSL"
        exit 1
        ;;
    esac
  else
    echo "Whether using SSL for SMTP is missing; please reset variable and relaunch script"
    exit 1
  fi

   read -p "Enter the SMTP user name (e.g. johndoe): " SKYDRM_SMTP_USERNAME

  while true; do
    read -r -sp "Enter the SMTP password (e.g. mypassword): " smtpPassword
    echo
    read -r -sp "Re-enter the SMTP password: " re_smtpPassword
    echo
    if compare "$smtpPassword" "$re_smtpPassword"; then
      break
    else
      echo "SMTP Password does not match, please try again"
    fi
  done
  if [[ -z $smtpPassword ]]; then
    (
      echo smtp.server=$SKYDRM_SMTP_SERVER
      echo smtp.port=$SKYDRM_SMTP_PORT
      echo smtp.ssl=$isSmtpSSL
      echo smtp.user_name=$SKYDRM_SMTP_USERNAME
      echo smtp.password=$smtpPassword
    ) >>${RMS_CONF_DIR}/admin.properties
  else
    encrypt_rms_secrets="$encrypt_rms_secrets smtp.password"
    encrypted_smtpPassword=$(encrypt "$smtpPassword")
    (
      echo smtp.server=$SKYDRM_SMTP_SERVER
      echo smtp.port=$SKYDRM_SMTP_PORT
      echo smtp.ssl=$isSmtpSSL
      echo smtp.user_name=$SKYDRM_SMTP_USERNAME
      echo smtp.password=$encrypted_smtpPassword
    ) >>${RMS_CONF_DIR}/admin.properties
  fi
}

# Function to configure database
function configure_database() {

  write_header "Check 4 - Configuring Database"

  local rmsdbUrl=""
  local routerdbUrl=""
  local dbPassword=""
  local dialect=""
  local driver=""
  local inbuilt=""
  
  if [ -n "$SKYDRM_DATABASE_IS_EMBEDDED" ]; then
    case $SKYDRM_DATABASE_IS_EMBEDDED in
      [yY][eE][sS] | [yY] | [tT][rR][uU][eE])
        inbuilt=true
        echo "Using embedded database : $inbuilt"
        echo "***Warning***: Using embedded container database is not secure for production-deployments, and is only meant for testing/demonstrations"
        rmsdbUrl="jdbc:postgresql://db:5432/rms"
        routerdbUrl="jdbc:postgresql://db:5432/router"
        dialect='org.hibernate.dialect.PostgreSQLDialect'
        driver='org.postgresql.Driver'
        ;;

      [nN][oO] | [nN] | [fF][aA][lL][sS][eE])
        inbuilt=false
        echo "Whether using embedded database : $inbuilt where supported providers are 1 for PostgreSQL, 2 for Microsoft SQL Server or 3 for Oracle Database"

        if [[ "$SKYDRM_DATABASE_PROVIDER" =~ ^[1-3]{1}$ ]] ; then
          echo "Using external database provider $SKYDRM_DATABASE_PROVIDER"
          
          if [ -z "$SKYDRM_DATABASE_HOSTNAME" ]; then
            echo "Database hostname missing; please set variable and relaunch script" 
            exit 1 
          fi
          echo "Using the database hostname (or IP address): $SKYDRM_DATABASE_HOSTNAME"

          if [[ "$SKYDRM_DATABASE_PORT" =~ ^[0-9]+$ ]]; then
            echo "Using the database port number (e.g. 5432): $SKYDRM_DATABASE_PORT"          
          else
            echo "Database port number missing or invalid; please set variable and relaunch script" 
            exit 1 
          fi

          sed -i '/db:/,/^\s*$/{d}' "$CURRENT_DIR/../docker-compose.yml"
          sed -i '/dbdata:/d' "$CURRENT_DIR/../docker-compose.yml"

          if [ "$SKYDRM_DATABASE_PROVIDER" == 1 ]; then
                rmsdbUrl='jdbc:postgresql://'${SKYDRM_DATABASE_HOSTNAME}':'${SKYDRM_DATABASE_PORT}'/rms'
                routerdbUrl='jdbc:postgresql://'${SKYDRM_DATABASE_HOSTNAME}':'${SKYDRM_DATABASE_PORT}'/router'
                dialect='org.hibernate.dialect.PostgreSQLDialect'
                driver='org.postgresql.Driver'
          elif [ "$SKYDRM_DATABASE_PROVIDER" == 2 ]; then
                rmsdbUrl='jdbc:sqlserver://'${SKYDRM_DATABASE_HOSTNAME}':'${SKYDRM_DATABASE_PORT}';databaseName=rms'
                routerdbUrl='jdbc:sqlserver://'${SKYDRM_DATABASE_HOSTNAME}':'${SKYDRM_DATABASE_PORT}';databaseName=router'
                dialect='org.hibernate.dialect.SQLServerDialect'
                driver='com.microsoft.sqlserver.jdbc.SQLServerDriver'
          elif [ "$SKYDRM_DATABASE_PROVIDER" == 3 ]; then
                if [ -z "$SKYDRM_DATABASE_ORACLE_SERVICENAME" ]; then
                  echo "Oracle database servicename missing; please set variable and relaunch script" 
                  exit 1 
                fi
                echo "Using Oracle service name: $SKYDRM_DATABASE_ORACLE_SERVICENAME"
                rmsdbUrl='jdbc:oracle:thin:@//'${SKYDRM_DATABASE_HOSTNAME}':'${SKYDRM_DATABASE_PORT}'/'${SKYDRM_DATABASE_ORACLE_SERVICENAME}''
                routerdbUrl=${rmsdbUrl}
                dialect='org.hibernate.dialect.OracleDialect'
                driver='oracle.jdbc.driver.OracleDriver'
          fi

        else
          echo "Database provider missing; please set variable and relaunch script"
          exit 1
        fi
        ;;

      *)
        echo "Invalid input for whether using embedded database instead of true/false : $SKYDRM_DATABASE_IS_EMBEDDED"
        exit 1
        ;;
    esac
  else
    echo "Whether using embedded database setting is missing; please reset variable and relaunch script"
    exit 1
  fi

  while true; do
    read -r -sp "Enter the Database password: " dbPassword
    echo
    read -r -sp "Re-enter the Database password: " re_dbPassword
    echo
    if compare "$dbPassword" "$re_dbPassword"; then
      break
    else
      echo "Database password does not match, please try again."
    fi
  done
  encrypted_dbPassword=$(encrypt "$dbPassword")
  encrypt_rms_secrets="$encrypt_rms_secrets hibernate.hibernate.connection.password"
  encrypt_router_secrets="$encrypt_router_secrets hibernate.hibernate.connection.password"
  
  case $inbuilt in
  [yY][eE][sS] | [yY] | [tT][rR][uU][eE])
    (
      echo CREATE USER rms PASSWORD \'$dbPassword\'\;
      echo CREATE USER router PASSWORD \'$dbPassword\'\;
      echo CREATE DATABASE router OWNER router ENCODING \'UTF-8\'\;
      echo CREATE DATABASE rms OWNER rms ENCODING \'UTF-8\'\;
      echo \\connect router\;
      echo SET ROLE router\;
      echo CREATE SCHEMA router AUTHORIZATION router\;
      echo \\connect rms\;
      echo SET ROLE rms\;
      echo CREATE SCHEMA rms AUTHORIZATION rms\;
      echo \\q
    ) >>$RMS_DATA_DIR/shared/db/init_setup.sql
    ;;
  esac

  (
    echo hibernate.hibernate.connection.url=${rmsdbUrl}
    echo hibernate.hibernate.connection.username=rms
    echo hibernate.hibernate.connection.password=$encrypted_dbPassword
    echo hibernate.hibernate.connection.driver_class=$driver
    echo hibernate.hibernate.dialect=$dialect
    echo hibernate.hibernate.default_schema=rms

  ) >>${RMS_CONF_DIR}/admin.properties

  (
    echo hibernate.hibernate.connection.url=${routerdbUrl}
    echo hibernate.hibernate.connection.username=router
    echo hibernate.hibernate.connection.password=$encrypted_dbPassword
    echo hibernate.hibernate.connection.driver_class=$driver
    echo hibernate.hibernate.dialect=$dialect
    echo hibernate.hibernate.default_schema=router

  ) >>${RMS_CONF_DIR}/router.properties

}

# Function to save added ldap values
function save_ldap() {
  echo web.idp.ldap.count=${idp_ldap_count} >>${RMS_CONF_DIR}/admin.properties
  for i in "${idp_ldap[@]}"; do
    echo $i >>${RMS_CONF_DIR}/admin.properties
  done
}

# Function to save added saml values
function save_saml() {
  echo web.idp.saml.count=${idp_saml_count} >>${RMS_CONF_DIR}/admin.properties
  for i in "${idp_saml[@]}"; do
    echo $i >>${RMS_CONF_DIR}/admin.properties
  done
}

function exit_configuration() {
  while true; do
    read -r -p "Do you want to exit the configuration step? [y/n] " exit_configure
    case $exit_configure in
    [yY][eE][sS] | [yY])
      [ -d 'scripts' ] && cp -R scripts/ $RMS_DATA_DIR/shared/db/
      echo "You can modify the properties files under ${RMS_DATA_DIR}shared/conf/ before starting the SkyDRM server"
      exit 1
      ;;
    [nN][oO] | [nN])
      display_IDP_menu
      break
      ;;

    *)
      echo "Invalid input. Please enter y/n"
      ;;
    esac
  done
}

function configure_modules() {

 write_header "Check 5 - Admin Configuration"

 if [ -z "$SKYDRM_HOSTNAME" ]; then
    echo "Application or load balancer endpoint missing; please set variable and relaunch script" 
    exit 1 
  fi
  echo "Using the application hostname (or IP address): $SKYDRM_HOSTNAME"

 if [ -z "$SKYDRM_PUBLIC_TENANT_ADMINS_CSV" ]; then
    echo "Default tenant admin(s) missing; please set variable and relaunch script" 
    exit 1 
  fi
  echo "Using the default tenant admin (whose IDP must be configured): $SKYDRM_PUBLIC_TENANT_ADMINS_CSV"

  (
    echo web.cookie_domain=.$SKYDRM_HOSTNAME
    echo web.router_url=https://$SKYDRM_HOSTNAME/router
    echo web.router_internal_url=https://router:8443/router
    echo web.viewer_url=https://$SKYDRM_HOSTNAME/viewer
    echo web.viewer_internal_url=https://viewer:8443/viewer
    echo web.rms_internal_url=https://rms:8443/rms
    echo web.public_tenant_admin=$SKYDRM_PUBLIC_TENANT_ADMINS_CSV
  ) >>${RMS_CONF_DIR}/admin.properties

  (
    echo web.rms_url=https://$SKYDRM_HOSTNAME/rms

  ) >>${RMS_CONF_DIR}/router.properties

  (
    echo web.rms_internal_url=https://rms:8443/rms
    echo web.router_url=https://$SKYDRM_HOSTNAME/router
    echo web.router_internal_url=https://router:8443/router

  ) >>${RMS_CONF_DIR}/viewer.properties

  # Configure web context for the skydrm web
  sed -i "s/skydrm_hostname/$SKYDRM_HOSTNAME/g" $CURRENT_DIR/../docker-compose.yml
}

# Function to configure control center's information for policies
function configure_control_center_policy_server() {

  write_header "Check 6 - Configuring Control Center Policy Server"
  
  if [ -z "$SKYDRM_CC_ADMIN_USERNAME" ]; then
    echo "Control Center's user name with admin privilege not set; please set variable and relaunch script"
    exit 1 
  fi
  echo "Using the Control Center user name with admin privilege: $SKYDRM_CC_ADMIN_USERNAME" 

  while true; do
    read -r -sp "Enter Control Center Admin Password :" cc_admin_secret
    echo
    read -r -sp "Re-enter Control Center Admin Password :" re_cc_admin_secret
    echo
    if compare "$cc_admin_secret" "$re_cc_admin_secret"; then
      break
    else
      echo "Admin Password does not match, please try again"
    fi
  done

  if [ -z "$SKYDRM_CC_ICENET_URL" ]; then
    echo "Control Center's ICENet URL not set; please set variable and relaunch script"
    exit 1 
  fi
  echo "Using the Control Center's ICENet URL: $SKYDRM_CC_ICENET_URL"

  if [ -z "$SKYDRM_CC_CONSOLE_URL" ]; then
    echo "Control Center's Console URL not set; please set variable and relaunch script"
    exit 1 
  fi
  echo "Using the Control Center's Console URL: $SKYDRM_CC_CONSOLE_URL" 

  encrypt_rms_secrets="$encrypt_rms_secrets web.cc.admin.secret"
  encrypted_cc_admin_secret=$(encrypt "$cc_admin_secret")
  (
    echo web.icenet.url=${SKYDRM_CC_ICENET_URL}
    echo web.cc.admin.id=${SKYDRM_CC_ADMIN_USERNAME}
    echo web.cc.admin.secret=${encrypted_cc_admin_secret}
    echo web.cc.console_url=${SKYDRM_CC_CONSOLE_URL}
  ) >>${RMS_CONF_DIR}/admin.properties

  (
    echo web.icenet.url=${SKYDRM_CC_ICENET_URL}
  ) >>${RMS_CONF_DIR}/viewer.properties
}

# Function to configure viewer state mode: stateful or stateless
function configure_viewer_state() {
  write_header "Check 7 - Configuring SkyDRM Viewer"

  local viewer_state=""
  if [ -n "$SKYDRM_VIEWER_STATELESS" ]; then
    case $SKYDRM_VIEWER_STATELESS in
      [yY][eE][sS] | [yY] | [tT][rR][uU][eE])
        viewer_state=true
        echo "Whether using multiple-stateless viewer replica(s): $viewer_state"
        (
          echo STATELESS_MODE=true
        ) >>${RMS_CONF_DIR}/viewer.properties
        ;;

      [nN][oO] | [nN] | [fF][aA][lL][sS][eE])
        viewer_state=false
        echo "Using single-stateful viewer"
        ;;

      *)
        echo "Invalid input for viewer's statelessness instead of true/false : $SKYDRM_VIEWER_STATELESS"
        exit 1
        ;;
    esac
  else
    echo "Whether using multiple-stateless viewer(s) not set; please reset variable and relaunch script"
    exit 1
  fi

  local cache_state=""
  if [ -n "$SKYDRM_CACHING_MODE_SERVER" ]; then
    case $SKYDRM_CACHING_MODE_SERVER in
      [yY][eE][sS] | [yY] | [tT][rR][uU][eE])
        cache_state=true
        echo "Whether using caching server (even as container) that is remotely hosted : $cache_state"
        (
          echo web.caching.mode.server=true
        ) >>${RMS_CONF_DIR}/admin.properties
        (
          echo web.caching.mode.server=true
        ) >>${RMS_CONF_DIR}/viewer.properties
        ;;

      [nN][oO] | [nN] | [fF][aA][lL][sS][eE])
        cache_state=false
        echo "Using caching server in embedded mode"
        sed -i '/cacheserver:/,/^\s*$/{d}' $CURRENT_DIR/../docker-compose.yml
        ;;

      *)
        echo "Invalid input for whether caching server is remotely hosted, instead of true/false : $SKYDRM_CACHING_MODE_SERVER"
        exit 1
        ;;
    esac
  else
    echo "Whether using remotely hosted caching server not set; please reset variable and relaunch script"
    exit 1
  fi
}

# Function to configure rabbitmq
function configure_rabbitmq() {
  
  write_header "Check 8 - Configuring RabbitMQ for SkyDRM mobile clients"

  (
    echo web.rabbitmq.api.host=messagequeue
    echo web.rabbitmq.api.port=15672
    echo web.rabbitmq.mq.host=messagequeue
    echo web.rabbitmq.mq.port=5672
    echo web.rabbitmq.username=rms
    echo web.rabbitmq.password=rms
  ) >>${RMS_CONF_DIR}/admin.properties
}

# Function to configure logger service
function configure_logger() {

  write_header "[Optional] Check 9 - Configuring Logging Service"

  if [ -z "$SKYDRM_LOGGER_URL" ]; then
    echo "***WARNING***: Logging service URL is not specified"
  fi
  echo "Using the URL for Logging service : $SKYDRM_LOGGER_URL"
  (
    echo web.logger.url=${SKYDRM_LOGGER_URL}
  ) >>${RMS_CONF_DIR}/router.properties
}

# Function to create a pseudo UUID
# This function is not a replacement for UUID v1 or UUID v4. It does not create a true random and there can be hash collisions.
function rms_gen_uuid() {
  local N B T
  for ((N = 0; N < 16; ++N)); do
    B=$((RANDOM % 255))
    if ((N == 6)); then
      printf '4%x' $((B % 15))
    elif ((N == 8)); then
      local C='89ab'
      printf '%c%x' ${C:$(($RANDOM % ${#C})):1} $((B % 15))
    else
      printf '%02x' $B
    fi

    for T in 3 5 7 9; do
      if ((T == N)); then
        printf '-'
        break
      fi
    done
  done
}

# Function to prepare the environment and default folders
function init() {

  write_header "Initializing with argument $1"
  if [ -f "$1" ]; then
    if source "$1" 2>/dev/null ; then
      write_header "Loaded environment variables from $1"
    else
      write_header "Exiting due to error in loading variables from $1" 
      exit 1
    fi
  else
    write_header "Using global environment variables"
  fi

  CURRENT_DIR=$(readlink -m $(dirname "$0"))
  #Class path to run the file
  export CLASSPATH="$CURRENT_DIR/lib/crypt.jar":"$CURRENT_DIR/lib/common-framework.jar"
  encrypt_rms_secrets=""
  encrypt_router_secrets=""
  configured_idp=""
  
  if [ -n "$SKYDRM_RMS_DATA_DIR" ]; then
    RMS_DATA_DIR=$(echo "$SKYDRM_RMS_DATA_DIR" | sed 's@/*$@/@g')
    if [[ $RMS_DATA_DIR != /* ]]; then
      write_header "SKYDRM_RMS_DATA_DIR does not begin with a '/', so please reset this environemtn variable and relaunch script"
      exit 1
    else
      DEFAULT_RMS_DATA_DIR='"/opt/nextlabs/rms/'
      REPLACED_RMS_DATA_DIR="\"$RMS_DATA_DIR"
      sed -i "s@$DEFAULT_RMS_DATA_DIR@$REPLACED_RMS_DATA_DIR@g" "$CURRENT_DIR/../docker-compose.yml"
    fi
  else
    RMS_DATA_DIR=/opt/nextlabs/rms/
  fi

  echo "Using directory for RMS: $RMS_DATA_DIR"
  mkdir -p "$RMS_DATA_DIR/shared/conf/"
  mkdir -p "$RMS_DATA_DIR/shared/db/"
  mkdir -p "$RMS_DATA_DIR/shared/cache/"
  mkdir -p "$RMS_DATA_DIR/shared/cachestore/"
  mkdir -p "$RMS_DATA_DIR/shared/viewerPackages/"
  [ -d "${CURRENT_DIR}/conf" ] && cp -rf "${CURRENT_DIR}/conf/" $RMS_DATA_DIR/shared/
  [ -d "${CURRENT_DIR}/cache" ] && cp -rf "${CURRENT_DIR}/cache/" $RMS_DATA_DIR/shared/
  chmod -R 777 $RMS_DATA_DIR
  [ -f "${RMS_DATA_DIR}/shared/conf/infinispan_clustered.xml" ] && sed -i "s/<transport cluster=\"RMS_JGROUPS\" stack=\"external-file\"\/>/<transport cluster=\"RMS_JGROUPS_$(rms_gen_uuid)\" stack=\"external-file\"\/>/g" $RMS_DATA_DIR/shared/conf/infinispan_clustered.xml
  [ -f "${RMS_DATA_DIR}/shared/conf/jgroups_aws.xml" ] && rm -f "${RMS_DATA_DIR}/shared/conf/jgroups_aws.xml"
}

# Function to encrypt the secret before its written to admin.properties and router.properties
function encrypt() {
  encrypted_secret=$(java -cp "$CLASSPATH" com.bluejungle.framework.crypt.Encryptor -password "$1")
  echo $encrypted_secret
}
# Function to compare two strings
function compare() {
  if [ "$1" = "$2" ]; then
    return 0
  else
    return 1
  fi
}
# Initial Function to start the configuration process
function configure() {
  RMS_CONF_DIR=$RMS_DATA_DIR/shared/conf

  #Necessary fields to start SkyDRM server
  configure_default_tenant_name
  configure_default_storage_provider
  configure_smtp
  configure_database
  configure_modules
  configure_control_center_policy_server
  configure_viewer_state
  configure_rabbitmq
  
  #Optional Fields to start server
  configure_logger
  configure_idp

  echo encrypt.secrets=$encrypt_rms_secrets >>${RMS_CONF_DIR}/admin.properties
  echo encrypt.secrets=$encrypt_router_secrets >>${RMS_CONF_DIR}/router.properties
}

# Entry point for the script
init $1
configure

echo "Successfully configured SkyDRM. You can find/edit the saved configuration from the properties files under ${RMS_DATA_DIR}shared/conf/"
