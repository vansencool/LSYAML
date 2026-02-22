package net.vansencool.lsyaml;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.writer.YamlWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WriterTest {

    @Test
    void writeSimpleMap() {
        MapNode map = LSYAML.map()
                .put("name", "John")
                .put("age", 30)
                .build();

        String yaml = LSYAML.write(map);

        assertTrue(yaml.contains("name: John"));
        assertTrue(yaml.contains("age: 30"));
    }

    @Test
    void writeNestedMap() {
        MapNode map = LSYAML.map()
                .put("database", LSYAML.map()
                        .put("host", "localhost")
                        .put("port", 3306))
                .build();

        String yaml = LSYAML.write(map);

        assertTrue(yaml.contains("database:"));
        assertTrue(yaml.contains("  host: localhost"));
        assertTrue(yaml.contains("  port: 3306"));
    }

    @Test
    void writeSimpleList() {
        ListNode list = LSYAML.list()
                .add("apple")
                .add("banana")
                .build();

        String yaml = LSYAML.write(list);

        assertTrue(yaml.contains("- apple"));
        assertTrue(yaml.contains("- banana"));
    }

    @Test
    void writeFlowMap() {
        MapNode map = LSYAML.map()
                .flow()
                .put("a", 1)
                .put("b", 2)
                .build();

        String yaml = LSYAML.write(map);

        assertTrue(yaml.contains("{"));
        assertTrue(yaml.contains("}"));
    }

    @Test
    void writeFlowList() {
        ListNode list = LSYAML.list()
                .flow()
                .add("a")
                .add("b")
                .build();

        String yaml = LSYAML.write(list);

        assertTrue(yaml.contains("["));
        assertTrue(yaml.contains("]"));
    }

    @Test
    void writePreservesComments() {
        MapNode map = LSYAML.map()
                .comment(" Main config")
                .entry("key")
                .comment(" Key comment")
                .inlineComment(" inline")
                .value("value")
                .build();

        String yaml = LSYAML.write(map);

        assertTrue(yaml.contains("# Main config"));
        assertTrue(yaml.contains("# Key comment"));
        assertTrue(yaml.contains("# inline"));
    }

    @Test
    void writeWithCustomIndent() {
        MapNode map = LSYAML.map()
                .put("level1", LSYAML.map()
                        .put("level2", "value"))
                .build();

        String yaml = LSYAML.writer()
                .indentSize(4)
                .write(map);

        assertTrue(yaml.contains("    level2: value"));
    }

    @Test
    void writeDisableComments() {
        MapNode map = LSYAML.map()
                .comment(" Comment")
                .put("key", "value")
                .build();

        String yaml = LSYAML.writer()
                .preserveComments(false)
                .write(map);

        assertFalse(yaml.contains("#"));
    }

    @Test
    void roundTripSimple() {
        String original = """
            name: John
            age: 30
            """;

        MapNode parsed = LSYAML.parseMap(original);
        String written = LSYAML.write(parsed);
        MapNode reparsed = LSYAML.parseMap(written);

        assertEquals(parsed.getString("name"), reparsed.getString("name"));
        assertEquals(parsed.getInt("age", 0), reparsed.getInt("age", 0));
    }

    @Test
    void roundTripNested() {
        String original = """
            server:
              host: localhost
              port: 8080
            """;

        MapNode parsed = LSYAML.parseMap(original);
        String written = LSYAML.write(parsed);
        MapNode reparsed = LSYAML.parseMap(written);

        assertEquals(
                parsed.getMap("server").getString("host"),
                reparsed.getMap("server").getString("host")
        );
    }

    @Test
    void roundTripList() {
        String original = """
            - item1
            - item2
            - item3
            """;

        ListNode parsed = LSYAML.parseList(original);
        String written = LSYAML.write(parsed);
        ListNode reparsed = LSYAML.parseList(written);

        assertEquals(parsed.size(), reparsed.size());
        assertEquals(parsed.getString(0), reparsed.getString(0));
    }

    @Test
    void roundTripWithComments() {
        String original = """
            # Header comment
            name: value # inline
            """;

        MapNode parsed = LSYAML.parseMap(original);
        String written = LSYAML.write(parsed);

        assertTrue(written.contains("# Header comment"));
        assertTrue(written.contains("# inline"));
    }

    @Test
    void writeQuotedValues() {
        MapNode map = LSYAML.parseMap("key: 'quoted value'");
        String yaml = LSYAML.write(map);

        assertTrue(yaml.contains("'quoted value'"));
    }

    @Test
    void writeSpecialCharacters() {
        MapNode map = LSYAML.map()
                .put("colon", "has: colon")
                .put("hash", "has # hash")
                .build();

        String yaml = LSYAML.write(map);
        MapNode reparsed = LSYAML.parseMap(yaml);

        assertEquals("has: colon", reparsed.getString("colon"));
        assertEquals("has # hash", reparsed.getString("hash"));
    }
}
