package com.apicatalog.rdf.nquads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
                reader.read((subject, predicate, object, datatype, language, direction, graph) -> {
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
            reader.read((subject, predicate, object, datatype, language, direction, graph) -> {
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
                reader.read((subject, predicate, object, datatype, language, direction, graph) -> {
                });
            }
        });
    }

    @Test
    void testI18NEmptyLang() throws NQuadsReaderException, IOException {
        try (var reader = new NQuadsReader(
                new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#_ltr> ."))) {
            reader.read((subject, predicate, object, datatype, language, direction, graph) -> {
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
                reader.read((subject, predicate, object, datatype, language, direction, graph) -> {
                });
            }
        });
    }

    @Test
    void testI18NEmpty() {
        assertThrows(NQuadsReaderException.class, () -> {
            try (var reader = new NQuadsReader(
                    new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#_> ."))) {
                reader.read((subject, predicate, object, datatype, language, direction, graph) -> {
                });
            }
        });
    }

    @Test
    void testI18NDirLangTag() throws NQuadsReaderException, IOException {
        try (var reader = new NQuadsReader(
                new StringReader("<test:a> <test:b> \"c\"^^<https://www.w3.org/ns/i18n#cs_ltr> ."))) {
            reader.read((subject, predicate, object, datatype, language, direction, graph) -> {
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
            reader.read((subject, predicate, object, datatype, language, direction, graph) -> {
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
            reader.read((subject, predicate, object, datatype, language, direction, graph) -> {
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
            assertTrue(reader.hasNext());
            reader.read((subject, predicate, object, datatype, language, direction, graph) -> {
                assertEquals("c", object);
                assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#dirLangString", datatype);
                assertEquals("en-GB", language);
                assertEquals("ltr", direction);
                assertNull(graph);
            });
            assertFalse(reader.hasNext());
        }
    }

    @Test
    void testInvalidDirection() {
        assertThrows(NQuadsReaderException.class, () -> {
            try (var reader = new NQuadsReader(new StringReader("<test:a> <test:b> \"c\"@cs--xtr."))) {
                reader.readLine((subject, predicate, object, datatype, language, direction, graph) -> {
                });
            }
            ;
        });
    }

    @Test
    void testXsdString() throws NQuadsReaderException, IOException {
        try (var reader = new NQuadsReader(new StringReader("<test:a> <test:b> \"abc\" .\n"))) {
            assertTrue(reader.hasNext());
            assertTrue(reader.readLine((subject, predicate, object, datatype, language, direction, graph) -> {
                assertEquals("abc", object);
                assertEquals("http://www.w3.org/2001/XMLSchema#string", datatype);
                assertNull(language);
                assertNull(direction);
                assertNull(graph);
            }));
            assertFalse(reader.readLine((subject, predicate, object, datatype, language, direction, graph) -> {
                fail();
            }));
            assertFalse(reader.hasNext());
        }
    }

    @Test
    void testTwoLines() throws NQuadsReaderException, IOException {
        try (var reader = new NQuadsReader(
                new StringReader("<test:a> <test:b> \"abc\" .\n<test:a> <test:b> \"bcd\" ."))) {
            assertTrue(reader.readLine((subject, predicate, object, datatype, language, direction, graph) -> {
                assertEquals("abc", object);
                assertEquals("http://www.w3.org/2001/XMLSchema#string", datatype);
                assertNull(language);
                assertNull(direction);
                assertNull(graph);
            }));
            assertTrue(reader.hasNext());
            assertTrue(reader.readLine((subject, predicate, object, datatype, language, direction, graph) -> {
                assertEquals("bcd", object);
                assertEquals("http://www.w3.org/2001/XMLSchema#string", datatype);
                assertNull(language);
                assertNull(direction);
                assertNull(graph);
            }));
            assertFalse(reader.readLine((subject, predicate, object, datatype, language, direction, graph) -> {
                fail();
            }));
            assertFalse(reader.hasNext());
        }
    }
}
