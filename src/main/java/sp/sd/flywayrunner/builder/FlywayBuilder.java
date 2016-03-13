package sp.sd.flywayrunner.builder;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;

import sp.sd.flywayrunner.installation.FlywayInstallation;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.base.Strings;


/**
 * Jenkins builder which runs liquibase.
 */
public class FlywayBuilder extends Builder {

    private static final String DEFAULT_LOGLEVEL = "info";

    /**
     * The Flyway action to execute.
     */
    private String flywayCommand;
    /**
     * Which Flyway installation to use during invocation.
     */
    private String installationName;
    /**
     * Username with which to connect to database.
     */
    private String username;
    /**
     * Password with which to connect to database.
     */
    private String password;
    /**
     * JDBC database connection URL.
     */
    private String url;    
    /**
     * Catch-all option which can be used to supply additional options to liquibase.
     */
    private String commandLineArgs;
	
	/**
     * Catch-all locations which can be used to migrate.
     */
    private String locations;

    @Extension
    public static final StepDescriptor DESCRIPTOR = new StepDescriptor();

    @DataBoundConstructor
    public FlywayBuilder(String commandLineArgs,  String flywayCommand, String locations,                         
                            String liquibaseCommand,
                            String installationName,
                            String username,
                            String password,
                            String url) {
        
        this.flywayCommand = flywayCommand;
        this.installationName = installationName;
        this.username = username;
        this.password = password;
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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        ArgumentListBuilder cliCommand = composeFlywayCommand(build);
				
        int exitStatus = launcher.launch().cmds(cliCommand).stdout(listener).pwd(build.getWorkspace()).join();

        boolean result = didErrorsOccur(build, exitStatus);
        return result;
    }

    private ArgumentListBuilder composeFlywayCommand(AbstractBuild<?, ?> build) {
        ArgumentListBuilder cliCommand = new ArgumentListBuilder();

        cliCommand.add(new File(getInstallation().getHome()));

        Util.addOptionIfPresent(cliCommand, CliOption.USERNAME, build.getEnvironment(listener).expand(username));
        if (!Strings.isNullOrEmpty(password)) {            
            cliCommand.addMasked(Util.OPTION_HYPHENS + CliOption.PASSWORD.getCliOption() + "=" + build.getEnvironment(listener).expand(password));
        }
        
        Util.addOptionIfPresent(cliCommand, CliOption.URL, build.getEnvironment(listener).expand(url));
        
		Util.addOptionIfPresent(cliCommand, CliOption.LOCATIONS, build.getEnvironment(listener).expand(locations));

        if (!Strings.isNullOrEmpty(commandLineArgs)) {
            cliCommand.addTokenized(build.getEnvironment(listener).expand(commandLineArgs));
        }       

        cliCommand.addTokenized(flywayCommand);
        return cliCommand;
    }

    private boolean didErrorsOccur(AbstractBuild<?, ?> build, int exitStatus) throws IOException {
        boolean result = true;
        if (exitStatus != 0) {
            result = false;
        } else {
            // check for errors that don't result in an exit code less than 0.
            File logFile = build.getLogFile();
            if (Util.doesErrorExist(logFile)) {
                result = false;
            }
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
        return password;
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
