package sp.sd.flywayrunner.builder;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import sp.sd.flywayrunner.installation.FlywayInstallation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class FlywayBuilderIntegrationTest {
    private static final String SIMPLE_MIGRATION = "/migrations/V1_2__Simple.sql";
    private static final String MIGRATION_WITH_ERROR = "/migrations/V1_2__ContainsSQLError.sql";

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected File migrationFileDirectory;
    protected FreeStyleProject project;

    @Before
    public void setup() throws IOException {
        // maven is configured to install flyway here and supply this system property.
        // If running test outside of maven (in one's IDE for example), you'll need to supply this system property
        // yourself (using -Dflyway.home=/some/path).  A fully unpacked version of flyway is expected to reside
        // here.
        String flywayHome = System.getProperty("flyway.home");

        createFlywayJenkinsInstallation(flywayHome);
        migrationFileDirectory = temporaryFolder.newFolder("migrations");
        project = createFlywayJenkinsProject(migrationFileDirectory);
    }

    @Test
    public void should_run_flyway_migration_successfully()
            throws IOException, ExecutionException, InterruptedException {
        supplyMigrationFromResource(SIMPLE_MIGRATION);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        assertThat(build.getResult(), is(Result.SUCCESS));
        String buildLog = FileUtils.readFileToString(build.getLogFile());
        assertThat(buildLog, containsString("Successfully applied 1 migration"));
    }

    @Test
    public void should_fail_build_when_migration_has_error()
            throws IOException, ExecutionException, InterruptedException {
        supplyMigrationFromResource(MIGRATION_WITH_ERROR);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        assertThat(build.getResult(), is(Result.FAILURE));
        String buildLog = FileUtils.readFileToString(build.getLogFile());
        assertThat(buildLog, containsString("Syntax error"));
    }

    @Test
    public void should_use_credentials_for_db_access() throws IOException, ExecutionException, InterruptedException {

        String username = RandomStringUtils.randomAlphabetic(5);
        String password = RandomStringUtils.randomAlphabetic(5);
        String credentialsId = createJenkinsCredentials(username, password);

        String jdbcUrl = createDatabase(username, password);

        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject();
        FlywayBuilder builder = new FlywayBuilder(
                "flyway",
                "migrate",
                "",
                "",
                jdbcUrl,
                "filesystem:" + migrationFileDirectory.getAbsolutePath(),
                "",
                credentialsId);


        freeStyleProject.getBuildersList().add(builder);
        FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();

        assertThat(build.getResult(), is(Result.SUCCESS));
    }

    private static FreeStyleProject createFlywayJenkinsProject(File migrationDir) throws IOException {
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject();
        FlywayBuilder flywayBuilder = new FlywayBuilder(
                "flyway",
                "migrate",
                "sa",
                "",
                "jdbc:h2:mem:test",
                "filesystem:" + migrationDir.getAbsolutePath(),
                "",
                "");

        freeStyleProject.getBuildersList().add(flywayBuilder);
        return freeStyleProject;
    }

    private void supplyMigrationFromResource(String resourcePath) throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream(resourcePath);
        String migrationSql = IOUtils.toString(resourceAsStream);
        String fileName = resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
        File migrationFile = new File(migrationFileDirectory, fileName);
        FileUtils.write(migrationFile, migrationSql);
    }

    private static void createFlywayJenkinsInstallation(String flywayHome) {
        FlywayInstallation flywayInstallation =
                new FlywayInstallation("flyway", flywayHome + "/flyway", JenkinsRule.NO_PROPERTIES);

        jenkinsRule.getInstance().getDescriptorByType(FlywayInstallation.DescriptorImpl.class)
                   .setInstallations(flywayInstallation);
    }

    private String createDatabase(String username, String password) throws IOException {
        String jdbcUrl = "jdbc:h2:" + temporaryFolder.newFolder().getAbsolutePath();
        // with H2, creating the initial connection creates a db that uses passed credentials.
        // subsequent connections will then require the same username & password
        Connection connection = null;
        try {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setUser(username);
            ds.setUrl(jdbcUrl);
            ds.setPassword(password);
            connection = ds.getConnection();
            connection.close();
        } catch (SQLException e) {
            throw new AssertionError("Error creating database for testing.", e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {

                }
            }
        }
        return jdbcUrl;
    }

    private static String createJenkinsCredentials(String username, String password) throws IOException {
        String credentialsId = RandomStringUtils.randomAlphabetic(10);
        Credentials credentials =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, "sample", username,
                        password);

        CredentialsProvider.lookupStores(jenkinsRule.getInstance()).iterator().next()
                           .addCredentials(Domain.global(), credentials);
        return credentialsId;
    }
}