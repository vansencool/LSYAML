package net.vansencool.lsyaml;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuilderTest {

    @Test
    void buildSimpleMap() {
        MapNode map = LSYAML.map()
                .put("name", "John")
                .put("age", 30)
                .build();

        assertEquals("John", map.getString("name"));
        assertEquals(30, map.getInt("age", 0));
    }

    @Test
    void buildNestedMap() {
        MapNode map = LSYAML.map()
                .put("database", LSYAML.map()
                        .put("host", "localhost")
                        .put("port", 3306))
                .build();

        MapNode db = map.getMap("database");
        assertNotNull(db);
        assertEquals("localhost", db.getString("host"));
        assertEquals(3306, db.getInt("port", 0));
    }

    @Test
    void buildSimpleList() {
        ListNode list = LSYAML.list()
                .add("apple")
                .add("banana")
                .add("cherry")
                .build();

        assertEquals(3, list.size());
        assertEquals("apple", list.getString(0));
        assertEquals("banana", list.getString(1));
        assertEquals("cherry", list.getString(2));
    }

    @Test
    void buildMapWithList() {
        MapNode map = LSYAML.map()
                .put("fruits", LSYAML.list()
                        .add("apple")
                        .add("banana"))
                .build();

        ListNode fruits = map.getList("fruits");
        assertNotNull(fruits);
        assertEquals(2, fruits.size());
    }

    @Test
    void buildListOfMaps() {
        ListNode list = LSYAML.list()
                .add(LSYAML.map()
                        .put("name", "Alice")
                        .put("age", 25))
                .add(LSYAML.map()
                        .put("name", "Bob")
                        .put("age", 30))
                .build();

        assertEquals(2, list.size());
        MapNode alice = list.getMap(0);
        assertEquals("Alice", alice.getString("name"));
    }

    @Test
    void buildWithComments() {
        MapNode map = LSYAML.map()
                .comment(" Configuration file")
                .entry("database")
                .comment(" Database settings")
                .value(LSYAML.map()
                        .put("host", "localhost"))
                .build();

        assertFalse(map.getCommentsBefore().isEmpty());
        MapNode.MapEntry dbEntry = map.getEntry("database");
        assertFalse(dbEntry.getCommentsBefore().isEmpty());
    }

    @Test
    void buildWithInlineComments() {
        MapNode map = LSYAML.map()
                .entry("port")
                .inlineComment(" Default port")
                .value(8080)
                .build();

        MapNode.MapEntry portEntry = map.getEntry("port");
        assertEquals(" Default port", portEntry.getInlineComment());
    }

    @Test
    void buildWithEmptyLines() {
        MapNode map = LSYAML.map()
                .put("section1", "value1")
                .entry("section2")
                .emptyLines(1)
                .value("value2")
                .build();

        MapNode.MapEntry entry = map.getEntry("section2");
        assertEquals(1, entry.getEmptyLinesBefore());
    }

    @Test
    void buildFlowStyle() {
        MapNode map = LSYAML.map()
                .flow()
                .put("a", 1)
                .put("b", 2)
                .build();

        assertEquals(CollectionStyle.FLOW, map.getStyle());
    }

    @Test
    void buildWithAnchors() {
        MapNode map = LSYAML.map()
                .anchor("myAnchor")
                .put("key", "value")
                .build();

        assertTrue(map.getMetadata().hasAnchor());
        assertEquals("myAnchor", map.getMetadata().getAnchor());
    }

    @Test
    void buildScalarWithStyle() {
        ScalarNode scalar = LSYAML.scalar()
                .string("hello world")
                .doubleQuoted()
                .build();

        assertEquals("hello world", scalar.getStringValue());
        assertEquals(ScalarStyle.DOUBLE_QUOTED, scalar.getStyle());
    }

    @Test
    void buildMixedTypes() {
        MapNode map = LSYAML.map()
                .put("string", "text")
                .put("integer", 42)
                .put("float", 3.14)
                .put("boolean", true)
                .put("long", 9999999999L)
                .build();

        assertEquals("text", map.getString("string"));
        assertEquals(42, map.getInt("integer", 0));
        assertTrue(map.getBoolean("boolean", false));
    }

    @Test
    void buildComplexStructure() {
        MapNode config = LSYAML.map()
                .comment(" Application Configuration")
                .entry("app")
                .comment(" Application settings")
                .value(LSYAML.map()
                        .put("name", "MyApp")
                        .put("version", "1.0.0"))
                .entry("servers")
                .comment(" Server list")
                .value(LSYAML.list()
                        .add(LSYAML.map()
                                .put("host", "server1.example.com")
                                .put("port", 8080))
                        .add(LSYAML.map()
                                .put("host", "server2.example.com")
                                .put("port", 8081)))
                .build();

        assertNotNull(config.getMap("app"));
        assertEquals("MyApp", config.getMap("app").getString("name"));

        ListNode servers = config.getList("servers");
        assertEquals(2, servers.size());
        assertEquals("server1.example.com", servers.getMap(0).getString("host"));
    }
}
