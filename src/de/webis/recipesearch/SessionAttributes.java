package de.webis.recipesearch;

import java.util.List;

import org.jsoup.Jsoup;

import com.amazon.speech.speechlet.Session;


/**
 * SESSION ATTRIBUTE SETTERS AND GETTERS
 * @author Jiani Qu
 *
 */
public class SessionAttributes {

	/**
	 * Save last query inside the session.
	 *
	 * @param request Speechlet request
	 * @param lastQuery query string
	 * @param lastResIndex previous query response index
	 */
	static void setQuery(final Session session, String lastQuery, int lastResIndex) {
	    session.setAttribute("lastQuery", lastQuery);
	    session.setAttribute("lastIndex", lastResIndex);
	}

	static String getQuery(final Session session) {
	    return (String) session.getAttribute("lastQuery");
	}

	static void setQueryResIndex(final Session session, int lastResIndex) {
		session.setAttribute("lastIndex", lastResIndex);
	}

	static int getQueryResIndex(final Session session) {
	    return (Integer) session.getAttribute("lastIndex");
	}

	static void setIncludeIngred(final Session session, String ingredient) {
		session.setAttribute("includeIngred", ingredient);
	}

	static String getIncludeIngred(final Session session) {
		return (String) session.getAttribute("includeIngred");
	}

	static void setExcludeIngred(final Session session, String ingredient) {
		session.setAttribute("excludeIngred", ingredient);
	}

	static String getExcludeIngred(final Session session) {
		return (String) session.getAttribute("excludeIngred");
	}

	static void clearIngredParams(final Session session) {
		setExcludeIngred(session, null);
		setIncludeIngred(session, null);
	}

	static void setSearchParam(final Session session, String param) {
		session.setAttribute("searchParameter", param);
	}

	static String getSearchParam(final Session session) {
		return (String) session.getAttribute("searchParameter");
	}

	static void clearSearchParam(final Session session) {
		setSearchParam(session, null);
	}

	/**
	 * Save query into search history.
	 */
	static void addToSearchHistory(final Session session, String query) {
		SessionAttributes.getSearchHistory(session).add(query);
	}

	@SuppressWarnings("unchecked")
	static List<String> getSearchHistory(final Session session) {
		return (List<String>) session.getAttribute("searchHistory");
	}

	/**
	 * Save last read step info in this session
	 * @param session
	 * @param abstractionLvl
	 * @param lastStepIndex
	 */
	static void setLastStepInfo(final Session session, int lastStepIndex, int numSteps) {
		session.setAttribute("lastStepIndex", lastStepIndex);
		session.setAttribute("numSteps", numSteps);
	}

	static int getLastStepIndex(final Session session) {
		return (int) session.getAttribute("lastStepIndex");
	}

	static int getNumSteps(final Session session) {
		return (int) session.getAttribute("numSteps");
	}

	/**
	 * Save info from last part
	 * @param session
	 * @param lastPartIndex
	 * @param numParts
	 */
	static void setLastPartInfo(final Session session, int lastPartIndex, int numParts, String partName) {
		session.setAttribute("lastPartIndex", lastPartIndex);
		session.setAttribute("numParts", numParts);
		session.setAttribute("lastPartName", partName);
	}

	static int getLastPartIndex(final Session session) {
		return (int) session.getAttribute("lastPartIndex");
	}

	static int getNumParts(final Session session) {
	 	return (int) session.getAttribute("numParts");
	}

	static String getLastPartName(final Session session) {
		return (String) session.getAttribute("lastPartName");
	}

	/**
	 * Set the current state (sum/ingred/steps)
	 * @param args
	 * @throws Exception
	 */
	static void setCurrentState(final Session session, String state) {
		session.setAttribute("currentState", state);
	}

	static String getCurrentState(final Session session) {
		return (String) session.getAttribute("currentState");
	}

	/**
	 * Save the most recently read text
	 * @param session
	 * @param text
	 */
	static void setLastReadText(final Session session, String text) {
		text = Jsoup.parse(text).text();
		session.setAttribute("lastReadText", text);
	}

	static String getLastReadText(final Session session) {
		return (String) session.getAttribute("lastReadText");
	}

	/**
	 * Set the abstraction level that user wants
	 * @param session
	 * @param detailLevel
	 */
	static void setAbstractionLevel(final Session session, int detailLevel) {
		session.setAttribute("abstractionLevel", detailLevel);
	}

	static int getAbstractionLevel(final Session session) {
		return (int) session.getAttribute("abstractionLevel");
	}

	static void increaseAbstractionLevel(final Session session) {
		int detailLevel = getAbstractionLevel(session);
		session.setAttribute("abstractionLevel", detailLevel + 1);
	}

	static void decreaseAbstractionLevel(final Session session) {
		int detailLevel = getAbstractionLevel(session);
		session.setAttribute("abstractionLevel", detailLevel - 1);
	}

	/**
	 * Store the current article url
	 * @param session
	 * @param url
	 */
	static void setArticleURL(final Session session, String url) {
		session.setAttribute("url", url);
	}

	static String getArticleURL(final Session session) {
		return (String) session.getAttribute("url");
	}

	/**
	 * Turn on skill help info
	 */
	static void setHelpOn(final Session session) {
		session.setAttribute("helpInterfaceOn", true);
	}

	/**
	 * Turn off skill help info
	 */
	static void setHelpOff(final Session session) {
		session.setAttribute("helpInterfaceOn", false);
	}

	/**
	 * Toggle skill help info
	 */
	static void toggleHelp(final Session session) {
		boolean helpOn = (boolean) session.getAttribute("helpInterfaceOn");
		if (helpOn) setHelpOff(session);
		else setHelpOn(session);
	}

	static boolean getHelpStatus(final Session session) {
		return (boolean) session.getAttribute("helpInterfaceOn");
	}

	static void setListHelpOn(final Session session) {
		session.setAttribute("listHelpOn", true);
	}

	static boolean getListHelpStatus(final Session session) {
		return (boolean) session.getAttribute("listHelpOn");
	}

	static void setListHelpOff(final Session session) {
		session.setAttribute("listHelpOn", false);
	}

	static void setCurrentIngredList(final Session session, String ingreds) {
		session.setAttribute("currentIngreds", ingreds);
	}

	static String getCurrentIngredList(final Session session) {
		return (String) session.getAttribute("currentIngreds");
	}

	static void setCurrentIngredIndex(final Session session, int index) {
		session.setAttribute("currentIngredIndex", index);
	}

	static int getCurrentIngredIndex(final Session session) {
		return (int) session.getAttribute("currentIngredIndex");
	}

	static void setGoShopping(final Session session) {
		session.setAttribute("goShopping", true);
	}

	static void setGoShoppingOff(final Session session) {
		session.setAttribute("goShopping", false);
	}

	static boolean getShoppingStatus(final Session session) {
		return (boolean) session.getAttribute("goShopping");
	}

	static void setIngredientsReadTrue(final Session session) {
		session.setAttribute("IngredAlreadyRead", true);
	}

	static void setIngredientsReadFalse(final Session session) {
		session.setAttribute("IngredAlreadyRead", false);
	}

	static boolean getIngredientsReadStatus(final Session session) {
		return (boolean) session.getAttribute("IngredAlreadyRead");
	}

	static void setMethodsReadFalse(final Session session) {
		session.setAttribute("MethodsRead", false);
	}

	static void setMethodsReadTrue(final Session session) {
		session.setAttribute("MethodsRead", true);
	}

	static boolean getMethodsReadStatus(final Session session) {
		return (boolean) session.getAttribute("MethodsRead");
	}

	static void setRecipeTitle(final Session session, String title) {
		session.setAttribute("Title", title);
	}

	static String getRecipeTitle(final Session session) {
		return (String) session.getAttribute("Title");
	}

	static void setAlwaysExclude(final Session session, String ingred) {
		session.setAttribute("alwaysExclude", ingred);
	}

	static String getAlwaysExclude(final Session session) {
		return (String) session.getAttribute("alwaysExclude");
	}
	
	static void setRecipeCategory(final Session session, String category) {
		session.setAttribute("recipeCategory", category);
	}
	
	static String getRecipeCategory(final Session session) {
		return (String) session.getAttribute("recipeCategory");
	}
	
	static void addDeniedRecipe(final Session session, String url) {
		SessionAttributes.getDeniedRecipes(session).add(url);
	}

	@SuppressWarnings("unchecked")
	static List<String> getDeniedRecipes(final Session session) {
		return (List<String>) session.getAttribute("deniedRecipes");
	}
}
