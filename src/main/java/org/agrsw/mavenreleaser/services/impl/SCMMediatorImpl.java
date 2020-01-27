package org.agrsw.mavenreleaser.services.impl;

import org.agrsw.mavenreleaser.beans.ReleaseArtifact;
import org.agrsw.mavenreleaser.dto.RepositoryDTO;
import org.agrsw.mavenreleaser.enums.RepositoryTypeEnum;
import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.factory.RepositoryFactory;
import org.agrsw.mavenreleaser.services.SCMMediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class SCMMediatorImpl implements SCMMediator {

    private Logger log = LoggerFactory.getLogger(this.getClass());


    @Autowired
    private RepositoryFactory repositoryFactory;

    @Value("${notcheck.token}")
    private String notCheckToken;

    private ReleaseArtifact releaseArtifact;

    @Override
    public boolean downloadProject(String url, File target) throws ReleaserException {
        log.info("Downloading from SCM {}", url);
        RepositoryDTO repositoryDTO  = buildRepositoryDTO(url);
        repositoryFactory.getRepositoryManager(repositoryDTO).downloadProject(repositoryDTO,target);
        return false;
    }

    @Override
    public boolean commitFile(String url, File target) throws ReleaserException {
        log.info("Commit to SCM {}", url);
        RepositoryDTO repositoryDTO  = buildRepositoryDTO(url);
        repositoryFactory.getRepositoryManager(repositoryDTO).commit(target,repositoryDTO,notCheckToken);
        return false;

    }

    private RepositoryDTO buildRepositoryDTO(String url){
        RepositoryDTO repositoryDTO  = new RepositoryDTO();
        repositoryDTO.setUserName(releaseArtifact.getUsername());
        repositoryDTO.setPassword(releaseArtifact.getPassword());
        repositoryDTO.setRemotePath(url.replaceAll("gitlabce.mercury-tfs.com","192.168.10.125"));

        if (isGit(url))
            repositoryDTO.setRepositoryType(RepositoryTypeEnum.GIT);
        else
            repositoryDTO.setRepositoryType(RepositoryTypeEnum.SVN);

        return repositoryDTO;
    }

    private boolean isGit(String url){
        return (url.contains(".git") || url.contains("192.168.10.125") || url.contains("gitlabce.mercury-tfs.com"));
    }

    @Override
    public void setReleaseArtifact(ReleaseArtifact releaseArtifact) {
        this.releaseArtifact = releaseArtifact;
    }

}