package sp.sd.flywayrunner.builder;

import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;


public class Util {

    private static final String ERROR_STRING = "Errors:";
    static final String OPTION_HYPHENS = "-";

    private Util() {
    }

    /**
     * Simple routine to check for the string "Errors:" in a file.
     * @param logFile file to scan.
     * @return true if {@link #ERROR_STRING} appears in file.
     * @throws IOException
     */
    protected static boolean doesErrorExist(File logFile) throws IOException {
        return Files.readLines(logFile, Charsets.UTF_8, new LineProcessor<Boolean>() {
            boolean containsError;

            public boolean processLine(String line) throws IOException {
                boolean continueProcessing = true;
                if (line !=null && line.contains(ERROR_STRING)) {
                    containsError = true;
                    continueProcessing = false;
                }
                return continueProcessing;
            }

            public Boolean getResult() {
                return containsError;
            }
        });
    }

    static void addOptionIfPresent(ArgumentListBuilder cmdExecArgs, CliOption cliOption, String value) {
        if (!Strings.isNullOrEmpty(value)) {
            cmdExecArgs.add(OPTION_HYPHENS + cliOption.getCliOption() + "=" + value);
        }
    }
}
