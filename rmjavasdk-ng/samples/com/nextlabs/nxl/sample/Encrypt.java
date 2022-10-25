package com.nextlabs.nxl.sample;

import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.RightsManager;

import java.util.HashMap;
import java.util.Map;

public class Encrypt {

    public static void main(String[] args) throws Exception {

        String routerURL = "https://{rms_router}/router";
        String appKey = "";
        int appId = 1;

        String inputFile = "";
        String outputFile = "";

        Rights[] rights = Rights.fromStrings(new String[] { "DOWNLOAD", "VIEW", "WATERMARK" });

        Map<String, String[]> tagMap = new HashMap<>();
        String[] classification = { "ITAR", "EAR" };
        String[] securityClearance = { "Level 7" };
        tagMap.put("Security Clearance", securityClearance);
        tagMap.put("Classification", classification);
        
        String tenantName = "";
        String projectName = "";
        //TokenGroupType tgType = null; // define TOKENGROUP_TENANT or TOKENGROUP_SYSTEMBUCKET
        
        RightsManager manager = new RightsManager(routerURL, appId, appKey);

        //If required, this method will return the current classification profile in the server in 
        //case the caller prefers to verify that its a valid profile, as encrypt methods do not do this.
        Map<String, String[]> permittedClassification = manager.getClassification(tenantName, TokenGroupType.TOKENGROUP_PROJECT, projectName);
        //Map<String, String[]> permittedClassification = manager.getClassification(tenantName, tgType, null);

        manager.encrypt(inputFile, outputFile, null, rights, tagMap, tenantName, projectName);
        //manager.encrypt(inputFile, outputFile, null, rights, tagMap, tenantName, tgType);
        
        System.out.println("Encryption Completed");
    }
}
