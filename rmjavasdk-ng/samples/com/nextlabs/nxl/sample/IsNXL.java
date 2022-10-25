package com.nextlabs.nxl.sample;

import com.nextlabs.nxl.Rights;
import com.nextlabs.nxl.RightsManager;

import java.util.HashMap;
import java.util.Map;

public class IsNXL {

    public static void main(String[] args) throws Exception {

        String routerURL = "https://{rms_router}/router";
        String appKey = "";
        int appId = 1;

        String inputFile = "";
        
        RightsManager manager = new RightsManager(routerURL, appId, appKey);
        Boolean isNxl = manager.isNXL(inputFile);
        if (isNxl) {
        	System.out.println("The input file " + inputFile + "is an NXL file.");
        } else {
        	System.out.println("The input file " + inputFile + "is not an NXL file.");
        }
    }
}
