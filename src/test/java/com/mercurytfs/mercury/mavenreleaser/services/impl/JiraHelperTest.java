package com.mercurytfs.mercury.mavenreleaser.services.impl;

import com.mercurytfs.mercury.mavenreleaser.Artefact;
import com.mercurytfs.mercury.mavenreleaser.Releaser;
import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtefactResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class JiraHelperTest {

    @TestConfiguration
    static class configClass {
        @Bean
        public JiraHelper jiraHelper() {
            return new JiraHelper();
        }
    }

    @Autowired
    private JiraHelper jiraHelper;

    /*proyecto no existe*/
    @Test
    public void updateJirasTestProjectNotFound(){
        String version = "1.7.0";
        String nextversion = "1.8.0";
        ReleaseArtefactResult result = new ReleaseArtefactResult();
        jiraHelper.updateJiras(version, nextversion, mockingArtefact("cosamala","bdp.service",
                version, "LIBERBANK-8540"), result);
    }

    /*Se genera bien el nuevo jira y se cierra el antiguo*/
    @Test
    public void updateJirasTestOK(){
        String version = "1.7.0";
        String nextversion = "1.8.0";
        ReleaseArtefactResult result = new ReleaseArtefactResult();
        jiraHelper.updateJiras(version,nextversion,mockingArtefact("com.mercurytfs.mercury.customers.liberbank.products","bdp.service",
                version, "LIBERBANK-8540"),result);
    }

    // mock de artefacto del bdp.service del LIBERANK. Para mas info ver http://jira.mercury-tfs.com/browse/LIBERBANK-8540
    private Artefact mockingArtefact(String groupId, String artefactId, String version, String jiraIssue){
        Artefact artefact = new Artefact();
        artefact.setGroupId(groupId);
        artefact.setArtefactId(artefactId);
        artefact.setVersion(version);
        artefact.setJiraIssue(jiraIssue);
        return artefact;
    }
}
