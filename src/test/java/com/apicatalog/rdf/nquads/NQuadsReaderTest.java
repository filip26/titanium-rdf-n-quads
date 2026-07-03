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
            new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#de> ."))
                    .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    });
        });
    }

    @Test
    void testI18NDirection() throws NQuadsReaderException {
        new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#_rtl> ."))
                .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    assertEquals("c", object);
                    assertEquals("https://www.w3.org/ns/i18n#", datatype);
                    assertNull(language);
                    assertEquals("rtl", direction);
                    assertNull(graph);
                });
    }

    @Test
    void testI18NEmptyDirection() {
        assertThrows(NQuadsReaderException.class, () -> {
            new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#cs_> ."))
                    .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    });
        });
    }

    @Test
    void testI18NEmptyLang() throws NQuadsReaderException {
        new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#_ltr> ."))
                .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    assertEquals("c", object);
                    assertEquals("https://www.w3.org/ns/i18n#", datatype);
                    assertNull(language);
                    assertEquals("ltr", direction);
                    assertNull(graph);
                });
    }

    @Test
    void testI18() {
        assertThrows(NQuadsReaderException.class, () -> {
            new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#> ."))
                    .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    });
        });
    }

    @Test
    void testI18NEmpty() {
        assertThrows(NQuadsReaderException.class, () -> {
            new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#_> ."))
                    .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    });
        });
    }

    @Test
    void testI18NDirLangTag() throws NQuadsReaderException {
        new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#cs_ltr> ."))
                .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    assertEquals("c", object);
                    assertEquals("https://www.w3.org/ns/i18n#", datatype);
                    assertEquals("cs", language);
                    assertEquals("ltr", direction);
                    assertNull(graph);
                });
    }

    @Test
    void testLangTag() throws NQuadsReaderException {
        new NQuadsReader(new StringReader("_:a <test:b> \"c\"@cs ."))
                .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    assertEquals("c", object);
                    assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString", datatype);
                    assertEquals("cs", language);
                    assertNull(direction);
                    assertNull(graph);
                });
    }

    @Test
    void testDirLangTag() throws NQuadsReaderException {
        new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"@cs--ltr ."))
                .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    assertEquals("c", object);
                    assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#dirLangString", datatype);
                    assertEquals("cs", language);
                    assertEquals("ltr", direction);
                    assertNull(graph);
                });
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
