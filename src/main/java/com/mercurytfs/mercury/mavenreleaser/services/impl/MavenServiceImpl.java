package com.mercurytfs.mercury.mavenreleaser.services.impl;

import com.mercurytfs.mercury.mavenreleaser.Artefact;
import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtefactResult;
import com.mercurytfs.mercury.mavenreleaser.dto.ArtifactVersion;
import com.mercurytfs.mercury.mavenreleaser.helpers.ConsoleHelper;
import com.mercurytfs.mercury.mavenreleaser.helpers.NewVersionHelper;
import com.mercurytfs.mercury.mavenreleaser.services.MavenService;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
   __  __
 |  \/  | __ ___   _____ _ __
 | |\/| |/ _` \ \ / / _ \ '_ \
 | |  | | (_| |\ V /  __/ | | |
 |_|  |_|\__,_| \_/ \___|_| |_|
  ___                 _
 |_ _|_ ____   _____ | | _____ _ __
  | || '_ \ \ / / _ \| |/ / _ \ '__|
  | || | | \ V / (_) |   <  __/ |
 |___|_| |_|\_/ \___/|_|\_\___|_|

 *
 */
@Service
public class MavenServiceImpl implements MavenService {
    private final Logger log = LoggerFactory.getLogger(MavenServiceImpl.class);

    public static final String SNAPSHOT_LITERAL = "-SNAPSHOT";
    public static final String USERNAME_LITERAL = "username";
    public static final String PASS_LITERAL = "password";


    @Value("${pelliMode:nonActive}")
    private String pelliMode;

    @Value("${maven.home}")
    private String mavenHome;

    @Autowired
    private JiraHelper jiraHelper;

    @Override
    public void invokeReleaser(String pom, String user, String pass, ReleaseArtefactResult releaseArtefactResult, ArtifactVersion artefactNextVersion) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        Artefact artefact = getArtefactFromFile(pom);
        if (artefactNextVersion == null)
            artefactNextVersion = computeNextVersion(artefact);

        request.setPomFile(new File(pom));
        final List<String> goals = new ArrayList<>();
        goals.add("release:prepare");
        goals.add("release:perform");
        request.setGoals(goals);

        final Properties properties = new Properties();
        properties.put(USERNAME_LITERAL, user);
        properties.put(PASS_LITERAL, pass);
        properties.put("arguments", " -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -U -DscmCommentPrefix=\"[skip ci]\"");
        properties.put("developmentVersion", artefactNextVersion.getNextVersion());

        if (artefactNextVersion.isOverrideCurrentVersion())
            properties.put("releaseVersion", artefactNextVersion.getCurrentVersion());

        request.setProperties(properties);

        final Invoker invoker = new DefaultInvoker();
        invoker.setInputStream(System.in);
        invoker.setMavenHome(new File(mavenHome));
        final InvocationResult result = invoker.execute(request);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Build failed.");
        }

        jiraHelper.updateJiras(
                artefactNextVersion.getPureCurrentVersion(SNAPSHOT_LITERAL),
                artefactNextVersion.getPureNextVersion(SNAPSHOT_LITERAL),
                artefact,
                releaseArtefactResult);
    }

    private ArtifactVersion computeNextVersion(Artefact artefact) {
        log.info("Current Version : " + artefact.getVersion());
        ArtifactVersion artefactNextVersion = new ArtifactVersion();
        artefactNextVersion.setArtifactId(artefact.getArtefactId());
        artefactNextVersion.setCurrentVersion(artefact.getVersion());
        artefactNextVersion.setScm(artefact.getScmURL());

        String autoVersion = NewVersionHelper.getNextVersion(artefact.getVersion(), artefact.getScmURL());
        String nextVersion = ConsoleHelper.getLineFromConsole("Type the new version (" + autoVersion + "): ");

        if (hasCurrentVersionIndicated(nextVersion)){
            String[] tempVersion = splitPossibleVersionPair(nextVersion);
            nextVersion = tempVersion[0];
            artefactNextVersion.setCurrentVersion(tempVersion[1]);
            artefactNextVersion.setOverrideCurrentVersion(true);
        }
        if (nextVersion.equals("")) {
            nextVersion = autoVersion;
        }
        if (!nextVersion.endsWith(SNAPSHOT_LITERAL)) {
            log.warn("Next Version has not -SNAPSHOT SUFFIX. Adding...");
            nextVersion = nextVersion + SNAPSHOT_LITERAL;
        }
        artefactNextVersion.setNextVersion(nextVersion);

        return artefactNextVersion;

    }


    public  boolean hasCurrentVersionIndicated(String possiblePair){
        return possiblePair.matches("[0-9]+\\.[0-9]+\\.[0-9]+@[0-9]+\\.[0-9]+\\.[0-9]+");
    }

    public String[] splitPossibleVersionPair(String possiblePair){
        return possiblePair.split("@");
    }

    private Artefact getArtefactFromFile(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final Artefact artefact = new Artefact();
        try {
            InputStreamReader fis = new InputStreamReader(new FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8);
            final Model model = mavenreader.read(fis);
            artefact.setArtefactId(model.getArtifactId());
            artefact.setGroupId(model.getGroupId());
            artefact.setVersion(model.getVersion());
            if (model.getScm() != null) {
                artefact.setScmURL(model.getScm().getDeveloperConnection());
            }
        } catch (IOException | XmlPullParserException ex2) {
            log.error(ex2.toString());
        }
        return artefact;
    }


}
