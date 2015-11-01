package com.baracuda.piepet.nexusrelease;

import hudson.model.AbstractBuild;
import hudson.model.Action;

/**
 *
 */
public class NexusReleaseBuildAction implements Action {

    private String message;
    private AbstractBuild<?, ?> build;

    @Override
    public String getIconFileName() {
        return "/plugin/nexus-release/img/build-goals.png";
    }

    @Override
    public String getDisplayName() {
        return "Test Example Build Page";
    }

    @Override
    public String getUrlName() {
        return "nexus-releaseBA";
    }

    public String getMessage() {
        return this.message;
    }

    public int getBuildNumber() {
        return this.build.number;
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }

    NexusReleaseBuildAction(final String message, final AbstractBuild<?, ?> build)
    {
        this.message = message;
        this.build = build;
    }
}
