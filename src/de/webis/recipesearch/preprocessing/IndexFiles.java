package de.webis.recipesearch.preprocessing;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class IndexFiles {
	
	// -------------------------------------------------------------------------
	// Property
	// -------------------------------------------------------------------------
	private static final String SOURCE_PATH = "/home/xiwo8493/Documents/thesis/corpus/json";
	private static final String INDEX_PATH = "/home/xiwo8493/Documents/thesis/corpus/indexes";
//	private static final String SOURCE_PATH = "/home/xiwo8493/Documents/tmp";
//	private static final String INDEX_PATH = SOURCE_PATH;
	private static final String[] STOP_WORDS = {"can","I", "can I"};
	
	
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
	 * Method from lucene demo
	 */
	public static void createIndex() {
		String docsPath = SOURCE_PATH;
		boolean create = true;

		final Path docDir = Paths.get(docsPath);

		if (!Files.isReadable(docDir)) {
			System.out.println("Document directory '" + docDir.toAbsolutePath()
					+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}

		Date start = new Date();
		try {
			System.out.println("Indexing to directory '" + INDEX_PATH + "'...");

			Directory dir = FSDirectory.open(Paths.get(INDEX_PATH));
			Analyzer analyzer = new StandardAnalyzer(setStopWords(STOP_WORDS));
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

			if (create) {
				// Create a new index in the directory, removing any
				// previously indexed documents:
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				// Add new documents to an existing index:
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}

			// Optional: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer. But if you do this, increase the max heap
			// size to the JVM (eg add -Xmx512m or -Xmx1g):
			//
			// iwc.setRAMBufferSizeMB(256.0);

			IndexWriter writer = new IndexWriter(dir, iwc);
			indexDocs(writer, docDir);

			// NOTE: if you want to maximize search performance,
			// you can optionally call forceMerge here. This can be
			// a terribly costly operation, so generally it's only
			// worth it when your index is relatively static (ie
			// you're done adding documents to it):
			//
			// writer.forceMerge(1);

			writer.close();

			Date end = new Date();
			System.out.println(end.getTime() - start.getTime() + " total milliseconds");

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}
	}
	
	
	/**
	 * Read JSON files and prepare for index
	 * 
	 * @throws ParseException
	 * @throws IOException
	 */
	static JSONObject readJson(String filename) throws IOException, ParseException {
		JSONParser parser = new JSONParser();
		try {
			Object object = parser.parse(new FileReader(filename));

			// convert Object to JSONObject
			JSONObject jsonObject = (JSONObject) object;
			return jsonObject;
		} catch (FileNotFoundException e) {
		}
		return null;
	}
	
	
	/**
	 * From lucene demo.
	 * 
	 * Indexes the given file using the given writer, or if a directory is given,
	 * recurses over files and directories found under the given directory.
	 * 
	 * NOTE: This method indexes one document per input file. This is slow. For good
	 * throughput, put multiple documents into your input file(s). An example of
	 * this is in the benchmark module, which can create "line doc" files, one
	 * document per line, using the <a href=
	 * "../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
	 * >WriteLineDocTask</a>.
	 * 
	 * @param writer
	 *            Writer to the index where the given file/dir info will be stored
	 * @param path
	 *            The file to index, or the directory to recurse into to find files
	 *            to index
	 * @throws IOException
	 *             If there is a low-level I/O error
	 */
	static void indexDocs(final IndexWriter writer, Path path) throws IOException {
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					try {
						indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
					} catch (IOException ignore) {
						// Don't index files that can't be read.
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
		}
	}
	

	/** Indexes a single document */
	static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
		// Call writeFields method to fill the fields of each document
		ArrayList<Document> docsList = new ArrayList<Document>();
		writeFields(file, docsList);

		// Add documents to our writer
		for (Document doc : docsList) {
			if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
				// New index, so we just add the document (no old document can be there)
				System.out.println("adding " + file);
				writer.addDocument(doc);
				System.out.println(doc.get(TITLE));
			} else {
				// Existing index, so we use updateDocument instead to replace the old one
				System.out.println("updating " + file);
				writer.updateDocument(new Term("path", file.toString()), doc);
			}
		}
	}
	
	
	/** Writes fields into a single document */
	static void writeFields(Path file, ArrayList<Document> docsList) {
		int count = 0;
		try {
			JSONObject obj = readJson(file.toString());

			if (obj == null) {
				System.out.println("File not found: " + file.toString());
			}
			else {
				// Create new empty document
				Document doc = new Document();
				
				// Add title field
				doc.add(new TextField(TITLE, (String) obj.get(TITLE), Field.Store.YES));
				// Add id field
				doc.add(new StringField(ARTICLE_ID, (String) obj.get(ARTICLE_ID), Field.Store.YES));
				// Add file path field
				doc.add(new TextField(FILE_PATH, file.toString(), Field.Store.YES));
				// Add ingredients
				doc.add(new TextField(INGREDIENTS, processIngredients(obj), Field.Store.YES));
				// Add categories
				doc.add(new TextField(CATEGORIES, getCategories(obj), Field.Store.YES));
				
				processIngredients(obj);
				
				
				// Add document to our list
				docsList.add(doc);
				count++;
			}
		} catch (Exception e) {
			System.out.println(count + " During writeFields caught a " + e.getClass() + "\n with message: "
					+ e.getMessage() + " | " + file);
		}
	}
	
	public static CharArraySet setStopWords(String[] stopWords) {
		Analyzer analyzer = new StandardAnalyzer();
		CharArraySet stopwords = ((StopwordAnalyzerBase) analyzer).getStopwordSet();
		CharArraySet customStopwords = CharArraySet.copy(stopwords);
		
		for (int i = 0; i < stopWords.length; i++) {
			customStopwords.add(stopWords[i].toCharArray());
		}
		
		analyzer.close();
		return customStopwords;
	}
	
	public static String getIngredients(JSONObject json) {
		String ingredients = "";
		JSONArray ingredlist = (JSONArray) json.get("ingredients-thingsneeded");
		int ingredParts = ingredlist.size();
		
		for (int i = 0; i < ingredParts; i++) {
			JSONObject partIngred = (JSONObject) ingredlist.get(i);
			JSONArray partIngredList = (JSONArray) partIngred.get("part-ingredients");
			
			for (int j = 0; j < partIngredList.size(); j++) {
				ingredients += partIngredList.get(j) + "\n";
			}
		}
		
		return ingredients;
	}
	
	public static String processIngredients(JSONObject json) {
		String ingredients = "";
		JSONArray ingredlist = (JSONArray) json.get("ingredients-thingsneeded");
		int ingredParts = ingredlist.size();
		
		for (int i = 0; i < ingredParts; i++) {
			JSONObject partIngred = (JSONObject) ingredlist.get(i);
			JSONArray partIngredList = (JSONArray) partIngred.get("part-ingredients");
			
			for (int j = 0; j < partIngredList.size(); j++) {
				String ingred = (String) partIngredList.get(j);
				ingred = extractIngredientFromString(ingred);
				ingredients += ingred + "\n";
			}
		}
		
		return ingredients;
	}
	
	private static String extractIngredientFromString(String ingredient) {
		String[] ingred = ingredient.split(",");
		// Cut the processing method after the comma
		ingredient = ingred[0];
		// Remove the quantity information in braces
		ingredient = ingredient.replaceAll("\\(.*\\)", "");
		// Remove all numbers
		ingredient = ingredient.replaceAll("[0-9]", "");
		ingredient = ingredient.replaceAll("/", "");
		// Remove the fractions
		ingredient = ingredient.replaceAll("[¼½¾⅐⅑⅒⅓⅔⅕⅖⅗⅘⅙⅚⅛⅜⅝⅞↉]+", "");
		// Remove measurement units
		ingredient = removeMeasurements(ingredient);
		ingredient = ingredient.replaceAll( "(?<!\\S)" + Pattern.quote("to") + "(?!\\S)", "");
		
		ingredient = ingredient.trim();
//		System.out.println(ingredient);
		return ingredient;
	}
	
	private static String removeMeasurements(String ingredient) {
		String[] measurementUnits = {"cups","cup","head","teaspoon","teaspoons","tsp",
				"tablespoons","tbsp","tablespoon","clove","cloves","fluid ounce","fl oz","pint",
				"quart","qt","oz","ounce","gallon","gal","milliliter","ml","cc","liter","litre"};
		for (String word : measurementUnits) {
			String pattern = "(?<!\\S)" + Pattern.quote(word) + "(?!\\S)";
			ingredient = ingredient.replaceAll(pattern, "");
		}
		
		return ingredient;
	}
	
	public static String getCategories(JSONObject json) {
		String categories = "";
		JSONArray categoryArr = (JSONArray) json.get("category");
		int num = categoryArr.size();
		
		for (int i = 0; i < num; i++) {
			String cat = (String) categoryArr.get(i);
			if (!cat.equals("Food and Entertaining")) {
				categories += cat + "\n";
			}
		}
		return categories;
	}
	
	public static void main(String[] args) {
		createIndex();
	}

}
