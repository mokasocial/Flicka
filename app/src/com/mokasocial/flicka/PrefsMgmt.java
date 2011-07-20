package com.mokasocial.flicka;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * This class manages preferences and can be called to get or set them. Remember
 * to override onStop and call savePreferences. All preferences should be
 * restored, set, get, and saved within this object.
 * 
 * @author Michael Hradek mhradek@gmail.com, mhradek@mokasocial.com,
 *         mhradek@flicka.mobi
 * @date 2010.01.22
 */
public class PrefsMgmt {

	static final String PREF_CONTACTS_UPDATE_NOTIFY = "contactsUpdateNotify";
	static final String PREF_PHOTOS_UPDATE_NOTIFY = "photosUpdateNotify";
	static final String PREF_USE_FULLSCREEN = "useFullscreen";
	static final String PREF_USE_LARGE_PHOTOS = "useLargePhotos";
	static final String PREF_LOAD_MORE_STREAM_AUTO = "loadMoreStreamAuto";

	private boolean mContactsNotify;
	private boolean mPhotosNotify;
	private boolean mUseFullscreen;
	private boolean mUseLargePhotos;
	private boolean mLoadMoreStreamAuto;

	private final Context mContext;

	/**
	 * Load the preferences management object capturing the context.
	 * 
	 * @param context
	 */
	public PrefsMgmt(Context context) {
		mContext = context;
	}

	/**
	 * Restore preferences from the shared preferences location.
	 */
	public void restorePreferences() {
		SharedPreferences settings = mContext.getSharedPreferences(Flicka.FLICKA_PREFERENCES_FILE, Flicka.MODE_PRIVATE);

		boolean enabled = settings.getBoolean(PrefsMgmt.PREF_CONTACTS_UPDATE_NOTIFY, true);
		setContactsNotifications(enabled);

		enabled = settings.getBoolean(PrefsMgmt.PREF_PHOTOS_UPDATE_NOTIFY, true);
		setPhotosNotifications(enabled);

		enabled = settings.getBoolean(PrefsMgmt.PREF_USE_FULLSCREEN, false);
		setUseFullScreen(enabled);

		enabled = settings.getBoolean(PrefsMgmt.PREF_USE_LARGE_PHOTOS, false);
		setUseLargePhotos(enabled);

		enabled = settings.getBoolean(PrefsMgmt.PREF_LOAD_MORE_STREAM_AUTO, true);
		setLoadMoreStreamAuto(enabled);
	}

	/**
	 * Save preferences to the shared preferences location. This MUST be called
	 * after any changes to any of the preferences to save them.
	 */
	public void savePreferences() {
		SharedPreferences settings = mContext.getSharedPreferences(Flicka.FLICKA_PREFERENCES_FILE, Flicka.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();

		editor.putBoolean(PrefsMgmt.PREF_CONTACTS_UPDATE_NOTIFY, isContactsNoticationsEnabled());
		editor.putBoolean(PrefsMgmt.PREF_PHOTOS_UPDATE_NOTIFY, isPhotosNotificationsEnabled());
		editor.putBoolean(PrefsMgmt.PREF_USE_FULLSCREEN, isUseFullscreenEnabled());
		editor.putBoolean(PrefsMgmt.PREF_USE_LARGE_PHOTOS, isUseLargePhotosEnabled());
		editor.putBoolean(PrefsMgmt.PREF_LOAD_MORE_STREAM_AUTO, isLoadMoreStreamAutoEnabled());

		boolean result = editor.commit();
		String commitResult = result ? "Successfully saved" : "Failed to save";
		Log.d("Flicka", commitResult + " SharedPreferences");
	}

	/**
	 * Whether or not the contacts updates notifications is enabled.
	 * 
	 * @return boolean
	 */
	public boolean isContactsNoticationsEnabled() {
		return mContactsNotify;
	}

	/**
	 * Set the contacts updates notifications enabled value.
	 * 
	 * @param enabled
	 */
	public void setContactsNotifications(boolean enabled) {
		mContactsNotify = enabled;
	}

	/**
	 * Whether or not the photos updates notifications is enabled.
	 * 
	 * @return boolean
	 */
	public boolean isPhotosNotificationsEnabled() {
		return mPhotosNotify;
	}

	/**
	 * Set the photos updates notifications enabled value.
	 * 
	 * @param enabled
	 */
	public void setPhotosNotifications(boolean enabled) {
		mPhotosNotify = enabled;
	}

	/**
	 * Set the use full screen enabled value.
	 * 
	 * @param enabled
	 */
	public void setUseFullScreen(boolean enabled) {
		mUseFullscreen = enabled;
	}

	/**
	 * Whether or not we should be using the full screen.
	 * 
	 * @return boolean
	 */
	public boolean isUseFullscreenEnabled() {
		return mUseFullscreen;
	}

	/**
	 * Set the use large photos enabled value.
	 * 
	 * @param enabled
	 */
	public void setUseLargePhotos(boolean enabled) {
		mUseLargePhotos = enabled;
	}

	/**
	 * Whether or not we should be showing large images.
	 * 
	 * @return boolean
	 */
	public boolean isUseLargePhotosEnabled() {
		return mUseLargePhotos;
	}

	/**
	 * Set the load more stream auto enabled value.
	 * 
	 * @param enabled
	 */
	public void setLoadMoreStreamAuto(boolean enabled) {
		mLoadMoreStreamAuto = enabled;
	}

	/**
	 * Whether or not we should be automatically loading more photos at the
	 * bottom of a stream.
	 * 
	 * @return boolean
	 */
	public boolean isLoadMoreStreamAutoEnabled() {
		return mLoadMoreStreamAuto;
	}
}
