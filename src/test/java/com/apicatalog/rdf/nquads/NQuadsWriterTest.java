package com.apicatalog.rdf.nquads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

class NQuadsWriterTest {

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
    void testI18DirectionWithoutLang() {
//        try (var reader = new NQuadsReader(new StringReader(
//                """
//                <test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#cs_> .
//                """))) {
//            reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
//            });
//        }
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
        try (var reader = new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"@cs--ltr."))) {
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
    void testComplexDirLangTag() throws NQuadsReaderException, IOException {
        try (var reader = new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"@en-GB--ltr."))) {
            reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
                assertEquals("c", object);
                assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#dirLangString", datatype);
                assertEquals("en-GB", language);
                assertEquals("ltr", direction);
                assertNull(graph);
            });
        }
    }

    @Test
    void testInvalidDirection() {
        assertThrows(NQuadsReaderException.class, () -> {
            try (var reader = new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"@cs--xtr."))) {
                reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
                });
            }
            ;
        });
    }

    @Test
    void testXsdString() throws NQuadsReaderException, IOException {
//        try (var reader = new NQuadsReader(new StringReader("<test:a> <test:b> \"abc\" ."))) {
//            reader.provide((subject, predicate, object, datatype, language, direction, graph) -> {
//                assertEquals("abc", object);
//                assertEquals("http://www.w3.org/2001/XMLSchema#string", datatype);
//                assertNull(language);
//                assertNull(direction);
//                assertNull(graph);
//            });
//        }
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
}
