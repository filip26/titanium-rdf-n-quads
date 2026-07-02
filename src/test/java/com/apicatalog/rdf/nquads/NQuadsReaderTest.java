package com.apicatalog.rdf.nquads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

class NQuadsReaderTest {

    @Test
    void testI18NLang() throws NQuadsReaderException {
        new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#de> ."))
                .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    assertEquals("c", object);
                    assertEquals("https://www.w3.org/ns/i18n#", datatype);
                    assertEquals("de", language);
                    assertNull(direction);
                    assertNull(graph);
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
    void testI18NEmptyDirection() throws NQuadsReaderException {
        new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#cz_> ."))
                .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    assertEquals("c", object);
                    assertEquals("https://www.w3.org/ns/i18n#", datatype);
                    assertEquals("cz", language);
                    assertNull(direction);
                    assertNull(graph);
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
    void testI18() throws NQuadsReaderException {
        new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#> ."))
                .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    assertEquals("c", object);
                    assertEquals("https://www.w3.org/ns/i18n#", datatype);
                    assertNull(language);
                    assertNull(direction);
                    assertNull(graph);
                });
    }

    @Test
    void testI18NEmpty() throws NQuadsReaderException {
        new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#_> ."))
                .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    assertEquals("c", object);
                    assertEquals("https://www.w3.org/ns/i18n#", datatype);
                    assertNull(language);
                    assertNull(direction);
                    assertNull(graph);
                });
    }

    @Test
    void testI18NDirLangTag() throws NQuadsReaderException {
        new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#cz_ltr> ."))
                .provide((subject, predicate, object, datatype, language, direction, graph) -> {
                    assertEquals("c", object);
                    assertEquals("https://www.w3.org/ns/i18n#", datatype);
                    assertEquals("cz", language);
                    assertEquals("ltr", direction);
                    assertNull(graph);
                });
    }
}
