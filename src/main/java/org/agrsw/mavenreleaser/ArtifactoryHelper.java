package org.agrsw.mavenreleaser;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClient;
import org.jfrog.artifactory.client.model.RepoPath;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ArtifactoryHelper {
    private final Releaser releaser;

    public ArtifactoryHelper(Releaser releaser) {
        this.releaser = releaser;
    }

    Model getArtifactFromArtifactory(final String groupId, final String artifactId, final String version, final boolean release) throws IOException, XmlPullParserException {
        releaser.getLog().info("-->Searching artifact in artifactory");
        Model model = null;
        final Artifactory artifactory = ArtifactoryClient.create("http://192.168.10.2:8081/artifactory/", releaser.getReleaseArtifact().getUsername(), releaser.getReleaseArtifact().getPassword());
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
                    releaser.getLog().debug("Pom found");
                    iStream = artifactory.repository(searchItem.getRepoKey()).download(itemPath).doDownload();
                    final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
                    model = mavenreader.read(iStream);
                }
            }
        }
        if (model == null) {
            releaser.getLog().debug("Pom not found in artifactory");
        }
        releaser.getLog().info("<--Searching artifact in artifactory");
        return model;
    }

    InputStream getArtifactSourceArtifactory(final String groupId, final String artifactId, final String version, final boolean release) throws IOException, XmlPullParserException {
        releaser.getLog().info("-->Searching artifact in artifactory");
        final Artifactory artifactory = ArtifactoryClient.create("http://192.168.10.2:8081/artifactory/", releaser.getReleaseArtifact().getUsername(), releaser.getReleaseArtifact().getPassword());
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
                    releaser.getLog().debug("Source found");
                    iStream = artifactory.repository(searchItem.getRepoKey()).download(itemPath).doDownload();
                }
            }
        }
        if (iStream == null) {
            releaser.getLog().debug("Source not found in artifactory");
        }
        releaser.getLog().info("<--Searching artifact in artifactory");
        return iStream;
    }
}