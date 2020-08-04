package com.mercurytfs.mercury.mavenreleaser.helpers;

import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtifact;
import com.mercurytfs.mercury.mavenreleaser.dto.ArtifactVersionsList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;

public class ConsoleHelper {
    public static final String USERNAME_LITERAL = "username";
    public static final String PASS_LITERAL = "password";
    public static final String ARTEFACT_NAME_LITERAL = "artefactName";
    public static final String URL_LITERAL = "url";
    public static final String ACTION_LITERAL = "action";
    public static final String VERSIONS_LITERAL = "versions";
    public static final String DRYRUN_LITERAL = "dryRun";
    private static final Logger log;

    static{
        log = LoggerFactory.getLogger(ConsoleHelper.class);
    }
    private ConsoleHelper(){
        throw new IllegalStateException("Utility class");
    }

    public static ReleaseArtifact interpretReleaseArtifact(CommandLine cmd) throws ParseException {
        ReleaseArtifact tempreleaseArtifact = new ReleaseArtifact();
        tempreleaseArtifact.setUsername((String) cmd.getParsedOptionValue(USERNAME_LITERAL));
        tempreleaseArtifact.setArtefactName((String) cmd.getParsedOptionValue(ARTEFACT_NAME_LITERAL));
        tempreleaseArtifact.setUrl(cmd.getOptionValue(URL_LITERAL));
        tempreleaseArtifact.setAction(cmd.getOptionValue(ACTION_LITERAL));
        tempreleaseArtifact.setPassword(cmd.getOptionValue(PASS_LITERAL));
        tempreleaseArtifact.setVersionsList(getVersionsList(cmd.getParsedOptionValue(VERSIONS_LITERAL)));
        tempreleaseArtifact.setDryRun(cmd.hasOption(DRYRUN_LITERAL));

        Console cnsl;
        if (tempreleaseArtifact.getPassword() == null){
            if ((cnsl = System.console()) != null)
                tempreleaseArtifact.setPassword(String.copyValueOf(cnsl.readPassword("Password: ", (Object) new String[0])));
            else
                tempreleaseArtifact.setPassword(getLineFromConsole("Type the password for " + tempreleaseArtifact.getUsername()));
        }

        return tempreleaseArtifact;
    }

    private static ArtifactVersionsList getVersionsList(Object parsedOptionValue) throws ParseException {
        ArtifactVersionsList response = new ArtifactVersionsList();
        if (!(parsedOptionValue instanceof String) || StringUtils.isEmpty((String) parsedOptionValue))
            return response;

        try {
            JAXBContext context = JAXBContext.newInstance(ArtifactVersionsList.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            response = (ArtifactVersionsList) unmarshaller.unmarshal(new File((String) parsedOptionValue));
        } catch (JAXBException e) {
            log.error("Unable to create Marshaller for Versions file " + parsedOptionValue);
            throw new ParseException("Unable to parse versions File:" + e.getMessage());
        }
        return response;
    }

    public static String getLineFromConsole(final String message) {
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


    public static Options configureArgsExtractor() {
        Options options = new Options();
        Option userNameOption = Option.builder().argName(USERNAME_LITERAL).hasArg(true).longOpt(USERNAME_LITERAL).required(true).build();
        Option urlOption = Option.builder().argName(URL_LITERAL).hasArg(true).longOpt(URL_LITERAL).required(true).build();
        Option artefactOption = Option.builder().argName(ARTEFACT_NAME_LITERAL).hasArg(true).longOpt(ARTEFACT_NAME_LITERAL).required(true).build();
        Option actionOption = Option.builder().argName(ACTION_LITERAL).hasArg(true).longOpt(ACTION_LITERAL).required(true).build();
        Option jiraOption = Option.builder().argName("jira").hasArg(true).longOpt("jira").required(false).build();
        Option password = Option.builder().argName(PASS_LITERAL).hasArg(true).longOpt(PASS_LITERAL).required(false).build();
        Option versions = Option.builder().argName(VERSIONS_LITERAL).hasArg(true).longOpt(VERSIONS_LITERAL).required(false).build();
        Option dryrun = Option.builder().argName(DRYRUN_LITERAL).hasArg(false).longOpt(DRYRUN_LITERAL).required(false).build();
        options.addOption(userNameOption);
        options.addOption(urlOption);
        options.addOption(artefactOption);
        options.addOption(actionOption);
        options.addOption(jiraOption);
        options.addOption(password);
        options.addOption(versions);
        options.addOption(dryrun);
        return options;
    }

}
