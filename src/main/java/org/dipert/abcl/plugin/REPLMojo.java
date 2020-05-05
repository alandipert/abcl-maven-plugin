package org.dipert.abcl.plugin;

import org.apache.commons.exec.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mojo(name = "repl", defaultPhase = LifecyclePhase.TEST_COMPILE, requiresDependencyResolution = ResolutionScope.TEST)
public class REPLMojo extends AbstractABCLMojo {
  public void execute() throws MojoExecutionException {
    List<String> args = new ArrayList<>();
    String mainClass = "org.armedbear.lisp.Main";

    outputDirectory.mkdirs();

    File[] sourceDirectory = getSourceDirectories(SourceDirectory.TEST, SourceDirectory.COMPILE);
    List<String> compileClasspathElements = getRunWithClasspathElements();
    String classpath = manifestClasspath(sourceDirectory, outputDirectory, compileClasspathElements);
    final String javaExecutable = getJavaExecutable();

    getLog().debug("Java executable used:  " + javaExecutable);
    getLog().debug("ABCL manifest classpath: " + classpath);

    CommandLine cl = new CommandLine(javaExecutable);
    cl.addArgument("-jar");
    File jar = createJar(classpath, mainClass);
    cl.addArgument(jar.getAbsolutePath(), false);

    getLog().debug("Command line: " + cl.toString());

    Executor exec = new DefaultExecutor();
    Map<String, String> env = new HashMap<String, String>(System.getenv());

    ExecuteStreamHandler handler = new AutoFlushingPumpStreamHandler(System.out, System.err, System.in);
    exec.setStreamHandler(handler);
    exec.setWorkingDirectory(getWorkingDirectory());
    ShutdownHookProcessDestroyer destroyer = new ShutdownHookProcessDestroyer();
    exec.setProcessDestroyer(destroyer);

    int status;
    Exception failureException = null;
    try {
      status = exec.execute(cl, env);
    } catch (ExecuteException e) {
      status = e.getExitValue();
      failureException = e;
    } catch (IOException e) {
      status = 1;
      failureException = e;
    }

    if (status != 0) {
      throw new MojoExecutionException("ABCL failed with exit value " + status + ".", failureException);
    }
  }
}
