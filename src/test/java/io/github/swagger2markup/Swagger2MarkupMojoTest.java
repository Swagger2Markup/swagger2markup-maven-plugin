package io.github.swagger2markup;

import io.github.swagger2markup.markup.builder.MarkupLanguage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class Swagger2MarkupMojoTest {

    private static final String RESOURCES_DIR = "src/test/resources";
    private static final String SWAGGER_DIR = "/docs/swagger";
    private static final String INPUT_DIR = RESOURCES_DIR + SWAGGER_DIR;
    private static final String SWAGGER_OUTPUT_FILE = "swagger";
    private static final String SWAGGER_INPUT_FILE = "swagger.json";
    private static final String OUTPUT_DIR = "target/generated-docs";
    private File outputDir;

    @Before
    public void clearGeneratedData() throws Exception {
        outputDir = new File(OUTPUT_DIR);
        FileUtils.deleteQuietly(outputDir);
    }

    @Test
    public void shouldSkipExecution() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.outputFile = new File(OUTPUT_DIR, SWAGGER_OUTPUT_FILE).getAbsoluteFile();
        mojo.skip = true;

        //when
        mojo.execute();

        //then
        assertThat(mojo.outputFile).doesNotExist();
    }

    @Test
    public void shouldConvertIntoFile() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.swaggerInput = new File(INPUT_DIR, SWAGGER_INPUT_FILE).getAbsoluteFile().getAbsolutePath();
        mojo.outputFile = new File(OUTPUT_DIR, SWAGGER_OUTPUT_FILE).getAbsoluteFile();

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(outputDir);
        assertThat(outputFiles).containsOnly("swagger.adoc");
    }

    @Test
    public void shouldConvertIntoDirectory() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.swaggerInput = new File(INPUT_DIR, SWAGGER_INPUT_FILE).getAbsoluteFile().getAbsolutePath();
        mojo.outputDir = new File(OUTPUT_DIR).getAbsoluteFile();

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDir);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
    }

    @Test
    public void shouldConvertIntoDirectoryIfInputIsDirectory() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.swaggerInput = new File(RESOURCES_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.outputDir = new File(OUTPUT_DIR).getAbsoluteFile();

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(new File(mojo.outputDir, SWAGGER_DIR));
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
        outputFiles = recursivelyListFileNames(new File(mojo.outputDir, SWAGGER_DIR + "2"));
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
    }

    @Test
    public void shouldConvertIntoMarkdown() throws Exception {
        //given
        Map<String, String> config = new HashMap<>();
        config.put(Swagger2MarkupProperties.MARKUP_LANGUAGE, MarkupLanguage.MARKDOWN.toString());

        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.swaggerInput = new File(INPUT_DIR, SWAGGER_INPUT_FILE).getAbsoluteFile().getAbsolutePath();
        mojo.outputDir = new File(OUTPUT_DIR).getAbsoluteFile();
        mojo.config = config;

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDir);
        assertThat(outputFiles).containsOnly("definitions.md", "overview.md", "paths.md", "security.md");
    }

    @Test
    public void shouldConvertFromUrl() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.swaggerInput = "http://petstore.swagger.io/v2/swagger.json";
        mojo.outputDir = new File(OUTPUT_DIR).getAbsoluteFile();

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDir);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
    }

    @Test(expected=MojoFailureException.class)
    public void testMissingInputDirectory() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.swaggerInput = new File(INPUT_DIR, "non-existent").getAbsoluteFile().getAbsolutePath();

        //when
        mojo.execute();
    }

    @Test(expected = MojoFailureException.class)
    public void testUnreadableOutputDirectory() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.swaggerInput = new File(INPUT_DIR, SWAGGER_INPUT_FILE).getAbsoluteFile().getAbsolutePath();
        mojo.outputDir = Mockito.mock(File.class, (Answer) invocationOnMock -> {
            if (!invocationOnMock.getMethod().getName().contains("toString")) {
                throw new IOException("test exception");
            }
            return null;
        });

        //when
        mojo.execute();
    }


    private static Iterable<String> recursivelyListFileNames(File dir) throws Exception {
        return FileUtils.listFiles(dir, null, true).stream()
                .map(File::getName).collect(Collectors.toList());
    }

    private static void verifyFileContains(File file, String value) throws IOException {
        assertThat(IOUtils.toString(file.toURI(), StandardCharsets.UTF_8)).contains(value);
    }
}
