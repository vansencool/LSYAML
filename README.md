<div align="center">

# **LSYAML**
### *Lightning fast, format-preserving YAML parser for Java*

**A modern YAML library built for speed and flexibility**
  
Fast, readable, flexible, format-preserving.  
Parses small configs instantly, scales to complex documents effortlessly.

</div>

---

## Features at a glance

- Very fast parsing (**~2x faster** than SnakeYAML)
- **Full format preservation** - comments, empty lines, indentation retained
- **Inline comments** supported (`key: value # comment`)
- **Strict and lenient** parsing modes with detailed error reporting
- **Runtime editing** of YAML nodes
- **Path-based access** - `node.getString("database.credentials.username")`
- Convenient type methods - `isMap()`, `asMap()`, `getString()`, `getInt()`, etc.
- Anchors and aliases support (`&anchor`, `*alias`)
- Flow and block style collections
- Multi-line strings support via `|` or `>`

---

<div align="center">

## **Installation**

### **Gradle**
</div>

```groovy
repositories {
    maven { url 'https://repository.vansencool.net' }
}

dependencies {
    implementation 'net.vansencool:LSYAML:1.0.0'
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
    <version>1.0.0</version>
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
String name = node.getString("app.name");       // "MyApp"
String version = node.getString("app.version"); // "1.0.0"
Boolean debug = node.getBoolean("app.debug");   // true

// Or navigate manually
MapNode app = node.get("app").asMap();
```

### Parsing to Map

```java
MapNode config = LSYAML.parseMap(yaml);

for (String key : config.keys()) {
    System.out.println(key + ": " + config.get(key));
}
```

### Modifying Values

```java
MapNode config = LSYAML.parseMap(yaml);

// Navigate to nested map and modify
MapNode pool = config.get("database").asMap().get("pool").asMap();
pool.put("max", new ScalarNode("50"));

// Write back - comments and formatting preserved!
String output = config.toYaml();
```

---

## Strict Mode

Enable strict parsing for detailed error reporting.

```java
ParseResult result = LSYAML.parseDetailed(yaml, ParseOptions.strict());

if (!result.isSuccess()) {
    for (ParseIssue issue : result.getIssues()) {
        System.out.println(issue.getFormattedMessage());
    }
}
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

