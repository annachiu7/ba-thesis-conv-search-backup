package de.webis.recipesearch.search;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.lang3.text.StrBuilder;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

public class SearchFiles {
	
	public SearchFiles() {}
	
	// -------------------------------------------------------------------------
	// PROPERTIES
	// -------------------------------------------------------------------------
	private static final String INDEX_PATH = "/home/xiwo8493/Documents/thesis/corpus/indexes";
	private static final String[] STOP_WORDS = {"can","i","can i","Can","to","how","how to","you","could","do","make","cook"};
	
	
	// -------------------------------------------------------------------------
	// Index fields
	// -------------------------------------------------------------------------
	static final String TITLE = "title";
	static final String ARTICLE_ID = "articleID";
	static final String FILE_PATH = "filePath";
	static final String INGREDIENTS = "ingredients";
	static final String CATEGORIES = "categories";
	
	
	// -------------------------------------------------------------------------
	// Methods
	// -------------------------------------------------------------------------
	
	/**
	 * Retrieve recipes
	 */
	public static String retrieveRecipes(String querystring, int numHits) {
		
//		List<String> resURLlist = new ArrayList<String>();
		
		// Setting up
		try {
			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(INDEX_PATH)));
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyser = new StandardAnalyzer(setStopWords(STOP_WORDS));
			
			// Create Lucene Queries with the prepared queries
			QueryParser queryParser = new QueryParser(TITLE,analyser);
			queryParser.setDefaultOperator(Operator.OR);
			try {
				Query query = queryParser.parse(querystring);
				TopDocs searchRes = searcher.search(query, numHits);
				
				ScoreDoc[] hits = searchRes.scoreDocs;
				System.out.println("Articles found:" + hits.length);
				
				String url = "";
				if (hits.length < numHits) {
					return null;
				}
				for (int i = 0; i < hits.length; i++) {
					Document doc = searcher.doc(hits[i].doc);
					url = doc.get(FILE_PATH);
//					resURLlist.add(url);
				}
				
				return url;
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
//	/**
//	 * Retrieve recipes
//	 */
//	public static void retrieveRecipes(String querystring) {
//		
//		// Setting up
//		try {
//			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(INDEX_PATH)));
//			IndexSearcher searcher = new IndexSearcher(reader);
//			Analyzer analyser = new StandardAnalyzer(setStopWords(STOP_WORDS));
//			
//			// Create Lucene Queries with the prepared queries
//			QueryParser queryParser = new QueryParser(TITLE, analyser);
//			queryParser.setDefaultOperator(Operator.OR);
//			try {
//				Query query = queryParser.parse(querystring);
//				TopDocs searchRes = searcher.search(query, 10);
//				
//				ScoreDoc[] hits = searchRes.scoreDocs;
//				System.out.println("Articles found:" + hits.length);
//				
//				for (int i = 0; i < hits.length; i++) {
//					Document doc = searcher.doc(hits[i].doc);
//					System.out.println(doc.get(TITLE));
//				}
//				
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	/**
	 * Retrieve recipes with category and ingredient restrictions
	 */
	public static String retrieveRecipes(String querystring, String category,
			String includIngredient, String excludIngredient, int numHits) {
		
		String ingredQuerystring = "";
		if (includIngredient != null) {
			ingredQuerystring += includIngredient + " ";
		}
		if (excludIngredient != null) {
			String[] exluded = excludIngredient.split(" ");
			for (String ingred : exluded) {
				ingredQuerystring += " NOT " + ingred;
			}
		}
		
		System.out.println("ingredient restriction: " + ingredQuerystring);
		System.out.println("category restriction:   " + category);
		
		// Setting up
		try {
			IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(INDEX_PATH)));
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyser = new StandardAnalyzer(setStopWords(STOP_WORDS));
			
			// Create Lucene Queries with the prepared queries
			QueryParser queryParser = new QueryParser(TITLE, analyser);
			QueryParser ingredParser = new QueryParser(INGREDIENTS, analyser);
			QueryParser volcabParser = new QueryParser(INGREDIENTS, analyser);
			QueryParser catParser = new QueryParser(CATEGORIES, analyser);
			queryParser.setDefaultOperator(Operator.OR);
			try {
				Query query = null;
				Query ingredQuery = null;
				Query volcabQuery = null;
				Query catQuery = null;
				Query combinedQuery = null;
				
				// All these if clauses is because we can't build a null query into BooleanQuery
				if (querystring!=null && !querystring.isEmpty()) {
					query = queryParser.parse(querystring);
				}
				if (!ingredQuerystring.isEmpty() && category == null) {
					// If meat is excluded
					if (ingredQuerystring.indexOf("NOT meat") > -1) {
						String notString = getNOTVolcab();
						ingredQuerystring += " " + notString;
					}
					
					ingredQuery = ingredParser.parse(ingredQuerystring);
					combinedQuery = buildCollectiveQuery(query, ingredQuery);
					
					// If meat should be included, add boost
					if (ingredQuerystring.indexOf("NOT meat") == -1 && 
							ingredQuerystring.indexOf("meat") > -1) {
						String volcab = getVolcab();
						Query volcabq = volcabParser.parse(volcab);
						volcabQuery = new BoostQuery(volcabq, 0.006f);
						combinedQuery = buildCollectiveQuery(query, volcabQuery, ingredQuery);
					}
				}
				else if (category != null && ingredQuerystring.isEmpty()) {
					catQuery = catParser.parse(category);
					combinedQuery = buildCollectiveQuery(query, catQuery);
				}
				else if (category != null && !ingredQuerystring.isEmpty()) {
					ingredQuery = ingredParser.parse(ingredQuerystring);
					catQuery = catParser.parse(category);
					combinedQuery = buildCollectiveQuery(query, ingredQuery, catQuery);
				} else {
					combinedQuery = query;
				}
				
				
				TopDocs searchRes = searcher.search(combinedQuery, numHits);
				ScoreDoc[] hits = searchRes.scoreDocs;
				
				String url = "";
				if (hits.length < numHits) {
					return null;
				}
				for (int i = 0; i < hits.length; i++) {
					Document doc = searcher.doc(hits[i].doc);
					System.out.println(doc.get(TITLE));
//					System.out.println(doc.get(CATEGORIES));
//					System.out.println(doc.get(INGREDIENTS));
					url = doc.get(FILE_PATH);
//					System.out.println(searcher.explain(combinedQuery, hits[i].doc));
				}
				
				return url;
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	
	private static Query buildCollectiveQuery(Query query1, Query query2) {
		if (query1 == null) {
			return query2;
		}
		
		BooleanQuery combinedQuery = new BooleanQuery.Builder()
				.add(query1, BooleanClause.Occur.MUST)
				.add(query2, BooleanClause.Occur.MUST)
				.build();
		return combinedQuery;
	}
	
	private static Query buildCollectiveQuery(Query query, Query ingredQuery, Query catQuery) {
		if (query == null) {
			return buildCollectiveQuery(ingredQuery, catQuery);
		} 
		
		BooleanQuery combinedQuery = new BooleanQuery.Builder()
				.add(query, BooleanClause.Occur.MUST)
				.add(ingredQuery, BooleanClause.Occur.MUST)
				.add(catQuery, BooleanClause.Occur.MUST)
				.build();
		return combinedQuery;
	}

	private static CharArraySet setStopWords(String[] stopWords) {
		Analyzer analyzer = new StandardAnalyzer();
		CharArraySet stopwords = ((StopwordAnalyzerBase) analyzer).getStopwordSet();
		CharArraySet customStopwords = CharArraySet.copy(stopwords);
		
		for (int i = 0; i < stopWords.length; i++) {
			customStopwords.add(stopWords[i].toCharArray());
		}
		
		analyzer.close();
		return customStopwords;
	}
	
	private static String getVolcab() {
		try {
			return SynVolcabGenerator.getMeatVolcab().toString();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	private static String getNOTVolcab() {
		try {
			StringBuilder stringBuilder = new StringBuilder();
			String volcabString =  SynVolcabGenerator.getMeatVolcab().toString();
			String[] volcab = volcabString.split(" ");
			
			for (String word : volcab) {
				stringBuilder.append("NOT " + word + " ");
			}
			System.out.println(stringBuilder.toString());
			return stringBuilder.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	
	// -------------------------------------------------------------------------
	// Main
	// -------------------------------------------------------------------------

	public static void main(String[] args) throws IOException {
		
		
//		retrieveRecipes("cook chicken");
		retrieveRecipes("make no bake cake", null, null, null, 3);

	}
}
