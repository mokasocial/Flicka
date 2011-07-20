package com.mokasocial.flicka;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;

import com.aetrion.flickr.photos.Photo;

/**
 * A container for all the possible SlideShow parameters. Usually to use within
 * the intent extras via Bundle.
 * 
 */
public class SlideShow {
	/**
	 * The current item which is being displayed. The consuming script will then
	 * in/decrement from this number.
	 * 
	 * NOTE: Flickr uses a 1 based numbering system for the lookups of items.
	 * That's why you'll see "position + 1" when this is being set.
	 */
	static final String CURRENT_ITEM = "currentItem";

	/**
	 * The current stream with which we are creating a slide show.
	 */
	static final String CURRENT_STREAM = "currentStream";

	/**
	 * The current identifier which should be used to grab a stream. For users
	 * this is an NSID, for groups it is a similar string. For the actual user
	 * this is null.
	 */
	static final String CURRENT_IDENTIFIER = "currentIdentifier";

	/**
	 * Whether the slide show should auto run.
	 */
	static final String AUTO_SLIDE_SHOW_MODE = "autoSlideShowMode";

	static final int STREAM_FAVORITES = 0;
	static final int STREAM_USER_PHOTOS = 1;
	static final int STREAM_GROUP_PHOTOS = 2;
	static final int STREAM_PLACE_PHOTOS = 3;
	static final int STREAM_SET_PHOTOS = 4;

	private static SlideShow mInstance;
	private final Map<String, SoftReference<Bitmap>> mBitmapMap;
	private final Map<String, SoftReference<Photo>> mPhotoMap;

	private SlideShow() {
		mBitmapMap = new HashMap<String, SoftReference<Bitmap>>();
		mPhotoMap = new HashMap<String, SoftReference<Photo>>();
	}

	public static SlideShow getSlideShow() {
		synchronized (SlideShow.class) {
			if (mInstance == null) {
				mInstance = new SlideShow();
			}
		}
		return mInstance;
	}

	public Bitmap getBitmap(String urlString) {
		if (mBitmapMap.containsKey(urlString)) {
			SoftReference<Bitmap> softReference = mBitmapMap.get(urlString);
			return softReference.get();
		}

		return null;
	}

	public void setBitmap(String urlString, Bitmap imageBitmap) {
		mBitmapMap.put(urlString, new SoftReference<Bitmap>(imageBitmap));
	}

	public Photo getPhoto(String photoId) {
		if (mPhotoMap.containsKey(photoId)) {
			SoftReference<Photo> softReference = mPhotoMap.get(photoId);
			return softReference.get();
		}

		return null;
	}

	public void setPhoto(String photoId, Photo photo) {
		mPhotoMap.put(photoId, new SoftReference<Photo>(photo));
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
}
