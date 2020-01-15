package org.agrsw.mavenreleaser.factory;


import org.agrsw.mavenreleaser.dto.RepositoryDTO;
import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.repository.VersionControlRepository;
import org.agrsw.mavenreleaser.enums.RepositoryTypeEnum;

public interface RepositoryFactory {

    VersionControlRepository getRepositoryManager(RepositoryTypeEnum repositoryTypeEnum) throws ReleaserException;
    VersionControlRepository getRepositoryManager(RepositoryDTO repositoryDTO) throws ReleaserException;
}
