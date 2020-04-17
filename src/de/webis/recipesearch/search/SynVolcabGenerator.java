package de.webis.recipesearch.search;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SynVolcabGenerator {
	
	private final static String VOLCAB_PATH = "data/hyponym-dics/";
	
	public static StringBuilder getMeatVolcab() throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		
		BufferedReader in = new BufferedReader(new FileReader(VOLCAB_PATH + "meat.txt"));
		
		String line;
		while ((line = in.readLine()) != null) {
			stringBuilder.append(line + " ");
		}
		return stringBuilder;
	}
}
