package com.nextlabs.rms.viewer.conversion;

import com.nextlabs.rms.viewer.exception.RMSException;

public interface IFileConverter {

    public boolean convertFile(String inputPath, String destinationPath) throws RMSException;

}
