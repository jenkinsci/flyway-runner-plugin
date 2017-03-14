package sp.sd.flywayrunner.dsl;

import hudson.Extension;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;
import sp.sd.flywayrunner.builder.FlywayBuilder;
import javaposse.jobdsl.dsl.RequiresPlugin;

/*
 ```
 For example:
 ```
    freeStyleJob('FlywayRunnerJob') {
        steps {
          flywayRunner {
            name('flyway')
            command('migrate')
            url('jdbc:mysql://mysqlserver:3306/mydb')
            locations('filesystem:$WORKSPACE/dbscripts')
            credentialsId('44620c50-1589-4617-a677-7563985e46e1')
          }
        }
    }
*/

@Extension(optional = true)
public class FlywayRunnerJobDslExtension extends ContextExtensionPoint {
    @DslExtensionMethod(context = StepContext.class)
    @RequiresPlugin(id = "flyway-runner", minimumVersion = "1.6")
    public Object flywayRunner(Runnable closure) {
        FlywayRunnerJobDslContext context = new FlywayRunnerJobDslContext();
        executeInContext(closure, context);
        return new FlywayBuilder(context.installationName, context.flywayCommand, context.url,
                context.locations, context.commandLineArgs, context.credentialsId);
    }
}
