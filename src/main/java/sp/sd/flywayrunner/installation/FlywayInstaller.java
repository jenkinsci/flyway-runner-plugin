package sp.sd.flywayrunner.installation;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

public class FlywayInstaller extends DownloadFromUrlInstaller {
    private static final Logger LOG = Logger.getLogger(FlywayInstaller.class.getName());
    private String databaseDriverUrl;

    @DataBoundConstructor
    public FlywayInstaller(String id, String databaseDriverUrl) {
        super(id);
        this.databaseDriverUrl = databaseDriverUrl;
    }

    public FlywayInstaller(String id) {
        super(id);
    }

    public String getDatabaseDriverUrl() {
        return databaseDriverUrl;
    }

    public void setDatabaseDriverUrl(String databaseDriverUrl) {
        this.databaseDriverUrl = databaseDriverUrl;
    }

    @Extension
    public static final class FlywayInstallerDescriptorImpl
            extends DownloadFromUrlInstaller.DescriptorImpl<FlywayInstaller> {

        @Override
        public String getDisplayName() {
            return "Install from Maven Central";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == FlywayInstallation.class;
        }
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log)
            throws IOException, InterruptedException {
        FilePath installationRoot = super.performInstallation(tool, node, log);

        if (!Strings.isNullOrEmpty(databaseDriverUrl)) {
            // hidden feature: database driver urls can be comma delimited list
            Iterable<String> urls = Splitter.on(",").trimResults().split(databaseDriverUrl);
            for (String url : urls) {
                String filename = url.substring(databaseDriverUrl.lastIndexOf("/") + 1);
                FilePath child = installationRoot.child("drivers/" + filename);
                if (!child.exists()) {
                    log.getLogger().println("Downloading " + databaseDriverUrl + " to " + child + " on " + node.getDisplayName());
                    URL downloadUrl = new URL(databaseDriverUrl);
                    child.copyFrom(downloadUrl);
                }
            }
        }
        return installationRoot;
    }
}
