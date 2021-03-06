package com.mercurytfs.mercury.mavenreleaser.helpers;

import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtifact;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.model.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class ArtifactoryHelper {
    private final Logger log = LoggerFactory.getLogger( this.getClass());


    @Value("${repository.url:http://192.168.10.9:8082/artifactory/}")
    private String artifactoryURL;
    @Value("${repository.url.legacy:http://192.168.10.2:8081/artifactory/}")
    private String artifactoryLegacyURL;
    @Value("${useLegacy:false}")
    private String useLegacy;

    @Value("${repository.snapshot.main}")
    private String mainSnapshotsRepo;
    @Value("${repository.snapshot.trunk}")
    private String mainTrunkSnapshotsRepo;
    @Value("${repository.release.main}")
    private String mainReleasesRepo;
    private String[] snapshotsRepos;

    @Value("${repository.snapshot.project}")
    private String projectTrunkSnapshotRepo;
    @Value("${repository.release.project}")
    private String projectReleaseRepo;
    private String[] releaseRepos;


    @PostConstruct
    private void construct(){

        log.debug("Configured to use main repository:"+artifactoryURL);
        log.debug("Checking legacy repositories:"+useLegacy);
        if (Boolean.TRUE.toString().equals(useLegacy)){
            log.debug("Use of Legacy repositories active"+artifactoryLegacyURL);
        }else
            artifactoryLegacyURL = artifactoryURL;

        snapshotsRepos = new String[]{mainSnapshotsRepo, mainTrunkSnapshotsRepo, projectTrunkSnapshotRepo};
        releaseRepos = new String[]{mainReleasesRepo, projectReleaseRepo};
    }

    @Setter @Getter
    private ReleaseArtifact releaseArtifact;

    public Model getReleasedArtifactFromArtifactory(final String groupId, final String artifactId, final String version) throws IOException, XmlPullParserException {
        return getArtifactFromArtifactory(groupId, artifactId, version, true);
    }
    public Model getSnapshotArtifactFromArtifactory(final String groupId, final String artifactId, final String version) throws IOException, XmlPullParserException {
        return getArtifactFromArtifactory(groupId, artifactId, version, false);
    }

    private Model getArtifactFromArtifactory(final String groupId, final String artifactId, final String version, final boolean release) throws IOException, XmlPullParserException {
    	
    	log.debug("Using repositories: " + ((release) ? mainReleasesRepo + " and " + projectReleaseRepo : mainSnapshotsRepo + " and " + mainTrunkSnapshotsRepo));
    	
    	Model model = null;
        Artifactory artifactory = (release)?getArtifactoryLegacyClient():getArtifactoryClient();
        final List<RepoPath> results = getResults(groupId, artifactId, version, artifactory, release);
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
        return ArtifactoryClientBuilder.create()
                .setUrl(artifactoryURL)
                .setUsername(getReleaseArtifact().getUsername())
                .setPassword(getReleaseArtifact().getPassword())
                .build();
    }
    private Artifactory getArtifactoryLegacyClient() {
        return ArtifactoryClientBuilder.create()
                .setUrl(artifactoryLegacyURL)
                .setUsername(getReleaseArtifact().getUsername())
                .setPassword(getReleaseArtifact().getPassword())
                .build();
    }


    public InputStream getArtifactSourceArtifactory(final String groupId, final String artifactId, final String version, final boolean release) throws IOException {
        Artifactory artifactory = getArtifactoryClient();

        final List<RepoPath> results = getResults(groupId, artifactId, version, artifactory, release);
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

    private List<RepoPath> getResults(String groupId, String artifactId, String version, Artifactory artifactory, boolean release) {
        return artifactory.searches().artifactsByGavc().groupId(groupId).artifactId(artifactId).version(version).repositories((release) ? releaseRepos:snapshotsRepos).doSearch();
    }

    private List<RepoPath> getResults(String groupId, String artifactId, String version, Artifactory artifactory, String s, String s2) {
        return artifactory.searches().artifactsByGavc().groupId(groupId).artifactId(artifactId).version(version).repositories(s, s2).doSearch();
    }
    public ReleaseArtifact getReleaseArtifact() {
		return releaseArtifact;
	}
	public void setReleaseArtifact(ReleaseArtifact releaseArtifact) {
		this.releaseArtifact = releaseArtifact;
	}
}