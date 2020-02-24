package com.mercurytfs.mercury.mavenreleaser.beans;

import com.mercurytfs.mercury.mavenreleaser.Artefact;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class ReleaseArtefactResult {
    @Getter
    private Map<String, String> artefacts;
    @Getter
    private Map<String, String> artefactsAlreadyReleased;
    @Getter
    private Map<String, String> artefactsNotInArtifactory;
    @Getter
    private Map<String, Artefact> jirasNotReleased;
    @Getter
    private Map<String, Artefact> jirasReleased;

    public ReleaseArtefactResult(){
        artefacts = new HashMap<>();
        artefactsAlreadyReleased = new HashMap<>();
        artefactsNotInArtifactory = new HashMap<>();
        jirasNotReleased = new HashMap<>();
        jirasReleased = new HashMap<>();
    }

    public ReleaseArtefactResult addAll(ReleaseArtefactResult input){
        artefacts.putAll(input.getArtefacts());
        artefactsAlreadyReleased.putAll(input.getArtefactsAlreadyReleased());
        artefactsNotInArtifactory.putAll(input.getArtefactsNotInArtifactory());
        jirasNotReleased.putAll(input.getJirasNotReleased());
        jirasReleased.putAll(input.getJirasReleased());
        return this;
    }
}
