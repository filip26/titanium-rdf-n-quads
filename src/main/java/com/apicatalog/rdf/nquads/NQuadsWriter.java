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

import java.io.IOException;
import java.io.Writer;

import com.apicatalog.rdf.api.RdfConsumerException;
import com.apicatalog.rdf.api.RdfQuadConsumer;

/**
 *
 * @see <a href="https://www.w3.org/TR/n-quads/">RDF 1.1. N-Quads</a>
 *
 */
public class NQuadsWriter implements RdfQuadConsumer {

    private final Writer writer;

    public NQuadsWriter(Writer writer) {
        this.writer = writer;
    }

    protected void writeIriOrBlank(String value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        if (value.startsWith("_:")) {
            writer.write(value);
            return;
        }

        writeIri(value);
    }

    protected void writeLiteral(String literal, String datatype, String langTag, String direction) throws IOException {

        if (literal == null) {
            throw new IllegalArgumentException();
        }

        writer.write('"');
        writer.write(escape(literal));
        writer.write('"');

        if (direction != null) {
            
        } else if (langTag != null) {

            writer.write("@");
            writer.write(langTag);

        } else if (datatype != null) {

            if (NQuadsReader.XSD_STRING.equals(datatype)) {
                return;
            }

            writer.write("^^");
            writeIri(datatype);
        }
    }

    public static final String escape(String value) {

        final StringBuilder escaped = new StringBuilder();

        int[] codePoints = value.codePoints().toArray();

        for (int ch : codePoints) {

            if (ch == 0x9) {
                escaped.append("\\t");

            } else if (ch == 0x8) {
                escaped.append("\\b");

            } else if (ch == 0xa) {
                escaped.append("\\n");

            } else if (ch == 0xd) {
                escaped.append("\\r");

            } else if (ch == 0xc) {
                escaped.append("\\f");

            } else if (ch == '"') {
                escaped.append("\\\"");

            } else if (ch == '\\') {
                escaped.append("\\\\");

            } else if (ch >= 0x0 && ch <= 0x1f || ch == 0x7f) {
                escaped.append(String.format("\\u%04x", ch));

            } else {
                escaped.appendCodePoint(ch);
            }
        }
        return escaped.toString();
    }

    protected void writeIri(String iri) throws IOException {

        if (iri == null) {
            throw new IllegalArgumentException();
        }

        writer.write('<');
        writer.write(iri);
        writer.write('>');
    }

    @Override
    public RdfQuadConsumer quad(String subject, String predicate, String object, String graph) throws RdfConsumerException {
        try {
            writeIriOrBlank(subject);
            writer.write(' ');

            writeIriOrBlank(predicate);
            writer.write(' ');

            writeIriOrBlank(object);
            writer.write(' ');

            if (graph != null) {
                writeIriOrBlank(graph);
                writer.write(' ');
            }

            writer.write(".\n");
        } catch (IOException e) {
            throw new RdfConsumerException(e, subject, predicate, object, graph);
        }
        return this;
    }

    @Override
    public RdfQuadConsumer quad(String subject, String predicate, String literal, String datatype, String graph) throws RdfConsumerException {
        try {
            writeIriOrBlank(subject);
            writer.write(' ');

            writeIriOrBlank(predicate);
            writer.write(' ');

            writeLiteral(literal, datatype, null, null);
            writer.write(' ');

            if (graph != null) {
                writeIriOrBlank(graph);
                writer.write(' ');
            }

            writer.write(".\n");
        } catch (IOException e) {
            throw new RdfConsumerException(e, subject, predicate, literal, datatype, graph);
        }
        return this;
    }

    @Override
    public RdfQuadConsumer quad(String subject, String predicate, String literal, String langTag, String direction, String graph) throws RdfConsumerException {
        try {
            writeIriOrBlank(subject);
            writer.write(' ');

            writeIriOrBlank(predicate);
            writer.write(' ');

            writeLiteral(literal, null, langTag, direction);
            writer.write(' ');

            if (graph != null) {
                writeIriOrBlank(graph);
                writer.write(' ');
            }

            writer.write(".\n");
        } catch (IOException e) {
            throw new RdfConsumerException(e, subject, predicate, literal, langTag, direction, graph);
        }
        return this;
    }
}
