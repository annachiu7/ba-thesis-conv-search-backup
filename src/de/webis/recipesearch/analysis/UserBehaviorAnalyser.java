package de.webis.recipesearch.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class UserBehaviorAnalyser {
	
	private static final Path LogFileLocation = Paths.get("/home/xiwo8493/logging");
	private static Map<String, Integer> queryMap = new HashMap<>();
	private static Map<String, Integer> includeMap = new HashMap<>();
	private static Map<String, Integer> excludeMap = new HashMap<>();
	private static Map<String, Integer> categoryMap = new HashMap<>();
	private static Map<String, Integer> confirmedMap = new HashMap<>();
	private static Map<String, String> maxVals = new HashMap<>();
	
	private static void countWordOccurrences(Map<String, Integer> map, String content) {
		// calc query metrics
		String[] contentWords = content.split("\\s+");
		for (String word : contentWords) {
			map.putIfAbsent(word, 0);
			map.put(word, map.get(word)+1);
		}
	}
	
	private static void countPhraseOccurrences(Map<String, Integer> map, String phrase) {
		if (!phrase.equals("Recipes")) {
			map.putIfAbsent(phrase, 0);
			map.put(phrase, map.get(phrase)+1);
		}
	}
	
	private static void printMapResults(Map<String, Integer> map) {
		boolean removal = false;
		for (Entry<String, Integer> entry : map.entrySet()) {
			if (entry.getKey().equals("null")) {
				removal = true;
			} else {
				System.out.println(entry.getKey() + "\t" + entry.getValue());
			}
		}
		if (removal) {
			map.remove("null");
		}
	}
	
	private static void removeNullEntries(Map<String, Integer> map) {
		boolean removal = false;
		for (Entry<String, Integer> entry : map.entrySet()) {
			if (entry.getKey().equals("null")) {
				removal = true;
			} 
		}
		if (removal) {
			map.remove("null");
		}
	}
	
	private static void getMapMaxValue(Map<String, Integer> map, String mapname) {
		String key = "";
		int maxVal = 0;
		for (Entry<String, Integer> entry : map.entrySet()) {
			int value = entry.getValue();
			if (value > maxVal) {
				maxVal = value;
				key = entry.getKey();
			}
		}
		if (maxVal > 1) {
			maxVals.put(mapname, key);
			System.out.println(mapname + ": \t" + key);
		}
	}
	
	public static Map<String, Integer> allAccessedCats() {
		printMapResults(categoryMap);
		return categoryMap;
	}
	
	public static Map<String, String> analyse() throws IOException {
		if (Files.isDirectory(LogFileLocation)) {
			Files.walkFileTree(LogFileLocation, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
					
					BufferedReader in = new BufferedReader(new FileReader(filePath.toString()));
					
					String line = in.readLine();
					while ((line = in.readLine()) != null) {
						
						String[] split = line.split(": ");
						String name = split[0];
						String content = split[1];
						
						// Calc occurrences for query, includes, excludes, categories
						if (name.equals("query")) {
							countWordOccurrences(queryMap, content);
						}
						else if (name.equals("Include ingredients")) {
							countWordOccurrences(includeMap, content);
						}
						else if (name.equals("Exclude ingredients")) {
							countWordOccurrences(excludeMap, content);
						}
						else if (name.equals("Category restriction")) {
							String[] categories = content.split(",");
							if (categories.length == 1) {
								countPhraseOccurrences(categoryMap, content);
							} else {
								for (String category : categories) {
									countPhraseOccurrences(categoryMap, category);
								}
							}
						}
						else if (name.equals("Confirmed recipe")) {
							countWordOccurrences(confirmedMap, content);
						}
					}
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		removeNullEntries(queryMap);
		getMapMaxValue(queryMap, "query");
		removeNullEntries(includeMap); 
		getMapMaxValue(includeMap, "include"); 
		removeNullEntries(excludeMap);
		getMapMaxValue(excludeMap, "exclude"); 
		removeNullEntries(categoryMap);
		getMapMaxValue(categoryMap, "category"); 
		removeNullEntries(confirmedMap);
		getMapMaxValue(confirmedMap, "confirmed");
		
		return maxVals;
	}
	
	public static void main(String[] args) throws IOException {
		
		analyse();
		
		
		printMapResults(queryMap);
		getMapMaxValue(queryMap, "query"); System.out.println();
		printMapResults(includeMap); 
		getMapMaxValue(includeMap, "include"); System.out.println();
		printMapResults(excludeMap);
		getMapMaxValue(excludeMap, "exclude"); System.out.println();
		printMapResults(categoryMap);
		getMapMaxValue(categoryMap, "category"); System.out.println();
		printMapResults(confirmedMap);
		getMapMaxValue(confirmedMap, "confirmed");
		
	}
}
