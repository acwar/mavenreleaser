package com.mercurytfs.mercury.mavenreleaser.services;

import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtefactResult;
import com.mercurytfs.mercury.mavenreleaser.dto.ArtifactVersion;
import org.apache.maven.shared.invoker.MavenInvocationException;

public interface MavenService {
    MavenService configureDryRun();

    MavenService configureRealRun();

    void invokeReleaser(String pom, String user, String pass, ReleaseArtefactResult releaseArtefactResult, ArtifactVersion dependencyNextVersion) throws MavenInvocationException;
}
