package evaluation;

/**
 * This class represents a relevance judgment of a particular document
 * for a specific query.
 */

public class Judgment {
    /**
     * @param documentNumber The document identifier.
     * @param judgment The relevance judgment for this document, where positive values mean relevant, and zero means not relevant.
     */
    public Judgment( String documentName, int judgment ) {
        this.documentName = documentName;
        this.judgment = judgment;
    }
    
    /** The document identifier. */
    public String documentName;
    /** The relevance judgment for this document, where positive values mean relevant, and zero means not relevant. */
    public int judgment;
}