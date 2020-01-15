package org.agrsw.mavenreleaser.helpers.impl;

import org.agrsw.mavenreleaser.beans.ReleaseArtifact;
import org.agrsw.mavenreleaser.dto.RepositoryDTO;
import org.agrsw.mavenreleaser.enums.RepositoryTypeEnum;
import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.factory.RepositoryFactory;
import org.agrsw.mavenreleaser.helpers.SCMMediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class SCMMediatorImpl implements SCMMediator {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RepositoryFactory repositoryFactory;

    //TODO add as parameter
    private ReleaseArtifact releaseArtifact;

    @Override
    public boolean downloadProject(String url, File target) throws ReleaserException {
        log.info("Downloading from SCM %s", url);

        RepositoryDTO repositoryDTO  = new RepositoryDTO();
        repositoryDTO.setUserName(releaseArtifact.getUsername());
        repositoryDTO.setPassword(releaseArtifact.getPassword());
        repositoryDTO.setRemotePath(releaseArtifact.getUrl());

        if (isGit(url))
            repositoryDTO.setRepositoryType(RepositoryTypeEnum.GIT);
        else
            repositoryDTO.setRepositoryType(RepositoryTypeEnum.SVN);

        repositoryFactory.getRepositoryManager(repositoryDTO).downloadProject(repositoryDTO,target);

        return false;
    }

    private boolean isGit(String url){
        return (url.contains(".git") || url.contains("192.168.10.125") || url.contains("gitlabce.mercury-tfs.com"));
    }

    @Override
    public void setReleaseArtifact(ReleaseArtifact releaseArtifact) {
        this.releaseArtifact = releaseArtifact;
    }

}
