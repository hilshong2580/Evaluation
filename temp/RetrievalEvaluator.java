package evaluation;

import java.util.ArrayList;
import java.util.HashMap;  
import java.util.TreeMap;
import java.util.List;
import java.util.Collection;

public class RetrievalEvaluator {
    
    private String _queryName;
    private ArrayList<Document> _retrieved;
    private ArrayList<Document> _relevant;
    private ArrayList<Document> _relevantRetrieved;
    private HashMap<String, Judgment> _judgments;
    /**
     * Creates a new instance of RetrievalEvaluator
     *
     * @param retrieved A ranked list of retrieved documents.
     * @param judgments A collection of relevance judgments.
     */
    public RetrievalEvaluator( String queryName, List<Document> retrieved, Collection<Judgment> judgments ) {
        _queryName = queryName;
        _retrieved = new ArrayList<Document>( retrieved );
        
        _buildJudgments( judgments );
        _judgeRetrievedDocuments();
    }
    
    private void _buildJudgments( Collection<Judgment> judgments ) {
        _judgments = new HashMap<String, Judgment>();
        _relevant = new ArrayList<Document>();
        for( Judgment judgment : judgments ) {
        	if ( (judgment != null ) && ( judgment.judgment > 0 )) {
        		_judgments.put( judgment.documentName, judgment );
        		_relevant.add(new Document(judgment.documentName));
        	}
        }
    }
    
    private void _judgeRetrievedDocuments() {
        _relevantRetrieved = new ArrayList<Document>();
         
        for( Document document: _retrieved ) {
            boolean relevant = false;
           Judgment judgment = _judgments.get( document.documentName );
            
            relevant = ( judgment != null ) && ( judgment.judgment > 0 );
             
            if( relevant ) {
                _relevantRetrieved.add( document );
            } 
        }
    }
        
    /**
     * Returns the name of the query represented by this evaluator.
     */
    
    public String queryName() {
        return _queryName;
    }
     /**
     * Returns the precision of the retrieval at a given number of documents retrieved.
     * The precision is the number of relevant documents retrieved
     * divided by the total number of documents retrieved.
     *
     * @param documentsRetrieved The evaluation rank.
     */
    
    public double precision( int documentsRetrieved ) {
        if (documentsRetrieved == 0) return 0;
        return (double) relevantRetrieved( documentsRetrieved ) / (double) documentsRetrieved;
    }
    
    /**
     * Returns the recall of the retrieval at a given number of documents retrieved.
     * The recall is the number of relevant documents retrieved
     * divided by the total number of relevant documents for the query.
     *
     * @param documentsRetrieved The evaluation rank.
     */
    public double recall( int documentsRetrieved ) {
        if (_relevant.size() == 0) return 0;
        return (double) relevantRetrieved( documentsRetrieved ) / (double) _relevant.size();
    }

    /**
     * Returns the F1 of the retrieval at a given number of documents retrieved.
     *
     * @param documentsRetrieved The evaluation rank.
     */
    public double F1( int documentsRetrieved ) {
        double r = recall(documentsRetrieved);
        double p = precision(documentsRetrieved);
        if (_relevant.size() == 0 || (r == 0 && p == 0)) return 0;

        return (2.0 * r * p)/(r + p);
    }
    /**
     * Returns the reciprocal of the rank of the first relevant document
     * retrieved, or zero if no relevant documents were retrieved.
     */
    
    public double reciprocalRank( ) {
        if( _relevantRetrieved.size() == 0 )
            return 0;
        
        return 1.0 / (double) _relevantRetrieved.get(0).rank;
    }
    
    /**
     * Returns the average precision of the query.<p>
     *
     * Suppose the precision is evaluated once at the rank of
     * each relevant document in the retrieval.  If a document is
     * not retrieved, we assume that it was retrieved at rank infinity.
     * The mean of all these precision values is the average precision.
     */
    public double averagePrecision( ) {
        double sumPrecision = 0;
        int relevantCount = 0;
        
        for( Document document : _relevantRetrieved ) {
            relevantCount++;
            sumPrecision += relevantCount / (double) document.rank;
        }
        if (_relevant.size() > 0)
            return (double) sumPrecision / _relevant.size();
        return 0;
    }

    
    /** 
     * <p>Normalized Discounted Cumulative Gain </p>
     *
     * This measure was introduced in Jarvelin, Kekalainen, "IR Evaluation Methods
     * for Retrieving Highly Relevant Documents" SIGIR 2001.  I copied the formula
     * from Vassilvitskii, "Using Web-Graph Distance for Relevance Feedback in Web
     * Search", SIGIR 2006.
     *
     * Score = N \sum_i (2^{r(i)} - 1) / \log_{2}(1 + i)
     *
     * Where N is such that the score cannot be greater than 1.  We compute this
     * by computing the DCG (unnormalized) of a perfect ranking.
     */
    
    public double normalizedDiscountedCumulativeGain( ) {
        return normalizedDiscountedCumulativeGain( Math.max( _retrieved.size(), _judgments.size() ) );
    }
    
    /** 
     * <p>Normalized Discounted Cumulative Gain </p>
     *
     * This measure was introduced in Jarvelin, Kekalainen, "IR Evaluation Methods
     * for Retrieving Highly Relevant Documents" SIGIR 2001.  I copied the formula
     * from Vassilvitskii, "Using Web-Graph Distance for Relevance Feedback in Web
     * Search", SIGIR 2006.
     *
     * Score = N \sum_i (2^{r(i)} - 1) / \log_{2}(1 + i)
     *
     * Where N is such that the score cannot be greater than 1.  We compute this
     * by computing the DCG (unnormalized) of a perfect ranking.
     */                     
     
    public double normalizedDiscountedCumulativeGain( int documentsRetrieved ) {
        // first, compute the gain from an optimal ranking  
        double normalizer = normalizationTermNDCG( documentsRetrieved );
        if (normalizer == 0) return 0;
        
        // now, compute the NDCG of the ranking and return that
        double dcg = 0;
        List<Document> truncated = _retrieved;
        
        if( _retrieved.size() > documentsRetrieved )
            truncated = _retrieved.subList( 0, documentsRetrieved );
        
        
        for( Document document : truncated ) {
            Judgment judgment = _judgments.get( document.documentName );
            
            if( judgment != null && judgment.judgment > 0 ) {
            	// use log_2
                dcg += (Math.pow(2, judgment.judgment) - 1.0) / (Math.log( 1 + document.rank )/Math.log(2));
            }
        }    
        
        return dcg / normalizer;
    }           
    
    protected double normalizationTermNDCG( int documentsRetrieved ) {
        TreeMap<Integer, Integer> relevanceCounts = new TreeMap<Integer, Integer>();
        
        // the normalization term represents the highest possible DCG score
        // that could possibly be attained.  we compute this by taking the relevance
        // judgments, ordering them by relevance value (highly relevant documents first)
        // then calling that the ranked list, and computing its DCG value.
                                      
        // we use negative judgment values so they come out of the map
        // in order from highest to lowest relevance
        for( Judgment judgment : _judgments.values() ) {
            if( judgment.judgment == 0 )
                continue;
                
            if( !relevanceCounts.containsKey(-judgment.judgment) ) {
                relevanceCounts.put( -judgment.judgment, 0 );
            }                                               
            
            relevanceCounts.put( -judgment.judgment, relevanceCounts.get( -judgment.judgment ) + 1 );
        }                                                                                          
                
        double normalizer = 0;
        int documentsProcessed = 0; 
        
        for( Integer negativeRelevanceValue : relevanceCounts.keySet() ) {        
            int relevanceCount = (int)relevanceCounts.get( negativeRelevanceValue );
            int relevanceValue = -negativeRelevanceValue;
            relevanceCount = Math.min( relevanceCount, documentsRetrieved - documentsProcessed );
            
            for( int i = 1; i <= relevanceCount; i++ ) {
                normalizer += (Math.pow(2, relevanceValue) - 1.0) / (Math.log( 1 + i + documentsProcessed )/Math.log(2)); 
            }
            
            documentsProcessed += relevanceCount;
            if( documentsProcessed >= documentsRetrieved )
                break;
        }                              
        
        return normalizer;
    }
    
    /**
     * The number of relevant documents retrieved at a particular
     * rank.  This is equivalent to <tt>n * precision(n)</tt>.
     */
    
    public int relevantRetrieved( int documentsRetrieved ) {
        int low = 0;
        int high = _relevantRetrieved.size() - 1;
        
        // if we didn't retrieve anything relevant, we know the answer
        if( _relevantRetrieved.size() == 0 )
            return 0;
        
        // is this after the last relevant document?
        Document lastRelevant = _relevantRetrieved.get( high );
        if( lastRelevant.rank <= documentsRetrieved )
            return _relevantRetrieved.size();
        
        // is this before the first relevant document?
        Document firstRelevant = _relevantRetrieved.get( low );
        if( firstRelevant.rank > documentsRetrieved )
            return 0;
        
        while( (high - low) >= 2 ) {
            int middle = low + (high - low) / 2;
            Document middleDocument = _relevantRetrieved.get( middle );
            
            if( middleDocument.rank == documentsRetrieved )
                return middle + 1;
            else if( middleDocument.rank > documentsRetrieved )
                high = middle;
            else
                low = middle;
        }
        
        // if the high document had rank == documentsRetrieved, we would
        // have already returned, either at the top, or at the return middle statement
        assert _relevantRetrieved.get( low ).rank <= documentsRetrieved &&
            _relevantRetrieved.get( high ).rank > documentsRetrieved;
        
        return low + 1;
    }
    
    /**
     * @return The list of retrieved documents.
     */
    public ArrayList<Document> retrievedDocuments() {
        return _retrieved;
    }
            
     /**
     * Returns a list of retrieved documents that were judged relevant,
     * in the order that they were retrieved.
     */
    
    public ArrayList<Document> relevantRetrievedDocuments() {
        return _relevantRetrieved;
    }
    
    /**
     * Returns a list of all documents judged relevant, whether they were
     * retrieved or not.
     */
    
    public ArrayList<Document> relevantDocuments() {
        return _relevant;
    }
}
