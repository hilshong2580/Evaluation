package evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class SetRetrievalEvaluator {
    Collection<RetrievalEvaluator> _evaluators;

    /**
     * Creates a new instance of SetRetrievalEvaluator
     */
    public SetRetrievalEvaluator() {
    }

    public void init(String qrels, String rankedlists) {

        Map<String, List<Document>> allRankings = loadRanking(rankedlists);
        Map<String, List<Judgment>> allJudgments = loadJudgments(qrels);
        // Map query numbers into Integer to get proper sorting.
        TreeMap<String, RetrievalEvaluator> evaluators = new TreeMap<String, RetrievalEvaluator>(new java.util.Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                try {
                    Integer a1 = new Integer(a);
                    Integer b1 = new Integer(b);
                    return a1.compareTo(b1);
                } catch (NumberFormatException e) {
                    // not an integer
                    return a.compareTo(b);
                }
            }
        });

        for (String query : allRankings.keySet()) {
            List<Judgment> judgments = allJudgments.get(query);
            List<Document> ranking = allRankings.get(query);

            /* resort ranking on score, renumber ranks */
            java.util.Collections.sort(ranking, new java.util.Comparator<Document>() {
                @Override
                public int compare(Document a, Document b) {
                    if (a.score < b.score) return 1;
                    if (a.score == b.score) return 0;
                    return -1;
                }
            });
            int i = 1;
            for (Document d : ranking) {
                d.rank = i++;
            }

            if (judgments == null || ranking == null) {
                continue;
            }

            RetrievalEvaluator evaluator = new RetrievalEvaluator(query, ranking, judgments);
            evaluators.put(query, evaluator);
        }

        _evaluators = evaluators.values();
    }

    /**
     * Loads a TREC judgments file.
     *
     * @param filename The filename of the judgments file to load.
     * @return Maps from query numbers to lists of judgments for each query.
     */

    public Map<String, List<Judgment>> loadJudgments(String filename) {
        try {
            // open file
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line = null;
            Map<String, List<Judgment>> judgments = new TreeMap<String, List<Judgment>>();
            String recentQuery = null;
            List<Judgment> recentJudgments = null;

            while ((line = in.readLine()) != null) {
                // allow for multiple whitespace characters between fields
                String[] fields = line.split("\\s+");

                String number = fields[0];
                String docno = fields[2];
                String judgment = fields[3];
                int jVal = 0;
                try {
                    jVal = Integer.valueOf(judgment);

                } catch (NumberFormatException e) {
                    jVal = (int) Math.round(Double.valueOf(judgment));
                }

                Judgment j = new Judgment(docno, jVal);

                if (recentQuery == null || !recentQuery.equals(number)) {
                    if (!judgments.containsKey(number)) {
                        judgments.put(number, new ArrayList<Judgment>());
                    }

                    recentJudgments = judgments.get(number);
                    recentQuery = number;
                }

                recentJudgments.add(j);
            }

            in.close();

            return judgments;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reads in a TREC ranking file.
     *
     * @param filename The filename of the ranking file.
     * @return A map from query numbers to document ranking lists.
     */

    public static Map<String, List<Document>> loadRanking(String filename) {
        // open file
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line = null;
            Map<String, List<Document>> ranking = new TreeMap<String, List<Document>>();
            List<Document> recentRanking = null;
            String recentQuery = null;

            while ((line = in.readLine()) != null) {
                // allow for multiple whitespace characters between fields
                String[] fields = line.split("\\s+");

                // 1 Q0 WSJ880711-0086 39 -3.05948 Exp

                String number = fields[0];
                String docno = fields[2];
                String rank = fields[3];
                String score = fields[4];
                // lemur can output nan (or NaN)
                double scoreNumber;
                try {
                    scoreNumber = Double.valueOf(score);
                } catch (NumberFormatException ex) {
                    scoreNumber = 0.0;
                }


                Document document = new Document(docno, Integer.valueOf(rank), scoreNumber);

                if (recentQuery == null || !recentQuery.equals(number)) {
                    if (!ranking.containsKey(number)) {
                        ranking.put(number, new ArrayList<Document>());
                    }

                    recentQuery = number;
                    recentRanking = ranking.get(number);
                }

                recentRanking.add(document);
            }

            in.close();
            return ranking;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns a collection of evaluators.
     */

    public Collection<RetrievalEvaluator> getEvaluators() {
        return _evaluators;
    }

    /**
     * Returns the mean average precision; the mean of the average
     * precision values for all queries.
     */
    public double meanAveragePrecision() {
        double sumAveragePrecision = 0;
        if (_evaluators.size() == 0) return 0;

        for (RetrievalEvaluator evaluator : _evaluators) {
            sumAveragePrecision += evaluator.averagePrecision();
        }

        return sumAveragePrecision / (double) _evaluators.size();
    }


    /**
     * Returns the mean of the precision values
     * for all queries.
     */

    public double meanPrecision(int documentsRetrieved) {
        double sumPrecision = 0;
        if (_evaluators.size() == 0) return 0;

        for (RetrievalEvaluator evaluator : _evaluators) {
            sumPrecision += evaluator.precision(documentsRetrieved);
        }

        return sumPrecision / _evaluators.size();
    }

    /**
     * Returns the mean of the recall values
     * for all queries.
     */

    public double meanRecall(int documentsRetrieved) {
        double sumRecall = 0;
        if (_evaluators.size() == 0) return 0;

        for (RetrievalEvaluator evaluator : _evaluators) {
            sumRecall += evaluator.recall(documentsRetrieved);
        }

        return sumRecall / _evaluators.size();
    }

    /**
     * Returns the mean of the F1 values
     * for all queries.
     */

    public double meanF1(int documentsRetrieved) {
        double sumF1 = 0;
        if (_evaluators.size() == 0) return 0;

        for (RetrievalEvaluator evaluator : _evaluators) {
            sumF1 += evaluator.F1(documentsRetrieved);
        }

        return sumF1 / _evaluators.size();
    }

    /**
     * Returns the mean of the reciprocal rank values for all queries.
     */
    public double meanReciprocalRank() {
        double sumRR = 0;
        if (_evaluators.size() == 0) return 0;

        for (RetrievalEvaluator evaluator : _evaluators) {
            sumRR += evaluator.reciprocalRank();
        }

        return sumRR / _evaluators.size();
    }

    /**
     * Returns the mean of the NDCG values for all queries.
     */

    public double meanNormalizedDiscountedCumulativeGain() {
        double sumNDCG = 0;
        if (_evaluators.size() == 0) return 0;

        for (RetrievalEvaluator evaluator : _evaluators) {
            sumNDCG += evaluator.normalizedDiscountedCumulativeGain();
        }

        return sumNDCG / _evaluators.size();
    }

    /**
     * Returns the mean of the NDCG values for all queries.
     */

    public double meanNormalizedDiscountedCumulativeGain(int documentsRetrieved) {
        double sumNDCG = 0;
        if (_evaluators.size() == 0) return 0;

        for (RetrievalEvaluator evaluator : _evaluators) {
            sumNDCG += evaluator.normalizedDiscountedCumulativeGain(documentsRetrieved);
        }

        return sumNDCG / _evaluators.size();
    }

    /**
     * The number of documents retrieved for all queries.
     */
    public int numberRetrieved() {
        int sumRetrieved = 0;

        for (RetrievalEvaluator evaluator : _evaluators) {
            sumRetrieved += evaluator.retrievedDocuments().size();
        }

        return sumRetrieved;
    }

    /**
     * The total number of relevant documents to any of the queries.
     */
    public int numberRelevant() {
        int sumRelevant = 0;

        for (RetrievalEvaluator evaluator : _evaluators) {
            sumRelevant += evaluator.relevantDocuments().size();
        }

        return sumRelevant;
    }

    /**
     * The total number of relevant documents retrieved for any of the queries.
     */
    public int numberRelevantRetrieved() {
        int sumRelevantRetrieved = 0;

        for (RetrievalEvaluator evaluator : _evaluators) {
            sumRelevantRetrieved += evaluator.relevantRetrievedDocuments().size();
        }

        return sumRelevantRetrieved;
    }
}
