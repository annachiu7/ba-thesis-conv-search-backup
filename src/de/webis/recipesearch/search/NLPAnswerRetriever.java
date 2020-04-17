package de.webis.recipesearch.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;


public class NLPAnswerRetriever {
	
//	private static final String url = "http://demo.allennlp.org/machine-comprehension";
	private static final String url = "http://localhost:8000/predict/machine-comprehension";
	
//	public static String getAnswer(String passage, String question) throws InterruptedException {
//		
//		System.setProperty("webdriver.gecko.driver", "/home/xiwo8493/geckodriver");
//		WebDriver driver = new FirefoxDriver();
//		FirefoxProfile profile = new FirefoxProfile();
//		profile.setPreference("permissions.default.stylesheet", 2);
//		
//		driver.get(url);
//		driver.findElement(By.id("input--mc-passage")).sendKeys(passage);
//		driver.findElement(By.id("input--mc-question")).sendKeys(question);
//		driver.findElement(By.id("input--mc-submit")).click();
//		
//		
//		synchronized (driver) {
//			driver.wait(500);
//		}
//
//		List<WebElement> elements = driver.findElements(By.className("model__content__summary"));
//		String answer = elements.get(0).getText();
//		driver.quit();
//		
//		return answer;
//	}
	
	public static String getAnswer(String passage, String question) throws IOException {
		String answer = "";
		
		Map<String, String> request = new HashMap<String, String>();
		request.put("passage", passage);
		request.put("question", question);
		String json = new JSONObject(request).toJSONString();
		
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(new StringEntity(json, "UTF-8"));
		httppost.setHeader("Accept", "application/json");
		httppost.setHeader("Content-Type", "application/json");

		//Execute and get the response.
		HttpResponse response = httpclient.execute(httppost);
		String output = EntityUtils.toString(response.getEntity(), "UTF-8");
		try {
			JSONObject answerObj = (JSONObject) new JSONParser().parse(output);
			String best_span_str = (String) answerObj.get("best_span_str");
			answer = best_span_str;
		} catch (Exception e) {
		}
		
		System.out.println(answer);
		return answer;
		
	}
	
	
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		getAnswer("Cook for 30 seconds first. "
				+ "If the eggs do not seem solid enough, cook for the additional 15 seconds.", "How long should I cook?");
	}
}
