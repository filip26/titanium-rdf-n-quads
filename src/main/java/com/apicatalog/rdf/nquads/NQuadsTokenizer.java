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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

/**
 * A tokenizer for parsing N-Quads data streams according to the
 * <a href="https://www.w3.org/TR/n-quads/#sec-grammar">N-Quads Grammar</a>.
 * <p>
 * This class reads an input stream character by character and extracts tokens
 * representing RDF terms, literals, and syntax elements in the N-Quads format.
 * It supports whitespace handling, comments, and various escape sequences.
 * </p>
 * 
 * <p>
 * Usage example:
 * </p>
 * 
 * <pre>
 * Reader reader = new FileReader("data.nq");
 * NQuadsTokenizer tokenizer = new NQuadsTokenizer(reader);
 * while (tokenizer.hasNext()) {
 *     Token token = tokenizer.next();
 *     System.out.println(token);
 * }
 * </pre>
 *
 * @see <a href="https://www.w3.org/TR/n-quads/">RDF 1.1 N-Quads
 *      Specification</a>
 */
public class NQuadsTokenizer implements Closeable {

    public static final int DEFAULT_BUFFER_SIZE = 8192 * 2;

    protected final Reader reader;
    protected Token next;

    protected final StringBuilder builder = new StringBuilder(256);

    public NQuadsTokenizer(Reader reader) {
        this.reader = new BufferedReader(reader, DEFAULT_BUFFER_SIZE);
        this.next = null;
    }

    public NQuadsTokenizer(Reader reader, int bufferSize) {
        this.reader = new BufferedReader(reader, bufferSize);
        this.next = null;
    }

    public boolean hasNext() throws NQuadsReaderException, IOException {
        if (next == null) {
            next = doRead();
        }
        return TokenType.END_OF_INPUT != next.type();
    }

    public Token next() throws NQuadsReaderException, IOException {

        if (!hasNext()) {
            return next;
        }

        next = doRead();
        return next;
    }

    public Token token() throws NQuadsReaderException, IOException {
        hasNext();
        return next;
    }

    public boolean accept(TokenType type) throws NQuadsReaderException, IOException {
        if (type == token().type()) {
            next();
            return true;
        }
        return false;
    }

    protected Token doRead() throws NQuadsReaderException, IOException {

        int ch = reader.read();

        if (ch == -1) {
            return Token.EOI;
        }

        // WS
        if (NQuadsAlphabet.WHITESPACE.test(ch)) {
            return skipWhitespaces();
        }

        if (NQuadsAlphabet.EOL.test(ch)) {
            return skipEol();
        }

        return switch (ch) {
        case '#' -> readComment();
        case '<' -> readIriRef();
        case '"' -> readString();
        case '.' -> Token.EOS;
        case '_' -> readBlankNode();
        case '^' -> {
            ch = reader.read();

            if ('^' != ch) {
                unexpected(ch, "^^");
            }

            yield Token.LITERAL_DATA_TYPE;
        }
        case '@' -> readLanguageTag();
        case '-' -> {
            ch = reader.read();
            if ('-' != ch) {
                unexpected(ch, "--");
            }
            yield readDirection();
        }
        default -> {
            unexpected(ch, "\\t", "\\n", "\\r", "^", "@", "SPACE", ".", "<", "_", "\"", "#");
            yield null;
        }
        };
    }

    protected static final void unexpected(int actual, String... expected) throws NQuadsReaderException {
        throw new NQuadsReaderException(
                actual != -1
                        ? "Unexpected character [" + (char) actual + "] expected " + Arrays.toString(expected) + "."
                        : "Unexpected end of input, expected " + Arrays.toString(expected) + ".");
    }

    protected Token skipWhitespaces() throws NQuadsReaderException, IOException {
        reader.mark(1);
        int ch = reader.read();

        while (NQuadsAlphabet.WHITESPACE.test(ch)) {
            reader.mark(1);
            ch = reader.read();
        }

        reader.reset();

        return Token.WS;
    }

    protected Token skipEol() throws NQuadsReaderException, IOException {
        reader.mark(1);
        int ch = reader.read();

        while (NQuadsAlphabet.EOL.test(ch)) {
            reader.mark(1);
            ch = reader.read();
        }

        reader.reset();

        return Token.EOL;
    }

    protected Token readIriRef() throws NQuadsReaderException, IOException {

        builder.setLength(0);

        int ch = reader.read();

        while (ch != '>' && ch != -1) {

            if ((0x00 <= ch && ch <= 0x20)
                    || ch == '<'
                    || ch == '"'
                    || ch == '{'
                    || ch == '}'
                    || ch == '|'
                    || ch == '^'
                    || ch == '`') {
                unexpected(ch, ">");
            }

            if (ch == '\\') {

                readIriEscape(builder);

            } else {
                builder.append((char) ch);
            }
            ch = reader.read();
        }

        if (ch == -1) {
            unexpected(ch);
        }

        return new Token(TokenType.IRI_REF, builder.toString());

    }

    protected Token readString() throws NQuadsReaderException, IOException {
        builder.setLength(0);

        int ch = reader.read();

        while (ch != '"' && ch != -1) {

            if (ch == 0xa || ch == 0xd) {
                unexpected(ch);
            }

            if (ch == '\\') {

                readEscape(builder);

            } else {
                builder.appendCodePoint(ch);
            }
            ch = reader.read();
        }

        if (ch == -1) {
            unexpected(ch);
        }

        return new Token(TokenType.STRING_LITERAL_QUOTE, builder.toString());
    }

    protected Token readLanguageTag() throws NQuadsReaderException, IOException {
        builder.setLength(0);

        int ch = reader.read();

        if (!NQuadsAlphabet.ASCII_ALPHA.test(ch) || ch == -1) {
            unexpected(ch);
        }
        builder.append((char) ch);

        reader.mark(1);
        ch = reader.read();

        // language tag [a-zA-Z]+
        while (NQuadsAlphabet.ASCII_ALPHA.test(ch)) {

            builder.append((char) ch);

            reader.mark(1);
            ch = reader.read();
        }

        if (ch == -1) {
            unexpected(ch);
        }

        // ('-' [a-zA-Z0-9]+)*
        if (ch == '-') {

            reader.reset();
            reader.mark(2);
            reader.read();

            ch = reader.read();

            if (NQuadsAlphabet.ASCII_ALPHA.test(ch)) {
                builder.append('-');

                do {
                    builder.append((char) ch);

                    reader.mark(1);
                    ch = reader.read();

                } while (NQuadsAlphabet.ASCII_ALPHA_NUM.test(ch));

                if (ch == -1) {
                    unexpected(ch);
                }
            }
        }

        reader.reset();

        return new Token(TokenType.LANGUAGE_TAG, builder.toString());
    }

    // ('--' [a-zA-Z]+)?
    protected Token readDirection() throws NQuadsReaderException, IOException {
        int ch = reader.read();

        if (!NQuadsAlphabet.ASCII_ALPHA.test(ch) || ch == -1) {
            unexpected(ch, "[a-zA-Z]+");
        }

        builder.setLength(0);

        do {
            builder.append((char) ch);

            reader.mark(1);
            ch = reader.read();

        } while (NQuadsAlphabet.ASCII_ALPHA.test(ch) || builder.length() < 3);

        if (ch == -1) {
            unexpected(ch);
        }

        if (!"ltr".equals(builder.toString()) && "rtl".equals(builder.toString())) {
            unexpected(ch, "ltr|rtl");
        }

        reader.reset();

        return new Token(TokenType.DIRECTION, builder.toString());
    }

    protected void readIriEscape(final StringBuilder value) throws NQuadsReaderException, IOException {
        int ch = reader.read();

        if (ch == 'u') {

            value.append(readUnicode());

        } else if (ch == 'U') {

            value.append(readUnicode64());

        } else {
            unexpected(ch);
        }
    }

    protected void readEscape(final StringBuilder value) throws NQuadsReaderException, IOException {
        int ch = reader.read();

        if (ch == 't' || ch == 'b' || ch == 'n' || ch == 'r' || ch == 'f' || ch == '\'' || ch == '\\' || ch == '"') {

            value.appendCodePoint(unescape(ch));

        } else if (ch == 'u') {

            value.append(readUnicode());

        } else if (ch == 'U') {

            value.append(readUnicode64());

        } else {
            unexpected(ch);
        }
    }

    protected Token readBlankNode() throws NQuadsReaderException, IOException {
        builder.setLength(0);

        int ch = reader.read();

        if (ch != ':') {
            unexpected(ch);
        }

        ch = reader.read();

        if (NQuadsAlphabet.PN_CHARS_U.negate().and(NQuadsAlphabet.ASCII_DIGIT.negate()).test(ch) || ch == -1) {
            unexpected(ch);
        }

        builder.append((char) ch);

        reader.mark(1);
        ch = reader.read();

        boolean delim = false;

        while (NQuadsAlphabet.PN_CHARS.test(ch) || ch == '.') {

            delim = ch == '.';

            builder.append((char) ch);

            if (delim) {
                reader.reset();
                reader.mark(2);
                reader.skip(1);
            } else {
                reader.mark(1);
            }

            ch = reader.read();

        }

        if (ch == -1) {
            unexpected(ch);
        }

        reader.reset();

        if (delim && builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }

        return new Token(TokenType.BLANK_NODE_LABEL, builder.toString());
    }

    protected char[] readUnicode() throws NQuadsReaderException, IOException {
        return Character.toChars(Integer.parseInt(String.valueOf(
                new char[] {
                        readHex8(),
                        readHex8(),
                        readHex8(),
                        readHex8()
                }), 16));
    }

    protected char readHex8() throws NQuadsReaderException, IOException {

        int hex = reader.read();

        if (NQuadsAlphabet.HEX.negate().test(hex)) {
            unexpected(hex, "0-9", "a-f", "A-F");
        }
        return (char) hex;
    }

    protected char[] readUnicode64() throws NQuadsReaderException, IOException {

        char[] code = new char[8];

        for (int i = 0; i < code.length; i++) {
            code[i] = readHex8();
        }

        return Character.toChars(Integer.parseInt(String.valueOf(code), 16));
    }

    protected static final int unescape(int symbol) {
        if (symbol == 't') {
            return 0x9;

        } else if (symbol == 'b') {
            return 0x8;

        } else if (symbol == 'n') {
            return 0xa;

        } else if (symbol == 'r') {
            return 0xd;

        } else if (symbol == 'f') {
            return 0xc;
        }
        return symbol;
    }

    protected Token readComment() throws NQuadsReaderException, IOException {
        builder.setLength(0);

        int ch = reader.read();

        while (NQuadsAlphabet.EOL.negate().test(ch) && ch != -1) {

            builder.appendCodePoint(ch);
            ch = reader.read();
        }

        return new Token(TokenType.COMMENT, builder.toString());
    }

    public record Token(TokenType type, String value) {

        static final Token EOI = new Token(TokenType.END_OF_INPUT, null);
        static final Token EOS = new Token(TokenType.END_OF_STATEMENT, null);
        static final Token EOL = new Token(TokenType.END_OF_LINE, null);
        static final Token WS = new Token(TokenType.WHITE_SPACE, null);
        static final Token LITERAL_DATA_TYPE = new Token(TokenType.LITERAL_DATA_TYPE, null);

        @Override
        public String toString() {
            return "Token [type=" + type + ", value=" + value + "]";
        }
    }

    public enum TokenType {
        LANGUAGE_TAG,
        DIRECTION,
        IRI_REF,
        STRING_LITERAL_QUOTE,
        BLANK_NODE_LABEL,
        WHITE_SPACE,
        LITERAL_DATA_TYPE,
        COMMENT,
        END_OF_STATEMENT,
        END_OF_LINE,
        END_OF_INPUT,
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
