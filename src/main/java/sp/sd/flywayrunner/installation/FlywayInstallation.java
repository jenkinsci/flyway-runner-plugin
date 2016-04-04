package sp.sd.flywayrunner.installation;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;

import java.io.IOException;
import java.util.List;

import sp.sd.flywayrunner.builder.FlywayBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


public class FlywayInstallation extends ToolInstallation implements NodeSpecific<FlywayInstallation>,
        EnvironmentSpecific<FlywayInstallation> {
    @DataBoundConstructor
    public FlywayInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties);
    }

    public FlywayInstallation forEnvironment(EnvVars environment) {
        return new FlywayInstallation(getName(), getHome(), getProperties().toList());
    }

    public FlywayInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new FlywayInstallation(getName(), getHome(), getProperties().toList());
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<FlywayInstallation> {
        @Override
        public String getDisplayName() {
            return "Flyway";
        }

        @Override
        @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
        public FlywayInstallation[] getInstallations() {
            return Hudson.getInstance().getDescriptorByType(FlywayBuilder.StepDescriptor.class).getInstallations();
        }

        @Override
        @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
        public void setInstallations(FlywayInstallation... installations) {
            Hudson.getInstance().getDescriptorByType(FlywayBuilder.StepDescriptor.class).setInstallations(installations);
        }
    }
}
