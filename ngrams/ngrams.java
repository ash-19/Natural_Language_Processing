import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This program builds lexical n-grams language model and computes the 
 * sentence probability using:
 * a) Unsmoothed Unigram Model
 * b) Unsmoothed Bigram Model
 * b) Add-One (Laplace) Smoothed Bigram Model
 * 
 * @author Snehashish Mishra
 * @uid u0946268
 */
public class ngrams {

	public static HashSet<String> unigramDictionary = new HashSet<String>();
	public static HashSet<String> bigramDictionary = new HashSet<String>();
	public static HashMap<String, Integer> unigramFreqMap = new HashMap<>();
	public static HashMap<String, Integer> bigramFreqMap = new HashMap<>();
	public static HashMap<String, Double> uniProbabilityMap = new HashMap<>();
	public static HashMap<String, Double> biProbabilityMap = new HashMap<>();
	public static int phiCount = 0;

	public static void main(String[] args) {
		
		if(	areCmdArgsInvalid(args) ) {
			return;
		}
		
		String trainFilePath = args[1];
		String testFilePath = args[3];

		makeUnigramsFromTxt(trainFilePath);
		makeBigramsFromTxt(trainFilePath);

		computeTestProbabilitiesFromTxt(testFilePath);
	}

	private static boolean areCmdArgsInvalid(String[] args) {

		boolean invalid = false;
		
//		System.out.println("=================================");
//		for(int i = 0; i < args.length; i++) {
//			System.out.println(i + ") " + args[i].toString());
//		}
		
		for(int i = 0; i < args.length; i++) {
			if(args[i].equals("ngrams")) {
				if(!args[i+2].equals("-test")) {
					invalid = true;
					System.err.println("DOES NOT CONFORM TO THE INPUT SPECIFICATIONS!");
					System.err.println("Only 1 training source file is allowed!");
					System.err.println("Expected -test but received: " +args[i+2]);
				}
			}
		}
		
		return invalid;
	}

	private static void computeTestProbabilitiesFromTxt(String filePath) {
		try {
			BufferedReader lineReader = new BufferedReader(new FileReader(filePath));
			String sentence = null;

			// Extract the data and add to TrainDataList as DataItem objects
			while ((sentence = lineReader.readLine()) != null) {
				computeUnigramSentenceProbability(sentence);
				computeBigramSentenceProbability(sentence);
				computeBigramSentenceProbabilityWithLaplaceSmoothing(sentence);
			}
			lineReader.close();
		} catch (IOException ex) {
			System.err.println(ex);
		}		
	}

	private static void computeUnigramSentenceProbability(String sentence) {
		int N = 0;

		// Compute N = sum of all the freq of unique terms
		for (HashMap.Entry<String, Integer> entry : unigramFreqMap.entrySet()) {
			Integer frequency = entry.getValue();
			N += frequency;
		}

		// Split the sentence into tokens
		String[] sentenceArr = sentence.toLowerCase().split("\\s+");
		ArrayList<Double> probList = new ArrayList<>();

		// Process each unigram word. If its found, compute P(w). Else
		// assign P(w) = 0. 
		for(String word: sentenceArr) {
			if(unigramFreqMap.containsKey(word)) {
				int frequency = unigramFreqMap.get(word);
				double prob_w = ((double) frequency) / N;
				probList.add(prob_w);
			}
			else {
				probList.add(0.0);
			}
		}

		double log2SentenceProbability = 0.0;
//		double sentenceProbability = 1.0;

		for(double prob: probList) {
			log2SentenceProbability += getLog2probability(prob);
//			sentenceProbability *= prob;
		}

		DecimalFormat df = new DecimalFormat("###.####");
		df.setRoundingMode(RoundingMode.HALF_DOWN);

		System.out.println("S = " + sentence + "\n");
		System.out.println("Unsmoothed Unigrams, logprob(S) = " + df.format(log2SentenceProbability));
	}

	private static void computeBigramSentenceProbability(String sentence) {

		//	Split the sentence into tokens
		String[] sentenceArr = sentence.toLowerCase().split("\\s+");
		ArrayList<Double> probList = new ArrayList<>();

		for(int i = sentenceArr.length-1; i >= 0 ; i--) {
			String word = "";

			// Compose the bigram (wn-1, wn)
			if( i == 0) {
				word = "phi " + sentenceArr[i];
			}
			else {
				word = sentenceArr[i-1] + " " + sentenceArr[i];
			}

			// Compute P(wn | wn-1)
			// Process each bigram. If its found, compute P(w). Else
			// assign P(w) = 0. 
			if(bigramFreqMap.containsKey(word)) {
				int frequency = bigramFreqMap.get(word);
				double prob_w = 0.0;
				if(word.contains("phi")) {
					prob_w = ((double) frequency) / phiCount;
				}
				else {
					String[] word_components = word.split("\\s+");
					prob_w = ((double) frequency) / unigramFreqMap.get(word_components[0]);
				}

				probList.add(prob_w);
			}
			else {
				System.out.println("Unsmoothed Bigrams, logprob(S) = undefined");
				return;
			}
		}

		double log2SentenceProbability = 0.0;
//		double sentenceProbability = 1.0;

		for(double prob: probList) {
			log2SentenceProbability += getLog2probability(prob);
//			sentenceProbability *= prob;
		}

		DecimalFormat df = new DecimalFormat("###.####");
		df.setRoundingMode(RoundingMode.HALF_DOWN);

		System.out.println("Unsmoothed Bigrams, logprob(S) = " + df.format(log2SentenceProbability));
	}

	private static void computeBigramSentenceProbabilityWithLaplaceSmoothing(String sentence) {

		//	 Split the sentence into tokens
		String[] sentenceArr = sentence.toLowerCase().split("\\s+");
		ArrayList<Double> probList = new ArrayList<>();

		for(int i = sentenceArr.length-1; i >= 0 ; i--) {
			String word = "";

			// Compose the bigram (wn-1, wn)
			if( i == 0) {
				word = "phi " + sentenceArr[i];
			}
			else {
				word = sentenceArr[i-1] + " " + sentenceArr[i];
			}

			// Compute P(wn | wn-1)
			// Process each bigram. If its found, compute P(w). Else
			// assign P(w) = 0.
			int frequency = 0;
			if(bigramFreqMap.containsKey(word)) {
				frequency = bigramFreqMap.get(word);
			}
			double prob_w = 0.0;
			if(word.contains("phi")) {
				prob_w = (((double) frequency) + 1.0) / (phiCount + unigramFreqMap.size());
			}
			else {
				String[] word_components = word.split("\\s+");
				prob_w = (((double) frequency) + 1.0) / (unigramFreqMap.get(word_components[0]) + unigramFreqMap.size());
			}

			probList.add(prob_w);
		}

		double log2SentenceProbability = 0.0;
//		double sentenceProbability = 1.0;

		for(double prob: probList) {
			log2SentenceProbability += getLog2probability(prob);
//			sentenceProbability *= prob;
		}

		DecimalFormat df = new DecimalFormat("###.####");
		df.setRoundingMode(RoundingMode.HALF_DOWN);

		System.out.println("Smoothed Bigrams, logprob(S) = " + df.format(log2SentenceProbability) + "\n");
	}

	private static double getLog2probability(double number) {
		double logProb = (Math.log10(number)/Math.log10(2));
		return logProb;
	}

	private static void makeUnigramsFromTxt(String filePath) {
		try {
			BufferedReader lineReader = new BufferedReader(new FileReader(filePath));
			String line = null;

			// Extract the data and add to TrainDataList as DataItem objects
			while ((line = lineReader.readLine()) != null) {
				//		    	System.out.println("---------NEXT LINE------------------");
				phiCount++;
				// Split the full name into components
				String[] sentenceArr = line.toLowerCase().split("\\s+");

				for(String word : sentenceArr) {

					unigramDictionary.add(word);

					// Add the unigram frequency 
					if(unigramFreqMap.containsKey(word)) {
						int freq = unigramFreqMap.get(word);
						freq++;
						unigramFreqMap.replace(word, freq);
					}
					else {
						unigramFreqMap.put(word, 1);
					}
				}
			}
			lineReader.close();
		} catch (IOException ex) {
			System.err.println(ex);
		}	
	}

	private static void makeBigramsFromTxt(String filePath) {
		try {
			BufferedReader lineReader = new BufferedReader(new FileReader(filePath));
			String line = null;

			while ((line = lineReader.readLine()) != null) {

				// Split the full name into components
				String[] sentenceArr = line.toLowerCase().split("\\s+");

				for(int i = sentenceArr.length-1; i >= 0 ; i--) {
					String word = "";

					if( i == 0) {
						word = "phi " + sentenceArr[i];
					}
					else {
						word = sentenceArr[i-1] + " " + sentenceArr[i];
					}
					bigramDictionary.add(word);

					// Add the unigram frequency 
					if(bigramFreqMap.containsKey(word)) {
						int freq = bigramFreqMap.get(word);
						freq++;
						bigramFreqMap.replace(word, freq);
					}
					else {
						bigramFreqMap.put(word, 1);
					}
				}

			}
			lineReader.close();
		} catch (IOException ex) {
			System.err.println(ex);
		}	
	}
}
