package io.github.ih0rd.codegen;

import io.github.ih0rd.contract.*;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

/**
 * Maven plugin that generates Java interfaces from polyglot script contracts.
 *
 * <p>
 * The plugin scans the configured input directory for supported script files
 * (e.g. Python, JavaScript), extracts contract definitions and generates
 * corresponding Java interfaces.
 * </p>
 *
 * <p>
 * The plugin is bound to the {@code generate-sources} lifecycle phase and
 * automatically registers the generated sources directory as a compile source root.
 * </p>
 *
 * <h3>Default behaviour</h3>
 * <ul>
 *     <li>Input directory: {@code src/main/resources}</li>
 *     <li>Output directory: {@code target/generated-sources/polyglot}</li>
 *     <li>Base package: {@code ${project.groupId}.polyglot}</li>
 * </ul>
 *
 * <p>
 * If {@code basePackage} is not explicitly configured, it falls back to
 * {@code ${project.groupId}.polyglot}.
 * </p>
 */
@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        threadSafe = true
)
public final class PolyglotCodegenMojo extends AbstractMojo {

    /**
     * Directory containing polyglot script contracts.
     *
     * <p>Default: {@code src/main/resources}</p>
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources")
    private File inputDirectory;

    /**
     * Output directory for generated Java source files.
     *
     * <p>Default: {@code target/generated-sources/polyglot}</p>
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/polyglot")
    private File outputDirectory;

    /**
     * Base package used for generated Java interfaces.
     *
     * <p>If not specified, defaults to:
     * {@code ${project.groupId}.polyglot}</p>
     */
    @Parameter
    private String basePackage;

    /**
     * Current Maven project instance.
     *
     * <p>Injected automatically by Maven.</p>
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Project groupId used for resolving default base package.
     */
    @Parameter(defaultValue = "${project.groupId}", readonly = true)
    private String projectGroupId;

    /**
     * Executes the plugin.
     *
     * @throws MojoExecutionException if validation or generation fails
     */
    @Override
    public void execute() throws MojoExecutionException {

        validateInputDirectory();

        Path outputRoot = prepareOutputDirectory();

        String effectivePackage = resolveBasePackage();

        generateContracts(outputRoot, effectivePackage);

        project.addCompileSourceRoot(outputRoot.toAbsolutePath().toString());
    }

    private void validateInputDirectory() throws MojoExecutionException {

        if (inputDirectory == null || !inputDirectory.exists()) {
            throw new MojoExecutionException(
                    "Input directory does not exist: " + inputDirectory
            );
        }

        if (!inputDirectory.isDirectory()) {
            throw new MojoExecutionException(
                    "Input path is not a directory: " + inputDirectory
            );
        }
    }

    private Path prepareOutputDirectory() throws MojoExecutionException {
        try {
            Path path = outputDirectory.toPath();
            Files.createDirectories(path);
            return path;
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to create output directory", e
            );
        }
    }

    private String resolveBasePackage() {
        if (basePackage == null || basePackage.isBlank()) {
            return projectGroupId + ".polyglot";
        }
        return basePackage;
    }

    private void generateContracts(Path outputRoot, String effectivePackage)
            throws MojoExecutionException {

        ContractGenerator generator = new DefaultContractGenerator();
        JavaInterfaceGenerator javaGenerator = new JavaInterfaceGenerator();

        try (Stream<Path> files = Files.walk(inputDirectory.toPath())) {

            files
                    .filter(Files::isRegularFile)
                    .filter(this::isSupported)
                    .forEach(path ->
                            generateForScript(
                                    path,
                                    generator,
                                    javaGenerator,
                                    outputRoot,
                                    effectivePackage
                            )
                    );

        } catch (IOException e) {
            throw new MojoExecutionException("Failed scanning input directory", e);
        }
    }

    private boolean isSupported(Path path) {
        try {
            SupportedLanguage.fromFileName(path.getFileName().toString());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void generateForScript(
            Path script,
            ContractGenerator generator,
            JavaInterfaceGenerator javaGenerator,
            Path outputRoot,
            String effectivePackage
    ) {

        try {
            String source = Files.readString(script);

            SupportedLanguage language =
                    SupportedLanguage.fromFileName(
                            script.getFileName().toString()
                    );

            ScriptDescriptor descriptor =
                    new ScriptDescriptor(
                            language,
                            source,
                            script.getFileName().toString()
                    );

            ContractModel model =
                    generator.generate(descriptor, new CodegenConfig(false));

            for (ContractClass contract : model.classes()) {

                String javaSource =
                        javaGenerator.generate(contract, effectivePackage);

                Path target =
                        outputRoot
                                .resolve(effectivePackage.replace('.', '/'))
                                .resolve(contract.name() + ".java");

                Files.createDirectories(target.getParent());
                Files.writeString(target, javaSource);

                getLog().info("Generated: " + target);
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed processing script: " + script, e
            );
        }
    }
}