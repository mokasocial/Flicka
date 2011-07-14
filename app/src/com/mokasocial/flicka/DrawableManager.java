package com.mokasocial.flicka;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

public class DrawableManager {
	
	/** DEFINITIONS */
    private Map<String, Drawable> drawableMap;
    private String saveLocation;

    /**
     * Constructor.
     */
    public DrawableManager(String saveLocation) {
    	drawableMap = new HashMap<String, Drawable>();
    	this.saveLocation = saveLocation;
    }

    /**
     * From whatever source whether it's local cache or remote URL, grab the image
     * and return a drawable.
     * 
     * @param urlString
     * @return
     */
    private Drawable fetchDrawable(String urlString) {
    	Utilities.debugLog(this, "Image url: " + urlString);
    	
    	try {
    		InputStream is = null;
    		
    		// Load the local copy first before attempting a load using the URL from Internet
    		is = ImageMgmt.loadImage(urlString, new File(saveLocation));
    		if(is == null) {
    			Utilities.debugLog(this, "Could not load locally; fetching from URL.");
    			is = ImageMgmt.fetchImage(urlString);
    			// Save it locally.
    			ImageMgmt.saveImage(is, urlString, new File(saveLocation));
    		}
    		
    		// We now try loading the input stream from one of the two sources above.
    		Drawable drawable = Drawable.createFromStream(is, "src");
    		
    		if(drawable == null) {
    			/** 
    			 * This is a work around for a known bug in the Android systems which do not return
    			 * buffered stream when loading things over the net. The solution is to download, save,
    			 * and then load the saved file. Users without SDCards will be screwed unless we decide
    			 * to go with using the phone memory. We will use the placeholder image if loads fail.
    			 */
				Utilities.debugLog(this, "Could not load initial fetch from URL. Attempting loading local image.");
				is = ImageMgmt.loadImage(urlString, new File(saveLocation));
	    		drawable = Drawable.createFromStream(is, "src");
    		}
    		
    		if(drawable == null) {
    			Utilities.debugLog(this, "Could not load drawable; returning null. Image url: " + urlString);
    		}
    		
    		if(is != null) {
    			is.close();
    		}
    			
    		return drawable;
    	} catch (Exception e) {
    		Utilities.errorOccurred(this, "Unable to fetch drawable.", e);
    		return null;
    	}
    }

    /**
     * Execute the fetching of drawables but perform the action from within a thread.
     * 
     * @param urlString
     * @param imageView
     */
    public void fetchDrawableOnThread(final String urlString, final ImageView imageView) {
    	if (drawableMap.containsKey(urlString)) {
    		imageView.setImageDrawable((Drawable) drawableMap.get(urlString));
    		return;
    	}

    	final Handler handler = new Handler() {
    		@Override
    		public void handleMessage(Message message) {
    			imageView.setImageDrawable((Drawable) message.obj);
    			// Put into the memory hash if it is valid
    			if(message.obj != null) {
    				drawableMap.put(urlString, (Drawable) message.obj);
    			}
    		}
    	};

    	Thread thread = new Thread() {
    		@Override
    		public void run() {
    			//TODO : set imageView to a "pending" image
    			Drawable drawable = fetchDrawable(urlString);
    			Message message = handler.obtainMessage(1, drawable);
    			handler.sendMessage(message);
    		}
    	};
    	thread.start();
    }
}