package com.rnk.thetync.engines;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SandboxingEngineTest {

    private static final Path FIXTURES = fixturesPath();

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private static Path fixturesPath() {
        try {
            return Path.of(SandboxFixtures.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /** An engine with no bubblewrap, so it can only use process isolation. */
    private SandboxingEngine processOnlyEngine() {
        SandboxingEngine engine = new SandboxingEngine(registry, Optional.empty(), Duration.ofSeconds(5));
        engine.initialize().block();
        return engine;
    }

    /** An engine configured from this host, OS isolation included when it works here. */
    private SandboxingEngine hostEngine() {
        SandboxingEngine engine = new SandboxingEngine(registry);
        engine.initialize().block();
        return engine;
    }

    private static EngineContext program(Class<?> fixture, String... args) {
        EngineContext context = new EngineContext();
        context.put("classpath", List.of(FIXTURES.toString()));
        context.put("mainClass", fixture.getName());
        context.put("args", List.of((Object[]) args));
        return context;
    }

    private static EngineResult run(SandboxingEngine engine, EngineContext context) {
        EngineResult result = engine.execute(context).block();
        assertNotNull(result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(EngineResult result) {
        return (Map<String, Object>) result.data();
    }

    // --- lifecycle ---

    @Test
    void lifecycleReportsNameVersionAndHealth() {
        SandboxingEngine engine = new SandboxingEngine(registry, Optional.empty(), Duration.ofSeconds(5));
        assertEquals("SandboxingEngine", engine.getName());
        assertEquals("2.0.0", engine.getVersion());
        assertEquals(EngineHealth.UNKNOWN, engine.getHealth());
        assertNotNull(engine.getMetrics());

        engine.initialize().block();
        assertEquals(EngineHealth.HEALTHY, engine.getHealth());
        assertFalse(engine.isOsIsolationAvailable());

        engine.shutdown().block();
        assertEquals(EngineHealth.UNKNOWN, engine.getHealth());
    }

    @Test
    void executeBeforeInitializeFails() {
        SandboxingEngine engine = new SandboxingEngine(registry, Optional.empty(), Duration.ofSeconds(5));
        assertThrows(IllegalStateException.class, () -> engine.execute(program(SandboxFixtures.Echo.class)).block());
    }

    // --- process isolation ---

    @Test
    void runsProgramInSeparateJvm() {
        SandboxingEngine engine = processOnlyEngine();
        EngineResult result = run(engine, program(SandboxFixtures.Echo.class, "a", "b"));

        assertTrue(result.success());
        assertNull(result.errorMessage());
        Map<String, Object> data = data(result);
        assertEquals("hello a,b", data.get("stdout"));
        assertEquals("", data.get("stderr"));
        assertEquals(0, data.get("exitCode"));
        assertEquals(false, data.get("timedOut"));
        assertEquals(false, data.get("outputTruncated"));
        assertEquals("process", data.get("isolation"));
        assertEquals(1, engine.getMetrics().getSuccessfulExecutions());
    }

    @Test
    void nonZeroExitIsFailureWithData() {
        SandboxingEngine engine = processOnlyEngine();
        EngineResult result = run(engine, program(SandboxFixtures.Exit.class, "3"));

        assertFalse(result.success());
        assertEquals("Sandboxed program exited with code 3", result.errorMessage());
        assertEquals(3, data(result).get("exitCode"));
        assertEquals(1, engine.getMetrics().getFailedExecutions());
    }

    @Test
    void timeLimitKillsProgram() {
        EngineContext context = program(SandboxFixtures.Sleep.class);
        context.setConfig("timeoutMs", 500);
        long start = System.nanoTime();

        EngineResult result = run(processOnlyEngine(), context);

        assertFalse(result.success());
        assertEquals(true, data(result).get("timedOut"));
        assertEquals("Sandboxed program exceeded the 500 ms time limit", result.errorMessage());
        assertTrue(Duration.ofNanos(System.nanoTime() - start).toSeconds() < 30);
    }

    @Test
    void outputIsCappedWithoutFailingTheRun() {
        EngineContext context = program(SandboxFixtures.Chatty.class, "5000", "5000");
        context.setConfig("maxOutputBytes", 100);

        EngineResult result = run(processOnlyEngine(), context);

        assertTrue(result.success());
        assertEquals(100, ((String) data(result).get("stdout")).length());
        assertEquals(100, ((String) data(result).get("stderr")).length());
        assertEquals(true, data(result).get("outputTruncated"));
    }

    @Test
    void stderrOverflowAloneMarksOutputTruncated() {
        EngineContext context = program(SandboxFixtures.Chatty.class, "10", "5000");
        context.setConfig("maxOutputBytes", 100);

        EngineResult result = run(processOnlyEngine(), context);

        assertEquals("x".repeat(10), data(result).get("stdout"));
        assertEquals(100, ((String) data(result).get("stderr")).length());
        assertEquals(true, data(result).get("outputTruncated"));
    }

    @Test
    void environmentIsEmptyAndWorkingDirectoryIsThrownAway() {
        EngineResult result = run(processOnlyEngine(), program(SandboxFixtures.Environment.class));

        assertTrue(result.success());
        String out = (String) data(result).get("stdout");
        assertTrue(out.startsWith("env=0 "), out);
        String cwd = out.substring(out.indexOf("cwd=") + 4);
        assertTrue(Path.of(cwd).getFileName().toString().startsWith("tync-sandbox-"), cwd);
        assertTrue(out.contains("home=" + cwd + " tmp=" + cwd + " "), out);
        assertFalse(Files.exists(Path.of(cwd)), "working directory should be deleted after the run");
    }

    @Test
    void heapCapStopsRunawayAllocation() {
        EngineContext context = program(SandboxFixtures.Hog.class);
        context.setConfig("maxHeapMb", 16);

        EngineResult result = run(processOnlyEngine(), context);

        assertFalse(result.success());
        assertEquals(3, data(result).get("exitCode"), "ExitOnOutOfMemoryError exits with code 3");
    }

    @Test
    void classpathMayBeAPathString() {
        EngineContext context = program(SandboxFixtures.Echo.class);
        String sep = java.io.File.pathSeparator;
        context.put("classpath", sep + FIXTURES + sep);

        assertTrue(run(processOnlyEngine(), context).success());
    }

    @Test
    void processIsolationCanBeRequestedExplicitly() {
        EngineContext context = program(SandboxFixtures.Echo.class);
        context.setConfig("isolation", "PROCESS");

        assertEquals("process", data(run(hostEngine(), context)).get("isolation"));
    }

    @Test
    void requiredOsIsolationFailsClosedWithoutBubblewrap() {
        EngineContext context = program(SandboxFixtures.Echo.class);
        context.setConfig("isolation", "os");

        assertThrows(IllegalStateException.class, () -> processOnlyEngine().execute(context).block());
    }

    // --- request validation ---

    @Test
    void rejectsInvalidRequests() {
        SandboxingEngine engine = processOnlyEngine();
        List<EngineContext> invalid = List.of(
                without("classpath"),
                withData("classpath", 42),
                withData("classpath", List.of()),
                withData("classpath", List.of(FIXTURES.resolve("missing.jar").toString())),
                without("mainClass"),
                withData("mainClass", "  "),
                withData("args", "not-a-list"),
                withConfig("isolation", "container"),
                withConfig("timeoutMs", 0),
                withConfig("maxHeapMb", "lots"));

        for (EngineContext context : invalid) {
            assertThrows(IllegalArgumentException.class, () -> engine.execute(context).block());
        }
    }

    /** The Echo program with one data key left out (EngineContext does not store nulls). */
    private static EngineContext without(String key) {
        EngineContext full = program(SandboxFixtures.Echo.class);
        EngineContext context = new EngineContext();
        full.getAllData().forEach((k, v) -> {
            if (!k.equals(key)) context.put(k, v);
        });
        return context;
    }

    private static EngineContext withData(String key, Object value) {
        EngineContext context = program(SandboxFixtures.Echo.class);
        context.put(key, value);
        return context;
    }

    private static EngineContext withConfig(String key, Object value) {
        EngineContext context = program(SandboxFixtures.Echo.class);
        context.setConfig(key, value);
        return context;
    }

    @Test
    void argsAreOptional() {
        EngineContext context = without("args");

        assertEquals("hello ", data(run(processOnlyEngine(), context)).get("stdout"));
    }

    // --- OS isolation (Linux with working bubblewrap) ---

    private SandboxingEngine osEngine() {
        SandboxingEngine engine = hostEngine();
        assumeTrue(engine.isOsIsolationAvailable(), "bubblewrap OS isolation is not available on this host");
        return engine;
    }

    private static EngineContext osProgram(Class<?> fixture, String... args) {
        EngineContext context = program(fixture, args);
        context.setConfig("isolation", "os");
        return context;
    }

    @Test
    void osIsolationRunsProgram() {
        EngineResult result = run(osEngine(), osProgram(SandboxFixtures.Echo.class, "os"));

        assertTrue(result.success(), String.valueOf(data(result).get("stderr")));
        assertEquals("hello os", data(result).get("stdout"));
        assertEquals("os", data(result).get("isolation"));
    }

    @Test
    void osIsolationHidesHostFiles(@TempDir Path hostDir) throws IOException {
        SandboxingEngine engine = osEngine();
        Path secret = Files.writeString(hostDir.resolve("secret.txt"), "host-only");

        assertEquals("visible", data(run(processOnlyEngine(),
                program(SandboxFixtures.CanSee.class, secret.toString()))).get("stdout"));
        assertEquals("hidden", data(run(engine,
                osProgram(SandboxFixtures.CanSee.class, secret.toString()))).get("stdout"));
    }

    @Test
    void osIsolationBlocksNetwork() {
        assertEquals("blocked", data(run(osEngine(), osProgram(SandboxFixtures.Network.class))).get("stdout"));
    }

    @Test
    void osIsolationUsesPrivateScratchDirectory() {
        String out = (String) data(run(osEngine(), osProgram(SandboxFixtures.Environment.class))).get("stdout");

        // bubblewrap sets PWD when it changes into the scratch directory; nothing else is passed.
        assertEquals("env=1 home=/sandbox tmp=/sandbox cwd=/sandbox", out);
    }

    @Test
    void osIsolationCapsDiskUse() {
        EngineContext context = osProgram(SandboxFixtures.FillDisk.class);
        context.setConfig("maxDiskMb", 4);

        String out = (String) data(run(osEngine(), context)).get("stdout");

        assertTrue(out.startsWith("full after "), out);
        assertTrue(Integer.parseInt(out.replaceAll("\\D+", "")) <= 4, out);
    }

    @Test
    void osIsolationTimeLimitKillsProgram() {
        EngineContext context = osProgram(SandboxFixtures.Sleep.class);
        context.setConfig("timeoutMs", 500);

        assertEquals(true, data(run(osEngine(), context)).get("timedOut"));
    }

    // --- bubblewrap probe ---

    @Test
    void probeRejectsMissingBubblewrap() {
        SandboxingEngine engine = new SandboxingEngine(
                registry, Optional.of(Path.of("/nonexistent/bwrap")), Duration.ofSeconds(5));
        engine.initialize().block();

        assertFalse(engine.isOsIsolationAvailable());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void probeRejectsFailingBubblewrap(@TempDir Path dir) throws IOException {
        SandboxingEngine engine = new SandboxingEngine(
                registry, Optional.of(script(dir, "exit 1")), Duration.ofSeconds(5));
        engine.initialize().block();

        assertFalse(engine.isOsIsolationAvailable());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void probeGivesUpOnHangingBubblewrap(@TempDir Path dir) throws IOException {
        SandboxingEngine engine = new SandboxingEngine(
                registry, Optional.of(script(dir, "exec sleep 30")), Duration.ofMillis(200));
        engine.initialize().block();

        assertFalse(engine.isOsIsolationAvailable());
    }

    private static Path script(Path dir, String body) throws IOException {
        Path script = Files.writeString(dir.resolve("fake-bwrap"), "#!/bin/sh\n" + body + "\n");
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwx------"));
        return script;
    }

    // --- helpers ---

    @Test
    void osIsolationPrefixBindsJdkAndClasspathReadOnly() throws IOException {
        SandboxingEngine engine = new SandboxingEngine(registry, Optional.empty(), Duration.ofSeconds(5));
        Path jar = FIXTURES.resolve("lib.jar");

        List<String> prefix = engine.osIsolationPrefix(Path.of("/usr/bin/bwrap"), List.of(jar), 8);

        assertEquals("/usr/bin/bwrap", prefix.get(0));
        assertTrue(prefix.contains("--unshare-all"));
        String javaHome = System.getProperty("java.home");
        assertTrue(containsSequence(prefix, "--ro-bind", javaHome, javaHome));
        assertTrue(containsSequence(prefix, "--ro-bind", jar.toString(), jar.toString()));
        assertTrue(containsSequence(prefix, "--size", Long.toString(8L * 1024 * 1024), "--tmpfs", "/sandbox"));
        assertTrue(containsSequence(prefix, "--chdir", "/sandbox"));
    }

    private static boolean containsSequence(List<String> list, String... sequence) {
        return java.util.Collections.indexOfSubList(list, List.of(sequence)) >= 0;
    }

    @Test
    void systemMountsRecreateSymlinksAndBindDirectories(@TempDir Path root) throws IOException {
        Path realDir = Files.createDirectory(root.resolve("lib"));
        Path link = Files.createSymbolicLink(root.resolve("bin"), Path.of("usr/bin"));
        Path missing = root.resolve("lib32");

        List<String> mounts = SandboxingEngine.systemMounts(List.of(link, realDir, missing));

        assertEquals(List.of(
                "--symlink", "usr/bin", link.toString(),
                "--ro-bind", realDir.toString(), realDir.toString()), mounts);
    }

    @Test
    void findExecutableSearchesLinuxPathOnly(@TempDir Path dir) throws IOException {
        Path bwrap = script(dir, "true");
        String sep = java.io.File.pathSeparator;
        String path = sep + "/nonexistent" + sep + dir;

        assertEquals(Optional.empty(), SandboxingEngine.findExecutable("fake-bwrap", null, path));
        assertEquals(Optional.empty(), SandboxingEngine.findExecutable("fake-bwrap", "Mac OS X", path));
        assertEquals(Optional.empty(), SandboxingEngine.findExecutable("fake-bwrap", "Linux", null));
        assertEquals(Optional.empty(), SandboxingEngine.findExecutable("missing-tool", "Linux", path));
        assertEquals(Optional.of(bwrap), SandboxingEngine.findExecutable("fake-bwrap", "Linux", path));
    }

    @Test
    void javaExecutableNameFollowsPlatform() {
        assertEquals("java.exe", SandboxingEngine.javaExecutableName("Windows 11"));
        assertEquals("java", SandboxingEngine.javaExecutableName("Linux"));
    }

    @Test
    void boundedCaptureKeepsWhatFitsAndSurvivesStreamErrors() throws InterruptedException {
        SandboxingEngine.BoundedCapture exact = SandboxingEngine.BoundedCapture.start(
                new ByteArrayInputStream("12345".getBytes(StandardCharsets.UTF_8)), 5);
        assertEquals("12345", exact.text());
        assertFalse(exact.truncated());

        InputStream failing = new InputStream() {
            private int served;

            @Override
            public int read() throws IOException {
                if (served++ < 3) return 'a';
                throw new IOException("pipe closed");
            }
        };
        SandboxingEngine.BoundedCapture broken = SandboxingEngine.BoundedCapture.start(failing, 100);
        assertEquals("aaa", broken.text());
        assertTrue(broken.truncated());
    }
}
