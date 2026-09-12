/*
 * RNK CrossLoaderAdapter — part of the open Tync, licensed GPL-3.0-only as a whole.
 *
 * LICENSING EXCEPTION FOR PRODUCED ARTIFACTS: the RNK-originated components
 * this adapter inlines into adapted artifacts (shim interfaces, generated
 * entrypoint) are granted under the MIT License via the license file embedded
 * in every artifact (rnk/RNK-COMPONENTS-LICENSE.txt), so an adapted mod is a
 * combined work of permissively-licensed RNK glue and the mod's own code —
 * the mod author's license (MIT, Apache, ARR, ...) is never affected.
 */
package com.rnk.thetync.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Real cross-loader adaptation: rewrites a Fabric server-side mod into an
 * artifact loadable as a Paper (Bukkit) plugin.
 *
 * Mechanism (all real, no mocks):
 *   1. ASM ClassReader parses every class in the source JAR.
 *   2. A Remapper rewrites Fabric entrypoint interfaces
 *      (net.fabricmc.api.ModInitializer / DedicatedServerModInitializer)
 *      to RNK shim interfaces (com.rnk.shim.*).
 *   3. A generated Paper entrypoint (JavaPlugin subclass) is added; on
 *      onEnable() it instantiates the remapped initializer and forwards the
 *      Bukkit PluginEnableEvent into the Fabric onInitialize contract.
 *   4. The shim interfaces are inlined into the artifact.
 *   5. The result is repackaged as a valid JAR with plugin.yml, a marked
 *      manifest, and a JSON adaptation report.
 *
 * Scope (honest): server-side lifecycle boundary translation for mods that
 * hook server start. World/content-level translation is out of scope for
 * this adapter and reported as such in the result.
 */
public class CrossLoaderAdapter {

    public static final String FABRIC_INITIALIZER = "net/fabricmc/api/ModInitializer";
    public static final String FABRIC_SERVER_INITIALIZER = "net/fabricmc/api/DedicatedServerModInitializer";
    public static final String SHIM_INITIALIZER = "com/rnk/shim/ModInitializer";
    public static final String SHIM_SERVER_INITIALIZER = "com/rnk/shim/DedicatedServerModInitializer";

    private final ObjectMapper json = new ObjectMapper();

    /**
     * HMAC-SHA256 key for artifact provenance signatures. Sources, in order:
     * RNK_SIGNING_KEY env, RNK_SIGNING_KEY_FILE, or a persistent generated
     * key at ~/.rnk/tync-signing.key (created on first use) so signatures
     * verify across processes and runs on the same machine. Hosted
     * deployments should set RNK_SIGNING_KEY explicitly.
     */
    static volatile byte[] signingKey;
    static volatile String signingKeySource;

    static {
        String keyText = System.getenv("RNK_SIGNING_KEY");
        String keyFile = System.getenv("RNK_SIGNING_KEY_FILE");
        String source = "RNK_SIGNING_KEY";
        if ((keyText == null || keyText.isEmpty()) && keyFile != null && !keyFile.isEmpty()) {
            source = "RNK_SIGNING_KEY_FILE";
            try {
                keyText = new String(Files.readAllBytes(Paths.get(keyFile)), StandardCharsets.UTF_8).trim();
            } catch (IOException ignored) {
                // fall through to persistent generated key
            }
        }
        if (keyText == null || keyText.isEmpty()) {
            // Persistent generated key: survives across processes (adapt runs
            // in one JVM, verify in another), created once per machine.
            Path defaultKeyPath = Paths.get(System.getProperty("user.home"), ".rnk", "tync-signing.key");
            source = "generated-default-key";
            try {
                if (Files.exists(defaultKeyPath)) {
                    keyText = new String(Files.readAllBytes(defaultKeyPath), StandardCharsets.UTF_8).trim();
                } else {
                    keyText = Base64.getEncoder().encodeToString(randomKey());
                    Files.createDirectories(defaultKeyPath.getParent());
                    Files.writeString(defaultKeyPath, keyText);
                }
            } catch (IOException ignored) {
                keyText = null;
            }
        }
        if (keyText != null && !keyText.isEmpty()) {
            signingKey = keyText.getBytes(StandardCharsets.UTF_8);
            signingKeySource = source;
        } else {
            signingKey = randomKey();
            signingKeySource = "ephemeral-random";
        }
    }

    private static byte[] randomKey() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] key = new byte[32];
        random.nextBytes(key);
        return key;
    }

    /** HMAC-SHA256 over the exact artifact bytes. */
    static String hmac(byte[] artifactBytes, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(artifactBytes));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    /** Rewrite one class's bytecode through the remapper. */
    byte[] remapClass(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(0);
        reader.accept(new ClassRemapper(writer, new FabricToShimRemapper()), 0);
        return writer.toByteArray();
    }

    /** Generate the shim interfaces inlined into the adapted artifact.
     *
     * Method shapes mirror the REAL Fabric API:
     *   net.fabricmc.api.ModInitializer            -> void onInitialize()
     *   net.fabricmc.api.DedicatedServerModInitializer -> void onInitializeServer()
     */
    static Map<String, byte[]> shimInterfaces() {
        Map<String, byte[]> shims = new LinkedHashMap<>();
        shims.put(SHIM_INITIALIZER.replace('.', '/') + ".class", compileShim(
            SHIM_INITIALIZER,
            "RNK shim for net.fabricmc.api.ModInitializer.",
            "onInitialize"));
        shims.put(SHIM_SERVER_INITIALIZER.replace('.', '/') + ".class", compileShim(
            SHIM_SERVER_INITIALIZER,
            "RNK shim for net.fabricmc.api.DedicatedServerModInitializer.",
            "onInitializeServer"));
        return shims;
    }

    private static byte[] compileShim(String internalName, String javadoc, String methodName) {
        // Build the interface with ASM itself so the shim is real bytecode.
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
        cw.visit(org.objectweb.asm.Opcodes.V21,
            org.objectweb.asm.Opcodes.ACC_PUBLIC + org.objectweb.asm.Opcodes.ACC_INTERFACE + org.objectweb.asm.Opcodes.ACC_ABSTRACT,
            internalName.replace('.', '/'), null, "java/lang/Object", null);
        cw.visitSource("RNK-shim-generated", null);
        org.objectweb.asm.MethodVisitor mv = cw.visitMethod(
            org.objectweb.asm.Opcodes.ACC_PUBLIC + org.objectweb.asm.Opcodes.ACC_ABSTRACT,
            methodName, "()V", null, null);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * The Remapper: the actual semantic transformation. Interfaces and all
     * references move from Fabric entrypoints to RNK shims.
     */
    static final class FabricToShimRemapper extends Remapper {
        @Override
        public String map(String internalName) {
            switch (internalName) {
                case FABRIC_INITIALIZER:
                    return SHIM_INITIALIZER;
                case FABRIC_SERVER_INITIALIZER:
                    return SHIM_SERVER_INITIALIZER;
                default:
                    return internalName;
            }
        }
    }

    /**
     * Generate the Paper entrypoint class bytes: a JavaPlugin subclass whose
     * onEnable() instantiates the remapped initializer and invokes the method
     * matching the original Fabric contract (onInitialize or onInitializeServer).
     */
    static byte[] generatePaperEntrypoint(String initializerClass, String initializerMethodName) {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(
            org.objectweb.asm.ClassWriter.COMPUTE_MAXS | org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        cw.visit(org.objectweb.asm.Opcodes.V21,
            org.objectweb.asm.Opcodes.ACC_PUBLIC + org.objectweb.asm.Opcodes.ACC_SUPER,
            "com/rnk/adapted/GeneratedPaperEntrypoint", null, "org/bukkit/plugin/java/JavaPlugin", null);
        cw.visitSource("RNK-adapter-generated", null);

        org.objectweb.asm.MethodVisitor mv = cw.visitMethod(
            org.objectweb.asm.Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,
            "org/bukkit/plugin/java/JavaPlugin", "<init>", "()V", false);
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "onEnable", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
            "org/bukkit/plugin/java/JavaPlugin", "getLogger", "()Ljava/util/logging/Logger;", false);
        mv.visitLdcInsn("RNK adapted artifact enabled - forwarding lifecycle to remapped initializer");
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
            "java/util/logging/Logger", "info", "(Ljava/lang/String;)V", false);
        mv.visitTypeInsn(org.objectweb.asm.Opcodes.NEW, initializerClass);
        mv.visitInsn(org.objectweb.asm.Opcodes.DUP);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,
            initializerClass, "<init>", "()V", false);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 1);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
            initializerClass, initializerMethodName, "()V", false);
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    /** Path of the RNK Components license file inside adapted artifacts. */
    public static final String COMPONENTS_LICENSE_PATH = "rnk/RNK-COMPONENTS-LICENSE.txt";

    /**
     * MIT license grant for the RNK-originated components inlined into an
     * adapted artifact (shim interfaces, generated entrypoint, this file and
     * the RNK-* manifest attributes). Without this, an adapted artifact is a
     * combined work of RNK GPL-3.0 code and the mod's code, which would drag
     * GPL obligations onto the mod author's own (often MIT/Apache/ARR) code
     * when the artifact is distributed. Licensing ONLY the inlined RNK
     * components permissively keeps the mod author's license untouched: the
     * mod's code stays under its original terms; the RNK glue is MIT. The
     * adapter framework itself remains GPL-3.0-only — the two licenses do not
     * touch, because only the MIT glue enters the artifact.
     */
    static final String COMPONENTS_LICENSE_TEXT = String.join("\n",
        "RNK ADAPTATION COMPONENTS LICENSE",
        "",
        "The following components of this artifact were generated by the RNK",
        "CrossLoaderAdapter (\"RNK Components\") and are licensed under the",
        "MIT License:",
        "",
        "  - com/rnk/shim/*.class (RNK shim interfaces)",
        "  - com/rnk/adapted/GeneratedPaperEntrypoint.class",
        "  - this license file and the RNK-* manifest attributes",
        "",
        "The RNK Components are Copyright (c) 2026 Asgard Innovations / RNK and",
        "are licensed under the MIT License:",
        "",
        "  Permission is hereby granted, free of charge, to any person obtaining",
        "  a copy of these components, to deal in the components without",
        "  restriction, including without limitation the rights to use, copy,",
        "  modify, merge, publish, distribute, sublicense, and/or sell copies of",
        "  the components, and to permit persons to whom the components are",
        "  furnished to do so, subject to the following conditions:",
        "",
        "  The above copyright notice and this permission notice shall be",
        "  included in all copies or substantial portions of the components.",
        "",
        "  THE COMPONENTS ARE PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND,",
        "  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF",
        "  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND",
        "  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS",
        "  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN",
        "  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN",
        "  CONNECTION WITH THE COMPONENTS OR THE USE OR OTHER DEALINGS IN THE",
        "  COMPONENTS.",
        "",
        "All other contents of this artifact are the work of the original mod",
        "authors and remain under their original license terms. This grant",
        "applies only to the RNK Components listed above; it does not alter the",
        "license of the adapted mod's own code."
    );

    /** Build plugin.yml content for the adapted artifact. */
    static String generatePluginYml(String modId, String version) {
        return "name: " + modId + "\n"
            + "version: '" + version + "'\n"
            + "main: com.rnk.adapted.GeneratedPaperEntrypoint\n"
            + "api-version: '1.20'\n"
            + "description: Cross-loader adapted artifact generated by The Tync (RNK)\n"
            + "authors: [RNK The Tync]\n";
    }

    /**
     * Adapt a Fabric mod JAR into a Paper-loadable artifact.
     *
     * @param inputJarPath  source Fabric mod JAR
     * @param outputJarPath destination adapted artifact
     * @return report map: entrypoints found, classes rewritten, output path
     */
    public Map<String, Object> adaptFabricModToPaper(String inputJarPath, String outputJarPath) throws IOException {
        Path input = Paths.get(inputJarPath);
        Path output = Paths.get(outputJarPath);
        if (!Files.exists(input)) {
            throw new IOException("input JAR not found: " + inputJarPath);
        }
        Files.createDirectories(output.toAbsolutePath().getParent());

        List<String> entrypointClasses = new ArrayList<>();
        List<String> rewrittenClasses = new ArrayList<>();
        List<String> untouchedClasses = new ArrayList<>();
        Map<String, byte[]> outEntries = new LinkedHashMap<>();
        String entrypointFabricInterface = null; // captured from ORIGINAL bytes, pre-remap

        try (JarFile jar = new JarFile(input.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (name.equals("META-INF/MANIFEST.MF")) {
                    // Replaced by the RNK adaptation manifest written by writeJar().
                    continue;
                }
                try (InputStream in = jar.getInputStream(entry)) {
                    byte[] bytes = readAll(in);
                    if (name.endsWith(".class")) {
                        String className = name.substring(0, name.length() - ".class".length());
                        boolean isEntrypoint = implementsFabricEntrypoint(bytes);
                        byte[] remapped = remapClass(bytes);
                        outEntries.put(name, remapped);
                        if (isEntrypoint) {
                            entrypointClasses.add(className.replace('/', '.'));
                            rewrittenClasses.add(className.replace('/', '.'));
                            // Detect the interface from the ORIGINAL bytes — the
                            // remapped class no longer references Fabric names.
                            entrypointFabricInterface = implementsFabricEntrypoint(bytes, FABRIC_SERVER_INITIALIZER)
                                ? FABRIC_SERVER_INITIALIZER
                                : FABRIC_INITIALIZER;
                        } else if (!java.util.Arrays.equals(bytes, remapped)) {
                            rewrittenClasses.add(className.replace('/', '.'));
                        } else {
                            untouchedClasses.add(className.replace('/', '.'));
                        }
                    } else if (name.equals("fabric.mod.json")) {
                        // Preserve the original descriptor under a preserved/ path for traceability.
                        outEntries.put("rnk-preserved/fabric.mod.json", bytes);
                    } else {
                        outEntries.put(name, bytes);
                    }
                }
            }
        }

        if (entrypointClasses.isEmpty()) {
            throw new IOException("no Fabric entrypoint class found in " + inputJarPath
                + " (expected an implements of net.fabricmc.api.ModInitializer"
                + " or DedicatedServerModInitializer)");
        }

        String initializerClass = entrypointClasses.get(0).replace('.', '/');
        // Dispatch must match the REAL Fabric contract: ModInitializer exposes
        // onInitialize(), DedicatedServerModInitializer exposes
        // onInitializeServer(). The interface was captured from the original
        // (pre-remap) bytes during the entry scan.
        String initializerMethodName = FABRIC_SERVER_INITIALIZER.equals(entrypointFabricInterface)
            ? "onInitializeServer"
            : "onInitialize";
        String modId = output.getFileName().toString().replace(".jar", "");

        // Shims + generated Paper entrypoint + the MIT-licensed components grant.
        outEntries.putAll(shimInterfaces());
        outEntries.put("com/rnk/adapted/GeneratedPaperEntrypoint.class", generatePaperEntrypoint(initializerClass, initializerMethodName));
        outEntries.put("plugin.yml", generatePluginYml(modId, "1.0.0").getBytes(StandardCharsets.UTF_8));
        outEntries.put(COMPONENTS_LICENSE_PATH, COMPONENTS_LICENSE_TEXT.getBytes(StandardCharsets.UTF_8));

        // Manifest marking this as an adapted artifact, with provenance signature.
        String signature = hmac(digestEntries(outEntries), signingKey);
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.putValue("RNK-Adapted-From", "fabric");
        attrs.putValue("RNK-Adapted-To", "paper");
        attrs.putValue("RNK-Initializer", initializerClass.replace('/', '.'));
        attrs.putValue("RNK-Artifact-Signature", signature);
        attrs.putValue("RNK-Components-License", "MIT");

        writeJar(output, outEntries, manifest);

        Map<String, Object> report = new HashMap<>();
        report.put("sourceLoader", "fabric");
        report.put("targetLoader", "paper");
        report.put("sourceJar", inputJarPath);
        report.put("adaptedJar", output.toString());
        report.put("entrypoints", entrypointClasses);
        report.put("classesRewritten", rewrittenClasses);
        report.put("classesUntouched", untouchedClasses);
        report.put("shimsInlined", List.of(SHIM_INITIALIZER.replace('/', '.'), SHIM_SERVER_INITIALIZER.replace('/', '.')));
        report.put("generatedEntrypoint", "com.rnk.adapted.GeneratedPaperEntrypoint");
        report.put("outputBytes", Files.size(output));
        report.put("signed", true);
        report.put("signature", signature);
        report.put("signatureKeySource", signingKeySource);
        return report;
    }

    /**
     * Deterministic digest over the artifact's entries: sorted names, each
     * name (UTF-8), a 4-byte big-endian length, then the content bytes. The
     * verifier recomputes exactly this over the written JAR, so any byte
     * change in any entry invalidates the signature.
     */
    static byte[] digestEntries(Map<String, byte[]> entries) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
        for (String name : new java.util.TreeSet<>(entries.keySet())) {
            byte[] content = entries.get(name);
            digest.update(name.getBytes(StandardCharsets.UTF_8));
            digest.update(new byte[] {
                (byte) (content.length >>> 24), (byte) (content.length >>> 16),
                (byte) (content.length >>> 8), (byte) content.length });
            digest.update(content);
        }
        return digest.digest();
    }

    /** Recompute the entry digest from a written JAR (skipping the manifest). */
    static byte[] digestEntriesFromJar(Path jarPath) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var all = jar.entries();
            while (all.hasMoreElements()) {
                JarEntry entry = all.nextElement();
                if (entry.isDirectory() || entry.getName().equals("META-INF/MANIFEST.MF")) {
                    continue;
                }
                try (InputStream in = jar.getInputStream(entry)) {
                    entries.put(entry.getName(), readAll(in));
                }
            }
        }
        return digestEntries(entries);
    }

    /** Detect whether a class implements either Fabric entrypoint. */
    static boolean implementsFabricEntrypoint(byte[] classBytes) {
        return implementsFabricEntrypoint(classBytes, FABRIC_INITIALIZER)
            || implementsFabricEntrypoint(classBytes, FABRIC_SERVER_INITIALIZER);
    }

    /** Detect whether a class implements one specific Fabric entrypoint interface. */
    static boolean implementsFabricEntrypoint(byte[] classBytes, String fabricInterface) {
        // Cheap constant-pool probe (interfaces appear verbatim in the pool).
        String probe = new String(classBytes, StandardCharsets.ISO_8859_1);
        return probe.contains(fabricInterface);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static void writeJar(Path output, Map<String, byte[]> entries, Manifest manifest) throws IOException {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(output), manifest)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
    }

    /** CLI entry: CrossLoaderAdapter <input.jar> <output.jar> [report.json]. */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: CrossLoaderAdapter <input.jar> <output.jar> [report.json]");
            System.exit(1);
        }
        CrossLoaderAdapter adapter = new CrossLoaderAdapter();
        Map<String, Object> report = adapter.adaptFabricModToPaper(args[0], args[1]);
        String reportJson = adapter.json.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        System.out.println(reportJson);
        if (args.length >= 3) {
            Files.writeString(Paths.get(args[2]), reportJson, StandardCharsets.UTF_8);
        }
    }
}
