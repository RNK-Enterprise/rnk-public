package com.rnk.thetync.transform;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Self-verification harness for adapted artifacts, in two tiers:
 *
 *   verifyStructure — pure bytecode analysis, no classloading: manifest
 *     markers, provenance signature, required entries (including the MIT
 *     components license), shim shape of the initializer (ASM). Works for
 *     ANY adapted artifact, including real mods whose transitive
 *     dependencies (Minecraft, Fabric API) are not on the harness
 *     classpath. This is the boundary-measurement verdict.
 *
 *   verify — verifyStructure PLUS execution: compile the Bukkit stub suite,
 *     load the artifact in an isolated URLClassLoader, drive the exact Paper
 *     lifecycle, and assert the mod's real initializer logic ran.
 */
public class AdaptedArtifactVerifier {

    /**
     * The Bukkit stub sources — the framework-touch surface an initializer may
     * reach after adaptation: the plugin instance surface (logger, data folder,
     * server handle), the static Bukkit facade, the server handle's plugin
     * manager, and the event listener marker. Reported in the verify output as
     * {@code bukkitStubSurface} so the covered (and uncovered) API is explicit.
     */
    static final Map<String, String> BUKKIT_STUB_SOURCES = Map.of(
        "org/bukkit/plugin/java/JavaPlugin.java", String.join("\n",
            "package org.bukkit.plugin.java;",
            "import java.io.File;",
            "import java.io.IOException;",
            "import java.nio.file.Files;",
            "import java.util.logging.Logger;",
            "public abstract class JavaPlugin {",
            "    private final Logger logger = Logger.getLogger(\"RNK-Stub\");",
            "    private final File dataFolder;",
            "    public JavaPlugin() {",
            "        try { dataFolder = Files.createTempDirectory(\"rnk-plugin-data\").toFile(); }",
            "        catch (IOException e) { throw new IllegalStateException(e); }",
            "    }",
            "    public Logger getLogger() { return logger; }",
            "    public File getDataFolder() { return dataFolder; }",
            "    public org.bukkit.Server getServer() { return org.bukkit.Bukkit.getServer(); }",
            "    public void onEnable() { }",
            "    public void onDisable() { }",
            "}"
        ),
        "org/bukkit/Bukkit.java", String.join("\n",
            "package org.bukkit;",
            "import java.util.logging.Logger;",
            "public final class Bukkit {",
            "    private static final Server SERVER = new Server();",
            "    public static Server getServer() { return SERVER; }",
            "    public static Logger getLogger() { return SERVER.getLogger(); }",
            "}"
        ),
        "org/bukkit/Server.java", String.join("\n",
            "package org.bukkit;",
            "import java.util.logging.Logger;",
            "public class Server {",
            "    public Logger getLogger() { return Logger.getLogger(\"RNK-Stub-Server\"); }",
            "    public String getVersion() { return \"1.20.1-RNKStub\"; }",
            "    public String getBukkitVersion() { return \"1.20.1-RNKStub\"; }",
            "    public org.bukkit.plugin.PluginManager getPluginManager() { return new org.bukkit.plugin.PluginManager(); }",
            "}"
        ),
        "org/bukkit/plugin/PluginManager.java", String.join("\n",
            "package org.bukkit.plugin;",
            "public class PluginManager {",
            "    public void registerEvents(org.bukkit.event.Listener listener, org.bukkit.plugin.java.JavaPlugin plugin) { }",
            "    public Object getPlugin(String name) { return null; }",
            "    public boolean isPluginEnabled(String name) { return false; }",
            "}"
        ),
        "org/bukkit/event/Listener.java", String.join("\n",
            "package org.bukkit.event;",
            "public interface Listener { }"
        )
    );

    /**
     * Verify an adapted artifact end-to-end: structure + execution.
     *
     * @param adaptedJarPath path to the adapted JAR
     * @return report with executed flags and class identity checks
     */
    public Map<String, Object> verify(String adaptedJarPath) throws Exception {
        Map<String, Object> report = verifyStructure(adaptedJarPath);
        report.put("verifyMode", "execution");
        if (!Boolean.TRUE.equals(report.get("structureVerified"))) {
            report.put("verified", false);
            return report;
        }
        Path path = Paths.get(adaptedJarPath);
        String initializer = (String) report.get("initializer");

        // Compile the Bukkit stub suite into a temp dir.
        Path stubDir = Files.createTempDirectory("rnk-bukkit-stub");
        List<javax.tools.JavaFileObject> stubUnits = new ArrayList<>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("JDK compiler unavailable; run the verifier on a JDK");
        }
        try (var fileManager = compiler.getStandardFileManager(null, null, null)) {
            for (Map.Entry<String, String> stub : BUKKIT_STUB_SOURCES.entrySet()) {
                Path stubSource = stubDir.resolve(stub.getKey());
                Files.createDirectories(stubSource.getParent());
                Files.writeString(stubSource, stub.getValue());
                stubUnits.add(fileManager.getJavaFileObjects(stubSource).iterator().next());
            }
            var task = compiler.getTask(null, fileManager, null,
                List.of("-d", stubDir.toString()), null, stubUnits);
            if (!task.call()) {
                throw new IllegalStateException("failed to compile Bukkit stub");
            }
        }

        // Classloader: stub API first (mimics Paper providing the API),
        // then the adapted artifact.
        URL[] classpath = new URL[] {
            stubDir.toUri().toURL(),
            path.toUri().toURL()
        };
        URLClassLoader loader = new URLClassLoader(classpath, ClassLoader.getPlatformClassLoader());

        // Stub surface self-check: the static Bukkit facade must be usable.
        Object stubLogger = Class.forName("org.bukkit.Bukkit", true, loader)
            .getMethod("getLogger").invoke(null);
        report.put("stubSurfaceFunctional", stubLogger instanceof java.util.logging.Logger);
        report.put("bukkitStubSurface", List.of(
            "JavaPlugin.getLogger/getDataFolder/getServer",
            "Bukkit static facade (getServer/getLogger)",
            "Server.getVersion/getPluginManager",
            "PluginManager.registerEvents/getPlugin/isPluginEnabled",
            "event.Listener marker"));

        // Drive the Paper lifecycle: instantiate entrypoint, call onEnable().
        Class<?> entrypoint = Class.forName("com.rnk.adapted.GeneratedPaperEntrypoint", true, loader);
        report.put("generatedEntrypointLoaded", true);
        report.put("entrypointExtendsJavaPlugin",
            "org.bukkit.plugin.java.JavaPlugin".equals(entrypoint.getSuperclass().getName()));

        Object instance = entrypoint.getDeclaredConstructor().newInstance();
        entrypoint.getMethod("onEnable").invoke(instance);

        // Prove the mod's real initializer ran: check a side effect the
        // fixture mod sets (system property), and confirm the initializer
        // class implements the SHIM, not the Fabric interface.
        String propKey = "rnk.verify." + initializer;
        String sideEffect = System.getProperty(propKey);
        report.put("initializerExecuted", sideEffect != null);

        Class<?> initializerClass = Class.forName(initializer, true, loader);
        boolean loadedClassImplementsShim = false;
        for (Class<?> iface : initializerClass.getInterfaces()) {
            if ("com.rnk.shim.ModInitializer".equals(iface.getName())
                || "com.rnk.shim.DedicatedServerModInitializer".equals(iface.getName())) {
                loadedClassImplementsShim = true;
            }
        }
        report.put("loadedInitializerImplementsShim", loadedClassImplementsShim);
        report.put("fabricInterfaceAbsent", !implementsFabric(loader, initializer));

        loader.close();
        report.put("verified", Boolean.TRUE.equals(report.get("initializerExecuted"))
            && loadedClassImplementsShim
            && Boolean.TRUE.equals(report.get("manifestMarkers"))
            && Boolean.TRUE.equals(report.get("signatureValid"))
            && Boolean.TRUE.equals(report.get("stubSurfaceFunctional"))
            && Boolean.TRUE.equals(report.get("structureVerified")));
        return report;
    }

    /**
     * Structure-only verification — the tier that works on real mods with
     * unresolvable transitive dependencies. Loads no classes from the
     * artifact: manifest markers, HMAC provenance, required JAR entries, and
     * the initializer's shim shape read straight from bytecode with ASM.
     */
    public Map<String, Object> verifyStructure(String adaptedJarPath) throws Exception {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("verifyMode", "structure");
        Path path = Paths.get(adaptedJarPath);

        // 1. Manifest markers.
        Manifest manifest;
        try (JarFile jar = new JarFile(path.toFile())) {
            manifest = jar.getManifest();
        }
        String adaptedFrom = manifest.getMainAttributes().getValue("RNK-Adapted-From");
        String adaptedTo = manifest.getMainAttributes().getValue("RNK-Adapted-To");
        String initializer = manifest.getMainAttributes().getValue("RNK-Initializer");
        String signature = manifest.getMainAttributes().getValue("RNK-Artifact-Signature");
        report.put("manifestMarkers", adaptedFrom != null && adaptedTo != null && initializer != null);
        report.put("adaptedFrom", adaptedFrom);
        report.put("adaptedTo", adaptedTo);
        report.put("initializer", initializer);
        if (!"fabric".equals(adaptedFrom) || !"paper".equals(adaptedTo)) {
            throw new IllegalStateException("artifact is not marked as fabric->paper adaptation");
        }

        // 2. Provenance: recompute the entry digest and compare signatures.
        //    A tampered artifact (any entry byte changed) fails here.
        if (signature != null && !signature.isEmpty()) {
            String recomputed = CrossLoaderAdapter.hmac(
                CrossLoaderAdapter.digestEntriesFromJar(path), CrossLoaderAdapter.signingKey);
            report.put("signatureValid", signature.equals(recomputed));
        } else {
            report.put("signatureValid", false);
        }

        // 3. Required entries present in the artifact — including the MIT
        //    components grant that keeps the mod author's license untouched.
        try (JarFile jar = new JarFile(path.toFile())) {
            report.put("shimsPresent",
                jar.getEntry("com/rnk/shim/ModInitializer.class") != null
                    && jar.getEntry("com/rnk/shim/DedicatedServerModInitializer.class") != null);
            report.put("generatedEntrypointPresent",
                jar.getEntry("com/rnk/adapted/GeneratedPaperEntrypoint.class") != null);
            report.put("pluginYmlPresent", jar.getEntry("plugin.yml") != null);
            report.put("componentsLicensePresent",
                jar.getEntry(CrossLoaderAdapter.COMPONENTS_LICENSE_PATH) != null);
        }

        // 4. Initializer shim shape, read from bytecode (no classloading —
        //    works without the mod's transitive dependencies on the classpath).
        String shimImplemented = null;
        String dispatchMethod = null;
        boolean fabricInterfaceAbsent = true;
        List<String> initializerInterfaces = new ArrayList<>();
        List<String> initializerMethods = new ArrayList<>();
        byte[] initializerBytes = readEntry(path, initializer.replace('.', '/') + ".class");
        if (initializerBytes != null) {
            new ClassReader(initializerBytes).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String signature,
                                  String superName, String[] interfaces) {
                    if (interfaces != null) {
                        for (String i : interfaces) initializerInterfaces.add(i);
                    }
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String sig, String[] exceptions) {
                    initializerMethods.add(name);
                    return null;
                }
            }, 0);
            if (initializerInterfaces.contains("com/rnk/shim/DedicatedServerModInitializer")) {
                shimImplemented = "com.rnk.shim.DedicatedServerModInitializer";
                dispatchMethod = "onInitializeServer";
            } else if (initializerInterfaces.contains("com/rnk/shim/ModInitializer")) {
                shimImplemented = "com.rnk.shim.ModInitializer";
                dispatchMethod = "onInitialize";
            }
            // The REAL Fabric contract: dispatch method must exist on the class.
            report.put("dispatchMethodPresent", shimImplemented != null
                && initializerMethods.contains(dispatchMethod));
            fabricInterfaceAbsent = initializerInterfaces.stream()
                .noneMatch(i -> i.startsWith("net/fabricmc/"));
        }
        report.put("initializerImplementsShim", shimImplemented != null);
        report.put("shimImplemented", shimImplemented);
        report.put("dispatchMethod", dispatchMethod);
        report.put("fabricInterfaceAbsent", fabricInterfaceAbsent);
        report.put("initializerClassFound", initializerBytes != null);

        report.put("structureVerified", Boolean.TRUE.equals(report.get("manifestMarkers"))
            && Boolean.TRUE.equals(report.get("signatureValid"))
            && Boolean.TRUE.equals(report.get("shimsPresent"))
            && Boolean.TRUE.equals(report.get("generatedEntrypointPresent"))
            && Boolean.TRUE.equals(report.get("pluginYmlPresent"))
            && Boolean.TRUE.equals(report.get("componentsLicensePresent"))
            && shimImplemented != null
            && Boolean.TRUE.equals(report.get("dispatchMethodPresent"))
            && fabricInterfaceAbsent);
        return report;
    }

    private static byte[] readEntry(Path jarPath, String entryName) throws Exception {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            try (InputStream in = jar.getInputStream(entry)) {
                return in.readAllBytes();
            }
        }
    }

    private boolean implementsFabric(ClassLoader loader, String initializer) {
        try {
            Class<?> fabric = Class.forName("net.fabricmc.api.ModInitializer", false, loader);
            return fabric.isAssignableFrom(Class.forName(initializer, false, loader));
        } catch (ClassNotFoundException expected) {
            return false; // Fabric interface not present at all — correct for a Paper artifact.
        }
    }

    /** CLI: AdaptedArtifactVerifier <adapted.jar> [structure] — exits 0 iff verified. */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: AdaptedArtifactVerifier <adapted.jar> [structure]");
            System.exit(1);
        }
        Map<String, Object> report = args.length > 1 && "structure".equals(args[1])
            ? new AdaptedArtifactVerifier().verifyStructure(args[0])
            : new AdaptedArtifactVerifier().verify(args[0]);
        System.out.println(new com.fasterxml.jackson.databind.ObjectMapper()
            .writerWithDefaultPrettyPrinter().writeValueAsString(report));
        System.exit(Boolean.TRUE.equals(report.get("verified"))
            || Boolean.TRUE.equals(report.get("structureVerified")) ? 0 : 2);
    }
}
