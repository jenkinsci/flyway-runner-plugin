package sp.sd.flywayrunner.builder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import sp.sd.flywayrunner.installation.FlywayInstallation;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
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

    private final
    @CheckForNull
    String credentialsId;


    public static final StepDescriptor DESCRIPTOR = new StepDescriptor();

    @DataBoundConstructor
    public FlywayBuilder(String installationName, String flywayCommand,
                         String username,
                         String password,
                         String url,
                         String locations,
                         String commandLineArgs,
                         String credentialsId) {

        this.flywayCommand = flywayCommand;
        this.installationName = installationName;
        this.username = username;
        this.password = Secret.fromString(password).getEncryptedValue();
        this.url = url;
        this.locations = locations;
        this.commandLineArgs = commandLineArgs;
        this.credentialsId = credentialsId;
    }

    public FlywayInstallation getInstallation() {
        FlywayInstallation found = null;
        if (installationName != null) {
            for (FlywayInstallation i : getDescriptor().getInstallations()) {
                if (installationName.equals(i.getName())) {
                    found = i;
                    break;
                }
            }
        }
        return found;
    }

    @Override
    public StepDescriptor<StandardCredentials> getDescriptor() {
        return (StepDescriptor) super.getDescriptor();
    }

    @Override
    public void perform(Run build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        boolean result = false;
        ArgumentListBuilder cliCommand = composeFlywayCommand(build, listener, launcher, workspace);
        if (cliCommand != null) {
            int exitStatus = launcher.launch().cmds(cliCommand).stdout(listener).join();
            result = didErrorsOccur(build, exitStatus);
        }
        if (!result) {
            throw new AbortException("Build step 'Invoke Flyway' failed due to errors.");
        }
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private ArgumentListBuilder composeFlywayCommand(Run build,
                                                     TaskListener listener,
                                                     Launcher launcher,
                                                     FilePath workspace) {
        ArgumentListBuilder cliCommand = new ArgumentListBuilder();
        Item project = build.getParent();
        try {
            FlywayInstallation installation = getInstallation();
            if (installation != null) {

                FlywayInstallation buildTool =
                        Util.getInstallation(installation, build.getEnvironment(listener), listener, workspace);
                cliCommand.add(buildTool.getExecutable(launcher));

                Util.addOptionIfPresent(cliCommand, CliOption.USERNAME,
                        getUsername(build.getEnvironment(listener), project));
                if (password != null) {
                    cliCommand.addMasked(Util.OPTION_HYPHENS + CliOption.PASSWORD.getCliOption() + "=" +
                            getCredentialsPassword(build.getEnvironment(listener), project));
                }

                Util.addOptionIfPresent(cliCommand, CliOption.URL, build.getEnvironment(listener).expand(url));

                Util.addOptionIfPresent(cliCommand, CliOption.LOCATIONS,
                        build.getEnvironment(listener).expand(locations));

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

    public
    @Nullable
    String getCredentialsId() {
        return credentialsId;
    }

    public StandardUsernameCredentials getCredentials(Item project) {
        StandardUsernameCredentials credentials = null;
        try {

            credentials = credentialsId == null ? null : this.lookupSystemCredentials(credentialsId, project);
            if (credentials != null) {
                return credentials;
            }
        } catch (Throwable t) {

        }

        return credentials;
    }

    public static StandardUsernameCredentials lookupSystemCredentials(String credentialsId, Item project) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider
                        .lookupCredentials(StandardUsernameCredentials.class, project, ACL.SYSTEM,
                                Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(credentialsId)
        );
    }

    public String getUsername(EnvVars environment, Item project) {
        String Username = null;
        if (Strings.isNullOrEmpty(username)) {
            Username = "";
        } else {
            Username = environment.expand(username);
        }
        if (!Strings.isNullOrEmpty(credentialsId)) {
            Username = this.getCredentials(project).getUsername();
        }
        return Username;
    }

    public String getCredentialsPassword(EnvVars environment, Item project) {
        String Password = null;
        if (password == null) {
            Password = "";
        } else {
            Password = environment.expand(password);
        }
        if (!Strings.isNullOrEmpty(credentialsId)) {
            Password = Secret.toString(
                    StandardUsernamePasswordCredentials.class.cast(this.getCredentials(project)).getPassword());
        }
        return Password;
    }

    @Extension
    public static final class StepDescriptor<C extends StandardCredentials> extends BuildStepDescriptor<Builder> {
        @CopyOnWrite
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

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item owner) {
            if (owner == null || !owner.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }
            return new StandardUsernameListBoxModel().withEmptySelection().withAll(CredentialsProvider
                    .lookupCredentials(StandardUsernamePasswordCredentials.class, owner, ACL.SYSTEM,
                            Collections.<DomainRequirement>emptyList()));
        }

    }

}
