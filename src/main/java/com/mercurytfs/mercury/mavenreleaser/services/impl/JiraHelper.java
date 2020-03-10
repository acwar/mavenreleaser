package com.mercurytfs.mercury.mavenreleaser.services.impl;

import com.mercurytfs.mercury.mavenreleaser.Artefact;
import com.mercurytfs.mercury.mavenreleaser.JiraClient;
import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtefactResult;
import com.mercurytfs.mercury.mavenreleaser.enums.ProjectsEnum;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JiraHelper {

    public void updateJiras(String version, String nextVersion, Artefact artefact, ReleaseArtefactResult releaseArtefactResult){
        log.info("--------------------------------------------");
        log.info("Starting the process of updating jiras...");
        log.info("--------------------------------------------");
      final String project = ProjectsEnum.getProjectNameFromGroupId(artefact.getGroupId());
        if (!project.equals("")) {
            final Artefact arti = JiraClient.getIssueKey(project, artefact.getArtefactId(), version);
            if (arti == null) {
                releaseArtefactResult.addJirasNotReleased(artefact.getJiraIssue(), artefact);
                log.error("Cannot find jira issue for artefact: " + artefact);
            } else {
                //JiraClient.createVersion(project, nextVersion, nextVersion);   deprecado?
                final String newIssue = JiraClient.createIssue(project, artefact.getArtefactId() + "-" + nextVersion, arti.getDescription(), artefact.getArtefactId(), version, nextVersion);
                if (newIssue == null) {
                    releaseArtefactResult.addJirasNotReleased(artefact.getJiraIssue(), artefact);
                    log.error("Cannot create jira issue for artefact: " + artefact);
                } else {
                    final String oldIssue = JiraClient.closeIssue(arti.getJiraIssue());
                    if (oldIssue == null) {
                        releaseArtefactResult.addJirasNotReleased(artefact.getJiraIssue(), artefact);
                        log.error("Cannot close jira issue for artefact: " + artefact);
                    } else {
                        releaseArtefactResult.addJirasReleased(artefact.getJiraIssue(), artefact);
                        artefact.setJiraIssue(newIssue);
                        log.error("jira issue released: " + artefact);
                    }
                }
            }
        } else {
            releaseArtefactResult.addJirasNotReleased(artefact.getJiraIssue(), artefact);
            log.error("Cannot determinate the project for artifact: " + artefact.getArtefactId());
        }
        log.info(releaseArtefactResult.toString());
        log.info("--------------------------------------------");
        log.info("Finalized the process of updating jiras");
        log.info("--------------------------------------------");
    }
}
