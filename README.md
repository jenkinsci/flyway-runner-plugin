Jenkins Flyway Plugin
=====================
Adds Flyway as an available build step. See Flyway documentation at https://flywaydb.org/documentation/.

Installation
---
    Install the flyway-runner plugin.
    Install flyway on the server where your job will run, including any database driver.

Configuration
---
    Add your Flyway installation in Manage Jenkins -> Configure System.   
    Note that the jar file containing your database driver should be located in FLYWAY_HOME/lib.
    Alternatively you may have Jenkins install flyway automatically from Maven central.
    Once defined, you may select "Invoke Flyway" as the step for any Jenkins Job.

Inspiration: https://github.com/prospero238/liquibase-runner
