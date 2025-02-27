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
import java.io.StringWriter;
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
        final StringWriter writer = new StringWriter();
        try {
            literal(new StringWriter(), literal, datatype, langTag, direction);

        } catch (IOException e) {
            /* ignore */
        }
        return writer.toString();
    }

    /**
     * Generates an N-Quad string representation for a quad with a resource subject,
     * predicate, and object.
     * 
     * @param subject   The subject of the triple. This can be either an IRI or a
     *                  blank node.
     * @param predicate The predicate of the triple, which must be an IRI.
     * @param object    The object of the triple, which can be either an IRI or a
     *                  blank node.
     * @param graph     The named graph for the triple, or null if no graph is
     *                  specified.
     * 
     * @return The N-Quad representation of the triple as a string.
     */
    public static final String nquad(String subject, String predicate, String object, String graph) {
        final StringWriter writer = new StringWriter();
        try {
            nquad(writer, subject, predicate, object, graph);

        } catch (IOException e) {
            /* ignore */
        }
        return writer.toString();
    }

    /**
     * Generates an N-Quad string representation for a quad with a literal object
     * and a specified datatype.
     * 
     * @param subject   The subject of the triple. This can be either an IRI or a
     *                  blank node.
     * @param predicate The predicate of the triple, which must be an IRI.
     * @param literal   The literal value for the object in the triple.
     * @param datatype  The datatype IRI for the literal.
     * @param graph     The named graph for the triple, or {@code null} if no graph
     *                  is specified.
     * @return The N-Quad representation of the triple as a string.
     */
    public static final String nquad(String subject, String predicate, String literal, String datatype, String graph) {
        final StringWriter writer = new StringWriter();
        try {
            nquad(writer, subject, predicate, literal, datatype, null, null, graph);

        } catch (IOException e) {
            /* ignore */
        }
        return writer.toString();
    }

    /**
     * Generates an N-Quad string representation for a quad with a lanauge-tagged
     * literal object and optionally direction.
     * 
     * @param subject   The subject of the triple. This can be either an IRI or a
     *                  blank node.
     * @param predicate The predicate of the triple, which must be an IRI.
     * @param literal   The literal value for the object in the triple.
     * @param langTag   The language tag for the literal.
     * @param direction The direction of the literal, or null if not applicable.
     * @param graph     The named graph for the triple, or null if no graph is
     *                  specified.
     * @return The N-Quad representation of the triple as a string.
     */
    public static final String nquad(String subject, String predicate, String literal, String langTag, String direction, String graph) {
        final StringWriter writer = new StringWriter();
        try {
            nquad(writer, subject, predicate, literal, null, langTag, direction, graph);

        } catch (IOException e) {
            /* ignore */
        }
        return writer.toString();
    }

    protected static final String nquad(String subject, String predicate, String literal, String datatype, String langTag, String direction, String graph) {
        final StringWriter writer = new StringWriter();
        try {
            nquad(writer, subject, predicate, literal, null, langTag, direction, graph);

        } catch (IOException e) {
            /* ignore */
        }
        return writer.toString();
    }

    @Override
    public RdfQuadConsumer quad(String subject, String predicate, String object, String graph) throws RdfConsumerException {
        try {
            nquad(writer, subject, predicate, object, graph);
            return this;
        } catch (IOException e) {
            throw new RdfConsumerException(subject, predicate, object, graph, e);
        }
    }

    @Override
    public RdfQuadConsumer quad(String subject, String predicate, String literal, String datatype, String graph) throws RdfConsumerException {
        try {
            nquad(writer, subject, predicate, literal, datatype, null, null, graph);
            return this;

        } catch (IOException e) {
            throw new RdfConsumerException(subject, predicate, literal, datatype, graph, e);
        }

    }

    @Override
    public RdfQuadConsumer quad(String subject, String predicate, String literal, String langTag, String direction, String graph) throws RdfConsumerException {
        try {
            nquad(writer, subject, predicate, literal, null, langTag, direction, graph);
            return this;

        } catch (IOException e) {
            throw new RdfConsumerException(subject, predicate, literal, langTag, direction, graph, e);
        }
    }

    protected static void nquad(Writer writer, String subject, String predicate, String object, String graph) throws IOException {
        writer.append(resourceOrBlank(subject))
                .append(' ')
                .append(resourceOrBlank(predicate))
                .append(' ')
                .append(resourceOrBlank(object))
                .append(' ');

        if (graph != null) {
            writer.append(resourceOrBlank(graph))
                    .append(' ');
        }

        writer.append(".\n");
    }

    protected static void nquad(Writer writer, String subject, String predicate, String literal, String datatype, String langTag, String direction, String graph) throws IOException {
        writer.append(resourceOrBlank(subject))
                .append(' ')
                .append(resourceOrBlank(predicate))
                .append(' ');

        literal(writer, literal, datatype, langTag, direction);
        writer.append(' ');

        if (graph != null) {
            writer.append(resourceOrBlank(graph))
                    .append(' ');
        }

        writer.append(".\n");
    }

    protected static final void literal(Writer writer, String literal, String datatype, String langTag, String direction) throws IOException {

        writer.append('"').append(NQuadsAlphabet.escape(literal)).append('"');

        if (direction != null) {
            writer.append(NQuadsReader.I18N_BASE)
                    .append(langTag)
                    .append("_")
                    .append(direction);

        } else if (langTag != null) {

            writer.append("@").append(langTag);

        } else if (datatype != null) {

            if (NQuadsReader.XSD_STRING.equals(datatype)) {
                return;
            }

            writer.append("^^").append(resource(datatype));
        }
    }
}
