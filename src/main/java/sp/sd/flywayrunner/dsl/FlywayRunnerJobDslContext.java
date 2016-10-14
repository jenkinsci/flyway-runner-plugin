package sp.sd.flywayrunner.dsl;

import javaposse.jobdsl.dsl.Context;

public class FlywayRunnerJobDslContext implements Context {

    String flywayCommand;
    String installationName;
    String url;
    String commandLineArgs;
    String locations;
    String credentialsId;

    void command(String flywayCommand) {
        this.flywayCommand = flywayCommand;
    }

    void name(String installationName) {
        this.installationName = installationName;
    }

    void url(String url) {
        this.url = url;
    }

    void commandLineArgs(String commandLineArgs) {
        this.commandLineArgs = commandLineArgs;
    }

    void locations(String locations) {
        this.locations = locations;
    }

    void credentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }
}
