import java.io.*;
import java.util.*;

public class Evaluation {

    // Load and store the given qrels file and trecrun file
    TreeMap<String, Retrieval> retrievals = new TreeMap<>();
    Map<String, List<Judgment>> qrels = new HashMap<>();
    Map<String, List<Trecrun>> trecruns = new HashMap<>();

    //Store the loaded data in different fields
    Map<String, ArrayList<Trecrun>> retrieved = new HashMap<>();
    Map<String, ArrayList<Trecrun>> relevant = new HashMap<>();
    Map<String, ArrayList<Trecrun>> relevantRetrieved = new HashMap<>();
    Map<String, Map<String, Judgment>> judgments = new HashMap<>();


    List<String> indexList = new ArrayList<>();

    public void loadQrels(String filename) {
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

    public void loadTrecrun(String filename) {
        String getLine = null;
        String check = null;
        String queryId = null;
        String identifier = null;
        int rank = -1;
        double score = -1.0;
        List<Trecrun> indexTrecrun = null;

        try {
            BufferedReader inLine = new BufferedReader(new FileReader(filename));
            while ((getLine = inLine.readLine()) != null) {
                String[] columns = getLine.split("\\s+");
                queryId = columns[0];
                identifier = columns[2];
                rank = Integer.valueOf(columns[3]);
                score = Double.valueOf(columns[4]);

                Trecrun trecrun = new Trecrun(identifier, rank, score);

                if (check == null || !check.equals(queryId)) {
                    if (!trecruns.containsKey(queryId)) {
                        trecruns.put(queryId, new ArrayList<Trecrun>());
                    }
                    check = queryId;
                    indexTrecrun = trecruns.get(queryId);
                }
                indexTrecrun.add(trecrun);
            }
            inLine.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sortRankingScore(List<Trecrun> rank) {
        Collections.sort(rank, new Comparator<Trecrun>() {
            public int compare(Trecrun o1, Trecrun o2) {
                if (o1.getScore() < o2.getScore()) return 1;
                if (o1.getScore() == o2.getScore()) return 0;
                return -1;
            }
        });
    }

    public void setRanking(List<Trecrun> rank) {
        int index = 1;
        for (Trecrun t : rank) {
            int x = index++;
            t.setRank(x);
        }
    }

    public void setRetrieval(String qrel, String trecrun) {
        loadQrels(qrel);
        loadTrecrun(trecrun);

        for (String index : trecruns.keySet()) {
            List<Judgment> judgments = qrels.get(index);
            List<Trecrun> rank = trecruns.get(index);
            sortRankingScore(rank);
            setRanking(rank);

            if (judgments == null) {
                continue;
            }
            Retrieval retrieval = new Retrieval(rank, judgments);
            retrievals.put(index, retrieval);
        }
    }

    public void convertRetrieval() {
        TreeMap<String, Retrieval> tempRetrieval = retrievals;
        for (String index : retrievals.keySet()) {

            indexList.add(index);

            //set retrieved by Retrieval's ranking
            retrieved.put(index, new ArrayList<Trecrun>(retrievals.get(index).getRanking()));
            ///////////// end of set retrieved ////////////////////

            //set relevant and judgments by Retrieval's judgments
            Map<String, Judgment> tempJudgment = new HashMap<>();
            ArrayList<Trecrun> tempRelevant = new ArrayList<Trecrun>();
            for (Judgment j : retrievals.get(index).getJudgments()) {
                if ((j.getRelevance() > 0) && (j != null)) {
                    tempJudgment.put(j.getIdentifier(), j);
                    tempRelevant.add(new Trecrun(j.getIdentifier(), Integer.MAX_VALUE,
                            Double.NEGATIVE_INFINITY));
                }
            }
            judgments.put(index, tempJudgment);
            relevant.put(index, tempRelevant);
            ///////////// end of set relevant and judgments ////////////////////

            //set relevantRetrieved by retrieved and relevant
            ArrayList<Trecrun> tempRelevantRetrieved = new ArrayList<Trecrun>();
            for (Trecrun t : retrieved.get(index)) {
                Judgment judgment = judgments.get(index).get(t.getIdentifier());
                if ((judgment != null) && (judgment.getRelevance() > 0)) {
                    tempRelevantRetrieved.add(t);
                }
            }
            relevantRetrieved.put(index, tempRelevantRetrieved);
            ///////////// end of set relevantRetrieved ////////////////////

        }
    }

    public int retrievalSize() {
        return retrievals.size();
    }

    public int retrievedSize() {
        int count = 0;
        for (String str : retrieved.keySet())
            count += retrieved.get(str).size();
        return count;
    }

    public int relevantSize() {
        int count = 0;
        for (String str : relevant.keySet())
            count += relevant.get(str).size();
        return count;
    }

    public int relevantRetrievedSize() {
        int count = 0;
        for (String str : relevantRetrieved.keySet())
            count += relevantRetrieved.get(str).size();
        return count;
    }

    public double NDCG(int num) {
        double ndcg = 0;
        if (retrievals.size() == 0) return 0;

        for (String str : retrievals.keySet()) {
            int documentsProcessed = 0;

            TreeMap<Integer, Integer> relevanceCounts = new TreeMap<Integer, Integer>();
            for (String s : judgments.get(str).keySet()) {
                if (judgments.get(str).get(s).getRelevance() == 0)
                    continue;

                int realRelevance = -judgments.get(str).get(s).getRelevance();
                if (!relevanceCounts.containsKey(realRelevance)) {
                    relevanceCounts.put(realRelevance, 0);
                }
                relevanceCounts.put(realRelevance, relevanceCounts.get(realRelevance) + 1);
            }


            for (Integer negativeRelevanceValue : relevanceCounts.keySet()) {
                int relevanceCount = (int) relevanceCounts.get(negativeRelevanceValue);
                int relevanceValue = -negativeRelevanceValue;
                relevanceCount = Math.min(relevanceCount, num - documentsProcessed);

                for (int i = 1; i <= relevanceCount; i++) {
                    ndcg += (Math.pow(2, relevanceValue) - 1.0) / (Math.log(1 + i + documentsProcessed) / Math.log(2));
                }

                documentsProcessed += relevanceCount;
                if (documentsProcessed >= num)
                    break;
            }


        }
        return ndcg / retrievals.size();
    }

    public double MRR() {
        double count = 0;
        if (relevantRetrieved.size() == 0)
            return 0;

        for (String str : relevantRetrieved.keySet()) {
            if (relevantRetrieved.get(str).size() == 0)
                count += 0;
            else
                count += (1.0 / (double) relevantRetrieved.get(str).get(0).getRank());
        }

        System.out.println("count: " + count);
        System.out.println("retrievals.size(): " + retrievals.size());
        return count / retrievals.size();
    }

    public double meanPrecision(int num) {
        double count = 0.0;
//        if (retrievals.size() == 0) return 0;
//
//        for (String str : relevantRetrieved.keySet()) {
//            double total = 0.0;
//            if (relevantRetrieved.get(str).size() == 0 || relevant.get(str).size() == 0)
//                total= 0;
//            else if (relevantRetrieved.get(str).get(0).getRank() > num)
//                total= 0;
//            else if (relevantRetrieved.get(str).get(relevantRetrieved.get(str).size()-1).getRank() <= num)
//                total= (double) relevantRetrieved.get(str).size();
//            else {
//                for (int i = 0; i < relevantRetrieved.get(str).size(); i++) {
//                    if (relevantRetrieved.get(str).get(i).getRank() == num) {
//                        total= (double) (i+1);
//                        break;
//                    }
//                }
//            }
//            count+=total / (double) relevant.get(str).size();
//        }
//
//        System.out.println("count: " + count);
//        System.out.println("retrievals.size(): " + retrievals.size());

        for (String str : relevantRetrieved.keySet()) {
            double total = 0.0;


            if (relevantRetrieved.get(str).size() == 0)
                total += 0;
            else {
                int low = 0;
                int high = relevantRetrieved.get(str).size() - 1;

                Trecrun lastRelevant = relevantRetrieved.get(str).get(high);
                Trecrun firstRelevant = relevantRetrieved.get(str).get(low);

                if (lastRelevant.getRank() <= num)
                    total += relevantRetrieved.get(str).size();

                else if (firstRelevant.getRank() > num)
                    total += 0;
                else if (relevantRetrieved.get(str).get(low).getRank() <= num &&
                        relevantRetrieved.get(str).get(high).getRank() > num) {
                    total += low + 1;
                } else {
                    while ((high - low) >= 2) {
                        int middle = low + (high - low) / 2;
                        Trecrun middleDocument = relevantRetrieved.get(str).get(middle);

                        if (middleDocument.getRank() == num)
                            total += middle + 1;
                        else if (middleDocument.getRank() > num)
                            high = middle;
                        else
                            low = middle;
                    }

                }
            }

            count += total / (double) relevant.get(str).size();
        }

        System.out.println("count: " + count);
        System.out.println("retrievals.size(): " + retrievals.size());
        return count / retrievals.size();
    }

    public String summaryEvaluation(boolean index) {
        StringWriter s = new StringWriter();
        PrintWriter out = new PrintWriter(s);
        String formatString = "%2$-25s\t%1$5s\t";

        // print summary data
        out.format(formatString + "%3$6d\n", "all", "num_q", retrievalSize());
        out.format(formatString + "%3$6d\n", "all", "num_ret", retrievedSize());
        out.format(formatString + "%3$6d\n", "all", "num_rel", relevantSize());
        out.format(formatString + "%3$6d\n", "all", "num_rel_ret", relevantRetrievedSize());
        out.format(formatString + "%3$6.4f\n", "all", "NDCG@15", NDCG(15)); //still not finish yet
        out.format(formatString + "%3$6.4f\n", "all", "MRR", MRR());
        out.format(formatString + "%3$6.4f\n", "all", "P@5", meanPrecision(5));
//        out.format( formatString + "%3$6.4f\n",   "all", "P@10",         setEvaluator.meanPrecision(10) );
//        out.format( formatString + "%3$6.4f\n",   "all", "R@10",         setEvaluator.meanRecall(10) );
//        out.format( formatString + "%3$6.4f\n",   "all", "F1@10",         setEvaluator.meanF1(10) );
//        out.format( formatString + "%3$6.4f\n",   "all", "MAP",         setEvaluator.meanAveragePrecision() );


        return s.toString();
    }

    public double RR(String index) {
        if (relevantRetrieved.get(index).size() == 0)
            return 0;

        return 1.0 / (double) relevantRetrieved.get(index).get(0).getRank();
    }

    public double singlePrecision(int num, String index) {
        double count = 0.0;
        if (num == 0) return 0;

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
        count += total / (double) num;
        return count;
    }

    public double recallPrecision(int num, String index) {

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
//                assert relevantRetrieved.get(index).get(min).getRank() <= num &&
//                        relevantRetrieved.get(index).get(max).getRank() > num;
                total = (double) min;
            }

        }
        return total / (double) relevant.get(index).size();
    }

//    public double F1(int num, String index){
//
//        return (2.0 * recallPrecision(num, index) * p)/(recallPrecision(num, index) + p);
//    }
    public String singleEvaluation(String index) {
        System.out.println("This is singleEvaluation");
        StringWriter s = new StringWriter();
        PrintWriter out = new PrintWriter(s);
        String formatString = "%2$-25s\t%1$5s\t";
        // print trec_eval relational-style output
        out.format(formatString + "%3$6d\n", index, "num_ret", retrieved.get(index).size());
        out.format(formatString + "%3$6d\n", index, "num_rel", relevant.get(index).size());
        out.format(formatString + "%3$6d\n", index, "num_rel_ret", relevantRetrieved.get(index).size());

        out.format(formatString + "%3$6.4f\n", index, "NDCG@15", NDCG(15)); //wrong
        out.format(formatString + "%3$6.4f\n", index, "RR", RR(index));
        out.format(formatString + "%3$6.4f\n", index, "P@5", singlePrecision(5, index));
        out.format(formatString + "%3$6.4f\n", index, "P@10", singlePrecision(10, index));
        out.format( formatString + "%3$6.4f\n",     index, "R@10",         recallPrecision(10, index) );
        //out.format( formatString + "%3$6.4f\n",     index, "F1@10",         F1(10) );
//        out.format( formatString + "%3$6.4f\n",     index, "AP",         evaluator.averagePrecision() );
        return s.toString();
    }

    public static void main(String[] args) {
        Evaluation evaluation = new Evaluation();
        evaluation.setRetrieval("qrels", "bm25.trecrun");
        evaluation.convertRetrieval();
        System.out.println(evaluation.singleEvaluation("324"));


//        System.out.println(evaluation.summaryEvaluation(true));
//        for(String str : evaluation.retrievals.keySet())
//            System.out.println("Index: "+str+", content: "+evaluation.retrievals.get(str).printSize());
    }
}
