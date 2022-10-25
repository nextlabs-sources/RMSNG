package com.nextlabs.rms.services.application;

import com.nextlabs.rms.services.resource.AddRepoResource;
import com.nextlabs.rms.services.resource.CheckUpdatesResource;
import com.nextlabs.rms.services.resource.GetRepositoryDetailsResource;
import com.nextlabs.rms.services.resource.RegisterAgentResource;
import com.nextlabs.rms.services.resource.RemoveRepoResource;
import com.nextlabs.rms.services.resource.UpdateRepoResource;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class RMSApplication extends Application {

    public synchronized Restlet createInboundRoot() {
        Router router = new Router(getContext());
        router.attach("/AddRepoService", AddRepoResource.class);
        router.attach("/UpdateRepoService", UpdateRepoResource.class);
        router.attach("/RemoveRepoService", RemoveRepoResource.class);
        router.attach("/GetRepositoryDetailsService", GetRepositoryDetailsResource.class);
        router.attach("/CheckUpdates", CheckUpdatesResource.class);
        router.attach("/RegisterAgent", RegisterAgentResource.class);
        return router;
    }
}
