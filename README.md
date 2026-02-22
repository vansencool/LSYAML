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

- Very fast parsing (**~2x faster** than SnakeYAML)
- **Full format preservation** - comments, empty lines, indentation retained
- **Strict and lenient** parsing modes with detailed error reporting
- **Runtime editing** of YAML nodes
- Anchors and aliases support (`&anchor`, `*alias`)
- Flow and block style collections
- Multi-line strings support via `|` or `>`

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
    implementation 'net.vansencool:LSYAML:1.2.0'
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
    <version>1.2.0</version>
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

YamlNode node = LSYAML.parse(yaml);

// Easy path-based access
String name = node.getString("app.name"); // "MyApp"
String version = node.getString("app.version"); // "1.0.0"
Boolean debug = node.getBoolean("app.debug"); // true

// Or navigate manually
MapNode app = node.get("app").asMap();
```

### Modifying Values

```java
MapNode config = LSYAML.parseMap(yaml);

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

Generated YAML (if not found):

```yaml
name: MyServer
port: 25565
debug: false
# List of enabled features
features:
  - auth
  - logging
database:
  host: localhost
  port: 3306
  credentials:
    user: admin
    password: secret
```

### Annotations

| Annotation | Purpose |
|------------|---------|
| `@ConfigFile("path.yml")` | Binds the class to a YAML file |
| `@Key("custom_name")` | Overrides the config key (converted to lowercase) |
| `@ExplicitKey("exactKey")` | Uses exact key name (no case conversion) |
| `@Comment("text")` | Adds a comment above the field |
| `@Space(before=1)` | Adds blank lines for readability |
| `@Ignore` | Excludes a field from the config |

### Custom Type Adapters

For complex types like Duration, Location, etc:

```java
ConfigLoader.registerAdapter(Duration.class, new ConfigAdapter<Duration>() {
    @Override
    public Duration fromNode(YamlNode node) {
        return Duration.ofMinutes(node.getLong());
    }
    
    @Override
    public YamlNode toNode(Duration value) {
        return new ScalarNode(value.toMinutes());
    }
});
```

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

## License

MIT License - see LICENSE file for details.