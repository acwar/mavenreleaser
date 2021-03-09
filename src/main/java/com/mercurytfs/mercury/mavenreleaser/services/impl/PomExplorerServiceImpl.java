package com.mercurytfs.mercury.mavenreleaser.services.impl;

import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtefactResult;
import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtifact;
import com.mercurytfs.mercury.mavenreleaser.dto.ArtifactVersion;
import com.mercurytfs.mercury.mavenreleaser.enums.ReleaseAction;
import com.mercurytfs.mercury.mavenreleaser.exception.ReleaserException;
import com.mercurytfs.mercury.mavenreleaser.helpers.ArtifactoryHelper;
import com.mercurytfs.mercury.mavenreleaser.services.MavenService;
import com.mercurytfs.mercury.mavenreleaser.services.PomExplorerService;
import com.mercurytfs.mercury.mavenreleaser.services.SCMMediator;
import lombok.Getter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;

/**
 *
  ____                 _____            _
 |  _ \ ___  _ __ ___ | ____|_  ___ __ | | ___  _ __ ___ _ __
 | |_) / _ \| '_ ` _ \|  _| \ \/ / '_ \| |/ _ \| '__/ _ \ '__|
 |  __/ (_) | | | | | | |___ >  <| |_) | | (_) | | |  __/ |
 |_|   \___/|_| |_| |_|_____/_/\_\ .__/|_|\___/|_|  \___|_|
  ____                  _        |_|
 / ___|  ___ _ ____   _(_) ___ ___
 \___ \ / _ \ '__\ \ / / |/ __/ _ \
  ___) |  __/ |   \ V /| | (_|  __/
 |____/ \___|_|    \_/ |_|\___\___|

 */
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
    private final Logger log = LoggerFactory.getLogger(PomExplorerServiceImpl.class);

    private ReleaseArtifact releaseArtifact;

    ReleaseArtefactResult processedSnapshots = new ReleaseArtefactResult();

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
        result.addAll(downloadAndProcess(releaseArtifact.getUrl(), releaseArtifact.getArtefactName()));
        return result.addAll(processedSnapshots);
    }
    @Override
    public ReleaseArtefactResult downloadAndProcess(String url, String artefactName) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        final String path = tempDir + artefactName + "-" + System.currentTimeMillis();
        ReleaseArtefactResult result = new ReleaseArtefactResult();
        log.info("--> ######## Processing Started for Artefact " + artefactName);

        scmMediator.downloadProject(url, new File(path));

        String parentFile = path + POM_XML_LITERAL;
        result.addAll(processPom(parentFile,parentFile));

        if (isRelease()) {
            log.info("Maven Release for " + getArtifactInfo(path + POM_XML_LITERAL));
            mavenService.invokeReleaser(path + POM_XML_LITERAL,releaseArtifact.getUsername(),releaseArtifact.getPassword(), result,releaseArtifact.getDependencyNextVersion(artefactName));
        }

        log.info("<-- ######## Processing Finished for Artefact " + artefactName);
        return result;
    }

    private ReleaseArtefactResult processPom(String file, String parentFile) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        log.debug(PROCESSING_POM_INPUT + file);
        ReleaseArtefactResult result = new ReleaseArtefactResult();
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        final Model model = mavenreader.read(new FileReader(pomfile));
        if (!model.getVersion().contains(SNAPSHOT_LITERAL)) {
            log.debug("The artifact " + model.getGroupId() + "." + model.getArtifactId() + "-" + model.getVersion() + " is already a release");
            return result;
        }

        if ("pom".equals(model.getPackaging())) {
            log.debug("Proccessing a parent module");
            //1º If there are some modules, those have to be processed first
            for (String module : model.getModules()) {
                log.debug("Founded module " + module);
                if (!module.endsWith("/"))
                    module = module + "/";

                result.addAll(processPom(file.replace("pom.xml", "") + module + "pom.xml",parentFile));
            }
        }else {
            final List<Dependency> deps = model.getDependencies();
            log.debug(PROCESSING_DEPENDENCIES);
            String artefact;
            for (Dependency d : deps) {
                artefact = d.getGroupId() + "." + d.getArtifactId() + "." + d.getVersion();
                if (d.getVersion() != null && d.getVersion().endsWith("SNAPSHOT"))
                    if (processedSnapshots.getArtefacts().containsKey(artefact) && !isRelease())
                        log.debug("Artifact " + artefact + " already processed");
                    else
                        result.addAll(processSnapshotDependency(artefact, d, parentFile));
                else
                    log.debug(artefact + " is a release, skiping");
            }

            if (isRelease()) {
                writeModel(pomfile, model);
                scmMediator.commitFile(extractSCMUrl(model.getScm()), new File(parentFile));
            }
        }
        log.debug(PROCESSING_POM_OUTPUT + file);
        return  result;
    }
    private ReleaseArtefactResult processSnapshotDependency(String artefact, Dependency dependency,String parentFile) throws IOException, XmlPullParserException, ReleaserException, MavenInvocationException {

        Model pom;
        ReleaseArtefactResult result = new ReleaseArtefactResult();

        log.debug(artefact + " is in SNAPSHOT, processing...");
        log.debug("\tCheck in artifactoy if the release version of " + artefact + ALREADY_EXISTS);
        if (artifactoryHelper.getReleasedArtifactFromArtifactory(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion().substring(0, dependency.getVersion().indexOf(SNAPSHOT_LITERAL))) != null) {
            log.debug(artefact + "\t in snapshot in the pom.xml but is already released");
            dependency.setVersion(dependency.getVersion().substring(0, dependency.getVersion().indexOf(SNAPSHOT_LITERAL)));
            if (!result.getArtefactsAlreadyReleased().containsKey(artefact))
                result.getArtefactsAlreadyReleased().put(artefact, artefact);
            else
                log.warn(ARTEFACT_IS_ALREADY_IN_THE_MAP + artefact);
            processedSnapshots.addAll(result);
            return result;
        }
        log.debug("\tArtifact release not found in artifactory");
        if ((pom = artifactoryHelper.getSnapshotArtifactFromArtifactory(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())) != null && !isAlreadyProcessing(pom, parentFile)) {
            downloadAndProcess(extractSCMUrl(pom.getScm()), String.valueOf(dependency.getArtifactId()));
            dependency.setVersion(dependency.getVersion().substring(0, dependency.getVersion().indexOf(SNAPSHOT_LITERAL)));
            if (!result.getArtefacts().containsKey(artefact)) {
                result.getArtefactsVersions().put(artefact,new ArtifactVersion(pom.getGroupId(), pom.getArtifactId(), pom.getVersion(),extractSCMUrl(pom.getScm())));
                result.getArtefacts().put(artefact, artefact);
            }
            else
                log.warn(ARTEFACT_IS_ALREADY_IN_THE_MAP + artefact);
        } else {
            log.error("Artifact not found at repository or already processing");
            if (!result.getArtefactsNotInArtifactory().containsKey(artefact))
                result.getArtefactsNotInArtifactory().put(artefact, artefact);
            else
                log.warn(ARTEFACT_IS_ALREADY_IN_THE_MAP + artefact);
        }
        processedSnapshots.addAll(result);
        return result;
    }

    private boolean isAlreadyProcessing(Model pom, String parentFile) throws ReleaserException {
        try {
            MavenXpp3Reader mavenreader = new MavenXpp3Reader();
            Model model = mavenreader.read(new FileReader(new File(parentFile)));
            log.debug("Processing " + extractSCMUrl(pom.getScm()) + " inside " + extractSCMUrl(model.getScm()) + " context");
            return extractSCMUrl(pom.getScm()).equals(extractSCMUrl(model.getScm()));
        } catch (IOException e) {
            log.error("Unable to check if already processing. Loop can be ocurring");
        } catch (XmlPullParserException e) {
            log.error("Unable to check if already processing. Loop can be ocurring");
        }
        return false;

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
    public static void writeModel(final File pomFile, final Model model) throws IOException {
        try (Writer writer = new FileWriter(pomFile)){
            final MavenXpp3Writer pomWriter = new MavenXpp3Writer();
            pomWriter.write(writer, model);
        }
    }
    private boolean isRelease(){
        return releaseArtifact.getReleaseAction() == ReleaseAction.RELEASE;
    }
}
