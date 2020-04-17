package de.webis.recipesearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;
import de.aitools.aq.alexa.AlexaService;
import de.webis.recipesearch.analysis.UserBehaviorAnalyser;
import de.webis.recipesearch.search.*;
import de.webis.recipesearch.util.CustomLoggingFormatter;
import de.webis.recipesearch.util.Similarity;

public class RecipeSearchSpeechlet implements SpeechletV2 {

	// -------------------------------------------------------------------------
	// LOGGER
	// -------------------------------------------------------------------------

	private static Logger LOG = LoggerFactory.getLogger(RecipeSearchSpeechlet.class);

	// -------------------------------------------------------------------------
	// INTENT NAMES
	// -------------------------------------------------------------------------

	private static final String INTENT_Yes	 		   = "AMAZON.YesIntent";
	private static final String INTENT_No	 		   = "AMAZON.NoIntent";
	private static final String INTENT_PreviousStep    = "AMAZON.PreviousIntent";
	private static final String INTENT_NextStep		   = "AMAZON.NextIntent";
	private static final String INTENT_Repeat		   = "AMAZON.RepeatIntent";
	private static final String INTENT_Stop			   = "AMAZON.StopIntent";
	private static final String INTENT_Cancel		   = "AMAZON.CancelIntent";
	private static final String INTENT_Help			   = "AMAZON.HelpIntent";
	private static final String INTENT_FallBack		   = "AMAZON.FallbackIntent";
	
	private static final String INTENT_HowQuery 		   	 	 = "HowQueryIntent";
	private static final String INTENT_NewSearch				 = "NewSearchIntent";
	private static final String INTENT_SearchWithIngred    		 = "SearchWithIngredIntent";
	private static final String INTENT_SearchWithoutIngred 		 = "SearchWithoutIngredIntent";
	private static final String INTENT_SearchSpecifyIngred		 = "SearchSpecifyIngredIntent";
	private static final String INTENT_SearchParam				 = "SearchParamIntent";
	private static final String INTENT_RemoveQueryParam			 = "RemoveQueryParamIntent";
	private static final String INTENT_AlwaysExcludedIngred		 = "AlwaysExcludedIngredIntent";
	private static final String INTENT_RequestCurrentQuery		 = "RequestCurrentQueryIntent";

	private static final String INTENT_MealIdea					 = "MealIdeaIntent";
	private static final String INTENT_Supermarket				 = "SupermarketIntent";
	private static final String INTENT_StayHome					 = "StayHomeIntent";
	private static final String INTENT_IHave					 = "IHaveIntent";
	private static final String INTENT_SignalListComplete		 = "SignalListCompleteIntent";
	private static final String INTENT_Adventurous				 = "AdventurousIntent";

	private static final String INTENT_ReadIngredients 			 = "ReadIngredientsIntent";
	private static final String INTENT_ExplicitReadIngred  		 = "ReadCertainIngredListIntent";
	private static final String INTENT_PrepareIngredients	  	 = "PrepareIngredientsIntent";
	private static final String INTENT_ReadIntroduction	 		 = "ReadIntroductionIntent";
	
	private static final String INTENT_LiveInstruction 			 = "LiveInstructionIntent";
	private static final String INTENT_MethodInstruction   	 	 = "InstructionForMethodIntent";
	private static final String INTENT_MethodSummarize		 	 = "SummarizeMethodIntent";
	
	private static final String INTENT_AskQuantity		 		 = "AskQuantityIntent";
	private static final String INTENT_AskTime			 		 = "AskTimeIntent";
	private static final String INTENT_AskConfirmIngred			 = "AskConfirmIngredIntent";

	private static final String INTENT_MoreDetail			 	 = "MoreDetailIntent";
	private static final String INTENT_LessDetail			 	 = "LessDetailIntent";
	private static final String INTENT_Wait				 		 = "WaitIntent";
	
	private static final String SLOT_Actions 	       = "action";
	private static final String SLOT_Objects 	   	   = "object";
	private static final String SLOT_Ingredient		   = "ingredient";
	private static final String SLOT_IncludeIngred	   = "includeIngred";
	private static final String SLOT_ExcludeIngred	   = "excludeIngred";
	private static final String SLOT_Number 		   = "number";
	private static final String SLOT_TimeUnits		   = "time_unit";
	private static final String SLOT_This			   = "this";
	private static final String SLOT_Category		   = "category";
	private static final String SLOT_No				   = "no";
	
	// -------------------------------------------------------------------------
	// SPEECH ELEMENTS
	// -------------------------------------------------------------------------
	
	private static final String BREAK_HALF_SEC 			 = " <break time=\"0.5s\"/>";
	private static final String BREAK_TEN_SEC 			 = "<break time = \"10s\"/>";
	private static final String BREAK_ONE_MIN 			 = BREAK_TEN_SEC + BREAK_TEN_SEC + BREAK_TEN_SEC + 
															BREAK_TEN_SEC + BREAK_TEN_SEC + BREAK_TEN_SEC;
	private static final String BREAK_FIVE_MIN 			 = BREAK_ONE_MIN + BREAK_ONE_MIN + BREAK_ONE_MIN + 
															BREAK_ONE_MIN + BREAK_ONE_MIN;
	private static final String BREAK_TEN_MIN 			 = BREAK_FIVE_MIN + BREAK_FIVE_MIN;
//	private static final String BREAK_TWENTY_MIN 		 = BREAK_TEN_MIN + BREAK_TEN_MIN;
	
	private static final String BREAK_STRONG 			 = " <break strength=\"strong\"/>";
	
	private static final String INSTRUCT_RECIPEINSTRUCTION  = " You can ask me for "
			+ "<phoneme alphabet=\"ipa\" ph=\"laɪv\"> live </phoneme> instruction if you want to cook this recipe. "
			+ "I can also help you with preparing the ingredients. ";
	private static final String INSTRUCT_NEXT 			    = " If you're finished, just say 'next'";
	private static final String INSTRUCT_INGREDIENTSEARCH   = " By the way, you can also tell me the ingredients that "
			+ "you want or don't want to cook with, if there's any.";
	
	
	// -------------------------------------------------------------------------
	// CONSTANTS
	// -------------------------------------------------------------------------
	private static final String STATE_SUMMARIZE = "summarization";
	private static final String STATE_INGRED = "ingredients";
	private static final String STATE_INGRED_QUANTITY = "ingred quantity";
	private static final String STATE_INSTRUCT = "instruction";
	private static final String STATE_ADVENTUROUS = "adventurous";
	private static final String STATE_NULL = "null";
	
	private static final String COMMON_CATEGORIES_PATH = "resources/common-categories.csv";
	private static final String PROPERTIES_PATH = "resources/userconfig.properties";
	private static final Properties props = loadProperties(PROPERTIES_PATH);
	
	
	private static final java.util.logging.Logger QueryLogger = java.util.logging.Logger.getLogger("QueryLogger");
	

	// -------------------------------------------------------------------------
	// HANDLER METHODS
	// -------------------------------------------------------------------------

	@Override
	public SpeechletResponse onLaunch(final SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
		LOG.debug("Skill launched");

		final SpeechletResponse response = new SpeechletResponse();
		final SsmlOutputSpeech output = new SsmlOutputSpeech();
		response.setOutputSpeech(output);

		output.setSsml("<speak>Hi! How can I help you with cooking?</speak>");

		response.setShouldEndSession(false);
		return response;
	}

	@Override
	public void onSessionStarted(final SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
		LOG.debug("Session started");
		
		System.out.println("Session started");
		final Session session = requestEnvelope.getSession();
		// Initialize abstraction level. Set to lowest
		SessionAttributes.setAbstractionLevel(session, 0);
		// Turn on instruction/tip/help 
		SessionAttributes.setHelpOn(session);
		SessionAttributes.setListHelpOn(session);
		SessionAttributes.setIngredientsReadFalse(session);
		SessionAttributes.setMethodsReadFalse(session);
		SessionAttributes.setCurrentState(session, STATE_NULL);
		// Initialize search history
		final List<String> searchHistory = new ArrayList<>();
		requestEnvelope.getSession().setAttribute("searchHistory", searchHistory);
		final List<String> deniedRecipes = new ArrayList<>();
		requestEnvelope.getSession().setAttribute("deniedRecipes", deniedRecipes);
		
		try {
			FileHandler fileHandler = new FileHandler("%h/logging/query-log.0.0.tsv", 100000000, 1, true);
			CustomLoggingFormatter formatter = new CustomLoggingFormatter();
			fileHandler.setFormatter(formatter);
			QueryLogger.addHandler(fileHandler);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
    public void onSessionEnded(final SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
        LOG.debug("Session ended");
    }

	@Override
	public SpeechletResponse onIntent(final SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
		String intentName = requestEnvelope.getRequest().getIntent().getName();
		LOG.debug("onIntent: " + intentName);
		// Save to log file
        LogRecord rec = new LogRecord(Level.INFO, "onIntent: " + intentName);
        QueryLogger.log(rec);

		final SpeechletResponse response = new SpeechletResponse();
		final SsmlOutputSpeech output = new SsmlOutputSpeech();
		response.setOutputSpeech(output);

		final Intent intent = requestEnvelope.getRequest().getIntent();
		final Session session = requestEnvelope.getSession();
		
		switch (intent.getName()) {
		
		case INTENT_HowQuery:
			output.setSsml("<speak>" + this.onHowToIntent(session, intent.getSlots(), response) + "</speak>");
	        response.setShouldEndSession(false);
	        break;
		case INTENT_Yes:
			output.setSsml("<speak>" + this.onYesIntent(session) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_No:
			output.setSsml("<speak>" + this.onNoIntent(session, intent.getSlots(), response) + "</speak>");
	        response.setShouldEndSession(false);
			break;
		case INTENT_LiveInstruction:
			output.setSsml("<speak>" + this.onLiveInstructionIntent(session) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_MoreDetail:
			output.setSsml("<speak>" + this.onMoreDetailIntent(session) + "</speak>");
	        response.setShouldEndSession(false);
			break;
		case INTENT_LessDetail:
			output.setSsml("<speak>" + this.onLessDetailIntent(session) + "</speak>");
	        response.setShouldEndSession(false);
			break;
		case INTENT_ReadIngredients:
			output.setSsml("<speak>" + this.onReadIngredientsIntent(session) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_ExplicitReadIngred:
			output.setSsml("<speak>" + this.onExplicitIngredientsIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_NextStep:
			output.setSsml("<speak>" + this.onNextIntent(session) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_PreviousStep:
			output.setSsml("<speak>" + this.onPreviousIntent(session) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_Repeat:
			output.setSsml("<speak>" + this.onRepeatIntent(session) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_Wait:
			output.setSsml("<speak>" + this.onWaitIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_ReadIntroduction:
			output.setSsml("<speak>" + this.onReadIntroIntent(session) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_MethodInstruction:
			output.setSsml("<speak>" + this.onInstructionForMethodIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_AskQuantity:
			output.setSsml("<speak>" + this.onAskQuantityIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_AskTime:
			output.setSsml("<speak>" + this.onAskTimeIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_AskConfirmIngred:
			output.setSsml("<speak>" + this.onAskIfIngredExistIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_SearchWithIngred:
			output.setSsml("<speak>" + this.onSearchWithIngredIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_SearchWithoutIngred:
			output.setSsml("<speak>" + this.onSearchWithoutIngredIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_SearchSpecifyIngred:
			output.setSsml("<speak>" + this.onSearchSpecifyIngredIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_SearchParam:
			output.setSsml("<speak>" + this.onSearchParamIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_PrepareIngredients:
			output.setSsml("<speak>" + this.onPrepareIngredIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_MealIdea:
			output.setSsml("<speak>" + "Do you mind going to the supermarket or would you rather "
					+ "use the ingredients on hand?" + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_StayHome:
			output.setSsml("<speak>" + "Okay. So, what do you have right now? "
					+ "You may say 'i have bla' once at a time. I'll confirm each time for you." + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_Supermarket:
			output.setSsml("<speak>" + this.onSupermarketIntent(session) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_IHave:
			output.setSsml("<speak>" + this.onIHaveIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_SignalListComplete:
			output.setSsml("<speak>" + this.onListCompleteIntent(session) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_NewSearch:
			output.setSsml("<speak>" + this.onNewSearchIntent(session) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_MethodSummarize:
			output.setSsml("<speak>" + this.onSummarizeMethodsIntent(session) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_AlwaysExcludedIngred:
			output.setSsml("<speak>" + this.onAlwaysExcludeIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_Adventurous:
			output.setSsml("<speak>" + this.onAdventurousIntent(session) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_RequestCurrentQuery:
			output.setSsml("<speak>" + this.onRequestCurrentQueryIntent(session) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_RemoveQueryParam:
			output.setSsml("<speak>" + this.onRemoveQueryParamIntent(session, intent.getSlots()) + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_Help:
			output.setSsml("<speak>" + this.onHelpIntent() + "</speak>");
			response.setShouldEndSession(false);
			break;
		case INTENT_Cancel:
		case INTENT_Stop:
			output.setSsml("<speak>Close</speak>");
			response.setShouldEndSession(true);
			break;
		default:
			output.setSsml("<speak>Error. Invalid intent. " + this.onHelpIntent() + "</speak>");
			response.setShouldEndSession(false);
		}
		return response;
	}

	
	

	


	// -------------------------------------------------------------------------
	// ACTIONS
	// -------------------------------------------------------------------------

	/**
	 * Action for help
	 * @return
	 */
	private String onHelpIntent() {
		String answer = "I can help you with searching recipes, preparing ingredients and cooking step-by-step. "
				+ "You can directly search for recipes by name by asking, for example: how to bake chocolate cookies? "
				+ "You can also search by ingredients by saying: I want to cook something with bla and bla. "
				+ "To find recipes of a certain cuisine, tell me for instance: I want to cook something indian. "
				+ "I can even recommend you recipes if you need inspiration, just say: I don't know what to cook today.";
		return answer;
	}

	/**
	 * Action for a how to question
	 * @param response 
	 * @return
	 */
	private String onHowToIntent(final Session session, Map<String, Slot> slots, SpeechletResponse response) {
		
		String actionWord = null;
		String objectWord = null;
		String querystring = null;
		JSONObject recipe = new JSONObject();
		SimpleCard card = new SimpleCard();
		String answer = "Sorry, I cannot find any relevant article in the database."
    			+ "You can remove certain existing criterias. For more information, ask for the current query.";;
		card.setContent("No relevant article found.");
		// Clear all previous search parameters
		SessionAttributes.clearIngredParams(session);
		SessionAttributes.setHelpOn(session);
		SessionAttributes.setListHelpOn(session);
		SessionAttributes.setIngredientsReadFalse(session);
		SessionAttributes.setMethodsReadFalse(session);
		
		try {
			actionWord = slots.get(SLOT_Actions).getValue();
			objectWord = slots.get(SLOT_Objects).getValue();
			querystring = actionWord + " " + objectWord;
			querystring = querystring.replaceAll("null", "").trim();
			System.out.println("Query string: " + querystring);
			
			SessionAttributes.setQuery(session, querystring, 1);
			card.setTitle("How to " + querystring);
			
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		String url = SearchFiles.retrieveRecipes(querystring, 1);
		if (url != null) {
			try {
				recipe = (JSONObject) new JSONParser().parse(new FileReader(url.toString()));
				String title = DetailGetter.getTitle(recipe);
				SessionAttributes.setRecipeTitle(session, title);
				
				answer = "I found an article: " + title + ". Is this what you're looking for?";
				
				card.setContent("Article found: " + title);
				response.setCard(card);
				SessionAttributes.setArticleURL(session, url);
				SessionAttributes.addToSearchHistory(session, querystring);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		SessionAttributes.setLastReadText(session, answer);
		
		
		// Save to log file
        LogRecord rec = new LogRecord(Level.INFO, "query: " + querystring);
        QueryLogger.log(rec);
		
		return answer;
	}
	
	/**
	 * Action for denial on the found recipe
	 * @return
	 */
	private String onNoIntent(final Session session, Map<String, Slot> slots, SpeechletResponse response) {
		
		if (SessionAttributes.getCurrentState(session).equals(STATE_ADVENTUROUS)) {
			return onAdventurousIntent(session);
		}
		
		String querystring = SessionAttributes.getQuery(session);
		String includedIngred = SessionAttributes.getIncludeIngred(session);
		String excludedIngred = SessionAttributes.getExcludeIngred(session);
		String searchParam	  = SessionAttributes.getSearchParam(session);
		
		// Add the last url into list of denied recipes (shouldn't appear again in this session)
		String lastUrl = SessionAttributes.getArticleURL(session);
		SessionAttributes.addDeniedRecipe(session, lastUrl);
		
		int lastResIndex = SessionAttributes.getQueryResIndex(session);
		int newResIndex = lastResIndex + 1;
		System.out.println("last index : " + lastResIndex + " new index : " + newResIndex);
		SessionAttributes.setQueryResIndex(session, newResIndex);
		
		JSONObject recipe = new JSONObject();
		SimpleCard card = new SimpleCard();
		String answer = "Sorry, I cannot find any more relevant articles. ";
		card.setContent("No relevant article found.");
		
		String url = null;
		url = SearchFiles.retrieveRecipes(querystring, searchParam, includedIngred, excludedIngred, newResIndex);
		
		// check denied?
		url = checkIfPrevlyDenied(session, url, querystring, searchParam, includedIngred, excludedIngred, newResIndex);
		
		if (url != null) {
			try {
				recipe = (JSONObject) new JSONParser().parse(new FileReader(url.toString()));
				String title = DetailGetter.getTitle(recipe);
				
				answer = "I found another one: " + title + ". Is "
						+ "<say-as interpret-as=\"interjection\"> this </say-as> what you're looking for?";
				
				card.setTitle("How to " + querystring);
				card.setContent("Article found: " + title);
				response.setCard(card);
				SessionAttributes.setArticleURL(session, url);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
		SessionAttributes.setLastReadText(session, answer);
		
		boolean helpOn = SessionAttributes.getHelpStatus(session);
		if (helpOn) {
			answer += INSTRUCT_INGREDIENTSEARCH;
			SessionAttributes.toggleHelp(session);
		}
		
		System.out.println(helpOn);
		return answer;
	}

	/**
	 * Action on request of a certain article.
	 * Read ingredients or an overview of different methods according to the article structure.
	 * @return
	 */
	private String onYesIntent(final Session session) {
		
		String answer = "";
		JSONObject recipe = new JSONObject();
		
		String querystring = SessionAttributes.getQuery(session);
		String url = SessionAttributes.getArticleURL(session);
		try {
			recipe = (JSONObject) new JSONParser().parse(new FileReader(url.toString()));
			JSONArray ingredlist = DetailGetter.getCompleteIngredList(recipe);
			
			// If there're several methods or parts, read the overview of method names
			if (DetailGetter.getStructure(recipe).equals("Parts") && SessionAttributes.getMethodsReadStatus(session) != true) {
				SessionAttributes.setCurrentState(session, STATE_SUMMARIZE);
				answer += "So, to " + querystring + ", we'll have to do the followings: ";
				answer += getPartSummarization(recipe);
				SessionAttributes.setMethodsReadTrue(session);
			}
			else if ((DetailGetter.getStructure(recipe).equals("Methods") ||
					 (DetailGetter.getAllParts(recipe)).size() > 1) && SessionAttributes.getMethodsReadStatus(session) != true) {
				SessionAttributes.setCurrentState(session, STATE_SUMMARIZE);
				answer += "There are more than one method to do it. ";
				answer += getPartSummarization(recipe);
				SessionAttributes.setMethodsReadTrue(session);
			} 
			
			// Read ingredients if there's only one ingredient list
			else if (ingredlist.size() == 1 && SessionAttributes.getIngredientsReadStatus(session) != true) {
				SessionAttributes.setCurrentState(session, STATE_INGRED);
				answer += retrieveOnlyIngredlist(ingredlist);
				SessionAttributes.setIngredientsReadTrue(session);
			} 
			
			// If more than one ingredients/tools list for a single document(without parts), read all ingredients and tools
			else if (SessionAttributes.getIngredientsReadStatus(session) != true) {
				SessionAttributes.setCurrentState(session, STATE_INGRED);
				answer += retrieveCollectedIngredlist(ingredlist);
				SessionAttributes.setIngredientsReadTrue(session);
			}
			
			answer +=  BREAK_STRONG + INSTRUCT_RECIPEINSTRUCTION;
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String category = DetailGetter.getCategory(recipe);
		SessionAttributes.setLastReadText(session, answer);
		SessionAttributes.setRecipeCategory(session, category);
		
		return answer;
	}
	
	private String onLiveInstructionIntent(Session session) {
		
		String answer = "";
		String url = SessionAttributes.getArticleURL(session);
		SessionAttributes.setCurrentState(session, STATE_INSTRUCT);
		
		answer = retrieveFirstStep(session, url);
		SessionAttributes.setLastReadText(session, answer);
		
		// Save to log file
        LogRecord rec = new LogRecord(Level.INFO, "Confirmed recipe: " + SessionAttributes.getRecipeTitle(session) 
        								+ "\nCategory restriction: " + SessionAttributes.getRecipeCategory(session));
        QueryLogger.log(rec);
		
		return answer;
	}

	private String onNextIntent(Session session) {
		String answer = "";
		String url = SessionAttributes.getArticleURL(session);
		
		String state = SessionAttributes.getCurrentState(session);
		System.out.println(state);
		if (state.equals(STATE_INSTRUCT)) {
			answer = retrieveNextStep(session, url);
		}
		else if (state.equals(STATE_INGRED_QUANTITY)) {
			answer = retrieveNextIngredient(session);
		}
		
		SessionAttributes.setLastReadText(session, answer);
		
		return answer;
	}

	private String onPreviousIntent(Session session) {
		String answer = "";
		String url = SessionAttributes.getArticleURL(session);
		
		String state = SessionAttributes.getCurrentState(session);
		if (state.equals(STATE_INSTRUCT)) {
			answer = retrievePreviousStep(session, url);
		}
		else if (state.equals(STATE_INGRED_QUANTITY)) {
			answer = retrievePrevIngredient(session);
		}
		
		SessionAttributes.setLastReadText(session, answer);
		
		return answer;
	}
	
	private String onRepeatIntent(Session session) {
		
		String answer = SessionAttributes.getLastReadText(session);
		return answer;
	}

	private String onMoreDetailIntent(Session session) {
//		System.out.println("Abstraction level from " + getAbstractionLevel(session));
		SessionAttributes.increaseAbstractionLevel(session);
//		System.out.print(" to " + getAbstractionLevel(session) + "\n");
		
		String answer = "";
		JSONObject recipe = new JSONObject();
		String url = SessionAttributes.getArticleURL(session);

		int partIndex = SessionAttributes.getLastPartIndex(session);
		int numParts = SessionAttributes.getNumParts(session);
		int stepIndex = SessionAttributes.getLastStepIndex(session);
		
		answer = retrieveSubStep(session, recipe, url, partIndex, stepIndex, numParts);
		SessionAttributes.setLastReadText(session, answer);
		
		return answer;
	}
	
	private String onLessDetailIntent(Session session) {
		SessionAttributes.decreaseAbstractionLevel(session);
		
		String answer = "<say-as interpret-as=\"interjection\">okey dokey</say-as>. I'll cut it short from now on.";
		SessionAttributes.setLastReadText(session, answer);
		
		return answer;
	}
	
	private String onReadIngredientsIntent(Session session) {
		String answer = "";
		
		try {
			JSONObject recipe = (JSONObject) new JSONParser().parse(new FileReader(SessionAttributes.getArticleURL(session)));
			JSONArray ingredlist = DetailGetter.getCompleteIngredList(recipe);
			JSONArray parts = DetailGetter.getAllParts(recipe);
			int numParts = parts.size();
			
			// If no ingredients exist
			if (ingredlist == null) {
				answer = "There are no ingredients or tools listed for this recipe.";
				SessionAttributes.setIngredientsReadTrue(session);
			}
			
			// Read ingredients directly if there's only one ingredient list
			if (ingredlist.size() == 1) {
				SessionAttributes.setCurrentState(session, STATE_INGRED);
				answer += retrieveOnlyIngredlist(ingredlist);
				SessionAttributes.setIngredientsReadTrue(session);
			} 
			
			else if (numParts > 1) {
				answer = "If you want the ingredients for a certain method, "
						+ "please also tell me which method by saying: ingredients for method bla. ";
			}
			
			// If more than one ingredients/tools list for a single document(without parts), read all ingredients and tools
			else {
				SessionAttributes.setCurrentState(session, STATE_INGRED);
				answer += retrieveCollectedIngredlist(ingredlist);
				SessionAttributes.setIngredientsReadTrue(session);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		SessionAttributes.setLastReadText(session, answer);
		
		return answer;
	}

	private String onExplicitIngredientsIntent(Session session, Map<String, Slot> slots) {
		String answer = "";
//		String GENERALINGREDS = "general ingredients";
//		String THINGSNEEDED = "things needed";
		
		String actionWord = null;
		String objectWord = null;
		String number = null;
		String thisIndicator = null;
		String methodName = null;
		
		try {
			actionWord = slots.get(SLOT_Actions).getValue();
			System.out.println("action word: " + actionWord);
			objectWord = slots.get(SLOT_Objects).getValue();
			System.out.println("object word: " + objectWord);
			thisIndicator = slots.get(SLOT_This).getValue();
			System.out.println("indicator: " + thisIndicator);
			number = slots.get(SLOT_Number).getValue();
			System.out.println("number: " + number);
			
			methodName = (actionWord == null ? "" : actionWord) + " " 
						+ (objectWord == null ? "" : objectWord);
			System.out.println("\nRequested method: " + methodName);
			
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		String url = SessionAttributes.getArticleURL(session);
//		if (ingredListStyle == null) {
//			answer = "Sorry, I'm not very sure. "
//					+ "I can understand you better if you say: 'ingredients for method bla'. ";
//		}
//		else if (ingredListStyle.equals("complete")) {
//			answer = retrieveTargetIngreds(url, GENERALINGREDS, -1);
//			answer += retrieveTargetIngreds(url, THINGSNEEDED, -1);
//		}
		if (thisIndicator != null) {
			String partname = SessionAttributes.getLastPartName(session);
			answer = retrieveTargetIngreds(url, partname, -1);
			if (!answer.isEmpty() && !answer.equals(" ")) {
				answer = "We need: " + answer;
			}
		}
		else if (methodName.equals(" ") && number == null) {
			answer = "Sorry, I didn't get that. "
					+ "I can understand you better if you say: 'ingredients for method bla'. ";
		}
		else if (!methodName.equals(" ")) {
			answer = retrieveTargetIngreds(url, methodName, -1);
			if ((answer.isEmpty() || answer.equals(" ")) && number!=null) {
				int index = Integer.parseInt(number);
				answer = retrieveTargetIngreds(url, null, index);
				if (!answer.isEmpty() && !answer.equals(" ")) {
					answer = "For method " + index + ", we need: " + answer;
				}
			}
		}
		else if (number != null) {
			System.out.println("\nParsing method number" + number + "\n");
			int index = Integer.parseInt(number);
			answer = retrieveTargetIngreds(url, null, index);
			if (!answer.isEmpty() && !answer.equals(" ")) {
				answer = "For method " + index + ", we need: " + answer;
			}
		}
		
		if (answer.isEmpty() || answer.equals(" ")) {
			answer = "Sorry, I can't find that. "
	    			+ "You can remove certain existing criterias. For more information, ask for the current query.";;
		}
		SessionAttributes.setLastReadText(session, answer);
		SessionAttributes.setIngredientsReadTrue(session);
		
		return answer;
	}

	private String onReadIntroIntent(Session session) {
		String answer = "Sorry, there's no introduction for this recipe.";
		
		try {
			JSONObject recipe = (JSONObject) new JSONParser().parse(new FileReader(SessionAttributes.getArticleURL(session)));
			answer = (String) recipe.get("intro");
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		
		SessionAttributes.setLastReadText(session, answer);
		return answer;
	}

	private String onWaitIntent(Session session, Map<String, Slot> slots) {
		String answer = "";
		
		String number = null;
		String timeunit = null;
		
		try {
			number = slots.get(SLOT_Number).getValue();
			timeunit= slots.get(SLOT_TimeUnits).getValue();
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		System.out.println(number + timeunit);
		
		if (timeunit.equals("minute") || timeunit.equals("minutes")) {
			int num = Integer.parseInt(number);
			for (int i = 0; i < num; i++) {
				answer += BREAK_ONE_MIN;
			}
			answer += "Ding ding ding. Time is up! If you want me to continue the instruction, say 'next'.";
		}
		
		return answer;
	}

	private String onInstructionForMethodIntent(Session session, Map<String, Slot> slots) {
		String answer = "";
		SessionAttributes.setCurrentState(session, STATE_INSTRUCT);
		
		String number = null;
		String actionWord = null;
		String objectWord = null;
		String methodName = null;
		
		try {
			actionWord = slots.get(SLOT_Actions).getValue();
			System.out.println("action word: " + actionWord);
			objectWord = slots.get(SLOT_Objects).getValue();
			System.out.println("object word: " + objectWord);
			number = slots.get(SLOT_Number).getValue();
			System.out.println("number: " + number);
			
			methodName = (actionWord == null ? "" : actionWord) + " " 
						+ (objectWord == null ? "" : objectWord);
			System.out.println("\nRequested method: " + methodName);
			
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		JSONObject recipe = new JSONObject();
		String url = SessionAttributes.getArticleURL(session);
		
		// Prioritize method name rather than number
		if (methodName.equals(" ") && number == null) {
			answer = "Sorry, I didn't get that. "
					+ "I can understand you better if you say: 'instruction for method bla'. ";
		}
		else if (!methodName.equals(" ")) {
			answer = retrieveTargetMethod(session, recipe, url, methodName, -1);
			if ((answer.isEmpty() || answer.equals(" ")) && number!=null) {
				int index = Integer.parseInt(number);
				answer = retrieveTargetMethod(session, recipe, url, null, index);
			}
		}
		else if (number != null) {
			System.out.println("\nParsing method number" + number + "\n");
			int index = Integer.parseInt(number);
			answer = retrieveTargetMethod(session, recipe, url, null, index);
		}
		
		if (answer.isEmpty() || answer.equals(" ")) {
			answer = "Sorry, I can't find that. Maybe you can try to add the number of the method?";
		}
		SessionAttributes.setLastReadText(session, answer);
		
		// Save to log file
        LogRecord rec = new LogRecord(Level.INFO, "Confirmed recipe: " + SessionAttributes.getRecipeTitle(session));
        QueryLogger.log(rec);
        

		answer = answer.replaceAll("&", "and");
		
		return answer;
	}
	
	/**
	 * Answers questions on quantity of eg. ingredients. 
	 * Uses allennlp pre-trained model for machine comprehension.
	 */
	private String onAskQuantityIntent(Session session, Map<String, Slot> slots) {
		String answer = "";
		
		String objectWord = null;
		try {
			objectWord = slots.get(SLOT_Objects).getValue();
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		// Check which part we're at, get part ingreds list
		String url = SessionAttributes.getArticleURL(session);
		String partname = SessionAttributes.getLastPartName(session);
		String[] ingreds = retrieveTargetIngreds(url, partname, -1).toLowerCase().split(",");
		String target = "";
		for (String string : ingreds) {
			int tIndex = string.indexOf(objectWord);
			if (tIndex > -1) {
				target = string;
			}
		}
		answer = target.trim();
		
		// TODO: If not found in part method, check general ingreds and tools?????????
		
		// If not found in ingredients, use machine comprehension on the last-read paragraph
		if (answer.isEmpty() || answer.equals(" ")) {
			String question = "How many " + objectWord + "?";
			System.out.println(question);
			
			try {
				answer = NLPAnswerRetriever.getAnswer(SessionAttributes.getLastReadText(session), question);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		if (answer.isEmpty() || answer.equals(" ")) {
			answer = "Hm.. I can't answer that. Perhaps you could try to ask me again?";
		}
		
		return answer;
	}
	
	/**
	 * Answers questions on how long a procedure takes. 
	 * Uses allennlp pre-trained model for machine comprehension.
	 */
	private String onAskTimeIntent(Session session, Map<String, Slot> slots) {
		String answer = "";
		
		String objectWord = null;
		String actionWord = null;
		try {
			objectWord = slots.get(SLOT_Objects).getValue();
			actionWord = slots.get(SLOT_Actions).getValue();
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		String question = "How long do I ";
		if (actionWord == null && objectWord != null) {
			question += "cook the " + objectWord + "?";
		} else if (actionWord != null) {
			question += actionWord + " the " + objectWord + "?";
		}
		
		System.out.println(actionWord);
		System.out.println(objectWord);
		System.out.println(question);
		
		try {
			answer = NLPAnswerRetriever.getAnswer(SessionAttributes.getLastReadText(session), question);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (answer.isEmpty() || answer.equals(" ")) {
			answer = "Hm.. I can't answer that. Perhaps you could try to ask me again?";
		}
		
		return answer;
	}
	
	/**
	 * Checks if a certain ingredient is in the recipe
	 */
	private String onAskIfIngredExistIntent(Session session, Map<String, Slot> slots) {
		String answer = "";
		
		String objectWord = null;
		try {
			objectWord = slots.get(SLOT_Objects).getValue();
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		// Get all ingredients and check if the asked object has a match
		String url = SessionAttributes.getArticleURL(session);
		try {
			JSONObject recipe = (JSONObject) new JSONParser().parse(new FileReader(url.toString()));
			String ingreds = DetailGetter.getCompleteIngredList(recipe).toString();
			int tIndex = ingreds.indexOf(objectWord);		// TODO: match multiple objects
			if (tIndex > -1) {
				answer = "Yes. " + objectWord + " is in the recipe. So... You still want it?";
			} else {
				answer = "No, it's not in the recipe. So... You still want it?";
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		
		
		return answer ;
	}
	
	/**
	 * Return search results if user specifies what ingredients to include.
	 */
	private String onSearchWithIngredIntent(Session session, Map<String, Slot> slots) {
		String includeIngredient = SessionAttributes.getIncludeIngred(session);
		String excludeIngredient = SessionAttributes.getExcludeIngred(session);
		boolean denial = false;
		try {
			String slotword = slots.get(SLOT_Ingredient).getValue();
			String no = slots.get(SLOT_No).getValue(); 
			
			if (slotword != null) {
				includeIngredient = appendToIngredAttList(includeIngredient, slotword);
			}
			SessionAttributes.setIncludeIngred(session, includeIngredient);
			if (no != null) {
				denial = true;
			}
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		String answer = searchWithSpecifiedIngreds(session, includeIngredient, excludeIngredient, denial);
		SessionAttributes.setHelpOff(session);
		
		SessionAttributes.setLastReadText(session, answer);
		
		
		// Save to log file
        LogRecord rec = new LogRecord(Level.INFO, "Include ingredients: " + includeIngredient);
        QueryLogger.log(rec);
		
		return answer;
	}
	
	/**
	 * Return search results if user specifies what ingredient to exclude.
	 */
	private String onSearchWithoutIngredIntent(Session session, Map<String, Slot> slots) {
		String excludeIngredient = SessionAttributes.getExcludeIngred(session);
		String includeIngredient = SessionAttributes.getIncludeIngred(session);
		boolean denial = false;
		try {
			String slotword = slots.get(SLOT_Ingredient).getValue();
			String no = slots.get(SLOT_No).getValue();
			
			if (slotword != null) {
				excludeIngredient = appendToIngredAttList(excludeIngredient, slotword);
			}
			SessionAttributes.setExcludeIngred(session, excludeIngredient);
			if (no != null) {
				denial = true;
			}
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		String answer = searchWithSpecifiedIngreds(session, includeIngredient, excludeIngredient, denial);
		SessionAttributes.setHelpOff(session);
		
		SessionAttributes.setLastReadText(session, answer);
		
		
		// Save to log file
        LogRecord rec = new LogRecord(Level.INFO, "Exclude ingredients: " + excludeIngredient);
        QueryLogger.log(rec);
		
		
		return answer;
	}
	
	/**
	 * Return search results if user specifies what ingredient to include AND exclude in one intent
	 */
	private String onSearchSpecifyIngredIntent(Session session, Map<String, Slot> slots) {
		String includeIngredient = SessionAttributes.getIncludeIngred(session);
		String excludeIngredient = SessionAttributes.getExcludeIngred(session);
		boolean denial = false;
		try {
			String slotInclude = slots.get(SLOT_IncludeIngred).getValue();
			String slotExclude = slots.get(SLOT_ExcludeIngred).getValue();
			String no = slots.get(SLOT_No).getValue();
			
			includeIngredient = appendToIngredAttList(includeIngredient, slotInclude);
			excludeIngredient = appendToIngredAttList(excludeIngredient, slotExclude);
			
			SessionAttributes.setIncludeIngred(session, includeIngredient);
			SessionAttributes.setExcludeIngred(session, excludeIngredient);
			
			if (no != null) {
				denial = true;
			}
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		String answer = searchWithSpecifiedIngreds(session, includeIngredient, excludeIngredient, denial);
		SessionAttributes.setHelpOff(session);
		SessionAttributes.setLastReadText(session, answer);
		
		
		// Save to log file
        LogRecord rec = new LogRecord(Level.INFO, "Include ingredients: " + includeIngredient 
        		+ "\nexclude \t" + excludeIngredient);
        QueryLogger.log(rec);
		
		
		return answer;
	}

	/**
	 * Return search results for specific category/restrictions/parameters
	 */
	private String onSearchParamIntent(Session session, Map<String, Slot> slots) {
		String searchParam = null;
		boolean denial = false;
		try {
			searchParam = slots.get(SLOT_Category).getValue();
			String no = slots.get(SLOT_No).getValue();
			SessionAttributes.setSearchParam(session, searchParam);
			if (no != null) {
				denial = true;
			}
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		String answer = searchWithParams(session, searchParam, denial);
//		SessionAttributes.setHelpOff(session);
		
		SessionAttributes.setLastReadText(session, answer);
		
		// Save to log file
        LogRecord rec = new LogRecord(Level.INFO, "Category restriction: " + searchParam);
        QueryLogger.log(rec);
		
		
		return answer;
	}
	
	/**
	 * Read ingredients with their quantities one by one (only the first one in this case, the rest
	 * will be recognized with NextIntent). Allow the user to prepare the ingredients.
	 */
	private String onPrepareIngredIntent(Session session, Map<String, Slot> slots) {
		String answer = "Sorry, seems like there are no ingredients listed for this method.";
		String url = SessionAttributes.getArticleURL(session);
		String partname = SessionAttributes.getLastPartName(session);
		SessionAttributes.setCurrentState(session, STATE_INGRED_QUANTITY);
		
		try {
			JSONObject recipe = (JSONObject) new JSONParser().parse(new FileReader(url));
			
			JSONArray ingredlist = DetailGetter.getCompleteIngredList(recipe);
			
			// If no ingred list exists, return immediately
			if (ingredlist == null) {
				System.out.println("This ingredlist is empty");
				return answer;
			}
			int numIngredParts = ingredlist.size();
			if (numIngredParts == 0) {
				System.out.println("This ingredlist is empty");
				return answer;
			}
			if (partname == null) {
				return "Okay, but you'll have to tell me which method you'd like to prepare ingredients for.";
			}
			partname = partname.replaceAll("Method [0-9] ", "").toLowerCase().trim();	// Remove "method x" from part name
			
			// Start matching
			JSONObject matchIngredPartObj = findMatchingIngredList(numIngredParts, ingredlist, partname);
			
			// Retrieve ingredients
			if (matchIngredPartObj != null) {
				JSONArray ingreds = DetailGetter.getIngredients(matchIngredPartObj);
				
				String ingredsString = concateIngredsWithQuantity(ingreds);
				int index = -1;
				SessionAttributes.setCurrentIngredList(session, ingredsString);
				SessionAttributes.setCurrentIngredIndex(session, index);
				
				answer = "First, " + retrieveNextIngredient(session) + BREAK_STRONG 
						+ ". If you're finished with preparing this, say next.";
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		
		SessionAttributes.setLastReadText(session, answer);
		return answer;
	}
	
	private String onSupermarketIntent(Session session) {
		String answer = "";
		String intro = "You often search for ";
		
		// Generate user behavior analysis
		Map<String, String> map = null;
		try {
			 map = UserBehaviorAnalyser.analyse();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (map != null) {
			for (Entry<String, String> entry : map.entrySet()) {
				String type = entry.getKey();
				String item = entry.getValue();
				if (type.equals("query")) {
					answer += " and " + item + " recipes, ";
				} else if (type.equals("include")) {
					answer += " and recipes that include " + item + ",";
				} else if (type.equals("category")) {
					answer += " and " + item + " recipes, ";
				}
			}
			if (!answer.isEmpty()) {
				answer = intro + answer.substring(4)
						+ " Which would you like: one of these or are you feeling adventurous today?";
			} else {
				answer = "Do you have any preference maybe? Something chinese? Indian? Meat? Salad?";
			}
		}
		
		return answer;
	}

	/**
	 * User tells alexa the items/ingredients he has on hand one by one. This method stores the ingredients
	 * and prepare it for search when the user finishes reading the list of items.
	 */
	private String onIHaveIntent(Session session, Map<String, Slot> slots) {
		String answer = "Yeah? ";
		
		String includeIngredient = SessionAttributes.getIncludeIngred(session);
		try {
			String newIngred = slots.get(SLOT_Objects).getValue();
			if (includeIngredient == null) {
				includeIngredient = newIngred;
			} else {
				includeIngredient += " " + newIngred;
			}
			
			SessionAttributes.setIncludeIngred(session, includeIngredient);
			
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		if (SessionAttributes.getListHelpStatus(session)) {
			answer += BREAK_STRONG + "Oh yeah, after I confirm the final item, "
					+ "you can signal me by saying: 'That's it', "
					+ "so that I can start searching recipes for you.";
			SessionAttributes.setListHelpOff(session);
		}
		
		return answer;
	}
	
	/**
	 * Return the search result for the ingredients the user has on hand after he finishes telling the list.
	 */
	private String onListCompleteIntent(Session session) {
		String includeIngredient = SessionAttributes.getIncludeIngred(session);
		String answer = searchWithSpecifiedIngreds(session, includeIngredient, null, false);
		
		// Save to log file
        LogRecord rec = new LogRecord(Level.INFO, "Include ingredients: " + includeIngredient);
        QueryLogger.log(rec);
		
        SessionAttributes.setLastReadText(session, answer);
		return answer;
	}
	
	/**
	 * Clear all stored query attributes to prepare for a new search
	 */
	private String onNewSearchIntent(Session session) {
		SessionAttributes.clearIngredParams(session);
		SessionAttributes.clearSearchParam(session);
		SessionAttributes.setHelpOn(session);
		SessionAttributes.setListHelpOn(session);
		SessionAttributes.setIngredientsReadFalse(session);
		SessionAttributes.setMethodsReadFalse(session);
		return "Yes?";
	}
	
	private String onSummarizeMethodsIntent(Session session) {
		String answer = "";
		
		String url = SessionAttributes.getArticleURL(session);
		try {
			JSONObject recipe = (JSONObject) new JSONParser().parse(new FileReader(url.toString()));
			SessionAttributes.setCurrentState(session, STATE_SUMMARIZE);
			SessionAttributes.setMethodsReadTrue(session);
			
			answer += getPartSummarization(recipe);
			
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		
		return answer;
	}
	
	/**
	 * In the case where the user have certain things that they don't eat, this intent provides the possibility
	 * to always exclude that ingredient/food instead of saying it every time.
	 */
	private String onAlwaysExcludeIntent(Session session, Map<String, Slot> slots) {
		String answer = "";
		
		String alwaysExclude = props.getProperty("always_excluded_ingredient");
		try {
			String newExclude = slots.get(SLOT_Ingredient).getValue();
			if (alwaysExclude == null) {
				alwaysExclude = newExclude;
			} else {
				alwaysExclude += " " + newExclude;
			}
			answer = "Okay, " + newExclude + " will now always be excluded from search result.";
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		// Set Properties
		props.setProperty("always_excluded_ingredient", alwaysExclude);
		// Store properties into property file
		try {
			props.store(new FileOutputStream(PROPERTIES_PATH), null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		SessionAttributes.setAlwaysExclude(session, alwaysExclude);
		
		return answer;
	}
	
	/**
	 * Suggests random category that the user didn't usually cook.
	 */
	private String onAdventurousIntent(Session session) {
		String answer = "";
		String onstate = SessionAttributes.getCurrentState(session);
		
		// Randomly choose one category that's never cooked
		StringBuilder availableCats = new StringBuilder();
		Map<String, Integer> calledCatMap =  UserBehaviorAnalyser.allAccessedCats();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(COMMON_CATEGORIES_PATH)));
			String line;
			
			while ((line = br.readLine()) != null) {
				// If a pre-set category correspondes with a called category, deem it unavailable
				// Otherwise, store it into available categories
				for (Entry<String, Integer> entry : calledCatMap.entrySet()) {
					String key = entry.getKey();
					double diceCoeff = Similarity.diceCoefficientOptimized(line, key);
					if (diceCoeff > 0.5) {
						break;
					}
				}
				availableCats.append(line + ",");
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String availableCatsString = availableCats.toString();
		availableCatsString = availableCatsString.substring(0, availableCatsString.length()-2);
		String[] availableCatsPool = availableCatsString.split(",");
		int randomNum = ThreadLocalRandom.current().nextInt(availableCatsPool.length);
		int randomNum2 = ThreadLocalRandom.current().nextInt(availableCatsPool.length);
		
		answer += "How do you think of " + availableCatsPool[randomNum] 
				+ " recipes or " + availableCatsPool[randomNum2] + " recipes?";
		
		if (!onstate.equals(STATE_ADVENTUROUS)) {
			answer = "Okay, let's see. " + answer + " Say, I want bla, so I can understand you better.";
		}
		SessionAttributes.setCurrentState(session, STATE_ADVENTUROUS);
		
		return answer;
	}

	private String onRequestCurrentQueryIntent(Session session) {
		String answer = "We are currently searching for ";
		
		String query = SessionAttributes.getQuery(session);
		String param = SessionAttributes.getSearchParam(session);
		String inclu = SessionAttributes.getIncludeIngred(session);
		String exclu = SessionAttributes.getExcludeIngred(session);
		
		if (query != null && !query.isEmpty()) {
			String[] querysplit = query.split(" ");
			if (querysplit[0].equals("null")) {
				query = "cook" + querysplit[1];
			}
			answer += ": How to " + query;
		} else {
			answer += "recipes ";
		}
		if (param != null && !param.isEmpty()) {
			answer += ", in category: " + param;
		}
		if (inclu != null && !inclu.isEmpty()) {
			answer += ", that have " + inclu + " in the ingredients ";
		}
		if (exclu != null && !exclu.isEmpty()) {
			answer += ", that don't contain " + exclu;
		}
		
		answer += ". You can change the parameters by saying: 'Remove bla'.";
		
		SessionAttributes.setLastReadText(session, answer);
		
		return answer;
	}
	
	private String onRemoveQueryParamIntent(Session session, Map<String, Slot> slots) {
		String ingredToRemove = null;
		try {
			ingredToRemove = slots.get(SLOT_Ingredient).getValue();
			System.out.println(ingredToRemove);
		} catch (NullPointerException e) {
			System.out.println("No slot word found!");
		}
		
		String params = SessionAttributes.getSearchParam(session);
		String includ = SessionAttributes.getIncludeIngred(session);
		String exclud = SessionAttributes.getExcludeIngred(session);
		String[] toRemove = ingredToRemove.split(" ");

		// Remove the ingredient(s) from query
		if (params != null) {
			for (String string : toRemove) {
				params = params.replaceAll(string, "");
			}
			SessionAttributes.setSearchParam(session, params);
		}
		if (includ != null) {
			for (String string : toRemove) {
				includ = includ.replaceAll(string, "");
			}
			SessionAttributes.setIncludeIngred(session, includ);
		}
		if (exclud != null) {
			for (String string : toRemove) {
				exclud = exclud.replaceAll(string, "");
			}
			SessionAttributes.setExcludeIngred(session, exclud);
		}
		
		String answer = "Okay. " + ingredToRemove + " removed."; 
		
		return answer;
	}
	
	
	
	
	
	// -------------------------------------------------------------------------
	// METHODS
	// -------------------------------------------------------------------------
	
	private String checkIfPrevlyDenied(Session session, String url, String querystring, String searchParam, 
			String includedIngred, String excludedIngred, int resIndex) {
		List<String> deniedList = SessionAttributes.getDeniedRecipes(session);
		for (String denied : deniedList) {
			if (url.equals(denied)) {
				System.out.println("denied before!");
				// Retrieve next result
				url = SearchFiles.retrieveRecipes(querystring, searchParam, 
						includedIngred, excludedIngred, resIndex + 1);
				// Update result index in session attributes
				SessionAttributes.setQueryResIndex(session, resIndex + 1);
				// Check if prevly denied again
				url = checkIfPrevlyDenied(session, url, querystring, searchParam, 
						includedIngred, excludedIngred, resIndex + 1);
			}
		}
		return url;
	}

	private String retrieveNextIngredient(Session session) {
		String ingredient = null;
		
		String ingredsString = SessionAttributes.getCurrentIngredList(session);
		int index = SessionAttributes.getCurrentIngredIndex(session) + 1;
		SessionAttributes.setCurrentIngredIndex(session, index);
		
		String[] ingredients = ingredsString.split("\n");
		int max = ingredients.length;
		if (index < max) {			
			ingredient = ingredients[index];
		} 
		if (index == max - 1) {
			ingredient += ". This is the last ingredient.";
			SessionAttributes.setCurrentState(session, STATE_INSTRUCT);
			int stepindex = SessionAttributes.getLastStepIndex(session);
			int numSteps = SessionAttributes.getNumSteps(session);
			SessionAttributes.setLastStepInfo(session, stepindex-1, numSteps);
		}
		return ingredient;
	}
	
	private String retrievePrevIngredient(Session session) {
		String ingredsString = SessionAttributes.getCurrentIngredList(session);
		int index = SessionAttributes.getCurrentIngredIndex(session) - 1;
		SessionAttributes.setCurrentIngredIndex(session, index);
		
		String[] ingredients = ingredsString.split("\n");
		String ingredient = ingredients[index];
		
		return ingredient;
	}
	
	private String getPartSummarization(JSONObject recipe) {
		String answer = "";
		
		JSONArray parts = DetailGetter.getAllParts(recipe);
		for (int i = 0; i < parts.size()-1; i++) {
			JSONObject partObj = DetailGetter.getPart(parts, i);
			String partname = DetailGetter.getPartName(partObj);
			answer += partname + ", ";
		}
		answer = answer.substring(0, answer.length()-2);
		answer += ", and " + DetailGetter.getPartName(DetailGetter.getPart(parts, parts.size()-1)) + ". ";
		answer = answer.replaceAll("&", " and ");
		
		return answer;
	}
	
	private String retrieveFirstStep(final Session session, String url) {
		
		String answer = "";
		JSONObject recipe = new JSONObject();
		
		try {
			recipe = (JSONObject) new JSONParser().parse(new FileReader(url));
			JSONArray parts = DetailGetter.getAllParts(recipe);
			int numParts = parts.size();
			
			JSONObject part = DetailGetter.getPart(parts, 0);
			String partname = DetailGetter.getPartName(part);
			JSONArray steps = DetailGetter.getPartSteps(part);
			int numSteps = steps.size();

			// Set step and part info
			SessionAttributes.setLastPartInfo(session, 0, numParts, partname);
			SessionAttributes.setLastStepInfo(session, 0, numSteps);
			
			JSONObject stepObj = DetailGetter.getStepObj(steps, 0);
			String step 	   = DetailGetter.getStepText(stepObj);

			
			if (numParts == 1) {
				answer += step + BREAK_STRONG;
				if (SessionAttributes.getAbstractionLevel(session) > 0) {
					answer += retrieveSubStep(stepObj);
				}
				answer += BREAK_HALF_SEC + INSTRUCT_NEXT;
			} 
			
			else if (numParts > 1) {
				answer += partname + ". " 
						+ retrieveMethodIngreds(partname, numParts, 0, recipe, url)
						+ BREAK_HALF_SEC + "Here's what we're gonna do: " + step + BREAK_STRONG;
				
				if (SessionAttributes.getAbstractionLevel(session) > 0) {
					answer += retrieveSubStep(stepObj);
				}
				// Retrieve tips
				answer += retrieveTips(stepObj);
				answer += BREAK_HALF_SEC + INSTRUCT_NEXT;
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		
		return answer;
	}
	
	private String retrieveNextStep(final Session session, String url) {
		
		String answer = "";
		JSONObject recipe = new JSONObject();

		int partIndex = SessionAttributes.getLastPartIndex(session);
		int numParts  = SessionAttributes.getNumParts(session);
		int stepIndex = SessionAttributes.getLastStepIndex(session) + 1;
		int numSteps  = SessionAttributes.getNumSteps(session);
		
		if (stepIndex < numSteps) {
			answer = retrieveStep(session, recipe, url, partIndex, stepIndex, numParts);
		} 
		else {
			stepIndex = 0;
			partIndex += 1;
			
			if (partIndex == numParts) {
				answer = "There are no further steps now. We're finished here. "
						+ "You could still repeat the last step if you want to.";
			} else {
				answer = retrieveStep(session, recipe, url, partIndex, stepIndex, numParts);
				String partname = SessionAttributes.getLastPartName(session);
				answer = partname + ". " 
						+ retrieveMethodIngreds(partname, numParts, stepIndex, recipe, url)
						+ BREAK_HALF_SEC + answer;
			}
		}
		
		return answer;
	}
	
	private String retrievePreviousStep(final Session session, String url) {
		
		String answer = "";
		JSONObject recipe = new JSONObject();
		
		int partIndex = SessionAttributes.getLastPartIndex(session);
		int numParts  = SessionAttributes.getNumParts(session);
		int stepIndex = SessionAttributes.getLastStepIndex(session) - 1;
		
		if (stepIndex == 0) {
			answer = retrieveStep(session, recipe, url, partIndex, stepIndex, numParts);
			answer += BREAK_STRONG + "This is the first step of " + SessionAttributes.getLastPartName(session);
		}
		else if (stepIndex > 0) {
			answer = retrieveStep(session, recipe, url, partIndex, stepIndex, numParts);
		}
		else {
			partIndex -= 1;
			stepIndex = Integer.MAX_VALUE;
			
			if (partIndex < 0) {
				answer = "There are no previous steps now. That was the very first step of the article. "
						+ "You could still repeat if you want to.";
			} else {
				answer = retrieveStep(session, recipe, url, partIndex, stepIndex, numParts);
				answer = "I'm reading the final step of " + SessionAttributes.getLastPartName(session) + ". " 
						+ BREAK_HALF_SEC + answer;
			}
		}
		return answer;
	}
	
	private String retrieveStep(final Session session, JSONObject recipe, 
			String url, int partIndex, int stepIndex, int numParts) {
		
		String answer = "";
		try {
			recipe = (JSONObject) new JSONParser().parse(new FileReader(url));
			JSONArray parts = DetailGetter.getAllParts(recipe);
			
			JSONObject part = DetailGetter.getPart(parts, partIndex > numParts ? numParts-1 : partIndex);	// Make sure no exceed
			String partName = DetailGetter.getPartName(part);
			JSONArray steps = DetailGetter.getPartSteps(part);
			
			// Update step and part info
			int numSteps = steps.size();
			if (stepIndex > numSteps) {
				stepIndex = numSteps - 1;
			}
			SessionAttributes.setLastStepInfo(session, stepIndex, numSteps);
			System.out.println("Step " + numSteps + " of " + SessionAttributes.getNumSteps(session));
			SessionAttributes.setLastPartInfo(session, partIndex, numParts, partName);
			
			JSONObject stepObj = DetailGetter.getStepObj(steps, stepIndex);
			String step 	   = DetailGetter.getStepText(stepObj);
			
			answer += step + BREAK_STRONG;
			
			
			// Retrieve substeps if details have been requested
			if (SessionAttributes.getAbstractionLevel(session) > 0) {
				answer += retrieveSubStep(stepObj) + BREAK_HALF_SEC;
			}
			
			// Retrieve tips
			answer += retrieveTips(stepObj);
			
			
			if (stepIndex == numSteps - 1 && (numParts == 1 || partIndex == numParts - 1)) {
				answer += " This is the final step of this article.";
			}
			else if (stepIndex == numSteps - 1 && numParts > 1) {
				answer += " This is the last step of this part.";
			}
			
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		return answer;
	}
	
	private String retrieveSubStep(final Session session, JSONObject recipe, 
			String url, int partIndex, int stepIndex, int numParts) {
		
		String answer = "";
		try {
			recipe = (JSONObject) new JSONParser().parse(new FileReader(url));
			JSONArray parts = DetailGetter.getAllParts(recipe);
			
			JSONObject part = DetailGetter.getPart(parts, partIndex);
			String partName = DetailGetter.getPartName(part);
			JSONArray steps = DetailGetter.getPartSteps(part);
			
			// Update step and part info
			int numSteps = steps.size();
			if (stepIndex > numSteps) {
				stepIndex = numSteps - 1;
			}
			SessionAttributes.setLastStepInfo(session, stepIndex, numSteps);
			System.out.println("Step " + numSteps + " of " + SessionAttributes.getNumSteps(session));
			SessionAttributes.setLastPartInfo(session, partIndex, numParts, partName);
			
			JSONObject stepObj = DetailGetter.getStepObj(steps, stepIndex);
			
			// Retrieve substeps if exist
			answer = retrieveSubStep(stepObj);
			if (answer.isEmpty() || answer.equals(" ")) {
				answer = "There are no more details for this step.";
			}
			
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		return answer;
	}
	
    private String retrieveSubStep(JSONObject stepObj) {
    	String detail = "";
    	
    	JSONArray substeps = DetailGetter.getSubsteps(stepObj);
    	int numSubsteps = substeps.size();
    	
    	if (numSubsteps > 0) {
			for (int i = 0; i < numSubsteps; i++) {
				JSONObject substep = DetailGetter.getSubstepObj(substeps, i);
				String substepText = DetailGetter.getSubstepText(substep);
				detail += substepText + " " + BREAK_STRONG;
			}
		}
    	
		return detail;
	}
    
    /**
     * @param ingreds The ingredient array to merge
     * @param offset The number of ingredients at the end to be excluded in merging
     */
    private String concateIngreds(JSONArray ingreds, int offset) {
    	String string = "";
    	for (int i = 0; i < ingreds.size() + offset; i++) {
			String object = ingreds.get(i).toString();
			object = extractIngredientFromString(object);
			string += object + ", ";
		}
    	return string;
	}
    
    private String concateIngredsWithQuantity(JSONArray ingreds) {
    	String string = "";
    	for (int i = 0; i < ingreds.size(); i++) {
			String object = ingreds.get(i).toString();
			string += object + "\n";
		}
    	return string;
	}
    
    private String extractIngredientFromString(String ingredient) {
		// Remove the quantity information in braces
		ingredient = ingredient.replaceAll("\\(.*\\)", "");
		System.out.println(ingredient);
		// Cut the processing method after the comma
		String[] ingred = ingredient.toLowerCase().split(",");
		ingredient = ingred[0];
		// Remove all numbers
		ingredient = ingredient.replaceAll("[0-9]", "");
		ingredient = ingredient.replaceAll("/", "");
		// Remove the fractions
		ingredient = ingredient.replaceAll("[¼½¾⅐⅑⅒⅓⅔⅕⅖⅗⅘⅙⅚⅛⅜⅝⅞↉]+", "");
		// Remove things after an "or"
		ingred = ingredient.split("(?<!\\S)" + Pattern.quote("or") + "(?!\\S)");
		ingredient = ingred[0];
		// Remove things before a dot (some abbreviation of units)
		ingred = ingredient.split("\\.");
		ingredient = ingred[ingred.length-1];
		// Remove measurement units
		ingredient = removeMeasurements(ingredient);
		ingredient = ingredient.replaceAll( "(?<!\\S)" + Pattern.quote("to") + "(?!\\S)", "");
		ingredient = ingredient.replaceAll("[^A-Za-z]", " ");
		
		ingredient = ingredient.trim();
		ingredient = ingredient.replaceAll( "(^)" + Pattern.quote("of") + "(?!\\S)", "");
		ingredient = ingredient.trim();
		ingredient = ingredient.replaceAll( "(^)" + Pattern.quote("and") + "(?!\\S)", "");
		ingredient = ingredient.trim();
		ingredient = ingredient.replaceAll( "(^)" + Pattern.quote("or") + "(?!\\S)", "");
		System.out.println();
		System.out.println(ingredient);
		return ingredient;
	}
	
	private String removeMeasurements(String ingredient) {
		String[] measurementUnits = {"cups","cup","head","teaspoon","teaspoons","tsp",
				"tablespoons","tbsp","tablespoon","clove","cloves","fluid ounce","fl oz","pint",
				"quart","qt","qt.","oz","ounces","ounce","gallon","gal","milliliter","ml","cc","liter","litre",
				"gram","grams","g","thread","threads"};
		for (String word : measurementUnits) {
			String pattern = "(?<!\\S)" + Pattern.quote(word) + "(?!\\S)";
			ingredient = ingredient.replaceAll(pattern, "");
		}
		
		return ingredient;
	}
    
    private String addConjunctToIngreds(String answer, JSONArray ingreds) {
    	answer = answer.substring(0, answer.length()-2);
		answer += ", and " + extractIngredientFromString((String) ingreds.get(ingreds.size()-1)) + ".";
		return answer;
	}

    private JSONObject findMatchingIngredList(int numIngredParts, JSONArray ingredlist, String partname) {
    	// Start matching
		double tmpDice = 0;
		JSONObject tempIngredPartObj = null;
		for (int i = 0; i < numIngredParts; i++) {

			JSONObject ingredPartObj = DetailGetter.getIngredPartObj(ingredlist, i);
			String ingredPartName    = DetailGetter.getIngredPartName(ingredPartObj);
			ingredPartName = ingredPartName.replaceAll("Method [0-9] ", "").toLowerCase().trim();
			
			// Compute Dice coefficient
			double diceCoeff = Similarity.diceCoefficientOptimized(partname, ingredPartName);
			System.out.println(partname + "\tVS\t" + ingredPartName + "    diceCoeff: " + diceCoeff);
			
			// Take the highest dice coeff
			if (diceCoeff == 1.0) {
				tempIngredPartObj = ingredPartObj;
				break;
			}
			else if (diceCoeff > tmpDice && diceCoeff > 0.4) {
				tmpDice = diceCoeff;
				tempIngredPartObj = ingredPartObj;
			}
		}
		return tempIngredPartObj;
	}
    
	private String retrieveOnlyIngredlist(JSONArray ingredlist) {
		String answer = "";
		answer = "There are no ingredients listed for this recipe. ";
		
		JSONObject ingredPart = DetailGetter.getIngredPartObj(ingredlist, 0);
		JSONArray  ingreds    = DetailGetter.getIngredients(ingredPart);
		if (ingreds.size() != 0) {
			answer = concateIngreds(ingreds, -1);
			if (!answer.isEmpty() && !answer.equals(" ")) {
				answer = addConjunctToIngreds(answer, ingreds);
				answer = "We're gonna need: " + answer;
			}
		}
		
		return answer;
	}
	
	private String retrieveCollectedIngredlist(JSONArray ingredlist) {
		String answer = "";
		
		JSONObject ingredPart1 = DetailGetter.getIngredPartObj(ingredlist, 0);
		JSONArray  ingreds1	   = DetailGetter.getIngredients(ingredPart1);
		JSONObject ingredPart2 = DetailGetter.getIngredPartObj(ingredlist, 1);
		JSONArray  ingreds2	   = DetailGetter.getIngredients(ingredPart2);
		answer += concateIngreds(ingreds1, 0);
		answer += concateIngreds(ingreds2, -1);
		if (!answer.isEmpty() && !answer.equals(" ")) {
			if (ingreds2.size() > 0) {
				answer = addConjunctToIngreds(answer, ingreds2);
			}
			answer = "We're gonna need: " + answer;
		}
		
		return answer;
	}
	
    /**
     * Retrieve method ingredients when reading the first sentence of the method.
     */
    private String retrieveMethodIngreds(String partName, int numParts, int stepIndex, 
    		JSONObject recipe, String url) {
    	String answer = "";
    	
    	boolean hasMultParts = false;
    	boolean isFirstStep = false;
    	
    	if (numParts > 1) {hasMultParts = true;}
    	if (stepIndex == 0) {isFirstStep = true;}
    	
    	if (!url.isEmpty()) {
			try {
				recipe = (JSONObject) new JSONParser().parse(new FileReader(url));
			} catch (Exception ignore) {}
		}
    	
    	// Retrieval prerequisite: first step of a method
    	if (isFirstStep) {
    		// If the recipe has only one part, the ingreds were already read after recipe confirmation
			if (hasMultParts) {
				System.out.println("Now we're at " + partName);
				System.out.println(partName);
				
				answer = retrieveTargetIngreds(url, partName, -1);
				if (!answer.isEmpty() && !answer.equals(" ")) {
					answer = "We're gonna need: " + answer;
				}
			}
		}
    	return answer;
	}
    
    private String retrieveTargetIngreds(String url, String target, int index) {
		String answer = "";
		
		try {
			JSONObject recipe = (JSONObject) new JSONParser().parse(new FileReader(url));
			
			JSONArray ingredlist = DetailGetter.getCompleteIngredList(recipe);
			// If no ingred list exists, quit
			if (ingredlist == null) {
				System.out.println("This ingredlist is empty");
				return "";
			}
			int numIngredParts = ingredlist.size();
			if (numIngredParts == 0) {
				System.out.println("This ingredlist is empty");
				return "";
			}
			
			System.out.println("And we're here!");
			
			if (target == null && index != -1) {
				System.out.println("Retrieving through ingred index \t" + ingredlist.size());
				JSONArray parts = DetailGetter.getAllParts(recipe);
				JSONObject part = DetailGetter.getPart(parts, index-1);
				String partName = DetailGetter.getPartName(part);
				target = partName;
			}
			
			target = target.replaceAll("Method [0-9] ", "").toLowerCase().trim();	// Remove "method x" from part name
			
			// Find match
			JSONObject matchIngredPartObj = findMatchingIngredList(numIngredParts, ingredlist, target);
			
			if (matchIngredPartObj != null) {
				JSONArray ingreds = DetailGetter.getIngredients(matchIngredPartObj);
				
					answer = concateIngreds(ingreds, -1);
					if (!answer.isEmpty() && !answer.equals(" ")) {
						answer = answer.substring(0, answer.length()-2);
						answer += ", and " + extractIngredientFromString((String) ingreds.get(ingreds.size()-1)) + ".";
					}
				
				System.out.println("Reading ingreds from " + DetailGetter.getIngredPartName(matchIngredPartObj));
			}
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		
		return answer;
	}

    private String retrieveTargetMethod(Session session, JSONObject recipe, String url, String target, int index) {
    	String answer = "";
    	
    	
    	try {
			recipe = (JSONObject) new JSONParser().parse(new FileReader(url));
			
			JSONArray allParts = DetailGetter.getAllParts(recipe);
			int numParts = allParts.size();
			
			
			if (target == null && index != -1) {
				System.out.println("Retrieving through part index");
				answer = retrieveStep(session, recipe, url, index - 1, 0, numParts);
				String partname = SessionAttributes.getLastPartName(session);
				answer = partname + ". " + BREAK_HALF_SEC  
						+ retrieveMethodIngreds(partname, numParts, 0, recipe, url) + BREAK_HALF_SEC
						+ " So, here's how it goes: first, "
						+ answer;
				return answer;
			}
			
			target = target.replaceAll("Method [0-9] ", "").toLowerCase().trim();	// Remove "method x" from part name
			
			// Start matching
			double tmpDice = 0;
			int tempPartIndex = -1;
			for (int i = 0; i < numParts; i++) {
				
				JSONObject partObj = DetailGetter.getPart(allParts, i);
				String partName = DetailGetter.getPartName(partObj);
				partName = partName.replaceAll("Method [0-9] ", "").toLowerCase().trim();
				
				// Compute Dice coefficient
				double diceCoeff = Similarity.diceCoefficientOptimized(target, partName);
				System.out.println(target + "\tVS\t" + partName + "    diceCoeff: " + diceCoeff);
				
				// Take the highest dice coeff
				if (diceCoeff == 1.0) {
					tempPartIndex = i;
					break;
				}
				else if (diceCoeff > tmpDice && diceCoeff > 0.3) {
					tmpDice = diceCoeff;
					tempPartIndex = i;
				}
			}
			
			if (tempPartIndex != -1) {
				answer = retrieveStep(session, recipe, url, tempPartIndex, 0, numParts);
				String partname = SessionAttributes.getLastPartName(session);
				answer = partname + ". " + BREAK_HALF_SEC  
						+ retrieveMethodIngreds(partname, numParts, 0, recipe, url) + BREAK_HALF_SEC 
						+ " So, here's how it goes: first, "
						+ answer;
			}
			
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
    	
		return answer;
	}
    
    private String searchWithSpecifiedIngreds(Session session, String includeIngredient, 
    		String excludeIngredient, boolean denial) {
    	String answer = "Sorry, I cannot find any relevant article in the database. "
    			+ "You can remove certain existing criterias. For more information, ask for the current query.";
    	String searchParam = SessionAttributes.getSearchParam(session);
    	String alwaysExclude = SessionAttributes.getAlwaysExclude(session);
    	if (alwaysExclude != null) {
    		excludeIngredient = appendToIngredAttList(excludeIngredient, alwaysExclude);
		}
    	
    	if (denial) {
    		// Add the last url into list of denied recipes (shouldn't appear again in this session)
    		String lastUrl = SessionAttributes.getArticleURL(session);
    		SessionAttributes.addDeniedRecipe(session, lastUrl);
		}
    	
    	String url = SearchFiles.retrieveRecipes
    			(SessionAttributes.getQuery(session), searchParam, includeIngredient, excludeIngredient, 1);
    	
    	// Reset query index (this method can only be called directly by with/without intent and 
    	// thus always retrieves the first ingredient-specific result)
		SessionAttributes.setQueryResIndex(session, 1);
		
		// check denied?
		url = checkIfPrevlyDenied(session, url, null, searchParam, includeIngredient, excludeIngredient, 2);
		
		// Generate answer string
		if (url != null) {
			try {
				JSONObject recipe = (JSONObject) new JSONParser().parse(new FileReader(url.toString()));
				String title = DetailGetter.getTitle(recipe);
				
				answer = "Here! I found this: " + title + ". Is that what you're looking for?";
				
				SessionAttributes.setArticleURL(session, url);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			SessionAttributes.clearIngredParams(session);
		}
		SessionAttributes.setLastReadText(session, answer);
		return answer;
	}
    
    private String searchWithParams(Session session, String searchParam, boolean denial) {
		String answer = "Sorry, I cannot find any relevant article in the database."
    			+ "You can remove certain existing criterias. For more information, ask for the current query.";;
		String includedIngred = SessionAttributes.getIncludeIngred(session);
		String excludedIngred = SessionAttributes.getExcludeIngred(session);
		
		if (denial) {
    		// Add the last url into list of denied recipes (shouldn't appear again in this session)
    		String lastUrl = SessionAttributes.getArticleURL(session);
    		SessionAttributes.addDeniedRecipe(session, lastUrl);
		}
		
		String url = SearchFiles.retrieveRecipes
				(SessionAttributes.getQuery(session), searchParam, includedIngred, excludedIngred, 1);
		
		// Reset query index (this method can only be called directly by with/without intent and 
    	// thus always retrieves the first ingredient-specific result)
		SessionAttributes.setQueryResIndex(session, 1);
		
		// check denied?
		url = checkIfPrevlyDenied(session, url, null, searchParam, includedIngred, excludedIngred, 2);
		
		// Generate answer string
		if (url != null) {
			try {
				JSONObject recipe = (JSONObject) new JSONParser().parse(new FileReader(url.toString()));
				String title = DetailGetter.getTitle(recipe);
				
				answer = "Okay. Here! I found this: " + title;
				if (SessionAttributes.getCurrentState(session).equals(STATE_ADVENTUROUS)) {
					answer += ". What do you think?";
				} else {
					 answer += ". Is that what you're looking for?";
				}
				
				SessionAttributes.setArticleURL(session, url);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			SessionAttributes.clearSearchParam(session);
		}
		SessionAttributes.setLastReadText(session, answer);
		return answer;
	}
    
    private String retrieveTips(JSONObject stepObj) {
		
    	String answer = "";
    	JSONArray tips = DetailGetter.getStepTips(stepObj);
    	if (tips != null && tips.size() > 0) {
			answer += "Here are some tips: ";
			for (int i = 0; i < tips.size(); i++) {
				answer += (String) tips.get(i) + " " + BREAK_STRONG;
			}
		}
    	return answer;
	}
    
    private String appendToIngredAttList(String existingAtt, String newWord) {
    	if (existingAtt == null) {
    		existingAtt = newWord;
		} else {
			existingAtt += " " + newWord;
		}
    	return existingAtt;
	}
    
    
    
    /**
	 * Loads the properties.
	 */
	private static Properties loadProperties(String path){
		Properties props = new Properties();
		FileInputStream inputStream = null;
		Path configFile = Paths.get(path);
		try {
			inputStream = new FileInputStream(configFile.toString());
			props.load(inputStream);
		} catch (IOException ioe){
			ioe.printStackTrace();
		}
		return props;
	}
	
	
    
    
    
	
	
	// -------------------------------------------------------------------------
	// MAIN PROGRAM
	// -------------------------------------------------------------------------

	public static void main(final String[] args) throws Exception {
		AlexaService.mainV2(RecipeSearchSpeechlet.class, args);
	}
}
