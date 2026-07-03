package com.apicatalog.rdf.nquads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

class NQuadsReaderTest {

    @Test
    void testI18NLang() {
        assertThrows(NQuadsReaderException.class, () -> {
            try (var reader = new NQuadsReader(new StringReader(
                    """
                    <test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#de> .
                    """))) {
                reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
                });
            }
        });
    }

    @Test
    void testI18NDirection() throws NQuadsReaderException, IOException {
        try (var reader = new NQuadsReader(new StringReader(
                """
                <test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#_rtl> .
                """))) {
            reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
                assertEquals("c", object);
                assertEquals("https://www.w3.org/ns/i18n#", datatype);
                assertNull(language);
                assertEquals("rtl", direction);
                assertNull(graph);
            });
        }
    }

    @Test
    void testI18NEmptyDirection() {
        assertThrows(NQuadsReaderException.class, () -> {
            try (var reader = new NQuadsReader(new StringReader(
                    """
                    <test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#cs_> .
                    """))) {
                reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
                });
            }
        });
    }

    @Test
    void testI18NEmptyLang() throws NQuadsReaderException, IOException {
        try (var reader = new NQuadsReader(
                new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#_ltr> ."))) {
            reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
                assertEquals("c", object);
                assertEquals("https://www.w3.org/ns/i18n#", datatype);
                assertNull(language);
                assertEquals("ltr", direction);
                assertNull(graph);
            });
        }
    }

    @Test
    void testI18() {
        assertThrows(NQuadsReaderException.class, () -> {
            try (var reader = new NQuadsReader(
                    new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#> ."))) {
                reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
                });
            }
        });
    }

    @Test
    void testI18NEmpty() {
        assertThrows(NQuadsReaderException.class, () -> {
            try (var reader = new NQuadsReader(
                    new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#_> ."))) {
                reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
                });
            }
        });
    }

    @Test
    void testI18NDirLangTag() throws NQuadsReaderException, IOException {
        try (var reader = new NQuadsReader(
                new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#cs_ltr> ."))) {
            reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
                assertEquals("c", object);
                assertEquals("https://www.w3.org/ns/i18n#", datatype);
                assertEquals("cs", language);
                assertEquals("ltr", direction);
                assertNull(graph);
            });
        }
    }

    @Test
    void testLangTag() throws NQuadsReaderException, IOException {
        try (var reader = new NQuadsReader(new StringReader("_:a <test:b> \"c\"@cs ."))) {
            reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
                assertEquals("c", object);
                assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString", datatype);
                assertEquals("cs", language);
                assertNull(direction);
                assertNull(graph);
            });
        }
    }

    @Test
    void testDirLangTag() throws NQuadsReaderException, IOException {
        try (var reader = new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"@cs--ltr ."))) {
            reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
                assertEquals("c", object);
                assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#dirLangString", datatype);
                assertEquals("cs", language);
                assertEquals("ltr", direction);
                assertNull(graph);
            });
        }
    }

    @Test
    void testXsdString() throws NQuadsReaderException, IOException {
        try (var reader = new NQuadsReader(new StringReader("<test:a> <test:b> \"abc\" ."))) {
            reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
                assertEquals("abc", object);
                assertEquals("http://www.w3.org/2001/XMLSchema#string", datatype);
                assertNull(language);
                assertNull(direction);
                assertNull(graph);
            });
        }
    }
}
