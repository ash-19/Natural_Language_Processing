import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This program computes the best tag sequence for any given 
 * sentence using the Viterbi Algorithm.
 * 
 * @author Snehashish Mishra
 * @uid u0946268
 */
public class Viterbi {

	static HashMap<String, Integer> probTagIndexMap = new HashMap<>();
	static HashMap<String, Integer> probWordIndexMap = new HashMap<>();
	static HashMap<Integer, String> indexToTagMapMap = new HashMap<>();
	static ArrayList<String[]> inputSentencesList = new ArrayList<>();
	static ArrayList<String> inputSentences = new ArrayList<>();
	static double[][] transitionPTable = new double[5][4];
	static double[][] emissionPTable;
	static int totalTags = 4;
	
	public static void main(String[] args) {
		
		if(	areCmdArgsInvalid(args) ) {
			return;
		}
		
		String probFilePath = args[1];
		String sentencesFilePath = args[2];
		
		probTagIndexMap.put("phi", 4);
		probTagIndexMap.put("noun", 0);
		probTagIndexMap.put("verb", 1);
		probTagIndexMap.put("inf", 2);
		probTagIndexMap.put("prep", 3);
		
		indexToTagMapMap.put(4, "phi");
		indexToTagMapMap.put(0, "noun");
		indexToTagMapMap.put(1, "verb");
		indexToTagMapMap.put(2, "inf");
		indexToTagMapMap.put(3, "prep");
		
		initializeProbArrayStructures(probFilePath);
		processProbabilitiesFromFile(probFilePath);
		processSentencesFromFile(sentencesFilePath);
		
		for(int i = 0; i < inputSentencesList.size(); i++ ) {
			viterbi(inputSentencesList.get(i), inputSentences.get(i));
			System.out.println("\n");
		}
		
	}
	
	private static boolean areCmdArgsInvalid(String[] args) {

		boolean invalid = false;
		
		if( !args[0].equals("viterbi") ) {
			System.err.println("Expected 1st argument to be \"viterbi\" but received \" "+ args[0] + "\"");
			System.err.println("Please follow the input format: viterbi <probabilities_file> <sentences_file>");
			invalid = true;
		}
		
		return invalid;
	}
	
	private static void viterbi(String[] inputSentenceArr, String sent) {
		
		int W = inputSentenceArr.length;
		int[] seq = new int[W];				//index num is word, value is its best tag
		Arrays.fill(seq, -1);
		
		System.out.println("PROCESSING SENTENCE: " + sent);
		
		// +1 so be consistent with startIndex=1 of pseudocode
		// rows : {0,1,2,3,4}   col: {0, w1, w2, w3, ... , W }
		double[][] score = new double[totalTags][W];
		int[][] backPtr = new int[totalTags][W];
		
		// INITIALIZATION STEP
		for(int t = 0; t < totalTags; t++) {
			int word1Index = probWordIndexMap.get(inputSentenceArr[0]);
			int phiIndex = probTagIndexMap.get("phi");
			
			score[t][0] = emissionPTable[t][word1Index] * transitionPTable[phiIndex][t];
			backPtr[t][0] = 0;
		}
		System.out.println();
		
		// ITERATION STEP
		for(int w = 1; w < W; w++) {
			for(int t = 0; t < totalTags; t++) {
				
				int currentWordIndex = -1;
				if(probWordIndexMap.containsKey(inputSentenceArr[w])) {
					currentWordIndex = probWordIndexMap.get(inputSentenceArr[w]);
				}
				
				// Compute max k = 1 to T P(tag_t | tag_k)
				double maxProb = 0.0;
				int maxK = 0;
				for(int k = 0; k < totalTags; k++) {
					double temp = score[k][w-1] * transitionPTable[k][t];
					if(temp > maxProb) {	
						maxProb = temp;
						maxK = k;
					}
				}
				
				if(currentWordIndex == -1) {
					score[t][w] = 0.0001 * maxProb;
				}else {
					score[t][w] = emissionPTable[t][currentWordIndex] * maxProb;
				}
				backPtr[t][w] = maxK;
			}
		}
		
		// SEQEUNCE IDENTIFICATION
		int maxT = 0;
		double tempMaxProb = 0.0;
		// Find t that maximizes score(t, last word)
		for(int t = 0; t < totalTags; t++) {
			double currentProb = score[t][W-1];
			if(currentProb > tempMaxProb) {
				tempMaxProb = currentProb;
				maxT = t;
			}
		}
		
		seq[W-1] = maxT;
		
		for(int w = W-2; w >= 0; w--) {
			seq[w] = backPtr[seq[w+1]][w+1];
		}
		
		double bestTagSeqProb = 0.0;
		
		// Best Score[tag, word]. Word str is sentenceArr[wordSeqIndex]
		for(int wSeqIndex = 0; wSeqIndex < seq.length; wSeqIndex++) {
			
			int tagIndex = seq[wSeqIndex];
			double temp = getLog2probability(score[tagIndex][wSeqIndex]);
			
			// Conditional is < since negative probabilities
			if(temp < bestTagSeqProb) {
				bestTagSeqProb = temp;
			}	
		}
		
		DecimalFormat df = new DecimalFormat("###.0000");
		
		System.out.println("FINAL VITERBI NETWORK");
		for(int wordCol = 0; wordCol < inputSentenceArr.length; wordCol++) {
			for(int tagRow = 0; tagRow < totalTags; tagRow++) {
				System.out.println("P(" + inputSentenceArr[wordCol] + "=" + indexToTagMapMap.get(tagRow) + 
						") = " + df.format(getLog2probability(score[tagRow][wordCol])));
			}
		}
		
		System.out.println("\nFINAL BACKPTR NETWORK");
		for(int wordCol = 1; wordCol < inputSentenceArr.length; wordCol++) {
			for(int tagRow = 0; tagRow < totalTags; tagRow++) {
				System.out.println("Backptr(" + inputSentenceArr[wordCol] + "=" + indexToTagMapMap.get(tagRow) + 
						") = " + indexToTagMapMap.get(backPtr[tagRow][wordCol]));
			}
		}
		
		System.out.println("\nBEST TAG SEQUENCE HAS LOG PROBABILITY = " + df.format(bestTagSeqProb));
		
		for(int i = seq.length-1; i >= 0; i--) {
			System.out.println(inputSentenceArr[i] + " -> " + indexToTagMapMap.get(seq[i]));
		}
	}
	
	private static double getLog2probability(double number) {
		double logProb = (Math.log10(number)/Math.log10(2));
		return logProb;
	}

	private static void initializeProbArrayStructures(String probFilePath) {
		try {
			BufferedReader lineReader = new BufferedReader(new FileReader(probFilePath));
			String line = null;
			
			int index = 0;
			while ((line = lineReader.readLine()) != null) {
				
				String[] sentenceArr = line.toLowerCase().split("\\s+");
				
				if( !probTagIndexMap.containsKey(sentenceArr[0]) ) {
					
					if( !probWordIndexMap.containsKey(sentenceArr[0]) ) {
						probWordIndexMap.put(sentenceArr[0], index);
						index++;
					}
				}
			}
			lineReader.close();
		} catch (IOException ex) {
			System.err.println(ex);
		}	
		
		emissionPTable = new double[4][probWordIndexMap.size()];
		
		// Fill with default value 0.0001
		for(double[] row: emissionPTable) {
			Arrays.fill(row, 0.0001);
		}
		
		// Fill with default value 0.0001
		for(double[] row: transitionPTable) {
			Arrays.fill(row, 0.0001);
		}
	}

	private static void processSentencesFromFile(String sentencesFilePath) {
		try {
			BufferedReader lineReader = new BufferedReader(new FileReader(sentencesFilePath));
			String line = null;
			
			while ((line = lineReader.readLine()) != null) {
				inputSentences.add(line);
				String[] sentenceArr = line.toLowerCase().split("\\s+");
				
				inputSentencesList.add(sentenceArr);
			}
			lineReader.close();
		} catch (IOException ex) {
			System.err.println(ex);
		}
	}

	private static void processProbabilitiesFromFile(String probFilePath) {
		
		try {
			BufferedReader lineReader = new BufferedReader(new FileReader(probFilePath));
			String line = null;
			
			while ((line = lineReader.readLine()) != null) {
				String[] sentenceArr = line.toLowerCase().split("\\s+");

				// Transition prob
				if(probTagIndexMap.containsKey(sentenceArr[0]) && 
						probTagIndexMap.containsKey(sentenceArr[1])) {
					
					int column = probTagIndexMap.get(sentenceArr[0]);
					int row = probTagIndexMap.get(sentenceArr[1]);
					double value = Double.parseDouble(sentenceArr[2]); 
					
					transitionPTable[row][column] = value;
				}
				else {
					
					int wordColumn = probWordIndexMap.get(sentenceArr[0]);
					int tagRow = probTagIndexMap.get(sentenceArr[1]);
					double value = Double.parseDouble(sentenceArr[2]); 
					
					emissionPTable[tagRow][wordColumn] = value;
				}
			}
			lineReader.close();
		} catch (IOException ex) {
			System.err.println(ex);
		}	
	}
}
