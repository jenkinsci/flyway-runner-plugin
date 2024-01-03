# Jenkins Flyway Plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/flyway-runner-plugin/job/main/badge/icon)](https://ci.jenkins.io/job/Plugins/job/flyway-runner-plugin/job/main/)
[![Coverage](https://ci.jenkins.io/job/Plugins/job/flyway-runner-plugin/job/main/badge/icon?status=${instructionCoverage}&subject=coverage&color=${colorInstructionCoverage})](https://ci.jenkins.io/job/Plugins/job/flyway-runner-plugin/job/main)
[![LOC](https://ci.jenkins.io/job/Plugins/job/flyway-runner-plugin/job/main/badge/icon?job=test&status=${lineOfCode}&subject=line%20of%20code&color=blue)](https://ci.jenkins.io/job/Plugins/job/flyway-runner-plugin/job/main)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/flyway-runner.svg)](https://plugins.jenkins.io/flyway-runner)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/flyway-runner-plugin.svg?label=changelog)](https://github.com/jenkinsci/flyway-runner-plugin/releases/latest)
[![GitHub license](https://img.shields.io/github/license/jenkinsci/flyway-runner-plugin)](https://github.com/jenkinsci/flyway-runner-plugin/blob/main/LICENSE.md)

Adds Flyway as an available build step. See Flyway documentation at https://flywaydb.org/documentation/.

## Installation

    Install the flyway-runner plugin.
    Install flyway on the server where your job will run, including any database driver.

## Configuration

    Add your Flyway installation in Manage Jenkins -> Configure System.   
    Note that the jar file containing your database driver should be located in FLYWAY_HOME/lib.
    Alternatively you may have Jenkins install flyway automatically from Maven central.
    Once defined, you may select "Invoke Flyway" as the step for any Jenkins Job.

Inspiration: https://github.com/prospero238/liquibase-runner
