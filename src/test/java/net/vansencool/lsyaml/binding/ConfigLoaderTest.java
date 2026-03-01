package net.vansencool.lsyaml.binding;

import net.vansencool.lsyaml.builder.MapBuilder;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("all")
class ConfigLoaderTest {

    private static final Path TEST_DIR = Path.of("build/test-configs");

    @BeforeAll
    static void setupDir() throws IOException {
        Files.createDirectories(TEST_DIR);
    }

    @BeforeEach
    void resetState() {
        BasicConfig.name = "TestServer";
        BasicConfig.port = 25565;
        BasicConfig.debug = true;

        UppercaseConfig.NAME = "UpperServer";
        UppercaseConfig.MAX_CONNECTIONS = 100;

        KeyUppercaseConfig.SERVER_NAME = "KeyUpperServer";

        ExplicitKeyConfig.NAME = "ExplicitServer";
        ExplicitKeyConfig.API_KEY = "secret123";

        NestedUppercaseConfig.APP_NAME = "NestedApp";
        NestedUppercaseConfig.DATABASE = new NestedUppercaseConfig.DatabaseConfig();

        LocationConfig.SPAWN = new Location(10.0, 64.0, 20.0);

        SpacedSectionConfig.NAME = "Test";
        SpacedSectionConfig.SECTION = new SpacedSectionConfig.SectionConfig();

        SpaceAfterConfig.name = "SpaceAfterTest";
        SpaceAfterConfig.port = 25565;
        SpaceAfterConfig.debug = true;

        NestedConfig.name = "MyApp";
        NestedConfig.database = new NestedConfig.Database();

        HyphenConfig.serverName = "HyphenServer";
        HyphenConfig.maxPlayers = 50;

        HyphenWithKeyConfig.serverName = "KeyOverride";
        HyphenWithKeyConfig.maxPlayers = 50;
    }

    @AfterEach
    void cleanup() {
        ConfigLoader.unloadAll();
        try {
            Files.walk(TEST_DIR)
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {}
                    });
        } catch (IOException e) {}
    }

    @Test
    void testBasicConfig() {
        ConfigLoader.load(BasicConfig.class);

        assertTrue(ConfigLoader.isLoaded(BasicConfig.class));
        assertTrue(Files.exists(Path.of("build/test-configs/basic.yml")));

        assertEquals("TestServer", BasicConfig.name);
        assertEquals(25565, BasicConfig.port);
        assertTrue(BasicConfig.debug);
    }

    @Test
    void testLoadFromExistingFile() throws IOException {
        String yaml = """
                name: CustomServer
                port: 8080
                debug: false
                """;
        Files.writeString(Path.of("build/test-configs/basic.yml"), yaml);

        ConfigLoader.load(BasicConfig.class);

        assertEquals("CustomServer", BasicConfig.name);
        assertEquals(8080, BasicConfig.port);
        assertFalse(BasicConfig.debug);
    }

    @Test
    void testListConfig() {
        ConfigLoader.load(ListConfig.class);

        assertNotNull(ListConfig.tags);
        assertEquals(3, ListConfig.tags.size());
        assertEquals("fast", ListConfig.tags.get(0));
        assertEquals("simple", ListConfig.tags.get(1));
        assertEquals("clean", ListConfig.tags.get(2));
    }

    @Test
    void testNestedConfig() {
        ConfigLoader.load(NestedConfig.class);

        assertEquals("MyApp", NestedConfig.name);
        assertNotNull(NestedConfig.database);
        assertEquals("localhost", NestedConfig.database.host);
        assertEquals(3306, NestedConfig.database.port);
    }

    @Test
    void testDeeplyNestedConfig() {
        ConfigLoader.load(DeeplyNestedConfig.class);

        assertEquals("DeepApp", DeeplyNestedConfig.name);
        assertNotNull(DeeplyNestedConfig.database);
        assertEquals("dbhost", DeeplyNestedConfig.database.host);
        assertNotNull(DeeplyNestedConfig.database.auth);
        assertEquals("admin", DeeplyNestedConfig.database.auth.user);
        assertEquals("secret123", DeeplyNestedConfig.database.auth.password);
    }

    @Test
    void testKeyAnnotation() {
        ConfigLoader.load(KeyAnnotationConfig.class);

        assertTrue(Files.exists(Path.of("build/test-configs/key-test.yml")));

        try {
            String content = Files.readString(Path.of("build/test-configs/key-test.yml"));
            assertTrue(content.contains("server_name:"));
            assertTrue(content.contains("max_players:"));
        } catch (IOException e) {
            fail("Failed to read config file");
        }
    }

    @Test
    void testCommentAnnotation() throws IOException {
        ConfigLoader.load(CommentConfig.class);

        String content = Files.readString(Path.of("build/test-configs/comment-test.yml"));
        assertTrue(content.contains("# The server name"));
        assertTrue(content.contains("# The server port"));
        assertTrue(content.contains("# Set to 0 for random"));
    }

    @Test
    void testReload() throws IOException {
        ConfigLoader.load(BasicConfig.class);
        assertEquals("TestServer", BasicConfig.name);

        String yaml = """
                name: ReloadedServer
                port: 9999
                debug: true
                """;
        Files.writeString(Path.of("build/test-configs/basic.yml"), yaml);

        ConfigLoader.reload();

        assertEquals("ReloadedServer", BasicConfig.name);
        assertEquals(9999, BasicConfig.port);
    }

    @Test
    void testSave() {
        ConfigLoader.load(BasicConfig.class);

        BasicConfig.name = "ModifiedServer";
        BasicConfig.port = 12345;

        ConfigLoader.save(BasicConfig.class);

        try {
            String content = Files.readString(Path.of("build/test-configs/basic.yml"));
            assertTrue(content.contains("ModifiedServer"));
            assertTrue(content.contains("12345"));
        } catch (IOException e) {
            fail("Failed to read config file");
        }
    }

    @Test
    void testIgnoreAnnotation() {
        ConfigLoader.load(IgnoreConfig.class);

        try {
            String content = Files.readString(Path.of("build/test-configs/ignore-test.yml"));
            assertTrue(content.contains("name:"));
            assertFalse(content.contains("cached"));
        } catch (IOException e) {
            fail("Failed to read config file");
        }
    }

    @Test
    void testCustomAdapter() {
        ConfigLoader.registerAdapter(TestDuration.class, new TestDurationAdapter());
        ConfigLoader.load(AdapterConfig.class);

        assertNotNull(AdapterConfig.timeout);
        assertEquals(30, AdapterConfig.timeout.minutes());

        try {
            String content = Files.readString(Path.of("build/test-configs/adapter-test.yml"));
            assertTrue(content.contains("30"));
        } catch (IOException e) {
            fail("Failed to read config file");
        }
    }

    @Test
    void testSpaceAnnotation() throws IOException {
        ConfigLoader.load(SpaceConfig.class);

        String content = Files.readString(Path.of("build/test-configs/space-test.yml"));
        assertTrue(content.contains("\n\nport:"));
    }

    @Test
    void testSpaceAfterAnnotation() throws IOException {
        ConfigLoader.load(SpaceAfterConfig.class);

        String content = Files.readString(Path.of("build/test-configs/space-after-test.yml"));
        assertTrue(content.contains("name: SpaceAfterTest\n\n\n\nport:"), "@Space(after = 2) + @Space(before = 1) should produce 3 blank lines between fields");
    }

    @Test
    void testLoadNestedFromExistingFile() throws IOException {
        String yaml = """
                name: LoadedApp
                database:
                  host: remotedb
                  port: 5555
                """;
        Files.writeString(Path.of("build/test-configs/nested.yml"), yaml);

        ConfigLoader.load(NestedConfig.class);

        assertEquals("LoadedApp", NestedConfig.name);
        assertEquals("remotedb", NestedConfig.database.host);
        assertEquals(5555, NestedConfig.database.port);
    }

    @Test
    void testResetToDefaults() {
        ConfigLoader.load(BasicConfig.class);

        BasicConfig.name = "ChangedServer";
        BasicConfig.port = 1111;

        ConfigLoader.resetToDefaults(BasicConfig.class);

        assertEquals("TestServer", BasicConfig.name);
        assertEquals(25565, BasicConfig.port);
    }

    @ConfigFile("build/test-configs/basic.yml")
    public static class BasicConfig {
        public static String name = "TestServer";
        public static int port = 25565;
        public static boolean debug = true;
    }

    @ConfigFile("build/test-configs/list.yml")
    public static class ListConfig {
        public static List<String> tags = List.of("fast", "simple", "clean");
    }

    @ConfigFile("build/test-configs/nested.yml")
    public static class NestedConfig {
        public static String name = "MyApp";
        public static Database database = new Database();

        public static class Database {
            public String host = "localhost";
            public int port = 3306;
        }
    }

    @ConfigFile("build/test-configs/deep-nested.yml")
    public static class DeeplyNestedConfig {
        public static String name = "DeepApp";
        public static Database database = new Database();

        public static class Database {
            public String host = "dbhost";
            public int port = 5432;
            public Auth auth = new Auth();

            public static class Auth {
                public String user = "admin";
                public String password = "secret123";
            }
        }
    }

    @ConfigFile("build/test-configs/key-test.yml")
    public static class KeyAnnotationConfig {
        @Key("server_name")
        public static String name = "KeyServer";

        @Key("max_players")
        public static int maxPlayers = 100;
    }

    @ConfigFile("build/test-configs/comment-test.yml")
    public static class CommentConfig {
        @Comment("The server name")
        public static String name = "CommentServer";

        @Comment({"The server port", "Set to 0 for random"})
        public static int port = 25565;
    }

    @ConfigFile("build/test-configs/ignore-test.yml")
    public static class IgnoreConfig {
        public static String name = "IgnoreTest";

        @Ignore
        public static String cachedValue = "should not appear";
    }

    @ConfigFile("build/test-configs/adapter-test.yml")
    public static class AdapterConfig {
        public static String name = "AdapterTest";
        public static TestDuration timeout = new TestDuration(30);
    }

    @ConfigFile("build/test-configs/space-test.yml")
    public static class SpaceConfig {
        public static String name = "SpaceTest";

        @Space(before = 1)
        public static int port = 25565;
    }

    @ConfigFile("build/test-configs/space-after-test.yml")
    public static class SpaceAfterConfig {
        @Space(after = 2)
        public static String name = "SpaceAfterTest";

        @Space(before = 1)
        public static int port = 25565;
        public static boolean debug = true;
    }

    public record TestDuration(int minutes) {}

    public static class TestDurationAdapter implements ConfigAdapter<TestDuration> {
        @Override
        public @Nullable TestDuration fromNode(@NotNull YamlNode node) {
            Integer mins = node.asScalar().getInt();
            return mins != null ? new TestDuration(mins) : null;
        }

        @Override
        public @NotNull YamlNode toNode(@NotNull TestDuration value) {
            return new ScalarNode(value.minutes());
        }
    }

    @Test
    void testUppercaseFieldsConvertToLowercase() throws IOException {
        ConfigLoader.load(UppercaseConfig.class);

        String content = Files.readString(Path.of("build/test-configs/uppercase-test.yml"));
        assertTrue(content.contains("name:"), "Should have lowercase 'name' key");
        assertTrue(content.contains("max_connections:"), "Should have lowercase 'max_connections' key");
        assertFalse(content.contains("NAME:"), "Should NOT have uppercase 'NAME' key");
        assertFalse(content.contains("MAX_CONNECTIONS:"), "Should NOT have uppercase 'MAX_CONNECTIONS' key");

        assertEquals("UpperServer", UppercaseConfig.NAME);
        assertEquals(100, UppercaseConfig.MAX_CONNECTIONS);
    }

    @Test
    void testKeyAnnotationConvertsToLowercase() throws IOException {
        ConfigLoader.load(KeyUppercaseConfig.class);

        String content = Files.readString(Path.of("build/test-configs/key-uppercase-test.yml"));
        assertTrue(content.contains("server_name:"), "@Key should convert to lowercase");
        assertFalse(content.contains("SERVER_NAME:"), "@Key should NOT keep uppercase");

        assertEquals("KeyUpperServer", KeyUppercaseConfig.SERVER_NAME);
    }

    @Test
    void testExplicitKeyPreservesExactCase() throws IOException {
        ConfigLoader.load(ExplicitKeyConfig.class);

        String content = Files.readString(Path.of("build/test-configs/explicit-key-test.yml"));
        assertTrue(content.contains("serverName:"), "@ExplicitKey should preserve exact case 'serverName'");
        assertTrue(content.contains("API_KEY:"), "@ExplicitKey should preserve exact case 'API_KEY'");
        assertFalse(content.contains("servername:"), "Should NOT have lowercase 'servername'");
        assertFalse(content.contains("api_key:"), "Should NOT have lowercase 'api_key'");

        assertEquals("ExplicitServer", ExplicitKeyConfig.NAME);
        assertEquals("secret123", ExplicitKeyConfig.API_KEY);
    }

    @Test
    void testLoadUppercaseFromExistingFile() throws IOException {
        String yaml = """
                name: LoadedServer
                max_connections: 200
                """;
        Files.writeString(Path.of("build/test-configs/uppercase-test.yml"), yaml);

        ConfigLoader.load(UppercaseConfig.class);

        assertEquals("LoadedServer", UppercaseConfig.NAME);
        assertEquals(200, UppercaseConfig.MAX_CONNECTIONS);
    }

    @Test
    void testLoadExplicitKeyFromExistingFile() throws IOException {
        String yaml = """
                serverName: LoadedExplicit
                API_KEY: loaded_secret
                """;
        Files.writeString(Path.of("build/test-configs/explicit-key-test.yml"), yaml);

        ConfigLoader.load(ExplicitKeyConfig.class);

        assertEquals("LoadedExplicit", ExplicitKeyConfig.NAME);
        assertEquals("loaded_secret", ExplicitKeyConfig.API_KEY);
    }

    @Test
    void testNestedUppercaseFields() throws IOException {
        ConfigLoader.load(NestedUppercaseConfig.class);

        String content = Files.readString(Path.of("build/test-configs/nested-uppercase-test.yml"));
        assertTrue(content.contains("app_name:"));
        assertTrue(content.contains("database:"));
        assertTrue(content.contains("host:"));
        assertTrue(content.contains("max_pool:"));

        assertEquals("NestedApp", NestedUppercaseConfig.APP_NAME);
        assertNotNull(NestedUppercaseConfig.DATABASE);
        assertEquals("dbhost", NestedUppercaseConfig.DATABASE.HOST);
        assertEquals(50, NestedUppercaseConfig.DATABASE.MAX_POOL);
    }

    @ConfigFile("build/test-configs/uppercase-test.yml")
    public static class UppercaseConfig {
        public static String NAME = "UpperServer";
        public static int MAX_CONNECTIONS = 100;
    }

    @ConfigFile("build/test-configs/key-uppercase-test.yml")
    public static class KeyUppercaseConfig {
        @Key("SERVER_NAME")
        public static String SERVER_NAME = "KeyUpperServer";
    }

    @ConfigFile("build/test-configs/explicit-key-test.yml")
    public static class ExplicitKeyConfig {
        @ExplicitKey("serverName")
        public static String NAME = "ExplicitServer";

        @ExplicitKey("API_KEY")
        public static String API_KEY = "secret123";
    }

    @ConfigFile("build/test-configs/nested-uppercase-test.yml")
    public static class NestedUppercaseConfig {
        public static String APP_NAME = "NestedApp";
        public static DatabaseConfig DATABASE = new DatabaseConfig();

        public static class DatabaseConfig {
            public String HOST = "dbhost";
            public int MAX_POOL = 50;
        }
    }

    @Test
    void testMapAdapter() throws IOException {
        ConfigLoader.registerAdapter(Location.class, new LocationAdapter());
        ConfigLoader.load(LocationConfig.class);

        String content = Files.readString(Path.of("build/test-configs/location-test.yml"));
        assertTrue(content.contains("spawn:"));
        assertTrue(content.contains("x:"));
        assertTrue(content.contains("y:"));
        assertTrue(content.contains("z:"));
        assertTrue(content.contains("10.0") || content.contains("10"));
        assertTrue(content.contains("64.0") || content.contains("64"));
        assertTrue(content.contains("20.0") || content.contains("20"));

        assertNotNull(LocationConfig.SPAWN);
        assertEquals(10.0, LocationConfig.SPAWN.x(), 0.001);
        assertEquals(64.0, LocationConfig.SPAWN.y(), 0.001);
        assertEquals(20.0, LocationConfig.SPAWN.z(), 0.001);
    }

    @Test
    void testMapAdapterLoadFromFile() throws IOException {
        ConfigLoader.registerAdapter(Location.class, new LocationAdapter());

        String yaml = """
                spawn:
                  x: 100.5
                  y: 128.0
                  z: -50.25
                """;
        Files.writeString(Path.of("build/test-configs/location-test.yml"), yaml);

        ConfigLoader.load(LocationConfig.class);

        assertNotNull(LocationConfig.SPAWN);
        assertEquals(100.5, LocationConfig.SPAWN.x(), 0.001);
        assertEquals(128.0, LocationConfig.SPAWN.y(), 0.001);
        assertEquals(-50.25, LocationConfig.SPAWN.z(), 0.001);
    }

    @Test
    void testNoExtraBlankLineInsideNestedSection() throws IOException {
        ConfigLoader.load(SpacedSectionConfig.class);

        String content = Files.readString(Path.of("build/test-configs/spaced-section-test.yml"));
        assertFalse(
            content.contains("section:\n\n"),
            "Should NOT have a blank line immediately inside the section block"
        );
        assertTrue(content.contains("\nsection:"), "Should have blank line BEFORE section key");
    }

    @ConfigFile("build/test-configs/location-test.yml")
    public static class LocationConfig {
        public static Location SPAWN = new Location(10.0, 64.0, 20.0);
    }

    @ConfigFile("build/test-configs/spaced-section-test.yml")
    public static class SpacedSectionConfig {
        public static String NAME = "Test";

        @Space(before = 1)
        public static SectionConfig SECTION = new SectionConfig();

        public static class SectionConfig {
            public String HOST = "localhost";
            public int PORT = 8080;
        }
    }

    public record Location(double x, double y, double z) {}

    public static class LocationAdapter implements ConfigAdapter<Location> {
        @Override
        public @Nullable Location fromNode(@NotNull YamlNode node) {
            MapNode map = node.asMap();
            Double x = map.getDouble("x");
            Double y = map.getDouble("y");
            Double z = map.getDouble("z");
            if (x == null || y == null || z == null) {
                return null;
            }
            return new Location(x, y, z);
        }

        @Override
        public @NotNull YamlNode toNode(@NotNull Location value) {
            return MapBuilder.create()
                    .put("x", value.x())
                    .put("y", value.y())
                    .put("z", value.z())
                    .build();
        }
    }

    @Test
    void testPreferKeysWithHyphen() throws IOException {
        ConfigLoader.load(HyphenConfig.class);

        String content = Files.readString(Path.of("build/test-configs/hyphen-test.yml"));
        assertTrue(content.contains("max-players:"), "Should write keys with hyphen separator");
        assertTrue(content.contains("server-name:"), "Should write keys with hyphen separator");
        assertFalse(content.contains("maxplayers:"), "Should not use plain lowercase");

        assertEquals("HyphenServer", HyphenConfig.serverName);
        assertEquals(50, HyphenConfig.maxPlayers);
    }

    @Test
    void testPreferKeysWithHyphenFallbackToUnderscore() throws IOException {
        String yaml = """
                server_name: UnderscoreServer
                max_players: 99
                """;
        Files.writeString(Path.of("build/test-configs/hyphen-test.yml"), yaml);

        ConfigLoader.load(HyphenConfig.class);

        assertEquals("UnderscoreServer", HyphenConfig.serverName);
        assertEquals(99, HyphenConfig.maxPlayers);
    }

    @Test
    void testPreferKeysWithHyphenPreferredOverUnderscore() throws IOException {
        String yaml = """
                server-name: PreferredServer
                max-players: 77
                """;
        Files.writeString(Path.of("build/test-configs/hyphen-test.yml"), yaml);

        ConfigLoader.load(HyphenConfig.class);

        assertEquals("PreferredServer", HyphenConfig.serverName);
        assertEquals(77, HyphenConfig.maxPlayers);
    }

    @Test
    void testPreferKeysWithKeyAnnotationOverride() throws IOException {
        ConfigLoader.load(HyphenWithKeyConfig.class);

        String content = Files.readString(Path.of("build/test-configs/hyphen-key-test.yml"));
        assertTrue(content.contains("custom_name:"), "@Key should override @PreferKeysWith");
        assertTrue(content.contains("max-players:"), "field without @Key should use @PreferKeysWith");
    }

    @Test
    void testTypeMismatchIntGetsString() throws IOException {
        String yaml = """
                name: TypeMismatchTest
                port: not_a_number
                debug: true
                """;
        Files.writeString(Path.of("build/test-configs/basic.yml"), yaml);

        ConfigLoader.load(BasicConfig.class);

        assertEquals("TypeMismatchTest", BasicConfig.name);
        assertEquals(25565, BasicConfig.port);
        assertTrue(BasicConfig.debug);
    }

    @Test
    void testTypeMismatchBooleanGetsString() throws IOException {
        String yaml = """
                name: BoolTest
                port: 8080
                debug: banana
                """;
        Files.writeString(Path.of("build/test-configs/basic.yml"), yaml);

        ConfigLoader.load(BasicConfig.class);

        assertEquals("BoolTest", BasicConfig.name);
        assertEquals(8080, BasicConfig.port);
        assertTrue(BasicConfig.debug);
    }

    @Test
    void testTypeMismatchListGetsScalar() throws IOException {
        String yaml = """
                tags: not_a_list
                """;
        Files.writeString(Path.of("build/test-configs/list.yml"), yaml);

        ConfigLoader.load(ListConfig.class);

        assertNotNull(ListConfig.tags);
        assertEquals(3, ListConfig.tags.size());
    }

    @Test
    void testCamelToSeparated() {
        assertEquals("max-players", TypeConverters.camelToSeparated("maxPlayers", "-"));
        assertEquals("max_players", TypeConverters.camelToSeparated("maxPlayers", "_"));
        assertEquals("http-server", TypeConverters.camelToSeparated("HTTPServer", "-"));
        assertEquals("max-connections", TypeConverters.camelToSeparated("MAX_CONNECTIONS", "-"));
        assertEquals("name", TypeConverters.camelToSeparated("name", "-"));
        assertEquals("server-name", TypeConverters.camelToSeparated("serverName", "-"));
    }

    @ConfigFile("build/test-configs/hyphen-test.yml")
    @PreferKeysWith("-")
    public static class HyphenConfig {
        public static String serverName = "HyphenServer";
        public static int maxPlayers = 50;
    }

    @ConfigFile("build/test-configs/hyphen-key-test.yml")
    @PreferKeysWith("-")
    public static class HyphenWithKeyConfig {
        @Key("custom_name")
        public static String serverName = "KeyOverride";
        public static int maxPlayers = 50;
    }
}
