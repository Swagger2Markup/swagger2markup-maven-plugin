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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static io.github.swagger2markup.Swagger2MarkupProperties.PROPERTIES_PREFIX;

/**
 * Basic mojo to invoke the {@link Swagger2MarkupConverter}
 * during the maven build cycle
 */
@Mojo(name = "convertSwagger2markup")
public class Swagger2MarkupMojo extends AbstractMojo {

    @Parameter(property = PROPERTIES_PREFIX + ".input")
    protected String input;

    @Parameter(property = PROPERTIES_PREFIX + ".outputDir")
    protected File outputDir;

    @Parameter(property = PROPERTIES_PREFIX + ".outputFile")
    protected File outputFile;

    @Parameter
    protected Map<String, String> config = new HashMap<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("convertSwagger2markup goal started");
            getLog().debug("input: " + input);
            getLog().debug("outputDir: " + outputDir);
            getLog().debug("outputFile: " + outputFile);
            for(Map.Entry<String, String> entry : this.config.entrySet()){
                getLog().debug(entry.getKey() + ": " + entry.getValue());
            }
        }

        try{
            Swagger2MarkupConfig swagger2MarkupConfig = new Swagger2MarkupConfigBuilder(config).build();
            Swagger2MarkupConverter converter = Swagger2MarkupConverter.from(URIUtils.create(input))
                    .withConfig(swagger2MarkupConfig).build();

            if(outputFile != null){
                converter.toFile(outputFile.toPath());
            }else if (outputDir != null){
                converter.toFolder(outputDir.toPath());
            }else {
                throw new IllegalArgumentException("Either outputFile or outputDir parameter must be used");
            }
        }catch(Exception e){
            throw new MojoFailureException("Failed to execute goal 'convertSwagger2markup'", e);
        }
        getLog().debug("convertSwagger2markup goal finished");
    }
}
