package com.mercurytfs.mercury.mavenreleaser.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

@XmlRootElement
public class ArtifactVersionsList{
    private Collection<ArtifactVersion> artifactVersions;


    public Collection<ArtifactVersion> getArtifactVersions() {
        return this.artifactVersions;
    }

    @XmlElement
    public void setArtifactVersions(Collection<ArtifactVersion> artifactVersions) {
        this.artifactVersions = artifactVersions;
    }
}