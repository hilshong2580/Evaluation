package evaluation;
/*
 * Main.java
 *
 * Created on November 30, 2006, 9:25 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */



import java.io.PrintWriter;
import java.io.StringWriter;

public class Main {

	/**
	 * Returns an output string very similar to that of trec_eval.  
	 */

	public static String singleQuery( String query, RetrievalEvaluator evaluator ) {
		StringWriter s = new StringWriter();
		PrintWriter out = new PrintWriter(s);
		String formatString = "%2$-25s\t%1$5s\t";
		// print trec_eval relational-style output
		out.format( formatString + "%3$6d\n",        query, "num_ret",     evaluator.retrievedDocuments().size() );
		out.format( formatString + "%3$6d\n",        query, "num_rel",     evaluator.relevantDocuments().size() );
		out.format( formatString + "%3$6d\n",        query, "num_rel_ret", evaluator.relevantRetrievedDocuments().size() );

		// counts
		out.format( formatString + "%3$6.4f\n",     query, "NDCG@15",      evaluator.normalizedDiscountedCumulativeGain( 15 ) );
		out.format( formatString + "%3$6.4f\n",     query, "RR",  evaluator.reciprocalRank() );
		out.format( formatString + "%3$6.4f\n",     query, "P@5",         evaluator.precision(5) );
		out.format( formatString + "%3$6.4f\n",     query, "P@10",         evaluator.precision(10) );
		out.format( formatString + "%3$6.4f\n",     query, "R@10",         evaluator.recall(10) );
		out.format( formatString + "%3$6.4f\n",     query, "F1@10",         evaluator.F1(10) );
		out.format( formatString + "%3$6.4f\n",     query, "AP",         evaluator.averagePrecision() );
		return s.toString();
	}

	/**
	 * Returns an output string very similar to that of trec_eval.  
	 */
	public static String singleEvaluation( SetRetrievalEvaluator setEvaluator, boolean showIndividual ) {
		StringWriter s = new StringWriter();
		PrintWriter out = new PrintWriter(s);
		String formatString = "%2$-25s\t%1$5s\t";
		// print trec_eval relational-style output
		if (showIndividual) {
			for( RetrievalEvaluator evaluator : setEvaluator.getEvaluators() ) {
				String query = evaluator.queryName();
				out.print(singleQuery(query, evaluator));
			}
		}
		// print summary data
		out.format( formatString + "%3$6d\n",      "all", "num_q",     setEvaluator.getEvaluators().size() );
		out.format( formatString + "%3$6d\n",      "all", "num_ret",     setEvaluator.numberRetrieved() );
		out.format( formatString + "%3$6d\n",      "all", "num_rel",     setEvaluator.numberRelevant() );
		out.format( formatString + "%3$6d\n",      "all", "num_rel_ret", setEvaluator.numberRelevantRetrieved() );
		out.format( formatString + "%3$6.4f\n",   "all", "NDCG@15",         setEvaluator.meanNormalizedDiscountedCumulativeGain(15) );
		out.format( formatString + "%3$6.4f\n",   "all", "MRR",         setEvaluator.meanReciprocalRank() );
		out.format( formatString + "%3$6.4f\n",   "all", "P@5",         setEvaluator.meanPrecision(5) );
		out.format( formatString + "%3$6.4f\n",   "all", "P@10",         setEvaluator.meanPrecision(10) );
		out.format( formatString + "%3$6.4f\n",   "all", "R@10",         setEvaluator.meanRecall(10) );  
		out.format( formatString + "%3$6.4f\n",   "all", "F1@10",         setEvaluator.meanF1(10) );  
		out.format( formatString + "%3$6.4f\n",   "all", "MAP",         setEvaluator.meanAveragePrecision() );   
		return s.toString();
	}



	public static void main( String[] args ) {
		if( args.length == 3 ) {

			SetRetrievalEvaluator setEvaluator = new SetRetrievalEvaluator();
			setEvaluator.init(args[0], args[1]);
			System.out.println(singleEvaluation( setEvaluator, Boolean.parseBoolean(args[2])));
		}
	}
}
