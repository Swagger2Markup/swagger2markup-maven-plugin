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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static io.github.swagger2markup.Swagger2MarkupProperties.*;

/**
 * Basic mojo to invoke the {@link Swagger2MarkupConverter}
 * during the maven build cycle
 */
@Mojo(name = "convertSwagger2markup")
public class Swagger2MarkupMojo extends AbstractMojo {

    @Parameter(property = PROPERTIES_PREFIX + ".input", defaultValue = "${project.basedir}/src/docs/swagger")
    protected String input;

    @Parameter(property = PROPERTIES_PREFIX + ".output", defaultValue = "${project.build.directory}/asciidoc")
    protected File output;

    @Parameter
    protected Map<String, String> config = new HashMap<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("convertSwagger2markup goal started");
            getLog().debug("Input: " + input);
            getLog().debug("Output: " + output);
            for(Map.Entry<String, String> entry : this.config.entrySet()){
                getLog().debug(entry.getKey() + ": " + entry.getValue());
            }
        }

        try{
            Swagger2MarkupConfig swagger2MarkupConfig = new Swagger2MarkupConfigBuilder(config).build();
            if(input.startsWith("http")){
                Swagger2MarkupConverter.from(URI.create(input))
                        .withConfig(swagger2MarkupConfig).build().toPath(output.toPath());
            }else {
                Path inputPath = Paths.get(input);
                if (Files.isDirectory(inputPath)) {
                    try {
                        Files.list(inputPath).forEach(path -> Swagger2MarkupConverter.from(path)
                                .withConfig(swagger2MarkupConfig).build().toPath(output.toPath()));
                    } catch (IOException e) {
                        throw new MojoFailureException(String.format("Failed to list files in directory %s", inputPath));
                    }
                } else {
                    Swagger2MarkupConverter.from(inputPath)
                            .withConfig(swagger2MarkupConfig).build().toPath(output.toPath());
                }
            }
        }catch(Exception e){
            throw new MojoFailureException("Failed to execute goal 'convertSwagger2markup'", e);
        }
        getLog().debug("convertSwagger2markup goal finished");
    }
}
