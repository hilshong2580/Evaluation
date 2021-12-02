import java.util.ArrayList;
import java.util.List;

public class Retrieval {
    private List<Trecrun> ranking;
    private List<Judgment> judgments;


    public Retrieval(List<Trecrun> ranking, List<Judgment> judgments) {
        this.ranking = ranking;
        this.judgments = judgments;
    }

    public List<Trecrun> getRanking() {
        return ranking;
    }

    public void setRanking(List<Trecrun> ranking) {
        this.ranking = ranking;
    }

    public List<Judgment> getJudgments() {
        return judgments;
    }

    public void setJudgments(List<Judgment> judgments) {
        this.judgments = judgments;
    }

    public String printSize() {
        return "ranking's size: " + ranking.size() + ", judgment's size: " + judgments.size();
    }
}
