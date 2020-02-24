package com.mercurytfs.mercury.mavenreleaser;

import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtifact;
import com.mercurytfs.mercury.mavenreleaser.exception.ReleaserException;
import lombok.Getter;
import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtefactResult;
import com.mercurytfs.mercury.mavenreleaser.helpers.ConsoleHelper;
import com.mercurytfs.mercury.mavenreleaser.services.PomExplorerService;
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

}
