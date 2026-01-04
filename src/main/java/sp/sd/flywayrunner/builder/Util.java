package sp.sd.flywayrunner.builder;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import java.io.File;
import java.io.IOException;

public class Util {

    private static final String ERROR_STRING = "Errors:";
    static final String OPTION_HYPHENS = "-";

    private Util() {}

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
                if (line != null && line.contains(ERROR_STRING)) {
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

    public static <T extends ToolInstallation & EnvironmentSpecific<T> & NodeSpecific<T>> T getInstallation(
            @Nullable T tool, EnvVars env, TaskListener listener, FilePath workspace)
            throws IOException, InterruptedException {
        Computer computer = workspace.toComputer();
        if (computer == null) {
            return null;
        }
        Node node = computer.getNode();
        if (tool == null || node == null) {
            return null;
        }
        T t = tool.forNode(node, listener);
        t = t.forEnvironment(env);

        return t;
    }
}
