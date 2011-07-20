package com.mokasocial.flicka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

import com.aetrion.flickr.contacts.Contact;
import com.aetrion.flickr.contacts.OnlineStatus;
import com.aetrion.flickr.groups.Group;
import com.aetrion.flickr.people.User;

public class Database {

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AUTHORIZE + " (" + COLUMN__ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_TOKEN + " TEXT, " + COLUMN_NSID + " TEXT, " + COLUMN_LAST_UPDATE + " INTEGER);");
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" + COLUMN_NSID + " TEXT PRIMARY KEY, " + COLUMN_LOCATION + " TEXT, " + COLUMN_BANDWIDTH_MAX + " INTEGER, " + COLUMN_BANDWIDTH_USED + " INTEGER, " + COLUMN_FAVORITE_DATE + " TEXT, " + COLUMN_FILE_MAXSIZE + " INTEGER, " + COLUMN_MBOX_SHA1SUM + " TEXT, " + COLUMN_PHOTO_COUNT + " INTEGER, " + COLUMN_PHOTOS_FIRSTDATE + " TEXT, " + COLUMN_PHOTOS_FIRSTDATE_TAKEN + " TEXT, " + COLUMN_HASH_CODE + " INTEGER, " + COLUMN_ADMIN_STATUS + " BOOLEAN, " + COLUMN_PRO_STATUS + " BOOLEAN, " + COLUMN_LAST_UPDATE + " INTEGER);");
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CONTACTS + " (" + COLUMN_NSID + " TEXT PRIMARY KEY, " + COLUMN_LOCATION + " TEXT, " + COLUMN_USERNAME + " TEXT, " + COLUMN_REALNAME + " TEXT, " + COLUMN_USERICON_SERVER + " INTEGER, " + COLUMN_USERICON_FARM + " INTEGER, " + COLUMN_AWAY_MESSAGE + " TEXT, " + COLUMN_ONLINE_STATUS + " INTEGER, " + COLUMN_FAMILY_STATUS + " BOOLEAN, " + COLUMN_FRIEND_STATUS + " BOOLEAN, " + COLUMN_IGNORE_STATUS + " BOOLEAN, " + COLUMN_LAST_UPDATE + " INTEGER);");
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CONTACT_PHOTO_UPLD_NTFY + " (" + COLUMN__ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_NSID + " TEXT, " + COLUMN_USERNAME + " TEXT, " + COLUMN_USERICON_SERVER + " INTEGER, " + COLUMN_USERICON_FARM + " INTEGER, " + COLUMN_REALNAME + " TEXT, " + COLUMN_PHOTOS_UPLOADED + " INTEGER, " + COLUMN_UPDATE_MSG + " TEXT, " + COLUMN_INSERT_TIME + " INTEGER);");
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_UPDATES + " (" + COLUMN_SECTION + " TEXT PRIMARY KEY, " + COLUMN_LAST_UPDATE + " INTEGER);");
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GROUPS + " (" + COLUMN_NSID + " TEXT PRIMARY KEY, " + COLUMN_USERICON_SERVER + " INTEGER, " + COLUMN_USERICON_FARM + " INTEGER, " + COLUMN_LANG + " TEXT, " + COLUMN_NAME + " TEXT, " + COLUMN_DESCRIPTION + " TEXT, " + COLUMN_MEMBERS + " INTEGER, " + COLUMN_PHOTO_COUNT + " INTEGER, " + COLUMN_PRIVACY + " STRING, " + COLUMN_ADMIN_STATUS + " BOOLEAN, " + COLUMN_LAST_UPDATE + " INTEGER);");
		}

		/**
		 * TODO This will house some spiffy logic to update tables rather than
		 * dropping and replacing them.
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// save authorize

			// drop tables
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUTHORIZE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACT_PHOTO_UPLD_NTFY);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_UPDATES);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);

			// reinsert tables
			onCreate(db);

			// reinsert authorize
		}
	}

	static final String COLUMN__ID = "_id";
	static final String COLUMN_ADMIN_STATUS = "admin_status";
	static final String COLUMN_AWAY_MESSAGE = "away_message";
	static final String COLUMN_BANDWIDTH_MAX = "bandwidth_max";
	static final String COLUMN_BANDWIDTH_USED = "bandwidth_used";
	static final String COLUMN_DESCRIPTION = "description";
	static final String COLUMN_FAMILY_STATUS = "family_status";
	static final String COLUMN_FAVORITE_DATE = "favorite_date";
	static final String COLUMN_FILE_MAXSIZE = "file_maxsize";
	static final String COLUMN_FRIEND_STATUS = "friend_status";
	static final String COLUMN_HASH_CODE = "hash_code";
	static final String COLUMN_IGNORE_STATUS = "ignore_status";
	static final String COLUMN_INSERT_TIME = "insert_time";
	static final String COLUMN_LANG = "language";
	static final String COLUMN_LAST_UPDATE = "last_update";
	static final String COLUMN_LOCATION = "location";
	static final String COLUMN_MBOX_SHA1SUM = "mbox_sha1sum";
	static final String COLUMN_MEMBERS = "members";
	static final String COLUMN_NAME = "name";
	static final String COLUMN_NSID = "nsid";
	static final String COLUMN_ONLINE_STATUS = "online_status";
	static final String COLUMN_PHOTO_COUNT = "photo_count";
	static final String COLUMN_PHOTOS_FIRSTDATE = "photos_firstdate";
	static final String COLUMN_PHOTOS_FIRSTDATE_TAKEN = "photos_firstdate_taken";
	static final String COLUMN_PHOTOS_UPLOADED = "photos_uploaded";
	static final String COLUMN_POOL_MOD_STATUS = "pool_mod_status";
	static final String COLUMN_PRIVACY = "privacy";
	static final String COLUMN_PRO_STATUS = "pro_status";
	static final String COLUMN_REALNAME = "realname";
	static final String COLUMN_SECTION = "section";
	static final String COLUMN_THROTTLE_MODE = "throttle";
	static final String COLUMN_TOKEN = "token";
	static final String COLUMN_UPDATE_MSG = "update_msg";
	static final String COLUMN_USERICON_FARM = "usericon_farm";
	static final String COLUMN_USERICON_SERVER = "usericon_server";
	static final String COLUMN_USERNAME = "username";

	private static final String DATABASE_NAME = "flicka.db";
	private static final int DATABASE_VERSION = 3;

	static final String MAP_KEY_CONTACT = "contact";
	static final String MAP_KEY_INSERT_TIME = "insert_time";
	static final String MAP_KEY_UPDATE_MSG = "update_msg";
	static final String TABLE_AUTHORIZE = "authorize";
	static final String TABLE_CONTACT_PHOTO_UPLD_NTFY = "contact_photo_upload_notify";
	static final String TABLE_CONTACTS = "contacts";
	static final String TABLE_GROUPS = "groups";
	static final String TABLE_UPDATES = "updates";
	static final String TABLE_USERS = "users";

	private final DatabaseHelper mOpenHelper;

	public Database(Context context) {
		mOpenHelper = new DatabaseHelper(context);
	}

	/**
	 * Add a contact deriving it from a user object. Now it is obvious but
	 * isFamily() and the likes aren't set inside user.
	 * 
	 * @param user
	 * @param context
	 */
	public void addContactDerivedFromUser(User user) {
		Contact contact = new Contact();
		contact.setAwayMessage(user.getAwayMessage());
		contact.setIconFarm(user.getIconFarm());
		contact.setIconServer(user.getIconServer());
		contact.setId(user.getId());
		contact.setLocation(user.getLocation());
		contact.setOnline(OnlineStatus.UNKNOWN);
		contact.setRealName(user.getRealName());
		contact.setUsername(user.getUsername());
		contact.setIgnored(user.isIgnored());
		contact.setFriend(user.isFriend());
		contact.setFamily(user.isFamily());

		ArrayList<Contact> contactList = new ArrayList<Contact>();
		contactList.add(contact);

		addContacts(contactList);
	}

	/**
	 * Add the user to the cache setting the update time to now().
	 * 
	 * @param user
	 */
	public void addUser(User user) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		final ContentValues values = new ContentValues();
		values.put(COLUMN_NSID, user.getId());
		values.put(COLUMN_LOCATION, user.getLocation());
		values.put(COLUMN_BANDWIDTH_MAX, user.getBandwidthMax());
		values.put(COLUMN_BANDWIDTH_USED, user.getBandwidthUsed());

		String faveDate = (user.getFaveDate() == null) ? null : user.getFaveDate().toGMTString();
		values.put(COLUMN_FAVORITE_DATE, faveDate);

		values.put(COLUMN_FILE_MAXSIZE, user.getFilesizeMax());
		values.put(COLUMN_MBOX_SHA1SUM, user.getMbox_sha1sum());
		values.put(COLUMN_PHOTO_COUNT, user.getPhotosCount());

		String photosFirstDate = (user.getPhotosFirstDate() == null) ? null : user.getPhotosFirstDate().toGMTString();
		values.put(COLUMN_PHOTOS_FIRSTDATE, photosFirstDate);

		String photosFirstDateTaken = (user.getPhotosFirstDateTaken() == null) ? null : user.getPhotosFirstDateTaken().toGMTString();
		values.put(COLUMN_PHOTOS_FIRSTDATE_TAKEN, photosFirstDateTaken);

		values.put(COLUMN_HASH_CODE, user.hashCode());
		values.put(COLUMN_ADMIN_STATUS, user.isAdmin());
		values.put(COLUMN_PRO_STATUS, user.isPro());
		values.put(COLUMN_LAST_UPDATE, System.currentTimeMillis());

		db.replace(TABLE_USERS, null, values);
		db.close();
	}

	/**
	 * Get the specified user. We store user info in two places to save on
	 * space. Not sure if this is the best approach but since a lot of the data
	 * was the same...
	 * 
	 * @param nsid
	 * @return User object
	 */
	public User getUser(String nsid) {

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		User user = new User();
		// Use QueryBuilder to build the query since we're doing a join.
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(TABLE_USERS + " LEFT OUTER JOIN " + TABLE_CONTACTS + " ON (" + TABLE_USERS + "." + COLUMN_NSID + " = " + TABLE_CONTACTS + "." + COLUMN_NSID + ")");

		Cursor result = qb.query(db, null, TABLE_USERS + "." + COLUMN_NSID + " = '" + nsid + "'", null, null, null, null);

		// No rows found
		if (result.getCount() < 1) {
			db.close();
			result.close();
			return null;
		}

		result.moveToFirst();
		// This first section is building the user
		user.setId(result.getString(result.getColumnIndexOrThrow(COLUMN_NSID)));
		user.setLocation(result.getString(result.getColumnIndexOrThrow(COLUMN_LOCATION)));
		user.setBandwidthMax(result.getLong(result.getColumnIndexOrThrow(COLUMN_BANDWIDTH_MAX)));
		user.setBandwidthUsed(result.getLong(result.getColumnIndexOrThrow(COLUMN_BANDWIDTH_USED)));

		try {
			Date faveDate = new Date(Date.parse(result.getString(result.getColumnIndexOrThrow(COLUMN_FAVORITE_DATE))));
			user.setFaveDate(faveDate);
		} catch (Exception e) {
			e.printStackTrace();
		}

		user.setFilesizeMax(result.getLong(result.getColumnIndexOrThrow(COLUMN_FILE_MAXSIZE)));
		user.setMbox_sha1sum(result.getString(result.getColumnIndexOrThrow(COLUMN_MBOX_SHA1SUM)));
		user.setPhotosCount(result.getInt(result.getColumnIndexOrThrow(COLUMN_PHOTO_COUNT)));

		try {
			Date photosFirstDate = new Date(Date.parse(result.getString(result.getColumnIndexOrThrow(COLUMN_PHOTOS_FIRSTDATE))));
			user.setPhotosFirstDate(photosFirstDate);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Date photosFirstDateTaken = new Date(Date.parse(result.getString(result.getColumnIndexOrThrow(COLUMN_PHOTOS_FIRSTDATE_TAKEN))));
			user.setPhotosFirstDateTaken(photosFirstDateTaken);
		} catch (Exception e) {
			e.printStackTrace();
		}

		boolean isAdmin = (1 == result.getInt(result.getColumnIndexOrThrow(COLUMN_ADMIN_STATUS)));
		user.setAdmin(isAdmin);
		boolean isPro = (1 == result.getInt(result.getColumnIndexOrThrow(COLUMN_PRO_STATUS)));
		user.setPro(isPro);

		// Now set the parts of the user that are in the contacts table
		Contact contact = new Contact();
		contact = loadContactFromCursor(contact, result);
		user.setAwayMessage(contact.getAwayMessage());
		user.setIconFarm(contact.getIconFarm());
		user.setIconServer(contact.getIconServer());
		user.setUsername(contact.getUsername());
		user.setRealName(contact.getRealName());
		user.setOnline(contact.getOnline());

		db.close();
		result.close();
		return user;
	}

	/**
	 * Get the last update time for the specified user.
	 * 
	 * @see Flicka.CACHED_USER_LIMIT
	 * @param nsid
	 * @return
	 */
	public long getUserUpdateTime(String nsid) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor result = db.query(TABLE_USERS, new String[] { COLUMN_LAST_UPDATE }, COLUMN_NSID + " = '" + nsid + "'", null, null, null, null);

		if (result.getCount() < 1) {
			db.close();
			result.close();
			return 0;
		}

		long lastUpdateTime = 0L;
		if (result.moveToFirst()) {
			lastUpdateTime = result.getLong(result.getColumnIndexOrThrow(COLUMN_LAST_UPDATE));
		} else {
			// cursor was empty, keep it 0
			lastUpdateTime = 0L;
		}

		result.close();
		db.close();

		return lastUpdateTime;
	}

	/**
	 * Add a user authorization to the authorize table. This is a simple insert
	 * and will put the entry to the last row hence the use of getLast... etc
	 * functions.
	 * 
	 * @param token
	 * @param nsid
	 * @return long val last row inserted or -1 on failure
	 */
	public long addAuth(String token, String nsid) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final ContentValues values = new ContentValues();
		values.put(COLUMN_TOKEN, token);
		values.put(COLUMN_NSID, nsid);
		values.put(COLUMN_LAST_UPDATE, System.currentTimeMillis());
		long val = db.insert(TABLE_AUTHORIZE, null, values);

		db.close();

		return val;
	}

	public void addContacts(ArrayList<Contact> contacts) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		for (Contact contact : contacts) {
			final ContentValues values = new ContentValues();
			values.put(COLUMN_NSID, contact.getId());
			values.put(COLUMN_LOCATION, contact.getLocation());
			values.put(COLUMN_USERNAME, contact.getUsername());
			values.put(COLUMN_REALNAME, contact.getRealName());
			values.put(COLUMN_USERICON_SERVER, contact.getIconServer());
			values.put(COLUMN_USERICON_FARM, contact.getIconFarm());
			values.put(COLUMN_AWAY_MESSAGE, contact.getAwayMessage());

			OnlineStatus onlineStatus = contact.getOnline();
			values.put(COLUMN_ONLINE_STATUS, onlineStatus.getType());
			// values.put(COLUMN_ONLINE_STATUS, 0);

			values.put(COLUMN_FAMILY_STATUS, contact.isFamily());
			values.put(COLUMN_FRIEND_STATUS, contact.isFriend());
			values.put(COLUMN_IGNORE_STATUS, contact.isIgnored());
			values.put(COLUMN_LAST_UPDATE, System.currentTimeMillis());

			db.replace(TABLE_CONTACTS, null, values);
		}
		db.close();
	}

	public void addContactsNotify(Contact contact, String msg) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		final ContentValues values = new ContentValues();
		values.put(COLUMN_NSID, contact.getId());
		values.put(COLUMN_USERNAME, contact.getUsername());
		values.put(COLUMN_USERICON_SERVER, contact.getIconServer());
		values.put(COLUMN_USERICON_FARM, contact.getIconFarm());
		values.put(COLUMN_REALNAME, contact.getRealName());
		values.put(COLUMN_PHOTOS_UPLOADED, contact.getPhotosUploaded());
		values.put(COLUMN_UPDATE_MSG, msg);
		values.put(COLUMN_INSERT_TIME, System.currentTimeMillis());
		db.insert(TABLE_CONTACT_PHOTO_UPLD_NTFY, null, values);

		db.close();
	}

	public void addGroup(Group group) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		final ContentValues values = new ContentValues();
		values.put(COLUMN_NSID, group.getId());
		values.put(COLUMN_USERICON_SERVER, group.getIconServer());
		values.put(COLUMN_USERICON_FARM, group.getIconFarm());
		values.put(COLUMN_LANG, group.getLang());
		values.put(COLUMN_NAME, group.getName());
		values.put(COLUMN_DESCRIPTION, group.getDescription());
		values.put(COLUMN_MEMBERS, group.getMembers());
		values.put(COLUMN_PHOTO_COUNT, group.getPhotoCount());
		values.put(COLUMN_PRIVACY, group.getPrivacy());
		values.put(COLUMN_ADMIN_STATUS, group.isAdmin());
		values.put(COLUMN_LAST_UPDATE, System.currentTimeMillis());

		db.replace(TABLE_GROUPS, null, values);
		db.close();
	}

	public void flushAuthTable() {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.execSQL("DELETE FROM " + TABLE_AUTHORIZE);
		db.close();
	}

	public void flushContactsNotifyTable() {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.execSQL("DELETE FROM " + TABLE_CONTACT_PHOTO_UPLD_NTFY);
		db.close();
	}

	public void flushContactsTable() {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.execSQL("DELETE FROM " + TABLE_CONTACT_PHOTO_UPLD_NTFY);
		db.close();
	}

	public void flushGroupsTable() {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.execSQL("DELETE FROM " + TABLE_CONTACT_PHOTO_UPLD_NTFY);
		db.close();
	}

	public void flushUpdatesTable() {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.execSQL("DELETE FROM " + TABLE_CONTACT_PHOTO_UPLD_NTFY);
		db.close();
	}

	public void flushAllTables() {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.execSQL("DELETE FROM " + TABLE_AUTHORIZE);
		db.execSQL("DELETE FROM " + TABLE_CONTACT_PHOTO_UPLD_NTFY);
		db.execSQL("DELETE FROM " + TABLE_CONTACTS);
		db.execSQL("DELETE FROM " + TABLE_GROUPS);
		db.execSQL("DELETE FROM " + TABLE_UPDATES);
		db.execSQL("DELETE FROM " + TABLE_USERS);
		db.close();
	}

	public void flushAllCacheTables() {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.execSQL("DELETE FROM " + TABLE_CONTACT_PHOTO_UPLD_NTFY);
		db.execSQL("DELETE FROM " + TABLE_CONTACTS);
		db.execSQL("DELETE FROM " + TABLE_GROUPS);
		db.execSQL("DELETE FROM " + TABLE_UPDATES);
		db.execSQL("DELETE FROM " + TABLE_USERS);
		db.close();
	}

	// /////////////////////////////////////////////////////////////////////////////////////////
	// NOTIFICATIONS TABLE FUNCTIONS
	// /////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Get a single, specific contact by NSID.
	 * 
	 * @param nsid
	 * @return
	 */
	public Contact getContact(String nsid) {
		Contact contact = new Contact();

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		Cursor result = db.query(TABLE_CONTACTS, null, TABLE_CONTACTS + "." + COLUMN_NSID + " = '" + nsid + "'", null, null, null, null);

		if (result.getCount() < 1) {
			result.close();
			return null;
		}

		result.moveToFirst();
		contact = loadContactFromCursor(contact, result);
		result.close();
		db.close();
		return contact;

	}

	public Collection<Contact> getContacts() {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Collection<Contact> contacts = new ArrayList<Contact>();

		// Grab all rows from the table
		Cursor result = db.query(TABLE_CONTACTS, null, null, null, null, null, null);

		// No result, no rows to worry about.
		if (result.getCount() < 1) {
			result.close();
			return null;
		}

		// Reset the cursor
		result.moveToFirst();
		while (!result.isAfterLast()) {
			Contact contact = new Contact();
			contact = loadContactFromCursor(contact, result);

			// Add the contact to this collection.
			contacts.add(contact);
			result.moveToNext();
		}

		result.close();
		db.close();
		return contacts;
	}

	public ArrayList<Map<String, Object>> getContactsNotify() {
		Cursor result = null;
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		try {
			result = db.query(TABLE_CONTACT_PHOTO_UPLD_NTFY, null, null, null, null, null, COLUMN_INSERT_TIME + " DESC", null);
			result.moveToFirst();

			ArrayList<Map<String, Object>> temp = new ArrayList<Map<String, Object>>();
			while (!result.isAfterLast()) {
				Contact contact = new Contact();
				contact.setId(result.getString(result.getColumnIndexOrThrow(COLUMN_NSID)));
				contact.setUsername(result.getString(result.getColumnIndexOrThrow(COLUMN_USERNAME)));
				contact.setIconServer(result.getString(result.getColumnIndexOrThrow(COLUMN_USERICON_SERVER)));
				contact.setIconFarm(result.getString(result.getColumnIndexOrThrow(COLUMN_USERICON_FARM)));
				contact.setRealName(result.getString(result.getColumnIndexOrThrow(COLUMN_REALNAME)));
				contact.setPhotosUploaded(result.getString(result.getColumnIndexOrThrow(COLUMN_PHOTOS_UPLOADED)));

				Map<String, Object> notifyMap = new HashMap<String, Object>();
				notifyMap.put(MAP_KEY_CONTACT, contact);
				notifyMap.put(COLUMN__ID, result.getInt(result.getColumnIndexOrThrow(COLUMN__ID)));
				notifyMap.put(MAP_KEY_UPDATE_MSG, result.getString(result.getColumnIndexOrThrow(COLUMN_UPDATE_MSG)));

				temp.add(notifyMap);
				result.moveToNext();
			}

			result.close();
			return temp;
		} catch (Exception e) {
			e.printStackTrace();
			if (result != null) {
				result.close();
			}
		}

		db.close();

		return null;
	}

	// /////////////////////////////////////////////////////////////////////////////////////////
	// UPDATES TABLE FUNCTIONS
	// /////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Get a single, specific group by NSID.
	 * 
	 * @param nsid
	 * @return
	 */
	public Group getGroup(String nsid) {
		Group group = new Group();
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		Cursor result = db.query(TABLE_GROUPS, null, TABLE_GROUPS + "." + COLUMN_NSID + " = '" + nsid + "'", null, null, null, null);

		if (result.getCount() < 1) {
			result.close();
			return null;
		}

		result.moveToFirst();
		group = loadGroupFromCursor(group, result);
		result.close();
		db.close();
		return group;
	}

	public Collection<Group> getGroups() {
		Collection<Group> groups = new ArrayList<Group>();
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		// Grab all rows from the table
		Cursor result = db.query(TABLE_GROUPS, null, null, null, null, null, null);

		// No result, no rows to worry about.
		if (result.getCount() < 1) {
			result.close();
			return null;
		}

		// Reset the cursor
		result.moveToFirst();
		while (!result.isAfterLast()) {
			Group group = new Group();
			group = loadGroupFromCursor(group, result);

			// Add the contact to this collection.
			groups.add(group);
			result.moveToNext();
		}

		result.close();
		db.close();
		return groups;
	}

	/**
	 * Get the last update time for the specified user.
	 * 
	 * @see Flicka.CACHED_USER_LIMIT
	 * @param nsid
	 * @return
	 */
	public long getGroupUpdateTime(String nsid) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Cursor result = db.query(TABLE_GROUPS, new String[] { COLUMN_LAST_UPDATE }, COLUMN_NSID + " = '" + nsid + "'", null, null, null, null);

		if (result.getCount() < 1) {
			result.close();
			return 0;
		}

		result.moveToFirst();
		long lastUpdateTime = result.getLong(result.getColumnIndexOrThrow(COLUMN_LAST_UPDATE));
		result.close();
		db.close();

		return lastUpdateTime;
	}

	/**
	 * Query the database for the last row and grab the NSID. This can perhaps
	 * be used for users that have a token but its not authenticated.
	 * 
	 * @return nsid
	 */
	public String getLastAuthNsid() {

		String nsid = "";
		Cursor result = null;

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		try {
			result = db.query(TABLE_AUTHORIZE, new String[] { COLUMN_NSID }, null, null, null, null, COLUMN_LAST_UPDATE + " DESC", "1");

			if (result.moveToFirst()) {
				int resultIndex = result.getColumnIndexOrThrow(COLUMN_NSID);
				nsid = result.getString(resultIndex);
				result.close();
				return nsid;
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (result != null) {
				result.close();
			}
		}

		db.close();
		return null;
	}

	/**
	 * Query the database for the last row and grab the token. This token
	 * doesn't guarantee an authenticated session and will need to be checked in
	 * Flickrj.AuthInterface.checkToken(token)
	 * 
	 * @return token
	 */
	public String getLastAuthToken() {

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		String token = "";
		Cursor result = null;

		try {
			result = db.query(TABLE_AUTHORIZE, new String[] { COLUMN_TOKEN }, null, null, null, null, COLUMN_LAST_UPDATE + " DESC", "1");
			result.moveToFirst();
			token = result.getString(result.getColumnIndexOrThrow(COLUMN_TOKEN));
			result.close();
			return token;

		} catch (Exception e) {
			e.printStackTrace();
			if (result != null) {
				result.close();
			}
			db.close();
			return null;
		}

	}

	/**
	 * The the last update time for the specified section. If there is no
	 * record, this function will return 0.
	 * 
	 * @param section
	 * @return
	 */
	public long getUpdateTime(String section) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Cursor result = db.query(TABLE_UPDATES, new String[] { COLUMN_LAST_UPDATE }, COLUMN_SECTION + " = '" + section + "'", null, null, null, null);

		if (result.getCount() < 1) {
			result.close();
			return 0;
		}

		result.moveToFirst();
		long lastUpdateTime = result.getLong(result.getColumnIndexOrThrow(COLUMN_LAST_UPDATE));
		result.close();
		db.close();
		return lastUpdateTime;
	}

	/**
	 * Build the actual contact object using the cursor result set.
	 * 
	 * @param contact
	 * @param result
	 * @return
	 */
	private Contact loadContactFromCursor(Contact contact, Cursor result) {
		contact.setId(result.getString(result.getColumnIndexOrThrow(COLUMN_NSID)));
		contact.setLocation(result.getString(result.getColumnIndexOrThrow(COLUMN_LOCATION)));
		contact.setUsername(result.getString(result.getColumnIndexOrThrow(COLUMN_USERNAME)));
		contact.setRealName(result.getString(result.getColumnIndexOrThrow(COLUMN_REALNAME)));
		contact.setIconServer(result.getInt(result.getColumnIndexOrThrow(COLUMN_USERICON_SERVER)));
		contact.setIconFarm(result.getInt(result.getColumnIndexOrThrow(COLUMN_USERICON_FARM)));
		int savedOnlineStatus = result.getInt(result.getColumnIndexOrThrow(COLUMN_ONLINE_STATUS));
		contact.setOnline(OnlineStatus.fromType(savedOnlineStatus));
		contact.setAwayMessage(result.getString(result.getColumnIndexOrThrow(COLUMN_AWAY_MESSAGE)));

		boolean isFamily = (1 == result.getInt(result.getColumnIndexOrThrow(COLUMN_FAMILY_STATUS)));
		contact.setFamily(isFamily);
		boolean isFriend = (1 == result.getInt(result.getColumnIndexOrThrow(COLUMN_FRIEND_STATUS)));
		contact.setFriend(isFriend);
		boolean isIgnored = (1 == result.getInt(result.getColumnIndexOrThrow(COLUMN_FAMILY_STATUS)));
		contact.setIgnored(isIgnored);

		return contact;
	}

	/**
	 * Build the actual group object using the cursor result set.
	 * 
	 * @param contact
	 * @param result
	 * @return
	 */
	private Group loadGroupFromCursor(Group group, Cursor result) {
		group.setId(result.getString(result.getColumnIndexOrThrow(COLUMN_NSID)));
		group.setIconServer(result.getInt(result.getColumnIndexOrThrow(COLUMN_USERICON_SERVER)));
		group.setIconFarm(result.getInt(result.getColumnIndexOrThrow(COLUMN_USERICON_FARM)));
		group.setLang(result.getString(result.getColumnIndexOrThrow(COLUMN_LANG)));
		group.setName(result.getString(result.getColumnIndexOrThrow(COLUMN_NAME)));
		group.setDescription(result.getString(result.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
		group.setMembers(result.getInt(result.getColumnIndexOrThrow(COLUMN_MEMBERS)));
		group.setPhotoCount(result.getInt(result.getColumnIndexOrThrow(COLUMN_PHOTO_COUNT)));
		group.setPrivacy(result.getString(result.getColumnIndexOrThrow(COLUMN_PRIVACY)));
		boolean isAdmin = (1 == result.getInt(result.getColumnIndexOrThrow(COLUMN_ADMIN_STATUS)));
		group.setAdmin(isAdmin);

		return group;
	}

	/**
	 * Insert a new record or update the existing one with an updated current
	 * time.
	 * 
	 * @param section
	 */
	public void setUpdateTime(String section) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		final ContentValues values = new ContentValues();
		values.put(COLUMN_SECTION, section);
		values.put(COLUMN_LAST_UPDATE, System.currentTimeMillis() / 1000L);

		db.replace(TABLE_UPDATES, null, values);
		db.close();
	}
}
