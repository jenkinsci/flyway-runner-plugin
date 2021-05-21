package sp.sd.flywayrunner.builder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static sp.sd.flywayrunner.builder.InstallerTest.VersionOptionMatcher.isOptionWithVersionText;

public class InstallerTest {

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();
    private static final String TOOL_UPDATES_FILENAME = "sp.sd.flywayrunner.installation.FlywayInstaller";

    @SuppressWarnings("unchecked")
    @Test
    public void should_list_versions() throws IOException, SAXException {
        populateFlywayInstallersFile();
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();
        HtmlPage configure = webClient.goTo("configure");

        HtmlElement addButton = configure.getFirstByXPath("//button[contains(., 'Add Flyway')]");
        addButton.click();
        List<HtmlElement> versionOptions =
                configure.getByXPath("//div[contains(@descriptorid, 'FlywayInstaller')]//option");

        assertThat(versionOptions, hasSize(20));
        assertThat(versionOptions, hasItem(isOptionWithVersionText("4.0.3 (without JRE)")));
    }

    private void populateFlywayInstallersFile() throws IOException {
        File dir = jenkinsRule.getInstance().getRootDir();
        File updatesDir = new File(dir, "updates");
        String updates = IOUtils.toString(getClass().getResourceAsStream("/flyway-installer/" + TOOL_UPDATES_FILENAME + ".json"));
        File installers = new File(updatesDir, TOOL_UPDATES_FILENAME);
        FileUtils.write(installers, updates);
    }

    static class VersionOptionMatcher extends TypeSafeMatcher<HtmlElement> {
        String optionText;

        public VersionOptionMatcher(String optionText) {
            this.optionText = optionText;
        }

        public static VersionOptionMatcher isOptionWithVersionText(String optionText) {
            return new VersionOptionMatcher(optionText);
        }

        @Override
        protected boolean matchesSafely(HtmlElement item) {
            return item.getTextContent().equals(optionText);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("an HTMLElement with text ").appendValue(optionText);
        }
    }
}
