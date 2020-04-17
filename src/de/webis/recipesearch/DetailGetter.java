package de.webis.recipesearch;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 *  GETTERS FOR JSON DOCUMENT
 * @author Jiani Qu
 *
 */
public class DetailGetter {

	static String getTitle(JSONObject recipe) {
		return (String) recipe.get("title");
	}

	static String getStructure(JSONObject recipe) {
		return (String) recipe.get("structure");
	}

	static JSONArray getCompleteIngredList(JSONObject recipe) {
		return (JSONArray) recipe.get("ingredients-thingsneeded");
	}

	static JSONObject getIngredPartObj(JSONArray ingredlist, int ingredpartIndex) {
		return (JSONObject) ingredlist.get(ingredpartIndex);
	}

	static String getIngredPartName(JSONObject ingredpart) {
		return (String) ingredpart.get("part-name");
	}

	static JSONArray getIngredients(JSONObject ingredpart) {
		return (JSONArray) ingredpart.get("part-ingredients");
	}

	static JSONArray getAllParts(JSONObject recipe) {
		return (JSONArray) recipe.get("parts");
	}

	static JSONObject getPart(JSONArray parts, int partIndex) {
		return (JSONObject) parts.get(partIndex);
	}

	static String getPartName(JSONObject partObj) {
		return (String) partObj.get("part");
	}

	static JSONArray getPartSteps(JSONObject partObj) {
		return (JSONArray) partObj.get("steps");
	}

	static JSONObject getStepObj(JSONArray partSteps, int stepIndex) {
		return (JSONObject) partSteps.get(stepIndex);
	}

	static String getStepText(JSONObject stepObj) {
		return (String) stepObj.get("step");
	}

	static JSONArray getStepTips(JSONObject stepObj) {
		return (JSONArray) stepObj.get("tips");
	}

	static JSONArray getSubsteps(JSONObject stepObj) {
		return (JSONArray) stepObj.get("sub-steps");
	}

	static JSONObject getSubstepObj(JSONArray substeps, int substepIndex) {
		return (JSONObject) substeps.get(substepIndex);
	}

	static String getSubstepText(JSONObject substep) {
		return (String) substep.get("sub-step");
	}
	
	static String getCategory(JSONObject recipe) {
		JSONArray catArr = (JSONArray) recipe.get("category");
		String catsString = "";
		for (int i = 1; i < catArr.size(); i++) {
			String category = (String) catArr.get(i);
			if (i == catArr.size()-1) {
				catsString += category;
			} else {
				catsString += category + ",";
			}
		}
		return catsString;
	}

}
