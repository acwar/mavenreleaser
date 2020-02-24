package com.mercurytfs.mercury.mavenreleaser.repository;


import com.mercurytfs.mercury.mavenreleaser.dto.RepositoryDTO;
import com.mercurytfs.mercury.mavenreleaser.exception.ReleaserException;

import java.io.File;

public interface VersionControlRepository {

    boolean commit(File file, RepositoryDTO repositoryDTO, String notCheckToken) throws ReleaserException;

    boolean downloadProject(RepositoryDTO repositoryDTO, final File target) throws ReleaserException;
}
