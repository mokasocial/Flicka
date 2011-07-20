package com.mokasocial.flicka;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.contacts.Contact;
import com.aetrion.flickr.contacts.ContactsInterface;

/**
 * This is fired off by the Alarm Manager which is set up within the
 * StatusbarNotify class. We can add more things to get fired off by the
 * notifier. We may need to make the firing class more sophisticated as time
 * goes on.
 * 
 * @date 2010.01.22
 */
public class NotifyReciever extends BroadcastReceiver {

	private Authorize mAuthorize;
	private Context mContext;
	private Intent mIntent;
	private static final int CONTACT_UPDATE = 1;

	// How far back should we look in seconds AND how often we check.
	// private static final long CONTACT_UPDATE_INTERVAL = 3600; // 1 hour
	private static final String CONTACT_UPDATE_SECTION = "contact_notify";
	private static final long CONTACT_UPDATE_LIMIT = 43200; // 12 hours

	// private static final long PHOTOS_UPDATE_INTERVAL = 3600000;
	// private static final String PHOTOS_UPDATE_SECTION= "photos_notify";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("Flicka", "Notify reciever started");
		mContext = context;
		mIntent = intent;

		try {
			mAuthorize = Authorize.initializeAuthObj(context);
			// Load the preferences
			PrefsMgmt mPrefsMgmt = new PrefsMgmt(context);
			mPrefsMgmt.restorePreferences();

			// Can we do a poll of the service?
			Collection<Contact> contacts = getContactsUpdate();

			// These will be shown by the ActivityNotifications
			saveContactsNotify(contacts);

			// Does the user want status bar notifications?
			if (mPrefsMgmt.isContactsNoticationsEnabled() == true) {
				performContactsNotify(contacts);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Do the actual checking of the contacts and form a string to display to
	 * the user.
	 * 
	 * @param context
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	@SuppressWarnings("unchecked")
	private Collection<Contact> getContactsUpdate() throws IOException, SAXException, FlickrException {
		ContactsInterface iface = mAuthorize.flickr.getContactsInterface();

		Long updateSince = System.currentTimeMillis() - (CONTACT_UPDATE_LIMIT * 1000);

		Collection<Contact> temp = iface.getListRecentlyUploaded(new Date(updateSince), null);

		// Only update the time if updates are returned
		if (temp.size() > 0) {
			setNotifyTime(CONTACT_UPDATE_SECTION);
		}

		Log.d("Flicka", "Ran getContactsUpdate. Got: " + temp.size());
		return temp;
	}

	private String createContactsUpdateMessage(Collection<Contact> contactsList) {
		int numUpdates = contactsList.size();
		if (numUpdates > 1) {
			String temp = (String) mContext.getText(R.string.cntcts_notif_mltpl_updates);
			return temp.replace(Flicka.PLACEHOLDER_INTEGER, Integer.toString(numUpdates));
		} else if (numUpdates == 1) {
			return (String) mContext.getText(R.string.cntcts_notif_sngl_update);
		}

		return null;
	}

	private void saveContactsNotify(Collection<Contact> contacts) {
		Log.d("Flicka", "saveContactsNotify running");
		if (contacts == null) {
			return;
		}

		Database dbObj = new Database(mContext);
		Object[] contactsArray = contacts.toArray();
		for (int i = 0; i < contactsArray.length; i++) {
			dbObj.addContactsNotify((Contact) contactsArray[i], "Uploaded some photos.");
		}
	}

	/**
	 * Build the notification based on the result of the contacts update call to
	 * Flickr and the settings.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	private void performContactsNotify(Collection<Contact> contacts) throws IOException, SAXException, FlickrException {
		if (contacts == null) {
			Log.d("Flicka", "Contacts update notification service ran but found null");
			return;
		}

		if (contacts.size() < 1) {
			Log.d("Flicka", "Contacts update notification service ran but 0 notifications to show");
			return;
		}

		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(ns);

		int icon = R.drawable.icon_status_bar_enabled;
		CharSequence tickerText = mContext.getText(R.string.app_name);
		long when = System.currentTimeMillis();
		CharSequence contentTitle = mContext.getText(R.string.cntcts_notif_title);
		CharSequence contentText = createContactsUpdateMessage(contacts);

		mIntent = new Intent(mContext, Flicka.class);

		int notificationFlags = PendingIntent.FLAG_UPDATE_CURRENT | Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;

		PendingIntent contentIntent = PendingIntent.getActivity(mContext, Flicka.BROADCAST_RQST_UPDATE_NOTIFICATIONS, mIntent, notificationFlags);

		// The next two lines initialize the Notification, using the
		// configurations above
		Notification notification = new Notification(icon, tickerText, when);
		notification.setLatestEventInfo(mContext, contentTitle, contentText, contentIntent);

		// The LED will flash green
		notification.ledARGB = 0xff00ff00;
		notification.ledOnMS = 300;
		notification.ledOffMS = 1000;
		notification.flags = notification.flags |= Notification.FLAG_AUTO_CANCEL;

		mNotificationManager.notify(CONTACT_UPDATE, notification);
		Log.d("Flicka", "Contacts update notification successful");
	}

	/**
	 * Set the time of update to now.
	 */
	private void setNotifyTime(String sectionName) {
		Database dbObj = new Database(mContext);
		dbObj.setUpdateTime(sectionName);
	}
}