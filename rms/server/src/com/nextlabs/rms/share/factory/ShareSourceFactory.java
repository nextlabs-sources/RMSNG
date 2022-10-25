package com.nextlabs.rms.share.factory;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.share.ShareSource;
import com.nextlabs.rms.share.ShareSourceMySpace;
import com.nextlabs.rms.share.ShareSourceProject;
import com.nextlabs.rms.shared.Nvl;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

public final class ShareSourceFactory {

    private static final ShareSourceFactory INSTANCE = new ShareSourceFactory();

    private ShareSourceFactory() {

    }

    public static ShareSourceFactory getInstance() {
        return INSTANCE;
    }

    public ShareSource create(JsonSharing shareReq, RMSUserPrincipal principal, HttpServletRequest request)
            throws RMSException {
        ShareSource source;
        switch (Nvl.nvl(shareReq.getFromSpace(), Constants.SHARESPACE.MYSPACE)) {
            case PROJECTSPACE:
                source = new ShareSourceProject(shareReq, principal, request);
                break;
            default:
                source = new ShareSourceMySpace(shareReq, principal, request);
                break;
        }
        return source;
    }

    public ShareSource create(JsonSharing shareReq, File file, RMSUserPrincipal principal, HttpServletRequest request)
            throws RMSException {
        ShareSource source;
        switch (Nvl.nvl(shareReq.getFromSpace(), Constants.SHARESPACE.MYSPACE)) {
            default:
                source = new ShareSourceMySpace(shareReq, file, principal, request);
                break;
        }
        return source;
    }

    public ShareSource create(SharingTransaction sharingTransaction, RMSUserPrincipal principal,
        HttpServletRequest request) throws RMSException {
        ShareSource source;
        if (sharingTransaction == null) {
            throw new ValidateException(403, "Access denied - No sharing transaction");
        }
        switch (sharingTransaction.getFromSpace()) {
            case PROJECTSPACE:
                source = new ShareSourceProject(sharingTransaction, principal, request);
                break;
            default:
                source = new ShareSourceMySpace(sharingTransaction, principal, request);
                break;
        }
        return source;
    }
}
