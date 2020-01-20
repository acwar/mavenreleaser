package org.agrsw.mavenreleaser;

import lombok.Getter;
import org.agrsw.mavenreleaser.beans.ReleaseArtifact;
import org.agrsw.mavenreleaser.enums.ProjectsEnum;
import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.helpers.ArtifactoryHelper;
import org.agrsw.mavenreleaser.helpers.SCMMediator;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.beans.factory.annotation.*;
import org.slf4j.*;
import org.codehaus.plexus.util.xml.pull.*;
import org.agrsw.mavenreleaser.dto.RepositoryDTO;
import org.agrsw.mavenreleaser.factory.RepositoryFactory;
import org.agrsw.mavenreleaser.repository.VersionControlRepository;
import org.apache.commons.cli.*;
import org.springframework.boot.*;
import org.tmatesoft.svn.core.io.*;
import org.tmatesoft.svn.core.auth.*;
import org.apache.maven.shared.invoker.*;
import org.apache.maven.model.*;
import org.tmatesoft.svn.core.internal.io.dav.*;
import org.tmatesoft.svn.core.wc.*;


import org.tmatesoft.svn.core.*;
import org.apache.maven.model.io.xpp3.*;
import org.codehaus.plexus.util.*;

import java.io.*;
import java.io.File;
import java.util.*;

@SpringBootApplication(exclude = {EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class})
public class Releaser implements CommandLineRunner {

    @Getter
    private Logger log = LoggerFactory.getLogger((Class) Releaser.class);

    @Autowired
    private ArtifactoryHelper artifactoryHelper;
    @Autowired
    private SCMMediator scmMediator;

    @Value("${maven.home}")
    private String mavenHome;

    @Value("${notcheck.token}")
    private String notCheckToken;

    @Getter
    private ReleaseArtifact releaseArtifact;
    
    private static Map<String, String> artefacts;
    private static Map<String, String> artefactsAlreadyReleased;
    private static Map<String, String> artefactsNotInArtifactory;
    private static Map<String, Artefact> jirasNotReleased;
    private static Map<String, Artefact> jirasReleased;

    @Value("${tempDir:/tmp/svn/}")
    private String tempDir;

    private static String repoURL;

    @Value("${jira.user}")
    public String jiraUser = "";

    @Value("${jira.password}")
    public String jiraPassword = "";


    static {
        Releaser.artefacts = new HashMap<String, String>();
        Releaser.artefactsAlreadyReleased = new HashMap<String, String>();
        Releaser.artefactsNotInArtifactory = new HashMap<String, String>();
        Releaser.jirasNotReleased = new HashMap<String, Artefact>();
        Releaser.jirasReleased = new HashMap<String, Artefact>();
        Releaser.repoURL = "http://192.168.10.2/svn/mercury/";
    }

    public static void main(final String[] args) throws MavenInvocationException, FileNotFoundException, IOException, XmlPullParserException {
        SpringApplication.run((Object) Releaser.class, args);
    }

    @Override
    public void run(final String... args) {
        log.debug("Start Releasing..");
        log.debug("Maven Home: " + mavenHome);
        log.debug("NotCheck Token: " + notCheckToken);
        final Options options = configureArgsExtractor();
        try {
            final CommandLine cmd = new DefaultParser().parse(options, args);
            releaseArtifact = interpretReleaseArtifact(cmd);
            artifactoryHelper.setReleaseArtifact(releaseArtifact);
            scmMediator.setReleaseArtifact(releaseArtifact);
            log.debug(cmd.toString());
        } catch (ParseException e1) {
            e1.printStackTrace();
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar nombrejar.jar", options);
            System.exit(-1);
        }
        try {
            if (releaseArtifact.getAction().equals("release")) {
                doRelease(releaseArtifact.getUrl(), String.valueOf(releaseArtifact.getArtefactName()) + "-" + System.currentTimeMillis());
                logArtifacts("######################## Artefactos encontrados:  #############################################################", Releaser.artefacts.keySet());
                logArtifacts("######################## Artefactos que ya estaban releseados:  ###############################################", Releaser.artefactsAlreadyReleased.keySet());
                logArtifacts("######################## Artefactos que no est\u00e1n en Artifactory (hacer clean deploy):  ###################", Releaser.artefactsNotInArtifactory.keySet());
                logArtifacts("######################## Artefactos jiras que se procesaron correctamente  ####################################", Releaser.jirasReleased.keySet());
                logArtifacts("######################## Artefactos jiras que NO se procesaron correctamente  #################################", Releaser.jirasNotReleased.keySet());
            } else if (releaseArtifact.getAction().equals("prepare")) {
                doPrepare(releaseArtifact.getUrl(), String.valueOf(releaseArtifact.getArtefactName()) + "-" + System.currentTimeMillis());
                logArtifacts("######################## Artefactos encontrados:  #############################################################", Releaser.artefacts.keySet());
            } else if (releaseArtifact.getAction().equals("sources")) {
                doSources(releaseArtifact.getUrl(), String.valueOf(releaseArtifact.getArtefactName()) + "-" + System.currentTimeMillis());
                logArtifacts("######################## Artefactos encontrados:  #############################################################", Releaser.artefacts.keySet());
            }
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
        } catch (IOException e3) {
            e3.printStackTrace();
        } catch (XmlPullParserException e4) {
            e4.printStackTrace();
        } catch (MavenInvocationException e5) {
            e5.printStackTrace();
        } catch (ReleaserException e) {
            e.printStackTrace();
        }
    }
    private void logArtifacts(String mensaje,Collection<String> values){
        log.info(mensaje);
        for (String artefact : values) {
            log.info(artefact);
        }
    }
    private ReleaseArtifact interpretReleaseArtifact(CommandLine cmd) throws ParseException {
        ReleaseArtifact tempreleaseArtifact = new ReleaseArtifact();
        tempreleaseArtifact.setUsername((String) cmd.getParsedOptionValue("username"));
        tempreleaseArtifact.setArtefactName((String) cmd.getParsedOptionValue("artefactName"));
        tempreleaseArtifact.setUrl(cmd.getOptionValue("url"));
        tempreleaseArtifact.setAction(cmd.getOptionValue("action"));
        tempreleaseArtifact.setPassword(cmd.getOptionValue("password"));

        Console cnsl;
        if (tempreleaseArtifact.getPassword() == null){
            if ((cnsl = System.console()) != null)
                tempreleaseArtifact.setPassword(String.copyValueOf(cnsl.readPassword("Password: ", new Object[0])));
            else
                tempreleaseArtifact.setPassword(getLineFromConsole("Type the password for " + tempreleaseArtifact.getUsername()));
        }
        
        return tempreleaseArtifact;
    }

    private String getLineFromConsole(final String message) {
        String line = "";
        log.info(message);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            line = reader.readLine();
        } catch (IOException e) {
            log.error(e.toString());
        }
        return line;
    }


    private Options configureArgsExtractor() {
        Options options = new Options();
        Option userNameOption = Option.builder().argName("username").hasArg(true).longOpt("username").required(true).build();
        Option urlOption = Option.builder().argName("url").hasArg(true).longOpt("url").required(true).build();
        Option artefactOption = Option.builder().argName("artefactName").hasArg(true).longOpt("artefactName").required(true).build();
        Option actionOption = Option.builder().argName("action").hasArg(true).longOpt("action").required(true).build();
        Option jiraOption = Option.builder().argName("jira").hasArg(true).longOpt("jira").required(false).build();
        Option password = Option.builder().argName("password").hasArg(true).longOpt("password").required(false).build();
        options.addOption(userNameOption);
        options.addOption(urlOption);
        options.addOption(artefactOption);
        options.addOption(actionOption);
        options.addOption(jiraOption);
        options.addOption(password);
        return options;
    }

    
    private boolean checkIfPathExist(final String path) {
        final SVNClientManager manager = SVNClientManager.newInstance();
        boolean exists = false;
        try {
            SVNRepository repository = null;
            repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(Releaser.repoURL));
            final ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(releaseArtifact.getUsername(), releaseArtifact.getPassword());
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

    private void doRelease(final String url, final String artefactName) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        final String path = tempDir + artefactName;
        log.info("--> #################### Release Started for Artefact " + artefactName);
        //downloadProject(url, new File(path));
        scmMediator.downloadProject(url, new File(path));
        log.info("Artefact Info :" + getArtifactInfo(path + "/pom.xml"));
        processPomRelease(path + "/pom.xml");
        mavenInvoker(path + "/pom.xml");
        log.info("<-- #################### Release Finished for Artefact " + artefactName);
    }

    private void doSources(final String url, final String artefactName) throws IOException, XmlPullParserException, MavenInvocationException {
        final String path = tempDir + artefactName;
        log.info("--> ######## Prepare Started for Artefact " + artefactName);
        downloadProject(url, new File(path));
        processPomSources(path + "/pom.xml");
        log.info("<-- ######## Prepare Finished for Artefact " + artefactName);
    }

    private void mavenInvoker(final String pom) throws MavenInvocationException {
        final InvocationRequest request = new DefaultInvocationRequest();
        log.debug("Before calling getArtefact");
        final Artefact artefact = getArtefactFromFile(pom);

        log.info("Current Version : " + artefact.getVersion());
        final String autoVersion = getNextVersion(artefact.getVersion(), artefact.getScmURL());
        String nextVersion = getLineFromConsole("Type the new version (" + autoVersion + "): ");
        if (nextVersion.equals("")) {
            nextVersion = autoVersion;
        }
        if (!nextVersion.endsWith("-SNAPSHOT")) {
            log.warn("Next Version has not -SNAPSHOT SUFFIX. Adding...");
            nextVersion = String.valueOf(nextVersion) + "-SNAPSHOT";
        }
        request.setPomFile(new File(pom));
        final List<String> goals = new ArrayList<String>();
        goals.add("release:prepare");
        goals.add("release:perform");
        request.setGoals((List) goals);

        final Properties properties = new Properties();
        properties.put("username", releaseArtifact.getUsername());
        properties.put("password", releaseArtifact.getPassword());
        properties.put("arguments", "-DskipTests -Dmaven.javadoc.skip=true ");
        properties.put("developmentVersion", nextVersion);
        request.setProperties(properties);

        final Invoker invoker = (Invoker) new DefaultInvoker();
        invoker.setInputStream(System.in);
        invoker.setMavenHome(new File(mavenHome));
        final InvocationResult result = invoker.execute(request);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Build failed.");
        }
        String version = artefact.getVersion();
        if (version.endsWith("-SNAPSHOT")) {
            final int snapshotPosition = version.indexOf("-SNAPSHOT");
            version = version.substring(0, snapshotPosition);
        }
        if (nextVersion.endsWith("-SNAPSHOT")) {
            final int snapshotPosition = nextVersion.indexOf("-SNAPSHOT");
            nextVersion = nextVersion.substring(0, snapshotPosition);
        }
        final String project = ProjectsEnum.getProjectNameFromGroupId(artefact.getGroupId());
        if (!project.equals("")) {
            final Artefact arti = JiraClient.getIssueKey(project, artefact.getArtefactId(), version);
            if (arti == null) {
                Releaser.jirasNotReleased.put(String.valueOf(artefact.getGroupId()) + artefact.getArtefactId(), artefact);
                log.error("Cannot find jira issue for artefact: " + artefact);
            } else {
                JiraClient.createVersion(project, nextVersion, nextVersion);
                final String newIssue = JiraClient.createIssue(project, String.valueOf(artefact.getArtefactId()) + "-" + nextVersion, arti.getDescription(), artefact.getArtefactId(), version, nextVersion);
                if (newIssue == null) {
                    Releaser.jirasNotReleased.put(String.valueOf(artefact.getGroupId()) + artefact.getArtefactId(), artefact);
                    log.error("Cannot create jira issue for artefact: " + artefact);
                } else {
                    final String oldIssue = JiraClient.closeIssue(arti.getJiraIssue());
                    if (oldIssue == null) {
                        Releaser.jirasNotReleased.put(String.valueOf(artefact.getGroupId()) + artefact.getArtefactId(), artefact);
                        log.error("Cannot close jira issue for artefact: " + artefact);
                    } else {
                        artefact.setJiraIssue(newIssue);
                        Releaser.jirasReleased.put(String.valueOf(artefact.getGroupId()) + artefact.getArtefactId(), artefact);
                        log.error("jira issue released: " + artefact);
                    }
                }
            }
        } else {
            Releaser.jirasNotReleased.put(String.valueOf(artefact.getGroupId()) + artefact.getArtefactId(), artefact);
            log.error("Cannot determinate the project for artifact: " + artefact);
        }
    }

    private void saveToFile(final InputStream is, final String name) {
        final File targetFile = new File(name);
        OutputStream outStream = null;
        try {
            outStream = new FileOutputStream(targetFile);
            final byte[] buffer = new byte[is.available()];
            is.read(buffer);
            outStream.write(buffer);
        } catch (FileNotFoundException e) {
            log.error(e.toString());
        } catch (IOException e2) {
            log.error(e2.toString());
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e3) {
                    log.error(e3.toString());
                }
            }
        }
        if (outStream != null) {
            try {
                outStream.close();
            } catch (IOException e3) {
                log.error(e3.toString());
            }
        }
    }

    private void processPomRelease(final String file) throws FileNotFoundException, IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        log.info("-->Processing Pom " + file);
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        File pomfile = new File(file);
        Model model = mavenreader.read((Reader) new FileReader(pomfile));
        if (model.getVersion().indexOf("-SNAPSHOT") == -1) {
            log.debug("The artifact " + model.getGroupId() + "." + model.getArtifactId() + "-" + model.getVersion() + " is already a release");
            return;
        }
        List<Dependency> deps = (List<Dependency>) model.getDependencies();
        log.info("Processing dependencies...");
        String artefact = "";
        for (Dependency d : deps) {
            Model pom;
            artefact = String.valueOf(d.getGroupId()) + "." + d.getArtifactId() + "." + d.getVersion();
            log.debug(artefact);
            if (d.getVersion()!=null && d.getVersion().endsWith("SNAPSHOT")) {
                log.debug(artefact + " is in SNAPSHOT, processing...");
                log.debug("\tCheck in artifactoy if the release version of " + artefact + " already exists");
                if (artifactoryHelper.getReleasedArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion().substring(0, d.getVersion().indexOf("-SNAPSHOT"))) != null) {
                    log.debug(artefact + "\t in snapshot in the pom.xml but is already released");
                    d.setVersion(d.getVersion().substring(0, d.getVersion().indexOf("-SNAPSHOT")));
                    if (!Releaser.artefactsAlreadyReleased.containsKey(artefact))
                        Releaser.artefactsAlreadyReleased.put(artefact, artefact);
                    else
                        log.warn("\tArtefact is already in the map " + artefact);
                    continue;
                }
                log.info("\tArtifact release not found at artifactory");
                if ((pom = artifactoryHelper.getSnapshotArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion())) != null) {
                    doRelease(extractSCMUrl(pom.getScm()), String.valueOf(d.getArtifactId()) + System.currentTimeMillis());
                    d.setVersion(d.getVersion().substring(0, d.getVersion().indexOf("-SNAPSHOT")));
                    if (!Releaser.artefacts.containsKey(artefact))
                        Releaser.artefacts.put(artefact, artefact);
                     else
                        log.warn("\tArtefact is already in the map " + artefact);
                } else {
                    log.error("Artifact not found at repository");
                    if (!Releaser.artefactsNotInArtifactory.containsKey(artefact))
                        Releaser.artefactsNotInArtifactory.put(artefact, artefact);
                     else
                        log.warn("\tArtefact is already in the map " + artefact);
                }
            } else
                log.debug("The artifact is a release, skiping");
        }

        writeModel(pomfile, model);
        commit(pomfile);
        log.info("<--Processing Pom " + file);
    }


    private void doPrepare(String url, String artefactName) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        final String path = tempDir + artefactName;
        log.info("--> ######## Prepare Started for Artefact " + artefactName);
        scmMediator.downloadProject(url, new File(path));
        processPomPrepare(path + "/pom.xml");
        log.info("<-- ######## Prepare Finished for Artefact " + artefactName);
    }

    private void processPomPrepare(String file) throws FileNotFoundException, IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        log.debug("Processin Pom " + file);
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        final Model model = mavenreader.read((Reader) new FileReader(pomfile));
        if (model.getVersion().indexOf("-SNAPSHOT") == -1) {
            log.debug("The artifact " + model.getGroupId() + "." + model.getArtifactId() + "-" + model.getVersion() + " is already a release");
            return;
        }
        final List<Dependency> deps = (List<Dependency>) model.getDependencies();
        log.debug("Processing dependencies...");
        String artefact = "";
        for (Dependency d : deps) {
            Model pom;
            artefact = String.valueOf(d.getGroupId()) + "." + d.getArtifactId() + "." + d.getVersion();
            if (d.getVersion()!=null && d.getVersion().endsWith("SNAPSHOT")) {
                log.debug(artefact + " is in SNAPSHOT, processing...");
                log.debug("\tCheck in artifactoy if the release version of " + artefact + " already exists");
                if (artifactoryHelper.getReleasedArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion().substring(0, d.getVersion().indexOf("-SNAPSHOT"))) != null) {
                    continue;
                }
                log.debug("\tArtifact release not found in artifactory");
                if (!Releaser.artefacts.containsKey(artefact)) {
                    Releaser.artefacts.put(artefact, artefact);
                    log.debug("\tCheck in artifactoy if the SNAPSHOT version of " + artefact + " already exists");
                    if ((pom = artifactoryHelper.getSnapshotArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion())) != null) {
                        doPrepare(extractSCMUrl(pom.getScm()), String.valueOf(d.getArtifactId()) + System.currentTimeMillis());
                        d.setVersion(d.getVersion().substring(0, d.getVersion().indexOf("-SNAPSHOT")));
                    } else
                        log.debug("\tArtifact snapshot not found in repository");
                } else
                    log.warn("\tArtefact is already in the map " + artefact);
            } else
                log.debug(artefact + " is a release, skiping");
        }
    }
    private String extractSCMUrl(Scm scm) throws ReleaserException {
        String svnURL = (scm.getDeveloperConnection()!=null)?scm.getDeveloperConnection():scm.getConnection();
        if (svnURL==null)
            throw new ReleaserException("SCM Info not provided in POM");

        return svnURL.substring(svnURL.indexOf("http"));
    }

    private void processPomSources(final String file) throws FileNotFoundException, IOException, XmlPullParserException, MavenInvocationException {
        log.debug("Processin Pom " + file);
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        final Model model = mavenreader.read((Reader) new FileReader(pomfile));
        final List<Dependency> deps = (List<Dependency>) model.getDependencies();
        log.debug("Processing dependencies...");
        String artefact = "";
        for (final Dependency d : deps) {
            artefact = String.valueOf(d.getGroupId()) + "." + d.getArtifactId() + "." + d.getVersion();
            log.debug(String.valueOf(d.getGroupId()) + "." + d.getArtifactId() + "." + d.getVersion());
            if (d.getGroupId().startsWith("com.mercury")) {
                final InputStream is = artifactoryHelper.getArtifactSourceArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion(), true);
                if (is == null) {
                    continue;
                }
                log.debug("Artifact source  found at artifactory");
                if (!Releaser.artefacts.containsKey(artefact)) {
                    Releaser.artefacts.put(artefact, artefact);
                    saveToFile(is, "/tmp/sources/" + d.getGroupId() + "-" + d.getArtifactId() + "-" + d.getVersion() + ".jar");
                } else {
                    log.warn("Artefact is already in the map " + artefact);
                }
            } else {
                log.debug("The artifact does not belong to mercury");
            }
        }
    }

    private boolean downloadProject(final String url, final File target) {
        log.info("--> Downloading from SVN " + url);
        DAVRepositoryFactory.setup();
        final SVNClientManager manager = SVNClientManager.newInstance();
        try {
            final ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(releaseArtifact.getUsername(), releaseArtifact.getPassword());
            final SVNUpdateClient client = manager.getUpdateClient();
            manager.setAuthenticationManager(authManager);
            log.debug("Before checking out " + new Date());
            final SVNURL svnURL = SVNURL.parseURIEncoded(url);
            client.doCheckout(svnURL, target, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
            log.debug("After checking out " + new Date());
            log.debug("Artefact downloaded from SVN");
            return true;
        } catch (SVNException e) {
            log.error("Error checking out project." + e);
        } catch (Exception e2) {
            log.debug("Error checking out project." + e2);
        } finally {
            manager.dispose();
        }
        log.info("<-- Downloading from SVN " + url);
        return false;
    }



    private boolean commit(final File file) {
        DAVRepositoryFactory.setup();
        final SVNClientManager manager = SVNClientManager.newInstance();
        try {
            final ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(releaseArtifact.getUsername(), releaseArtifact.getPassword());
            manager.setAuthenticationManager(authManager);
            final SVNCommitClient svnCommitClient = manager.getCommitClient();
            svnCommitClient.doCommit(new File[]{file}, false, "Releaser " + notCheckToken, (SVNProperties) null, (String[]) null, false, false, SVNDepth.INFINITY);
        } catch (SVNException e) {
            log.debug("Error checking out project." + e);
            return false;
        } finally {
            manager.dispose();
        }
        manager.dispose();
        return false;
    }

    private String getFilefromSVN(final String repositoryURL, final String filePath) {
        SVNRepository repository = null;
        final String file = null;
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(repositoryURL));
            final ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(releaseArtifact.getUsername(), releaseArtifact.getPassword());
            repository.setAuthenticationManager(authManager);
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            final long num = repository.getFile(filePath, -1L, (SVNProperties) null, (OutputStream) os);
            final String aString = new String(os.toByteArray(), "UTF-8");
            return aString;
        } catch (Exception e) {
            log.debug("Error getting file." + e);
            return file;
        }
    }

    public Artefact getArtefactOfFile(final String repositoryURL, final String file, final String jiraIssue) {
        Artefact jiraArtefactOfFile = null;
        if (file != null) {
            final String[] splits = file.split("/src/main");
            log.debug(splits.toString());
            String url = getPomURL(file);
            if ((url != null) && (splits.length > 0)) {
                final String pom = getFilefromSVN(repositoryURL, url);
                final Artefact artefact = getArtefactFromString(pom);
                jiraArtefactOfFile = JiraClient.getIssueKey(ProjectsEnum.getProjectNameFromGroupId(artefact.getGroupId()), artefact.getArtefactId(), artefact.getVersion().substring(0, artefact.getVersion().indexOf("-SNAPSHOT")));
            }
        }
        return jiraArtefactOfFile;
    }

    private String getPomURL(String file) {
        log.debug("getPomURL->" + file);
        String[] splits = null;
        String url = null;
        if (file != null) {
            if (file.contains("/src/main")) {
                splits = file.split("/src/main");
                if (splits.length > 0) {
                    url = String.valueOf(splits[0]) + "/pom.xml";
                }
            } else if (file.contains("/src/resources")) {
                splits = file.split("/src/resources");
                if (splits.length > 0) {
                    url = String.valueOf(splits[0]) + "/pom.xml";
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

    public int checkCommit(final String[] svnFiles, final String issueKey) {
        int result = 0;
        Artefact jiraArtefactOfFile = null;
        log.info("Get the jira issue by key: " + issueKey);
        String message = "";
        final Artefact jiraIssueArtefact = JiraClient.getIssueByKey(issueKey, true);
        if (jiraIssueArtefact == null) {
            message = "There is not a Jira Issue in open status for the key " + issueKey;
            System.err.println(message);
            log.info(message);

            result = 3;
        } else {
            for (int i = 0; i < svnFiles.length; ++i) {
                jiraArtefactOfFile = getArtefactOfFile(Releaser.repoURL, svnFiles[i], issueKey);
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

        return result;
    }

    public void writeModel(final File pomFile, final Model model) throws IOException {
        Writer writer = null;
        try {
            writer = new FileWriter(pomFile);
            final MavenXpp3Writer pomWriter = new MavenXpp3Writer();
            pomWriter.write(writer, model);
        } finally {
            IOUtil.close(writer);
        }
        IOUtil.close(writer);
    }

    private String getArtifactInfo(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        String artefactInfo = "";
        try {
            final Model model = mavenreader.read((Reader) new FileReader(pomfile));
            artefactInfo = String.valueOf(model.getGroupId()) + "." + model.getArtifactId() + "-" + model.getVersion();
        } catch (IOException | XmlPullParserException ex2) {

            log.error(ex2.toString());
        }
        return artefactInfo;
    }

    private String getArtefactVersion(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        String version = "";
        try {
            final Model model = mavenreader.read((Reader) new FileReader(pomfile));
            version = model.getVersion();
        } catch (IOException | XmlPullParserException ex2) {
            log.error(ex2.toString());
        }
        return version;
    }

    private Artefact getArtefactFromFile(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final Artefact artefact = new Artefact();
        try {
            File pomfile = new File(file);

            InputStreamReader fis = new InputStreamReader(new FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8);
            final Model model = mavenreader.read(fis);
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

    private Artefact getArtefactFromString(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final Artefact artefact = new Artefact();
        try {
            final InputStream stream = new ByteArrayInputStream(file.getBytes("UTF-8"));
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

    private String getArtefactSCMURL(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        String url = "";
        try {
            final Model model = mavenreader.read((Reader) new FileReader(pomfile));
            url = model.getScm().getDeveloperConnection();
        } catch (IOException | XmlPullParserException ex2) {
            log.error(ex2.toString());
        }
        return url;
    }


    private String getNextVersion(String version, String branchName) {
        log.debug("-->getNextVersion");
        String nextVersion = "";
        log.debug("Current Version: " + version);
        log.debug("BranchName: " + branchName);
        try {
            if (version.endsWith("-SNAPSHOT")) {
                int snapshotPosition = version.indexOf("-SNAPSHOT");
                version = version.substring(0, snapshotPosition);
            }
            if (branchName.endsWith("-SNAPSHOT")) {
                int snapshotPosition = branchName.indexOf("-SNAPSHOT");
                branchName = branchName.substring(0, snapshotPosition);
            }
            branchName = new StringBuilder(branchName).reverse().toString();
            int index = branchName.indexOf("-");
            if (index == -1) {
                nextVersion = incrementMiddle(version);
            } else {
                branchName = branchName.substring(0, index);
                branchName = new StringBuilder(branchName).reverse().toString();
                int position = branchName.toUpperCase().indexOf("X");
                if (position > -1) {
                    if (position == 2) {
                        int position2 = version.indexOf(".", position + 1);
                        Integer num = Integer.valueOf(version.substring(position, position2));
                        num = Integer.valueOf(num.intValue() + 1);
                        nextVersion = String.valueOf(version.substring(0, position)) + num;
                        nextVersion = String.valueOf(nextVersion) + version.substring(position2, version.length());
                    }
                    if ((position == 4) || (position == 5)) {
                        int position2 = version.indexOf(".", position);
                        position = (position2 > -1) ? position2 + 1 : position;
                        Integer num = Integer.valueOf(version.substring(position, version.length()));
                        num = Integer.valueOf(num.intValue() + 1);
                        nextVersion = String.valueOf(version.substring(0, position)) + num;
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.toString());
            log.info("The Next Version could not be discover automatically");
            nextVersion = "";
        }
        if (!nextVersion.equals("")) {
            nextVersion = String.valueOf(nextVersion) + "-SNAPSHOT";
        }

        log.debug("New Version " + nextVersion);
        log.debug("<--getNextVersion");
        return nextVersion;
    }


    private String incrementMiddle(final String version) {
        log.debug("-->getNextVersion");
        String newVersion = "";
        try {
            final int position = version.indexOf(".");
            final int position2 = version.indexOf(".", position + 1);
            if (position == 1) {
                Integer num = Integer.valueOf(version.substring(position + 1, position2));
                ++num;
                newVersion = String.valueOf(version.substring(0, position)) + "." + num;
                newVersion = String.valueOf(newVersion) + version.substring(position2, version.length());
            }
        } catch (Exception e) {
            log.error("Error incrementing version of: " + version);
        }
        log.debug("<--getNextVersion");
        return newVersion;
    }

    public String getToken() {
        String notcheckTokenProperty = null;
        Properties prop = new Properties();
        try {
            prop.load(Releaser.class.getClassLoader().getResourceAsStream("config.properties"));
            notcheckTokenProperty = prop.getProperty("notchecktoken");
        } catch (IOException e) {
            log.error(e.toString());
        }
        return notcheckTokenProperty;

    }
}
