# Titanium RDF N-QUADS

RDF N-Quads reader and writer optimized for efficient parsing, serialization, and handling of large RDF datasets. It enables scalable processing of RDF statements in a memory-efficient, streaming fashion, supporting RDF 1.2 `dirLangString` alongside the `i18n` datatype.

[![Java 21 CI](https://github.com/filip26/titanium-rdf-n-quads/actions/workflows/java21-build.yml/badge.svg)](https://github.com/filip26/titanium-rdf-n-quads/actions/workflows/java21-build.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/9b9b6b24ae1e468f93428c3d70fd59f2)](https://app.codacy.com/gh/filip26/titanium-rdf-n-quads/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/9b9b6b24ae1e468f93428c3d70fd59f2)](https://app.codacy.com/gh/filip26/titanium-rdf-n-quads/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)
[![Maven Central](https://img.shields.io/maven-central/v/com.apicatalog/titanium-rdf-n-quads.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.apicatalog%20AND%20a:titanium-rdf-n-quads)
[![javadoc](https://javadoc.io/badge2/com.apicatalog/titanium-rdf-n-quads/javadoc.svg)](https://javadoc.io/doc/com.apicatalog/titanium-rdf-n-quads)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Formerly part of [Titanium JSON-LD](https://github.com/filip26/titanium-json-ld)

## Example

```javascript

// Reading N-Quads
try (var reader = new NQuadsReader(input)) {
    reader.provide((...) -> {
      // N-Quad processing
    );
}

// Writing N-Quads
try (var writer = new NQuadsWriter(output)) {
  writer.quad(...);
  writer.quad(...);
  writer.quad(...);
}
  

// Demonstrating RdfQuadConsumer usage by reading and writing in one step
// Since NQuadsWriter implements the RdfQuadConsumer interface,
// it can be directly used as a consumer, e.g. with JsonLd.toRdf.
try (var reader = new NQuadsReader(input);
     var writer = new NQuadsWriter(output)) {
    reader.provide(writer);
}

// Static access to NQuadsWriter methods
var encoded = NQuadsWriter.nquad(...);
var encodedLiteral = NQuadsWriter.literal(...);

```

## 📦 Installation

```xml
<dependency>
    <groupId>com.apicatalog</groupId>
    <artifactId>titanium-rdf-n-quads</artifactId>
    <version>${nquads.version}</version>
</dependency>
```

## 🤝 Contributing

Contributions of all kinds are welcome - whether it’s code, documentation, testing, or community support! Please open PR or issue to get started.

## 📚 Resources

* [W3C RDF 1.1 N-Quads](https://www.w3.org/TR/n-quads/)
* [W3C RDF 1.2 N-Quads](https://www.w3.org/TR/rdf12-n-quads/)

## 💼 Commercial Support

Commercial support and consulting are available.
For inquiries, please contact: <filip26@gmail.com>
