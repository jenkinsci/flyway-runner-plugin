package sp.sd.flywayrunner.builder;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import sp.sd.flywayrunner.installation.FlywayInstallation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class FlywayBuilderIntegrationTest {


    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    protected String flywayHome;
    protected File migrationDir;

    @Before
    public void setup() throws IOException {
        // maven is configured to supply this property (as well as ensuring flyway is installed here).
        // If running test outside of maven (in one's IDE for example), you'll need to supply this system property
        // yourself (using -Dflyway.home=/some/path).  A fully unpacked version of flyway is expected to reside
        // here (including db driver!)

        flywayHome = System.getProperty("flyway.home");

        createFlywayJenkinsInstallation(flywayHome);

        migrationDir = temporaryFolder.newFolder("migrations");

        copySimpleMigrationToMigrationDirectory();
    }

    @Test
    public void should_run_flyway_migration_successfully() throws IOException, ExecutionException, InterruptedException {

        FreeStyleProject project = createFlywayJenkinsProject();
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        assertThat(build.getResult(), is(Result.SUCCESS));
        String buildLog = FileUtils.readFileToString(build.getLogFile());
        assertThat(buildLog, containsString("Successfully applied 1 migration"));
    }

    private void copySimpleMigrationToMigrationDirectory() throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/migrations/V1_2__Simple.sql");
        String migrationSql = IOUtils.toString(resourceAsStream);
        File sqlFile = new File(migrationDir, "V1_2__Simple.sql");
        FileUtils.write(sqlFile, migrationSql);
    }

    private FreeStyleProject createFlywayJenkinsProject() throws IOException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        FlywayBuilder flywayBuilder = new FlywayBuilder(
                                        "flyway",
                                        "migrate",
                                        "sa",
                                        "",
                                        "jdbc:h2:mem:test",
                                        "filesystem:" + migrationDir.getAbsolutePath(),
                                        "",
                                        "");

        project.getBuildersList().add(flywayBuilder);
        return project;
    }

    private static void createFlywayJenkinsInstallation(String flywayHome) {
        FlywayInstallation flywayInstallation =
                new FlywayInstallation("flyway", flywayHome + "/flyway", JenkinsRule.NO_PROPERTIES);

        jenkinsRule.getInstance().getDescriptorByType(FlywayInstallation.DescriptorImpl.class)
               .setInstallations(flywayInstallation);
    }
}