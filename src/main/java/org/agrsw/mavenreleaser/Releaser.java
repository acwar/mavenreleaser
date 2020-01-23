package org.agrsw.mavenreleaser;

import lombok.Getter;
import org.agrsw.mavenreleaser.beans.ReleaseArtifact;
import org.agrsw.mavenreleaser.enums.ProjectsEnum;
import org.agrsw.mavenreleaser.exception.ReleaserException;
import org.agrsw.mavenreleaser.helpers.ArtifactoryHelper;
import org.agrsw.mavenreleaser.helpers.SCMMediator;
import org.apache.commons.cli.*;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;

import java.io.*;
import java.util.*;

//TODO Unifica el dascargador y buscador de artefactos, el metodo recursivo

@SpringBootApplication(exclude = {EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class})
public class Releaser implements CommandLineRunner {

    public static final String ARTEFACTOS_ENCONTRADOS = "######################## Artefactos encontrados:  #############################################################";
    public static final String ARTEFACTOS_QUE_YA_ESTABAN_RELESEADOS = "######################## Artefactos que ya estaban releseados:  ###############################################";
    public static final String ARTEFACTOS_QUE_NO_ESTAN_EN_ARTIFACTORY_HACER_CLEAN_DEPLOY = "######################## Artefactos que no est\u00e1n en Artifactory (hacer clean deploy):  ###################";
    public static final String ARTEFACTOS_JIRAS_QUE_SE_PROCESARON_CORRECTAMENTE = "######################## Artefactos jiras que se procesaron correctamente  ####################################";
    public static final String ARTEFACTOS_JIRAS_QUE_NO_SE_PROCESARON_CORRECTAMENTE = "######################## Artefactos jiras que NO se procesaron correctamente  #################################";
    public static final String ARTEFACT_NAME_LITERAL = "artefactName";
    public static final String USERNAME_LITERAL = "username";
    public static final String URL_LITERAL = "url";
    public static final String ACTION_LITERAL = "action";
    public static final String PASS_LITERAL = "password";
    public static final String POM_XML_LITERAL = "/pom.xml";
    public static final String SNAPSHOT_LITERAL = "-SNAPSHOT";
    public static final String PROCESSING_DEPENDENCIES = "Processing dependencies...";
    public static final String PROCESSING_POM_INPUT = "-->Processing Pom ";
    public static final String PROCESSING_POM_OUTPUT = "<--Processing Pom ";
    public static final String ARTEFACT_IS_ALREADY_IN_THE_MAP = "\tArtefact is already in the map ";
    public static final String ALREADY_EXISTS = " already exists";
    @Getter
    private Logger log = LoggerFactory.getLogger(Releaser.class);

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

    @Value("${jira.user}")
    public String jiraUser = "";

    @Value("${jira.password}")
    public String jiraPassword = "";


    static {
        Releaser.artefacts = new HashMap<>();
        Releaser.artefactsAlreadyReleased = new HashMap<>();
        Releaser.artefactsNotInArtifactory = new HashMap<>();
        Releaser.jirasNotReleased = new HashMap<>();
        Releaser.jirasReleased = new HashMap<>();
    }

    public static void main(final String[] args) {
        SpringApplication.run(Releaser.class, args);
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
            log.debug(e1.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar nombrejar.jar", options);
            System.exit(-1);
        }
        try {
            switch (releaseArtifact.getAction()) {
                case "release":
                    doRelease(releaseArtifact.getUrl(), releaseArtifact.getArtefactName() + "-" + System.currentTimeMillis());
                    logArtifacts(ARTEFACTOS_ENCONTRADOS, Releaser.artefacts.keySet());
                    logArtifacts(ARTEFACTOS_QUE_YA_ESTABAN_RELESEADOS, Releaser.artefactsAlreadyReleased.keySet());
                    logArtifacts(ARTEFACTOS_QUE_NO_ESTAN_EN_ARTIFACTORY_HACER_CLEAN_DEPLOY, Releaser.artefactsNotInArtifactory.keySet());
                    logArtifacts(ARTEFACTOS_JIRAS_QUE_SE_PROCESARON_CORRECTAMENTE, Releaser.jirasReleased.keySet());
                    logArtifacts(ARTEFACTOS_JIRAS_QUE_NO_SE_PROCESARON_CORRECTAMENTE, Releaser.jirasNotReleased.keySet());
                    break;
                case "prepare":
                    doPrepare(releaseArtifact.getUrl(), releaseArtifact.getArtefactName() + "-" + System.currentTimeMillis());
                    logArtifacts(ARTEFACTOS_ENCONTRADOS, Releaser.artefacts.keySet());
                    break;
                case "sources":
                    doSources(releaseArtifact.getUrl(), releaseArtifact.getArtefactName() + "-" + System.currentTimeMillis());
                    logArtifacts(ARTEFACTOS_ENCONTRADOS, Releaser.artefacts.keySet());
                    break;
                default:
                    log.info("No compatible action provided. User release/prepare/sources");
            }
        } catch (IOException | XmlPullParserException | MavenInvocationException | ReleaserException e1) {
            log.debug(e1.getMessage());
            e1.printStackTrace();
        }
    }
    private void logArtifacts(String mensaje,Collection<String> values){
        log.info(mensaje);
        for (String artefact : values)
            log.info(artefact);
    }
    private ReleaseArtifact interpretReleaseArtifact(CommandLine cmd) throws ParseException {
        ReleaseArtifact tempreleaseArtifact = new ReleaseArtifact();
        tempreleaseArtifact.setUsername((String) cmd.getParsedOptionValue(USERNAME_LITERAL));
        tempreleaseArtifact.setArtefactName((String) cmd.getParsedOptionValue(ARTEFACT_NAME_LITERAL));
        tempreleaseArtifact.setUrl(cmd.getOptionValue(URL_LITERAL));
        tempreleaseArtifact.setAction(cmd.getOptionValue(ACTION_LITERAL));
        tempreleaseArtifact.setPassword(cmd.getOptionValue(PASS_LITERAL));

        Console cnsl;
        if (tempreleaseArtifact.getPassword() == null){
            if ((cnsl = System.console()) != null)
                tempreleaseArtifact.setPassword(String.copyValueOf(cnsl.readPassword("Password: ", (Object) new String[0])));
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
        Option userNameOption = Option.builder().argName(USERNAME_LITERAL).hasArg(true).longOpt(USERNAME_LITERAL).required(true).build();
        Option urlOption = Option.builder().argName(URL_LITERAL).hasArg(true).longOpt(URL_LITERAL).required(true).build();
        Option artefactOption = Option.builder().argName(ARTEFACT_NAME_LITERAL).hasArg(true).longOpt(ARTEFACT_NAME_LITERAL).required(true).build();
        Option actionOption = Option.builder().argName(ACTION_LITERAL).hasArg(true).longOpt(ACTION_LITERAL).required(true).build();
        Option jiraOption = Option.builder().argName("jira").hasArg(true).longOpt("jira").required(false).build();
        Option password = Option.builder().argName(PASS_LITERAL).hasArg(true).longOpt(PASS_LITERAL).required(false).build();
        options.addOption(userNameOption);
        options.addOption(urlOption);
        options.addOption(artefactOption);
        options.addOption(actionOption);
        options.addOption(jiraOption);
        options.addOption(password);
        return options;
    }


    private void doRelease(final String url, final String artefactName) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        final String path = tempDir + artefactName;
        log.info("--> #################### Release Started for Artefact " + artefactName);
        scmMediator.downloadProject(url, new File(path));
        log.info("Artefact Info :" + getArtifactInfo(path + POM_XML_LITERAL));
        processPomRelease(path + POM_XML_LITERAL);
        log.info("Maven Release for " + getArtifactInfo(path + POM_XML_LITERAL));
        mavenInvoker(path + POM_XML_LITERAL);
        log.info("<-- #################### Release Finished for Artefact " + artefactName);
    }

    private void doSources(final String url, final String artefactName) throws IOException, XmlPullParserException, ReleaserException {
        final String path = tempDir + artefactName;
        log.info("--> ######## Prepare Started for Artefact " + artefactName);
        scmMediator.downloadProject(url, new File(path));
        processPomSources(path + POM_XML_LITERAL);
        log.info("<-- ######## Prepare Finished for Artefact " + artefactName);
    }

    private void mavenInvoker(final String pom) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        Artefact artefact = getArtefactFromFile(pom);

        log.info("Current Version : " + artefact.getVersion());
        final String autoVersion = getNextVersion(artefact.getVersion(), artefact.getScmURL());
        String nextVersion = getLineFromConsole("Type the new version (" + autoVersion + "): ");
        if (nextVersion.equals("")) {
            nextVersion = autoVersion;
        }
        if (!nextVersion.endsWith(SNAPSHOT_LITERAL)) {
            log.warn("Next Version has not -SNAPSHOT SUFFIX. Adding...");
            nextVersion = nextVersion + SNAPSHOT_LITERAL;
        }
        request.setPomFile(new File(pom));
        final List<String> goals = new ArrayList<>();
        goals.add("release:prepare");
        goals.add("release:perform");
        request.setGoals(goals);

        final Properties properties = new Properties();
        properties.put(USERNAME_LITERAL, releaseArtifact.getUsername());
        properties.put(PASS_LITERAL, releaseArtifact.getPassword());
        properties.put("arguments", "-DskipTests -Dmaven.javadoc.skip=true -U ");
        properties.put("developmentVersion", nextVersion);
        request.setProperties(properties);

        final Invoker invoker = new DefaultInvoker();
        invoker.setInputStream(System.in);
        invoker.setMavenHome(new File(mavenHome));
        final InvocationResult result = invoker.execute(request);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Build failed.");
        }
        String version = artefact.getVersion();
        if (version.endsWith(SNAPSHOT_LITERAL)) {
            final int snapshotPosition = version.indexOf(SNAPSHOT_LITERAL);
            version = version.substring(0, snapshotPosition);
        }
        if (nextVersion.endsWith(SNAPSHOT_LITERAL)) {
            final int snapshotPosition = nextVersion.indexOf(SNAPSHOT_LITERAL);
            nextVersion = nextVersion.substring(0, snapshotPosition);
        }
        final String project = ProjectsEnum.getProjectNameFromGroupId(artefact.getGroupId());
        if (!project.equals("")) {
            final Artefact arti = JiraClient.getIssueKey(project, artefact.getArtefactId(), version);
            if (arti == null) {
                Releaser.jirasNotReleased.put(artefact.getGroupId() + artefact.getArtefactId(), artefact);
                log.error("Cannot find jira issue for artefact: " + artefact);
            } else {
                JiraClient.createVersion(project, nextVersion, nextVersion);
                final String newIssue = JiraClient.createIssue(project, artefact.getArtefactId() + "-" + nextVersion, arti.getDescription(), artefact.getArtefactId(), version, nextVersion);
                if (newIssue == null) {
                    Releaser.jirasNotReleased.put(artefact.getGroupId() + artefact.getArtefactId(), artefact);
                    log.error("Cannot create jira issue for artefact: " + artefact);
                } else {
                    final String oldIssue = JiraClient.closeIssue(arti.getJiraIssue());
                    if (oldIssue == null) {
                        Releaser.jirasNotReleased.put(artefact.getGroupId() + artefact.getArtefactId(), artefact);
                        log.error("Cannot close jira issue for artefact: " + artefact);
                    } else {
                        artefact.setJiraIssue(newIssue);
                        Releaser.jirasReleased.put(artefact.getGroupId() + artefact.getArtefactId(), artefact);
                        log.error("jira issue released: " + artefact);
                    }
                }
            }
        } else {
            Releaser.jirasNotReleased.put(artefact.getGroupId() + artefact.getArtefactId(), artefact);
            log.error("Cannot determinate the project for artifact: " + artefact);
        }
    }

    private void saveToFile(final InputStream is, final String name) {
        final File targetFile = new File(name);
        try (
           OutputStream outStream = new FileOutputStream(targetFile)
        ){
            final byte[] buffer = new byte[is.available()];
            while (is.read(buffer) > 0)
                outStream.write(buffer);
        } catch (IOException e) {
            log.error(e.toString());
        }

    }

    private void processPomRelease(final String file) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        log.info(PROCESSING_POM_INPUT + file);
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        File pomfile = new File(file);
        Model model = mavenreader.read(new FileReader(pomfile));
        if (!model.getVersion().contains(SNAPSHOT_LITERAL)) {
            log.debug("The artifact " + model.getGroupId() + "." + model.getArtifactId() + "-" + model.getVersion() + " is already a release");
            return;
        }
        List<Dependency> deps = model.getDependencies();
        log.info(PROCESSING_DEPENDENCIES);
        String artefact;
        for (Dependency d : deps) {
            Model pom;
            artefact = d.getGroupId() + "." + d.getArtifactId() + "." + d.getVersion();
            log.debug(artefact);
            if (d.getVersion()!=null && d.getVersion().endsWith("SNAPSHOT")) {
                log.debug(artefact + " is in SNAPSHOT, processing...");
                log.debug("\tCheck in artifactoy if the release version of " + artefact + ALREADY_EXISTS);
                if (artifactoryHelper.getReleasedArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion().substring(0, d.getVersion().indexOf(SNAPSHOT_LITERAL))) != null) {
                    log.debug(artefact + "\t in snapshot in the pom.xml but is already released");
                    d.setVersion(d.getVersion().substring(0, d.getVersion().indexOf(SNAPSHOT_LITERAL)));
                    if (!Releaser.artefactsAlreadyReleased.containsKey(artefact))
                        Releaser.artefactsAlreadyReleased.put(artefact, artefact);
                    else
                        log.warn(ARTEFACT_IS_ALREADY_IN_THE_MAP + artefact);
                    continue;
                }
                log.info("\tArtifact release not found at artifactory");
                if ((pom = artifactoryHelper.getSnapshotArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion())) != null) {
                    doRelease(extractSCMUrl(pom.getScm()), String.valueOf(d.getArtifactId()) + System.currentTimeMillis());
                    d.setVersion(d.getVersion().substring(0, d.getVersion().indexOf(SNAPSHOT_LITERAL)));
                    if (!Releaser.artefacts.containsKey(artefact))
                        Releaser.artefacts.put(artefact, artefact);
                     else
                        log.warn(ARTEFACT_IS_ALREADY_IN_THE_MAP + artefact);
                } else {
                    log.error("Artifact not found at repository");
                    if (!Releaser.artefactsNotInArtifactory.containsKey(artefact))
                        Releaser.artefactsNotInArtifactory.put(artefact, artefact);
                     else
                        log.warn(ARTEFACT_IS_ALREADY_IN_THE_MAP + artefact);
                }
            } else
                log.debug("The artifact is a release, skiping");
        }
        /**
         * Is this really needed??
         */
        writeModel(pomfile, model);
        scmMediator.commitFile(extractSCMUrl(model.getScm()),pomfile);
        log.info(PROCESSING_POM_OUTPUT + file);
    }


    private void doPrepare(String url, String artefactName) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        final String path = tempDir + artefactName;
        log.info("--> ######## Prepare Started for Artefact " + artefactName);
        scmMediator.downloadProject(url, new File(path));
        processPomPrepare(path + POM_XML_LITERAL);
        log.info("<-- ######## Prepare Finished for Artefact " + artefactName);
    }

    private void processPomPrepare(String file) throws IOException, XmlPullParserException, MavenInvocationException, ReleaserException {
        log.debug(PROCESSING_POM_INPUT + file);
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        final Model model = mavenreader.read(new FileReader(pomfile));
        if (!model.getVersion().contains(SNAPSHOT_LITERAL)) {
            log.debug("The artifact " + model.getGroupId() + "." + model.getArtifactId() + "-" + model.getVersion() + " is already a release");
            return;
        }
        final List<Dependency> deps = model.getDependencies();
        log.debug(PROCESSING_DEPENDENCIES);
        String artefact;
        for (Dependency d : deps) {
            Model pom;
            artefact = d.getGroupId() + "." + d.getArtifactId() + "." + d.getVersion();
            if (d.getVersion()!=null && d.getVersion().endsWith("SNAPSHOT")) {
                log.debug(artefact + " is in SNAPSHOT, processing...");
                log.debug("\tCheck in artifactoy if the release version of " + artefact + ALREADY_EXISTS);
                if (artifactoryHelper.getReleasedArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion().substring(0, d.getVersion().indexOf(SNAPSHOT_LITERAL))) != null) {
                    continue;
                }
                log.debug("\tArtifact release not found in artifactory");
                if (!Releaser.artefacts.containsKey(artefact)) {
                    Releaser.artefacts.put(artefact, artefact);
                    log.debug("\tCheck in artifactoy if the SNAPSHOT version of " + artefact + ALREADY_EXISTS);
                    if ((pom = artifactoryHelper.getSnapshotArtifactFromArtifactory(d.getGroupId(), d.getArtifactId(), d.getVersion())) != null) {
                        doPrepare(extractSCMUrl(pom.getScm()), String.valueOf(d.getArtifactId()) + System.currentTimeMillis());
                        d.setVersion(d.getVersion().substring(0, d.getVersion().indexOf(SNAPSHOT_LITERAL)));
                    } else
                        log.debug("\tArtifact snapshot not found in repository");
                } else
                    log.warn(ARTEFACT_IS_ALREADY_IN_THE_MAP + artefact);
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

    private void processPomSources(final String file) throws IOException, XmlPullParserException {
        log.debug("Processin Pom " + file);
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        final Model model = mavenreader.read(new FileReader(pomfile));
        final List<Dependency> deps = model.getDependencies();
        log.debug(PROCESSING_DEPENDENCIES);
        String artefact;
        for (final Dependency d : deps) {
            artefact = d.getGroupId() + "." + d.getArtifactId() + "." + d.getVersion();
            log.debug(d.getGroupId() + "." + d.getArtifactId() + "." + d.getVersion());
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

    public void writeModel(final File pomFile, final Model model) throws IOException {
        try (Writer writer = new FileWriter(pomFile)){
            final MavenXpp3Writer pomWriter = new MavenXpp3Writer();
            pomWriter.write(writer, model);
        }
    }

    private String getArtifactInfo(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final File pomfile = new File(file);
        String artefactInfo = "";
        try {
            final Model model = mavenreader.read(new FileReader(pomfile));
            artefactInfo = model.getGroupId() + "." + model.getArtifactId() + "-" + model.getVersion();
        } catch (IOException | XmlPullParserException ex2) {

            log.error(ex2.toString());
        }
        return artefactInfo;
    }

    private Artefact getArtefactFromFile(final String file) {
        final MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        final Artefact artefact = new Artefact();
        try {
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

    private String getNextVersion(String version, String branchName) {
        log.debug("-->getNextVersion");
        String nextVersion = "";
        log.debug("Current Version: " + version);
        log.debug("BranchName: " + branchName);
        try {
            if (version.endsWith(SNAPSHOT_LITERAL)) {
                int snapshotPosition = version.indexOf(SNAPSHOT_LITERAL);
                version = version.substring(0, snapshotPosition);
            }
            if (branchName.endsWith(SNAPSHOT_LITERAL)) {
                int snapshotPosition = branchName.indexOf(SNAPSHOT_LITERAL);
                branchName = branchName.substring(0, snapshotPosition);
            }
            branchName = new StringBuilder(branchName).reverse().toString();
            int index = branchName.indexOf('-');
            if (index == -1) {
                nextVersion = incrementMiddle(version);
            } else {
                branchName = branchName.substring(0, index);
                branchName = new StringBuilder(branchName).reverse().toString();
                int position = branchName.toUpperCase().indexOf('X');
                if (position > -1) {
                    if (position == 2) {
                        int position2 = version.indexOf('.', position + 1);
                        int num = Integer.parseInt(version.substring(position, position2));
                        num++;
                        nextVersion = version.substring(0, position) + num;
                        nextVersion = nextVersion + version.substring(position2);
                    }
                    if ((position == 4) || (position == 5)) {
                        int position2 = version.indexOf('.', position);
                        position = (position2 > -1) ? position2 + 1 : position;
                        int num = Integer.parseInt(version.substring(position));
                        num++;
                        nextVersion = version.substring(0, position) + num;
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.toString());
            log.info("The Next Version could not be discover automatically");
            nextVersion = "";
        }
        if (!nextVersion.equals("")) {
            nextVersion = nextVersion + SNAPSHOT_LITERAL;
        }

        log.debug("New Version " + nextVersion);
        log.debug("<--getNextVersion");
        return nextVersion;
    }


    private String incrementMiddle(final String version) {
        log.debug("-->getNextVersion");
        String newVersion = "";
        try {
            final int position = version.indexOf('.');
            final int position2 = version.indexOf('.', position + 1);
            if (position == 1) {
                int num = Integer.parseInt(version.substring(position + 1, position2));
                ++num;
                newVersion = version.substring(0, position) + "." + num;
                newVersion = newVersion + version.substring(position2);
            }
        } catch (Exception e) {
            log.error("Error incrementing version of: " + version);
        }
        log.debug("<--getNextVersion");
        return newVersion;
    }

}
