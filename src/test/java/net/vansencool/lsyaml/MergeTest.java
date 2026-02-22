package net.vansencool.lsyaml;

import net.vansencool.lsyaml.node.MapNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MergeTest {

    @Test
    void mergeSimpleMaps() {
        MapNode base = LSYAML.map()
                .put("a", 1)
                .put("b", 2)
                .build();

        MapNode override = LSYAML.map()
                .put("b", 20)
                .put("c", 3)
                .build();

        MapNode merged = LSYAML.merge(base, override);

        assertEquals(1, merged.getInt("a", 0));
        assertEquals(20, merged.getInt("b", 0));
        assertEquals(3, merged.getInt("c", 0));
    }

    @Test
    void mergeNestedMaps() {
        MapNode base = LSYAML.map()
                .put("config", LSYAML.map()
                        .put("host", "localhost")
                        .put("port", 8080))
                .build();

        MapNode override = LSYAML.map()
                .put("config", LSYAML.map()
                        .put("port", 9090))
                .build();

        MapNode merged = LSYAML.merge(base, override);

        MapNode config = merged.getMap("config");
        assertEquals("localhost", config.getString("host"));
        assertEquals(9090, config.getInt("port", 0));
    }

    @Test
    void mergeDoesNotModifyOriginal() {
        MapNode base = LSYAML.map()
                .put("key", "original")
                .build();

        MapNode override = LSYAML.map()
                .put("key", "override")
                .build();

        LSYAML.merge(base, override);

        assertEquals("original", base.getString("key"));
    }

    @Test
    void mergeEmptyOverride() {
        MapNode base = LSYAML.map()
                .put("a", 1)
                .put("b", 2)
                .build();

        MapNode override = LSYAML.emptyMap();

        MapNode merged = LSYAML.merge(base, override);

        assertEquals(1, merged.getInt("a", 0));
        assertEquals(2, merged.getInt("b", 0));
    }

    @Test
    void mergeEmptyBase() {
        MapNode base = LSYAML.emptyMap();

        MapNode override = LSYAML.map()
                .put("a", 1)
                .put("b", 2)
                .build();

        MapNode merged = LSYAML.merge(base, override);

        assertEquals(1, merged.getInt("a", 0));
        assertEquals(2, merged.getInt("b", 0));
    }

    @Test
    void mergeReplacesNonMapValues() {
        MapNode base = LSYAML.map()
                .put("key", "string")
                .build();

        MapNode override = LSYAML.map()
                .put("key", LSYAML.map()
                        .put("nested", "value"))
                .build();

        MapNode merged = LSYAML.merge(base, override);

        MapNode nested = merged.getMap("key");
        assertNotNull(nested);
        assertEquals("value", nested.getString("nested"));
    }
}
