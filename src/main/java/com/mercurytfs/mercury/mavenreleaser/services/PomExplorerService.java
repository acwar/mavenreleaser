package com.mercurytfs.mercury.mavenreleaser.services;

import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtifact;
import com.mercurytfs.mercury.mavenreleaser.exception.ReleaserException;
import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtefactResult;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;

public interface PomExplorerService {
    PomExplorerService configure(ReleaseArtifact actifact);

    ReleaseArtefactResult launch() throws MavenInvocationException, XmlPullParserException, ReleaserException, IOException;

    ReleaseArtefactResult downloadAndProcess(String url, String artefactName) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException;
}
