package com.mercurytfs.mercury.mavenreleaser.factory.impl;


import com.mercurytfs.mercury.mavenreleaser.exception.ReleaserException;
import com.mercurytfs.mercury.mavenreleaser.factory.RepositoryFactory;
import com.mercurytfs.mercury.mavenreleaser.repository.VersionControlRepository;
import com.mercurytfs.mercury.mavenreleaser.repository.impl.GitManagerImpl;
import com.mercurytfs.mercury.mavenreleaser.dto.RepositoryDTO;
import com.mercurytfs.mercury.mavenreleaser.repository.impl.SVNManagerImpl;
import com.mercurytfs.mercury.mavenreleaser.enums.RepositoryTypeEnum;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class RepositoryFactoryImpl implements RepositoryFactory {

    private static final Map<RepositoryTypeEnum, VersionControlRepository> REPOSITORY_MAP = new HashMap<>();

   static {
       REPOSITORY_MAP.put(RepositoryTypeEnum.SVN, new SVNManagerImpl());
       REPOSITORY_MAP.put(RepositoryTypeEnum.GIT, new GitManagerImpl());
    }

    @Deprecated
    @Override
    public VersionControlRepository getRepositoryManager(RepositoryTypeEnum repositoryTypeEnum) throws ReleaserException {

        if(Objects.isNull(REPOSITORY_MAP.get(repositoryTypeEnum))){
            throw new ReleaserException("Repository not found");
        }

        return REPOSITORY_MAP.get(repositoryTypeEnum);
    }

    @Override
    public VersionControlRepository getRepositoryManager(RepositoryDTO repositoryDTO) throws ReleaserException {
        return getRepositoryManager(repositoryDTO.getRepositoryType());
    }
}
