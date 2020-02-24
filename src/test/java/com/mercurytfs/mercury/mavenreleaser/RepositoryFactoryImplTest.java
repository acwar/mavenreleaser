package com.mercurytfs.mercury.mavenreleaser;

import com.mercurytfs.mercury.mavenreleaser.dto.RepositoryDTO;
import com.mercurytfs.mercury.mavenreleaser.enums.RepositoryTypeEnum;
import com.mercurytfs.mercury.mavenreleaser.exception.ReleaserException;
import com.mercurytfs.mercury.mavenreleaser.factory.RepositoryFactory;
import com.mercurytfs.mercury.mavenreleaser.factory.impl.RepositoryFactoryImpl;
import com.mercurytfs.mercury.mavenreleaser.repository.VersionControlRepository;
import com.mercurytfs.mercury.mavenreleaser.repository.impl.SVNManagerImpl;
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