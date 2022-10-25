package com.nextlabs.nxl.sample;

import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.RightsManager;

import java.util.HashMap;
import java.util.Map;

public class CopyNXLwithNewDUID {

    public static void main(String[] args) throws Exception {

        String routerURL = "https://{rms_router}/router";
        String appKey = "";
        int appId = 1;

        String inputFile = ""; // Input NXL file with full path
        String outputFile = ""; // Ouput NXL file with full path

        String tenantName = "";

        TokenGroupType tgType = TokenGroupType.TOKENGROUP_SYSTEMBUCKET;

        RightsManager manager = new RightsManager(routerURL, appId, appKey);

        manager.copyNXLwithNewDUID(inputFile, outputFile, tenantName, tgType);

        System.out.println("Copy of " + inputFile+ "is created at "+outputFile+" successfully with new DUID"));
    }
}
