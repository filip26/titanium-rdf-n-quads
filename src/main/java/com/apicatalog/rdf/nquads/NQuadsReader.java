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

import java.io.Reader;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import com.apicatalog.rdf.api.RdfConsumerException;
import com.apicatalog.rdf.api.RdfQuadConsumer;
import com.apicatalog.rdf.nquads.NQuadsTokenizer.Token;
import com.apicatalog.rdf.nquads.NQuadsTokenizer.TokenType;

/**
 * A simple and efficient streaming N-Quads reader.
 * <p>
 * Use the {@link #provide(RdfQuadConsumer)} method to parse input and process
 * N-Quads statements.
 * </p>
 * 
 * @see <a href="https://www.w3.org/TR/n-quads/">RDF 1.1 N-Quads
 *      Specification</a>
 */
public class NQuadsReader {

    protected final NQuadsTokenizer tokenizer;
    protected final Predicate<String> testAbsoluteIRI;

    // runtime state
    protected String ltObject;
    protected String ltDatatype;
    protected String ltLangTag;
    protected String ltDirection;

    /**
     * Creates a new {@code NQuadsReader} instance with the specified character
     * stream.
     *
     * @param reader the {@link Reader} to read N-Quads data from
     */
    public NQuadsReader(final Reader reader) {
        this(new NQuadsTokenizer(reader), NQuadsReader::startsWithScheme);
    }

    /**
     * Creates a new {@code NQuadsReader} instance with the specified character
     * stream.
     *
     * @param reader          the {@link Reader} to read N-Quads data from
     * @param testAbsoluteIRI a function to test if an IRI is absolute or not
     */
    public NQuadsReader(final Reader reader, Predicate<String> testAbsoluteIRI) {
        this(new NQuadsTokenizer(reader), testAbsoluteIRI);
    }

    /**
     * Creates a new {@code NQuadsReader} instance with the specified character
     * stream and buffer size for optimized reading.
     *
     * @param reader     the {@link Reader} to read N-Quads data from
     * @param bufferSize the size of the buffer used for reading &gt; 0 (bytes)
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive number
     */
    public NQuadsReader(final Reader reader, int bufferSize) {
        this(new NQuadsTokenizer(reader, bufferSize), NQuadsReader::startsWithScheme);
    }

    /**
     * Creates a new {@code NQuadsReader} instance with the specified character
     * stream and buffer size for optimized reading.
     *
     * @param reader          the {@link Reader} to read N-Quads data from
     * @param bufferSize      the size of the buffer used for reading &gt; 0 (bytes)
     * @param testAbsoluteIRI a function to test if an IRI is absolute or not
     * @throws IllegalArgumentException if {@code bufferSize} is non-positive number
     */
    public NQuadsReader(final Reader reader, int bufferSize, final Predicate<String> testAbsoluteIRI) {
        this(new NQuadsTokenizer(reader, bufferSize), testAbsoluteIRI);
    }

    protected NQuadsReader(final NQuadsTokenizer tokenizer, final Predicate<String> testAbsoluteIRI) {
        this.tokenizer = tokenizer;
        this.testAbsoluteIRI = testAbsoluteIRI;
    }

    /**
     * Reads and processes N-Quads, invoking the provided consumer immediately after
     * each N-Quad statement is deserialized.
     *
     * @param consumer the {@link RdfQuadConsumer} that processes each deserialized
     *                 N-Quad statement
     * 
     * @throws NQuadsReaderException if an error occurs while reading the N-Quads
     * @throws RdfConsumerException  if an error occurs while processing the N-Quad
     *                               statement
     */
    public void provide(RdfQuadConsumer consumer) throws NQuadsReaderException, RdfConsumerException {
        while (tokenizer.hasNext()) {

            // skip EOL and whitespace
            if (tokenizer.accept(NQuadsTokenizer.TokenType.END_OF_LINE)
                    || tokenizer.accept(NQuadsTokenizer.TokenType.WHITE_SPACE)
                    || tokenizer.accept(NQuadsTokenizer.TokenType.COMMENT)) {

                continue;
            }

            statement(consumer);
        }
    }

    protected void statement(RdfQuadConsumer consumer) throws NQuadsReaderException, RdfConsumerException {

        String subject = resource("Subject");

        skipWhitespace(0);

        String predicate = resource("Predicate");

        skipWhitespace(0);

        object();

        String graphName = null;

        skipWhitespace(0);

        if (TokenType.IRI_REF == tokenizer.token().getType()) {

            final String graphNameIri = tokenizer.token().getValue();

            assertAbsoluteIri(graphNameIri, "Graph name");

            graphName = graphNameIri;
            tokenizer.next();
            skipWhitespace(0);
        }

        if (TokenType.BLANK_NODE_LABEL == tokenizer.token().getType()) {

            graphName = "_:".concat(tokenizer.token().getValue());
            tokenizer.next();
            skipWhitespace(0);
        }

        if (TokenType.END_OF_STATEMENT != tokenizer.token().getType()) {
            unexpected(tokenizer.token(), TokenType.END_OF_STATEMENT);
        }

        tokenizer.next();

        skipWhitespace(0);

        // skip comment
        if (TokenType.COMMENT == tokenizer.token().getType()) {
            tokenizer.next();

            // skip end of line
        } else if (TokenType.END_OF_LINE != tokenizer.token().getType() && TokenType.END_OF_INPUT != tokenizer.token().getType()) {
            unexpected(tokenizer.token(), TokenType.END_OF_LINE, TokenType.END_OF_INPUT);
            tokenizer.next();
        }

        consumer.quad(subject, predicate, ltObject, ltDatatype, ltLangTag, ltDirection, graphName);
    }

    protected String resource(String name) throws NQuadsReaderException {

        final Token token = tokenizer.token();

        if (TokenType.IRI_REF == token.getType()) {

            tokenizer.next();

            final String iri = token.getValue();

            assertAbsoluteIri(iri, name);

            return iri;
        }

        if (TokenType.BLANK_NODE_LABEL == token.getType()) {

            tokenizer.next();

            return "_:".concat(token.getValue());
        }

        return unexpected(token);
    }

    protected void object() throws NQuadsReaderException {

        ltObject = null;
        ltDatatype = null;
        ltLangTag = null;
        ltDirection = null;

        Token token = tokenizer.token();

        if (TokenType.IRI_REF == token.getType()) {
            tokenizer.next();

            final String iri = token.getValue();

            assertAbsoluteIri(iri, "Object");

            ltObject = iri;
            return;
        }

        if (TokenType.BLANK_NODE_LABEL == token.getType()) {

            tokenizer.next();

            ltObject = "_:".concat(token.getValue());
            return;
        }

        // read literal
        if (TokenType.STRING_LITERAL_QUOTE != token.getType()) {
            unexpected(token);
        }

        tokenizer.next();

        skipWhitespace(0);

        if (TokenType.LANGTAG == tokenizer.token().getType()) {

            String langTag = tokenizer.token().getValue();

            tokenizer.next();

            this.ltDatatype = NQuadsAlphabet.LANG_STRING;
            this.ltObject = token.getValue();
            this.ltLangTag = langTag;

            return;

        } else if (TokenType.LITERAL_DATA_TYPE == tokenizer.token().getType()) {

            tokenizer.next();
            skipWhitespace(0);

            Token attr = tokenizer.token();

            if (TokenType.IRI_REF == attr.getType()) {

                tokenizer.next();

                final String datatype = attr.getValue();

                assertAbsoluteIri(datatype, "DataType");

                this.ltObject = token.getValue();

                datatype(datatype, (a, b) -> {
                    this.ltDatatype = a;
                    if (b != null) {
                        if (b.length > 1) {
                            this.ltDirection = b[1];
                        }
                        if (b.length > 0) {
                            this.ltLangTag = b[0];
                        }
                    }
                });
                return;
            }

            unexpected(attr);
        }

        this.ltObject = token.getValue();
        this.ltDatatype = NQuadsAlphabet.XSD_STRING;
    }

    protected static final <T> T unexpected(Token token, TokenType... types) throws NQuadsReaderException {
        throw new NQuadsReaderException(
                "Unexpected token " + token.getType() + (token.getValue() != null ? "[" + token.getValue() + "]" : "") + ". "
                        + "Expected one of " + Arrays.toString(types) + ".");
    }

    protected void skipWhitespace(int min) throws NQuadsReaderException {

        int count = 0;

        while (tokenizer.accept(TokenType.WHITE_SPACE)) {
            count++;
        }

        if (count < min) {
            unexpected(tokenizer.token());
        }
    }

    protected final void assertAbsoluteIri(final String uri, final String what) throws NQuadsReaderException {
        if (!testAbsoluteIRI.test(uri)) {
            throw new NQuadsReaderException(what + " must be an absolute URI [" + uri + "]. ");
        }
    }

    protected static final void datatype(final String datatype, final BiConsumer<String, String[]> result) {
        if (datatype.startsWith(NQuadsAlphabet.I18N_BASE)) {

            String[] langDir = datatype.substring(NQuadsAlphabet.I18N_BASE.length()).split("_");

            result.accept(NQuadsAlphabet.I18N_BASE, langDir);

            return;
        }
        result.accept(datatype, null);
    }

    protected static final boolean startsWithScheme(final String uri) {

        if (uri == null
                || uri.length() < 2 // a scheme must have at least one letter followed by ':'
                || !Character.isLetter(uri.codePointAt(0)) // a scheme name must start with a letter
        ) {
            return false;
        }

        for (int i = 1; i < uri.length(); i++) {

            if (
            // a scheme name must start with a letter followed by a letter/digit/+/-/.
            Character.isLetterOrDigit(uri.codePointAt(i))
                    || uri.charAt(i) == '-' || uri.charAt(i) == '+' || uri.charAt(i) == '.') {
                continue;
            }

            // a scheme name must be terminated by ':'
            return uri.charAt(i) == ':';
        }
        return false;
    }
}
