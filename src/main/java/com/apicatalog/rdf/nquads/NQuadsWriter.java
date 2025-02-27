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
 * A writer for serializing RDF data in the
 * <a href="https://www.w3.org/TR/n-quads/">N-Quads format</a>.
 * <p>
 * This class implements the {@link RdfQuadConsumer} interface, allowing RDF
 * quads to be written to an output stream in the standard N-Quads
 * serialization. It supports writing IRIs, blank nodes, literals, and
 * language-tagged literals.
 * </p>
 *
 * <p>
 * Usage example:
 * </p>
 * 
 * <pre>
 * Writer writer = new FileWriter("output.nq");
 * NQuadsWriter nQuadsWriter = new NQuadsWriter(writer);
 * nQuadsWriter.quad("<http://example.com/subject>", "<http://example.com/predicate>", "\"Object\"", null, null);
 * writer.close();
 * </pre>
 *
 * @see <a href="https://www.w3.org/TR/n-quads/">RDF 1.1 N-Quads
 *      Specification</a>
 */
public class NQuadsWriter implements RdfQuadConsumer {

    protected final Writer writer;

    public NQuadsWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * Converts an IRI or a blank node identifier into an N-Quads compliant
     * representation.
     *
     * @param value the IRI or blank node identifier to process
     * @return the formatted IRI in angle brackets or the blank node identifier
     * @throws IllegalArgumentException if the input value is {@code null}
     */
    public static final String resourceOrBlank(String value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        if (value.startsWith("_:")) {
            return value;
        }
        return resource(value);
    }

    /**
     * Formats an IRI for use in N-Quads by enclosing it in angle brackets.
     *
     * @param iri the IRI to format
     * @return the IRI wrapped in angle brackets
     * @throws IllegalArgumentException if the input IRI is {@code null}
     */
    public static final String resource(String iri) {

        if (iri == null) {
            throw new IllegalArgumentException();
        }
        return "<" + iri + ">";
    }

    /**
     * Formats an RDF literal with optional datatype, language tag, or direction.
     *
     * @param literal   the literal value
     * @param datatype  the optional datatype IRI (may be {@code null})
     * @param langTag   the optional language tag (may be {@code null})
     * @param direction the optional text direction (may be {@code null})
     * @return the formatted N-Quads literal representation
     * @throws IllegalArgumentException if the literal value is {@code null}
     */
    public static final String literal(String literal, String datatype, String langTag, String direction) {

        if (literal == null) {
            throw new IllegalArgumentException();
        }

        StringBuilder builder = new StringBuilder();

        builder.append('"').append(NQuadsAlphabet.escape(literal)).append('"');

        if (direction != null) {

        } else if (langTag != null) {

            builder.append("@").append(langTag);

        } else if (datatype != null) {

            if (NQuadsReader.XSD_STRING.equals(datatype)) {
                return builder.toString();
            }

            builder.append("^^").append(resource(datatype));
        }

        return builder.toString();
    }

    @Override
    public RdfQuadConsumer quad(String subject, String predicate, String object, String graph) throws RdfConsumerException {
        try {
            writer.write(resourceOrBlank(subject));
            writer.write(' ');

            writer.write(resourceOrBlank(predicate));
            writer.write(' ');

            writer.write(resourceOrBlank(object));
            writer.write(' ');

            if (graph != null) {
                writer.write(resourceOrBlank(graph));
                writer.write(' ');
            }

            writer.write(".\n");
        } catch (IOException e) {
            throw new RdfConsumerException(subject, predicate, object, graph, e);
        }
        return this;
    }

    @Override
    public RdfQuadConsumer quad(String subject, String predicate, String literal, String datatype, String graph) throws RdfConsumerException {
        try {
            writer.write(resourceOrBlank(subject));
            writer.write(' ');

            writer.write(resourceOrBlank(predicate));
            writer.write(' ');

            writeLiteral(literal, datatype, null, null);
            writer.write(' ');

            if (graph != null) {
                writer.write(resourceOrBlank(graph));
                writer.write(' ');
            }

            writer.write(".\n");
        } catch (IOException e) {
            throw new RdfConsumerException(subject, predicate, literal, datatype, graph, e);
        }
        return this;
    }

    @Override
    public RdfQuadConsumer quad(String subject, String predicate, String literal, String langTag, String direction, String graph) throws RdfConsumerException {
        try {
            writer.write(resourceOrBlank(subject));
            writer.write(' ');

            writer.write(resourceOrBlank(predicate));
            writer.write(' ');

            writeLiteral(literal, null, langTag, direction);
            writer.write(' ');

            if (graph != null) {
                writer.write(resourceOrBlank(graph));
                writer.write(' ');
            }

            writer.write(".\n");
        } catch (IOException e) {
            throw new RdfConsumerException(subject, predicate, literal, langTag, direction, graph, e);
        }
        return this;
    }

    protected void writeLiteral(String literal, String datatype, String langTag, String direction) throws IOException {

        if (literal == null) {
            throw new IllegalArgumentException();
        }

        writer.write('"');
        writer.write(NQuadsAlphabet.escape(literal));
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
            writer.write(resource(datatype));
        }
    }
}
