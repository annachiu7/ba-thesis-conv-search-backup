package de.webis.recipesearch.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WikihowCrawler {
	
	static final Path sitemapDirPath = Paths.get("/home/xiwo8493/Documents/thesis/wstud-thesis-qu/data/wikihow-sitemap");
	static final String foodLexicon = "/home/xiwo8493/Documents/thesis/wstud-thesis-qu/data/food-lexicon.csv";
	static final String outputFolder = "/home/xiwo8493/Documents/thesis/corpus/wikihow-new/html/";
	static List<String> mLex;
	
	
	
/*	private static void readLexicon() throws IOException {
		FileReader lexReader = new FileReader(foodLexicon);
		BufferedReader bufferedLexReader = new BufferedReader(lexReader);
		List<String> tokens = new ArrayList<String>();
		String line;
		
		// Reading lexicon
		while ((line = bufferedLexReader.readLine())!= null) {
			if (!line.isEmpty()) {
				tokens.add(line);
			}
		}
		
		// Close readers
		bufferedLexReader.close();
		lexReader.close();
		mLex = tokens;
	}
*/	
	// Set folder numbering (each folder contains at max 1000 files)
	static int numFolder = 35;
	static int count = 35275;
	static int tmp_cnt = 0;
	
	public static void main(String[] args) throws IOException {
		
		// Read lexicon and save tokens in a member list object
//		readLexicon();
		
		// Read sitemaps
		if (Files.isDirectory(sitemapDirPath)) {
			Files.walkFileTree(sitemapDirPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
					File sitemap = new File(filePath.toString());
					
					// For each sitemap, parse document 
					Document document = Jsoup.parse(sitemap, "UTF-8");
					System.out.println("\n sitemap read");
					
					// Locate links with <loc> tags
					Elements links = document.getElementsByTag("loc");
					System.out.println(links.size());
					
					for (Element link : links) {
						if (tmp_cnt < count) {
							tmp_cnt++;
							System.out.println("Counting up, now: " + tmp_cnt);
						} 
						else {
							String linkstring = link.text().replaceAll("https://www.wikihow.com/", "");
							String linktext = linkstring.replaceAll("[^A-Za-z0-9]", " ");
							Document site = null;
							try {
								site = Jsoup.connect(link.text()).get();
							} catch (Exception e) {
								System.out.println("Error connecting");
							}
							count++;
							tmp_cnt++;
							if (count % 1000 == 1) {
								numFolder++;
							}
							
							if (site != null) {
								// Create empty file
								String folderPath = outputFolder + numFolder + "/";
								File folder = new File(folderPath);
								File file = new File(folderPath +  linktext + ".html");
								if (!folder.exists()) {
									try {
										folder.mkdirs();
									} catch (Exception e) {
										System.out.println("Error occurs while making new folder");
									}
								}
								file.createNewFile();
								
								// Write content
								FileWriter writer = new FileWriter(file);
								writer.write(site.toString());
								System.out.println((count-1) + " documents crawled. Folder " + numFolder);
								try {
									Thread.sleep(1000);
								} catch (Exception e) {
									System.out.println("Error when trying to delay");
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
