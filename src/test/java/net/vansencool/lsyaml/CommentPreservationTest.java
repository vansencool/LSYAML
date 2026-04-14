package net.vansencool.lsyaml;

import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Written by AI.
 */
@SuppressWarnings("all")
class CommentPreservationTest {

    @Test
    void preserveCommentsInNestedMaps() {
        String yaml = """
            # Root comment
            root:
              # Child comment
              child:
                # Grandchild comment
                value: test
            """;
        MapNode map = LSYAML.parse(yaml);
        String output = LSYAML.write(map);

        assertTrue(output.contains("# Root comment"));
        assertTrue(output.contains("# Child comment"));
        assertTrue(output.contains("# Grandchild comment"));

        MapNode root = map.getMap("root");
        assertNotNull(root);
        MapNode child = root.getMap("child");
        assertNotNull(child);
        assertEquals("test", child.getString("value"));
    }

    @Test
    void preserveCommentsInListItems() {
        String yaml = """
            items:
              # First
              - one
              # Second
              - two
              # Third
              - three
            """;
        MapNode map = LSYAML.parse(yaml);
        String output = LSYAML.write(map);

        assertTrue(output.contains("# First"));
        assertTrue(output.contains("# Second"));
        assertTrue(output.contains("# Third"));

        ListNode items = map.getList("items");
        assertEquals(3, items.size());
    }

    @Test
    void preserveCommentsInListOfMaps() {
        String yaml = """
            servers:
              # Production server
              - name: prod
                host: prod.example.com
              # Staging server  
              - name: staging
                host: staging.example.com
            """;
        MapNode map = LSYAML.parse(yaml);
        String output = LSYAML.write(map);

        assertTrue(output.contains("# Production server"));
        assertTrue(output.contains("# Staging server"));

        ListNode servers = map.getList("servers");
        assertEquals(2, servers.size());
        assertEquals("prod", servers.getMap(0).getString("name"));
        assertEquals("staging", servers.getMap(1).getString("name"));
    }

    @Test
    void preserveInlineComments() {
        String yaml = """
            config:
              host: localhost # main server
              port: 8080 # default port
              debug: true # enable for testing
            """;
        MapNode map = LSYAML.parse(yaml);
        String output = LSYAML.write(map);

        assertTrue(output.contains("# main server"));
        assertTrue(output.contains("# default port"));
        assertTrue(output.contains("# enable for testing"));
    }

    @Test
    void preserveEmptyLines() {
        String yaml = """
            section1: value1

            section2: value2

            section3: value3
            """;
        MapNode map = LSYAML.parse(yaml);
        String output = LSYAML.write(map);

        MapNode.MapEntry entry2 = map.getEntry("section2");
        MapNode.MapEntry entry3 = map.getEntry("section3");

        assertTrue(entry2.getEmptyLinesBefore() > 0);
        assertTrue(entry3.getEmptyLinesBefore() > 0);
    }

    @Test
    void preserveMixedCommentsAndEmptyLines() {
        String yaml = """
            key1: value1

            # Comment after empty line
            key2: value2

            # Another comment
            # With multiple lines
            key3: value3
            """;
        MapNode map = LSYAML.parse(yaml);
        String output = LSYAML.write(map);

        assertTrue(output.contains("# Comment after empty line"));
        assertTrue(output.contains("# Another comment"));
        assertTrue(output.contains("# With multiple lines"));
    }

    @Test
    void roundTripComplexStructure() {
        String yaml = """
            # Application configuration
            app:
              # General settings
              name: MyApp
              version: 1.0.0
              
              # Database configuration
              database:
                # Main database
                host: localhost
                port: 5432 # PostgreSQL default
                
              # List of modules
              modules:
                # Core module
                - name: core
                  enabled: true
                # Auth module
                - name: auth
                  enabled: true
            """;
        MapNode parsed = LSYAML.parse(yaml);
        String written = LSYAML.write(parsed);
        MapNode reparsed = LSYAML.parse(written);

        MapNode app = reparsed.getMap("app");
        assertNotNull(app);
        assertEquals("MyApp", app.getString("name"));

        MapNode db = app.getMap("database");
        assertNotNull(db);
        assertEquals("localhost", db.getString("host"));

        ListNode modules = app.getList("modules");
        assertNotNull(modules);
        assertEquals(2, modules.size());

        assertTrue(written.contains("# Application configuration"));
        assertTrue(written.contains("# Database configuration"));
        assertTrue(written.contains("# PostgreSQL default"));
    }

    @Test
    void nestedListsWithComments() {
        String yaml = """
            matrix:
              # First row
              - - 1
                - 2
                - 3
              # Second row
              - - 4
                - 5
                - 6
            """;
        MapNode map = LSYAML.parse(yaml);

        ListNode matrix = map.getList("matrix");
        assertNotNull(matrix);
        assertEquals(2, matrix.size());
    }

    @Test
    void commentsOnlyYaml() {
        String yaml = """
            # Just comments
            # No actual content
            """;
        MapNode map = LSYAML.parse(yaml);
        assertTrue(map.isEmpty());
    }

    @Test
    void preserveCommentSpacing() {
        String yaml = """
            # Comment with no space after hash
            #Another without space
            key: value
            """;
        MapNode map = LSYAML.parse(yaml);

        MapNode.MapEntry entry = map.getEntry("key");
        assertEquals(2, entry.getCommentsBefore().size());
    }

    @Test
    void deeplyNestedWithComments() {
        String yaml = """
            # Level 0
            l0:
              # Level 1
              l1:
                # Level 2
                l2:
                  # Level 3
                  l3:
                    # Level 4
                    value: deep
            """;
        MapNode map = LSYAML.parse(yaml);

        MapNode l0 = map.getMap("l0");
        MapNode l1 = l0.getMap("l1");
        MapNode l2 = l1.getMap("l2");
        MapNode l3 = l2.getMap("l3");

        assertEquals("deep", l3.getString("value"));

        String output = LSYAML.write(map);
        assertTrue(output.contains("# Level 0"));
        assertTrue(output.contains("# Level 1"));
        assertTrue(output.contains("# Level 2"));
        assertTrue(output.contains("# Level 3"));
        assertTrue(output.contains("# Level 4"));
    }

    @Test
    void modifyAndPreserveComments() {
        String yaml = """
            # Config header
            database:
              # Connection
              host: localhost
              port: 3306
            """;
        MapNode map = LSYAML.parse(yaml);

        map.getMap("database").put("host", "127.0.0.1");
        map.getMap("database").put("timeout", 30);

        String output = LSYAML.write(map);

        assertTrue(output.contains("# Config header"));
        assertTrue(output.contains("# Connection"));
        assertTrue(output.contains("host: 127.0.0.1"));
        assertTrue(output.contains("timeout: 30"));
    }

    @Test
    void addCommentsToExistingNode() {
        MapNode map = LSYAML.parse("key: value");

        MapNode.MapEntry entry = map.getEntry("key");
        entry.addCommentBefore(" Added comment");
        entry.setInlineComment(" Inline added");

        String output = LSYAML.write(map);

        assertTrue(output.contains("# Added comment"));
        assertTrue(output.contains("# Inline added"));
    }
}
