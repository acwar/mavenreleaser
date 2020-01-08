package org.agrsw.mavenreleaser.enums;

public enum RepositoryTypeEnum {

    GIT("git"),
    SVN("svn");

    private String repositoryType;

    RepositoryTypeEnum(String repositoryType) {
        this.repositoryType = repositoryType;
    }

    public String getRepositoryType() {
        return repositoryType;
    }
}
