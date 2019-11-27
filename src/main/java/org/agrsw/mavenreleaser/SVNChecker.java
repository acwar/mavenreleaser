package org.agrsw.mavenreleaser;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.tmatesoft.svn.core.wc2.SvnAnnotate;

public class SVNChecker
{
    private static final Logger log;
    private Releaser releaser;
    private String repositoryURL = "http://192.168.10.2/svn/mercury";
    private static int ERROR_WRONG_PARAMETERS;
    private static int ERROR_COMMIT_MESSAGE_FORMAT;
    private static int ERROR_SVN_FILE_HAS_NOT_OPEN_ARTEfACT_OR_DONT_EXIST;
    private static int ERROR_SVN_FILE_ARTEFACT_IS_NOT_LINKED_WITH_COMMIT_ISSUE;
    private static int ERROR_JIRA_ISSUE_IS_RESOLVED_CANNOT_COMMIT_TO_THIS_ISSUE;
    
    final String[] projects = { "MERCURY", "BANORTE", "PRUEB", "SANESPBACK", "SANMEXICO","LIBERBANK", "SANGER", "SANCHILE", "TARIFARIO", "SANESP","SANCHILEBK","SANESPBCK2","WETRADE","SANGTS"};
    
    @Value("${notcheck.token}")
    private String notcheckTokenProperty;
    private static String notcheckToken ="#NOTCHECK";
	private static String repoType = "SVN";
    
    //java -cp mavenreleaser-3.0.0-SNAPSHOT.jar -Dloader.main=org.agrsw.mavenreleaser.SVNChecker    org.springframework.boot.loader.PropertiesLauncher
    
    static {
        log = LoggerFactory.getLogger((Class)SVNChecker.class);
        SVNChecker.ERROR_WRONG_PARAMETERS = 1;
        SVNChecker.ERROR_COMMIT_MESSAGE_FORMAT = 2;
        SVNChecker.ERROR_SVN_FILE_HAS_NOT_OPEN_ARTEfACT_OR_DONT_EXIST = 3;
        SVNChecker.ERROR_SVN_FILE_ARTEFACT_IS_NOT_LINKED_WITH_COMMIT_ISSUE = 4;
        SVNChecker.ERROR_JIRA_ISSUE_IS_RESOLVED_CANNOT_COMMIT_TO_THIS_ISSUE = 5;
    }
    
    public SVNChecker() {
		notcheckTokenProperty = Releaser.getToken();
    }
    
    public Artefact getArtefactOfFile(final String file) {
        final Artefact artefact = null;
        if (file != null) {
            final String[] splits = file.split("/src/main");
            SVNChecker.log.debug(splits.toString());
        }
        return artefact;
    }
    
    public static void main(final String[] args) {
        String[] svnFiles = null;
        String commitMessage = null;
        String issueKey = null;
         
        if (args!=null){
        	for (int i=0;i<args.length;i++){
        		SVNChecker.log.info("ARGS[" + i +"] " + args[i]);
        	}
        	SVNChecker.log.info("ARGS: " + args.toString());
        } 
        
        final SVNChecker fm = new SVNChecker();
        SVNChecker.notcheckToken = fm.notcheckTokenProperty;
        SVNChecker.log.info("Not Check Token: " + notcheckToken);
        
        if (args.length == 3) {
        	SVNChecker.log.info("SVN Files: " + args[0]);
            SVNChecker.log.info("Commit Message: " + args[1]);
            SVNChecker.log.info("Usuario: " + args[2]);
            svnFiles = args[0].split(";");
            commitMessage = args[1];
            
        } else if (args.length == 5) {
        	SVNChecker.log.info("SVN Files: " + args[0]);
            SVNChecker.log.info("Commit Message: " + args[1]);
            SVNChecker.log.info("Usuario (optional): " + args[2]);
            SVNChecker.log.info("Repo Path: " + args[3]);
            SVNChecker.log.info("Branch Name: " + args[4]);
            svnFiles = args[0].split(";");
            commitMessage = args[1];
            fm.setRepositoryURL(args[3]);
            repoType  = args[4].toUpperCase();
            
        }  else if (args.length == 4) {
        	SVNChecker.log.info("GIT New Branch");
            SVNChecker.log.info("Commit Message: " + args[0]);
            commitMessage = args[0];
            
        } else {
        	SVNChecker.log.error("ERROR_WRONG_PARAMETERS");
            System.exit(SVNChecker.ERROR_WRONG_PARAMETERS);
        }
        
        if ((issueKey=fm.isJiraCommit(commitMessage))!=null) {
            SVNChecker.log.info("jira issue key found");
            SVNChecker.log.info("Before call checkCommit");
            JiraClient.userName = Releaser.jiraUser;
            JiraClient.password = Releaser.jiraPassword;
            Releaser.setUsername(Releaser.jiraUser);
            Releaser.setPassword(Releaser.jiraPassword);
            
            final int result = Releaser.checkCommit(svnFiles, issueKey,repoType,fm.repositoryURL);
            SVNChecker.log.info("After call checkCommit. Result: " + result);
            switch (result) {
                case 0: {
                    System.exit(0);
                }
                case 1: {
                    System.exit(SVNChecker.ERROR_SVN_FILE_HAS_NOT_OPEN_ARTEfACT_OR_DONT_EXIST);
                }
                case 2: {
                    System.exit(SVNChecker.ERROR_SVN_FILE_ARTEFACT_IS_NOT_LINKED_WITH_COMMIT_ISSUE);
                }
                case 3: {
                    System.exit(SVNChecker.ERROR_JIRA_ISSUE_IS_RESOLVED_CANNOT_COMMIT_TO_THIS_ISSUE);
                    break;
                }
            }
        } else {
            SVNChecker.log.info("Jira issue key not found. Checking if it`s a maven release plugin commit");
           
            issueKey = fm.checkCommitMessageWithOutNumber(commitMessage, "maven-release-plugin");
            if (issueKey==null){
            	issueKey = fm.checkCommitMessageWithOutNumber(commitMessage,SVNChecker.notcheckToken );            	
            }
            if (issueKey == null) {
                SVNChecker.log.error("The commit Message has not the correct format: ERROR_COMMIT_MESSAGE_FORMAT");
                System.err.println("The commit Message has not the correct format: ERROR_COMMIT_MESSAGE_FORMAT");     
                System.exit(SVNChecker.ERROR_COMMIT_MESSAGE_FORMAT);
            }
            else {
                SVNChecker.log.info("The commit Message has the correct format");
                System.exit(0);
            }
        }
    }
    
    private String isJiraCommit(String commitMessage) {
        SVNChecker.log.info("Check if the commit message contains de jira issue key");
        String issueKey;
        
        for (int i = 0; i < projects.length; ++i) {
            if ((issueKey=checkCommitMessage(commitMessage, projects[i])) != null) {
            	SVNChecker.log.info("issueKey: " + issueKey);
                return issueKey;
            }
        }
        return null;
	}

	private void setRepositoryURL(String string) {
    	repositoryURL = string;
	}

	public String checkCommitMessage(final String commitMessage, final String projectName) {
        String key = null;
        final Pattern p = Pattern.compile(".*(" + projectName + "-[\\d]+).*", 2);
        final Matcher m = p.matcher(commitMessage);
        final boolean matches = m.find();
        if (matches) {
            SVNChecker.log.debug(m.group(1));
            key = m.group(1);
        }
        return key;
    }
    
    public String checkCommitMessageWithOutNumber(final String commitMessage, final String projectName) {
        String key = null;
        final Pattern p = Pattern.compile(".*(" + projectName + ").*", Pattern.CASE_INSENSITIVE);
        
        final Matcher m = p.matcher(commitMessage);
        final boolean matches = m.find();
        if (matches) {
            SVNChecker.log.debug(m.group(1));
            key = m.group(1);
        }
        return key;
    }
    
    public Releaser getReleaser() {
        return this.releaser;
    }
    
    public void setReleaser(final Releaser releaser) {
        this.releaser = releaser;
    }
    
}
