package sp.sd.flywayrunner.builder;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import sp.sd.flywayrunner.installation.FlywayInstallation;

@WithJenkins
public class FlywayBuilderIntegrationTest {

    private static final String SIMPLE_MIGRATION = "/migrations/V1_2__Simple.sql";
    private static final String MIGRATION_WITH_ERROR = "/migrations/V1_2__ContainsSQLError.sql";

    protected File migrationFileDirectory;
    protected FreeStyleProject project;

    public void setup(JenkinsRule jenkinsRule, @TempDir Path temporaryFolder) throws Exception {
        // maven is configured to install flyway here and supply this system property.
        // If running test outside of maven (in one's IDE for example), you'll need to supply this system property
        // yourself (using -Dflyway.home=/some/path).  A fully unpacked version of flyway is expected to reside
        // here.
        String flywayHome = System.getProperty("flyway.home");
        createFlywayJenkinsInstallation(jenkinsRule, flywayHome);
        migrationFileDirectory =
                Files.createDirectory(temporaryFolder.resolve("migrations")).toFile();
        project = createFlywayJenkinsProject(jenkinsRule, migrationFileDirectory);
        jenkinsRule.createSlave(Label.get("test-agent"));
    }

    @Test
    public void shouldRunFreestyleJob(JenkinsRule jenkinsRule, @TempDir Path temporaryFolder) throws Exception {
        setup(jenkinsRule, temporaryFolder);
        supplyMigrationFromResource(SIMPLE_MIGRATION);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        assertThat(build.getResult(), is(Result.SUCCESS));

        String buildLog = IOUtils.toString(build.getLogReader());
        assertThat(buildLog, containsString("Successfully applied 1 migration"));
    }

    @Test
    public void shouldRunPipeline(JenkinsRule jenkinsRule, @TempDir Path temporaryFolder) throws Exception {
        setup(jenkinsRule, temporaryFolder);
        String pipeline = IOUtils.toString(
                FlywayBuilderIntegrationTest.class.getResourceAsStream("/pipelines/pipeline.groovy"),
                StandardCharsets.UTF_8);
        createJenkinsPipelineCredentials(jenkinsRule);
        WorkflowJob workflowJob = jenkinsRule.createProject(WorkflowJob.class);
        workflowJob.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run1 = workflowJob.scheduleBuild2(0).waitForStart();
        jenkinsRule.waitForCompletion(run1);
        assertThat(
                run1.getLog(),
                allOf(
                        containsString("Successfully applied 1 migration to schema \"PUBLIC\", now at version v1 "),
                        containsString("flyway -user=foo ******** -url=jdbc:h2:mem:test")));
    }

    @Test
    public void shouldFailWhenMigrationHasError(JenkinsRule jenkinsRule, @TempDir Path temporaryFolder)
            throws Exception {
        setup(jenkinsRule, temporaryFolder);
        supplyMigrationFromResource(MIGRATION_WITH_ERROR);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        assertThat(build.getResult(), is(Result.FAILURE));
        String buildLog = IOUtils.toString(build.getLogReader());
        assertThat(buildLog, containsString("Syntax error"));
    }

    @Test
    public void shouldUseCredentialsForDbAccess(JenkinsRule jenkinsRule, @TempDir Path temporaryFolder)
            throws Exception {
        setup(jenkinsRule, temporaryFolder);
        String username = RandomStringUtils.insecure().nextAlphabetic(5);
        String password = RandomStringUtils.insecure().nextAlphabetic(5);
        String credentialsId = createJenkinsCredentials(jenkinsRule, username, password);

        String jdbcUrl = createDatabase(jenkinsRule, temporaryFolder, username, password);

        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject();
        FlywayBuilder builder = new FlywayBuilder(
                "flyway",
                "migrate",
                jdbcUrl,
                "filesystem:" + migrationFileDirectory.getAbsolutePath(),
                "",
                credentialsId);

        freeStyleProject.getBuildersList().add(builder);
        FreeStyleBuild build = freeStyleProject.scheduleBuild2(0).get();
        String buildLog = IOUtils.toString(build.getLogReader());
        assertThat(buildLog, containsString("No migration necessary."));
        assertThat(build.getResult(), is(Result.SUCCESS));
    }

    private FreeStyleProject createFlywayJenkinsProject(JenkinsRule jenkinsRule, File migrationDir) throws IOException {
        FreeStyleProject freeStyleProject = jenkinsRule.createFreeStyleProject();
        FlywayBuilder flywayBuilder = new FlywayBuilder(
                "flyway", "migrate", "jdbc:h2:mem:test", "filesystem:" + migrationDir.getAbsolutePath(), "", "");

        freeStyleProject.getBuildersList().add(flywayBuilder);
        return freeStyleProject;
    }

    private void supplyMigrationFromResource(String resourcePath) throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream(resourcePath);
        String migrationSql = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
        String fileName = resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
        File migrationFile = new File(migrationFileDirectory, fileName);
        FileUtils.write(migrationFile, migrationSql, StandardCharsets.UTF_8);
    }

    private void createFlywayJenkinsInstallation(JenkinsRule jenkinsRule, String flywayHome) {
        FlywayInstallation flywayInstallation = new FlywayInstallation(
                "flyway",
                flywayHome + (SystemUtils.IS_OS_WINDOWS ? "/flyway.cmd" : "/flyway"),
                JenkinsRule.NO_PROPERTIES);

        jenkinsRule
                .getInstance()
                .getDescriptorByType(FlywayInstallation.DescriptorImpl.class)
                .setInstallations(flywayInstallation);
    }

    private String createDatabase(JenkinsRule jenkinsRule, Path temporaryFolder, String username, String password)
            throws IOException {
        String jdbcUrl = "jdbc:h2:" + temporaryFolder.toAbsolutePath();
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

    private String createJenkinsCredentials(JenkinsRule jenkinsRule, String username, String password)
            throws IOException, Descriptor.FormException {
        String credentialsId = RandomStringUtils.insecure().nextAlphabetic(10);
        Credentials credentials = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, credentialsId, "sample", username, password);

        CredentialsProvider.lookupStores(jenkinsRule.getInstance())
                .iterator()
                .next()
                .addCredentials(Domain.global(), credentials);
        return credentialsId;
    }

    private void createJenkinsPipelineCredentials(JenkinsRule jenkinsRule)
            throws IOException, Descriptor.FormException {
        Credentials credentials = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "pipeline-credentials", "sample", "foo", "bar");
        CredentialsProvider.lookupStores(jenkinsRule.getInstance())
                .iterator()
                .next()
                .addCredentials(Domain.global(), credentials);
    }
}
