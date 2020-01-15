package org.agrsw.mavenreleaser.helpers;

import lombok.Getter;
import lombok.Setter;
import org.agrsw.mavenreleaser.beans.ReleaseArtifact;
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
    

    @Setter @Getter
    private ReleaseArtifact releaseArtifact;

    public Model getReleasedArtifactFromArtifactory(final String groupId, final String artifactId, final String version) throws IOException, XmlPullParserException {
        return getArtifactFromArtifactory(groupId, artifactId, version, true);
    }
    public Model getSnapshotArtifactFromArtifactory(final String groupId, final String artifactId, final String version) throws IOException, XmlPullParserException {
        return getArtifactFromArtifactory(groupId, artifactId, version, false);
    }

    private Model getArtifactFromArtifactory(final String groupId, final String artifactId, final String version, final boolean release) throws IOException, XmlPullParserException {
        log.info("\tSearching artifact in artifactory");
        Model model = null;
        final Artifactory artifactory = ArtifactoryClient.create("http://192.168.10.2:8081/artifactory/", getReleaseArtifact().getUsername(), getReleaseArtifact().getPassword());
        final String repoSnapshot1 = "libs-snapshot-local";
        final String repoSnapshot2 = "libs-snapshot-santander";
        final String repoRelease1 = "libs-release-santander";
        final String repoRelease2 = "libs-release-local";
        String repo1;
        String repo2;
        if (release) {
            repo1 = repoRelease1;
            repo2 = repoRelease2;
        } else {
            repo1 = repoSnapshot1;
            repo2 = repoSnapshot2;
        }
        final List<RepoPath> results = (List<RepoPath>) artifactory.searches().artifactsByGavc().groupId(groupId).artifactId(artifactId).version(version).repositories(new String[]{repo1, repo2}).doSearch();
        String itemPath = "";
        InputStream iStream = null;
        if (results != null) {
            for (final RepoPath searchItem : results) {
                itemPath = searchItem.getItemPath();
                if (itemPath.endsWith(".pom")) {
                    log.debug("\tPom found");
                    iStream = artifactory.repository(searchItem.getRepoKey()).download(itemPath).doDownload();
                    final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
                    model = mavenreader.read(iStream);
                }
            }
        }
        if (model == null) {
            log.debug("\tPom not found in artifactory");
        }
        log.info("\tSearching artifact in artifactory");
        return model;
    }

    public InputStream getArtifactSourceArtifactory(final String groupId, final String artifactId, final String version, final boolean release) throws IOException, XmlPullParserException {
        log.info("-->Searching artifact in artifactory");
        final Artifactory artifactory = ArtifactoryClient.create("http://192.168.10.2:8081/artifactory/", getReleaseArtifact().getUsername(), getReleaseArtifact().getPassword());
        final String repoSnapshot1 = "libs-snapshot-local";
        final String repoSnapshot2 = "libs-snapshot-santander";
        final String repoRelease1 = "libs-release-santander";
        final String repoRelease2 = "libs-release-local";
        String repo1;
        String repo2;
        if (release) {
            repo1 = repoRelease1;
            repo2 = repoRelease2;
        } else {
            repo1 = repoSnapshot1;
            repo2 = repoSnapshot2;
        }
        final List<RepoPath> results = (List<RepoPath>) artifactory.searches().artifactsByGavc().groupId(groupId).artifactId(artifactId).version(version).repositories(new String[]{repo1, repo2}).doSearch();
        String itemPath = "";
        InputStream iStream = null;
        if (results != null) {
            for (final RepoPath searchItem : results) {
                itemPath = searchItem.getItemPath();
                if (itemPath.endsWith("sources.jar") || itemPath.endsWith(".war")) {
                    log.debug("Source found");
                    iStream = artifactory.repository(searchItem.getRepoKey()).download(itemPath).doDownload();
                }
            }
        }
        if (iStream == null) {
            log.debug("Source not found in artifactory");
        }
        log.info("<--Searching artifact in artifactory");
        return iStream;
    }
}