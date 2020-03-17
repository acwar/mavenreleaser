package com.mercurytfs.mercury.mavenreleaser.beans;

import com.mercurytfs.mercury.mavenreleaser.Artefact;
import com.mercurytfs.mercury.mavenreleaser.dto.ArtifactVersion;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ReleaseArtefactResult {
    @Getter
    private Map<String, String> artefacts;
    @Getter
    private Map<String, ArtifactVersion> artefactsVersions;
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
        artefactsVersions = new HashMap<>();
        artefactsAlreadyReleased = new HashMap<>();
        artefactsNotInArtifactory = new HashMap<>();
        jirasNotReleased = new HashMap<>();
        jirasReleased = new HashMap<>();
    }

    public ReleaseArtefactResult addAll(ReleaseArtefactResult input){
        artefacts.putAll(input.getArtefacts());
        artefactsVersions.putAll(input.getArtefactsVersions());
        artefactsAlreadyReleased.putAll(input.getArtefactsAlreadyReleased());
        artefactsNotInArtifactory.putAll(input.getArtefactsNotInArtifactory());
        jirasNotReleased.putAll(input.getJirasNotReleased());
        jirasReleased.putAll(input.getJirasReleased());
        return this;
    }

    public void addJirasNotReleased(String artefactKey, Artefact artefact){
        jirasNotReleased.put(artefactKey,artefact);
    }
    public void addJirasReleased(String artefactKey, Artefact artefact){
        jirasReleased.put(artefactKey,artefact);
    }
    @Override
    public String toString(){
        return "\n" +
        "Status of artefacts:" + "\n" +
        "Artefacts:" + "\n" + convertWithStream(artefacts) + "\n" +
        "Artefacts already released: " + "\n" + convertWithStream(artefactsAlreadyReleased) + "\n" +
        "Artefacts not in artifactory: " + "\n" + convertWithStream(artefactsNotInArtifactory) + "\n" +
        "Jiras not released: " + "\n" + convertWithStream(jirasNotReleased) + "\n" +
        "Jiras released: " + "\n" + convertWithStream(jirasReleased) + "\n";
    }

    private  String convertWithStream(Map<?, ?> map) {
        String mapAsString = map.keySet().stream()
                .map(key -> key + "=" + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        return mapAsString;
    }
}
