package com.mercurytfs.mercury.mavenreleaser.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
\\      //
 \\    //                       ''
  \\  //   .|''|, '||''| (''''  ||  .|''|, `||''|,
   \\//    ||..||  ||     `'')  ||  ||  ||  ||  ||
    \/     `|...  .||.   `...' .||. `|..|' .||  ||.


'||  ||`        '||`
 ||  ||          ||
 ||''||  .|''|,  ||  '||''|, .|''|, '||''|
 ||  ||  ||..||  ||   ||  || ||..||  ||
.||  ||. `|...  .||.  ||..|' `|...  .||.
                      ||
                     .||
 */
public class NewVersionHelper {
    private static final Logger log;
    public static final String SNAPSHOT_LITERAL = "-SNAPSHOT";
    static{
        log = LoggerFactory.getLogger(ConsoleHelper.class);
    }
    private NewVersionHelper(){
        throw new IllegalStateException("Utility class");
    }

    public static String getNextVersion(String version, String branchName) {
        log.debug("-->getNextVersion");
        String nextVersion = "";
        log.debug("Current Version: " + version);

        version = version.replace("-SNAPSHOT","");
        if (isValidVersion(version)){
            String[] versions = version.split("\\.");
            Integer majorVersion = Integer.valueOf(versions[0]);
            Integer middleVersion = Integer.valueOf(versions[1]);
            Integer minorVersion = Integer.valueOf(versions[2]);

            if (minorVersion>0){
                log.debug("Minor version advanced");
                minorVersion++;
            }else {
                log.debug("Middle version advanced");
                middleVersion++;
            }
            nextVersion = majorVersion.toString()+"."+middleVersion.toString()+"."+minorVersion.toString();
        }
        /*try {
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
        */

        if (!nextVersion.equals("")) {
            nextVersion = nextVersion + SNAPSHOT_LITERAL;
        }

        log.debug("New Version " + nextVersion);
        log.debug("<--getNextVersion");
        return nextVersion;
    }

    private static boolean isValidVersion(String possiblePair){
        return possiblePair.matches("[0-9]+\\.[0-9]+\\.[0-9]+");
    }
    private static String incrementMiddle(final String version) {
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
