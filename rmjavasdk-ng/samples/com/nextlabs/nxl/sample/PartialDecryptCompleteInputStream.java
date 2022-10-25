package com.nextlabs.nxl.sample;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.nextlabs.common.shared.Constants.TokenGroupType;
import com.nextlabs.nxl.RightsManager;

public class PartialDecryptCompleteInputStream {

    public static void main(String[] args) throws Exception {

        String routerURL = "https://{rms_router}/router";
        String appKey = "";
        int appId = 1;

        String inputFile = "";
        String outputFile = "";

        int start = 510;
        int len = 10;
        
        String tenantName = "";
        String projectName = "";
        //TokenGroupType tgType = null; // define TOKENGROUP_TENANT or TOKENGROUP_SYSTEMBUCKET
        
        RightsManager manager = new RightsManager(routerURL, appId, appKey);
        try (InputStream is = new FileInputStream(inputFile)) {
            try (OutputStream os = new FileOutputStream(outputFile)) {
                //If header is passed as null then decryptPartial will parse it automatically but the preceding skip calls would not be required.
                manager.decryptPartial(is, os, null, start, len, tenantName, projectName);
                //manager.decryptPartial(is, os, null, start, len, tenantName, tgType);

                //If caller does not know the tenantName/projectName/tgType and prefers to use the tokenGroupName in the NXL 
                //file header which must be valid/non-null, then may call the following methods which will parse the header themselves
                manager.decryptPartial(is, os, header, start, len);
            }
        }
        System.out.println("Partial Decryption Completed");
    }
}
