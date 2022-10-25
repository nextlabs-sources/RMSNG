package com.nextlabs.rms.share.factory;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.rms.share.IShareMapper;
import com.nextlabs.rms.share.ShareEWSMapper;
import com.nextlabs.rms.share.SharePersonalMapper;
import com.nextlabs.rms.share.ShareProjectMapper;
import com.nextlabs.rms.shared.Nvl;

public final class ShareMapperFactory {

    private static final ShareMapperFactory INSTANCE = new ShareMapperFactory();

    private ShareMapperFactory() {

    }

    public static ShareMapperFactory getInstance() {
        return INSTANCE;
    }

    public IShareMapper create(Constants.SHARESPACE fromSpace, Constants.SHARESPACE toSpace) {
        IShareMapper mapper;
        switch (Nvl.nvl(fromSpace, Constants.SHARESPACE.MYSPACE)) {
            case PROJECTSPACE:
                mapper = new ShareProjectMapper();
                break;
            case ENTERPRISESPACE:
                mapper = new ShareEWSMapper();
                break;
            default:
                mapper = new SharePersonalMapper();
                break;
        }
        return mapper;
    }
}
