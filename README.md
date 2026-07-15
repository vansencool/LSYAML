<div align="center">

# **LSYAML**
### *Lightning fast, format-preserving YAML parser for Java*

**A modern YAML library built for speed and flexibility**

Fast, readable, flexible, format-preserving.  
LSYAML offers lightning-fast parsing while retaining the original formatting of your YAML files.

</div>

---

## Features at a glance

- Very fast parsing (**up to ~35× faster than SnakeYAML** in [large-scale benchmarks](#benchmarks))
- **Full format preservation** - comments, empty lines, indentation retained
- **Lenient, fast, standard, and rich validation** modes, with Rust-style diagnostics (source spans, notes, suggested fixes)
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
    implementation 'net.vansencool:LSYAML:1.6.6'
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
    <version>1.6.6</version>
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
* **JVM:** 26 (GraalVM), run with `-XX:+UseCompactObjectHeaders`
* **OS:** Linux 6.8

`-XX:+UseCompactObjectHeaders` (JDK 24+) shrinks object headers and helps every parser; the throughput table below uses it for all four columns.

---

### Throughput (higher is better)

Parsing runs in one of four modes:

- **Lenient** (`ParseOptions.lenient()`) - no validation, maximum speed.
- **Fast validator** (`ParseOptions.strict(FastYamlValidator.newInstance())`) - checks the document is valid YAML 1.2 and reports where it is not, without the rich diagnostic machinery.
- **Standard validator** (`ParseOptions.defaults()`) - every validity error with full Rust-style diagnostics, leaving duplicate keys to the parser and skipping schema warnings.
- **Rich validator** (`ParseOptions.strict(RichYamlValidator.newInstance())`) - everything Standard reports plus duplicate keys and YAML 1.2 schema warnings such as `yes` as a boolean.

Hot throughput (JMH, steady state) across four workloads:

| Workload | Lenient      | Fast validator | Standard validator | Rich validator | SnakeYAML    |
| -------- | ------------ | -------------- | ------------------ | -------------- | ------------ |
| Simple   | 7452 ops/ms  | 6469 ops/ms    | 3716 ops/ms        | 3095 ops/ms    | 280 ops/ms   |
| Medium   | 613 ops/ms   | 468 ops/ms     | 462 ops/ms         | 385 ops/ms     | 40 ops/ms    |
| Complex  | 1.640 ops/ms | 1.051 ops/ms   | 1.009 ops/ms       | 0.967 ops/ms   | 0.105 ops/ms |
| Insane   | 0.132 ops/ms | 0.114 ops/ms   | 0.114 ops/ms       | 0.104 ops/ms   | 0.004 ops/ms |

Standard, the default, tracks the Fast validator closely while still giving full diagnostics, because the checks it drops are the ones the parser already covers. Rich costs more for the extra duplicate key tracking and schema warnings. Every mode runs many times faster than SnakeYAML.

The numbers above validate on the parsing thread. If you parse one large document at a time, `ParseOptions.builder().parallelValidation(true)` moves validation onto a second core so it overlaps the parse, reaching within a few percent of lenient past `DEFAULT_PARALLEL_THRESHOLD` (100 KB); below that it stays sequential, since the thread handoff costs more than the check itself.

*Simple: flat key/value. Medium: nested sections. Complex: 218 KB, ~9k lines. Insane: 1.4 MB stress test with anchors, flow collections, and block scalars.*

### Since the previous release

Recent versions completely rewrote the parser and did many optimizations. Compared to `1.2.5`:

Numbers are lenient throughput on both versions.

| Workload | 1.2.5        | Current      | Speedup     |
| -------- | ------------ | ------------ | ----------- |
| Simple   | 572 ops/ms   | 7452 ops/ms  | **~13×**    |
| Medium   | 46.3 ops/ms  | 613 ops/ms   | **~13×**    |
| Complex  | 0.129 ops/ms | 1.640 ops/ms | **~13×**    |
| Insane   | 0.024 ops/ms | 0.132 ops/ms | **~5.5×**   |

---

### Allocation (bytes per operation, lower is better)

| Workload | LSYAML Lenient     | SnakeYAML   | Reduction     |
| -------- | ---------- | ----------- | ------------- |
| Simple   | 1.7 KB/op  | 15.7 KB/op  | **~89% less** |
| Medium   | 18 KB/op   | 92 KB/op    | **~80% less** |
| Complex  | 5.7 MB/op  | 30 MB/op    | **~81% less** |
| Insane   | 77 MB/op   | 380 MB/op   | **~80% less** |

---

> LSYAML consistently allocates far less memory and scales better under heavy
> workloads, reaching up to **~35× higher throughput** than SnakeYAML while
> preserving comments, blank lines, and formatting that SnakeYAML discards.

Full benchmark source: https://github.com/vansencool/LSYAML-Benchmark

---

## License

MIT License - see LICENSE file for details.
