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
package com.apicatalog.rdf.io.nquad;

import java.io.Reader;
import java.net.URI;
import java.util.Arrays;
import java.util.function.BiConsumer;

import com.apicatalog.rdf.api.RdfConsumerException;
import com.apicatalog.rdf.api.RdfQuadConsumer;
import com.apicatalog.rdf.io.error.RdfReaderException;
import com.apicatalog.rdf.io.nquad.Tokenizer.Token;
import com.apicatalog.rdf.io.nquad.Tokenizer.TokenType;
import com.apicatalog.rdf.lang.RdfConstants;
import com.apicatalog.rdf.lang.XsdConstants;

/**
 *
 * @see <a href="https://www.w3.org/TR/n-quads/">RDF 1.1. N-Quads</a>
 *
 */
public class NQuadsReader {

    protected final Tokenizer tokenizer;

    // runtime state
    protected String ltObject;
    protected String ltDatatype;
    protected String ltLangTag;
    protected String ltDirection;

    public NQuadsReader(final Reader reader) {
        this.tokenizer = new Tokenizer(reader);
    }

    public void read(RdfQuadConsumer consumer) throws RdfReaderException {
        try {
            while (tokenizer.hasNext()) {

                // skip EOL and whitespace
                if (tokenizer.accept(Tokenizer.TokenType.END_OF_LINE)
                        || tokenizer.accept(Tokenizer.TokenType.WHITE_SPACE)
                        || tokenizer.accept(Tokenizer.TokenType.COMMENT)) {

                    continue;
                }

                reaStatement(consumer);
            }
        } catch (RdfConsumerException e) {
            if (e.getCause() instanceof RdfReaderException) {
                throw (RdfReaderException) e.getCause();
            }
            throw new RdfReaderException(e);
        }
    }

    protected void reaStatement(RdfQuadConsumer consumer) throws RdfReaderException, RdfConsumerException {

        String subject = readResource("Subject");

        skipWhitespace(0);

        String predicate = readResource("Predicate");

        skipWhitespace(0);

        readObject();

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

    protected String readResource(String name) throws RdfReaderException {

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

    protected void readObject() throws RdfReaderException {

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

        readLiteral();
    }

    protected void readLiteral() throws RdfReaderException {

        Token value = tokenizer.token();

        if (TokenType.STRING_LITERAL_QUOTE != value.getType()) {
            unexpected(value);
        }

        tokenizer.next();

        skipWhitespace(0);

        if (TokenType.LANGTAG == tokenizer.token().getType()) {

            String langTag = tokenizer.token().getValue();

            tokenizer.next();

            this.ltObject = value.getValue();
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

                this.ltObject = value.getValue();

                readDatatype(datatype, (a, b) -> {
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

        this.ltObject = value.getValue();
        this.ltDatatype = XsdConstants.STRING;
    }

    protected static final <T> T unexpected(Token token, TokenType... types) throws RdfReaderException {
        throw new RdfReaderException(
                "Unexpected token " + token.getType() + (token.getValue() != null ? "[" + token.getValue() + "]" : "") + ". "
                        + "Expected one of " + Arrays.toString(types) + ".");
    }

    protected void skipWhitespace(int min) throws RdfReaderException {

        int count = 0;

        while (tokenizer.accept(TokenType.WHITE_SPACE)) {
            count++;
        }

        if (count < min) {
            unexpected(tokenizer.token());
        }
    }

    protected static final void assertAbsoluteIri(final String uri, final String what) throws RdfReaderException {
        if (!isAbsoluteUri(uri)) {
            throw new RdfReaderException(what + " must be an absolute URI [" + uri + "]. ");
        }
    }

    protected static final boolean isAbsoluteUri(final String uri) {

        if (uri == null
                || uri.length() < 3 // minimal form s(1):ssp(1)
        ) {
            return false;
        }

        try {
            return URI.create(uri).isAbsolute();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    protected static final void readDatatype(String datatype, BiConsumer<String, String[]> result) {
        if (datatype.startsWith(RdfConstants.I18N_BASE)) {

            String[] langDir = datatype.substring(RdfConstants.I18N_BASE.length()).split("_");

            result.accept(RdfConstants.I18N_BASE, langDir);

            return;
        }
        result.accept(datatype, null);
    }
}
