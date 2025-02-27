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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.apicatalog.rdf.api.RdfConsumerException;
import com.apicatalog.rdf.nquads.NQuadsReaderTestCase.Type;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue.ValueType;
import jakarta.json.stream.JsonParser;

class NQuadsTest {

    private final static String TEST_CASE_BASE_PATH = "nquads-test-suite/";

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testReadWrite(NQuadsReaderTestCase testCase) throws IOException, URISyntaxException {

        assertNotNull(testCase);
        assertNotNull(testCase.getName());
        assertNotNull(testCase.getType());

        try (final Reader reader = new InputStreamReader(NQuadsTest.class.getResourceAsStream(TEST_CASE_BASE_PATH + testCase.getName() + ".nq"))) {
            
            final String input = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
            assertNotNull(input);

            final StringWriter writer = new StringWriter();
            new NQuadsReader(new StringReader(input)).provide((new NQuadsWriter(writer)));

            final String result = writer.toString();
            assertNotNull(result);

            assertEquals(Type.POSITIVE, testCase.getType());
            
            assertEquals(input, result);

        } catch (IllegalArgumentException | NQuadsReaderException | RdfConsumerException e) {
            assertEquals(Type.NEGATIVE, testCase.getType());
        }
    }

    static final Stream<NQuadsReaderTestCase> data() throws ZipException, IOException, URISyntaxException {
        return load(TEST_CASE_BASE_PATH, "manifest.json");
    }
    
    static final Stream<NQuadsReaderTestCase> load(String path, String name) throws ZipException, IOException, URISyntaxException {
        try (final InputStream is  =  NQuadsTest.class.getResourceAsStream(path + name)) {
            final JsonParser parser = Json.createParser(is);

            parser.next();

            return parser
                        .getArray()
                        .stream()
                        .filter(v -> ValueType.OBJECT.equals(v.getValueType())) 
                        .map(JsonObject.class::cast)
                        .map(NQuadsReaderTestCase::of);
        }
    }
}
