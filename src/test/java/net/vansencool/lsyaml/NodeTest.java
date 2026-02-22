package net.vansencool.lsyaml;

import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    @Test
    void mapNodePutAndGet() {
        MapNode map = new MapNode();
        map.put("key", "value");

        assertEquals("value", map.getString("key"));
        assertTrue(map.containsKey("key"));
        assertEquals(1, map.size());
    }

    @Test
    void mapNodeRemove() {
        MapNode map = new MapNode();
        map.put("key1", "value1");
        map.put("key2", "value2");

        map.remove("key1");

        assertFalse(map.containsKey("key1"));
        assertEquals(1, map.size());
    }

    @Test
    void mapNodeClear() {
        MapNode map = new MapNode();
        map.put("key1", "value1");
        map.put("key2", "value2");

        map.clear();

        assertTrue(map.isEmpty());
    }

    @Test
    void mapNodeCopy() {
        MapNode original = LSYAML.map()
                .put("name", "John")
                .comment(" Comment")
                .build();

        MapNode copy = (MapNode) original.copy();

        assertEquals(original.getString("name"), copy.getString("name"));
        original.put("name", "Jane");
        assertEquals("John", copy.getString("name"));
    }

    @Test
    void listNodeAddAndGet() {
        ListNode list = new ListNode();
        list.add("item1");
        list.add("item2");

        assertEquals("item1", list.getString(0));
        assertEquals("item2", list.getString(1));
        assertEquals(2, list.size());
    }

    @Test
    void listNodeInsert() {
        ListNode list = new ListNode();
        list.add("first");
        list.add("third");
        list.insert(1, new ScalarNode("second"));

        assertEquals("second", list.getString(1));
        assertEquals(3, list.size());
    }

    @Test
    void listNodeRemove() {
        ListNode list = new ListNode();
        list.add("item1");
        list.add("item2");
        list.add("item3");

        list.remove(1);

        assertEquals(2, list.size());
        assertEquals("item3", list.getString(1));
    }

    @Test
    void listNodeClear() {
        ListNode list = new ListNode();
        list.add("item1");
        list.add("item2");

        list.clear();

        assertTrue(list.isEmpty());
    }

    @Test
    void listNodeCopy() {
        ListNode original = LSYAML.list()
                .add("item1")
                .add("item2")
                .build();

        ListNode copy = (ListNode) original.copy();

        assertEquals(original.size(), copy.size());
        original.add("item3");
        assertEquals(2, copy.size());
    }

    @Test
    void scalarNodeTypes() {
        ScalarNode strNode = new ScalarNode("hello");
        ScalarNode intNode = new ScalarNode(42);
        ScalarNode boolNode = new ScalarNode(true);
        ScalarNode nullNode = new ScalarNode(null);

        assertEquals("hello", strNode.getStringValue());
        assertEquals(42, intNode.getIntValue());
        assertTrue(boolNode.getBooleanValue());
        assertTrue(nullNode.isNull());
    }

    @Test
    void scalarNodeConversions() {
        ScalarNode node = new ScalarNode("123");

        assertEquals(123, node.getIntValue());
        assertEquals(123L, node.getLongValue());
        assertEquals(123.0, node.getDoubleValue());
    }

    @Test
    void listNodeIteration() {
        ListNode list = new ListNode();
        list.add("a");
        list.add("b");
        list.add("c");

        int count = 0;
        for (var node : list) {
            count++;
            assertNotNull(node);
        }

        assertEquals(3, count);
    }

    @Test
    void mapNodeKeyOrder() {
        MapNode map = new MapNode();
        map.put("z", "last");
        map.put("a", "first");
        map.put("m", "middle");

        var keys = map.keys().toArray(new String[0]);

        assertEquals("z", keys[0]);
        assertEquals("a", keys[1]);
        assertEquals("m", keys[2]);
    }

    @Test
    void nodeChaining() {
        MapNode map = new MapNode();
        map.put("a", "1")
                .put("b", "2")
                .put("c", "3");

        assertEquals(3, map.size());
    }

    @Test
    void listChaining() {
        ListNode list = new ListNode();
        list.add("a")
                .add("b")
                .add("c");

        assertEquals(3, list.size());
    }

    @Test
    void mapEntryMetadata() {
        MapNode map = LSYAML.map()
                .entry("key")
                .comment(" Comment")
                .inlineComment(" Inline")
                .emptyLines(1)
                .value("value")
                .build();

        MapNode.MapEntry entry = map.getEntry("key");

        assertFalse(entry.getCommentsBefore().isEmpty());
        assertEquals(" Inline", entry.getInlineComment());
        assertEquals(1, entry.getEmptyLinesBefore());
    }

    @Test
    void listEntryMetadata() {
        ListNode list = new ListNode();
        ListNode.ListEntry entry = new ListNode.ListEntry(new ScalarNode("value"));
        entry.addCommentBefore(" Item comment");
        entry.setInlineComment(" Inline");
        entry.setEmptyLinesBefore(1);

        list.addEntry(entry);

        ListNode.ListEntry retrieved = list.getEntry(0);
        assertFalse(retrieved.getCommentsBefore().isEmpty());
        assertEquals(" Inline", retrieved.getInlineComment());
        assertEquals(1, retrieved.getEmptyLinesBefore());
    }
}
