package io.github.jvmspec.generation;

import io.github.jvmspec.model.DescribedClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class ClassSkeletonGeneratorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void rendersPackagedClassSkeleton() {
        String source = ClassSkeletonGenerator.render(DescribedClass.of("com.example.Generated"));

        assertEquals("package com.example;\n\npublic class Generated { }\n", source);
    }

    @Test
    public void rendersDefaultPackageClassSkeleton() {
        String source = ClassSkeletonGenerator.render(DescribedClass.of("Generated"));

        assertEquals("public class Generated { }\n", source);
    }

    @Test
    public void createsPlanWithTargetPathAndSkeletonContent() throws Exception {
        File sourceRoot = temporaryFolder.newFolder("source-root");
        DescribedClass describedClass = DescribedClass.of("com.example.Generated");

        ClassGenerationPlan plan = ClassSkeletonGenerator.plan(describedClass, sourceRoot);

        assertEquals(describedClass, plan.describedClass());
        assertEquals(sourceRoot, plan.sourceRoot());
        assertEquals(new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Generated.java"), plan.targetFile());
        assertEquals("package com.example;\n\npublic class Generated { }\n", plan.sourceContent());
    }
}
