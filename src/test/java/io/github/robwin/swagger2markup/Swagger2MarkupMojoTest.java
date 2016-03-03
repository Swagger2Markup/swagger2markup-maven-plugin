package io.github.robwin.swagger2markup;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class Swagger2MarkupMojoTest {

    private static final String INPUT_DIR = "src/test/resources/docs/swagger";
    private static final String SWAGGER_FILE = "swagger.json";
    private static final String OUTPUT_DIR = "target/generated-docs";
    private static final String DOCS_DIR = "src/test/resources/docs";

    @Before
    public void clearGeneratedData() throws Exception {
        FileUtils.deleteDirectory(new File(OUTPUT_DIR));
    }

    @Test
    public void testSwagger2MarkupConvertsSwaggerFileToAsciidoc() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.inputDirectory = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.swaggerFile = SWAGGER_FILE;
        mojo.outputDirectory = new File(OUTPUT_DIR + "/asciidoc").getAbsoluteFile();
        mojo.separateDefinitions = false;

        //when
        mojo.execute();

        //then
        assertThat(mojo.inputDirectory).isEqualTo(new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath());
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDirectory);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc");
    }

    @Test
    public void testSwagger2MarkupConvertsSwaggerToAsciiDoc() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.inputDirectory = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.outputDirectory = new File(OUTPUT_DIR + "/asciidoc").getAbsoluteFile();
        mojo.separateDefinitions = false;

        //when
        mojo.execute();

        //then
        assertThat(mojo.inputDirectory).isEqualTo(new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath());
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDirectory);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc");
    }

    @Test
    public void testSwagger2MarkupConvertsSwaggerToMarkdown() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.inputDirectory = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.outputDirectory = new File(OUTPUT_DIR + "/markdown").getAbsoluteFile();
        mojo.markupLanguage = "markdown";

        //when
        mojo.execute();

        //then
        assertThat(mojo.inputDirectory).isEqualTo(new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath());
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDirectory);
        assertThat(outputFiles).containsOnly("definitions.md", "overview.md", "paths.md");
    }

    @Test
    public void testSwagger2MarkupEnableWithDescriptionsWithExamplesAndWithSchemas() throws Exception {
        //when
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.inputDirectory = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.outputDirectory = new File(OUTPUT_DIR + "/asciidoc").getAbsoluteFile();
        mojo.markupLanguage = "asciidoc";
        mojo.schemasDirectory = new File(DOCS_DIR, "schemas").getAbsoluteFile();
        mojo.descriptionsDirectory = new File(DOCS_DIR, "descriptions").getAbsoluteFile();
        mojo.examplesDirectory = new File(DOCS_DIR, "examples").getAbsoluteFile();
        mojo.separateDefinitions = true;
        mojo.execute();

        //then
        assertThat(mojo.inputDirectory).isEqualTo(new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath());
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDirectory);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc", "pet.adoc");

        final File definitionsFile = new File(mojo.outputDirectory, "definitions.adoc");
        final File pathsFile = new File(mojo.outputDirectory, "paths.adoc");
        verifyFileContains(definitionsFile, "{\"test\":\"schema\"}");
        verifyFileContains(definitionsFile, "test-description");
        verifyFileContains(pathsFile, "test-curl");
        verifyFileContains(pathsFile, "test-http-request");
        verifyFileContains(pathsFile, "test-http-response");
    }

    @Test
    public void testDebugLogging() throws Exception {
        //mock enable debugging
        Swagger2MarkupMojo mojo = Mockito.spy(new Swagger2MarkupMojo());
        Log logSpy = Mockito.spy(mojo.getLog());
        when(logSpy.isDebugEnabled()).thenReturn(true);
        when(mojo.getLog()).thenReturn(logSpy);

        //given
        mojo.inputDirectory = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.outputDirectory = new File(OUTPUT_DIR + "/markdown").getAbsoluteFile();
        mojo.markupLanguage = "markdown";

        //when
        mojo.execute();

        //then
        assertThat(mojo.inputDirectory).isEqualTo(new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath());
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDirectory);
        assertThat(outputFiles).containsOnly("definitions.md", "overview.md", "paths.md");
    }

    @Test
    public void testNullOutputDirectory() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.inputDirectory = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.projectBuildDir = new File(OUTPUT_DIR).getAbsolutePath();

        //when
        mojo.execute();

        //then
        assertThat(mojo.inputDirectory).isEqualTo(new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath());
        Iterable<String> outputFiles = recursivelyListFileNames(mojo.outputDirectory);
        FileUtils.deleteDirectory(mojo.outputDirectory);
        assertThat(outputFiles).containsOnly("definitions.adoc", "overview.adoc", "paths.adoc");
    }

    @Test(expected=MojoFailureException.class)
    public void testMissingInputDirectory() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.inputDirectory = new File(INPUT_DIR, "non-existent").getAbsoluteFile().getAbsolutePath();

        //when
        mojo.execute();
    }

    @Test(expected=MojoFailureException.class)
    public void testEmptyInputDirectory() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        File emptyDir = new File(OUTPUT_DIR, "empty");
        FileUtils.forceMkdir(emptyDir);
        mojo.inputDirectory = emptyDir.getAbsoluteFile().getAbsolutePath();

        //when
        mojo.execute();
    }

    @Test(expected = MojoFailureException.class)
    public void testUnreadableOutputDirectory() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.inputDirectory = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.outputDirectory = Mockito.mock(File.class, new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (!invocationOnMock.getMethod().getName().contains("toString")) {
                    throw new IOException("test exception");
                }
                return null;
            }
        });

        //when
        mojo.execute();
    }

    @Test
    public void testSwagger2MarkupConvertsWithRussianOutput() throws Exception {
        //given
        Swagger2MarkupMojo mojo = new Swagger2MarkupMojo();
        mojo.inputDirectory = new File(INPUT_DIR).getAbsoluteFile().getAbsolutePath();
        mojo.outputDirectory = new File(OUTPUT_DIR + "/asciidoc").getAbsoluteFile();
        mojo.separateDefinitions = false;
        mojo.outputLanguage = Language.RU;

        //when
        mojo.execute();

        //then
        assertThat(new String(java.nio.file.Files
                .readAllBytes(Paths.get(mojo.outputDirectory + File.separator + "definitions.adoc"))))
                .contains("== Определения");
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