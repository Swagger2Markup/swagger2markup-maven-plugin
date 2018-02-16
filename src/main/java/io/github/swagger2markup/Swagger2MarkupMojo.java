/*
 * Copyright 2016 Robert Winkler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.swagger2markup;

import io.github.swagger2markup.builder.Swagger2MarkupConfigBuilder;
import io.github.swagger2markup.utils.URIUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Basic mojo to invoke the {@link Swagger2MarkupConverter}
 * during the maven build cycle
 */
@Mojo(name = "convertSwagger2markup")
public class Swagger2MarkupMojo extends AbstractMojo {

    @Parameter(property = "swaggerInput", required = true)
    protected String swaggerInput;

    @Parameter(property = "outputDir")
    protected File outputDir;

    @Parameter(property = "outputFile")
    protected File outputFile;

    @Parameter
    protected Map<String, String> config = new HashMap<>();

    @Parameter(property = "skip")
    protected boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().info("convertSwagger2markup is skipped.");
            return;
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug("convertSwagger2markup goal started");
            getLog().debug("swaggerInput: " + swaggerInput);
            getLog().debug("outputDir: " + outputDir);
            getLog().debug("outputFile: " + outputFile);
            for (Map.Entry<String, String> entry : this.config.entrySet()) {
                getLog().debug(entry.getKey() + ": " + entry.getValue());
            }
        }

        try {
            Swagger2MarkupConfig swagger2MarkupConfig = new Swagger2MarkupConfigBuilder(config).build();
            if (isLocalFolder(swaggerInput)) {
                getSwaggerFiles(new File(swaggerInput), true).forEach(f -> {
                        Swagger2MarkupConverter converter = Swagger2MarkupConverter.from(f.toURI())
                                                                               .withConfig(swagger2MarkupConfig)
                                                                               .build();
                        swaggerToMarkup(converter, true);
                });
            } else {
                Swagger2MarkupConverter converter = Swagger2MarkupConverter.from(URIUtils.create(swaggerInput))
                                                                           .withConfig(swagger2MarkupConfig).build();
                swaggerToMarkup(converter, false);
            }
        } catch (Exception e) {
            throw new MojoFailureException("Failed to execute goal 'convertSwagger2markup'", e);
        }
        getLog().debug("convertSwagger2markup goal finished");
    }

    private boolean isLocalFolder(String swaggerInput) {
        return !swaggerInput.toLowerCase().startsWith("http") && new File(swaggerInput).isDirectory();
    }

    private void swaggerToMarkup(Swagger2MarkupConverter converter, boolean inputIsLocalFolder) {
        if (outputFile != null) {
            converter.toFile(outputFile.toPath());
        } else if (outputDir != null) {
            File effectiveOutputDir = outputDir;
            if (inputIsLocalFolder) {
                effectiveOutputDir = getEffectiveOutputDirWhenInputIsAFolder(converter);
            }
            converter.toFolder(effectiveOutputDir.toPath());
        } else {
            throw new IllegalArgumentException("Either outputFile or outputDir parameter must be used");
        }
    }

    private File getEffectiveOutputDirWhenInputIsAFolder(Swagger2MarkupConverter converter) {
        String outputDirAddendum = getInputDirStructurePath(converter);
        if (multipleSwaggerFilesInSwaggerLocationFolder(converter)) {
            /*
             * If the folder the current Swagger file resides in contains at least one other Swagger file then the
             * output dir must have an extra subdir per file to avoid markdown files getting overwritten.
             */
            outputDirAddendum += "/" + extracSwaggerFileNameWithoutExtension(converter);
        }
        return new File(outputDir, outputDirAddendum);
    }

    private String getInputDirStructurePath(Swagger2MarkupConverter converter) {
        /*
         * When the Swagger input is a local folder (e.g. /Users/foo/) you'll want to group the generated output in the
         * configured output directory. The most obvious approach is to replicate the folder structure from the input
         * folder to the output folder. Example:
         * - swaggerInput is set to /Users/foo
         * - there's a single Swagger file at /Users/foo/bar-service/v1/bar.yaml
         * - outputDir is set to /tmp/asciidoc
         * -> markdown files from bar.yaml are generated to /tmp/asciidoc/bar-service/v1
         */
        String swaggerFilePath = converter.getContext().getSwaggerLocation().getPath(); // /Users/foo/bar-service/v1/bar.yaml
        String swaggerFileFolder = StringUtils.substringBeforeLast(swaggerFilePath, File.separator); // /Users/foo/bar-service/v1
        return StringUtils.remove(swaggerFileFolder, swaggerInput); // /bar-service/v1
    }

    private boolean multipleSwaggerFilesInSwaggerLocationFolder(Swagger2MarkupConverter converter) {
        Collection<File> swaggerFiles = getSwaggerFiles(new File(converter.getContext().getSwaggerLocation())
          .getParentFile(), false);
        return swaggerFiles != null && swaggerFiles.size() > 1;
    }

    private String extracSwaggerFileNameWithoutExtension(Swagger2MarkupConverter converter) {
        return FilenameUtils.removeExtension(new File(converter.getContext().getSwaggerLocation()).getName());
    }

    private Collection<File> getSwaggerFiles(File directory, boolean recursive) {
        return FileUtils.listFiles(directory, new String[]{"yaml", "yml", "json"}, recursive);
    }
}
