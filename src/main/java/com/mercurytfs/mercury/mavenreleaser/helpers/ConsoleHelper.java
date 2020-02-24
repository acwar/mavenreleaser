package com.mercurytfs.mercury.mavenreleaser.helpers;

import com.mercurytfs.mercury.mavenreleaser.beans.ReleaseArtifact;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleHelper {
    public static final String USERNAME_LITERAL = "username";
    public static final String PASS_LITERAL = "password";
    public static final String ARTEFACT_NAME_LITERAL = "artefactName";
    public static final String URL_LITERAL = "url";
    public static final String ACTION_LITERAL = "action";
    private static final Logger log;

    static{
        log = LoggerFactory.getLogger(ConsoleHelper.class);
    }

    public static ReleaseArtifact interpretReleaseArtifact(CommandLine cmd) throws ParseException {
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
        options.addOption(userNameOption);
        options.addOption(urlOption);
        options.addOption(artefactOption);
        options.addOption(actionOption);
        options.addOption(jiraOption);
        options.addOption(password);
        return options;
    }
}
