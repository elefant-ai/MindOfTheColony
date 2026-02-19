package com.goodbird.buildtools;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

public class MixinGeneratorPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        TaskProvider<MixinGeneratorTask> genTask = project.getTasks().register(
            "generateForkMixins", MixinGeneratorTask.class, task -> {
                task.setGroup("mindofthecolony");
                task.setDescription("Generate mixin classes from fork source diffs");

                task.getReferenceDir().set(
                    project.file("minecolonies-reference/src/main/java/com/minecolonies"));
                task.getForkDir().set(
                    project.file("src/main/java/com/goodbird/mindofthecolony/mc"));
                task.getOutputDir().set(
                    project.file("src/generated/mixins/java"));
                task.getMixinPackage().set(
                    "com.goodbird.mindofthecolony.mixin.generated");
                task.getMixinClassListOutput().set(
                    project.file("src/generated/mixins/generated-mixins-list.txt"));
            }
        );
    }
}
