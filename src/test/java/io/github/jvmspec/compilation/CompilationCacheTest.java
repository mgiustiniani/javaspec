package io.github.jvmspec.compilation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class CompilationCacheTest {

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    // -------------------------------------------------------------------------
    // isUpToDate — negative cases

    @Test
    public void notUpToDateWhenCacheFileAbsent() throws Exception {
        File outputDir = tmp.newFolder("out");
        CompilationCache cache = new CompilationCache(outputDir);
        assertFalse(cache.isUpToDate(noFiles(), noFiles(), noOptions()));
    }

    @Test
    public void notUpToDateWhenSourceFileTimestampChanges() throws Exception {
        File outputDir = tmp.newFolder("out");
        File src = writeSource(tmp.newFolder("src"), "A.java", "class A {}");
        List<File> sources = files(src);
        List<File> cp = noFiles();
        List<String> opts = noOptions();

        CompilationCache cache = new CompilationCache(outputDir);
        cache.save(sources, cp, opts);
        assertTrue("should be up to date immediately after save",
                cache.isUpToDate(sources, cp, opts));

        // Simulate a change by touching the file.
        boolean set = src.setLastModified(src.lastModified() + 2000L);
        if (!set) {
            return; // skip if OS doesn't support setLastModified reliably
        }
        assertFalse("changed timestamp must invalidate cache",
                cache.isUpToDate(sources, cp, opts));
    }

    @Test
    public void notUpToDateWhenNewSourceFileAdded() throws Exception {
        File outputDir = tmp.newFolder("out");
        File srcDir = tmp.newFolder("src");
        File src1 = writeSource(srcDir, "A.java", "class A {}");
        CompilationCache cache = new CompilationCache(outputDir);
        cache.save(files(src1), noFiles(), noOptions());

        File src2 = writeSource(srcDir, "B.java", "class B {}");
        assertFalse("adding a source file must invalidate cache",
                cache.isUpToDate(files(src1, src2), noFiles(), noOptions()));
    }

    @Test
    public void notUpToDateWhenOptionChanges() throws Exception {
        File outputDir = tmp.newFolder("out");
        List<String> opts1 = options("-source", "8");
        List<String> opts2 = options("-source", "11");
        CompilationCache cache = new CompilationCache(outputDir);
        cache.save(noFiles(), noFiles(), opts1);
        assertFalse("changed options must invalidate cache",
                cache.isUpToDate(noFiles(), noFiles(), opts2));
    }

    @Test
    public void notUpToDateWhenClasspathChanges() throws Exception {
        File outputDir = tmp.newFolder("out");
        File jar1 = tmp.newFile("a.jar");
        File jar2 = tmp.newFile("b.jar");
        CompilationCache cache = new CompilationCache(outputDir);
        cache.save(noFiles(), files(jar1), noOptions());
        assertFalse("changed classpath must invalidate cache",
                cache.isUpToDate(noFiles(), files(jar2), noOptions()));
    }

    // -------------------------------------------------------------------------
    // isUpToDate — positive cases

    @Test
    public void upToDateAfterSaveWithNoSources() throws Exception {
        File outputDir = tmp.newFolder("out");
        CompilationCache cache = new CompilationCache(outputDir);
        cache.save(noFiles(), noFiles(), noOptions());
        assertTrue(cache.isUpToDate(noFiles(), noFiles(), noOptions()));
    }

    @Test
    public void upToDateAfterSaveWithSources() throws Exception {
        File outputDir = tmp.newFolder("out");
        File src = writeSource(tmp.newFolder("src"), "Hello.java", "class Hello {}");
        CompilationCache cache = new CompilationCache(outputDir);
        cache.save(files(src), noFiles(), noOptions());
        assertTrue(cache.isUpToDate(files(src), noFiles(), noOptions()));
    }

    @Test
    public void upToDateAfterSaveWithOptionsAndClasspath() throws Exception {
        File outputDir = tmp.newFolder("out");
        File jar = tmp.newFile("dep.jar");
        List<String> opts = options("--release", "11");
        CompilationCache cache = new CompilationCache(outputDir);
        cache.save(noFiles(), files(jar), opts);
        assertTrue(cache.isUpToDate(noFiles(), files(jar), opts));
    }

    // -------------------------------------------------------------------------
    // invalidate

    @Test
    public void invalidateRemovesCacheFile() throws Exception {
        File outputDir = tmp.newFolder("out");
        CompilationCache cache = new CompilationCache(outputDir);
        cache.save(noFiles(), noFiles(), noOptions());
        File cacheFile = new File(outputDir, CompilationCache.CACHE_FILE_NAME);
        assertTrue("cache file should exist after save", cacheFile.isFile());

        cache.invalidate();
        assertFalse("cache file should be gone after invalidate", cacheFile.isFile());
        assertFalse("must report not up-to-date after invalidate",
                cache.isUpToDate(noFiles(), noFiles(), noOptions()));
    }

    // -------------------------------------------------------------------------
    // Helpers

    private static File writeSource(File dir, String name, String content) throws IOException {
        File file = new File(dir, name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static List<File> noFiles() {
        return Collections.emptyList();
    }

    private static List<File> files(File... files) {
        return Arrays.asList(files);
    }

    private static List<String> noOptions() {
        return Collections.emptyList();
    }

    private static List<String> options(String... opts) {
        return Arrays.asList(opts);
    }
}
