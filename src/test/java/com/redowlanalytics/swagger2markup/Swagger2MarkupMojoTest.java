package com.redowlanalytics.swagger2markup;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class Swagger2MarkupMojoTest {

    private static final String INPUT_DIR = "src/test/resources/docs/swagger";
    private static final String OUTPUT_DIR = "target/generated-docs";
    private static final String DOCS_DIR = "src/test/resources/docs";

    @Before
    public void clearGeneratedData() throws Exception {
        FileUtils.deleteDirectory(new File(OUTPUT_DIR));
    }

    @Test
    public void testSwagger2MarkupConvertsSwaggerToAsciiDoc() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.inputDirectory = new File(INPUT_DIR).getAbsoluteFile();
        mojo.outputDirectory = new File(OUTPUT_DIR + "/asciidoc").getAbsoluteFile();

        //when
        mojo.execute();

        //then
        assertThat(mojo.inputDirectory).isEqualTo(new File(INPUT_DIR).getAbsoluteFile());
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDirectory);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc");
    }

    @Test
    public void testSwagger2MarkupConvertsSwaggerToMarkdown() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.inputDirectory = new File(INPUT_DIR).getAbsoluteFile();
        mojo.outputDirectory = new File(OUTPUT_DIR + "/markdown").getAbsoluteFile();
        mojo.markupLanguage = "MARKDOWN";

        //when
        mojo.execute();

        //then
        assertThat(mojo.inputDirectory).isEqualTo(new File(INPUT_DIR).getAbsoluteFile());
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDirectory);
        assertThat(outputFiles).containsOnly("definitions.md", "overview.md", "paths.md");
    }

    @Test
    public void testSwagger2MarkupEnableWithDescriptionsWithExamplesAndWithSchemas() throws Exception {
        //when
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.inputDirectory = new File(INPUT_DIR).getAbsoluteFile();
        mojo.outputDirectory = new File(OUTPUT_DIR + "/asciidoc").getAbsoluteFile();
        mojo.markupLanguage = "asciidoc";
        mojo.schemasDirectory = new File(DOCS_DIR, "schemas").getAbsoluteFile();
        mojo.descriptionsDirectory = new File(DOCS_DIR, "descriptions").getAbsoluteFile();
        mojo.examplesDirectory = new File(DOCS_DIR, "examples").getAbsoluteFile();
        mojo.execute();

        //then
        assertThat(mojo.inputDirectory).isEqualTo(new File(INPUT_DIR).getAbsoluteFile());
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDirectory);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc");

        final File definitionsFile = new File(mojo.outputDirectory, "definitions.adoc");
        final File pathsFile = new File(mojo.outputDirectory, "paths.adoc");
        verifyFileContains(definitionsFile, "{\"test\":\"schema\"}");
        verifyFileContains(definitionsFile, "test-description");
        verifyFileContains(pathsFile, "test-curl");
        verifyFileContains(pathsFile, "test-http-request");
        verifyFileContains(pathsFile, "test-http-response");
    }

    private static Iterable<String> recursivelyListFileNames(File dir) throws Exception {
        Function<File, String> transform = new Function<File, String>() {
            @Nullable
            @Override
            public String apply(File file) {
                return file.getName();
            }
        };
        return Iterables.transform(FileUtils.listFiles(dir, null, true), transform);
    }

    private static void verifyFileContains(File file, String value) throws IOException {
        assertThat(Files.toString(file, Charsets.UTF_8)).contains(value);
    }
}