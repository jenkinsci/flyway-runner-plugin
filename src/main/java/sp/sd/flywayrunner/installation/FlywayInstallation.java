package sp.sd.flywayrunner.installation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import jenkins.security.MasterToSlaveCallable;
import sp.sd.flywayrunner.builder.FlywayBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.inject.Inject;


public class FlywayInstallation extends ToolInstallation implements NodeSpecific<FlywayInstallation>,
        EnvironmentSpecific<FlywayInstallation> {
    private static final long serialVersionUID = 1;
    private String flywayHome;

    @DataBoundConstructor
    public FlywayInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, launderHome(home), properties);
        this.flywayHome = home;
    }

    public FlywayInstallation forEnvironment(EnvVars environment) {
        return new FlywayInstallation(getName(), environment.expand(flywayHome), getProperties().toList());
    }

    public FlywayInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new FlywayInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Override
    public String getHome() {
        String resolvedHome;
        if (flywayHome != null) {
            resolvedHome= flywayHome;
        } else {
            resolvedHome=super.getHome();
        }
        return resolvedHome;
    }

    public String getExecutable(Launcher launcher) throws IOException, InterruptedException {
        return launcher.getChannel().call(new MasterToSlaveCallable<String, IOException>() {
            public String call() throws IOException {
                File exe = getExecutableFile();
                if (exe.exists()) {
                    return exe.getPath();
                }
                return null;
            }
        });
    }

    public File getExecutableFile() {
        String execName = Functions.isWindows() ? "flyway.cmd" : "flyway";
        String resolvedFlywayHome = Util.replaceMacro(flywayHome, EnvVars.masterEnvVars);
        return new File(resolvedFlywayHome, execName);
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<FlywayInstallation> {
        @Inject
        private FlywayBuilder.StepDescriptor flywayDescriptor;

        @Override
        public String getDisplayName() {
            return "Flyway";
        }

        @Override
        @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
        public FlywayInstallation[] getInstallations() {
            return flywayDescriptor.getInstallations();
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new FlywayInstaller(null));
        }

        @Override
        @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
        public void setInstallations(FlywayInstallation... installations) {
            flywayDescriptor.setInstallations(installations);

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
