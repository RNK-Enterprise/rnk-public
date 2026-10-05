package com.rnk.thetync.engines;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Programs that {@link SandboxingEngineTest} runs inside the sandbox. Each one
 * probes a single guarantee and reports what it saw on stdout.
 */
final class SandboxFixtures {

    private SandboxFixtures() {
    }

    /** Prints its arguments. */
    static final class Echo {
        public static void main(String[] args) {
            System.out.print("hello " + String.join(",", args));
        }
    }

    /** Exits with the code given as its first argument. */
    static final class Exit {
        public static void main(String[] args) {
            System.exit(Integer.parseInt(args[0]));
        }
    }

    /** Never finishes on its own. */
    static final class Sleep {
        public static void main(String[] args) throws InterruptedException {
            Thread.sleep(120_000);
        }
    }

    /** Writes {@code args[0]} bytes to stdout and {@code args[1]} bytes to stderr. */
    static final class Chatty {
        public static void main(String[] args) {
            byte[] out = "x".repeat(Integer.parseInt(args[0])).getBytes();
            System.out.write(out, 0, out.length);
            System.out.flush();
            byte[] err = "x".repeat(Integer.parseInt(args[1])).getBytes();
            System.err.write(err, 0, err.length);
            System.err.flush();
        }
    }

    /** Reports its environment size, home, temp dir and working directory, and leaves a file behind. */
    static final class Environment {
        public static void main(String[] args) throws IOException {
            Path cwd = Path.of("").toAbsolutePath();
            Files.writeString(cwd.resolve("left-behind.txt"), "data");
            System.out.print("env=" + System.getenv().size()
                    + " home=" + System.getProperty("user.home")
                    + " tmp=" + System.getProperty("java.io.tmpdir")
                    + " cwd=" + cwd);
        }
    }

    /** Reports whether the host path in {@code args[0]} is visible. */
    static final class CanSee {
        public static void main(String[] args) {
            System.out.print(Files.exists(Path.of(args[0])) ? "visible" : "hidden");
        }
    }

    /** Reports whether an outbound TCP connection can be opened. */
    static final class Network {
        public static void main(String[] args) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("1.1.1.1", 53), 2000);
                System.out.print("connected");
            } catch (IOException e) {
                System.out.print("blocked");
            }
        }
    }

    /** Allocates until the heap runs out. */
    static final class Hog {
        public static void main(String[] args) {
            List<byte[]> hoard = new ArrayList<>();
            while (true) {
                hoard.add(new byte[1 << 20]);
            }
        }
    }

    /** Writes to its working directory until the disk is full and reports how many MiB fit. */
    static final class FillDisk {
        public static void main(String[] args) {
            byte[] mib = new byte[1 << 20];
            long written = 0;
            try (OutputStream out = Files.newOutputStream(Path.of("fill.bin"))) {
                while (written < 1024) {
                    out.write(mib);
                    written++;
                }
                System.out.print("unbounded");
            } catch (IOException e) {
                System.out.print("full after " + written + " MiB");
            }
        }
    }
}
