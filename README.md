<div align="center">

# **LSYAML**
### *Lightning fast, format-preserving YAML parser for Java*

**A modern YAML library built for speed and flexibility**

Fast, readable, flexible, format-preserving.  
LSYAML offers lightning-fast parsing while retaining the original formatting of your YAML files.

<details>
  <summary><strong>Project Status</strong></summary>
  <br/>
  LSYAML is under continuous development, progressing toward a stable 2.0.0 release.
  Core functionality is stable, with incremental API refinements planned.

  Documentation is currently in progress.
</details>
</div>

---

## Features at a glance

- Very fast parsing (**up to ~9× faster than SnakeYAML** in [large-scale benchmarks](#benchmarks))
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
    implementation 'net.vansencool:LSYAML:1.3.5'
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
    <version>1.3.5</version>
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
* **RAM:** 32 GB DDR5 (5200 MT/s)
* **JVM:** 25.0.2 (Oracle)
* **OS:** Linux 6.8

---

### Throughput (higher is better)

### Standard Workloads

| Workload | LSYAML (lenient) | LSYAML (strict) | SnakeYAML   | Speedup (lenient) |
| -------- | ---------------- | --------------- | ----------- |-------------------|
| Simple   | 589 ops/ms       | 526 ops/ms      | 293 ops/ms  | **~2.0×**         |
| Medium   | 47.6 ops/ms      | 44.3 ops/ms     | 44.3 ops/ms | ~1.07×            |
| Complex  | 0.138 ops/ms      | 0.127 ops/ms     | 0.100 ops/ms | **~1.38×**        |

### Large / Insane Workload

(723k characters, 135k+ lines YAML)

| Mode    | LSYAML       | SnakeYAML    | Speedup        |
| ------- | ------------ | ------------ | -------------- |
| Lenient | 0.027 ops/ms | 0.003 ops/ms | **~9× faster** |
| Strict  | 0.025 ops/ms | 0.003 ops/ms | **~8× faster** |

---

### Allocation (bytes per operation, lower is better)

#### Standard Workloads

| Workload | LSYAML      | SnakeYAML    | Reduction     |
| -------- | ----------- | ------------ | ------------- |
| Simple   | 3,928 B/op  | 15,672 B/op  | **~75% less** |
| Medium   | 45,072 B/op | 92,264 B/op  | **~51% less** |
| Complex  | 68,952 B/op | 180,680 B/op | **~62% less** |

#### Large / Insane Workload

| Workload | LSYAML    | SnakeYAML | Reduction     |
| -------- | --------- | --------- | ------------- |
| Insane   | 132 MB/op | 384 MB/op | **~65% less** |

---

> LSYAML consistently allocates significantly less memory and scales better under heavy workloads, achieving up to **~9× higher throughput** in extreme scenarios.

Full benchmark source: https://github.com/vansencool/LSYAML-Benchmark

---

## License

MIT License - see LICENSE file for details.