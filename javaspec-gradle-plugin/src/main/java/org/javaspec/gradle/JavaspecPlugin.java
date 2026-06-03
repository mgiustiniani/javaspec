package org.javaspec.gradle;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * Gradle plugin entry point for org.javaspec.
 */
public final class JavaspecPlugin implements Plugin<Project> {
    public static final String EXTENSION_NAME = "javaspec";
    public static final String TASK_NAME = "javaspecRun";

    public void apply(final Project project) {
        final JavaspecExtension extension = project.getExtensions().create(
                EXTENSION_NAME,
                JavaspecExtension.class,
                project
        );

        final JavaspecRunTask runTask = project.getTasks().create(TASK_NAME, JavaspecRunTask.class);
        runTask.setJavaspecExtension(extension);
        runTask.setGroup("verification");
        runTask.setDescription("Runs javaspec specifications with the canonical no-JUnit launcher.");

        project.getPlugins().withType(JavaPlugin.class, new Action<JavaPlugin>() {
            public void execute(JavaPlugin javaPlugin) {
                configureJavaDefaults(project, runTask);
            }
        });
    }

    private static void configureJavaDefaults(Project project, JavaspecRunTask runTask) {
        SourceSetContainer sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
        if (sourceSets == null) {
            return;
        }
        SourceSet testSourceSet = sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME);
        if (testSourceSet == null) {
            return;
        }
        runTask.setDefaultClasspath(testSourceSet.getRuntimeClasspath());
        runTask.dependsOn(testSourceSet.getClassesTaskName());
    }
}
