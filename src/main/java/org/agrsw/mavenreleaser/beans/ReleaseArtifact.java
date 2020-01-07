package org.agrsw.mavenreleaser.beans;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
public class ReleaseArtifact {

    @Getter @Setter
    private  String username;
    @Getter @Setter
    private  String password;
    @Getter @Setter
    private  String url;
    @Getter @Setter
    private  String artefactName;
    @Getter @Setter
    private  String action;

}
