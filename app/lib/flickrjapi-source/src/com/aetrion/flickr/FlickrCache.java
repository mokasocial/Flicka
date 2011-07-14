/*
 * Copyright (c) 2009, MokaSocial, LLC
 * All rights reserved.
 */

package com.aetrion.flickr;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Rather than having Flickr return the same responses and making extra calls
 * we will cache the response in memory.
 * 
 * We need to test how long things stay cached. 
 * 
 * The key format is: InterfaceName_23490823904 (Class name followed by the
 * hash of the paramters.
 *
 */
public class FlickrCache {
	private static FlickrCache mInstance;
	private Map<String, SoftReference<Response>> mHashMap;
	
	/** 
	 * This is a private constructor so singleton in nature.
	 */
	private FlickrCache() {
		mHashMap = new HashMap<String, SoftReference<Response>>();
	}
	
	/**
	 * Return an instance of our singleton.
	 * 
	 * @return
	 */
	public static FlickrCache getFlickrCache()
	{
		synchronized(FlickrCache.class) {
			if (mInstance == null) {
				mInstance = new FlickrCache();
			}
		}
		return mInstance;
	}
	
	/**
	 * Get the results object stored for this key.
	 * 
	 * @param key
	 * @return
	 */
	public Response get(String key) {
    	if (mHashMap.containsKey(key)) {
    		SoftReference<Response> softReference = mHashMap.get(key);
    		return softReference.get();
    	}
    	
    	return null;
	}
	
	/**
	 * Set the results object for the given key.
	 * 
	 * @param key
	 * @param response
	 */
	public void set(String key, Response response) {
		mHashMap.put(key, new SoftReference<Response>(response));
	}
	
	/**
	 * Prevent anyone from cloning a singleton. 
	 */
	public Object clone() throws CloneNotSupportedException {
	    throw new CloneNotSupportedException();
	}
}
