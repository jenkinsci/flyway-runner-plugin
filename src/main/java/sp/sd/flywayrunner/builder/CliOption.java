package sp.sd.flywayrunner.builder;

import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;


public enum CliOption {
    USERNAME("user"),
    PASSWORD,
    URL(), 
	LOCATIONS,
	LOG_LEVEL("logLevel");


    private String cliOption;

    CliOption(String cliOption) {
        this.cliOption = cliOption;
    }

    CliOption() {
    }

    public String getCliOption() {
        String optionName;
        if (cliOption == null) {
            optionName = name().toLowerCase();
        } else {
            optionName = cliOption;
        }
        return optionName;
    }
}