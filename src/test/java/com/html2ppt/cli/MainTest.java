package com.html2ppt.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    @Test
    void mainCommandWithoutSubcommandReturnsZero() {
        int exitCode = new CommandLine(new Main()).execute();

        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void compileCommandReturnsErrorForMissingFile(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("missing.html");

        int exitCode = new CommandLine(new Main()).execute("compile", missing.toString());

        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    void compileCommandCreatesDefaultOutputFile(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("deck.html");
        Files.writeString(input, """
            <html data-title="Deck Title" data-author="Tester">
              <body>
                <section><h1>Hello</h1><p>World</p></section>
              </body>
            </html>
            """);

        int exitCode = new CommandLine(new Main()).execute("compile", input.toString());

        assertThat(exitCode).isEqualTo(0);
        assertThat(tempDir.resolve("deck.pptx")).exists();
    }

    @Test
    void compileCommandCreatesExplicitOutputAndDebugDump(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("report.html");
        Path output = tempDir.resolve("custom-output.pptx");
        Files.writeString(input, """
            <html data-layout="4x3">
              <body>
                <section><h1>Quarterly Report</h1><p>Status</p></section>
              </body>
            </html>
            """);

        int exitCode = new CommandLine(new Main()).execute(
            "compile", input.toString(), "-o", output.toString(), "--debug");

        assertThat(exitCode).isEqualTo(0);
        assertThat(output).exists();
        assertThat(tempDir.resolve("custom-output.layout.txt")).exists();
    }
}
