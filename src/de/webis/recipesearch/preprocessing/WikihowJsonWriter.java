package de.webis.recipesearch.preprocessing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.sym.Name;

/**
 * This class extracts a set of information needed from html raw files 
 * for recipe searching and voice presentation.
 * <BR>
 * The output is Json files that will be indexed later.
 *  
 * @author Jiani 
 *
 */

public class WikihowJsonWriter {
	
	static final Path htmlsPath = Paths.get("/home/xiwo8493/Documents/thesis/corpus/wikihow-food/html");//"/home/xiwo8493/Documents/tmp/2");
	static final String outputFolder = "/home/xiwo8493/Documents/thesis/corpus/json/wikihow/";
	
	static int pageCount = 0;
	static int numFolder = 0;
	
	/**
	 * Creates .json file from string
	 * Stores the crawed data into json file
	 * @param String data to be saved
	 * @param File path
	 */
	private static void createJSONFile(String text, String filepath) {
		//write String to Json file
		try (FileWriter file = new FileWriter(filepath)) {
			file.write(text);
//			System.out.print("   Successfully created JSON file... \n");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error! Json file creation failed.");
		}
	}
	
	@SuppressWarnings("unchecked")
	private static String extrTitle(Document document, JSONObject json) {
		// Extract title
		String title = document.getElementsByClass("firstHeading").get(0).text();
		System.out.print(pageCount + "  " + title);
		json.put("title", title);
		return title;
	}
	
	@SuppressWarnings("unchecked")
	private static void extrCategory(Document document, JSONObject json) {
		// Extract category information from html and save to array
		JSONArray category = new JSONArray();
		Element catInfoContainer = document.getElementById("breadcrumb");
		Elements catInfoList = catInfoContainer.children();
		for (int i = 0; i < catInfoList.size(); i++) {
			if (i == 0 || i == 1) {		// Jump "Home" and "Categories"
				continue;
			} else {
				category.add(catInfoList.get(i).child(0).text());
			}
		}					
		json.put("category", category);
	}
	
	@SuppressWarnings("unchecked")
	private static void extrIntro(Document document, JSONObject json) {
		// Extract intro 
		Element introParent = document.getElementById("intro");
		Elements paragraphs = introParent.getElementsByTag("p");
		int listPsize = paragraphs.size();
		String intro = "";
		if (paragraphs.size() > 0) {			
			Element introElem = paragraphs.get(listPsize-1 < 0 ? 0 : listPsize-1);
			intro = introElem.text();
			Element nextElem = introElem.nextElementSibling();
			if (nextElem != null && nextElem.className() != null && !nextElem.className().isEmpty()) {
				while (!nextElem.className().equals("clearall")) {
					System.out.println("Additional intro info found!");
					intro += nextElem.text();
					nextElem = nextElem.nextElementSibling();
				}
			}
		}
		json.put("intro", intro);
	}
	
	@SuppressWarnings("unchecked")
	private static void extrRelArticles(Document document, JSONObject json) {
		// Extract related articles
		JSONArray related = new JSONArray();
		Elements relatedArticleSTitle = document.getElementsByClass("related-title-text");
		for (int i = 0; i < relatedArticleSTitle.size(); i++) {
			String articleTitle = relatedArticleSTitle.get(i).text();
			related.add(articleTitle);
		}
		json.put("related", related);
	}
	
	@SuppressWarnings("unchecked")
	private static void extrTipsWarnings(Document document, JSONObject json) {
		// Extract tips and warnings
		JSONArray tipsandwarnings = new JSONArray();
		Element tipsContainer = document.getElementById("tips");
		if (tipsContainer != null) {
			Elements tipsList = tipsContainer.child(0).children();
			for (Element tip : tipsList) {
				tipsandwarnings.add(tip.text());
			}
		}
		Element warningsContainer = document.getElementById("warnings");
		if (warningsContainer != null) {
			Elements warningsList = warningsContainer.child(0).children();
			for (Element warning : warningsList) {
				tipsandwarnings.add(warning.text());
			}
		}
		json.put("tips-warnings", tipsandwarnings);
	}
	
	@SuppressWarnings("unchecked")
	private static String extrStructureInfo(Document document, JSONObject json) {
		// Extract structure information (what are the parts: different steps, methods, variations?)
		Element methodToc = document.getElementById("method_toc");
		String structureType = "";
		if (methodToc != null) {
			structureType = methodToc.getElementsByTag("span").get(0).text();
			String[] words = structureType.replaceAll("[^a-zA-Z0-9]", " ").split("\\s+");
			structureType = words[words.length-1];
		}
		json.put("structure", structureType);
		return structureType;
	}
	
	@SuppressWarnings("unchecked")
	private static void extrIngrednTools(Document document, JSONObject json, Element ingredientsContainer) {
		// Extract ingredients
		JSONArray ingredientsAndTools = new JSONArray();
		if (ingredientsContainer != null) {
			
			Elements ingredContainerChildren = ingredientsContainer.children();
			for (int i = 0; i < ingredContainerChildren.size(); i++) {
				Element child = ingredContainerChildren.get(i);
				if (child.tagName().equals("h3")) {
					i++;
					JSONObject partIngred = new JSONObject();
					String partName = child.text();
					JSONArray partIngreds = new JSONArray();
					Element ingredList = ingredContainerChildren.get(i);
					for (Element ingred : ingredList.children()) {
						if (!ingred.tagName().equals("li")) {
							break;
						}
						partIngreds.add(ingred.text());
					}
					partIngred.put("part-name", partName);
					partIngred.put("part-ingredients", partIngreds);
					ingredientsAndTools.add(partIngred);
				} else if (child.tagName().equals("ul")) {
					JSONObject partIngred = new JSONObject();
					String partName = "General Ingredients";
					JSONArray partIngreds = new JSONArray();
					for (Element ingred : child.children()) {
						if (!ingred.tagName().equals("li")) {
							break;
						}
						partIngreds.add(ingred.text());
					}
					partIngred.put("part-name", partName);
					partIngred.put("part-ingredients", partIngreds);
					ingredientsAndTools.add(partIngred);
				} else if (child.tagName().equals("p")) {
					i++;
					JSONObject partIngred = new JSONObject();
					String partName = child.text();
					JSONArray partIngreds = new JSONArray();
					Element ingredList = ingredContainerChildren.get(i);
					for (Element ingred : ingredList.children()) {
						if (!ingred.tagName().equals("li")) {
							i--;
							break;
						}
						partIngreds.add(ingred.text());
					}
					partIngred.put("part-name", partName);
					partIngred.put("part-ingredients", partIngreds);
					ingredientsAndTools.add(partIngred);
				}
			}
		}
		
		// Extract things needed (tools etc.)
		Element thingsNeededContainer = document.getElementById("thingsyoullneed");
		JSONObject tools = new JSONObject();
		String partName = "Things needed";
		JSONArray partIngreds = new JSONArray();
		if (thingsNeededContainer != null) {
			Elements toolsList = thingsNeededContainer.getElementsByTag("li");
			for (Element tool : toolsList) {
				partIngreds.add(tool.text());
			}
			tools.put("part-name", partName);
			tools.put("part-ingredients", partIngreds);
			ingredientsAndTools.add(tools);
		}
		
		json.put("ingredients-thingsneeded", ingredientsAndTools);
	}
	
	@SuppressWarnings("unchecked")
	private static void extrInstructions(Document document, JSONObject json, String structureType) {
		
		// Extract instructions
		JSONArray instruction = new JSONArray();
		Elements parts = document.getElementsByClass("steps_list_2");
		// For each part(method/variation), get part name, ingredients and steps
		for (Element elem : parts) {
			JSONObject part = new JSONObject();
			String partName = elem.parent().previousElementSibling().text();	// Part name
			
			JSONArray steps = new JSONArray();									// Steps
			Elements stepList = elem.children();
			// For each step, get abstraction, details
			for (Element astep : stepList) {
				JSONObject step = new JSONObject();
				String stepText = astep.getElementsByTag("b").get(0).text();	// Abstraction of step
				JSONArray substeps = new JSONArray();							// Details
				JSONObject substep = new JSONObject();							// (Right now, only extract the complete detail)
				// For the detail (each sub-step) get text and tips
				Element detailContainer = astep.getElementsByClass("step").get(0);
				
				JSONArray tips = new JSONArray();								// Tips
				Elements tipsList = detailContainer.getElementsByTag("li");		
				for (Element atip : tipsList) {
					tips.add(atip.text());
				}
				detailContainer.children().remove();							// Clean container for detail text only
				String detail = detailContainer.text();	
				
				substep.put("sub-step", detail);
				substeps.add(substep);
				
				step.put("tips", tips);
				step.put("step", stepText);
				step.put("sub-steps", substeps);
				steps.add(step);
			}
			
			part.put("part", partName);
			part.put("steps", steps);
			instruction.add(part);
		}
		json.put("parts", instruction);
	}
	
	public static void main(String[] args) throws IOException {
		
		// Read all crawled htmls
		if (Files.isDirectory(htmlsPath)) {
			Files.walkFileTree(htmlsPath, new SimpleFileVisitor<Path>() {
				@SuppressWarnings("unchecked")
				@Override
				public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
					
					// For each file, parse content
					File htmlFile = new File(filePath.toString());
					Document document = Jsoup.parse(htmlFile, "UTF-8");
					pageCount++;
					
					// Initialize Json object
					JSONObject json = new JSONObject();
					

					Element ingredientsContainer = document.getElementById("ingredients");
					
					String structureType = extrStructureInfo(document, json);
					String title = extrTitle(document, json);
					extrCategory(document, json);
					extrIngrednTools(document, json, ingredientsContainer);
					extrInstructions(document, json, structureType);
					extrIntro(document, json);
					extrRelArticles(document, json);
					extrTipsWarnings(document, json);
					
					// Create articleID
					String articleID = "wikihow-";
					if (pageCount > 999) {
						articleID += pageCount;
					} else if (pageCount > 99) {
						articleID += "0" + pageCount;
					} else if (pageCount > 9) {
						articleID += "00" + pageCount;
					} else {
						articleID += "000" + pageCount;
					}
					System.out.println("\n" + articleID);
					json.put("articleID", articleID);
					
					// Write json file
					if (pageCount % 1000 == 1) {
						numFolder++;
					}
					String folderPath = outputFolder + numFolder + "/";
					String destPath = folderPath + title.replaceAll("[^A-Za-z0-9]", " ") + ".json";
					File folder = new File(folderPath);
					File file = new File(destPath);
					if (!folder.exists()) {
						try {
							folder.mkdirs();
						} catch (Exception e) {
							System.out.println("Error occurs while making new folder");
						}
					}
					file.createNewFile();
					createJSONFile(json.toString(), destPath);
					
//					System.out.println(json);
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}
	
}
