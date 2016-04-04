package io.github.swagger2markup;

import io.github.swagger2markup.markup.builder.MarkupLanguage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class Swagger2MarkupMojoTest {

    private static final String INPUT_DIR = "src/test/resources/docs/swagger";
    private static final String SWAGGER_FILE = "swagger.json";
    private static final String OUTPUT_DIR = "target/generated-docs";

    @Before
    public void clearGeneratedData() throws Exception {
        File output = new File(OUTPUT_DIR);
        FileUtils.deleteQuietly(output);
        Files.createDirectory(output.toPath());
    }

    @Test
    public void testSwagger2MarkupConvertsSwaggerFileToAsciidoc() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.input = new File(INPUT_DIR, SWAGGER_FILE).getAbsoluteFile().getAbsolutePath();
        mojo.output = new File(OUTPUT_DIR).getAbsoluteFile();

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.output);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
    }

    @Test
    public void testSwagger2MarkupConvertsSwaggerToAsciiDoc() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.input = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.output = new File(OUTPUT_DIR).getAbsoluteFile();

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.output);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
    }

    @Test
    public void testSwagger2MarkupConvertsSwaggerToMarkdown() throws Exception {
        //given
        Map<String, String> config = new HashMap<>();
        config.put(Swagger2MarkupProperties.MARKUP_LANGUAGE, MarkupLanguage.MARKDOWN.toString());

        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.input = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.output = new File(OUTPUT_DIR).getAbsoluteFile();
        mojo.config = config;

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.output);
        assertThat(outputFiles).containsOnly("definitions.md", "overview.md", "paths.md", "security.md");
    }

    @Test
    public void testDebugLogging() throws Exception {
        //mock enable debugging
        Swagger2MarkupMojo mojo = Mockito.spy(new Swagger2MarkupMojo());
        Log logSpy = Mockito.spy(mojo.getLog());
        when(logSpy.isDebugEnabled()).thenReturn(true);
        when(mojo.getLog()).thenReturn(logSpy);

        //given
        mojo.input = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.output = new File(OUTPUT_DIR).getAbsoluteFile();

        //when
        mojo.execute();

        //then
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.output);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "security.adoc");
    }

    @Test(expected=MojoFailureException.class)
    public void testMissingInputDirectory() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.input = new File(INPUT_DIR, "non-existent").getAbsoluteFile().getAbsolutePath();

        //when
        mojo.execute();
    }

    @Test(expected = MojoFailureException.class)
    public void testUnreadableOutputDirectory() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.input = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.output = Mockito.mock(File.class, (Answer) invocationOnMock -> {
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