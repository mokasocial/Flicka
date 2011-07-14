/*
 * Copyright (c) 2010 MokaSocial, LLC
 */

package com.aetrion.flickr.people;

import java.io.Serializable;

/**
 * A class representing the genders as used by the Flickr API.
 * 
 * @author Michael Hradek <mhradek@mokasocial.com>
 */
public class Gender implements Serializable {
	
	public static final char MALE_CHAR = 'M';
	public static final char FEMALE_CHAR = 'F';
	public static final char UNKNOWN_CHAR = 'X';
	
	private char mChar;
	
	public static final Gender MALE = new Gender(MALE_CHAR);
	public static final Gender FEMALE = new Gender(FEMALE_CHAR);
	public static final Gender UNKNOWN = new Gender(UNKNOWN_CHAR);
	
    private Gender(char fChar) {
        mChar = fChar;
    }
    
    /**
     * Get the Gender object for the given character.
     * 
     * @param stringName
     * @return
     */
    public static Gender fromChar(char charName) {
    	if(Character.toUpperCase(charName) == MALE_CHAR) {
    		return MALE;
    	} else if(Character.toUpperCase(charName) == FEMALE_CHAR) {
    		return FEMALE;
    	} else {
    		return UNKNOWN;
    	}
    }
    
    /**
     * Get the Gender object for the given string.
     * 
     * @param stringName
     * @return
     */
    public static Gender fromString(String stringName) {
        if (stringName == null || "".equals(stringName)) {
            return UNKNOWN;
        }
        
    	return fromChar(stringName.charAt(0));
	}
    
    /**
     * Get the char representation of the object
     * 
     * @return
     */
    public char getGenderAsChar() {
    	return mChar;
    }
}
