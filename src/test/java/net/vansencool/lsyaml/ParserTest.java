package net.vansencool.lsyaml;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("all")
class ParserTest {

    @Test
    void parseSimpleMap() {
        String yaml = "name: John\nage: 30";
        MapNode map = LSYAML.parseMap(yaml);

        assertEquals("John", map.getString("name"));
        assertEquals(30, map.getInt("age", 0));
    }

    @Test
    void parseNestedMap() {
        String yaml = """
            database:
              host: localhost
              port: 3306
            """;
        MapNode map = LSYAML.parseMap(yaml);

        MapNode db = map.getMap("database");
        assertNotNull(db);
        assertEquals("localhost", db.getString("host"));
        assertEquals(3306, db.getInt("port", 0));
    }

    @Test
    void parseSimpleList() {
        String yaml = """
            - apple
            - banana
            - cherry
            """;
        ListNode list = LSYAML.parseList(yaml);

        assertEquals(3, list.size());
        assertEquals("apple", list.getString(0));
        assertEquals("banana", list.getString(1));
        assertEquals("cherry", list.getString(2));
    }

    @Test
    void parseMapWithList() {
        String yaml = """
            fruits:
              - apple
              - banana
            vegetables:
              - carrot
            """;
        MapNode map = LSYAML.parseMap(yaml);

        ListNode fruits = map.getList("fruits");
        assertNotNull(fruits);
        assertEquals(2, fruits.size());
        assertEquals("apple", fruits.getString(0));

        ListNode vegetables = map.getList("vegetables");
        assertNotNull(vegetables);
        assertEquals(1, vegetables.size());
    }

    @Test
    void parseListOfMaps() {
        String yaml = """
            - name: Alice
              age: 25
            - name: Bob
              age: 30
            """;
        ListNode list = LSYAML.parseList(yaml);

        assertEquals(2, list.size());

        MapNode alice = list.getMap(0);
        assertNotNull(alice);
        assertEquals("Alice", alice.getString("name"));
        assertEquals(25, alice.getInt("age", 0));

        MapNode bob = list.getMap(1);
        assertNotNull(bob);
        assertEquals("Bob", bob.getString("name"));
    }

    @Test
    void parseQuotedStrings() {
        String yaml = """
            single: 'hello world'
            double: "hello world"
            plain: hello world
            """;
        MapNode map = LSYAML.parseMap(yaml);

        assertEquals("hello world", map.getString("single"));
        assertEquals("hello world", map.getString("double"));
        assertEquals("hello world", map.getString("plain"));
    }

    @Test
    void parsePreservesQuoteStyle() {
        String yaml = """
            single: 'value'
            double: "value"
            plain: value
            """;
        MapNode map = LSYAML.parseMap(yaml);

        ScalarNode single = (ScalarNode) map.get("single");
        ScalarNode dbl = (ScalarNode) map.get("double");
        ScalarNode plain = (ScalarNode) map.get("plain");

        assertEquals(ScalarStyle.SINGLE_QUOTED, single.getStyle());
        assertEquals(ScalarStyle.DOUBLE_QUOTED, dbl.getStyle());
        assertEquals(ScalarStyle.PLAIN, plain.getStyle());
    }

    @Test
    void parseNumbers() {
        String yaml = """
            integer: 42
            negative: -17
            float: 3.14
            scientific: 1.5e10
            hex: 0xFF
            """;
        MapNode map = LSYAML.parseMap(yaml);

        ScalarNode integer = (ScalarNode) map.get("integer");
        assertEquals(42, integer.getIntValue());

        ScalarNode negative = (ScalarNode) map.get("negative");
        assertEquals(-17, negative.getIntValue());

        ScalarNode flt = (ScalarNode) map.get("float");
        assertEquals(3.14, flt.getDoubleValue(), 0.001);

        ScalarNode hex = (ScalarNode) map.get("hex");
        assertEquals(255, hex.getLongValue());
    }

    @Test
    void parseBooleans() {
        String yaml = """
            yes_val: yes
            no_val: no
            true_val: true
            false_val: false
            on_val: on
            off_val: off
            """;
        MapNode map = LSYAML.parseMap(yaml);

        assertTrue(map.getBoolean("yes_val", false));
        assertFalse(map.getBoolean("no_val", true));
        assertTrue(map.getBoolean("true_val", false));
        assertFalse(map.getBoolean("false_val", true));
        assertTrue(map.getBoolean("on_val", false));
        assertFalse(map.getBoolean("off_val", true));
    }

    @Test
    void parseNull() {
        String yaml = """
            null1: null
            null2: ~
            null3:
            """;
        MapNode map = LSYAML.parseMap(yaml);

        ScalarNode null1 = (ScalarNode) map.get("null1");
        assertTrue(null1.isNull());

        ScalarNode null2 = (ScalarNode) map.get("null2");
        assertTrue(null2.isNull());

        ScalarNode null3 = (ScalarNode) map.get("null3");
        assertTrue(null3.isNull());
    }

    @Test
    void parseFlowMap() {
        String yaml = "config: {host: localhost, port: 8080}";
        MapNode map = LSYAML.parseMap(yaml);

        MapNode config = map.getMap("config");
        assertNotNull(config);
        assertEquals(CollectionStyle.FLOW, config.getStyle());
        assertEquals("localhost", config.getString("host"));
        assertEquals(8080, config.getInt("port", 0));
    }

    @Test
    void parseFlowList() {
        String yaml = "items: [a, b, c]";
        MapNode map = LSYAML.parseMap(yaml);

        ListNode items = map.getList("items");
        assertNotNull(items);
        assertEquals(CollectionStyle.FLOW, items.getStyle());
        assertEquals(3, items.size());
        assertEquals("a", items.getString(0));
    }

    @Test
    void parseComments() {
        String yaml = """
            # This is a comment
            name: John
            age: 30 # inline comment
            """;
        MapNode map = LSYAML.parseMap(yaml);

        assertFalse(map.entries().iterator().next().getCommentsBefore().isEmpty());

        MapNode.MapEntry ageEntry = map.getEntry("age");
        assertNotNull(ageEntry);
        assertEquals(" inline comment", ageEntry.getInlineComment());
    }

    @Test
    void parsePreservesEmptyLines() {
        String yaml = """
            name: John

            age: 30
            """;
        MapNode map = LSYAML.parseMap(yaml);

        MapNode.MapEntry ageEntry = map.getEntry("age");
        assertNotNull(ageEntry);
        assertTrue(ageEntry.getEmptyLinesBefore() > 0);
    }

    @Test
    void parseEscapedStrings() {
        String yaml = """
            escaped: "hello\\nworld"
            tabs: "hello\\tworld"
            """;
        MapNode map = LSYAML.parseMap(yaml);

        assertEquals("hello\nworld", map.getString("escaped"));
        assertEquals("hello\tworld", map.getString("tabs"));
    }

    @Test
    void parseEmptyDocument() {
        YamlNode node = LSYAML.parse("");
        assertNotNull(node);
        assertTrue(node instanceof MapNode);
        assertTrue(((MapNode) node).isEmpty());
    }

    @Test
    void parseQuotedKeys() {
        String yaml = """
            "special:key": value1
            'another:key': value2
            """;
        MapNode map = LSYAML.parseMap(yaml);

        assertEquals("value1", map.getString("special:key"));
        assertEquals("value2", map.getString("another:key"));
    }

    @Test
    void parseDeeplyNested() {
        String yaml = """
            level1:
              level2:
                level3:
                  level4:
                    value: deep
            """;
        MapNode map = LSYAML.parseMap(yaml);

        MapNode l1 = map.getMap("level1");
        MapNode l2 = l1.getMap("level2");
        MapNode l3 = l2.getMap("level3");
        MapNode l4 = l3.getMap("level4");

        assertEquals("deep", l4.getString("value"));
    }

    @Test
    void parseNestedMapWithCommentsBefore() {
        String yaml = """
            # Database config
            database:
              # Connection settings
              host: localhost
              port: 3306
            """;
        MapNode map = LSYAML.parseMap(yaml);

        MapNode db = map.getMap("database");
        assertNotNull(db);
        assertEquals("localhost", db.getString("host"));
        assertEquals(3306, db.getInt("port", 0));

        MapNode.MapEntry hostEntry = db.getEntry("host");
        assertFalse(hostEntry.getCommentsBefore().isEmpty());
    }

    @Test
    void parseListWithComments() {
        String yaml = """
            # List of items
            items:
              # First item
              - apple
              # Second item
              - banana
            """;
        MapNode map = LSYAML.parseMap(yaml);

        ListNode items = map.getList("items");
        assertNotNull(items);
        assertEquals(2, items.size());
        assertEquals("apple", items.getString(0));
        assertEquals("banana", items.getString(1));

        ListNode.ListEntry first = items.getEntry(0);
        assertFalse(first.getCommentsBefore().isEmpty());
    }

    @Test
    void parseListOfMapsWithComments() {
        String yaml = """
            servers:
              # Primary server
              - host: server1
                port: 8080
              # Backup server
              - host: server2
                port: 8081
            """;
        MapNode map = LSYAML.parseMap(yaml);

        ListNode servers = map.getList("servers");
        assertNotNull(servers);
        assertEquals(2, servers.size());

        MapNode server1 = servers.getMap(0);
        assertEquals("server1", server1.getString("host"));
        assertEquals(8080, server1.getInt("port", 0));

        MapNode server2 = servers.getMap(1);
        assertEquals("server2", server2.getString("host"));
    }

    @Test
    void parseComplexNestedWithComments() {
        String yaml = """
            # Application config
            app:
              # App name
              name: MyApp
              # Database section
              database:
                # DB host
                host: localhost
                # DB port
                port: 5432
              # Server list
              servers:
                # Main server
                - name: main
                  port: 8080
            """;
        MapNode map = LSYAML.parseMap(yaml);

        MapNode app = map.getMap("app");
        assertNotNull(app);
        assertEquals("MyApp", app.getString("name"));

        MapNode db = app.getMap("database");
        assertNotNull(db);
        assertEquals("localhost", db.getString("host"));
        assertEquals(5432, db.getInt("port", 0));

        ListNode servers = app.getList("servers");
        assertNotNull(servers);
        assertEquals(1, servers.size());
        assertEquals("main", servers.getMap(0).getString("name"));
    }

    @Test
    void roundTripNestedWithComments() {
        String yaml = """
            # Config header
            database:
              # Connection settings
              host: localhost
              port: 3306
            """;
        MapNode parsed = LSYAML.parseMap(yaml);
        String written = LSYAML.write(parsed);
        MapNode reparsed = LSYAML.parseMap(written);

        MapNode db = reparsed.getMap("database");
        assertNotNull(db);
        assertEquals("localhost", db.getString("host"));
        assertEquals(3306, db.getInt("port", 0));

        assertTrue(written.contains("# Config header"));
        assertTrue(written.contains("# Connection settings"));
    }

    @Test
    void parseEmptyListItems() {
        String yaml = """
            items:
              -
              - value
              -
            """;
        MapNode map = LSYAML.parseMap(yaml);

        ListNode items = map.getList("items");
        assertNotNull(items);
        assertEquals(3, items.size());
    }

    @Test
    void parseInlineCommentsOnNestedKeys() {
        String yaml = """
            server:
              host: localhost # The hostname
              port: 8080 # The port number
            """;
        MapNode map = LSYAML.parseMap(yaml);

        MapNode server = map.getMap("server");
        assertNotNull(server);

        MapNode.MapEntry hostEntry = server.getEntry("host");
        assertEquals(" The hostname", hostEntry.getInlineComment());

        MapNode.MapEntry portEntry = server.getEntry("port");
        assertEquals(" The port number", portEntry.getInlineComment());
    }

    @Test
    void parseMultipleCommentsBeforeKey() {
        String yaml = """
            # Comment line 1
            # Comment line 2
            # Comment line 3
            key: value
            """;
        MapNode map = LSYAML.parseMap(yaml);

        MapNode.MapEntry entry = map.getEntry("key");
        assertEquals(3, entry.getCommentsBefore().size());
    }

    @Test
    void parseCommentsAndEmptyLines() {
        String yaml = """
            key1: value1

            # Comment after empty line
            key2: value2
            """;
        MapNode map = LSYAML.parseMap(yaml);

        MapNode.MapEntry entry = map.getEntry("key2");
        assertTrue(entry.getEmptyLinesBefore() > 0);
        assertFalse(entry.getCommentsBefore().isEmpty());
    }
}
