package cs224n.wordaligner;  

import cs224n.util.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Zhemin Li
 */
public class IBMModel1 implements WordAligner {

	private static final long serialVersionUID = 1315751943476440515L;

	// TODO: Use arrays or Counters for collecting sufficient statistics
	// from the training data.
	private CounterMap<String,String> sourceTargetCounts;
	private CounterMap<String,String> sourceTargetProbability;
	
	String str_null = "NULL";

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
			source = sentencePair.getSourceWords().get(srcIndex);
			
			for(int tgtIndex = 0; tgtIndex < numTargetWords; tgtIndex++)
			{
				target = sentencePair.getTargetWords().get(tgtIndex);
				double p_t_s = sourceTargetProbability.getCount(source, target);
				tempAlignment.setCount(tgtIndex, p_t_s);
			}
			int maxIndex = tempAlignment.argMax();
			alignment.addPredictedAlignment(maxIndex, srcIndex);
		}
		return alignment;
	}

	public void train(List<SentencePair> trainingPairs) {
		sourceTargetCounts = new CounterMap<String,String>();
		sourceTargetProbability = new CounterMap<String,String>();
		Counter<String> targetCounts = new Counter<String>();
		for(SentencePair pair : trainingPairs){
			List<String> targetWords = pair.getTargetWords();
			for(String target : targetWords)
				targetCounts.setCount(target, 1.0);
		}
		
		for(SentencePair pair : trainingPairs){
			List<String> targetWords = pair.getTargetWords();
			List<String> sourceWords = pair.getSourceWords();
			//insert NULL at the beginning of the source sentence
			List<String> sourceWithNULL = new ArrayList<String>();
			sourceWithNULL.add(str_null);
			sourceWithNULL.addAll(1, sourceWords);
			
			for(String source : sourceWithNULL)
				for(String target : targetWords)
				{
					sourceTargetCounts.setCount(source, target, 0);
					sourceTargetProbability.setCount(source, target, 1.0/targetCounts.size());
				}
		}
		
		
		for(int i=0;;i++)
		{
			if(i == 0)	
			{
				IBMModel1_train(trainingPairs);
				continue;
			}
			
			CounterMap<String,String> pre_sourceTargetProbability = new CounterMap<String, String>();
			copyCounterMap(sourceTargetProbability, pre_sourceTargetProbability);
			IBMModel1_train(trainingPairs);
			if(maxProbDiff(pre_sourceTargetProbability, sourceTargetProbability) < 0.001)
				break;
		}
		save();
		
	}
	
	private void IBMModel1_train(List<SentencePair> trainingPairs)
	{
		for(SentencePair pair : trainingPairs){
			List<String> targetWords = pair.getTargetWords();
			List<String> sourceWords = pair.getSourceWords();
			//insert NULL at the beginning of the source sentence
			List<String> sourceWithNULL = new ArrayList<String>();
			sourceWithNULL.add(str_null);
			sourceWithNULL.addAll(1, sourceWords);
			for(String target : targetWords)
			{
				double total = 0;
				
				for(String source : sourceWithNULL)
					total += sourceTargetProbability.getCount(source, target);
				
				for(String source : sourceWithNULL)
				{
					double Pfi = sourceTargetProbability.getCount(source, target);
					sourceTargetCounts.incrementCount(source, target, Pfi/total);
				}
			}
		}
		for(String source : sourceTargetProbability.keySet())
		{
			double total = sourceTargetCounts.getCounter(source).totalCount();
			for(String target : sourceTargetCounts.getCounter(source).keySet())
			{
				double tc_fe = sourceTargetCounts.getCount(source, target);
				sourceTargetProbability.setCount(source, target, tc_fe/total);
			}
		}
	}
	
	private void copyCounterMap(CounterMap<String,String> origin, CounterMap<String,String> copy)
	{
		for(String source : origin.keySet())
		{
			for(String target : origin.getCounter(source).keySet())
				copy.setCount(source, target, origin.getCount(source, target));
		}
	}
	
	private double maxProbDiff(CounterMap<String,String> pre, CounterMap<String,String> curr)
	{
		double maxdiff = 0;
		for(String source : pre.keySet())
		{
			for(String target : pre.getCounter(source).keySet())
			{
				double diff = Math.abs(pre.getCount(source, target) - curr.getCount(source, target));
			    if(maxdiff<diff)
			    	maxdiff = diff;
			}
		}
		return maxdiff;
	}
	
	public void save() {   
	      try {
	        // Create the necessary output streams to save the scribble.
	        FileOutputStream fos = new FileOutputStream("IBMModel1_Obj"); 
								// Save to file
	        ObjectOutputStream out = new ObjectOutputStream(fos); 
								// Save objects
	        out.writeObject(sourceTargetProbability);      	// Write the entire Vector of scribbles
	        out.flush();                 		// Always flush the output.
	        out.close();                 		// And close the stream.
	      }
	      // Print out exceptions. 
	      catch (IOException e) { System.out.println(e); }
	}
}
