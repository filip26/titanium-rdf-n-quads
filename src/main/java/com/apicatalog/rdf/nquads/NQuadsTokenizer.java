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

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.function.IntPredicate;

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
public final class NQuadsTokenizer implements Closeable {

    public enum TokenType {
        LANGUAGE,
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

    private static final int EOF = -1;
    private static final IntPredicate isEOF = ch -> ch == EOF;

    private final Reader reader;
    private Token nextToken;

    private int currentChar;
    private int nextChar;

    private int lineNumber;
    private int columnNumber;

    private final StringBuilder builder = new StringBuilder(256);

    public NQuadsTokenizer(Reader reader) {
        this.reader = reader;
        this.nextToken = null;
        this.currentChar = EOF;
        this.nextChar = EOF;
        this.lineNumber = 0;
        this.columnNumber = 0;
    }

    public boolean hasNext() throws NQuadsReaderException, IOException {
        if (nextToken == null) {
            init();
            nextToken = readToken();
        }
        return TokenType.END_OF_INPUT != nextToken.type();
    }

    public Token next() throws NQuadsReaderException, IOException {

        if (!hasNext()) {
            return nextToken;
        }

        nextToken = readToken();
        return nextToken;
    }

    public Token token() throws NQuadsReaderException, IOException {
        hasNext();
        return nextToken;
    }

    public boolean accept(TokenType type) throws NQuadsReaderException, IOException {

        if (!hasNext()) {
            return false;
        }

        if (type == token().type()) {
            next();
            return true;
        }
        return false;
    }

    private void init() throws IOException {
        // first initialization
        currentChar = reader.read();
        if (currentChar != EOF) {
            nextChar = reader.read();
        }
    }

    private int readChar() throws IOException {
        if (currentChar != EOF) {
            currentChar = nextChar;
            nextChar = reader.read();
            if (NQuadsAlphabet.EOL.test(currentChar)) {
                columnNumber = 0;
                lineNumber++;
            } else {
                columnNumber++;
            }
        }
        return currentChar;
    }

    private Token readToken() throws NQuadsReaderException, IOException {

        if (currentChar == EOF) {
            return Token.EOF;
        }

        // WS
        if (NQuadsAlphabet.WHITESPACE.test(currentChar)) {
            return skipWhitespaces();
        }

        if (NQuadsAlphabet.EOL.test(currentChar)) {
            return skipEol();
        }

        return switch (currentChar) {
        case '.' -> {
            readChar();
            yield Token.EOS;
        }
        case '#' -> readComment();
        case '<' -> readIriRef();
        case '"' -> readString();
        case '_' -> readBlankNode();
        case '^' -> {
            if ('^' != nextChar) {
                throw error(nextChar, "^^");
            }
            readChar();
            readChar();
            yield Token.LITERAL_DATA_TYPE;
        }
        case '@' -> readLanguage();
        case '-' -> {
            if ('-' != nextChar) {
                throw error(nextChar, "--");
            }
            yield readDirection();
        }
        default -> throw error(currentChar, "\\t", "\\n", "\\r", "^", "@", "SPACE", ".", "<", "_", "\"", "#");
        };
    }

    private Token skipWhitespaces() throws NQuadsReaderException, IOException {
        while (NQuadsAlphabet.WHITESPACE.test(readChar()))
            ;
        return Token.WS;
    }

    private Token skipEol() throws NQuadsReaderException, IOException {
        while (NQuadsAlphabet.EOL.test(readChar()))
            ;
        return Token.EOL;
    }

    private Token readComment() throws NQuadsReaderException, IOException {
        builder.setLength(0);

        while (NQuadsAlphabet.EOL.negate()
                .and(isEOF.negate())
                .test(readChar())) {
            builder.appendCodePoint(currentChar);
        }

        return new Token(TokenType.COMMENT, builder.toString());
    }

    private Token readIriRef() throws NQuadsReaderException, IOException {

        builder.setLength(0);

        while (isEOF.negate().and(ch -> ch != '>').test(readChar())) {

            if (NQuadsAlphabet.IRIREF_FORBIDDEN.test(currentChar)) {
                throw error(currentChar, ">");
            }

            if (currentChar == '\\') {
                readIriEscape(builder);

            } else {
                builder.appendCodePoint(currentChar);
            }
        }

        if (currentChar == EOF) {
            throw error(currentChar);
        }

        readChar();

        return new Token(TokenType.IRI_REF, builder.toString());
    }

    private Token readBlankNode() throws NQuadsReaderException, IOException {
        builder.setLength(0);

        if (readChar() != ':') {
            throw error(currentChar);
        }

        if ((NQuadsAlphabet.PN_CHARS_U.negate()
                .and(NQuadsAlphabet.ASCII_DIGIT.negate()))
                .or(isEOF)
                .test(readChar())) {
            throw error(currentChar);
        }

        builder.appendCodePoint(currentChar);

        while (NQuadsAlphabet.PN_CHARS.and(ch -> ch != '.').test(readChar())) {
            if (NQuadsAlphabet.PN_CHARS.negate().test(nextChar) && nextChar != '.') {
                if (currentChar != '.') {
                    builder.appendCodePoint(currentChar);
                    readChar();
                }
                break;
            }
            builder.appendCodePoint(currentChar);
        }

        if (currentChar == EOF) {
            throw error(currentChar);
        }

        return new Token(TokenType.BLANK_NODE_LABEL, builder.toString());
    }

    private Token readString() throws NQuadsReaderException, IOException {
        builder.setLength(0);

        readChar();

        while (currentChar != '"' && currentChar != EOF) {

            if (currentChar == 0xa || currentChar == 0xd) {
                throw error(currentChar);
            }

            if (currentChar == '\\') {
                readEscape(builder);

            } else {
                builder.appendCodePoint(currentChar);
            }
            readChar();
        }

        if (currentChar == EOF) {
            throw error(currentChar);
        }

        readChar();

        return new Token(TokenType.STRING_LITERAL_QUOTE, builder.toString());
    }

    private Token readLanguage() throws NQuadsReaderException, IOException {
        builder.setLength(0);

        if (NQuadsAlphabet.ASCII_ALPHA.negate().or(isEOF).test(readChar())) {
            throw error(currentChar);
        }

        builder.append((char) currentChar);

        // language tag [a-zA-Z]+
        while (NQuadsAlphabet.ASCII_ALPHA.test(readChar())) {
            builder.appendCodePoint(currentChar);
        }

        if (currentChar == EOF) {
            throw error(currentChar);
        }

        // ('-' [a-zA-Z0-9]+)*
        if (currentChar == '-' && nextChar != '-') {

            if (NQuadsAlphabet.ASCII_ALPHA.test(readChar())) {
                builder.append('-');

                do {
                    builder.appendCodePoint(currentChar);

                } while (NQuadsAlphabet.ASCII_ALPHA_NUM.test(readChar()));

                if (currentChar == EOF) {
                    throw error(currentChar);
                }
            }
        }

        return new Token(TokenType.LANGUAGE, builder.toString());
    }

    // ('--' [a-zA-Z]+)?
    private Token readDirection() throws NQuadsReaderException, IOException {

        readChar();

        if (NQuadsAlphabet.ASCII_ALPHA.negate().or(isEOF).test(readChar())) {
            throw error(currentChar, "[a-zA-Z]+");
        }

        builder.setLength(0);

        do {
            builder.appendCodePoint(currentChar);

        } while (NQuadsAlphabet.ASCII_ALPHA.test(readChar()) || builder.length() < 3);

        if (currentChar == EOF) {
            throw error(currentChar);
        }

        var ltDirection = builder.toString();

        if (!"ltr".equals(ltDirection) && !"rtl".equals(ltDirection)) {
            throw error(currentChar, "ltr", "rtl");
        }

        return new Token(TokenType.DIRECTION, ltDirection);
    }

    private void readIriEscape(final StringBuilder value) throws NQuadsReaderException, IOException {
        readChar();

        if (currentChar == 'u') {

            value.append(readUnicode());

        } else if (currentChar == 'U') {

            value.append(readUnicode64());

        } else {
            throw error(currentChar);
        }
    }

    private void readEscape(final StringBuilder value) throws NQuadsReaderException, IOException {
        int ch = readChar();

        if (ch == 't' || ch == 'b' || ch == 'n' || ch == 'r' || ch == 'f' || ch == '\'' || ch == '\\' || ch == '"') {

            value.appendCodePoint(unescape(ch));

        } else if (ch == 'u') {

            value.append(readUnicode());

        } else if (ch == 'U') {

            value.append(readUnicode64());

        } else {
            throw error(ch);
        }
    }

    private char[] readUnicode() throws NQuadsReaderException, IOException {
        return Character.toChars(Integer.parseInt(String.valueOf(
                new char[] {
                        readHex8(),
                        readHex8(),
                        readHex8(),
                        readHex8()
                }), 16));
    }

    private char readHex8() throws NQuadsReaderException, IOException {

        int hex = readChar();

        if (NQuadsAlphabet.HEX.negate().test(hex)) {
            throw error(hex, "0-9", "a-f", "A-F");
        }
        return (char) hex;
    }

    private char[] readUnicode64() throws NQuadsReaderException, IOException {

        char[] code = new char[8];

        for (int i = 0; i < code.length; i++) {
            code[i] = readHex8();
        }

        return Character.toChars(Integer.parseInt(String.valueOf(code), 16));
    }

    private static int unescape(int symbol) {
        return switch (symbol) {
        case 't' -> '\t';
        case 'b' -> '\b';
        case 'n' -> '\n';
        case 'r' -> '\r';
        case 'f' -> '\f';
        default -> symbol;
        };
    }

    private NQuadsReaderException error(int actual, String... expected) throws NQuadsReaderException {
        return new NQuadsReaderException(
                actual != EOF
                        ? "Unexpected character [" + (char) actual + "] expected " + Arrays.toString(expected) + "."
                        : "Unexpected end of input, expected " + Arrays.toString(expected) + ".",
                lineNumber,
                columnNumber);
    }

    public record Token(TokenType type, String value) {

        static final Token EOF = new Token(TokenType.END_OF_INPUT, null);
        static final Token EOS = new Token(TokenType.END_OF_STATEMENT, null);
        static final Token EOL = new Token(TokenType.END_OF_LINE, null);
        static final Token WS = new Token(TokenType.WHITE_SPACE, null);
        static final Token LITERAL_DATA_TYPE = new Token(TokenType.LITERAL_DATA_TYPE, null);

        @Override
        public String toString() {
            return "Token [type=" + type + ", value=" + value + "]";
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
