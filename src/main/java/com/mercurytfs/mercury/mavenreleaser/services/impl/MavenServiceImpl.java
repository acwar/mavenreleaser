package com.mercurytfs.mercury.mavenreleaser.services.impl;

import com.mercurytfs.mercury.mavenreleaser.Artefact;
import com.mercurytfs.mercury.mavenreleaser.helpers.ConsoleHelper;
import com.mercurytfs.mercury.mavenreleaser.helpers.NewVersionHelper;
import com.mercurytfs.mercury.mavenreleaser.services.MavenService;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private Logger log = LoggerFactory.getLogger(MavenServiceImpl.class);

    public static final String SNAPSHOT_LITERAL = "-SNAPSHOT";
    public static final String USERNAME_LITERAL = "username";
    public static final String PASS_LITERAL = "password";


    @Value("${maven.home}")
    private String mavenHome;

    @Override
    public void invokeReleaser(String pom,String user,String pass) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        Artefact artefact = getArtefactFromFile(pom);

        log.info("Current Version : " + artefact.getVersion());
        final String autoVersion = NewVersionHelper.getNextVersion(artefact.getVersion(), artefact.getScmURL());
        String nextVersion = ConsoleHelper.getLineFromConsole("Type the new version (" + autoVersion + "): ");
        if (nextVersion.equals("")) {
            nextVersion = autoVersion;
        }
        if (!nextVersion.endsWith(SNAPSHOT_LITERAL)) {
            log.warn("Next Version has not -SNAPSHOT SUFFIX. Adding...");
            nextVersion = nextVersion + SNAPSHOT_LITERAL;
        }
        request.setPomFile(new File(pom));
        final List<String> goals = new ArrayList<>();
        goals.add("release:prepare");
        goals.add("release:perform");
        request.setGoals(goals);

        final Properties properties = new Properties();
        properties.put(USERNAME_LITERAL, user);
        properties.put(PASS_LITERAL, pass);
        properties.put("arguments", "-DskipTests -Dmaven.javadoc.skip=true -U ");
        properties.put("developmentVersion", nextVersion);
        request.setProperties(properties);

        final Invoker invoker = new DefaultInvoker();
        invoker.setInputStream(System.in);
        invoker.setMavenHome(new File(mavenHome));
        final InvocationResult result = invoker.execute(request);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Build failed.");
        }
        String version = artefact.getVersion();
        if (version.endsWith(SNAPSHOT_LITERAL)) {
            final int snapshotPosition = version.indexOf(SNAPSHOT_LITERAL);
            version = version.substring(0, snapshotPosition);
        }
        if (nextVersion.endsWith(SNAPSHOT_LITERAL)) {
            final int snapshotPosition = nextVersion.indexOf(SNAPSHOT_LITERAL);
            nextVersion = nextVersion.substring(0, snapshotPosition);
        }
//        final String project = ProjectsEnum.getProjectNameFromGroupId(artefact.getGroupId());
//        if (!project.equals("")) {
//            final Artefact arti = JiraClient.getIssueKey(project, artefact.getArtefactId(), version);
//            if (arti == null) {
//                Releaser.jirasNotReleased.put(artefact.getGroupId() + artefact.getArtefactId(), artefact);
//                log.error("Cannot find jira issue for artefact: " + artefact);
//            } else {
//                JiraClient.createVersion(project, nextVersion, nextVersion);
//                final String newIssue = JiraClient.createIssue(project, artefact.getArtefactId() + "-" + nextVersion, arti.getDescription(), artefact.getArtefactId(), version, nextVersion);
//                if (newIssue == null) {
//                    Releaser.jirasNotReleased.put(artefact.getGroupId() + artefact.getArtefactId(), artefact);
//                    log.error("Cannot create jira issue for artefact: " + artefact);
//                } else {
//                    final String oldIssue = JiraClient.closeIssue(arti.getJiraIssue());
//                    if (oldIssue == null) {
//                        Releaser.jirasNotReleased.put(artefact.getGroupId() + artefact.getArtefactId(), artefact);
//                        log.error("Cannot close jira issue for artefact: " + artefact);
//                    } else {
//                        artefact.setJiraIssue(newIssue);
//                        Releaser.jirasReleased.put(artefact.getGroupId() + artefact.getArtefactId(), artefact);
//                        log.error("jira issue released: " + artefact);
//                    }
//                }
//            }
//        } else {
//            Releaser.jirasNotReleased.put(artefact.getGroupId() + artefact.getArtefactId(), artefact);
//            log.error("Cannot determinate the project for artifact: " + artefact);
//        }
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
