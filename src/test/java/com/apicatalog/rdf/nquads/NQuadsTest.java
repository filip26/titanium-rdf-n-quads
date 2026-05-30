/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apicatalog.rdf.nquads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.apicatalog.rdf.api.RdfConsumerException;
import com.apicatalog.rdf.nquads.NQuadsTestCase.Type;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue.ValueType;
import jakarta.json.stream.JsonParser;

class NQuadsTest {

    private final static String RDF11_TEST_CASE_BASE_PATH = "nquads-test-suite/";
    private final static String RDF12_TEST_CASE_BASE_PATH = "rdf12/syntax/";

    @DisplayName("RDF 1.1")
    @ParameterizedTest(name = "{0}")
    @MethodSource("rdf11")
    void testRdf11(NQuadsTestCase testCase) throws IOException, URISyntaxException {
    	execute(testCase);
    }

    @DisplayName("RDF 1.2")
    @ParameterizedTest(name = "{0}")
    @MethodSource("rdf12")
    void testRdf12(NQuadsTestCase testCase) throws IOException, URISyntaxException {
    	execute(testCase);
    }

    final static void execute(NQuadsTestCase testCase) throws IOException, URISyntaxException {

        assertNotNull(testCase);
        assertNotNull(testCase.getName());
        assertNotNull(testCase.getType());

        try (final InputStream is = NQuadsTest.class.getResourceAsStream(testCase.getBasePath() + testCase.getAction())) {

            final String input = isToString(is);
            assertNotNull(input);

            final StringWriter writer = new StringWriter();
            new NQuadsReader(new StringReader(input)).provide((new NQuadsWriter(writer)));

            final String result = writer.toString();
            assertNotNull(result);

            assertEquals(Type.POSITIVE, testCase.getType());

            String expected = input;

            try (final InputStream out = NQuadsTest.class.getResourceAsStream(testCase.getBasePath() + testCase.getAction().substring(0, testCase.getAction().length() - 3) + ".out.nq")) {
                if (out != null) {
                    expected = isToString(out);
                }
            }

            final boolean match = expected.equals(result);

            if (!match) {
                System.out.println(testCase.getName());
                System.out.println("Expected:");
                System.out.println(expected);
                System.out.println("Result:");
                System.out.println(result);
            }

            assertTrue(match);

        } catch (IllegalArgumentException | NQuadsReaderException | RdfConsumerException e) {
            assertEquals(Type.NEGATIVE, testCase.getType());
        }
    }

    static final Stream<NQuadsTestCase> rdf11() throws IOException {
        return load(RDF11_TEST_CASE_BASE_PATH, "manifest.json");
    }

    static final Stream<NQuadsTestCase> rdf12() throws IOException {
        return load(RDF12_TEST_CASE_BASE_PATH, "manifest.json");
    }

    static final Stream<NQuadsTestCase> load(String path, String name) throws IOException {
        try (final InputStream is = NQuadsTest.class.getResourceAsStream(path + name)) {
            final JsonParser parser = Json.createParser(is);

            parser.next();

            return parser
                    .getArray()
                    .stream()
                    .filter(v -> ValueType.OBJECT.equals(v.getValueType()))
                    .map(JsonObject.class::cast)
                    .map(NQuadsTestCase::of)
                    .map(testCase -> { testCase.basePath = path; return testCase; });
        }
    }

    static final String isToString(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = is.read(buffer)) != -1;) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }
}
