package org.dipert.abcl.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static java.util.Optional.ofNullable;

public abstract class AbstractABCLMojo extends AbstractMojo {
    @Component
    private ToolchainManager toolchainManager;

    @Parameter(required = true, readonly = true, property = "session")
    private MavenSession session;

    @Parameter(required = true, defaultValue = "${project.build.outputDirectory}/../generated-sources")
    protected File generatedSourceDirectory;

    @Parameter(required = true, readonly = true, property = "basedir")
    protected File baseDirectory;

    @Parameter protected String[] sourceDirectories = new String[] {"src/main/abcl"};

    @Parameter protected String[] testSourceDirectories = new String[] {"src/test/abcl"};

    @Parameter(required = true, defaultValue = "${project.build.testSourceDirectory}")
    protected File baseTestSourceDirectory;

    @Parameter(required = true, defaultValue = "${project.build.outputDirectory}")
    protected File outputDirectory;

    @Parameter(required = true, readonly = true, property = "project.compileClasspathElements")
    protected List<String> classpathElements;

    @Parameter protected File workingDirectory;

    public List<String> getRunWithClasspathElements() {
        Set<String> classPathElements = new HashSet<String>();
        classPathElements.addAll(classpathElements);
        return new ArrayList<String>(classPathElements);
    }

    public String getDefaultJavaHomeExecutable(Map<String, String> env) {
        return ofNullable(env.get("JAVA_HOME"))
                .map(home -> Paths.get(home, "bin").toString() + "/")
                .orElse("")
                + "java";
    }

    public String getJavaExecutable() throws MojoExecutionException {

        Toolchain tc =
                toolchainManager.getToolchainFromBuildContext(
                        "jdk", //NOI18N
                        session);
        if (tc != null) {
            getLog().info("Toolchain in abcl-maven-plugin: " + tc);
            String foundExecutable = tc.findTool("java");
            if (foundExecutable != null) {
                return foundExecutable;
            } else {
                throw new MojoExecutionException("Unable to find 'java' executable for toolchain: " + tc);
            }
        }

        return getDefaultJavaHomeExecutable(System.getenv());
    }

    public enum SourceDirectory {
        COMPILE,
        TEST
    }

    private File[] translatePaths(String[] paths) {
        File[] files = new File[paths.length];
        for (int i = 0; i < paths.length; i++) {
            files[i] = new File(baseDirectory, paths[i]);
        }
        return files;
    }

    public File[] getSourceDirectories(SourceDirectory... sourceDirectoryTypes) {
        List<File> dirs = new ArrayList<File>();

        if (Arrays.asList(sourceDirectoryTypes).contains(SourceDirectory.COMPILE)) {
            dirs.add(generatedSourceDirectory);
            dirs.addAll(Arrays.asList(translatePaths(sourceDirectories)));
        }
        if (Arrays.asList(sourceDirectoryTypes).contains(SourceDirectory.TEST)) {
            dirs.add(baseTestSourceDirectory);
            dirs.addAll(Arrays.asList(translatePaths(testSourceDirectories)));
        }

        return dirs.toArray(new File[] {});
    }


    protected String getPath(File[] sourceDirectory) {
        String cp = "";
        for (File directory : sourceDirectory) {
            cp = cp + directory.getPath() + File.pathSeparator;
        }
        return cp.substring(0, cp.length() - 1);
    }

    protected String manifestClasspath(final File[] sourceDirectory, final File outputDirectory, final List<String> compileClasspathElements) {
        String cp = getPath(sourceDirectory);

        cp = cp + outputDirectory.toURI() + " ";

        for (String classpathElement : compileClasspathElements) {
            cp = cp + new File(classpathElement).toURI() + " ";
        }

        cp = cp.replaceAll("\\s+", "\\ ");
        return cp;
    }

    protected File createJar(final String cp, final String mainClass) {
        try {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, cp);
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
            File tempFile = File.createTempFile("abclmavenplugin", "jar");
            tempFile.deleteOnExit();
            JarOutputStream target = new JarOutputStream(new FileOutputStream(tempFile), manifest);
            target.close();
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected File getWorkingDirectory() throws MojoExecutionException {
        if (workingDirectory != null) {
            if (workingDirectory.exists()) {
                return workingDirectory;
            } else {
                throw new MojoExecutionException("Directory specified in <workingDirectory/> does not exists: " + workingDirectory.getPath());
            }
        } else {
            return session.getCurrentProject().getBasedir();
        }
    }
}
