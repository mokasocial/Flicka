package com.mokasocial.flicka;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Utilities {

	/**
	 * Notify the user something went wrong using a toast message.
	 * 
	 * @param context
	 * @param message
	 */
	public static void alertUser(Context context, String message, Exception e) {
		if (e != null) {
			Toast.makeText(context, message + " Code: [" + e.toString() + "]", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * A simple popup with okay button.
	 * 
	 * @param context
	 * @param message
	 */
	public static void notifyUser(Context context, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message).setCancelable(false).setPositiveButton(context.getString(R.string.option_okay), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Load up the layout and build the various strings for a particular
	 * activity.
	 * 
	 * @param Activity
	 *            activity
	 * @param int activityName
	 * @param int activityLayout
	 */
	public static void setupActivityView(final Activity activity, int activityName, int activityLayout) {
		activity.setTitle(activity.getTitle() + " " + activity.getString(activityName));
		Utilities.setupActivityScreen(activity);
		activity.setContentView(activityLayout);
		final TextView sectionName = (TextView) activity.findViewById(R.id.main_breadcrumb_current_activity);
		sectionName.setText(activity.getString(activityName));
	}

	/**
	 * Set the text of the last part of the breadcrumb.
	 * 
	 * @param activity
	 * @param text
	 * @param clickable
	 */
	public static void setupActivityBreadcrumbEndText(Activity activity, String text) {
		if (text == null || text == "") {
			final ImageView rightMostArrow = (ImageView) activity.findViewById(R.id.arrow_right_2);
			rightMostArrow.setVisibility(ImageView.GONE);
			return;
		}

		final TextView breadCrumbEndText = (TextView) activity.findViewById(R.id.main_breadcrumb_current_user);
		breadCrumbEndText.setText(text);
	}

	/**
	 * Set whether the screen is fullscreen or not. Default is yes set by the
	 * manifest.
	 * 
	 * @param activity
	 */
	public static void setupActivityScreen(Activity activity) {
		final PrefsMgmt prefs = new PrefsMgmt(activity);
		prefs.restorePreferences();
		if (prefs.isUseFullscreenEnabled() == true) {
			activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
			activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	/**
	 * Take the icon URL string as provided by a FlickrJ object and grab the
	 * file name omitting the path.
	 * 
	 * @param String
	 *            urlString
	 * @return
	 */
	public static String extractFilenameFromUrl(String urlString) {
		String[] pathParts = urlString.split("/");
		return pathParts[pathParts.length - 1];
	}

	/**
	 * Take the icon URL string as provided by a FlickrJ object and grab the
	 * path omitting the file name.
	 * 
	 * @param urlString
	 * @return
	 */
	public static String extractDirectoryFromUrl(String urlString) {
		urlString = urlString.substring(0, urlString.lastIndexOf("/"));
		return urlString.substring(urlString.indexOf("//") + 2);
	}

	/**
	 * Gets the software version and version name for this application
	 * 
	 * @param context
	 * @return
	 */
	public static PackageInfo getSoftwareVersion(Context context) {
		PackageInfo info = null;
		try {
			info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			// info.versionCode; // this is the Android mobile store version
			// (ie, 1, 2)
			// info.versionName; // this is the actual version (ie "1.0", "1.1")
		} catch (Exception e) {
			// Since we can't use 'this' inside static functions...
			Utilities utils = Utilities.createUtilitiesObj();
		}
		return info;
	}

	/**
	 * Convert the image URI to the direct file system path of the image file.
	 * 
	 * @param contentUri
	 * @return
	 */
	public static String getRealPathFromURI(Activity activity, Uri contentUri) {
		try {
			String[] proj = { MediaStore.Images.Media.DATA };
			Cursor cursor = activity.managedQuery(contentUri, proj, // Which
																	// columns
																	// to return
			null, // WHERE clause; which rows to return (all rows)
			null, // WHERE clause selection arguments (none)
			null); // Order-by clause (ascending by name)

			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			String temp = cursor.getString(column_index);
			cursor.close();
			return temp;
		} catch (Exception e) {
			e.printStackTrace();
			Utilities utils = Utilities.createUtilitiesObj();
			return contentUri.getPath();
		}
	}

	/**
	 * what the fuck is this
	 */
	private static Utilities createUtilitiesObj() {
		return new Utilities();
	}
}
