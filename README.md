<div align="center">

# **LSYAML**
### *Lightning fast, format-preserving YAML parser for Java*

**A modern YAML library built for speed and flexibility**

Fast, readable, flexible, format-preserving.  
LSYAML offers lightning-fast parsing while retaining the original formatting of your YAML files.

</div>

---

## Features at a glance

- Very fast parsing (**up to ~30× faster than SnakeYAML** in [large-scale benchmarks](#benchmarks))
- **Full format preservation** - comments, empty lines, indentation retained
- **Strict and lenient** parsing modes with detailed error reporting
- **Runtime editing** of YAML nodes
- Anchors and aliases support (`&anchor`, `*alias`)
- Flow and block style collections
- Multi-line strings support via `|` or `>`
- Complex key support (maps as keys)
- Config binding API for easy YAML-to-Java mapping

---

<div align="center">

## **Installation**

Check out the [Repository](https://repo.vansencool.net/artifact/net.vansencool/LSYAML) for previous versions and snapshots.

### **Gradle**
</div>

```groovy
repositories {
    maven { url 'https://repository.vansencool.net' }
}

dependencies {
    implementation 'net.vansencool:LSYAML:1.5.8'
}
```

<div align="center">

### **Maven**

</div>

```xml
<repository>
    <id>vansencool</id>
    <url>https://repository.vansencool.net</url>
</repository>

<dependency>
    <groupId>net.vansencool</groupId>
    <artifactId>LSYAML</artifactId>
    <version>1.5.8</version>
</dependency>
```

---

## Quick Start

### Parsing YAML

```java
String yaml = """
    app:
      name: MyApp
      version: 1.0.0
      debug: true
    """;

MapNode node = LSYAML.parse(yaml);

// Easy path-based access
String name = node.getString("app.name"); // "MyApp"
String version = node.getString("app.version"); // "1.0.0"
Boolean debug = node.getBoolean("app.debug"); // true

// Or navigate manually
MapNode app = node.getMap("app");
```

### Modifying Values

```java
MapNode config = LSYAML.parse(yaml);

// Navigate to nested map and modify directly (put returns the node for chaining)
config.getMap("database").getMap("pool")
    .put("min", 5)
    .put("max", 100);

// Comments and formatting preserved!
String output = config.toYaml();
```

---

## Config Binding API

Bind YAML files directly to Java static fields - the easiest way to manage configuration.

Uppercase field names are automatically converted to lowercase YAML keys.

```java
@ConfigFile("config.yml")
public class MyConfig {
    public static String NAME = "MyServer";
    public static int PORT = 25565;
    public static boolean DEBUG = false;
    
    @Comment("List of enabled features")
    public static List<String> FEATURES = List.of("auth", "logging");
    
    public static Database DATABASE = new Database();
    
    public static class Database {
        public String HOST = "localhost";
        public int PORT = 3306;
        
        public Credentials CREDENTIALS = new Credentials();
        
        public static class Credentials {
            public String USER = "admin";
            public String PASSWORD = "secret";
        }
    }
}
```

Load it with a single line:

```java
ConfigLoader.load(MyConfig.class);
System.out.println(MyConfig.NAME);
System.out.println(MyConfig.DATABASE.HOST);
System.out.println(MyConfig.DATABASE.CREDENTIALS.USER);
// Reload later if needed:
ConfigLoader.reload(MyConfig.class);
// Or, to reload all configs:
ConfigLoader.reload();
```

It will also automatically generate the full YAML file for you (if it doesn't exist.)

---

## Key Difference vs SnakeYAML

LSYAML **preserves everything** when parsing:

- Comments (standalone and inline)
- Empty lines
- Original indentation
- Key quoting style
- Collection style (flow vs block)

SnakeYAML **does not preserve formatting** - writing back produces reformatted output,
losing original comments, spacing, and document structure.

> LSYAML keeps your YAML files looking exactly as before.

---

## Benchmarks

Benchmarks performed using JMH on:

* **CPU:** AMD Ryzen 9 9900X3D
* **RAM:** 32 GB DDR5 (5200 MT/s, one ram stick)
* **JVM:** 25.0.2 (Oracle)
* **OS:** Linux 6.8

---

### Throughput (higher is better)

Hot throughput (JMH, steady state) across four workloads, LSYAML lenient vs SnakeYAML:

| Workload | LSYAML (lenient) | SnakeYAML    | Speedup         |
| -------- | ---------------- | ------------ | --------------- |
| Simple   | 7867 ops/ms      | 280 ops/ms   | **~28× faster** |
| Medium   | 520 ops/ms       | 39 ops/ms    | **~13× faster** |
| Complex  | 1.272 ops/ms     | 0.099 ops/ms | **~13× faster** |
| Insane   | 0.089 ops/ms     | 0.003 ops/ms | **~30× faster** |

*Simple: flat key/value. Medium: nested sections. Complex: 218 KB, ~9k lines. Insane: 1.4 MB stress test with anchors, flow collections, and block scalars.*

### Since the previous release

Recent versions rewrote the parser onto an offset engine and eliminated the
biggest allocation and control-flow costs. Compared to `1.2.5`:

| Workload | 1.2.5        | Current      | Speedup     |
| -------- | ------------ | ------------ | ----------- |
| Simple   | 572 ops/ms   | 7867 ops/ms  | **~13×**    |
| Medium   | 46.3 ops/ms  | 520 ops/ms   | **~11×**    |
| Complex  | 0.129 ops/ms | 1.272 ops/ms | **~10×**    |
| Insane   | 0.024 ops/ms | 0.089 ops/ms | **~3.7×**   |

---

### Allocation (bytes per operation, lower is better)

| Workload | LSYAML     | SnakeYAML   | Reduction     |
| -------- | ---------- | ----------- | ------------- |
| Simple   | 1.7 KB/op  | 15.7 KB/op  | **~89% less** |
| Medium   | 18 KB/op   | 92 KB/op    | **~80% less** |
| Complex  | 5.7 MB/op  | 30 MB/op    | **~81% less** |
| Insane   | 77 MB/op   | 380 MB/op   | **~80% less** |

---

> LSYAML consistently allocates far less memory and scales better under heavy
> workloads, achieving over **~28× higher throughput** than SnakeYAML while
> preserving comments, blank lines, and formatting that SnakeYAML discards.

Full benchmark source: https://github.com/vansencool/LSYAML-Benchmark

---

## License

MIT License - see LICENSE file for details.
