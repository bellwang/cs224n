package cs224n.wordaligner;  

import cs224n.util.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Zhemin Li
 */
public class PMIModel implements WordAligner {

	private static final long serialVersionUID = 1315751943476440515L;

	// TODO: Use arrays or Counters for collecting sufficient statistics
	// from the training data.
	private CounterMap<String,String> sourceTargetCounts;
	private Counter<String> sourceCounts;
	private Counter<String> targetCounts;
	private String str_null = "NULL";

	public Alignment align(SentencePair sentencePair) {
		// Placeholder code below. 
		// TODO Implement an inference algorithm for Eq.1 in the assignment
		// handout to predict alignments based on the counts you collected with train().
		Alignment alignment = new Alignment();
		int numSourceWords = sentencePair.getSourceWords().size();
		int numTargetWords = sentencePair.getTargetWords().size();
		

		String source;
		String target;
		for (int srcIndex = 0; srcIndex < numSourceWords; srcIndex++) {
			Counter<Integer> tempAlignment = new Counter<Integer>();
			double tgtCounts = 0;
			double sfCounts = 0;
			double sCounts = 0;
			source = sentencePair.getSourceWords().get(srcIndex);
			sCounts = sourceCounts.getCount(source);
			for(int tgtIndex = 0; tgtIndex < numTargetWords; tgtIndex++)
			{
				target = sentencePair.getTargetWords().get(tgtIndex);
				tgtCounts = targetCounts.getCount(target);
				sfCounts = sourceTargetCounts.getCount(source, target);
				tempAlignment.setCount(tgtIndex,sfCounts/(tgtCounts*sCounts));
			}
			int maxIndex = tempAlignment.argMax();
			alignment.addPredictedAlignment(maxIndex, srcIndex);
		}
		return alignment;
	}

	public void train(List<SentencePair> trainingPairs) {
		sourceTargetCounts = new CounterMap<String,String>();
		sourceCounts = new Counter<String>();
		targetCounts = new Counter<String>();
		
		for(SentencePair pair : trainingPairs){
			List<String> targetWords = pair.getTargetWords();
			List<String> sourceWords = pair.getSourceWords();
			//insert NULL at the beginning of the source sentence
			List<String> sourceWithNULL = new ArrayList<String>();
			sourceWithNULL.add(str_null);
			sourceWithNULL.addAll(1, sourceWords);
			
			CounterMap<String, String> tempSourceTargetCounts = new CounterMap<String, String>();
			for(String source : sourceWithNULL){
				for(String target : targetWords){
					// TODO: Warm-up. Your code here for collecting sufficient statistics.
					tempSourceTargetCounts.setCount(source, target, 1.0);
				}
			}
			
			Counter<String> tempSourceCounts = new Counter<String>();
			Counter<String> tempTargetCounts = new Counter<String>();
			for(String source : sourceWithNULL)
				tempSourceCounts.setCount(source, 1.0);
			//f0 is null
			tempTargetCounts.setCount(str_null, 1.0);
			for(String targt : targetWords)
				tempTargetCounts.setCount(targt, 1.0);
		    for(String source : tempSourceTargetCounts.keySet())
		    {
		    	for(String target : tempSourceTargetCounts.getCounter(source).keySet())
		    		sourceTargetCounts.incrementCount(source, target, 1.0);
		    }
		    for(String source : tempSourceCounts.keySet())
		    	sourceCounts.incrementCount(source, 1.0);
		    for(String target : tempTargetCounts.keySet())
		    	targetCounts.incrementCount(target, 1.0);
		}
	}	
}
