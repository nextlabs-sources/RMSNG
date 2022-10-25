package com.nextlabs.nxl.sample;

import com.nextlabs.common.shared.JsonProject;
import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.RightsManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EncryptProjectFile {

    public static void main(String[] args) throws Exception {

        String routerURL = "https://{rms_router}/router";
        String appKey = "";
        int appId = 1;
        String projectName = "APIProject";
        
        RightsManager manager = new RightsManager(routerURL, appId, appKey);
        // list all projects owned by me
        List<JsonProject> projects = manager.listProjects(true, null);
        for (JsonProject project : projects) {
        	System.out.println("Project Name: " + project.getName() + " Tenant Name: " + project.getParentTenantName());
        }
        
        // get metadata of APIProject
        JsonProject project = manager.getProjectMetadata(null, projectName, null);
        if (null == project) {
        	project = manager.createProject(projectName, new String[] {"IT"}, null);
        }
        
        // use tenantName of project to encrypt files
        String inputFile = "";
        String outputFile = "";

        Map<String, String[]> tagMap = new HashMap<>();
        String[] classification = { "ITAR", "EAR" };
        String[] securityClearance = { "Level 7" };
        tagMap.put("Security Clearance", securityClearance);
        tagMap.put("Classification", classification);
        
        manager.encrypt(inputFile, outputFile, null, null, tagMap, project.getParentTenantName(), projectName);
        System.out.println("Encryption completed");
    }
}
