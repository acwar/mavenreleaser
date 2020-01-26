package org.agrsw.mavenreleaser.beans;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.agrsw.mavenreleaser.enums.ReleaseAction;

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

    private ReleaseAction releaseAction;

    public ReleaseAction getReleaseAction(){
        return ReleaseAction.getReleaseAction(action);
    }

}
