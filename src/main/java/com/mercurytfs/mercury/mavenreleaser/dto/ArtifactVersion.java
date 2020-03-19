package com.mercurytfs.mercury.mavenreleaser.dto;

import com.mercurytfs.mercury.mavenreleaser.helpers.NewVersionHelper;
import lombok.Getter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
     _         _   _  __            _ __     __            _
    / \   _ __| |_(_)/ _| __ _  ___| |\ \   / /__ _ __ ___(_) ___  _ __
   / _ \ | '__| __| | |_ / _` |/ __| __\ \ / / _ \ '__/ __| |/ _ \| '_ \
  / ___ \| |  | |_| |  _| (_| | (__| |_ \ V /  __/ |  \__ \ | (_) | | | |
 /_/   \_\_|   \__|_|_|  \__,_|\___|\__| \_/ \___|_|  |___/_|\___/|_| |_|

 * Setter not lomboked beacuse of Jaxb
 */
@XmlRootElement
public class ArtifactVersion implements Serializable {

    @Getter
    private String groupId;
    @Getter
    private String artifactId;
    @Getter
    private String currentVersion;
    @Getter
    private String nextVersion;
    @Getter
    private String scm;

    public ArtifactVersion(String groupId, String artifactId, String currentVersion, String scm) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.currentVersion = currentVersion;
        this.scm = scm;
        this.nextVersion = NewVersionHelper.getNextVersion(currentVersion,scm);
    }

    public ArtifactVersion() {
    }

    @XmlElement
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @XmlElement
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @XmlElement
    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    @XmlElement
    public void setNextVersion(String nextVersion) {
        this.nextVersion = nextVersion;
    }

    @XmlElement
    public void setScm(String scm) {
        this.scm = scm;
    }

    public String getPureNextVersion(String match){
        if (nextVersion.endsWith(match)) {
            final int snapshotPosition = nextVersion.indexOf(match);
            return nextVersion.substring(0, snapshotPosition);
        }
        return nextVersion;
    }
    public  String getPureCurrentVersion(String match){
        if (currentVersion.endsWith(match)) {
            final int snapshotPosition = currentVersion.indexOf(match);
            return currentVersion.substring(0, snapshotPosition);
        }
        return currentVersion;
    }
}
