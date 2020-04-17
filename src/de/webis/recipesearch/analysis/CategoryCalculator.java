package de.webis.recipesearch.analysis;

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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CategoryCalculator {

	static final Path jsonPath = Paths.get("/home/xiwo8493/Documents/thesis/corpus/json/wikihow");
	
	static Map<String, Integer> secLvlCat = new HashMap<>();
	static Map<String, Integer> thirdLvlCat = new HashMap<>();
	static Map<String, Integer> frthLvlCat = new HashMap<>();
	static int cnt2 = 0;
	
	public static void main(String[] args) throws IOException {
		
		if (Files.isDirectory(jsonPath)) {
			Files.walkFileTree(jsonPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
					
					JSONObject json = null;
					
					// For each file, parse content
					try {
						json = (JSONObject) new JSONParser().parse(new FileReader(filePath.toString()));
					} catch (ParseException e) {
						e.printStackTrace();
					}
					
					JSONArray category = (JSONArray) json.get("category");
					
					if (category.size() > 1) {
						String key = (String) category.get(1);
						secLvlCat.putIfAbsent(key, 0);
						secLvlCat.put(key, secLvlCat.get(key)+1);
						if (category.size() > 2) {
							key = (String) category.get(2);
							thirdLvlCat.putIfAbsent(key, 0);
							thirdLvlCat.put(key, thirdLvlCat.get(key)+1);
							if (category.size() > 3) {
								key = (String) category.get(3);
								frthLvlCat.putIfAbsent(key, 0);
								frthLvlCat.put(key, frthLvlCat.get(key)+1);
							}
						}
					}
					
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		
		for (Entry<String, Integer> entry : secLvlCat.entrySet()) {
			System.out.println(entry.getKey() + " = " + entry.getValue());
		}
		
		
		System.out.println();
		for (Entry<String, Integer> entry : thirdLvlCat.entrySet()) {
			System.out.println(entry.getKey() + " = " + entry.getValue());
		}
		
		System.out.println("\n moooooooooost detailed");
		for (Entry<String, Integer> entry : frthLvlCat.entrySet()) {
			System.out.println(entry.getKey() + " = " + entry.getValue());
		}
	}
	
	
}
