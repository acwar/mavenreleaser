package org.agrsw.mavenreleaser.services;

import org.agrsw.mavenreleaser.beans.ReleaseArtifact;
import org.agrsw.mavenreleaser.dto.RepositoryDTO;
import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.factory.RepositoryFactory;
import org.agrsw.mavenreleaser.repository.VersionControlRepository;
import org.agrsw.mavenreleaser.services.impl.SCMMediatorImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;

@RunWith(SpringRunner.class)
public class SCMMediatorTest {
    @Autowired
    private SCMMediator scmMediator;

    @TestConfiguration
    static class Configuration{
        @Bean
        public SCMMediator createMediator(){
            return new SCMMediatorImpl();
        }
    }

    @MockBean
    private RepositoryFactory factory;

    @MockBean
    private VersionControlRepository svmRepo;

    @Test
    public void SetReleaseArtifactTest(){
        scmMediator.setReleaseArtifact(new ReleaseArtifact());
    }

    @Test
    public void legitimateDownloadTest() throws ReleaserException {

        Mockito.when(factory.getRepositoryManager(Mockito.any(RepositoryDTO.class))).thenReturn(svmRepo);
        Mockito.when(svmRepo.downloadProject(Mockito.any(RepositoryDTO.class),Mockito.any(File.class))).thenReturn(true);
        Mockito.when(svmRepo.commit(Mockito.any(File.class),Mockito.any(RepositoryDTO.class),Mockito.anyString())).thenReturn(true);

        ReleaseArtifact artifact = new ReleaseArtifact();
        artifact.setUsername("test");
        artifact.setPassword("test");
        scmMediator.setReleaseArtifact(artifact);

        scmMediator.downloadProject("www.google.es", new File(""));
        scmMediator.downloadProject("www.google.es.git", new File(""));
        scmMediator.commitFile("www.google.es.git", new File(""));
    }

    //No release Artifact Error control

    //No URL Error control


}
