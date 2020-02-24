package com.mercurytfs.mercury.mavenreleaser.factory;


import com.mercurytfs.mercury.mavenreleaser.exception.ReleaserException;
import com.mercurytfs.mercury.mavenreleaser.repository.VersionControlRepository;
import com.mercurytfs.mercury.mavenreleaser.dto.RepositoryDTO;
import com.mercurytfs.mercury.mavenreleaser.enums.RepositoryTypeEnum;

public interface RepositoryFactory {

    VersionControlRepository getRepositoryManager(RepositoryTypeEnum repositoryTypeEnum) throws ReleaserException;
    VersionControlRepository getRepositoryManager(RepositoryDTO repositoryDTO) throws ReleaserException;
}
