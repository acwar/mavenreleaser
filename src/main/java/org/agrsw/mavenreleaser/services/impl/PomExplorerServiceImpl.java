package org.agrsw.mavenreleaser.services.impl;

import lombok.Getter;
import org.agrsw.mavenreleaser.beans.ReleaseArtefactResult;
import org.agrsw.mavenreleaser.beans.ReleaseArtifact;
import org.agrsw.mavenreleaser.enums.ReleaseAction;
import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.helpers.ArtifactoryHelper;
import org.agrsw.mavenreleaser.services.MavenService;
import org.agrsw.mavenreleaser.services.PomExplorerService;
import org.agrsw.mavenreleaser.services.SCMMediator;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@Service
public class PomExplorerServiceImpl implements PomExplorerService {
    public static final String PROCESSING_DEPENDENCIES = "Processing dependencies...";
    public static final String PROCESSING_POM_INPUT = "-->Processing Pom ";
    public static final String PROCESSING_POM_OUTPUT = "<--Processing Pom ";
    public static final String ARTEFACT_IS_ALREADY_IN_THE_MAP = "\tArtefact is already in the map ";
    public static final String ALREADY_EXISTS = " already exists";
    public static final String SNAPSHOT_LITERAL = "-SNAPSHOT";
    public static final String POM_XML_LITERAL = "/pom.xml";

    @Autowired
    private ArtifactoryHelper artifactoryHelper;
    @Autowired
    private SCMMediator scmMediator;
    @Autowired
    private MavenService mavenService;

    @Value("${tempDir:/tmp/svn/}")
    private String tempDir;

    @Getter
    private Logger log = LoggerFactory.getLogger(PomExplorerServiceImpl.class);

    private ReleaseArtifact releaseArtifact;

    @Override
    public PomExplorerService configure(ReleaseArtifact actifact){
        releaseArtifact = actifact;
        artifactoryHelper.setReleaseArtifact(releaseArtifact);
        scmMediator.setReleaseArtifact(releaseArtifact);
        return this;
    }

    @Override
    public ReleaseArtefactResult launch() throws MavenInvocationException, XmlPullParserException, ReleaserException, IOException {
        ReleaseArtefactResult result = new ReleaseArtefactResult();
        downloadAndProcess(releaseArtifact.getUrl(), releaseArtifact.getArtefactName() + "-" + System.currentTimeMillis());
        return result;
    }
    @Override
    public ReleaseArtefactResult downloadAndProcess(String url, String artefactName) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        final String path = tempDir + artefactName;
        ReleaseArtefactResult result = new ReleaseArtefactResult();
        log.info("--> ######## Processing Started for Artefact " + artefactName);
        scmMediator.downloadProject(url, new File(path));
        result.addAll(processPom(path + POM_XML_LITERAL));

        if (isRelease()) {
            log.info("Maven Release for " + getArtifactInfo(path + POM_XML_LITERAL));
            mavenService.invokeReleaser(path + POM_XML_LITERAL,releaseArtifact.getUsername(),releaseArtifact.getPassword());
        }

        log.info("<-- ######## Processing Finished for Artefact " + artefactName);
        return result;
    }

    private ReleaseArtefactResult processPom(String file) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        log.debug(PROCESSING_POM_INPUT + file);
        ReleaseArtefactResult result = new ReleaseArtefactResult();
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        final Model model = mavenreader.read(new FileReader(pomfile));
        if (!model.getVersion().contains(SNAPSHOT_LITERAL)) {
            log.debug("The artifact " + model.getGroupId() + "." + model.getArtifactId() + "-" + model.getVersion() + " is already a release");
            return result;
        }
        final List<Dependency> deps = model.getDependencies();
        log.debug(PROCESSING_DEPENDENCIES);
        String artefact;
        for (Dependency d : deps) {
            artefact = d.getGroupId() + "." + d.getArtifactId() + "." + d.getVersion();
            if (d.getVersion()!=null && d.getVersion().endsWith("SNAPSHOT"))
                result.addAll(processSnapshotDependency(artefact, d));
            else
                log.debug(artefact + " is a release, skiping");
        }
        log.debug(PROCESSING_POM_OUTPUT + file);
        return  result;
    }
    private ReleaseArtefactResult processSnapshotDependency(String artefact, Dependency d) throws IOException, XmlPullParserException, ReleaserException, MavenInvocationException {
        Model pom;
        ReleaseArtefactResult result = new ReleaseArtefactResult();

        log.debug(artefact + " is in SNAPSHOT, processing...");
        log.debug("\tCheck in artifactoy if the release version of " + artefact + ALREADY_EXISTS);
        if (artifactoryHelper.getReleasedArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion().substring(0, d.getVersion().indexOf(SNAPSHOT_LITERAL))) != null) {
            log.debug(artefact + "\t in snapshot in the pom.xml but is already released");
            d.setVersion(d.getVersion().substring(0, d.getVersion().indexOf(SNAPSHOT_LITERAL)));
            if (!result.getArtefactsAlreadyReleased().containsKey(artefact))
                result.getArtefactsAlreadyReleased().put(artefact, artefact);
            else
                log.warn(ARTEFACT_IS_ALREADY_IN_THE_MAP + artefact);
            return result;
        }
        log.debug("\tArtifact release not found in artifactory");
        if ((pom = artifactoryHelper.getSnapshotArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion())) != null) {
            downloadAndProcess(extractSCMUrl(pom.getScm()), String.valueOf(d.getArtifactId()) + System.currentTimeMillis());
            d.setVersion(d.getVersion().substring(0, d.getVersion().indexOf(SNAPSHOT_LITERAL)));
            if (!result.getArtefacts().containsKey(artefact))
                result.getArtefacts().put(artefact, artefact);
            else
                log.warn(ARTEFACT_IS_ALREADY_IN_THE_MAP + artefact);
        } else {
            log.error("Artifact not found at repository");
            if (!result.getArtefactsNotInArtifactory().containsKey(artefact))
                result.getArtefactsNotInArtifactory().put(artefact, artefact);
            else
                log.warn(ARTEFACT_IS_ALREADY_IN_THE_MAP + artefact);
        }
        return  result;
    }

    private String extractSCMUrl(Scm scm) throws ReleaserException {
        String svnURL = (scm.getDeveloperConnection()!=null)?scm.getDeveloperConnection():scm.getConnection();
        if (svnURL==null)
            throw new ReleaserException("SCM Info not provided in POM");

        return svnURL.substring(svnURL.indexOf("http"));
    }

    private String getArtifactInfo(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        String artefactInfo = "";
        try {
            final Model model = mavenreader.read(new FileReader(pomfile));
            artefactInfo = model.getGroupId() + "." + model.getArtifactId() + "-" + model.getVersion();
        } catch (IOException | XmlPullParserException ex2) {

            log.error(ex2.toString());
        }
        return artefactInfo;
    }

    private boolean isRelease(){
        return releaseArtifact.getReleaseAction() == ReleaseAction.RELEASE;
    }
}
