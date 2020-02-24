package com.mercurytfs.mercury.mavenreleaser.services;

import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtifact;
import com.mercurytfs.mercury.mavenreleaser.exception.ReleaserException;

import java.io.File;

public interface SCMMediator {

    boolean downloadProject(String url, File target) throws ReleaserException;

    boolean commitFile(String url, File target) throws ReleaserException;

    void setReleaseArtifact(ReleaseArtifact releaseArtifact);
}
