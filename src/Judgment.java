public class Judgment {
    private String identifier;
    private int relevance;

    public Judgment( String identifier, int relevance ) {
        this.identifier = identifier;
        this.relevance = relevance;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public int getRelevance() {
        return relevance;
    }

    public void setRelevance(int relevance) {
        this.relevance = relevance;
    }
}
