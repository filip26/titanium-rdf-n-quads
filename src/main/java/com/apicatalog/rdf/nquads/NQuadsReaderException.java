package com.apicatalog.rdf.nquads;

/**
 * Exception thrown to indicate an error occurred while parsing N-Quads.
 */
public class NQuadsReaderException extends Exception {

    private static final long serialVersionUID = 767977814295891581L;

    private final int lineNumber;
    private final int columnNumber;

    public NQuadsReaderException(String message) {
        this(message, -1, -1);
    }

    public NQuadsReaderException(String message, int lineNumber, int columnNumber) {
        super(formatMessage(message, lineNumber, columnNumber));
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    private static String formatMessage(String message, int line, int col) {
        return (line == -1 && col == -1) ? message : String.format("%s at line %d, column %d", message, line, col);
    }
}
