package sp.sd.flywayrunner.installation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.PersistentDescriptor;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import java.io.File;
import java.io.IOException;
import java.util.List;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Flyway installation.  The "flywayHome" may either be the full path to the executable, or the directory in which
 * the executable resides.  This dual meaning allows backwards compatibility with previous versions of the plugin.
 */
public class FlywayInstallation extends ToolInstallation
        implements NodeSpecific<FlywayInstallation>, EnvironmentSpecific<FlywayInstallation> {
    private static final long serialVersionUID = 2;
    private String flywayHome;

    @DataBoundConstructor
    public FlywayInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, launderHome(home), properties);
        flywayHome = home;
    }

    public FlywayInstallation forEnvironment(EnvVars environment) {
        return new FlywayInstallation(
                getName(), environment.expand(flywayHome), getProperties().toList());
    }

    public FlywayInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new FlywayInstallation(
                getName(), translateFor(node, log), getProperties().toList());
    }

    public static FlywayInstallation[] allInstallations() {
        FlywayInstallation.DescriptorImpl ansibleDescriptor =
                Jenkins.get().getDescriptorByType(FlywayInstallation.DescriptorImpl.class);
        return ansibleDescriptor.getInstallations();
    }

    @Override
    public String getHome() {
        String resolvedHome;
        if (flywayHome != null) {
            resolvedHome = flywayHome;
        } else {
            resolvedHome = super.getHome();
        }
        return resolvedHome;
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public String getExecutable(Launcher launcher) throws IOException, InterruptedException {
        return launcher.getChannel().call(new GetExecutable(getHome()));
    }

    private static File getExecutableFile(String flywayHome) {
        File file = new File(flywayHome);
        File executable;
        if (file.isFile()) {
            executable = file;
        } else {
            String execName = Functions.isWindows() ? "flyway.cmd" : "flyway";
            String resolvedFlywayHome = Util.replaceMacro(flywayHome, EnvVars.masterEnvVars);
            executable = new File(resolvedFlywayHome, execName);
        }
        return executable;
    }

    private static class GetExecutable extends MasterToSlaveCallable<String, IOException> {
        private final String flywayHome;

        GetExecutable(String flywayHome) {
            this.flywayHome = flywayHome;
        }

        @Override
        public String call() throws IOException {
            File exe = getExecutableFile(flywayHome);
            if (exe.exists()) {
                return exe.getPath();
            }
            return null;
        }
    }

    @Extension
    @Symbol("flyway")
    public static class DescriptorImpl extends ToolDescriptor<FlywayInstallation> implements PersistentDescriptor {
        @Override
        public String getDisplayName() {
            return "Flyway";
        }

        @Override
        public boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {
            super.configure(req, json);
            save();
            return true;
        }
    }

    private static String launderHome(String home) {
        if (home.endsWith("/") || home.endsWith("\\")) {

            return home.substring(0, home.length() - 1);
        } else {
            return home;
        }
    }
}
