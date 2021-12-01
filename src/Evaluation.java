import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class Evaluation {
    Map<String, List<Judgment>> qrels = new HashMap<>();
    Map<String, List<Trecrun>> trecrun = new HashMap<>();

    public void loadQrels(String filename){
        String getLine = null;
        String check = null;
        String index = null;
        String identifier = null;
        int relevance = -1;
        List<Judgment> indexJudgments = null;

        try {
            BufferedReader inLine = new BufferedReader(new FileReader(filename));
            while ((getLine = inLine.readLine()) != null) {
                String[] columns = getLine.split("\\s+");
                index = columns[0];
                identifier = columns[2];
                relevance = Integer.valueOf(columns[3]);
                Judgment judgment = new Judgment(identifier, relevance);
                if (check == null || !check.equals(index)) {
                    if (!qrels.containsKey(index)) {
                        qrels.put(index, new ArrayList<Judgment>());
                    }
                    indexJudgments = qrels.get(index);
                    check = index;
                }
                indexJudgments.add(judgment);
            }
            inLine.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Evaluation evaluation = new Evaluation();
        //System.out.println(evaluation.loadJudgments("qrels"));
        evaluation.loadQrels("qrels");
    }
}
