package sp.sd.flywayrunner.builder;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.Secret;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import jenkins.tasks.SimpleBuildStep;
import sp.sd.flywayrunner.installation.FlywayInstallation;
import org.kohsuke.stapler.DataBoundConstructor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.google.common.base.Strings;


/**
 * Jenkins builder which runs flyway.
 */
public class FlywayBuilder extends Builder implements SimpleBuildStep, Serializable {

    private static final String DEFAULT_LOGLEVEL = "info";

    /**
     * The Flyway action to execute.
     */
    private final String flywayCommand;
    /**
     * Which Flyway installation to use during invocation.
     */
    private final String installationName;
    /**
     * Username with which to connect to database.
     */
    private final String username;
    /**
     * Password with which to connect to database.
     */
    private final String password;
    /**
     * JDBC database connection URL.
     */
    private final String url;
    /**
     * Catch-all option which can be used to supply additional options to liquibase.
     */
    private final String commandLineArgs;

    /**
     * Catch-all locations which can be used to migrate.
     */
    private final String locations;

    @Extension
    public static final StepDescriptor DESCRIPTOR = new StepDescriptor();

    @DataBoundConstructor
    public FlywayBuilder(String installationName, String flywayCommand,
                         String username,
                         String password,
                         String url,
                         String locations,
                         String commandLineArgs) {

        this.flywayCommand = flywayCommand;
        this.installationName = installationName;
        this.username = username;
        this.password = Secret.fromString(password).getEncryptedValue();
        this.url = url;
        this.locations = locations;
        this.commandLineArgs = commandLineArgs;
    }

    public FlywayInstallation getInstallation() {
        FlywayInstallation found = null;
        if (installationName != null) {
            for (FlywayInstallation i : DESCRIPTOR.getInstallations()) {
                if (installationName.equals(i.getName())) {
                    found = i;
                    break;
                }
            }
        }
        return found;
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void perform(Run build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        boolean result = false;
        ArgumentListBuilder cliCommand = composeFlywayCommand(build, listener);
        if (cliCommand != null) {
            int exitStatus = launcher.launch().cmds(cliCommand).stdout(listener).join();
            result = didErrorsOccur(build, exitStatus);
        }
        if (!result) {
            build.setResult(Result.FAILURE);
        }
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private ArgumentListBuilder composeFlywayCommand(Run build, TaskListener listener) {
        ArgumentListBuilder cliCommand = new ArgumentListBuilder();

        try {
            if (getInstallation() != null && getInstallation().getHome() != null) {
                cliCommand.add(new File(getInstallation().getHome()));

                Util.addOptionIfPresent(cliCommand, CliOption.USERNAME, build.getEnvironment(listener).expand(username));
                if (password != null) {
                    cliCommand.addMasked(Util.OPTION_HYPHENS + CliOption.PASSWORD.getCliOption() + "=" + build.getEnvironment(listener).expand(Secret.decrypt(password).getPlainText()));
                }

                Util.addOptionIfPresent(cliCommand, CliOption.URL, build.getEnvironment(listener).expand(url));

                Util.addOptionIfPresent(cliCommand, CliOption.LOCATIONS, build.getEnvironment(listener).expand(locations));

                if (!Strings.isNullOrEmpty(commandLineArgs)) {
                    cliCommand.addTokenized(build.getEnvironment(listener).expand(commandLineArgs));
                }

                cliCommand.addTokenized(flywayCommand);
            } else {
                listener.fatalError("Flyway installation was not found.");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace(listener.getLogger());
        }
        return cliCommand;
    }

    private boolean didErrorsOccur(Run build, int exitStatus) throws IOException {
        boolean result = true;
        if (exitStatus != 0) {
            result = false;
        }
        return result;
    }

    public String getCommandLineArgs() {
        return commandLineArgs;
    }

    public String getFlywayCommand() {
        return flywayCommand;
    }

    public String getInstallationName() {
        return installationName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return Secret.decrypt(password).getPlainText();
    }

    public String getLocations() {
        return locations;
    }

    public String getUrl() {
        return url;
    }

    public static final class StepDescriptor extends BuildStepDescriptor<Builder> {
        private volatile FlywayInstallation[] installations = new FlywayInstallation[0];

        public StepDescriptor() {
            super(FlywayBuilder.class);
            load();
        }

        @SuppressFBWarnings(value = "EI_EXPOSE_REP")
        public FlywayInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(FlywayInstallation... installations) {
            this.installations = installations;
            save();
        }

        public FlywayInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(FlywayInstallation.DescriptorImpl.class);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Invoke Flyway";
        }

    }
}
