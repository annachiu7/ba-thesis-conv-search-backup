package de.webis.recipesearch.preprocessing;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WikihowRecipeFilter {
	
	static final Path htmlsPath = Paths.get("/home/xiwo8493/Documents/thesis/corpus/wikihow-new/html");
	static final String outputFolder = "/home/xiwo8493/Documents/thesis/corpus/wikihow-food/html/";
//	static final Path htmlsPath = Paths.get("/home/xiwo8493/Documents/tmp");
	
	static int pageCount = 0;
	static int recipeCount = 0;
	static int numFolder = 0;
	
	public static void main(String[] args) throws IOException {
		
		// Read all crawled htmls
		if (Files.isDirectory(htmlsPath)) {
			Files.walkFileTree(htmlsPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
					
					// For each file, parse content
					File htmlFile = new File(filePath.toString());
					Document document = Jsoup.parse(htmlFile, "UTF-8");
					pageCount++;
					System.out.println("Page " + pageCount);
					
					// Find category information
					Element catInfoContainer = document.getElementById("breadcrumb");
					
					
					if (catInfoContainer != null) {
						Elements catInfoList = catInfoContainer.children();
						int catlvls = catInfoList.size();
						
						if (catlvls > 2) {
							String toplvlCat = catInfoList.get(2).child(0).text();	// Food and Entertaining
							String seclvlCat = null;
							
							if (toplvlCat.equals("Food and Entertaining")) {
								try {
									seclvlCat = catInfoList.get(3).child(0).text();	// Recipes
									
									// Check if the current page is a recipe
									if (seclvlCat.equals("Recipes")) {
										recipeCount++;
										System.out.println(recipeCount + " Recipe found! ");
										
										// Copy the file to the destination folder
										if (recipeCount % 1000 == 1) {
											numFolder++;
										}
										String folderPath = outputFolder + numFolder + "/";
										File folder = new File(folderPath);
										if (!folder.exists()) {
											try {
												folder.mkdirs();
											} catch (Exception e) {
												System.out.println("Error occurs while making new folder");
											}
										}
										Files.copy(filePath, (new File(folderPath + htmlFile.getName())).toPath(), 
												StandardCopyOption.REPLACE_EXISTING);
									}
								} catch (Exception e) {
									// TODO: handle exception
								}
								
							}	
							
						}
					}
					
					
					return FileVisitResult.CONTINUE;
				}
			});
		}

	}
}
