package com.mercurytfs.mercury.mavenreleaser.helpers;

import lombok.Getter;
import lombok.Setter;
import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtifact;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClient;
import org.jfrog.artifactory.client.model.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class ArtifactoryHelper {
    private Logger log = LoggerFactory.getLogger( this.getClass());

    private final String repoSnapshot1 = "libs-snapshot-local";
    private final String repoSnapshot2 = "libs-snapshot-santander";
    private final String repoRelease1 = "libs-release-santander";
    private final String repoRelease2 = "libs-release-local";


    @Setter @Getter
    private ReleaseArtifact releaseArtifact;

    public Model getReleasedArtifactFromArtifactory(final String groupId, final String artifactId, final String version) throws IOException, XmlPullParserException {
        return getArtifactFromArtifactory(groupId, artifactId, version, true);
    }
    public Model getSnapshotArtifactFromArtifactory(final String groupId, final String artifactId, final String version) throws IOException, XmlPullParserException {
        return getArtifactFromArtifactory(groupId, artifactId, version, false);
    }

    private Model getArtifactFromArtifactory(final String groupId, final String artifactId, final String version, final boolean release) throws IOException, XmlPullParserException {
        Model model = null;
        Artifactory artifactory = getArtifactoryClient();
        final List<RepoPath> results = getResults(groupId, artifactId, version, artifactory, (release) ? repoRelease1 : repoSnapshot1, (release) ? repoRelease2 : repoSnapshot2);
        String itemPath = "";
        InputStream iStream = null;
        if (results != null) {
            for (final RepoPath searchItem : results) {
                itemPath = searchItem.getItemPath();
                if (itemPath.endsWith(".pom")) {
                    log.debug("\tPom found: " + itemPath);
                    iStream = artifactory.repository(searchItem.getRepoKey()).download(itemPath).doDownload();
                    final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
                    model = mavenreader.read(iStream);
                }
            }
        }
        if (model == null) {
            log.debug("\tPom not found in artifactory");
        }
        return model;
    }

    private Artifactory getArtifactoryClient() {
        return ArtifactoryClient.create("http://192.168.10.2:8081/artifactory/", getReleaseArtifact().getUsername(), getReleaseArtifact().getPassword());
    }

    public InputStream getArtifactSourceArtifactory(final String groupId, final String artifactId, final String version, final boolean release) throws IOException, XmlPullParserException {
        Artifactory artifactory = getArtifactoryClient();

        final List<RepoPath> results = getResults(groupId, artifactId, version, artifactory, (release) ? repoRelease1 : repoSnapshot1, (release) ? repoRelease2 : repoSnapshot2);
        String itemPath = "";
        InputStream iStream = null;
        if (results != null) {
            for (final RepoPath searchItem : results) {
                itemPath = searchItem.getItemPath();
                if (itemPath.endsWith("sources.jar") || itemPath.endsWith(".war")) {
                    log.debug("\tSource found");
                    iStream = artifactory.repository(searchItem.getRepoKey()).download(itemPath).doDownload();
                }
            }
        }
        if (iStream == null) {
            log.debug("\tSource not found in artifactory");
        }
        return iStream;
    }

    private List<RepoPath> getResults(String groupId, String artifactId, String version, Artifactory artifactory, String s, String s2) {
        return (List<RepoPath>) artifactory.searches().artifactsByGavc().groupId(groupId).artifactId(artifactId).version(version).repositories(new String[]{s, s2}).doSearch();
    }
}