package org.agrsw.mavenreleaser.helpers;

import org.agrsw.mavenreleaser.beans.ReleaseArtifact;
import org.agrsw.mavenreleaser.exception.ReleaserException;

import java.io.File;

public interface SCMMediator {

    boolean downloadProject(String url, File target) throws ReleaserException;
    void setReleaseArtifact(ReleaseArtifact releaseArtifact);
}
