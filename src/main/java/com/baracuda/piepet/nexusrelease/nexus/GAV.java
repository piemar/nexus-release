package com.baracuda.piepet.nexusrelease.nexus;

/**
 * Created by FPPE12 on 2015-11-01.
 */
public class GAV {
    private String groupId;
    private String artifactId;
    private String version;


    public GAV(String gavDescription) {
        if(gavDescription!=null) {
            String[] gavProperties = gavDescription.split(":");
            this.groupId = gavProperties[0];
            this.artifactId = gavProperties[1];
            this.version = gavProperties[2];

        }

    }
    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

}
