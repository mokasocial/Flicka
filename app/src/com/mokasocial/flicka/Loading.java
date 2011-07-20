package com.mokasocial.flicka;

import android.app.Activity;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This class is tightly coupled with the loading spinner stuff on the start of
 * certain activities.
 * 
 * The following are REQUIRED for this class to work within your activities:
 * 
 * 
 * <include android:id="@+id/loading" layout="@layout/loading"/>
 * 
 * <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
 * android:id="@+id/activity_layout" android:layout_width="fill_parent"
 * android:layout_height="fill_parent" android:visibility="gone">
 * 
 * ..........
 * 
 * 
 * </RelativeLayout>
 * 
 * 
 * @author Michael Hradek mhradek@gmail.com, mhradek@mokasocial.com,
 *         mhradek@flicka.mobi
 * @date 2010.02.06
 */
public class Loading {

	/**
	 * This set of constants are for use for loading activities
	 */
	public static final int ACTIVITY_LOADING_ICON = R.id.activity_loading_icon;
	public static final int ACTIVITY_LOADING_TEXT = R.id.activity_loading_text;
	public static final int ACTIVITY_LOADING_PARENT = R.id.loading_activity;
	public static final int ACTIVITY_LOADING_TARGET = R.id.activity_layout;
	public static final int ACTIVITY_LOADING_LAYOUT = R.id.activity_loading;
	public static final int ACTIVITY_FAILED_LOAD = R.id.activity_failed_load;
	public static final int ACTIVITY_NO_DISPLAY = R.id.activity_no_display;

	/**
	 * This set of constants are for use for loading drawers
	 */
	public static final int DRAWER_LOADING_ICON = R.id.drawer_loading_icon;
	public static final int DRAWER_LOADING_TEXT = R.id.drawer_loading_text;
	public static final int DRAWER_LOADING_PARENT = R.id.loading_drawer;
	public static final int DRAWER_LOADING_TARGET = R.id.sliding_drawer_loadable_content;
	public static final int DRAWER_LOADING_LAYOUT = R.id.drawer_loading;
	public static final int DRAWER_FAILED_LOAD = R.id.drawer_failed_load;
	public static final int DRAWER_NO_DISPLAY = R.id.drawer_no_display;

	/**
	 * Animate the given image (by id) by rotating it.
	 * 
	 * @param Activity
	 * @param int
	 */
	public static void rotatingAnimImageView(Activity activity, int imageViewId) {
		final ImageView imageView = (ImageView) activity.findViewById(imageViewId);
		float positionPercentage = (float) 0.5;
		Animation anim = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, positionPercentage, Animation.RELATIVE_TO_SELF, positionPercentage);
		anim.setRepeatCount(Animation.INFINITE);
		anim.setDuration(500L);
		anim.setRepeatMode(Animation.RESTART);
		anim.setInterpolator(new LinearInterpolator());

		imageView.startAnimation(anim);
	}

	/**
	 * Start the loading display.
	 * 
	 * @param activity
	 * @param loadingTextId
	 * @param loadingIconId
	 */
	public static void start(Activity activity, int loadingTextViewId, int loadingTextId, int loadingIconId) {
		final TextView loadingTextView = (TextView) activity.findViewById(loadingTextViewId);
		loadingTextView.setText(activity.getString(loadingTextId));
		Loading.rotatingAnimImageView(activity, loadingIconId);
	}

	/**
	 * Update the text inside the loading display. Optional.
	 * 
	 * @param activity
	 * @param loadingTextViewId
	 * @param updatedLoadingTextId
	 */
	public static void update(Activity activity, int loadingTextViewId, int updatedLoadingTextId) {
		final TextView loadingTextView = (TextView) activity.findViewById(loadingTextViewId);
		loadingTextView.setText(activity.getString(updatedLoadingTextId));
	}

	/**
	 * Dismiss the loading mechanism and display whatever was inside the
	 * activity_layout LinearLayout container.
	 * 
	 * @param activity
	 * @param loadingParentViewId
	 * @param subsequentViewId
	 */
	public static void dismiss(Activity activity, int loadingParentViewId, int subsequentViewId) {
		try {
			final LinearLayout loadingLayout = (LinearLayout) activity.findViewById(loadingParentViewId);
			loadingLayout.setVisibility(LinearLayout.GONE);
			final RelativeLayout activityView = (RelativeLayout) activity.findViewById(subsequentViewId);
			activityView.setVisibility(RelativeLayout.VISIBLE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Change from the loading mechanism to the failed to load display.
	 * 
	 * @param activity
	 * @param loadingViewId
	 * @param failedLoadViewId
	 */
	public static void failed(Activity activity, int loadingViewId, int failedLoadViewId) {
		RelativeLayout relativeLayout = (RelativeLayout) activity.findViewById(loadingViewId);
		relativeLayout.setVisibility(RelativeLayout.GONE);
		relativeLayout = (RelativeLayout) activity.findViewById(failedLoadViewId);
		relativeLayout.setVisibility(RelativeLayout.VISIBLE);
	}

	/**
	 * Change from the loading mechanism to the no display display.
	 * 
	 * @param activity
	 * @param loadingViewId
	 * @param noDisplayViewId
	 */
	public static void noDisplay(Activity activity, int loadingViewId, int noDisplayViewId) {
		RelativeLayout relativeLayout = (RelativeLayout) activity.findViewById(loadingViewId);
		relativeLayout.setVisibility(RelativeLayout.GONE);
		relativeLayout = (RelativeLayout) activity.findViewById(noDisplayViewId);
		relativeLayout.setVisibility(RelativeLayout.VISIBLE);
	}
}
