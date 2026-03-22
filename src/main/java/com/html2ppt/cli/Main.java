package com.html2ppt.cli;

import com.html2ppt.layout.LayoutDebugger;
import com.html2ppt.layout.YogaLayoutEngine;
import com.html2ppt.model.SlideNode;
import com.html2ppt.parser.HtmlParser;
import com.html2ppt.renderer.PptRenderer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * CLI entry point for html2ppt.
 */
@Command(name = "html2ppt", mixinStandardHelpOptions = true, version = "1.0.0",
    description = "Compile HTML+CSS to PowerPoint presentations.",
    subcommands = {Main.CompileCommand.class})
public class Main implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Command(name = "compile", description = "Compile an HTML file to PPTX.")
    static class CompileCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Input HTML file")
        private Path inputFile;

        @Option(names = {"-o", "--output"}, description = "Output PPTX file")
        private Path outputFile;

        @Option(names = {"--debug"}, description = "Output layout debug file (.layout.txt)")
        private boolean debug;

        @Override
        public Integer call() throws Exception {
            if (!Files.exists(inputFile)) {
                System.err.println("Error: File not found: " + inputFile);
                return 1;
            }

            // Default output: same name with .pptx extension
            if (outputFile == null) {
                String name = inputFile.getFileName().toString();
                int dot = name.lastIndexOf('.');
                String base = dot > 0 ? name.substring(0, dot) : name;
                outputFile = inputFile.resolveSibling(base + ".pptx");
            }

            System.out.println("Parsing: " + inputFile);

            HtmlParser parser = new HtmlParser();
            String html = Files.readString(inputFile);

            // Parse metadata
            HtmlParser.PresentationMeta meta = parser.parseMeta(html);

            // Parse slides (use parseFile to resolve relative paths)
            List<SlideNode.Slide> slides = parser.parseFile(inputFile);
            System.out.println("Found " + slides.size() + " slide(s)");

            // Compute layout
            YogaLayoutEngine layoutEngine = new YogaLayoutEngine();
            float slideWidth = "4x3".equals(meta.layout()) ? 720f : 720f;
            float slideHeight = "4x3".equals(meta.layout()) ? 540f : 405f;
            for (SlideNode.Slide slide : slides) {
                layoutEngine.computeLayout(slide, slideWidth, slideHeight);
            }

            // Debug: dump layout tree
            if (debug) {
                Path debugPath = outputFile.resolveSibling(
                    outputFile.getFileName().toString().replace(".pptx", "") + ".layout.txt");
                LayoutDebugger.dumpToFile(slides, debugPath);
                System.out.println("Debug layout: " + debugPath);
            }

            // Render to PPTX
            PptRenderer renderer = new PptRenderer();
            var pptx = renderer.renderPresentation(slides, meta.layout());
            renderer.setMeta(pptx, meta.title(), meta.author(), meta.subject());

            // Write output
            try (var out = Files.newOutputStream(outputFile)) {
                pptx.write(out);
            }
            pptx.close();

            System.out.println("Written: " + outputFile);
            return 0;
        }
    }
}
