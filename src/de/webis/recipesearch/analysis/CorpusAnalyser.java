package de.webis.recipesearch.analysis;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
 * Get structure info from a json order
 * @author xiwo8493
 *
 */

public class CorpusAnalyser {
	
	static final Path jsonPath = Paths.get("/home/xiwo8493/Documents/thesis/corpus/json/ehow");
	
	static int fileCnt = 0, fileWithParts = 0, fileWithSteps = 0, fileWithSubsteps = 0, fileWithIngreds = 0;
	static int partsCnt = 0, stepsCnt = 0, substepsCnt = 0, ingredientsCnt = 0;
	
	public static void main(String[] args) throws IOException {
		// Read all crawled htmls
		if (Files.isDirectory(jsonPath)) {
			Files.walkFileTree(jsonPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
					fileCnt++;
					JSONObject json = null;
					boolean stepExists = false;
					boolean substepExists = false;

					// For each file, parse content
					try {
						json = (JSONObject) new JSONParser().parse(new FileReader(filePath.toString()));
					} catch (ParseException e) {
						e.printStackTrace();
					}
					
					JSONArray parts = (JSONArray) json.get("parts");
					int numParts = parts.size();
					if (numParts > 1) {
						fileWithParts++;
						partsCnt += numParts;
					}
					
					for (int i = 0; i < parts.size(); i++) {
						JSONObject part = (JSONObject) parts.get(i);
						JSONArray steps = (JSONArray) part.get("steps");
						int numSteps = steps.size();
						if (numSteps > 0) {
							stepExists = true;
							stepsCnt += numSteps;
						}
						
						for (int j = 0; j < numSteps; j++) {
							JSONObject step = (JSONObject) steps.get(j);
							JSONArray substeps = (JSONArray) step.get("sub-steps");
							int numSubsteps = substeps.size();
							if (numSubsteps > 0) {
								substepExists = true;
								substepsCnt += numSubsteps;
							}
							// Break when substeps are found
							if (substepExists) {
								fileWithSubsteps++;
								break;
							}
						}
						
						// Break when steps are found
						if (stepExists) {
							fileWithSteps++;
							break;
						}
					}

					if (!stepExists) {
						System.out.println("traitor!" + filePath.toString());
					}
					
					
					// Check ingreds
					JSONArray ingredlists = (JSONArray) json.get("ingredients-thingsneeded");
					int numList = ingredlists.size();
					if (numList > 0) {
						fileWithIngreds++;
					}
					for (int i = 0; i < numList; i++) {
						JSONObject list = (JSONObject) ingredlists.get(i);
						int listsize = list.size();
						ingredientsCnt += listsize;
					}
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		
		System.out.println("filecnt = " + fileCnt + "\n" 
							+ "filewithparts = " + fileWithParts + "\n"
							+ "filewithsteps = " + fileWithSteps + "\n"
							+ "filewithsubsteps = " + fileWithSubsteps + "\n"
							+ "filewithingreds = " + fileWithIngreds + "\n"
							+ "partscnt = " + partsCnt + "---" + partsCnt/fileWithParts + "\n"
							+ "stepscnt = " + stepsCnt + "---" + stepsCnt/fileWithSteps + "\n"
							+ "substepsCnt = " + substepsCnt + "---" + substepsCnt/fileWithSubsteps + "\n"
							+ "ingredCnt = " + ingredientsCnt + "---" + ingredientsCnt/fileWithIngreds + "\n");
	}

}
