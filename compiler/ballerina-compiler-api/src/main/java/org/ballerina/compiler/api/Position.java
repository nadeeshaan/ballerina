package org.ballerina.compiler.api;

/**
 * Position in a text document expressed as zero-based line and character offset.
 */
public class Position {
    /**
     * Line position in a document (zero-based).
     */
    private int line;

    /**
     * Character offset on a line in a document (zero-based).
     */
    private int character;

    public Position() {
    }

    public Position(final int line, final int character) {
        this.line = line;
        this.character = character;
    }

    /**
     * Line position in a document (zero-based).
     */
    public int getLine() {
        return this.line;
    }

    /**
     * Line position in a document (zero-based).
     */
    public void setLine(final int line) {
        this.line = line;
    }

    /**
     * Character offset on a line in a document (zero-based).
     */
    public int getCharacter() {
        return this.character;
    }

    /**
     * Character offset on a line in a document (zero-based).
     */
    public void setCharacter(final int character) {
        this.character = character;
    }
}
