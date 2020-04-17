package de.webis.recipesearch.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import edu.smu.tspell.wordnet.*;

import net.didion.jwnl.*;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.PointerUtils;
import net.didion.jwnl.data.list.PointerTargetTree;
import net.didion.jwnl.data.relationship.*;
import net.didion.jwnl.dictionary.Dictionary;
import net.didion.jwnl.test.generic.TestDefaults;

public class SynSetGetter {
	static WordNetDatabase database = WordNetDatabase.getFileInstance(); 
	
	private static void demonstrateTreeOperation(IndexWord word) throws JWNLException {
        // Get all the hyponyms (children) of the first sense of <var>word</var>
//		for (int i = 1; i < word.getSenses().length; i++) {
			
			PointerTargetTree hyponyms = PointerUtils.getInstance().getHyponymTree(word.getSense(1));
			System.out.println("Hyponyms of \"" + word.getLemma() + "\":");
			hyponyms.print();
//		}
	}
	
	private static void demonstrateListOperation(IndexWord word) throws JWNLException {
        // Get all of the hypernyms (parents) of the first sense of <var>word</var>
        PointerTargetTree hypernyms = PointerUtils.getInstance().getHypernymTree(word.getSense(1));
        System.out.println("Direct hypernyms of \"" + word.getLemma() + "\":");
        hypernyms.print();
	}
	
	private static void getSynonymTree(IndexWord word) throws JWNLException {
        // Get all of the hypernyms (parents) of the first sense of <var>word</var>
        PointerTargetTree synonym = PointerUtils.getInstance().getSynonymTree(word.getSense(1), 2);
        System.out.println("Direct synonyms of \"" + word.getLemma() + "\":");
        synonym.print();
	}
	
	 private static void demonstrateAsymmetricRelationshipOperation(IndexWord start, IndexWord end) throws JWNLException {
	        // Try to find a relationship between the first sense of <var>start</var> and the first sense of <var>end</var>
	        RelationshipList list = RelationshipFinder.getInstance().findRelationships(start.getSense(1), end.getSense(1), PointerType.HYPERNYM);
	        System.out.println("Hypernym relationship between \"" + start.getLemma() + "\" and \"" + end.getLemma() + "\":");
	        for (Iterator itr = list.iterator(); itr.hasNext();) {
	            ((Relationship) itr.next()).getNodeList().print();
	        }
	        System.out.println("Common Parent Index: " + ((AsymmetricRelationship) list.get(0)).getCommonParentIndex());
	        System.out.println("Depth: " + ((Relationship) list.get(0)).getDepth());
	}
	
	public static void main(String[] args) {
		try {  
			JWNL.initialize(TestDefaults.getInputStream());
			
			
			IndexWord MEAT = Dictionary.getInstance().getIndexWord(POS.NOUN, "meat");
			IndexWord BEEF = Dictionary.getInstance().getIndexWord(POS.NOUN, "beef");
			IndexWord PORK = Dictionary.getInstance().getIndexWord(POS.NOUN, "pork");
//			demonstrateListOperation(PORK);
//			demonstrateListOperation(MEAT);
//			demonstrateListOperation(BEEF);
			
			System.out.println("\n\n\n");
			
			demonstrateTreeOperation(MEAT);
//			getSynonymTree(MEAT);
			
		} catch (JWNLException e) {
			e.printStackTrace();
		}
	}
}
