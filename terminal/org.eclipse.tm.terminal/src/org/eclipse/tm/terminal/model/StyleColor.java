package org.eclipse.tm.terminal.model;

import java.util.HashMap;
import java.util.Map;

/** 
 * 
 * Flyweight
 * Threadsafe.
 */
public class StyleColor {
	private final static Map fgStyleColors=new HashMap();
	final String fName;
	
	/**
	 * @param name the name of the color. It is up to the UI to associate a
	 * named color with a visual representation
	 * @return a StyleColor
	 */
	public static StyleColor getStyleColor(String name) {
		StyleColor result;
		synchronized (fgStyleColors) {
			result=(StyleColor) fgStyleColors.get(name);
			if(result==null) {
				result=new StyleColor(name);
				fgStyleColors.put(name, result);
			}
		}
		return result;
	}
	// nobody except the factory method is allowed to instantiate this class!
	private StyleColor(String name) {
		fName = name;
	}

	public String getName() {
		return fName;
	}

	public String toString() {
		return fName;
	}
	// no need to override equals and hashCode, because Object uses object identity
}