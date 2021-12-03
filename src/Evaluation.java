import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Evaluation {

    // Load and store the given qrels file and trecrun file
    Map<String, List<Judgment>> qrels = new HashMap<>();
    Map<String, List<Trecrun>> trecruns = new HashMap<>();
    TreeSet<String> indexSet = new TreeSet<>();

    //Store the loaded data in different fields
    Map<String, ArrayList<Trecrun>> retrieved = new HashMap<>();
    Map<String, ArrayList<Trecrun>> relevant = new HashMap<>();
    Map<String, ArrayList<Trecrun>> relevantRetrieved = new HashMap<>();
    Map<String, Map<String, Judgment>> judgments = new HashMap<>();

    public void resetVariables() {
        this.qrels = new HashMap<>();
        this.trecruns = new HashMap<>();
        this.indexSet = new TreeSet<>();

        this.retrieved = new HashMap<>();
        this.relevant = new HashMap<>();
        this.relevantRetrieved = new HashMap<>();
        this.judgments = new HashMap<>();
    }

    //load the qrels content to qrels Map
    public void loadQrels(String filename) {
        String check = null;
        List<Judgment> indexJudgments = null;
        try {
            BufferedReader inLine = new BufferedReader(new FileReader(filename));
            String getLine = null;
            while ((getLine = inLine.readLine()) != null) {
                String[] columns = getLine.split("\\s+");

                Judgment judgment = new Judgment(columns[2], Integer.valueOf(columns[3]));
                if (check == null || !check.equals(columns[0])) {
                    if (!qrels.containsKey(columns[0])) {
                        qrels.put(columns[0], new ArrayList<Judgment>());
                    }
                    check = columns[0];
                    indexJudgments = qrels.get(columns[0]);
                }
                indexJudgments.add(judgment);

            }
            inLine.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //load the trecrun content to trecrun map
    public void loadTrecrun(String filename) {
        String check = null;
        List<Trecrun> indexTrecrun = null;
        try {
            BufferedReader inLine = new BufferedReader(new FileReader(filename));
            String getLine = null;
            while ((getLine = inLine.readLine()) != null) {
                String[] columns = getLine.split("\\s+");
                Trecrun trecrun = new Trecrun(columns[2], Integer.valueOf(columns[3]), Double.valueOf(columns[4]));

                if (check == null || !check.equals(columns[0])) {
                    if (!trecruns.containsKey(columns[0])) {
                        trecruns.put(columns[0], new ArrayList<Trecrun>());
                    }
                    check = columns[0];
                    indexTrecrun = trecruns.get(columns[0]);
                }
                indexTrecrun.add(trecrun);
            }
            inLine.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //update the qrels map and trecrun map
    public void setRetrieval(String qrel, String trecrun) {
        loadQrels(qrel);
        loadTrecrun(trecrun);

        for (String index : trecruns.keySet()) {
            List<Judgment> judgments = qrels.get(index);
            List<Trecrun> rank = trecruns.get(index);

            //sort and update the rank by score
            sortRankingScore(rank);
            setRanking(rank);

            if (judgments == null)
                continue;

            //update the list of qrels and list of trecruns content
            qrels.put(index, judgments);
            trecruns.put(index, rank);

            //save the index in order
            indexSet.add(index);
        }
    }

    //sort the list of trecrun by score
    public void sortRankingScore(List<Trecrun> rank) {
        Collections.sort(rank, new Comparator<Trecrun>() {
            public int compare(Trecrun o1, Trecrun o2) {
                if (o1.getScore() < o2.getScore()) return 1;
                if (o1.getScore() == o2.getScore()) return 0;
                return -1;
            }
        });
    }

    //update the list of trecrun's ranking by score
    public void setRanking(List<Trecrun> rank) {
        int index = 1;
        for (Trecrun t : rank) {
            int x = index++;
            t.setRank(x);
        }
    }

    // update the content for retrieved, relevant, judgments and relevantRetrieved
    public void convertRetrieval() {
        for (String index : indexSet) {

            /////////////  set retrieved by trecruns  /////////////
            retrieved.put(index, new ArrayList<Trecrun>(trecruns.get(index)));
            ///////////// end of set retrieved ////////////////////


            /////////////  set relevant and judgments by qrels /////////////
            Map<String, Judgment> tempJudgment = new HashMap<>();
            ArrayList<Trecrun> tempRelevant = new ArrayList<Trecrun>();
            for (Judgment j : qrels.get(index))
                if ((j.getRelevance() > 0) && (j != null)) {
                    tempJudgment.put(j.getIdentifier(), j);
                    tempRelevant.add(new Trecrun(j.getIdentifier(), 0, 0.0));
                }
            judgments.put(index, tempJudgment);
            relevant.put(index, tempRelevant);
            ///////////// end of set relevant and judgments ////////////////////


            /////////////  set relevantRetrieved by retrieved and relevant /////////////
            ArrayList<Trecrun> tempRelevantRetrieved = new ArrayList<Trecrun>();
            for (Trecrun t : retrieved.get(index)) {
                Judgment judgment = judgments.get(index).get(t.getIdentifier());
                if ((judgment != null) && (judgment.getRelevance() > 0))
                    tempRelevantRetrieved.add(t);
            }
            relevantRetrieved.put(index, tempRelevantRetrieved);
            ///////////// end of set relevantRetrieved ////////////////////
        }
    }

    //return qrel's index size
    public int retrievalSize() {
        return indexSet.size();
    }

    //return Retrieved size
    public int retrievedSize() {
        return retrieved.values().stream().mapToInt(Collection::size).sum();
    }

    //return Relevant size
    public int relevantSize() {
        return relevant.values().stream().mapToInt(Collection::size).sum();
    }

    //return Relevant-Retrieved size
    public int relevantRetrievedSize() {
        return relevantRetrieved.values().stream().mapToInt(Collection::size).sum();
    }

    //return mean of reciprocal rank
    public double MRR() {
        return relevantRetrieved.values().stream().mapToDouble(e -> {
            if (e.size() != 0)  return 1.0 / e.get(0).getRank();
            else    return 0.0;
        }).sum() / indexSet.size();
    }

    // return reciprocal rank
    public double RR(String index) {
        if (relevantRetrieved.get(index).size() == 0)
            return 0;
        return 1.0 / (double) relevantRetrieved.get(index).get(0).getRank();
    }

    public double calculate_AP_Recall(int num, String index){

        double total = 0.0;
        if (relevantRetrieved.get(index).size() == 0) {
            total = 0;
        } else if (relevantRetrieved.get(index).get(0).getRank() > num) {
            total = 0;
        } else if (relevantRetrieved.get(index).get(relevantRetrieved.get(index).size() - 1).getRank() <= num) {
            total = (double) relevantRetrieved.get(index).size();
        } else {
            int min = 0, max = 0;
            for (int i = 0; i < relevantRetrieved.get(index).size(); i++) {
                if (relevantRetrieved.get(index).get(i).getRank() == num) {
                    total = (double) (i + 1);
                    break;
                }
                if (relevantRetrieved.get(index).get(min).getRank() <= num) {
                    min = i;
                }

                if (relevantRetrieved.get(index).get(i).getRank() > num && max == 0) {
                    max = i;
                }
            }
            if (total == 0) {
                assert relevantRetrieved.get(index).get(min).getRank() <= num &&
                        relevantRetrieved.get(index).get(max).getRank() > num;
                total = (double) min;
            }

        }
        return total;
    }

    public double precision(int num, String index) {
        return calculate_AP_Recall(num, index) / num;
    }

    public double meanPrecision(int num) { return indexSet.stream().mapToDouble(index -> precision(num, index)).sum() / indexSet.size(); }

    public double recall(int num, String index) { return calculate_AP_Recall(num, index) / relevant.get(index).size(); }

    public double meanRecall(int num) { return indexSet.stream().mapToDouble(index -> recall(num, index)).sum() / indexSet.size();    }

    //return f1
    public double F1(int num, String index) {
        double r = recall(num, index);
        double p = precision(num, index);

        if (((2.0 * r * p) != 0) && (r + p != 0))
            return (2.0 * r * p) / (r + p);
        return 0.0;
    }

    public double meanF1(int num) { return indexSet.stream().mapToDouble(index -> F1(num, index)).sum() / indexSet.size(); }

    //return AP
    public double AP(String index) {
        double sum = 0.0, count = 1.0;
        for (Trecrun t : relevantRetrieved.get(index)) {
            sum += count / (double) t.getRank();
            count++;
        }
        return sum / relevant.get(index).size();
    }

    public double meanAP() { return indexSet.stream().mapToDouble(index -> AP(index)).sum() / indexSet.size(); }

    public double NDCG(int num, String index) {
        double ndcg = 0.0, dcg = 0.0, iDcg = 0.0;

        //calculate the dcg
        for (int i = 0; i < num; i++)
            if (retrieved.get(index) != null) {
                String query = retrieved.get(index).get(i).getIdentifier();
                if (judgments.get(index).get(query) != null && judgments.get(index).get(query).getRelevance() > 0)
                    dcg += (Math.pow(2, judgments.get(index).get(query).getRelevance()) - 1.0) / (Math.log(1 + retrieved.get(index).get(i).getRank()) / Math.log(2));
                if (retrieved.get(index).size() - 1 == i)  i = num;
            }

        //return 0 if dcg is 0
        if (dcg == 0.0)  return dcg;

        ArrayList<Integer> sorted = new ArrayList<>();

        //put the relevance to the list, then sort it later
        for (String str : judgments.get(index).keySet())
            sorted.add(judgments.get(index).get(str).getRelevance());
        Collections.sort(sorted, Collections.reverseOrder());

        //set the number of for-loop size to be the smallest value between param num and relevant size
        if (num > relevant.get(index).size())
            num = relevant.get(index).size();

        int process = 0, count = 0;

        //calculate the iDcg value
        for (int i = 0; i < num; i++) {
            iDcg += ((Math.pow(2, sorted.get(count)) - 1.0) / (Math.log(2 + i + process) / Math.log(2)));
            if (i < num && count + 1 < sorted.size())
                if (sorted.get(count) != sorted.get(count + 1)) {
                    process = i + 1;
                    num -= process;
                    i = -1;
                }
            count++;
        }

        return dcg / iDcg;
    }

    //return the mean of NDCG
    public double meanNDCG(int index) {  return indexSet.stream().mapToDouble(str -> NDCG(index, str)).sum() / indexSet.size(); }
    
    public String summaryEvaluation(boolean index) {
        StringWriter s = new StringWriter();
        PrintWriter out = new PrintWriter(s);
        String formatString = "%2$-25s\t%1$5s\t";

        // print summary data
        out.format(formatString + "%3$6d\n", "all", "num_q", retrievalSize());
        out.format(formatString + "%3$6d\n", "all", "num_ret", retrievedSize());
        out.format(formatString + "%3$6d\n", "all", "num_rel", relevantSize());
        out.format(formatString + "%3$6d\n", "all", "num_rel_ret", relevantRetrievedSize());
        out.format(formatString + "%3$6.4f\n", "all", "NDCG@15", meanNDCG(15)); //still not finish yet
        out.format(formatString + "%3$6.4f\n", "all", "MRR", MRR());
        out.format(formatString + "%3$6.4f\n", "all", "P@5", meanPrecision(5));
        out.format(formatString + "%3$6.4f\n", "all", "P@10", meanPrecision(10));
        out.format(formatString + "%3$6.4f\n", "all", "R@10", meanRecall(10));
        out.format(formatString + "%3$6.4f\n", "all", "F1@10", meanF1(10));
        out.format(formatString + "%3$6.4f\n", "all", "MAP", meanAP());


        return s.toString();
    }


    public String singleEvaluation(String index) {
        System.out.println("This is singleEvaluation");
        StringWriter s = new StringWriter();
        PrintWriter out = new PrintWriter(s);
        String formatString = "%2$-25s\t%1$5s\t";
        // print trec_eval relational-style output
        out.format(formatString + "%3$6d\n", index, "num_ret", retrieved.get(index).size());
        out.format(formatString + "%3$6d\n", index, "num_rel", relevant.get(index).size());
        out.format(formatString + "%3$6d\n", index, "num_rel_ret", relevantRetrieved.get(index).size());

        out.format(formatString + "%3$6.4f\n", index, "NDCG@15", NDCG(15, index)); //wrong
        out.format(formatString + "%3$6.4f\n", index, "RR", RR(index));
        out.format(formatString + "%3$6.4f\n", index, "P@5", precision(5, index));
        out.format(formatString + "%3$6.4f\n", index, "P@10", precision(10, index));
        out.format(formatString + "%3$6.4f\n", index, "R@10", recall(10, index));
        out.format(formatString + "%3$6.4f\n", index, "F1@10", F1(10, index));
        out.format(formatString + "%3$6.4f\n", index, "AP", AP(index));
        return s.toString();
    }

    public static void main(String[] args) {
        Evaluation evaluation = new Evaluation();
        evaluation.setRetrieval("qrels", "stress.trecrun");
        evaluation.convertRetrieval();
//        System.out.println(evaluation.singleEvaluation("604"));
        System.out.println("stress ===============================================");
        System.out.println(evaluation.summaryEvaluation(false));

        evaluation = new Evaluation();
        evaluation.setRetrieval("qrels", "bm25.trecrun");
        evaluation.convertRetrieval();
        System.out.println("bm25 ===============================================");
        System.out.println(evaluation.summaryEvaluation(false));

        evaluation = new Evaluation();
        evaluation.setRetrieval("qrels", "ql.trecrun");
        evaluation.convertRetrieval();
        System.out.println("ql ===============================================");
        System.out.println(evaluation.summaryEvaluation(false));

        evaluation = new Evaluation();
        evaluation.setRetrieval("qrels", "sdm.trecrun");
        evaluation.convertRetrieval();
        System.out.println("sdm ===============================================");
        System.out.println(evaluation.summaryEvaluation(false));


    }
}
