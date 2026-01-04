package sp.sd.flywayrunner.builder;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.PersistentDescriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import sp.sd.flywayrunner.installation.FlywayInstallation;

/**
 * Jenkins builder which runs flyway.
 */
public class FlywayBuilder extends Builder implements SimpleBuildStep, Serializable {

    /**
     * The Flyway action to execute.
     */
    private final String flywayCommand;
    /**
     * Which Flyway installation to use during invocation.
     */
    private final String installationName;
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

    private final @CheckForNull String credentialsId;

    @DataBoundConstructor
    public FlywayBuilder(
            String installationName,
            String flywayCommand,
            String url,
            String locations,
            String commandLineArgs,
            String credentialsId) {

        this.flywayCommand = flywayCommand;
        this.installationName = installationName;
        this.url = url;
        this.locations = locations;
        this.commandLineArgs = commandLineArgs;
        this.credentialsId = credentialsId;
    }

    public FlywayInstallation getInstallation() {
        for (FlywayInstallation i : getDescriptor().getInstallations()) {
            if (installationName != null && installationName.equals(i.getName())) return i;
        }
        return null;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        boolean result = false;
        ArgumentListBuilder cliCommand = composeFlywayCommand(build, listener, launcher, workspace);
        if (cliCommand != null) {
            int exitStatus = launcher.launch().cmds(cliCommand).stdout(listener).join();
            result = didErrorsOccur(exitStatus);
        }
        if (!result) {
            throw new AbortException("Build step 'Invoke Flyway' failed due to errors.");
        }
    }

    private ArgumentListBuilder composeFlywayCommand(
            Run<?, ?> build, TaskListener listener, Launcher launcher, FilePath workspace) {
        ArgumentListBuilder cliCommand = new ArgumentListBuilder();
        Item project = build.getParent();
        try {
            FlywayInstallation installation = getInstallation();
            if (installation != null) {

                FlywayInstallation buildTool = sp.sd.flywayrunner.builder.Util.getInstallation(
                        installation, build.getEnvironment(listener), listener, workspace);
                cliCommand.add(buildTool.getExecutable(launcher));

                sp.sd.flywayrunner.builder.Util.addOptionIfPresent(
                        cliCommand, CliOption.USERNAME, getUsername(project));

                cliCommand.addMasked(sp.sd.flywayrunner.builder.Util.OPTION_HYPHENS + CliOption.PASSWORD.getCliOption()
                        + "=" + getCredentialsPassword(project));

                sp.sd.flywayrunner.builder.Util.addOptionIfPresent(
                        cliCommand,
                        CliOption.URL,
                        build.getEnvironment(listener).expand(url));

                sp.sd.flywayrunner.builder.Util.addOptionIfPresent(
                        cliCommand,
                        CliOption.LOCATIONS,
                        build.getEnvironment(listener).expand(locations));

                if (!Strings.isNullOrEmpty(commandLineArgs)) {
                    cliCommand.addTokenized(build.getEnvironment(listener).expand(commandLineArgs));
                }

                cliCommand.addTokenized(build.getEnvironment(listener).expand(flywayCommand));
            } else {
                listener.fatalError("Flyway installation was not found.");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace(listener.getLogger());
        }
        return cliCommand;
    }

    private boolean didErrorsOccur(int exitStatus) {
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

    public String getLocations() {
        return locations;
    }

    public String getUrl() {
        return url;
    }

    public @Nullable String getCredentialsId() {
        return credentialsId;
    }

    public StandardUsernameCredentials getCredentials(Item project) {
        StandardUsernameCredentials credentials = null;
        try {

            credentials = credentialsId == null ? null : lookupSystemCredentials(credentialsId, project);
            if (credentials != null) {
                return credentials;
            }
        } catch (Throwable t) {

        }

        return credentials;
    }

    public StandardUsernameCredentials lookupSystemCredentials(String credentialsId, Item project) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItem(
                        StandardUsernameCredentials.class,
                        project,
                        ACL.SYSTEM2,
                        Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(credentialsId));
    }

    public String getUsername(Item project) {
        String username = null;
        if (!Strings.isNullOrEmpty(credentialsId)) {
            username = this.getCredentials(project).getUsername();
        }
        return username;
    }

    public String getCredentialsPassword(Item project) {
        String password = null;
        if (!Strings.isNullOrEmpty(credentialsId)) {
            password = Secret.toString(StandardUsernamePasswordCredentials.class
                    .cast(this.getCredentials(project))
                    .getPassword());
        }
        return password;
    }

    @Extension
    @Symbol("flywayrunner")
    public static final class DescriptorImpl<C extends StandardCredentials> extends BuildStepDescriptor<Builder>
            implements PersistentDescriptor {

        public DescriptorImpl() {
            super(FlywayBuilder.class);
            load();
        }

        public FlywayInstallation[] getInstallations() {
            return FlywayInstallation.allInstallations();
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
            return new StandardUsernameListBoxModel()
                    .includeEmptyValue()
                    .includeAs(ACL.SYSTEM2, owner, StandardUsernamePasswordCredentials.class);
        }

        public ListBoxModel doFillInstallationItems() {
            ListBoxModel model = new ListBoxModel();
            for (FlywayInstallation tool : FlywayInstallation.allInstallations()) {
                model.add(tool.getName());
            }
            return model;
        }
    }
}
