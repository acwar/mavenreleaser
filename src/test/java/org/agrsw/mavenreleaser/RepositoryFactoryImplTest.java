package org.agrsw.mavenreleaser;

import org.agrsw.mavenreleaser.dto.RepositoryDTO;
import org.agrsw.mavenreleaser.enums.RepositoryTypeEnum;
import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.factory.RepositoryFactory;
import org.agrsw.mavenreleaser.factory.impl.RepositoryFactoryImpl;
import org.agrsw.mavenreleaser.repository.VersionControlRepository;
import org.agrsw.mavenreleaser.repository.impl.SVNManagerImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class RepositoryFactoryImplTest {

    @Autowired
    private RepositoryFactory repositoryFactory;

    @TestConfiguration
    static class TestConfig{
        @Bean
        public RepositoryFactory repositoryFactory(){
            return new RepositoryFactoryImpl();
        }
    }
    @Test
    public void getRepositoryManagerTest() {
        RepositoryDTO repositoryDTO = new RepositoryDTO();
        repositoryDTO.setRepositoryType(RepositoryTypeEnum.SVN);
        try {
            VersionControlRepository repo = repositoryFactory.getRepositoryManager(repositoryDTO);
            assertTrue(repo instanceof SVNManagerImpl);
        } catch (ReleaserException e) {
            e.printStackTrace();
        }
    }

}