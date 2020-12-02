package com.mercurytfs.mercury.mavenreleaser;

import com.mercurytfs.mercury.mavenreleaser.enums.ProjectsEnum;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SVNChecker {
    private static final Logger log;

    private Releaser releaser;
    private final String repositoryURL;
    private static int ERROR_WRONG_PARAMETERS;
    private static int ERROR_COMMIT_MESSAGE_FORMAT;
    private static int ERROR_SVN_FILE_HAS_NOT_OPEN_ARTEfACT_OR_DONT_EXIST;
    private static int ERROR_SVN_FILE_ARTEFACT_IS_NOT_LINKED_WITH_COMMIT_ISSUE;
    private static int ERROR_JIRA_ISSUE_IS_RESOLVED_CANNOT_COMMIT_TO_THIS_ISSUE;

    private final String notcheckTokenProperty;

    public static String user = "";
    public static String pass = "";

    private final String scmUser;
    private final String commitMessage;
    private final String[] svnFiles;
    private final String[] projects;

    //java -cp mavenreleaser-3.0.0-SNAPSHOT.jar -Dloader.main=org.agrsw.mavenreleaser.SVNChecker  org.springframework.boot.loader.PropertiesLauncher
    static {
        log = LoggerFactory.getLogger(SVNChecker.class);
        SVNChecker.ERROR_WRONG_PARAMETERS = 1;
        SVNChecker.ERROR_COMMIT_MESSAGE_FORMAT = 2;
        SVNChecker.ERROR_SVN_FILE_HAS_NOT_OPEN_ARTEfACT_OR_DONT_EXIST = 3;
        SVNChecker.ERROR_SVN_FILE_ARTEFACT_IS_NOT_LINKED_WITH_COMMIT_ISSUE = 4;
        SVNChecker.ERROR_JIRA_ISSUE_IS_RESOLVED_CANNOT_COMMIT_TO_THIS_ISSUE = 5;
    }


    public static void main(final String[] args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                SVNChecker.log.info("ARGS[" + i + "] " + args[i]);
            }
            SVNChecker.log.info("ARGS: " + args.toString());
        }
        if (args.length != 3) {
            SVNChecker.log.error("ERROR_WRONG_PARAMETERS");
            System.exit(SVNChecker.ERROR_WRONG_PARAMETERS);
        } else {
            SVNChecker.log.info("SVN Files: " + args[0]);
            SVNChecker.log.info("Commit Message: " + args[1]);
            SVNChecker.log.info("Usuario: " + args[2]);
        }
        SVNChecker fm = new SVNChecker(args[2], args[1], args[0].split(";"));
        System.exit(fm.valideteCommitAgainstProjects());

    }

    private int valideteCommitAgainstProjects(){
        SVNChecker.log.info("Not Check Toker: " + notcheckTokenProperty);
        String issueKey = null;
        if ((issueKey=searhcIssueKey())!=null) {
            SVNChecker.log.info("jira issue key found");
            switch (checkCommit(svnFiles, issueKey)) {
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
            SVNChecker.log.info("jira issue key not found. Checking if it`s a notcheck commit or a maven release plugin commit");
            if (checkCommitMessageWithOutNumber(commitMessage, "maven-release-plugin") == null && checkCommitMessageWithOutNumber(commitMessage, notcheckTokenProperty) == null) {
                SVNChecker.log.error("The commit Message has not the correct format: ERROR_COMMIT_MESSAGE_FORMAT");
                System.err.println("The commit Message has not the correct format: ERROR_COMMIT_MESSAGE_FORMAT");
                System.exit(SVNChecker.ERROR_COMMIT_MESSAGE_FORMAT);
            } else {
                SVNChecker.log.info("The commit Message has the correct format");
                System.exit(0);
            }
        }
        System.exit(10);
        return 0;
    }

    public SVNChecker(String scmUser, String commitMessage, String[] svnFiles) {

        this.scmUser=scmUser;
        this.commitMessage= commitMessage;
        this.svnFiles = svnFiles;

        Properties properties = loadProperties();
        repositoryURL = "http://192.168.10.2/svn/mercury";
        notcheckTokenProperty = properties.getProperty("notcheck.token","#NOTCHECK");
        user =properties.getProperty("jira.user","");
        pass =properties.getProperty("jira.password","");
        projects = properties.getProperty("projects.list","MERCURY").split(",");
    }

    private Properties loadProperties(){
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
        }
        return  properties;
    }

    private String searhcIssueKey(){
        String issueKey=null;
        SVNChecker.log.info("Check if the commit message contains de jira issue key");
        for (int i = 0; i < projects.length; ++i) {
            issueKey = checkCommitMessage(commitMessage, projects[i]);
            SVNChecker.log.info("issueKey: " + issueKey);
            if (issueKey != null)
                return issueKey;
        }
        return null;
    }

    public Artefact getArtefactOfFile(final String repositoryURL, final String file, final String jiraIssue) {
        if (file != null) {
            final String[] splits = file.split("/src/main");
            log.debug(splits.toString());
            String url = getPomURL(file);
            if ((url != null) && (splits.length > 0)) {
                final String pom = getFilefromSVN(repositoryURL, url);
                final Artefact artefact = getArtefactFromString(pom);
                return artefact.getVersion().indexOf("-SNAPSHOT")>=0?JiraClient.getIssueKey(ProjectsEnum.getProjectNameFromGroupId(artefact.getGroupId()), artefact.getArtefactId(), artefact.getVersion().substring(0, artefact.getVersion().indexOf("-SNAPSHOT"))):null;
            }
        }
        return null;
    }
    private String getPomURL(String file) {
        log.debug("getPomURL->" + file);
        String[] splits = null;
        String url = null;
        if (file != null) {
            if (file.contains("/src/main")) {
                splits = file.split("/src/main");
                if (splits.length > 0) {
                    url = splits[0] + "/pom.xml";
                }
            } else if (file.contains("/src/resources")) {
                splits = file.split("/src/resources");
                if (splits.length > 0) {
                    url = splits[0] + "/pom.xml";
                }

            } else if (file.contains("pom.xml")) {
                url = file;
            } else {
                int position = file.length();
                boolean existPom = false;
                String tentativePom = file;

                while (!existPom && position > -1) {

                    position = tentativePom.lastIndexOf("/");
                    if (position > -1) {
                        tentativePom = file.substring(0, position);
                        log.debug(tentativePom + "/pom.xml");
                        existPom = checkIfPathExist(tentativePom + "/pom.xml");
                        if (existPom) {
                            url = tentativePom + "/pom.xml";
                        } else {
                            log.debug("Pom does not exist");
                        }
                    }
                }
            }

        }
        log.debug("getPomURL<-");
        return url;
    }

    private boolean checkIfPathExist(final String path) {
        final SVNClientManager manager = SVNClientManager.newInstance();
        boolean exists = false;
        try {
            SVNRepository repository = null;
            repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(repositoryURL));
            final ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(user, pass);
            repository.setAuthenticationManager(authManager);
            final SVNNodeKind nodeKind = repository.checkPath(path, -1L);
            if (nodeKind == SVNNodeKind.NONE) {
                System.err.println("There is no entry at '" + path + "'.");
            } else if (nodeKind == SVNNodeKind.FILE) {
                exists = true;
            }
            log.debug("");
        } catch (Exception e) {
            log.debug("");
        }
        return exists;
    }

    private String getFilefromSVN(final String repositoryURL, final String filePath) {
        SVNRepository repository = null;
        final String file = null;
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(repositoryURL));
            final ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(user, pass);
            repository.setAuthenticationManager(authManager);
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            final long num = repository.getFile(filePath, -1L, null, os);
            final String aString = os.toString(StandardCharsets.UTF_8);
            return aString;
        } catch (Exception e) {
            log.debug("Error getting file." + e);
            return file;
        }
    }


    private Artefact getArtefactFromString(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final Artefact artefact = new Artefact();
        try {
            final InputStream stream = new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8));
            final Model model = mavenreader.read(stream);
            artefact.setArtefactId(model.getArtifactId());
            artefact.setGroupId(model.getGroupId());
            artefact.setVersion(model.getVersion());
            if (model.getScm() != null) {
                artefact.setScmURL(model.getScm().getDeveloperConnection());
            }
        } catch (IOException | XmlPullParserException ex2) {
            log.error(ex2.toString());
        }
        return artefact;
    }


    public int checkCommit(final String[] svnFiles, final String issueKey) {
        int result = 0;
        Artefact jiraArtefactOfFile = null;
        log.info("Get the jira issue by key: " + issueKey);
        String message = "";

        JiraClient.userName = user;
        JiraClient.password = pass;

        final Artefact jiraIssueArtefact ;
        if ((jiraIssueArtefact=JiraClient.getIssueByKey(issueKey, true)) == null) {
            message = "There is not a Jira Issue in open status for the key " + issueKey;
            System.err.println(message);
            log.info(message);
            result = 3;

        } else {
            for (int i = 0; i < svnFiles.length; ++i) {
                jiraArtefactOfFile = getArtefactOfFile(repositoryURL, svnFiles[i], issueKey);
                if (jiraArtefactOfFile == null) {
                    message = "There is not a Jira Artefact for " + svnFiles[i];
                    System.err.println(message);
                    log.info(message);
                    result = 1;
                } else {
                    if (!jiraIssueArtefact.containsLinkedIssue(jiraArtefactOfFile.getJiraIssue())) {
                        result = 2;
                        message = "The issue  " + jiraIssueArtefact.getJiraIssue() + " has not linked the artefact " + jiraArtefactOfFile.getJiraIssue();
                        System.err.println(message);
                        log.info(message);
                        break;
                    }
                    result = 0;
                }
            }
        }
        SVNChecker.log.info("After call checkCommit. Result: " + result);
        return result;
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
//    private static boolean isUserAllowed(String user){
//    	boolean isAllowed = false;
//    	SVNChecker.log.debug("->  + isUserAllowed " + user);
//    	if (user.equals("alberto.garcia") || user.equals("alfonso.adiego") || user.equals("carlos.palacios")){
//    		isAllowed = true;
//    	}
//    	SVNChecker.log.debug("<-  + isUserAllowed " + isAllowed);
//    	return isAllowed;
//    }
}
