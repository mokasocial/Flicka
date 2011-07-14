package com.mokasocial.flicka;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

/**
 * The settings activity where users pick and perform settings related things.
 * 
 * @author Michael Hradek mhradek@gmail.com, mhradek@mokasocial.com, mhradek@flicka.mobi
 * @date 2010.01.22
 */
public class ActivitySettings extends Activity {

	PrefsMgmt mPrefsMgmt;
	Context mContext;
	Activity mActivity;

	private final static int FINISHED_FLUSHING_CACHED_DATA = 0;
	private final static int FINISHED_DEAUTHORIZING_DEVICE = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		mActivity = this;

		// Set up the various strings and view for this sub activity.
		Utilities.setupActivityView(mActivity, R.string.settings, R.layout.view_settings);
		Utilities.setupActivityBreadcrumbEndText(mActivity, null);

		// Load the preferences
		mPrefsMgmt = new PrefsMgmt(mContext);
		mPrefsMgmt.restorePreferences();

		// Now set the various checkboxes and things in the settings layout.
		setContactUpdateCheckBox();
		setUseFullscreenCheckBox();
		setUseLargePhotosCheckBox();
		setAutoLoadMorePhotosCheckBox();
	}

	@Override
	public void onStop() {
		super.onStop();

		// Our settings will be saved.
		mPrefsMgmt.savePreferences();
	}

	/**
	 * Set the checkbox depending on what is listed in the settings file.
	 */
	private void setContactUpdateCheckBox() {
		final CheckBox checkBox = (CheckBox) findViewById(R.id.contact_updates_notify_cb);
		final boolean enabled = mPrefsMgmt.isContactsNoticationsEnabled();
		checkBox.setChecked(enabled);
	}

	/**
	 * Set the appropriate value in the preferences object depending on what
	 * the user checks.
	 * 
	 * @param view
	 */
	public void updateContactUpdateNotifySetting(View view) {
		final CheckBox checkBox = (CheckBox) findViewById(R.id.contact_updates_notify_cb);
		mPrefsMgmt.setContactsNotifications(checkBox.isChecked());
	}

	/**
	 * Set the checkbox depending on what is listed in the settings file.
	 */
	private void setUseFullscreenCheckBox() {
		final CheckBox checkBox = (CheckBox) findViewById(R.id.use_full_screen_cb);
		final boolean enabled = mPrefsMgmt.isUseFullscreenEnabled();
		checkBox.setChecked(enabled);
	}

	/**
	 * Set the appropriate value in the preferences object depending on what
	 * the user checks.
	 * 
	 * @param view
	 */
	public void updateUseFullscreenSetting(View view) {
		final CheckBox checkBox = (CheckBox) findViewById(R.id.use_full_screen_cb);
		mPrefsMgmt.setUseFullScreen(checkBox.isChecked());
	}
	
	/**
	 * Set the checkbox depending on what is listed in the settings file.
	 */
	private void setUseLargePhotosCheckBox() {
		final CheckBox checkBox = (CheckBox) findViewById(R.id.use_large_photos_cb);
		final boolean enabled = mPrefsMgmt.isUseLargePhotosEnabled();
		checkBox.setChecked(enabled);
	}

	/**
	 * Set the appropriate value in the preferences object depending on what
	 * the user checks.
	 * 
	 * @param view
	 */
	public void updateUseLargePhotosSetting(View view) {
		final CheckBox checkBox = (CheckBox) findViewById(R.id.use_large_photos_cb);
		mPrefsMgmt.setUseLargePhotos(checkBox.isChecked());
	}
	
	/**
	 * Set the checkbox depending on what is listed in the settings file.
	 */
	private void setAutoLoadMorePhotosCheckBox() {
		final CheckBox checkBox = (CheckBox) findViewById(R.id.use_auto_load_photos_cb);
		final boolean enabled = mPrefsMgmt.isLoadMoreStreamAutoEnabled();
		checkBox.setChecked(enabled);
	}

	/**
	 * Set the appropriate value in the preferences object depending on what
	 * the user checks.
	 * 
	 * @param view
	 */
	public void updateAutoLoadMorePhotosSetting(View view) {
		final CheckBox checkBox = (CheckBox) findViewById(R.id.use_auto_load_photos_cb);
		mPrefsMgmt.setLoadMoreStreamAuto(checkBox.isChecked());
	}

	/**
	 * Remove the tokens and clear the caches.
	 * 
	 */
	public void deauthorizeDevice() {

		Utilities.debugLog(this, "Removing authorization token(s)...");

		// Flush the tables
		Database.flushAllTable(mContext);

		// If they are removing their auth, flush everything.
		flushCachedData();

		// also, deactivate notifications, since we're no longer authorized
		NotifyMgmt.cancelNotifications(mContext);
	}

	/**
	 * 
	 * @see deauthorizeDevice()
	 * @param view
	 */
	public void deauthorizeDevice(View view) {
		Thread deauthorizeDeviceThread = new Thread(){
			@Override
			public void run() {
				deauthorizeDevice();
				settingsHandler.sendEmptyMessage(FINISHED_DEAUTHORIZING_DEVICE);
			}
		};
		deauthorizeDeviceThread.start();
	}

	/**
	 * Flush all the cached data
	 * 
	 */
	public void flushCachedData() {
		try {
			deleteFiles(new File(Flicka.CONTACT_ICON_DIR));
			deleteFiles(new File(Flicka.GROUP_ICON_DIR));
			deleteFiles(new File(Flicka.PHOTO_ICON_DIR));
			deleteFiles(new File(Flicka.PHOTO_CACHE_DIR));

			Utilities.debugLog(this, "Removing the cached database entries...");
			Database.flushAllCacheTables(mContext);
		}
		catch (Exception e) {
			Utilities.errorOccurred(this, "Problem flushing data", e);
		}
	}

	/**
	 * Delete the directory, sub directories, and files within.
	 * 
	 * @param directory
	 */
	private void deleteFiles(File directory) {
		Utilities.debugLog(this, "Attempting to remove: " + directory.getName());
		if(!directory.exists()) {
			Utilities.debugLog(this, "Directory '" + directory.getPath() + "' does not exist");
			return;
		}

		// Go through directory contents and delete
		String[] fileList = directory.list();
		Utilities.debugLog(this, "Removing " + fileList.length + " files");
		for(int i = 0; i < fileList.length; i++){
			File targetDeleteFile = new File(directory.getPath() + File.separator + fileList[i]);
			if(targetDeleteFile.isFile()) {
				targetDeleteFile.delete();
				continue;
			}

			// Directories must be empty before we can delete them.
			if(targetDeleteFile.isDirectory()) {
				deleteFiles(targetDeleteFile);
				targetDeleteFile.delete();
				continue;
			}

			Utilities.debugLog(this, "Encountered something that was neither a file or directory");
		}

		// Remove the directory
		directory.delete();
	}

	/**
	 * Threaded cache flush accessible from view.
	 * 
	 * @param view
	 */
	public void flushCachedData(View view) {
		Thread flushCachedDataThread = new Thread(){
			@Override
			public void run() {
				flushCachedData();
				settingsHandler.sendEmptyMessage(FINISHED_FLUSHING_CACHED_DATA);
			}
		};
		flushCachedDataThread.start();
	}

	/**
	 * The settings thread handler.
	 * 
	 */
	final private Handler settingsHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FINISHED_FLUSHING_CACHED_DATA:
				Utilities.alertUser(mContext, mContext.getString(R.string.settings_clear_cached_data_success), null);
				break;
			case FINISHED_DEAUTHORIZING_DEVICE:
				// Disable button
				final Button resetButton = (Button) findViewById(R.id.removeAuthTokenId);
				resetButton.setEnabled(false);
				Utilities.notifyUser(mContext, mContext.getString(R.string.modal_settings_deauthorize));
				break;
			}
		}
	};
}