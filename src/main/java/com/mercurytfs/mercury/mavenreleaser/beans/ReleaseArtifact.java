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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getArtefactName() {
		return artefactName;
	}

	public void setArtefactName(String artefactName) {
		this.artefactName = artefactName;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public ArtifactVersionsList getVersionsList() {
		return versionsList;
	}

	public void setVersionsList(ArtifactVersionsList versionsList) {
		this.versionsList = versionsList;
	}

	public void setReleaseAction(ReleaseAction releaseAction) {
		this.releaseAction = releaseAction;
	}

}
