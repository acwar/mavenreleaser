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

    public ArtifactVersion findByArtifactId(String artifacId){
        if (artifacId == null || artifacId.isEmpty())
            return null;
        if (artifactVersions == null || artifactVersions.isEmpty())
            return null;

        for (ArtifactVersion artifact:artifactVersions) {
            if (artifact.getArtifactId().equals(artifacId))
                    return artifact;
        }
        return null;
    }
}