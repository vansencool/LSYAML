package net.vansencool.lsyaml;

import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Written by AI.
 */
@SuppressWarnings("all")
class ModifyApiTest {

    @Test
    void modifyEntryValue() {
        MapNode map = new MapNode();
        map.put("key", "oldValue");
        
        map.modify("key").value("newValue");
        
        assertEquals("newValue", map.getString("key"));
    }

    @Test
    void modifyEntryAddComment() {
        MapNode map = new MapNode();
        map.put("key", "value");
        
        map.modify("key")
            .commentBefore(" This is a comment")
            .inlineComment(" inline");
        
        String yaml = map.toYaml();
        assertTrue(yaml.contains("# This is a comment"));
        assertTrue(yaml.contains("# inline"));
    }

    @Test
    void modifyEntryMultipleComments() {
        MapNode map = new MapNode();
        map.put("key", "value");
        
        map.modify("key")
            .commentsBefore(" Comment 1", " Comment 2", " Comment 3");
        
        String yaml = map.toYaml();
        assertTrue(yaml.contains("# Comment 1"));
        assertTrue(yaml.contains("# Comment 2"));
        assertTrue(yaml.contains("# Comment 3"));
    }

    @Test
    void modifyEntrySetCommentsClearsExisting() {
        MapNode map = new MapNode();
        map.put("key", "value");
        map.modify("key").commentBefore(" Old comment");
        
        map.modify("key").setCommentsBefore(" New comment");
        
        String yaml = map.toYaml();
        assertFalse(yaml.contains("Old comment"));
        assertTrue(yaml.contains("# New comment"));
    }

    @Test
    void modifyEntryEmptyLinesBefore() {
        MapNode map = new MapNode();
        map.put("key1", "value1");
        map.put("key2", "value2");
        
        map.modify("key2").emptyLinesBefore(2);
        
        String yaml = map.toYaml();
        assertTrue(yaml.contains("value1\n\n\nkey2:"));
    }

    @Test
    void modifyCreatesEntryIfNotExists() {
        MapNode map = new MapNode();
        
        map.modify("newKey").value("newValue").commentBefore(" New entry");
        
        assertTrue(map.containsKey("newKey"));
        assertEquals("newValue", map.getString("newKey"));
    }

    @Test
    void modifyChainedOperations() {
        MapNode map = new MapNode();
        
        map.modify("server")
            .commentBefore(" Server configuration")
            .value("localhost")
            .inlineComment(" main server")
            .emptyLinesBefore(1);
        
        assertTrue(map.containsKey("server"));
        assertEquals("localhost", map.getString("server"));
        assertEquals(" Server configuration", map.modify("server").getCommentsBefore().get(0));
        assertEquals(" main server", map.modify("server").getInlineComment());
    }

    @Test
    void modifyDoneReturnsMap() {
        MapNode map = new MapNode();
        
        MapNode result = map.modify("key").value("value").done();
        
        assertSame(map, result);
    }

    @Test
    void insertBefore() {
        MapNode map = new MapNode();
        map.put("first", "1");
        map.put("third", "3");
        
        map.insertBefore("second", new ScalarNode("2"), "third");
        
        String yaml = map.toYaml();
        int firstIdx = yaml.indexOf("first:");
        int secondIdx = yaml.indexOf("second:");
        int thirdIdx = yaml.indexOf("third:");
        assertTrue(firstIdx < secondIdx);
        assertTrue(secondIdx < thirdIdx);
    }

    @Test
    void insertAfter() {
        MapNode map = new MapNode();
        map.put("first", "1");
        map.put("third", "3");
        
        map.insertAfter("second", new ScalarNode("2"), "first");
        
        String yaml = map.toYaml();
        int firstIdx = yaml.indexOf("first:");
        int secondIdx = yaml.indexOf("second:");
        int thirdIdx = yaml.indexOf("third:");
        assertTrue(firstIdx < secondIdx);
        assertTrue(secondIdx < thirdIdx);
    }

    @Test
    void renameKey() {
        MapNode map = new MapNode();
        map.put("oldName", "value");
        map.modify("oldName").commentBefore(" A comment");
        
        map.renameKey("oldName", "newName");
        
        assertFalse(map.containsKey("oldName"));
        assertTrue(map.containsKey("newName"));
        assertEquals("value", map.getString("newName"));
        assertEquals(1, map.modify("newName").getCommentsBefore().size());
    }

    @Test
    void mapTrailingComment() {
        MapNode map = new MapNode();
        map.put("key", "value");
        map.addTrailingComment(" End of section");
        
        String yaml = map.toYaml();
        assertTrue(yaml.endsWith("# End of section"));
    }

    @Test
    void mapSetTrailingComments() {
        MapNode map = new MapNode();
        map.put("key", "value");
        map.addTrailingComment(" Old comment");
        
        map.setTrailingComments(" New comment 1", " New comment 2");
        
        String yaml = map.toYaml();
        assertFalse(yaml.contains("Old comment"));
        assertTrue(yaml.contains("# New comment 1"));
        assertTrue(yaml.contains("# New comment 2"));
    }

    @Test
    void listModifyEntry() {
        ListNode list = new ListNode();
        list.add("item1");
        list.add("item2");
        
        list.modify(0).value("modifiedItem1").inlineComment(" first item");
        
        assertEquals("modifiedItem1", list.getString(0));
        String yaml = list.toYaml();
        assertTrue(yaml.contains("# first item"));
    }

    @Test
    void listModifyComments() {
        ListNode list = new ListNode();
        list.add("item1");
        list.add("item2");
        
        list.modify(1)
            .commentBefore(" Comment for second item")
            .emptyLinesBefore(1);
        
        String yaml = list.toYaml();
        assertTrue(yaml.contains("# Comment for second item"));
    }

    @Test
    void listAddWithComment() {
        ListNode list = new ListNode();
        list.addWithComment("item1", " First item comment");
        
        String yaml = list.toYaml();
        assertTrue(yaml.contains("# First item comment"));
        assertTrue(yaml.contains("- item1"));
    }

    @Test
    void listTrailingComment() {
        ListNode list = new ListNode();
        list.add("item1");
        list.addTrailingComment(" End of list");
        
        String yaml = list.toYaml();
        assertTrue(yaml.endsWith("# End of list"));
    }

    @Test
    void modifyEntryRemove() {
        MapNode map = new MapNode();
        map.put("key1", "value1");
        map.put("key2", "value2");
        
        map.modify("key1").remove();
        
        assertFalse(map.containsKey("key1"));
        assertTrue(map.containsKey("key2"));
    }

    @Test
    void modifyEntryExists() {
        MapNode map = new MapNode();
        map.put("existing", "value");
        
        assertTrue(map.modify("existing").exists());
        assertFalse(map.modify("nonexistent").exists());
    }

    @Test
    void listModifyDoneReturnsList() {
        ListNode list = new ListNode();
        list.add("item");
        
        ListNode result = list.modify(0).value("newItem").done();
        
        assertSame(list, result);
    }

    @Test
    void complexModificationScenario() {
        MapNode config = new MapNode();
        
        config.modify("server")
            .commentBefore(" Server configuration")
            .commentBefore(" This section defines server settings")
            .value(new MapNode())
            .emptyLinesBefore(0);
        
        MapNode serverMap = config.getMap("server");
        serverMap.modify("host")
            .value("localhost")
            .inlineComment(" primary host");
        serverMap.modify("port")
            .value(8080)
            .inlineComment(" default port");
        serverMap.addTrailingComment(" End of server configuration");
        
        config.modify("database")
            .commentBefore(" Database configuration")
            .value(new MapNode())
            .emptyLinesBefore(1);
        
        config.insertBefore("logging", new MapNode(), "database");
        config.modify("logging")
            .commentBefore(" Logging configuration");
        
        String yaml = config.toYaml();
        
        assertTrue(yaml.contains("# Server configuration"));
        assertTrue(yaml.contains("# This section defines server settings"));
        assertTrue(yaml.contains("host: localhost # primary host"));
        assertTrue(yaml.contains("port: 8080 # default port"));
        assertTrue(yaml.contains("# End of server configuration"));
        assertTrue(yaml.contains("# Database configuration"));
        assertTrue(yaml.contains("# Logging configuration"));
        
        int serverIdx = yaml.indexOf("server:");
        int loggingIdx = yaml.indexOf("logging:");
        int databaseIdx = yaml.indexOf("database:");
        assertTrue(serverIdx < loggingIdx);
        assertTrue(loggingIdx < databaseIdx);
    }

    @Test
    void modifyPreservesExistingComments() {
        String yaml = """
            # Header comment
            key: value # inline
            """;
        
        MapNode map = LSYAML.parse(yaml);
        map.modify("key").value("newValue");
        
        String output = map.toYaml();
        assertTrue(output.contains("# Header comment"));
        assertTrue(output.contains("# inline"));
        assertTrue(output.contains("newValue"));
    }

    @Test
    void clearCommentsWorks() {
        MapNode map = new MapNode();
        map.modify("key")
            .commentsBefore(" Comment 1", " Comment 2")
            .inlineComment(" Inline")
            .value("value");
        
        map.modify("key")
            .clearCommentsBefore()
            .clearInlineComment();
        
        String yaml = map.toYaml();
        assertFalse(yaml.contains("Comment"));
        assertFalse(yaml.contains("Inline"));
    }

    @Test
    void renameKeyPreservesPosition() {
        MapNode map = new MapNode();
        map.put("first", "1");
        map.put("middle", "2");
        map.put("last", "3");
        
        map.renameKey("middle", "center");
        
        String yaml = map.toYaml();
        int firstIdx = yaml.indexOf("first:");
        int centerIdx = yaml.indexOf("center:");
        int lastIdx = yaml.indexOf("last:");
        assertTrue(firstIdx < centerIdx);
        assertTrue(centerIdx < lastIdx);
    }
}
