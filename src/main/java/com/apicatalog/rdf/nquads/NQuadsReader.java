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
import java.net.URI;
import java.util.Arrays;
import java.util.function.BiConsumer;

import com.apicatalog.rdf.api.RdfConsumerException;
import com.apicatalog.rdf.api.RdfQuadConsumer;
import com.apicatalog.rdf.nquads.NQuadsTokenizer.Token;
import com.apicatalog.rdf.nquads.NQuadsTokenizer.TokenType;

/**
 *
 * @see <a href="https://www.w3.org/TR/n-quads/">RDF 1.1. N-Quads</a>
 *
 */
public class NQuadsReader {

    static final String I18N_BASE = "https://www.w3.org/ns/i18n#";
    static final String LANG_STRING = "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString";
    static final String XSD_STRING = "http://www.w3.org/2001/XMLSchema#string";

    protected final NQuadsTokenizer tokenizer;

    // runtime state
    protected String ltObject;
    protected String ltDatatype;
    protected String ltLangTag;
    protected String ltDirection;

    public NQuadsReader(final Reader reader) {
        this(new NQuadsTokenizer(reader));
    }

    public NQuadsReader(final Reader reader, int bufferSize) {
        this(new NQuadsTokenizer(reader, bufferSize));
    }

    protected NQuadsReader(final NQuadsTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

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

        if (ltDirection != null || ltLangTag != null) {
            consumer.quad(subject, predicate, ltObject, ltLangTag, ltDirection, graphName);

        } else if (ltDatatype != null) {
            consumer.quad(subject, predicate, ltObject, ltDatatype, graphName);

        } else {
            consumer.quad(subject, predicate, ltObject, graphName);
        }
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

            this.ltDatatype = LANG_STRING;
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
        this.ltDatatype = XSD_STRING;
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

    protected static final void assertAbsoluteIri(final String uri, final String what) throws NQuadsReaderException {
        if (!isAbsoluteUri(uri)) {
            throw new NQuadsReaderException(what + " must be an absolute URI [" + uri + "]. ");
        }
    }

    protected static final boolean isAbsoluteUri(final String uri) {
        // minimal form s(1):ssp(1)
        if (uri == null || uri.length() < 3) {
            return false;
        }

        try {
            return URI.create(uri).isAbsolute();
        } catch (IllegalArgumentException e) {
            /* ignore */
        }
        return false;
    }

    protected static final void datatype(final String datatype, final BiConsumer<String, String[]> result) {
        if (datatype.startsWith(I18N_BASE)) {

            String[] langDir = datatype.substring(I18N_BASE.length()).split("_");

            result.accept(I18N_BASE, langDir);

            return;
        }
        result.accept(datatype, null);
    }
}
