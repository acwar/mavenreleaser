package com.mercurytfs.mercury.mavenreleaser.beans;


import com.mercurytfs.mercury.mavenreleaser.dto.ArtifactVersion;
import com.mercurytfs.mercury.mavenreleaser.dto.ArtifactVersionsList;
import com.mercurytfs.mercury.mavenreleaser.enums.ReleaseAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Getter @Setter
    private ArtifactVersionsList versionsList;
    @Getter @Setter
    private boolean DryRun;

    private ReleaseAction releaseAction;

    /**
     * Return a Release action enum based on the action passed as param
     * (indeed the releaseAction is unneeded)
     * @return RelaseAction
     */
    public ReleaseAction getReleaseAction(){
        return ReleaseAction.getReleaseAction(action);
    }

    public ArtifactVersion getDependencyNextVersion(String dependency){
        if (versionsList==null)
            return null;

        return versionsList.findByArtifactId(dependency);

    }

}
