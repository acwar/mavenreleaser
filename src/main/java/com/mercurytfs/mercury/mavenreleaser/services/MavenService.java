package com.mercurytfs.mercury.mavenreleaser.services;

import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtefactResult;
import org.apache.maven.shared.invoker.MavenInvocationException;

public interface MavenService {
    void invokeReleaser(String pom, String user, String pass, ReleaseArtefactResult releaseArtefactResult) throws MavenInvocationException;
}
