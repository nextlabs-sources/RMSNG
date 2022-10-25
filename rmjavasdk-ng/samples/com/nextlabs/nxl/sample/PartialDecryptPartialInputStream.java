package com.nextlabs.nxl.sample;

import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.RightsManager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class PartialDecryptPartialInputStream {

    public static void main(String[] args) throws Exception {

        String routerURL = "https://{rms_router}/router";
        String appKey = "";
        int appId = 1;

        String inputFile = "";
        String outputFile = "";

        int start = 510;
        int len = 10;

        RightsManager manager = new RightsManager(routerURL, appId, appKey);
        
        String tenantName = "";
        String projectName = "";
        //TokenGroupType tgType = null; // define TOKENGROUP_TENANT or TOKENGROUP_SYSTEMBUCKET

        // Build NXL header and store it to reuse it for future partial decryptions 
        byte[] buf = new byte[RightsManager.getHeaderSize()];
        try (InputStream is = new FileInputStream(inputFile)) {
            is.read(buf);
        }
        // SDK adds ability to prefetch/cache the token during buildNxlHeader in Guava cache for subsequent decryptPartial if token caching were enabled
        NxlFile header = manager.buildNxlHeader(buf, tenantName, projectName);
        //NxlFile header = manager.buildNxlHeader(buf, tenantName, tgType);
        
        //If caller does not know the tenantName/projectName/tgType and prefers to use the tokenGroupName
        //in the NXL file header then may call the following which will parse the header themselves
        //NxlFile header = manager.buildNxlHeader(inputFile);
        //NxlFile header = manager.buildNxlHeader(new File(inputFile));
        //NxlFile header = manager.buildNxlHeader(headerByteArray);

        try (InputStream is = new FileInputStream(inputFile)) {
            // Emulating the behavior where a client would pass the input stream without the NXL header and partially stripped content.
            is.skip(RightsManager.getHeaderSize());
            // The input stream provided by the client should start from multiples of RightsManager.getBlockSize(). (e.g. 0 or 1 * BLK_SIZE or 2 * BLK_SIZE)
            is.skip((start / RightsManager.getBlockSize()) * RightsManager.getBlockSize());

            try (OutputStream os = new FileOutputStream(outputFile)) {
                //If header is passed as null then decryptPartial will parse it automatically but the preceding skip calls would not be required.
                manager.decryptPartial(is, os, header, start, len, tenantName, projectName);
                //manager.decryptPartial(is, os, header, start, len, tenantName, tgType);

                //If caller does not know the tenantName/projectName/tgType and prefers to use the tokenGroupName in the NXL 
                //file header which must be valid/non-null, then may call the following methods which will parse the header themselves
                manager.decryptPartial(is, os, header, start, len);
            }
        }
        System.out.println("Partial Decryption Completed");
    }
}
