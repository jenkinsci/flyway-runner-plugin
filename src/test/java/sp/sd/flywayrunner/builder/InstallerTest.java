package sp.sd.flywayrunner.builder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.xml.sax.SAXException;

@WithJenkins
public class InstallerTest {

    private static final String TOOL_UPDATES_FILENAME = "sp.sd.flywayrunner.installation.FlywayInstaller";

    @Test
    public void shouldContainerFlywayInstaller(JenkinsRule jenkinsRule) throws IOException, SAXException {
        populateFlywayInstallersFile(jenkinsRule);
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();
        HtmlPage configure = webClient.goTo("manage/configureTools");
        HtmlElement addButton = configure.getFirstByXPath("//button[contains(., 'Add Flyway')]");
        addButton.click();
    }

    private void populateFlywayInstallersFile(JenkinsRule jenkinsRule) throws IOException {
        File dir = jenkinsRule.getInstance().getRootDir();
        File updatesDir = new File(dir, "updates");
        String updates = IOUtils.toString(
                getClass().getResourceAsStream("/flyway-installer/" + TOOL_UPDATES_FILENAME + ".json"),
                StandardCharsets.UTF_8);
        File installers = new File(updatesDir, TOOL_UPDATES_FILENAME);
        FileUtils.write(installers, updates, StandardCharsets.UTF_8);
    }
}
