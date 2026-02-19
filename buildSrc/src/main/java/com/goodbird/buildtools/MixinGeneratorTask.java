package com.goodbird.buildtools;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public abstract class MixinGeneratorTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getReferenceDir();

    @InputDirectory
    public abstract DirectoryProperty getForkDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getMixinPackage();

    @OutputFile
    public abstract RegularFileProperty getMixinClassListOutput();

    private static final String FORK_PKG = "com.goodbird.mindofthecolony.mc.";
    private static final String ORIG_PKG = "com.minecolonies.";

    @TaskAction
    public void generate() throws Exception {
        File refRoot = getReferenceDir().get().getAsFile();
        File forkRoot = getForkDir().get().getAsFile();
        File outRoot = getOutputDir().get().getAsFile();
        String mixinPkg = getMixinPackage().get();

        // Clean generated mixin output (preserve other generated content)
        Path genPkgDir = outRoot.toPath().resolve(mixinPkg.replace('.', File.separatorChar));
        deleteRecursively(genPkgDir);
        Files.createDirectories(genPkgDir);

        List<String> generatedMixinClasses = new ArrayList<>();
        int skippedParseErrors = 0;

        // Walk all fork files
        List<Path> forkFiles;
        try (Stream<Path> stream = Files.walk(forkRoot.toPath())) {
            forkFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
        }

        ForkDiffer differ = new ForkDiffer();
        MixinClassWriter writer = new MixinClassWriter();

        for (Path forkPath : forkFiles) {
            try {
                String relPath = forkRoot.toPath().relativize(forkPath).toString();
                Path refPath = refRoot.toPath().resolve(relPath);

                if (!Files.exists(refPath)) {
                    getLogger().debug("New fork file (no reference): {}", relPath);
                    continue;
                }

                String forkSource = Files.readString(forkPath);
                String refSource = Files.readString(refPath);

                // Un-rename fork source for comparison
                String normalizedFork = unrename(forkSource);

                if (normalizedFork.equals(refSource)) {
                    continue; // Only package rename, no real changes
                }

                // Analyze differences
                ForkDiffer.DiffResult diff = differ.diff(refSource, normalizedFork);

                if (diff.isEmpty()) {
                    continue;
                }

                // Compute target MC class FQCN from relative path
                // relPath = "core/colony/CitizenData.java" -> "com.minecolonies.core.colony.CitizenData"
                String targetFqcn = ORIG_PKG + relPath
                    .replace(File.separatorChar, '.')
                    .replace('/', '.')
                    .replace(".java", "");

                // Compute mixin class name from relative path
                // "core/colony/CitizenData.java" -> "Generated_core_colony_CitizenData"
                String mixinClassName = "Generated_" + relPath
                    .replace(File.separatorChar, '_')
                    .replace('/', '_')
                    .replace(".java", "");

                // Generate mixin source
                String mixinSource = writer.writeMixin(
                    mixinPkg, mixinClassName, targetFqcn, diff, forkSource, refSource);

                // Un-rename MC types in generated mixin, preserve real MOTC types
                mixinSource = unrenameForMixin(mixinSource);

                // Write output file
                Path outPath = genPkgDir.resolve(mixinClassName + ".java");
                Files.writeString(outPath, mixinSource);

                generatedMixinClasses.add(mixinPkg + "." + mixinClassName);
                getLogger().lifecycle("Generated mixin: {} ({} changed methods, {} new fields, {} new methods)",
                    mixinClassName, diff.changedMethods.size(), diff.newFields.size(), diff.newMethods.size());

            } catch (Exception e) {
                skippedParseErrors++;
                getLogger().warn("Failed to process {}: {}", forkPath.getFileName(), e.getMessage());
            }
        }

        // Write class list
        Files.createDirectories(getMixinClassListOutput().get().getAsFile().toPath().getParent());
        Files.writeString(
            getMixinClassListOutput().get().getAsFile().toPath(),
            String.join("\n", generatedMixinClasses));

        getLogger().lifecycle("Generated {} fork mixins ({} files skipped due to parse errors)",
            generatedMixinClasses.size(), skippedParseErrors);
    }

    /**
     * Un-rename fork source: replace renamed MC packages back to original.
     */
    static String unrename(String source) {
        return source.replace(FORK_PKG, ORIG_PKG);
    }

    /**
     * Un-rename for generated mixin code: replace MC fork references back to original,
     * but preserve references to actual MOTC types (com.goodbird.mindofthecolony.mixin.*, etc.)
     */
    static String unrenameForMixin(String source) {
        // Strategy: protect real MOTC references, then bulk un-rename, then restore.
        // Real MOTC packages are com.goodbird.mindofthecolony.{anything except mc.}
        String PLACEHOLDER = "\u0000MOTC_PKG\u0000";

        // Protect non-mc MOTC references
        // Match com.goodbird.mindofthecolony. followed by a word char that is NOT "mc."
        source = source.replaceAll(
            "com\\.goodbird\\.mindofthecolony\\.(?!mc\\.)",
            PLACEHOLDER);

        // Un-rename mc references
        source = source.replace(FORK_PKG, ORIG_PKG);

        // Restore protected references
        source = source.replace(PLACEHOLDER, "com.goodbird.mindofthecolony.");

        return source;
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            try (Stream<Path> stream = Files.walk(path)) {
                stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
    }
}
