package evaluation;

/**
 * This class represents a document returned by a retrieval
 * system.  Better to combine the two into a single structure?
 */

public class Document {
     
    public Document( String documentName, int rank, double score ) {
        this.documentName = documentName;
        this.rank = rank;
        this.score = score;
    }
    
    /**
     * @param documentNumber The document identifier.
     */
    
    public Document( String documentName ) {
        this.documentName = documentName;
        this.rank = Integer.MAX_VALUE;
        this.score = Double.NEGATIVE_INFINITY;
    }
    
    /** The rank of the document in a retrieved ranked list. */
    public int rank;
    /** The document identifier. */
    public String documentName;
    /** The score given to this document by the retrieval system. */
    public double score;
}