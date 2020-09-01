package com.mercurytfs.mercury.mavenreleaser.repository.impl;


import com.mercurytfs.mercury.mavenreleaser.repository.VersionControlRepository;
import com.mercurytfs.mercury.mavenreleaser.dto.RepositoryDTO;
import com.mercurytfs.mercury.mavenreleaser.exception.ReleaserException;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

@Component
public class GitManagerImpl implements VersionControlRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitManagerImpl.class);
    private static final String ADD_ALL_FILES_TO_COMMIT = ".";
    private String branch;
    @Override
    public boolean commit(File file, RepositoryDTO repositoryDTO, String notCheckToken) throws ReleaserException{
        try {
            readProperties();
            LOGGER.debug("Branch to commit: "+branch);
            CredentialsProvider cp = new UsernamePasswordCredentialsProvider(repositoryDTO.getUserName(), repositoryDTO.getPassword());
            Git git =  Git.open(new File(file.getPath().replace("pom.xml", "")));
            git.checkout().setName("development").setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).call();
            git.add().addFilepattern(ADD_ALL_FILES_TO_COMMIT).call();
            git.commit()
                    .setAll(true)
                    .setMessage("Versioned files released")
                    .call();

            git.push()
                    .setCredentialsProvider(cp)
                    .call();
           return true;
        } catch (Exception e) {
            LOGGER.error("Error in git commit: ", e.getMessage());
            throw new ReleaserException("Error commit changes into repository: ", e);
        }
    }

    @Override
    public boolean downloadProject(RepositoryDTO repositoryDTO, File target) throws ReleaserException {
        try {
            readProperties();
            CredentialsProvider cp = new UsernamePasswordCredentialsProvider(repositoryDTO.getUserName(), repositoryDTO.getPassword());
            LOGGER.info("Branch to download: "+branch);
            Git git = Git.cloneRepository()
                    .setCredentialsProvider(cp)
                    .setURI(repositoryDTO.getRemotePath())
                    .setDirectory(new File(target.getPath()))
                    .setBranch("development")
                    .call();
            LOGGER.info("Artifact downloaded from GIT");
            return true;
        } catch (GitAPIException e) {
            LOGGER.error("Error in git cloning repository: ", e.getMessage());
            throw new ReleaserException("Error cloning repository.", e);
        }
    }

    private void readProperties(){
        try {
            Properties propiedades = new Properties();
            ClassLoader classLoader = getClass().getClassLoader();
            File f = new File(classLoader.getResource("config.properties").getFile());
            FileReader fr = new FileReader(f);
            propiedades.load(fr);

            this.branch=propiedades.getProperty("git.branch");
            if(branch==null || branch.isEmpty()){
                this.branch="master";
            }
        } catch (IOException e) {
            this.branch="master";
            LOGGER.error("Error en la lectura de propiedades",e);
        }

    }
}
