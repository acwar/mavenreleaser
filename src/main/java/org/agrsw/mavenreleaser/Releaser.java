package org.agrsw.mavenreleaser;

import lombok.Getter;
import org.agrsw.mavenreleaser.beans.ReleaseArtefactResult;
import org.agrsw.mavenreleaser.beans.ReleaseArtifact;
import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.helpers.ConsoleHelper;
import org.agrsw.mavenreleaser.services.PomExplorerService;
import org.apache.commons.cli.*;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;

import java.io.IOException;
import java.util.Collection;


@SpringBootApplication(exclude = {EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class})
public class Releaser implements CommandLineRunner {

    public static final String ARTEFACTOS_ENCONTRADOS = "######################## Artefactos encontrados:  #############################################################";
    public static final String ARTEFACTOS_QUE_YA_ESTABAN_RELESEADOS = "######################## Artefactos que ya estaban releseados:  ###############################################";
    public static final String ARTEFACTOS_QUE_NO_ESTAN_EN_ARTIFACTORY_HACER_CLEAN_DEPLOY = "######################## Artefactos que no est\u00e1n en Artifactory (hacer clean deploy):  ###################";
    public static final String ARTEFACTOS_JIRAS_QUE_SE_PROCESARON_CORRECTAMENTE = "######################## Artefactos jiras que se procesaron correctamente  ####################################";
    public static final String ARTEFACTOS_JIRAS_QUE_NO_SE_PROCESARON_CORRECTAMENTE = "######################## Artefactos jiras que NO se procesaron correctamente  #################################";

    @Getter
    private Logger log = LoggerFactory.getLogger(Releaser.class);

    @Autowired
    private PomExplorerService pomExplorer;

    @Value("${maven.home}")
    private String mavenHome;

    @Value("${notcheck.token}")
    private String notCheckToken;

    @Getter
    private ReleaseArtifact releaseArtifact;

    public static void main(final String[] args) {
        SpringApplication.run(Releaser.class, args);
    }

    @Override
    public void run(final String... args) {
        log.debug("Start Releasing..");
        log.debug("Maven Home: " + mavenHome);
        log.debug("NotCheck Token: " + notCheckToken);
        final Options options = ConsoleHelper.configureArgsExtractor();
        try {
            final CommandLine cmd = new DefaultParser().parse(options, args);
            releaseArtifact = ConsoleHelper.interpretReleaseArtifact(cmd);
            log.debug(cmd.toString());
        } catch (ParseException e1) {
            log.debug(e1.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar nombrejar.jar", options);
            System.exit(-1);
        }

        ReleaseArtefactResult result;
        try {
            switch (releaseArtifact.getReleaseAction()) {
                case RELEASE:
                    result = pomExplorer.configure(releaseArtifact).launch();
                    logArtifacts(ARTEFACTOS_ENCONTRADOS, result.getArtefacts().keySet());
                    logArtifacts(ARTEFACTOS_QUE_YA_ESTABAN_RELESEADOS, result.getArtefactsAlreadyReleased().keySet());
                    logArtifacts(ARTEFACTOS_QUE_NO_ESTAN_EN_ARTIFACTORY_HACER_CLEAN_DEPLOY, result.getArtefactsNotInArtifactory().keySet());
                    logArtifacts(ARTEFACTOS_JIRAS_QUE_SE_PROCESARON_CORRECTAMENTE, result.getJirasReleased().keySet());
                    logArtifacts(ARTEFACTOS_JIRAS_QUE_NO_SE_PROCESARON_CORRECTAMENTE, result.getJirasNotReleased().keySet());
                    break;
                case PREPARE:
                case SOURCES:
                    result = pomExplorer.configure(releaseArtifact).launch();
                    logArtifacts(ARTEFACTOS_ENCONTRADOS, result.getArtefacts().keySet());
                    break;
                default:
                    log.info("No compatible action provided. User release/prepare/sources");
            }
        } catch (IOException | XmlPullParserException | MavenInvocationException | ReleaserException e1) {
            log.debug(e1.getMessage());
            e1.printStackTrace();
        }
    }
    private void logArtifacts(String mensaje,Collection<String> values){
        log.info(mensaje);
        for (String artefact : values)
            log.info(artefact);
    }


//
//    private void doRelease(final String url, final String artefactName) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
//        final String path = tempDir + artefactName;
//        log.info("--> #################### Release Started for Artefact " + artefactName);
//        scmMediator.downloadProject(url, new File(path));
//        log.info("Artefact Info :" + getArtifactInfo(path + POM_XML_LITERAL));
//        processPomRelease(path + POM_XML_LITERAL);
//        log.info("Maven Release for " + getArtifactInfo(path + POM_XML_LITERAL));
//        mavenInvoker(path + POM_XML_LITERAL);
//        log.info("<-- #################### Release Finished for Artefact " + artefactName);
//    }
//
//    private void doSources(final String url, final String artefactName) throws IOException, XmlPullParserException, ReleaserException {
//        final String path = tempDir + artefactName;
//        log.info("--> ######## Prepare Started for Artefact " + artefactName);
//        scmMediator.downloadProject(url, new File(path));
//        processPomSources(path + POM_XML_LITERAL);
//        log.info("<-- ######## Prepare Finished for Artefact " + artefactName);
//    }
//
//    private void saveToFile(final InputStream is, final String name) {
//        final File targetFile = new File(name);
//        try (
//           OutputStream outStream = new FileOutputStream(targetFile)
//        ){
//            final byte[] buffer = new byte[is.available()];
//            while (is.read(buffer) > 0)
//                outStream.write(buffer);
//        } catch (IOException e) {
//            log.error(e.toString());
//        }
//
//    }
//
//    private void doPrepare(String url, String artefactName) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
//        final String path = tempDir + artefactName;
//        log.info("--> ######## Prepare Started for Artefact " + artefactName);
//        scmMediator.downloadProject(url, new File(path));
//        processPomPrepare(path + POM_XML_LITERAL);
//        log.info("<-- ######## Prepare Finished for Artefact " + artefactName);
//    }
//
//
//    private void processPomSources(final String file) throws IOException, XmlPullParserException {
//        log.debug("Processin Pom " + file);
//        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
//        final File pomfile = new File(file);
//        final Model model = mavenreader.read(new FileReader(pomfile));
//        final List<Dependency> deps = model.getDependencies();
//        log.debug(PROCESSING_DEPENDENCIES);
//        String artefact;
//        for (final Dependency d : deps) {
//            artefact = d.getGroupId() + "." + d.getArtifactId() + "." + d.getVersion();
//            log.debug(d.getGroupId() + "." + d.getArtifactId() + "." + d.getVersion());
//            if (d.getGroupId().startsWith("com.mercury")) {
//                final InputStream is = artifactoryHelper.getArtifactSourceArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion(), true);
//                if (is == null) {
//                    continue;
//                }
//                log.debug("Artifact source  found at artifactory");
//                if (!Releaser.artefacts.containsKey(artefact)) {
//                    Releaser.artefacts.put(artefact, artefact);
//                    saveToFile(is, "/tmp/sources/" + d.getGroupId() + "-" + d.getArtifactId() + "-" + d.getVersion() + ".jar");
//                } else {
//                    log.warn("Artefact is already in the map " + artefact);
//                }
//            } else {
//                log.debug("The artifact does not belong to mercury");
//            }
//        }
//    }

}
