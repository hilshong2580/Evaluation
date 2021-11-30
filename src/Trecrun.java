public class Trecrun {

    private String identifier;
    private int rank;
    private double score;

    public Trecrun(String identifier, int rank, double score) {
        this.identifier = identifier;
        this.rank = rank;
        this.score = score;
    }


    public String getIdentifier() {
        return identifier;
    }

    public int getRank() {
        return rank;
    }

    public double getScore() {
        return score;
    }
}
