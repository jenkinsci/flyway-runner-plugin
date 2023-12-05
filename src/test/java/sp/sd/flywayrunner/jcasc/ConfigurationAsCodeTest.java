package sp.sd.flywayrunner.jcasc;

import static org.junit.Assert.assertEquals;

import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.util.DescribableList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import sp.sd.flywayrunner.installation.FlywayInstallation;

public class ConfigurationAsCodeTest {

    @Rule
    public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void should_support_configuration_as_code() throws Exception {

        // Just ensure that the structure is correct
        FlywayInstallation flywayInstallation = Jenkins.get()
                .getDescriptorByType(FlywayInstallation.DescriptorImpl.class)
                .getInstallations()[0];
        assertEquals("flyway-9.22.3", flywayInstallation.getName());
        DescribableList<ToolProperty<?>, ToolPropertyDescriptor> flywayToolInstallers =
                flywayInstallation.getProperties();
        assertEquals(1, flywayToolInstallers.size());
        InstallSourceProperty flywayInstallSourceProperty = (InstallSourceProperty) flywayToolInstallers.get(0);
        assertEquals(1, flywayInstallSourceProperty.installers.size());
    }
}
