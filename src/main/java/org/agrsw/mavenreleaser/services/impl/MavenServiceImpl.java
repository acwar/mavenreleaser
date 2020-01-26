package org.agrsw.mavenreleaser.services.impl;

import org.agrsw.mavenreleaser.Artefact;
import org.agrsw.mavenreleaser.Releaser;
import org.agrsw.mavenreleaser.helpers.ConsoleHelper;
import org.agrsw.mavenreleaser.services.MavenService;
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

@Service
public class MavenServiceImpl implements MavenService {
    private Logger log = LoggerFactory.getLogger(Releaser.class);

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
        final String autoVersion = getNextVersion(artefact.getVersion(), artefact.getScmURL());
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

    private String getNextVersion(String version, String branchName) {
        log.debug("-->getNextVersion");
        String nextVersion = "";
        log.debug("Current Version: " + version);
        log.debug("BranchName: " + branchName);
        try {
            if (version.endsWith(SNAPSHOT_LITERAL)) {
                int snapshotPosition = version.indexOf(SNAPSHOT_LITERAL);
                version = version.substring(0, snapshotPosition);
            }
            if (branchName.endsWith(SNAPSHOT_LITERAL)) {
                int snapshotPosition = branchName.indexOf(SNAPSHOT_LITERAL);
                branchName = branchName.substring(0, snapshotPosition);
            }
            branchName = new StringBuilder(branchName).reverse().toString();
            int index = branchName.indexOf('-');
            if (index == -1) {
                nextVersion = incrementMiddle(version);
            } else {
                branchName = branchName.substring(0, index);
                branchName = new StringBuilder(branchName).reverse().toString();
                int position = branchName.toUpperCase().indexOf('X');
                if (position > -1) {
                    if (position == 2) {
                        int position2 = version.indexOf('.', position + 1);
                        int num = Integer.parseInt(version.substring(position, position2));
                        num++;
                        nextVersion = version.substring(0, position) + num;
                        nextVersion = nextVersion + version.substring(position2);
                    }
                    if ((position == 4) || (position == 5)) {
                        int position2 = version.indexOf('.', position);
                        position = (position2 > -1) ? position2 + 1 : position;
                        int num = Integer.parseInt(version.substring(position));
                        num++;
                        nextVersion = version.substring(0, position) + num;
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.toString());
            log.info("The Next Version could not be discover automatically");
            nextVersion = "";
        }
        if (!nextVersion.equals("")) {
            nextVersion = nextVersion + SNAPSHOT_LITERAL;
        }

        log.debug("New Version " + nextVersion);
        log.debug("<--getNextVersion");
        return nextVersion;
    }

    private String incrementMiddle(final String version) {
        log.debug("-->getNextVersion");
        String newVersion = "";
        try {
            final int position = version.indexOf('.');
            final int position2 = version.indexOf('.', position + 1);
            if (position == 1) {
                int num = Integer.parseInt(version.substring(position + 1, position2));
                ++num;
                newVersion = version.substring(0, position) + "." + num;
                newVersion = newVersion + version.substring(position2);
            }
        } catch (Exception e) {
            log.error("Error incrementing version of: " + version);
        }
        log.debug("<--getNextVersion");
        return newVersion;
    }
}
