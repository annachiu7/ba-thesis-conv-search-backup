package de.webis.recipesearch.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class TitleGramAnalyser {
	
	////////////////////////////////////////////////////////////////////////////
	// 									MEMBERS								  //
	////////////////////////////////////////////////////////////////////////////
	
	private static Path ehowJsonFolder = Paths.get("/home/xiwo8493/Documents/thesis/corpus/ehow/json");
	private static Path wikihowJsonFolder = Paths.get("/home/xiwo8493/Documents/thesis/corpus/wikihow-food/json");
	private static Path JsonFolder = Paths.get("/home/xiwo8493/Documents/thesis/corpus/json");
	
	private static String ehowOutputFile = "/home/xiwo8493/Documents/thesis/wstud-thesis-qu/data/ehow-3-gram-title-statistics";
	private static String wikihowOutputFile = "/home/xiwo8493/Documents/thesis/wstud-thesis-qu/data/wikihow-3rd-gram-title-statistics.csv";
	private static String collectiveOutputFile = "/home/xiwo8493/Documents/thesis/wstud-thesis-qu/data/collective-3-gram-statistics.csv";
	
	private static Map<String, Integer> gramMap1 = new HashMap<>();
	private static Map<String, Integer> gramMap2 = new HashMap<>(); 
	private static Map<String, Integer> gramMap3 = new HashMap<>(); 
	private static Map<String, Integer> gramMap1n2 = new HashMap<>();
	
	
	////////////////////////////////////////////////////////////////////////////
	// 									FUNCTIONS							  //
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Statistical function to determine the third gram in title string.
	 * This class should be used on wikihow Json files, where the first 2 words of each title remain the same.
	 * 
	 * @return
	 */
	public static Map<String, Integer> simpleThreeGram(Map<String, Integer> map, String title) {
		
		title = title.replace(".json", "");
		String[] titleSplit = title.split("\\s+");
		
		if (titleSplit.length > 2) {
			String threeGram = titleSplit[0] + " " + titleSplit[1] + " " + titleSplit[2];
			
			int occurrence;
			if (map.get(threeGram) == null) {			// If this word hasn't appeared before, update occurence to 1
				occurrence = 1;	
			} else {
				occurrence = map.get(threeGram) + 1;	// Otherwise, get stored value and accumulate
			}
			
			map.put(threeGram, occurrence);		// Update value in map
		} else {
			String twoGram = titleSplit[0] + " " + titleSplit[1];
			
			int occurrence;
			if (map.get(twoGram) == null) {			// If this word hasn't appeared before, update occurence to 1
				occurrence = 1;	
			} else {
				occurrence = map.get(twoGram) + 1;	// Otherwise, get stored value and accumulate
			}
			
			map.put(twoGram, occurrence);		// Update value in map
		}
		return map;
	}
	
	/**
	 * Statistical function to determine the first three grams in title string.
	 * This class should be used on ehow Json files, where the first 3 words of each title vary.
	 * 
	 * @return
	 */
	public static void advancedThreeGram(String title) {
		
		String[] titleSplit = title.split("\\s+");
		String firstGram = titleSplit[0];
		
		int occurrence;
		if (gramMap1.get(firstGram) == null) {
			occurrence = 1;
		} else {
			occurrence = gramMap1.get(firstGram) + 1;
		}
		gramMap1.put(firstGram, occurrence);
		
		String secondGram = titleSplit[1];
		if (gramMap2.get(secondGram) == null) {
			occurrence = 1;
		} else {
			occurrence = gramMap2.get(secondGram) + 1;
		}
		gramMap2.put(secondGram, occurrence);
		
		String twoGrams = firstGram + " " + secondGram;
		if (gramMap1n2.get(twoGrams) == null) {
			occurrence = 1;
		} else {
			occurrence = gramMap1n2.get(twoGrams) + 1;
		}
		gramMap1n2.put(twoGrams, occurrence);

		if (titleSplit.length > 2) {
			String thirdGram = titleSplit[2];
			if (gramMap3.get(thirdGram) == null) {
				occurrence = 1;
			} else {
				occurrence = gramMap3.get(thirdGram) + 1;
			}
			gramMap3.put(thirdGram, occurrence);			
		}
				
	}
	
	public static Map<String, Integer> walkFileTree(Path inputFolder, Map<String, Integer> thirdGramMap) throws IOException {
		if (Files.isDirectory(inputFolder)) {
			Files.walkFileTree(inputFolder, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
					
					// For each file, parse name ( = article title )
					String jsonfilepath = filePath.toString();
					String[] pathSplit = jsonfilepath.split("/");
					String title = pathSplit[pathSplit.length - 1];
					
					// 3-gram
					simpleThreeGram(thirdGramMap, title);
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		
		return thirdGramMap;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// 									MAIN								  //
	////////////////////////////////////////////////////////////////////////////
	
	public static void main(String[] args) throws IOException {
		

		// ------------------------------------------------------------------------
		// Calc simple third 3-gram of json files
		// ------------------------------------------------------------------------
		
		FileWriter fileWriter = new FileWriter(collectiveOutputFile);
		Map<String, Integer> thirdGramMap = new HashMap<>();
		
		// Read input files
		thirdGramMap = walkFileTree(JsonFolder, thirdGramMap);
//		thirdGramMap = walkFileTree(ehowJsonFolder, thirdGramMap);
		
		// Write to csv
		for (Map.Entry<String, Integer> entry : thirdGramMap.entrySet()) {
			String line = entry.getKey() + "," + entry.getValue() + "\n";
			fileWriter.write(line);
		}
		
		fileWriter.close();
		
		// ------------------------------------------------------------------------
		// Calc 3-gram of ehow json files
		// ------------------------------------------------------------------------
		
/*		if (Files.isDirectory(ehowJsonFolder)) {
			Files.walkFileTree(ehowJsonFolder, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
					
					// For each file, parse name ( = article title )
					String jsonfilepath = filePath.toString();
					String[] pathSplit = jsonfilepath.split("/");
					String title = pathSplit[pathSplit.length - 1];
					
					// 3-gram
					advancedThreeGram(title);
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		
		FileWriter fileWriter1 = new FileWriter(ehowOutputFile + "-1.csv");
		for (Map.Entry<String, Integer> entry : gramMap1.entrySet()) {
			String line = entry.getKey() + "," + entry.getValue() + "\n";
			fileWriter1.write(line);
		}
		fileWriter1.close();
		
		FileWriter fileWriter2 = new FileWriter(ehowOutputFile + "-2.csv");
		for (Map.Entry<String, Integer> entry : gramMap2.entrySet()) {
			String line = entry.getKey() + "," + entry.getValue() + "\n";
			fileWriter2.write(line);
		}
		fileWriter2.close();
		
		FileWriter fileWriter3 = new FileWriter(ehowOutputFile + "-3.csv");
		for (Map.Entry<String, Integer> entry : gramMap3.entrySet()) {
			String line = entry.getKey() + "," + entry.getValue() + "\n";
			fileWriter3.write(line);
		}
		fileWriter3.close();
		
		FileWriter fileWriter4 = new FileWriter(ehowOutputFile + "-4.csv");
		for (Map.Entry<String, Integer> entry : gramMap1n2.entrySet()) {
			String line = entry.getKey() + "," + entry.getValue() + "\n";
			fileWriter4.write(line);
		}
		fileWriter4.close();
		*/
	}
}
