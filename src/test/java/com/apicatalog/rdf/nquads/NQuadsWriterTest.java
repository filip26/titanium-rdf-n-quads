package com.apicatalog.rdf.nquads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

class NQuadsWriterTest {

    @Test
    void testI18NDirection() throws IOException {
        var output = new StringWriter();
        try (var writer = new NQuadsWriter(output)) {
            writer.quad(
                    "_:s", "_:p",
                    "a", "https://www.w3.org/ns/i18n#", "cs", "ltr",
                    "_:g");
        }
        assertEquals("_:s _:p \"a\"^^<https://www.w3.org/ns/i18n#cs_ltr> _:g .\n", output.toString());
    }

    @Test
    void testI18DirectionWithoutLang() throws IOException {
        var output = new StringWriter();
        try (var writer = new NQuadsWriter(output)) {
            writer.quad(
                    "_:s", "_:p",
                    "a", "https://www.w3.org/ns/i18n#", null, "ltr",
                    "_:g");
        }
        assertEquals("_:s _:p \"a\"^^<https://www.w3.org/ns/i18n#_ltr> _:g .\n", output.toString());
    }

    @Test
    void testLangTag() throws IOException {
        var output = new StringWriter();
        try (var writer = new NQuadsWriter(output)) {
            writer.quad(
                    "_:s", "_:p",
                    "a", "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString", "cs", null,
                    "_:g");
        }
        assertEquals("_:s _:p \"a\"@cs _:g .\n", output.toString());
    }

    @Test
    void testDirLangTag() throws IOException {
        var output = new StringWriter();
        try (var writer = new NQuadsWriter(output)) {
            writer.quad(
                    "_:s", "_:p",
                    "a", "http://www.w3.org/1999/02/22-rdf-syntax-ns#dirLangString", "cs", "ltr",
                    "_:g");
        }
        assertEquals("_:s _:p \"a\"@cs--ltr _:g .\n", output.toString());
    }

    @Test
    void testDirLangTagWithoutDirection() throws IOException {
        var output = new StringWriter();
        try (var writer = new NQuadsWriter(output)) {
            writer.quad(
                    "_:s", "_:p",
                    "a", "http://www.w3.org/1999/02/22-rdf-syntax-ns#dirLangString", "cs", null,
                    "_:g");
        }
        assertEquals("_:s _:p \"a\"@cs _:g .\n", output.toString());
    }

    @Test
    void testComplexDirLangTag() throws IOException {
        var output = new StringWriter();
        try (var writer = new NQuadsWriter(output)) {
            writer.quad(
                    "_:s", "_:p",
                    "a", "http://www.w3.org/1999/02/22-rdf-syntax-ns#dirLangString", "en-GB", "ltr",
                    "_:g");
        }
        assertEquals("_:s _:p \"a\"@en-GB--ltr _:g .\n", output.toString());
    }

    @Test
    void testInvalidDirection() {
        assertThrows(IllegalArgumentException.class, () -> {
            try (var writer = new NQuadsWriter(new StringWriter())) {
                writer.quad(
                        "_:s", "_:p",
                        "a", "http://www.w3.org/1999/02/22-rdf-syntax-ns#dirLangString", null, "rtr",
                        "_:g");
            }
            ;
        });
    }

    @Test
    void testXsdString() throws IOException {
        var output = new StringWriter();
        try (var writer = new NQuadsWriter(output)) {
            writer.quad(
                    "_:s", "_:p",
                    "a", "http://www.w3.org/2001/XMLSchema#string", null, null,
                    "_:g");
        }
        assertEquals("_:s _:p \"a\" _:g .\n", output.toString());
    }

    @Test
    void testDirLangTagWithoutLang() throws IOException {
        var output = new StringWriter();
        try (var writer = new NQuadsWriter(output)) {
            writer.quad(
                    "_:s", "_:p",
                    "a", "http://www.w3.org/1999/02/22-rdf-syntax-ns#dirLangString", null, "ltr",
                    "_:g");
        }
        assertEquals("_:s _:p \"a\"@und--ltr _:g .\n", output.toString());
    }

    @Test
    void testLanguageWithoutDatatype() throws IOException {
        var output = new StringWriter();
        try (var writer = new NQuadsWriter(output)) {
            writer.quad(
                    "_:s", "_:p",
                    "a", null, "cs", null,
                    "_:g");
        }
        assertEquals("_:s _:p \"a\"@cs _:g .\n", output.toString());
    }

    @Test
    void testDirectionWithoutDatatype() throws IOException {
        var output = new StringWriter();
        try (var writer = new NQuadsWriter(output)) {
            writer.quad(
                    "_:s", "_:p",
                    "a", null, null, "ltr",
                    "_:g");
        }
        assertEquals("_:s _:p \"a\"@und--ltr _:g .\n", output.toString());
    }

    @Test
    void testLangDirWithoutDatatype() throws IOException {
        var output = new StringWriter();
        try (var writer = new NQuadsWriter(output)) {
            writer.quad(
                    "_:s", "_:p",
                    "a", null, "cs", "ltr",
                    "_:g");
        }
        assertEquals("_:s _:p \"a\"@cs--ltr _:g .\n", output.toString());
    }

}
