package org.agrsw.mavenreleaser.services;

import org.apache.maven.shared.invoker.MavenInvocationException;

public interface MavenService {
    void invokeReleaser(String pom,String user,String pass) throws MavenInvocationException;
}
