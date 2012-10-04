package cs224n.wordaligner;  

import cs224n.util.*;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Zhemin Li
 */
public class IBMModel2 implements WordAligner {
	class Triple{
		int i, l, m;
		Triple(int i, int l, int m)
		{
			this.i = i;
			this.l = l;
			this.m = m;
		}
		public int hashCode()
		{
			return i*l*m;
		}
		
		
		public boolean equals(Object o) {
		    if (o instanceof Triple) {
		    	Triple other = (Triple) o;
		      return (i == other.i && l == other.l && m == other.m);
		    }
		    return false;
		  }
	}

	private static final long serialVersionUID = 1315751943476440515L;

	// TODO: Use arrays or Counters for collecting sufficient statistics
	// from the training data.
	private CounterMap<String,String> sourceTargetCounts;
	private Counter<String> sourceCounts;
	private CounterMap<String,String> sourceTargetProbability;
	private CounterMap<Triple, Integer> c_jilmCounts;
	private CounterMap<Triple, Integer> q_jilmCounts;
	private Counter<Triple>  ilmCounts;
	
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
				double q = q_jilmCounts.getCount(new Triple(tgtIndex, numSourceWords, numTargetWords), srcIndex);
				tempAlignment.setCount(tgtIndex, p_t_s);
			}
			int maxIndex = tempAlignment.argMax();
			alignment.addPredictedAlignment(maxIndex, srcIndex);
		}
		return alignment;
	}

	public void train(List<SentencePair> trainingPairs) {
		load();
		sourceTargetCounts = new CounterMap<String,String>();
		sourceCounts = new Counter<String>();
		c_jilmCounts = new CounterMap<Triple, Integer>();
		q_jilmCounts = new CounterMap<Triple, Integer>();
		ilmCounts = new Counter<Triple>();
	
		for(SentencePair pair : trainingPairs){
			List<String> targetWords = pair.getTargetWords();
			List<String> sourceWords = pair.getSourceWords();
			//insert NULL at the beginning of the source sentence
			List<String> sourceWithNULL = new ArrayList<String>();
			sourceWithNULL.add(str_null);
			sourceWithNULL.addAll(1, sourceWords);
			int l = sourceWithNULL.size();
			int m = targetWords.size();
			for(int i = 0; i < m; i++)
				for(int j = 0; j < l; j++)
					q_jilmCounts.setCount(new Triple(i,l,m), j, Math.random());
		}
		
		
		for(int i=0;;i++)
		{
			if(i == 0)	
			{
				IBMModel2_train(trainingPairs);
				continue;
			}
			
			CounterMap<String,String> pre_sourceTargetProbability = new CounterMap<String, String>();
			copyCounterMap(sourceTargetProbability, pre_sourceTargetProbability);
			IBMModel2_train(trainingPairs);
			if(maxProbDiff(pre_sourceTargetProbability, sourceTargetProbability) < 0.01)
				break;
		}		
		
	}
	
	private void IBMModel2_train(List<SentencePair> trainingPairs)
	{
		for(SentencePair pair : trainingPairs){
			List<String> targetWords = pair.getTargetWords();
			List<String> sourceWords = pair.getSourceWords();
			//insert NULL at the beginning of the source sentence
			List<String> sourceWithNULL = new ArrayList<String>();
			sourceWithNULL.add(str_null);
			sourceWithNULL.addAll(1, sourceWords);
			
			int m = targetWords.size();
			int l = sourceWithNULL.size();
			
			for(int i = 0; i < m; i++)
			{
				String target = targetWords.get(i);
				double total = 0;
				for(int j = 0; j < l; j++)
				{
					String source = sourceWithNULL.get(j);
					double pts = sourceTargetProbability.getCount(source, target);
					double q_jilm = q_jilmCounts.getCount(new Triple(i, l, m), j);
					total += pts*q_jilm;
				}
				for(int j = 0; j < sourceWithNULL.size(); j++)
				{
					Triple triple =  new Triple(i, l, m);
					String source = sourceWithNULL.get(j);
					double pts = sourceTargetProbability.getCount(source, target);
					double q_jilm = q_jilmCounts.getCount(triple, j);
					double c = pts*q_jilm/total;
					sourceTargetCounts.incrementCount(source, target, c);
					sourceCounts.incrementCount(source, c);
					ilmCounts.incrementCount(triple, c);
					c_jilmCounts.incrementCount(triple, j, c);
				}
			}
		}
		for(String source : sourceTargetProbability.keySet())
		{
			double total = sourceCounts.getCount(source);
			for(String target : sourceTargetCounts.getCounter(source).keySet())
			{
				double tc_fe = sourceTargetCounts.getCount(source, target);
				sourceTargetProbability.setCount(source, target, tc_fe/total);
			}
		}
		for(Triple triple : q_jilmCounts.keySet())
		{
			double total = ilmCounts.getCount(triple);
			for(int j : q_jilmCounts.getCounter(triple).keySet())
			{
				double c_jilm = c_jilmCounts.getCount(triple, j);
				q_jilmCounts.setCount(triple, j, c_jilm/total);
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
	
	public void load() {
		try {
			// Create necessary input streams
			FileInputStream fis = new FileInputStream("IBMModel1_Obj"); // Read from file
			ObjectInputStream in = new ObjectInputStream(fis);  // Read objects
			// Read in an object.  It should be a vector of scribbles
			CounterMap<String,String> readObject = (CounterMap<String,String>)in.readObject();
			CounterMap<String,String> IBMModel1_probabilities = readObject;
			in.close();                    // Close the stream.
			sourceTargetProbability = IBMModel1_probabilities;              
			
		}	
		catch (Exception e) { System.out.println(e); }
	}

}
