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
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * This class extracts a set of information needed from html raw files 
 * for recipe searching and voice presentation.
 * <BR>
 * The output is Json files that will be indexed later.
 *  
 * @author Jiani 
 *
 */

public class EhowJsonWriter {
	
	static final Path htmlsPath = Paths.get(/*"/home/xiwo8493/Documents/thesis/corpus/ehow/html");*/"/home/xiwo8493/Documents/tmp/1");
	static final String outputFolder = "/home/xiwo8493/Documents/thesis/corpus/ehow/json/";
	
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
		String title = document.getElementsByTag("h1").get(0).text();
		System.out.print(pageCount + "  " + title);
		
		json.put("title", title);
		return title;
	}
	
	@SuppressWarnings("unchecked")
	private static void extrCategory(Document document, JSONObject json) {
		// Extract category information from html and save to array
		JSONArray category = new JSONArray();
		Elements scriptTags = document.getElementsByTag("script");
		boolean found = false;
		for (Element script : scriptTags) {
			if (script.attr("type").equals("text/javascript")) {
				for (DataNode node : script.dataNodes()) {
					String nodeText = node.getWholeData();
					int catIndex = nodeText.indexOf("category");
					if (catIndex > -1) {
						String[] snippets = nodeText.split("push");
						String target= snippets[snippets.length-1];
						target = target.trim().substring(1, target.length()-3);
						
						JSONParser parser = new JSONParser();
						JSONObject catContainer = null;
						try {
							catContainer = (JSONObject)parser.parse(target);
						} catch (ParseException e) {
							System.out.println("OOps, JSON parsing failed");
							e.printStackTrace();
						}
						if (catContainer != null) {
							category.add(catContainer.get("category"));
							category.add(catContainer.get("subcategory"));
							category.add(catContainer.get("subsubcat"));
						}
						
						found = true;
						break;
					}
				}
			}
			if (found) {
				break;
			}
		}

		json.put("category", category);
	}
	
	@SuppressWarnings("unchecked")
	private static void extrIntro(Document document, JSONObject json) {
		// Extract intro 
		Elements pTags = document.getElementsByTag("p");
		String intro = pTags.get(0).text();
		
		json.put("intro", intro);
	}
	
	@SuppressWarnings("unchecked")
	private static void extrRelArticles(Document document, JSONObject json) {
		// Extract related articles
		JSONArray related = new JSONArray();
		Elements h2Tags = document.getElementsByTag("h2");
		for (Element h2 : h2Tags) {
			if (h2.text().equals("You May Like")) {
				Elements listOfArticles = h2.parent().getElementsByTag("li");
				for (Element li : listOfArticles) {
					related.add(li.text());
				}
			}
		}
		
		json.put("related", related);
	}
	
	@SuppressWarnings("unchecked")
	private static void extrTipsWarnings(Document document, JSONObject json) {
		// Extract tips and warnings
		JSONArray tipsandwarnings = new JSONArray();
		Elements h2Tags = document.getElementsByTag("h2");	
		for (Element h2 : h2Tags) {
			if (h2.text().equals("Tips & Warnings")) {
				String tip = h2.nextElementSibling().text();
				tipsandwarnings.add(tip);
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
	private static void extrIngrednTools(Document document, JSONObject json) {
		// Extract all things needed
		JSONArray ingredientsAndTools = new JSONArray();
		Elements h3Tags = document.getElementsByTag("h3");
		for (Element h3 : h3Tags) {
			if (h3.hasClass("headline3") && h3.text().equals("Things You'll Need")) {
				Elements listOfThings = h3.nextElementSibling().getElementsByTag("li");
				
				JSONObject partIngred = new JSONObject();
				String partName = "";
				if (h3.parent().parent().previousElementSibling() != null) {
					partName = h3.parent().parent().previousElementSibling().text();
				} else {
					partName = "Things needed";
				}
				JSONArray partIngreds = new JSONArray();
				Elements ingredList = h3.nextElementSibling().getElementsByTag("li");
				for (Element ingred : ingredList) {
					partIngreds.add(ingred.text());
				}
				partIngred.put("part-name", partName);
				partIngred.put("part-ingredients", partIngreds);
				ingredientsAndTools.add(partIngred);
//				
//				for (Element thing : listOfThings) {
//					ingredientsAndTools.add(thing.text());
//				}
			}
		}
		
		json.put("ingredients-thingsneeded", ingredientsAndTools);
	}
	
	@SuppressWarnings("unchecked")
	private static void extrSteps(Document document, JSONObject part, JSONArray instruction, Element partHeadline) {
		
		JSONArray steps = new JSONArray();									// Steps
		Element partHeadParent = partHeadline.parent();
		Elements stepList = null;
		if (partHeadParent.getElementsByClass("headline5").size() > 0) {		// determining between diff structures (new/old)
			stepList = partHeadParent.getElementsByClass("headline5");				// (old)
		} else if (partHeadParent.getElementsByClass("things-needed").size() > 0) {
			stepList = partHeadParent.parent().nextElementSibling().getElementsByClass("headline5");
		}
		
		if (stepList != null && stepList.size() > 0) {
			Iterator<Element> iter = stepList.iterator();
			while (iter.hasNext()) {
				boolean isStepHeader = iter.next().hasClass("head-alternate");
				if (!isStepHeader) {					  					  // filter real step headers
					iter.remove();
				}
			}
		}
		
		// case 2 & 3: "step x" / "part prepare - method frozen/dried"
		if (stepList != null && stepList.size() > 0) {
			for (Element stepHeader : stepList) {	
				JSONObject step = new JSONObject();
				String stepText = stepHeader.text();
				
				Element detailContainer = stepHeader.nextElementSibling();
				Elements detailContainerSpans = detailContainer.children();	  // Sub step 
				JSONArray substeps = new JSONArray();
				JSONArray tips = new JSONArray();								// Tips
				
				for (Element span : detailContainerSpans) {
					Element spanChild = span.child(0);						  // check if content in span is directly <p> 
					if (spanChild.tagName().equals("p")) {
						
						JSONObject substep = new JSONObject();
						String substepText = spanChild.text();
						
						substep.put("sub-step", substepText);
						substeps.add(substep);
						
					} else if (spanChild.hasClass("tip-warning")) {				// store tips <li> into array
						Elements tipsList = spanChild.getElementsByTag("li");
						for (Element atip : tipsList) {
							tips.add(atip.text());
						}
					}
				}
				step.put("step", stepText);
				step.put("tips", tips);
				step.put("sub-steps", substeps);
				steps.add(step);
			}
		} 
		// if the part doesn't have step headlines
		else {
			Elements listofPtags = partHeadParent.getElementsByTag("p");
			for (int i = 0; i < listofPtags.size(); i++) {
				Element pElement = listofPtags.get(i);
				if (pElement.className().isEmpty() && pElement.parent().tagName().equals("span")) {
					JSONObject step = new JSONObject();
					String stepText = pElement.text();
					
					JSONArray tips = new JSONArray();
					JSONArray substeps = new JSONArray();
					
					step.put("step", stepText);
					step.put("tips", tips);
					step.put("sub-steps", substeps);
					steps.add(step);
				}
			}
		}
		
		part.put("steps", steps);
		instruction.add(part);
	}
	
	@SuppressWarnings("unchecked")
	private static void extrSteps(Document document, JSONObject part, JSONArray instruction) {
		
		JSONArray steps = new JSONArray();									// Steps
		Elements stepList = document.getElementsByClass("headline5");	
		if (stepList.size() > 0) {
			Iterator<Element> iter = stepList.iterator();
			while (iter.hasNext()) {
				boolean isStepHeader = iter.next().hasClass("head-alternate");
				if (!isStepHeader) {					  					  // filter real step headers
					iter.remove();
				}
			}
		}
		
		// case 1: no step headers at all (only paragraphs)
		if (stepList.size() == 0) {
			Elements listofPtags = document.getElementsByTag("article").get(0).getElementsByTag("p");
			for (int i = 1; i < listofPtags.size(); i++) {
				Element pElement = listofPtags.get(i);
				if (pElement.className().isEmpty() && pElement.parent().tagName().equals("span")) {
					JSONObject step = new JSONObject();
					String stepText = pElement.text();
					
					JSONArray tips = new JSONArray();
					JSONArray substeps = new JSONArray();
					
					step.put("step", stepText);
					step.put("tips", tips);
					step.put("sub-steps", substeps);
					steps.add(step);
				}
			}
		}
		
		// case 2 & 3: "step x" / "part prepare - method frozen/dried"
		if (stepList != null && stepList.size() > 0) {
			for (Element stepHeader : stepList) {	
				JSONObject step = new JSONObject();
				String stepText = stepHeader.text();
				
				Element detailContainer = stepHeader.nextElementSibling();
				Elements detailContainerSpans = detailContainer.children();	  // Sub step 
				JSONArray substeps = new JSONArray();
				JSONArray tips = new JSONArray();								// Tips
				
				for (Element span : detailContainerSpans) {
					Element spanChild = span.child(0);						  // check if content in span is directly <p> 
					if (spanChild.tagName().equals("p")) {
						
						JSONObject substep = new JSONObject();
						String substepText = spanChild.text();
						
						substep.put("sub-step", substepText);
						substeps.add(substep);
						
					} else if (spanChild.hasClass("tip-warning")) {				// store tips <li> into array
						Elements tipsList = spanChild.getElementsByTag("li");
						for (Element atip : tipsList) {
							tips.add(atip.text());
						}
					}
				}
				step.put("step", stepText);
				step.put("tips", tips);
				step.put("sub-steps", substeps);
				steps.add(step);
			}
		} 

		part.put("steps", steps);
		instruction.add(part);
	}
	
	@SuppressWarnings("unchecked")
	private static void extrInstructions(Document document, JSONObject json) {
				
		// Extract instructions
		JSONArray instruction = new JSONArray();
		Elements parts = document.getElementsByTag("article").get(0).getElementsByClass("Heading2");
		
		// For each part(method/variation), get part name, ingredients and steps, IF MULTIPLE PARTS EXIST
		if (parts.size() > 0) {
			for (Element partElem : parts) {
				JSONObject part = new JSONObject();
				String partName = partElem.text();										// Part name / recipe name / method

				extrSteps(document, part, instruction, partElem);
				part.put("part", partName);
			}
			
			System.out.println("Multiple parts");
		}
		
		// If only one part exists with no special heading:
		else {
			JSONObject part = new JSONObject();
			String partName = "";
			
			extrSteps(document, part, instruction);
			part.put("part", partName);
			
			System.out.println("Only one part");
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
					
					String title = extrTitle(document, json);
					String firstChar = Character.toString(title.charAt(0));
					if (!firstChar.matches("[0-9]")) {
						try {
							extrIntro(document, json);
							extrCategory(document, json);
							extrIngrednTools(document, json);
							extrRelArticles(document, json);
							extrTipsWarnings(document, json);
							json.put("structure", "");
							
							System.out.println();
							extrInstructions(document, json);
							
							// Create articleID
							String articleID = "ehow-";
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
							
						} catch (Exception e) {
							System.out.print("\t Error! Something went wrong during extraction. JSON File not written ");
							e.printStackTrace();
						}
						
					}
					
					
					
					
					
					
//					System.out.println(json);
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}
	
}
