package com.rnk.thetync.engines;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Sandboxing Engine - runs untrusted Java code in a separate, locked-down JVM.
 *
 * <p>The Security Manager no longer exists in modern Java, so isolation comes from
 * the process boundary and, where available, the operating system:</p>
 * <ul>
 *   <li><b>Process isolation (always):</b> a separate JVM that shares no heap or
 *       classes with the Tync, a heap cap, a wall-clock limit that kills the whole
 *       process tree, an output cap, an empty environment, closed stdin and a
 *       throwaway working directory.</li>
 *   <li><b>OS isolation (Linux with bubblewrap):</b> additionally no network, private
 *       PID/IPC/UTS namespaces, and a filesystem view limited to the system libraries,
 *       the JDK and the classpath (all read-only) plus a size-capped scratch
 *       directory. bubblewrap itself sets {@code PWD} to that directory; nothing
 *       else reaches the environment.</li>
 * </ul>
 * <p>Not covered: CPU-core and thread-count limits.</p>
 *
 * <p>Context data: {@code classpath} (a path string or a list of paths, required),
 * {@code mainClass} (required) and {@code args} (a list, optional). Context config:
 * {@code isolation} ({@code auto}, {@code os} or {@code process}; {@code auto} uses OS
 * isolation when available), {@code timeoutMs}, {@code maxHeapMb},
 * {@code maxOutputBytes} and {@code maxDiskMb} (OS isolation only).</p>
 *
 * <p>The result data reports {@code exitCode}, {@code timedOut}, {@code stdout},
 * {@code stderr}, {@code outputTruncated} and the {@code isolation} actually used.
 * The result is a success only when the program exits with code 0 in time.</p>
 */
public class SandboxingEngine implements Engine {

    private static final Logger logger = LoggerFactory.getLogger(SandboxingEngine.class);

    static final long DEFAULT_TIMEOUT_MS = 10_000;
    static final long DEFAULT_MAX_HEAP_MB = 256;
    static final long DEFAULT_MAX_OUTPUT_BYTES = 1 << 20;
    static final long DEFAULT_MAX_DISK_MB = 64;
    static final Duration DEFAULT_PROBE_TIMEOUT = Duration.ofSeconds(15);

    /** Scratch directory inside the OS sandbox (a size-capped tmpfs). */
    static final String SANDBOX_WORKDIR = "/sandbox";

    /** Top-level system directories made visible, read-only, inside the OS sandbox. */
    static final List<Path> SYSTEM_DIRS = List.of(
            Path.of("/bin"), Path.of("/sbin"), Path.of("/lib"), Path.of("/lib32"), Path.of("/lib64"));

    private final String name = "SandboxingEngine";
    private final String version = "2.0.0";
    private final EngineMetrics metrics = new EngineMetrics();
    private final MeterRegistry meterRegistry;
    private final Optional<Path> bubblewrap;
    private final Duration probeTimeout;
    private final String javaHome = System.getProperty("java.home");
    private final Path javaExecutable =
            Path.of(javaHome, "bin", javaExecutableName(System.getProperty("os.name")));

    private volatile EngineHealth health = EngineHealth.UNKNOWN;
    private volatile boolean initialized = false;
    private volatile boolean osIsolationAvailable = false;

    public SandboxingEngine(MeterRegistry meterRegistry) {
        this(meterRegistry,
                findExecutable("bwrap", System.getProperty("os.name"), System.getenv("PATH")),
                DEFAULT_PROBE_TIMEOUT);
    }

    SandboxingEngine(MeterRegistry meterRegistry, Optional<Path> bubblewrap, Duration probeTimeout) {
        this.meterRegistry = meterRegistry;
        this.bubblewrap = bubblewrap;
        this.probeTimeout = probeTimeout;
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getVersion() { return version; }

    /** Whether OS-level isolation passed its startup probe. Valid after {@link #initialize()}. */
    public boolean isOsIsolationAvailable() { return osIsolationAvailable; }

    @Override
    public Mono<Void> initialize() {
        return Mono.fromCallable(() -> {
            osIsolationAvailable = bubblewrap.isPresent() && probe(bubblewrap.get());
            logger.info("Initializing Sandboxing Engine ({} isolation)",
                    osIsolationAvailable ? "OS + process" : "process");
            health = EngineHealth.HEALTHY;
            initialized = true;
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<EngineResult> execute(EngineContext context) {
        if (!initialized) return Mono.error(new IllegalStateException("Engine not initialized"));

        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();

        return Mono.fromCallable(() -> {
            Request request = Request.from(context);
            boolean useOs = useOsIsolation(request.isolation());
            Map<String, Object> data = run(request, useOs);

            boolean timedOut = (Boolean) data.get("timedOut");
            int exitCode = (Integer) data.get("exitCode");
            boolean ok = !timedOut && exitCode == 0;
            String error = timedOut
                    ? "Sandboxed program exceeded the " + request.timeoutMs() + " ms time limit"
                    : "Sandboxed program exited with code " + exitCode;

            long executionTime = System.currentTimeMillis() - startTime;
            metrics.recordExecution(ok, executionTime);
            sample.stop(Timer.builder("engine.execution").tag("engine", name).register(meterRegistry));

            return new EngineResult(ok, data, ok ? null : error, executionTime);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.fromCallable(() -> {
            health = EngineHealth.UNKNOWN;
            initialized = false;
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public EngineHealth getHealth() { return health; }

    @Override
    public EngineMetrics getMetrics() { return metrics; }

    private boolean useOsIsolation(String isolation) {
        if (isolation.equals("process")) return false;
        if (isolation.equals("os") && !osIsolationAvailable) {
            throw new IllegalStateException("OS isolation was required but bubblewrap is not available");
        }
        return osIsolationAvailable;
    }

    private Map<String, Object> run(Request request, boolean useOs) throws IOException, InterruptedException {
        Path hostWorkDir = useOs ? null : Files.createTempDirectory("tync-sandbox-");
        try {
            String workDir = useOs ? SANDBOX_WORKDIR : hostWorkDir.toString();
            List<String> command = new ArrayList<>();
            if (useOs) {
                command.addAll(osIsolationPrefix(bubblewrap.get(), request.classpath(), request.maxDiskMb()));
            }
            command.addAll(javaCommand(request, workDir));

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().clear();
            if (!useOs) builder.directory(hostWorkDir.toFile());

            Process process = builder.start();
            process.getOutputStream().close();
            BoundedCapture stdout = BoundedCapture.start(process.getInputStream(), request.maxOutputBytes());
            BoundedCapture stderr = BoundedCapture.start(process.getErrorStream(), request.maxOutputBytes());

            boolean finished = process.waitFor(request.timeoutMs(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor();
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("exitCode", process.exitValue());
            data.put("timedOut", !finished);
            data.put("stdout", stdout.text());
            data.put("stderr", stderr.text());
            data.put("outputTruncated", stdout.truncated() || stderr.truncated());
            data.put("isolation", useOs ? "os" : "process");
            return data;
        } finally {
            if (hostWorkDir != null) deleteRecursively(hostWorkDir);
        }
    }

    private List<String> javaCommand(Request request, String workDir) {
        List<String> command = new ArrayList<>(List.of(
                javaExecutable.toString(),
                "-Xmx" + request.maxHeapMb() + "m",
                "-XX:+ExitOnOutOfMemoryError",
                "-XX:+UseSerialGC",
                "-XX:TieredStopAtLevel=1",
                "-Djava.io.tmpdir=" + workDir,
                "-Duser.home=" + workDir,
                "-cp", String.join(File.pathSeparator, request.classpath().stream().map(Path::toString).toList()),
                request.mainClass()));
        command.addAll(request.args());
        return command;
    }

    /** bubblewrap invocation that confines the child to the read-only system, JDK and classpath. */
    List<String> osIsolationPrefix(Path bwrap, List<Path> classpath, long maxDiskMb) throws IOException {
        List<String> command = new ArrayList<>(List.of(
                bwrap.toString(), "--unshare-all", "--die-with-parent", "--new-session",
                "--ro-bind", "/usr", "/usr"));
        command.addAll(systemMounts(SYSTEM_DIRS));
        command.addAll(List.of("--ro-bind-try", "/etc/ld.so.cache", "/etc/ld.so.cache"));
        command.addAll(List.of("--ro-bind", javaHome, javaHome));
        for (Path entry : classpath) {
            command.addAll(List.of("--ro-bind", entry.toString(), entry.toString()));
        }
        command.addAll(List.of(
                "--proc", "/proc", "--dev", "/dev", "--tmpfs", "/tmp",
                "--size", Long.toString(maxDiskMb * 1024 * 1024), "--tmpfs", SANDBOX_WORKDIR,
                "--chdir", SANDBOX_WORKDIR));
        return command;
    }

    /** Mount arguments for top-level system directories: symlinks (merged /usr) are recreated, real directories bound read-only. */
    static List<String> systemMounts(List<Path> dirs) throws IOException {
        List<String> mounts = new ArrayList<>();
        for (Path dir : dirs) {
            if (Files.isSymbolicLink(dir)) {
                mounts.addAll(List.of("--symlink", Files.readSymbolicLink(dir).toString(), dir.toString()));
            } else if (Files.isDirectory(dir)) {
                mounts.addAll(List.of("--ro-bind", dir.toString(), dir.toString()));
            }
        }
        return mounts;
    }

    /** Starts a trivial JVM under bubblewrap to confirm OS isolation works on this host. */
    private boolean probe(Path bwrap) throws InterruptedException {
        try {
            List<String> command = osIsolationPrefix(bwrap, List.of(), 1);
            command.addAll(List.of(javaExecutable.toString(), "-version"));
            ProcessBuilder builder = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD);
            builder.environment().clear();
            Process process = builder.start();
            if (process.waitFor(probeTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                return process.exitValue() == 0;
            }
            process.destroyForcibly();
            return false;
        } catch (IOException e) {
            logger.warn("bubblewrap probe failed, falling back to process isolation: {}", e.getMessage());
            return false;
        }
    }

    /** Locates an executable on a Linux PATH; OS isolation is Linux-only. */
    static Optional<Path> findExecutable(String name, String osName, String pathEnv) {
        if (osName == null || !osName.toLowerCase(Locale.ROOT).contains("linux") || pathEnv == null) {
            return Optional.empty();
        }
        return Arrays.stream(pathEnv.split(File.pathSeparator))
                .filter(dir -> !dir.isEmpty())
                .map(dir -> Path.of(dir, name))
                .filter(Files::isExecutable)
                .findFirst();
    }

    static String javaExecutableName(String osName) {
        return osName.toLowerCase(Locale.ROOT).startsWith("windows") ? "java.exe" : "java";
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    /** Validated execution request read from an {@link EngineContext}. */
    record Request(List<Path> classpath, String mainClass, List<String> args, String isolation,
                   long timeoutMs, long maxHeapMb, long maxOutputBytes, long maxDiskMb) {

        static Request from(EngineContext context) {
            return new Request(
                    classpath(context.get("classpath")),
                    mainClass(context.get("mainClass")),
                    args(context.get("args")),
                    isolation(context.getConfig("isolation")),
                    positive(context, "timeoutMs", DEFAULT_TIMEOUT_MS),
                    positive(context, "maxHeapMb", DEFAULT_MAX_HEAP_MB),
                    positive(context, "maxOutputBytes", DEFAULT_MAX_OUTPUT_BYTES),
                    positive(context, "maxDiskMb", DEFAULT_MAX_DISK_MB));
        }

        private static List<Path> classpath(Object value) {
            Stream<String> entries;
            if (value instanceof String text) {
                entries = Arrays.stream(text.split(File.pathSeparator));
            } else if (value instanceof List<?> list) {
                entries = list.stream().map(String::valueOf);
            } else {
                throw new IllegalArgumentException("Missing classpath to execute");
            }
            List<Path> paths = entries.filter(e -> !e.isBlank())
                    .map(e -> Path.of(e).toAbsolutePath().normalize())
                    .toList();
            if (paths.isEmpty()) throw new IllegalArgumentException("Missing classpath to execute");
            for (Path path : paths) {
                if (!Files.exists(path)) throw new IllegalArgumentException("Classpath entry does not exist: " + path);
            }
            return paths;
        }

        private static String mainClass(Object value) {
            if (value instanceof String text && !text.isBlank()) return text;
            throw new IllegalArgumentException("Missing mainClass to execute");
        }

        private static List<String> args(Object value) {
            if (value == null) return List.of();
            if (value instanceof List<?> list) return list.stream().map(String::valueOf).toList();
            throw new IllegalArgumentException("args must be a list");
        }

        private static String isolation(Object value) {
            String isolation = value == null ? "auto" : String.valueOf(value).toLowerCase(Locale.ROOT);
            if (List.of("auto", "os", "process").contains(isolation)) return isolation;
            throw new IllegalArgumentException("isolation must be auto, os or process: " + value);
        }

        private static long positive(EngineContext context, String key, long defaultValue) {
            Object value = context.getConfig(key);
            if (value == null) return defaultValue;
            if (value instanceof Number number && number.longValue() > 0) return number.longValue();
            throw new IllegalArgumentException(key + " must be a positive number: " + value);
        }
    }

    /** Drains a child stream on a virtual thread, keeping at most {@code limit} bytes. */
    static final class BoundedCapture {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final long limit;
        private volatile boolean truncated;
        private Thread thread;

        private BoundedCapture(long limit) {
            this.limit = limit;
        }

        static BoundedCapture start(InputStream in, long limit) {
            BoundedCapture capture = new BoundedCapture(limit);
            capture.thread = Thread.ofVirtual().start(() -> capture.drain(in));
            return capture;
        }

        private void drain(InputStream in) {
            byte[] chunk = new byte[8192];
            try (in) {
                int read;
                while ((read = in.read(chunk)) != -1) {
                    int room = (int) Math.min(read, limit - buffer.size());
                    buffer.write(chunk, 0, room);
                    if (room < read) truncated = true;
                }
            } catch (IOException e) {
                // The stream closes when a timed-out process is killed; keep what was captured.
                truncated = true;
            }
        }

        String text() throws InterruptedException {
            thread.join();
            return buffer.toString(StandardCharsets.UTF_8);
        }

        boolean truncated() throws InterruptedException {
            thread.join();
            return truncated;
        }
    }
}
