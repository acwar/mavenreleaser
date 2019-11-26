package org.agrsw.mavenreleaser.dto;


import org.agrsw.mavenreleaser.util.RepositoryTypeEnum;

public class RepositoryDTO {

    private String userName;
    private String password;
    private String remotePath;
    private String localPath;
    private RepositoryTypeEnum repositoryType;
    
	public RepositoryDTO(String username2, String password2, String url, String localRepositoryPath,
			RepositoryTypeEnum repositoryTypeEnum) {

		setUserName(username2);
		setPassword(password2);
		setRemotePath(url);
		setLocalPath(localRepositoryPath);
		setRepositoryType(repositoryTypeEnum);
		
	}
	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getRemotePath() {
		return remotePath;
	}
	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}
	public String getLocalPath() {
		return localPath;
	}
	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}
	public RepositoryTypeEnum getRepositoryType() {
		return repositoryType;
	}
	public void setRepositoryType(RepositoryTypeEnum repositoryType) {
		this.repositoryType = repositoryType;
	}
    
    
}
