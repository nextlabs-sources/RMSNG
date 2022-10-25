package com.nextlabs.rms.share.factory;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.rms.share.ShareStrategy;
import com.nextlabs.rms.share.ShareToPerson;
import com.nextlabs.rms.share.ShareToProject;
import com.nextlabs.rms.share.ShareToTenant;
import com.nextlabs.rms.shared.Nvl;

public final class ShareStrategyFactory {

    private static final ShareStrategyFactory INSTANCE = new ShareStrategyFactory();

    private ShareStrategyFactory() {

    }

    public static ShareStrategyFactory getInstance() {
        return INSTANCE;
    }

    public ShareStrategy create(Constants.SHARESPACE fromSpace, Constants.SHARESPACE toSpace) {
        ShareStrategy strategy;
        switch (Nvl.nvl(fromSpace, Constants.SHARESPACE.MYSPACE)) {
            case PROJECTSPACE:
                strategy = new ShareToProject();
                break;
            case ENTERPRISESPACE:
                strategy = new ShareToTenant();
                break;
            default:
                strategy = new ShareToPerson();
        }
        return strategy;
    }

}
