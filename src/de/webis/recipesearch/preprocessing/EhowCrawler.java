package de.webis.recipesearch.preprocessing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

public class EhowCrawler {
	
	public static void main(String[] args) throws IOException {
		
		String ehow = "https://www.ehow.com";
		String ehowFood =  "https://www.ehow.com/food/?page=";
		int numAllPages = 231;
		String output = "/home/xiwo8493/Documents/ehow/html/";
		
		for (int page = 2; page < numAllPages+1; page++) {
			
			// Get parent html DOM
			Document doc = Jsoup.connect(ehowFood + page).get();

			// Parse for article links
			Elements allArticleLinks = doc.getElementsByClass("title__link");
			for (Element articleLinkObj : allArticleLinks) {
				String link = articleLinkObj.attr("href");
				String title = articleLinkObj.attr("title");
				title = title.replaceAll("[^a-zA-Z0-9]", " ");
				
				// Retrieve site html after js being loaded in a virtual browser
					// Setting profiles 
				System.setProperty("webdriver.gecko.driver", "/home/xiwo8493/geckodriver");
				WebDriver driver = new FirefoxDriver();
				FirefoxProfile profile = new FirefoxProfile();
				profile.setPreference("permissions.default.stylesheet", 2);
					// Retrieve
				String baseURl = ehow + link;
				driver.get(baseURl);
		        driver.findElement(By.tagName("body")).sendKeys("Keys.ESCAPE");;
		        String content=driver.getPageSource();
		        driver.quit();  
				
				// Create empty file
				int numFolder = (page + 9) / 10;	// create a new folder for every 10 pages
				String folderPath = output + numFolder + "/";
				File folder = new File(folderPath);
				File file = new File(folderPath +  page + "-" + title + ".html");
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
				writer.write(content);
				
				System.out.println("File created " + file.toString());
				System.out.println("Page " + page + "\t Folder " + numFolder);
			}
			
		}
		
	}
	
	
}
