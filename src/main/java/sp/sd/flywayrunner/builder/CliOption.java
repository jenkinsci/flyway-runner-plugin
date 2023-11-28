package sp.sd.flywayrunner.builder;

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

    CliOption() {}

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
