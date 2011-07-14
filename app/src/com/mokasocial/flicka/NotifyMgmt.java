package com.mokasocial.flicka;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * This class sets up the notifications and relies on the PrefsMgmt class to
 * load preferences in the receiver to handle which preferences to show. I
 * suppose eventually we should cancel the AlarmManager to prevent needless
 * running if all settings are set to disabled.
 * 
 * @date 2010.01.22
 */
public class NotifyMgmt {
	public NotifyMgmt() {
	}

	public static String JUMP_TO_NOTIFICATIONS = "jump_to_notifications";

	/**
	 * Start the notification alarm service to run in the background to alert
	 * the user of updates. We're using a very soft and battery friendly
	 * approach to notifying the user.
	 * 
	 * @param context
	 */
	public static void initNotifications(Context context) {
		NotifyMgmt statusbarNotifyObj = NotifyMgmt.createStatusbarNotify();
		try {
			Utilities.debugLog(statusbarNotifyObj, "Initializing the notifications service");

			Calendar calendar = Calendar.getInstance();

			// Prepare the intent for the receiver
			Intent intent = new Intent(context, NotifyReciever.class);
			intent.putExtra(NotifyMgmt.JUMP_TO_NOTIFICATIONS, true);
			PendingIntent sender = PendingIntent.getBroadcast(context, Flicka.BROADCAST_RQST_UPDATE_NOTIFICATIONS, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			// Set up the service itself
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

			// Set up the repeating so this fires off once an hour or whatever.
			// Do not wake phone
			alarmManager.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), AlarmManager.INTERVAL_HALF_DAY, sender);
		} catch (Exception e) {
			Utilities.errorOccurred(statusbarNotifyObj, "Unable to initialize notifications", e);
		}
	}

	/**
	 * Cancel the notification alarm service running in the background.
	 * 
	 * @param context
	 */
	public static void cancelNotifications(Context context) {
		NotifyMgmt statusbarNotifyObj = NotifyMgmt.createStatusbarNotify();
		try {
			Intent intent = new Intent(context, NotifyReciever.class);
			PendingIntent sender = PendingIntent.getBroadcast(context, Flicka.BROADCAST_RQST_UPDATE_NOTIFICATIONS, intent, 0);
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Flicka.ALARM_SERVICE);
			alarmManager.cancel(sender);
		} catch (Exception e) {
			Utilities.errorOccurred(statusbarNotifyObj, "Unable to cancel notifications", e);
		}
	}

	/**
	 * Simple static object creator for use with debugging and other places
	 * where 'this' is required.
	 * 
	 * @return
	 */
	private static NotifyMgmt createStatusbarNotify() {
		return new NotifyMgmt();
	}
}
