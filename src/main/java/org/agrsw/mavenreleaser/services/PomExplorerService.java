package org.agrsw.mavenreleaser.services;

import org.agrsw.mavenreleaser.beans.ReleaseArtefactResult;
import org.agrsw.mavenreleaser.beans.ReleaseArtifact;
import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;

public interface PomExplorerService {
    PomExplorerService configure(ReleaseArtifact actifact);

    ReleaseArtefactResult launch() throws MavenInvocationException, XmlPullParserException, ReleaserException, IOException;

    ReleaseArtefactResult downloadAndProcess(String url, String artefactName) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException;
}
