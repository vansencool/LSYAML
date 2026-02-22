package net.vansencool.lsyaml.binding;

import net.vansencool.lsyaml.node.MapNode;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("all")
class LatestConfigTest {

    private static final Path TEST_DIR = Path.of("build/test-latest-configs");

    @BeforeAll
    static void setupDir() throws IOException {
        Files.createDirectories(TEST_DIR);
    }

    @BeforeEach
    void resetFields() {
        FlatConfig.name = "default-name";
        FlatConfig.port = 9999;

        PartialConfig.name = "default-name";
        PartialConfig.port = 9999;
        PartialConfig.debug = false;

        NestedMergeConfig.name = "default-name";
        NestedMergeConfig.server = new NestedMergeConfig.ServerSection();

        ExtraKeysConfig.name = "default-name";
        ExtraKeysConfig.port = 9999;

        AnnotationConfig.name = "default-name";
        AnnotationConfig.port = 9999;
        AnnotationConfig.features = new AnnotationConfig.Features();
    }

    @AfterEach
    void cleanup() {
        ConfigLoader.unloadAll();
        try {
            Files.walk(TEST_DIR)
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }

    @Test
    void noLatestConfig_noFile_generatesFromDefaults() throws IOException {
        ConfigLoader.load(FlatConfig.class);

        Path file = Path.of("build/test-latest-configs/flat.yml");
        assertTrue(Files.exists(file), "File should have been generated");

        String content = Files.readString(file);
        assertTrue(content.contains("name:"));
        assertTrue(content.contains("port:"));

        assertEquals("default-name", FlatConfig.name);
        assertEquals(9999, FlatConfig.port);
    }

    @Test
    void noLatestConfig_fileExistsWithAllKeys_valuesLoadedNoWrite() throws IOException, InterruptedException {
        Path file = Path.of("build/test-latest-configs/flat.yml");
        Files.writeString(file, "name: user-name\nport: 1234\n");

        long beforeModified = Files.getLastModifiedTime(file).toMillis();
        Thread.sleep(50);

        ConfigLoader.load(FlatConfig.class);

        assertEquals("user-name", FlatConfig.name);
        assertEquals(1234, FlatConfig.port);

        long afterModified = Files.getLastModifiedTime(file).toMillis();
        assertEquals(beforeModified, afterModified, "File should NOT be rewritten when no keys are missing");
    }

    @Test
    void noLatestConfig_fileMissingKey_mergesFromJavaDefaults() throws IOException {
        Path file = Path.of("build/test-latest-configs/flat.yml");
        Files.writeString(file, "name: user-name\n");

        ConfigLoader.load(FlatConfig.class);

        assertEquals("user-name", FlatConfig.name);
        assertEquals(9999, FlatConfig.port, "Missing key should get the Java field default value");

        String content = Files.readString(file);
        assertTrue(content.contains("port:"), "Missing key should be written back to disk");
        assertTrue(content.contains("9999"));
    }

    @Test
    void noLatestConfig_fileMissingNestedKey_mergesFromJavaDefaults() throws IOException {
        Path file = Path.of("build/test-latest-configs/nested-merge.yml");
        Files.writeString(file, "name: user-name\nserver:\n  host: custom-host\n");

        ConfigLoader.load(NestedMergeConfig.class);

        assertEquals("user-name", NestedMergeConfig.name);
        assertEquals("custom-host", NestedMergeConfig.server.host);
        assertEquals(25565, NestedMergeConfig.server.port, "Missing nested key should come from Java default");

        String content = Files.readString(file);
        assertTrue(content.contains("port:"));
        assertTrue(content.contains("25565"));
    }

    @Test
    void latestConfig_noFile_generatesFromLatest() throws IOException {
        String latest = """
                # The server name
                name: latest-name
                # The server port
                port: 8080
                """;
        ConfigLoader.setLatestConfig(FlatConfig.class, latest);
        ConfigLoader.load(FlatConfig.class);

        Path file = Path.of("build/test-latest-configs/flat.yml");
        assertTrue(Files.exists(file));

        String content = Files.readString(file);
        assertTrue(content.contains("# The server name"), "Latest config comments should be present in generated file");
        assertTrue(content.contains("# The server port"));
    }

    @Test
    void latestConfig_fileExistsWithAllKeys_userValuesPreserved() throws IOException {
        Path file = Path.of("build/test-latest-configs/flat.yml");
        Files.writeString(file, "name: user-name\nport: 7777\n");

        String latest = "# Latest comment\nname: latest-name\nport: 8080\n";
        ConfigLoader.setLatestConfig(FlatConfig.class, latest);
        ConfigLoader.load(FlatConfig.class);

        assertEquals("user-name", FlatConfig.name, "User's value must be preserved, not overwritten by latest");
        assertEquals(7777, FlatConfig.port, "User's value must be preserved, not overwritten by latest");
    }

    @Test
    void latestConfig_fileExistsWithAllKeys_latestCommentsApplied() throws IOException {
        Path file = Path.of("build/test-latest-configs/flat.yml");
        Files.writeString(file, "# old comment\nname: user-name\nport: 7777\n");

        String latest = "# new comment from latest\nname: latest-name\nport: 8080\n";
        ConfigLoader.setLatestConfig(FlatConfig.class, latest);
        ConfigLoader.load(FlatConfig.class);

        MapNode node = ConfigLoader.node(FlatConfig.class);
        assertNotNull(node);
        MapNode.MapEntry nameEntry = node.getEntry("name");
        assertNotNull(nameEntry);
        assertTrue(
                nameEntry.getCommentsBefore().stream().anyMatch(c -> c.contains("new comment from latest")),
                "Latest config's comments should be applied to the in-memory merged node"
        );
    }

    @Test
    void latestConfig_fileMissingKey_missingKeyAdded() throws IOException {
        Path file = Path.of("build/test-latest-configs/flat.yml");
        Files.writeString(file, "name: user-name\n");

        String latest = "name: latest-name\nport: 8080\n";
        ConfigLoader.setLatestConfig(FlatConfig.class, latest);
        ConfigLoader.load(FlatConfig.class);

        assertEquals("user-name", FlatConfig.name, "User's existing value must be preserved");
        assertEquals(8080, FlatConfig.port, "Missing key should come from latest config's value");

        String content = Files.readString(file);
        assertTrue(content.contains("port:"), "Missing key should be written back to disk");
        assertTrue(content.contains("8080"));
    }

    @Test
    void latestConfig_fileMissingKey_fileWrittenToDisk() throws IOException, InterruptedException {
        Path file = Path.of("build/test-latest-configs/flat.yml");
        Files.writeString(file, "name: user-name\n");

        long before = Files.getLastModifiedTime(file).toMillis();
        Thread.sleep(50);

        ConfigLoader.setLatestConfig(FlatConfig.class, "name: x\nport: 1\n");
        ConfigLoader.load(FlatConfig.class);

        long after = Files.getLastModifiedTime(file).toMillis();
        assertTrue(after > before, "File must be rewritten when keys were missing");
    }

    @Test
    void latestConfig_fileCompleteNoMissingKeys_fileNotRewritten() throws IOException, InterruptedException {
        Path file = Path.of("build/test-latest-configs/flat.yml");
        Files.writeString(file, "name: user-name\nport: 7777\n");

        long before = Files.getLastModifiedTime(file).toMillis();
        Thread.sleep(50);

        ConfigLoader.setLatestConfig(FlatConfig.class, "name: x\nport: 1\n");
        ConfigLoader.load(FlatConfig.class);

        long after = Files.getLastModifiedTime(file).toMillis();
        assertEquals(before, after, "File must NOT be rewritten when no keys are missing");
    }

    @Test
    void latestConfig_extraUserKeysNotInLatest_preserved() throws IOException {
        Path file = Path.of("build/test-latest-configs/flat.yml");
        Files.writeString(file, "name: user-name\nport: 7777\nextra-user-key: keep-me\n");

        ConfigLoader.setLatestConfig(FlatConfig.class, "name: x\nport: 1\n");
        ConfigLoader.load(FlatConfig.class);

        String content = Files.readString(file);
        assertTrue(content.contains("extra-user-key"), "User keys absent from latest config must be kept");
        assertTrue(content.contains("keep-me"));
    }

    @Test
    void latestConfig_deepNestedMerge_missingNestedKeyAdded() throws IOException {
        Path file = Path.of("build/test-latest-configs/nested-merge.yml");
        Files.writeString(file, "name: user-name\nserver:\n  host: custom-host\n");

        String latest = """
                name: latest-name
                server:
                  host: latest-host
                  port: 12345
                """;
        ConfigLoader.setLatestConfig(NestedMergeConfig.class, latest);
        ConfigLoader.load(NestedMergeConfig.class);

        assertEquals("user-name", NestedMergeConfig.name);
        assertEquals("custom-host", NestedMergeConfig.server.host, "User's nested value must be preserved");
        assertEquals(12345, NestedMergeConfig.server.port, "Missing nested key should come from latest config");

        String content = Files.readString(file);
        assertTrue(content.contains("port:"));
        assertTrue(content.contains("12345"));
    }

    @Test
    void latestConfig_deepNestedMerge_latestCommentsAppliedToNested() throws IOException {
        Path file = Path.of("build/test-latest-configs/nested-merge.yml");
        Files.writeString(file, "name: user-name\nserver:\n  host: custom-host\n  port: 8888\n");

        String latest = """
                name: latest-name
                server:
                  # The server host
                  host: latest-host
                  # The server port
                  port: 12345
                """;
        ConfigLoader.setLatestConfig(NestedMergeConfig.class, latest);
        ConfigLoader.load(NestedMergeConfig.class);

        MapNode node = ConfigLoader.node(NestedMergeConfig.class);
        assertNotNull(node);
        MapNode serverNode = node.getMap("server");
        assertNotNull(serverNode);
        MapNode.MapEntry hostEntry = serverNode.getEntry("host");
        assertNotNull(hostEntry);
        assertTrue(
                hostEntry.getCommentsBefore().stream().anyMatch(c -> c.contains("The server host")),
                "Latest config's nested comments should be applied to the in-memory merged node"
        );
        assertEquals("custom-host", NestedMergeConfig.server.host, "User's nested value must still be preserved");
        assertEquals(8888, NestedMergeConfig.server.port, "User's nested value must still be preserved");
    }

    @Test
    void latestConfig_spacingFromLatestApplied() throws IOException {
        Path file = Path.of("build/test-latest-configs/flat.yml");
        Files.writeString(file, "name: user-name\nport: 7777\n");

        String latest = "name: x\n\nport: 1\n";
        ConfigLoader.setLatestConfig(FlatConfig.class, latest);
        ConfigLoader.load(FlatConfig.class);

        MapNode node = ConfigLoader.node(FlatConfig.class);
        assertNotNull(node);
        MapNode.MapEntry portEntry = node.getEntry("port");
        assertNotNull(portEntry);
        assertTrue(portEntry.getEmptyLinesBefore() > 0, "Empty lines from latest config should be applied to merged output");
    }

    @Test
    void latestConfig_inputStream_worksIdenticallyToString() throws IOException {
        Path file = Path.of("build/test-latest-configs/flat.yml");
        Files.writeString(file, "name: user-name\n");

        ConfigLoader.setLatestConfig(FlatConfig.class,
                LatestConfigTest.class.getResourceAsStream("/test-latest-config.yml"));
        ConfigLoader.load(FlatConfig.class);

        assertEquals("user-name", FlatConfig.name);
        assertEquals(8080, FlatConfig.port, "Port missing from user file - should come from resource latest config (8080)");

        String content = Files.readString(file);
        assertTrue(content.contains("port:"));
    }

    @Test
    void getResource_findsClasspathResource() {
        assertDoesNotThrow(() -> {
            try (var stream = ConfigLoader.getResource(LatestConfigTest.class, "test-latest-config.yml")) {
                assertNotNull(stream);
                byte[] bytes = stream.readAllBytes();
                assertTrue(bytes.length > 0);
            }
        });
    }

    @Test
    void getResource_throwsForMissingResource() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfigLoader.getResource(LatestConfigTest.class, "nonexistent-resource.yml"));
    }

    @Test
    void clearLatestConfig_removesRegistration() throws IOException {
        Path file = Path.of("build/test-latest-configs/flat.yml");
        Files.writeString(file, "name: user-name\n");

        ConfigLoader.setLatestConfig(FlatConfig.class, "name: x\nport: 1111\n");
        ConfigLoader.clearLatestConfig(FlatConfig.class);
        ConfigLoader.load(FlatConfig.class);

        assertEquals(9999, FlatConfig.port,
                "After clearing latest config, missing key should come from Java default (9999), not the cleared latest (1111)");
    }

    @Test
    void annotationLatestConfig_autoLoadedFromClasspath() throws IOException {
        Path file = Path.of("build/test-latest-configs/annotation.yml");
        Files.writeString(file, "name: user-name\nport: 7777\n");

        ConfigLoader.load(AnnotationConfig.class);

        assertEquals("user-name", AnnotationConfig.name);
        assertEquals(7777, AnnotationConfig.port);

        String content = Files.readString(file);
        assertTrue(content.contains("features:"), "features section from @LatestConfig resource should be added to user file");
    }

    @Test
    void annotationLatestConfig_noFile_generatesFromAnnotationResource() throws IOException {
        ConfigLoader.load(AnnotationConfig.class);

        Path file = Path.of("build/test-latest-configs/annotation.yml");
        assertTrue(Files.exists(file));

        String content = Files.readString(file);
        assertTrue(content.contains("# Latest port setting"), "Comments from @LatestConfig resource should appear in generated file");
        assertTrue(content.contains("# Feature flags section"));
    }

    @Test
    void noLatestConfig_twoOfThreeKeysPresent_thirdAddedFromDefault() throws IOException {
        Path file = Path.of("build/test-latest-configs/partial.yml");
        Files.writeString(file, "name: user-name\nport: 7777\n");

        ConfigLoader.load(PartialConfig.class);

        assertEquals("user-name", PartialConfig.name);
        assertEquals(7777, PartialConfig.port);
        assertFalse(PartialConfig.debug, "Missing key should get the Java field default value (false)");

        String content = Files.readString(file);
        assertTrue(content.contains("debug:"), "Missing key must be written back to disk");
    }

    @Test
    void latestConfig_multipleNewKeys_allAddedToFile() throws IOException {
        Path file = Path.of("build/test-latest-configs/partial.yml");
        Files.writeString(file, "name: user-name\n");

        String latest = "name: x\nport: 5555\ndebug: true\n";
        ConfigLoader.setLatestConfig(PartialConfig.class, latest);
        ConfigLoader.load(PartialConfig.class);

        assertEquals("user-name", PartialConfig.name);
        assertEquals(5555, PartialConfig.port, "Both missing keys should come from latest config");
        assertTrue(PartialConfig.debug, "Both missing keys should come from latest config");

        String content = Files.readString(file);
        assertTrue(content.contains("port:"));
        assertTrue(content.contains("debug:"));
    }

    @ConfigFile("build/test-latest-configs/flat.yml")
    public static class FlatConfig {
        public static String name = "default-name";
        public static int port = 9999;
    }

    @ConfigFile("build/test-latest-configs/partial.yml")
    public static class PartialConfig {
        public static String name = "default-name";
        public static int port = 9999;
        public static boolean debug = false;
    }

    @ConfigFile("build/test-latest-configs/nested-merge.yml")
    public static class NestedMergeConfig {
        public static String name = "default-name";
        public static ServerSection server = new ServerSection();

        public static class ServerSection {
            public String host = "localhost";
            public int port = 25565;
        }
    }

    @ConfigFile("build/test-latest-configs/extra-keys.yml")
    public static class ExtraKeysConfig {
        public static String name = "default-name";
        public static int port = 9999;
    }

    @ConfigFile("build/test-latest-configs/annotation.yml")
    @LatestConfig("test-latest-config.yml")
    public static class AnnotationConfig {
        public static String name = "default-name";
        public static int port = 9999;
        public static Features features = new Features();

        public static class Features {
            public boolean enabled = true;
            public int limit = 1000;
        }
    }
}
