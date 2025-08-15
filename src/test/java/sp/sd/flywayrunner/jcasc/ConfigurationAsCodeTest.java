package sp.sd.flywayrunner.jcasc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.util.DescribableList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.Test;
import sp.sd.flywayrunner.installation.FlywayInstallation;

public class ConfigurationAsCodeTest {

    @Test
    @WithJenkinsConfiguredWithCode
    @ConfiguredWithCode("configuration-as-code.yml")
    public void should_support_configuration_as_code(JenkinsConfiguredWithCodeRule rule) throws Exception {

        // Just ensure that the structure is correct
        FlywayInstallation flywayInstallation = rule.jenkins.getDescriptorByType(
                        FlywayInstallation.DescriptorImpl.class)
                .getInstallations()[0];
        assertEquals("flyway-10.21.0", flywayInstallation.getName());
        DescribableList<ToolProperty<?>, ToolPropertyDescriptor> flywayToolInstallers =
                flywayInstallation.getProperties();
        assertEquals(1, flywayToolInstallers.size());
        InstallSourceProperty flywayInstallSourceProperty = (InstallSourceProperty) flywayToolInstallers.get(0);
        assertEquals(1, flywayInstallSourceProperty.installers.size());
    }
}
