package com.mokasocial.flicka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.aetrion.flickr.contacts.Contact;
import com.aetrion.flickr.contacts.OnlineStatus;
import com.aetrion.flickr.groups.Group;
import com.aetrion.flickr.people.User;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

/**
 * The Flicka Database class handle all DB interactivity. It follows the onCreate/onUpdate
 * override paradigm. The DB for Flicka will likely have several tables. The first being the
 * user table. The rest will have things pertaining to caching and following event feeds
 * from Flickr.
 * 
 * A note about the user/contacts tables: The user is conceived as an object that inherits
 * from a Contact. This isn't the case in the FlickrJ library but by doing it this way
 * we will save on space. The table join should be negligible.
 * 
 * There is something wonky about how Flickr gives us info. It gives us a list of contacts
 * without photo counts but then gives that info to us when we get info about a particular user.
 * This create difficulty when trying to show info consistently without having to do joins.
 * 
 * We'll continue using seperate Contact and User tables for now.
 * 
 * @author Michael Hradek mhradek@gmail.com, mhradek@mokasocial.com, mhradek@flicka.mobi
 * @date 2009.11.04
 */

public class Database {

	/** GLOBAL DATABASE DEFINITIONS */
	private static final String DATABASE_NAME = "flicka.db";
	private static final int DATABASE_VERSION = 2;

	/** TABLE DEFINITIONS */
	static final String TABLE_AUTHORIZE = "authorize";
	static final String TABLE_CONTACTS = "contacts";
	static final String TABLE_UPDATES = "updates";
	static final String TABLE_USERS = "users";
	static final String TABLE_CONTACT_PHOTO_UPLD_NTFY = "contact_photo_upload_notify";
	static final String TABLE_GROUPS = "groups";

	/** COLUMN DEFINITIONS */
	// AUTHORIZE
	static final String COLUMN__ID = "_id";
	static final String COLUMN_TOKEN = "token";
	static final String COLUMN_NSID = "nsid";
	static final String COLUMN_LAST_UPDATE = "last_update";
	// CONTACTS
	static final String COLUMN_USERNAME = "username";
	static final String COLUMN_REALNAME = "realname";
	static final String COLUMN_USERICON_SERVER = "usericon_server";
	static final String COLUMN_USERICON_FARM = "usericon_farm";
	static final String COLUMN_AWAY_MESSAGE = "away_message";
	static final String COLUMN_ONLINE_STATUS = "online_status";
	static final String COLUMN_FAMILY_STATUS = "family_status";
	static final String COLUMN_FRIEND_STATUS = "friend_status";
	static final String COLUMN_IGNORE_STATUS = "ignore_status";
	// UPDATES
	static final String COLUMN_SECTION = "section";
	// USERS
	static final String COLUMN_LOCATION = "location";
	static final String COLUMN_BANDWIDTH_MAX = "bandwidth_max";
	static final String COLUMN_BANDWIDTH_USED = "bandwidth_used";
	static final String COLUMN_FAVORITE_DATE = "favorite_date";
	static final String COLUMN_FILE_MAXSIZE = "file_maxsize";
	static final String COLUMN_MBOX_SHA1SUM = "mbox_sha1sum";
	static final String COLUMN_PHOTO_COUNT = "photo_count";
	static final String COLUMN_PHOTOS_FIRSTDATE = "photos_firstdate";
	static final String COLUMN_PHOTOS_FIRSTDATE_TAKEN = "photos_firstdate_taken";
	static final String COLUMN_HASH_CODE = "hash_code";
	static final String COLUMN_ADMIN_STATUS = "admin_status";
	static final String COLUMN_PRO_STATUS = "pro_status";
	// GROUPS
	static final String COLUMN_DESCRIPTION = "description";
	static final String COLUMN_LANG = "language";
	static final String COLUMN_MEMBERS = "members";
	static final String COLUMN_NAME = "name";
	static final String COLUMN_PRIVACY = "privacy";
	static final String COLUMN_THROTTLE_MODE = "throttle";
	static final String COLUMN_POOL_MOD_STATUS = "pool_mod_status";
	// CONTACT PHOTO UPLOAD NOTIFY
	static final String COLUMN_PHOTOS_UPLOADED = "photos_uploaded";
	static final String COLUMN_UPDATE_MSG = "update_msg";
	static final String COLUMN_INSERT_TIME = "insert_time";

	static final String MAP_KEY_CONTACT = "contact";
	static final String MAP_KEY_INSERT_TIME = "insert_time";
	static final String MAP_KEY_UPDATE_MSG = "update_msg";

	/** CONTEXT DEFINITION */
	private final Context mContext;
	private final DatabaseHelper DBHelper;
	private SQLiteDatabase db;

	public Database(Context context) {
		mContext = context;
		DBHelper = new DatabaseHelper(mContext);
	}

	public class DatabaseHelper extends SQLiteOpenHelper {

		///////////////////////////////////////////////////////////////////////////////////////////
		// CONSTRUCTOR
		///////////////////////////////////////////////////////////////////////////////////////////

		/**
		 * The Flicka DB constructor.
		 * 
		 * @param Context context
		 */
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		///////////////////////////////////////////////////////////////////////////////////////////
		// ANDROID DB OVERRIDE FUNCTIONS
		///////////////////////////////////////////////////////////////////////////////////////////

		/**
		 * Perform this when the application is created. For now we house the SQL here.
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			createTableAuthorize(db);
			createTableUsers(db);
			createTableContacts(db);
			createTableContactNotify(db);
			createTableUpdates(db);
			createTableGroups(db);
		}

		/**
		 * Perform this when an upgrade to the application DB is detected.
		 * 
		 * TODO This will house some spiffy logic to update tables rather than dropping and
		 * replacing them.
		 * 
		 * @see http://www.unwesen.de/articles/android-development-database-upgrades
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if(newVersion > oldVersion) {
				db.beginTransaction();
				boolean success = true;

				for (int i = oldVersion ; i < newVersion ; ++i) {
					int nextVersion = i + 1;
					switch (nextVersion) {
					case 2:
						success = upgradeToVersion2(db);
						break;
						// etc. for later versions.
					}

					if(!success) {
						break;
					}
				}

				if(success) {
					db.setTransactionSuccessful();
				}

				db.endTransaction();
			}
			else {
				clearDatabase(db);
				onCreate(db);
			}
		}
	}

	private void createTableAuthorize(SQLiteDatabase db) {
		try {
			// Create the user table. This houses the token and nsid for use throughout the app.
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AUTHORIZE + " ("
					+ COLUMN__ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ COLUMN_TOKEN + " TEXT, "
					+ COLUMN_NSID + " TEXT, "
					+ COLUMN_LAST_UPDATE + " INTEGER);");
		} catch (SQLException e) {
			Utilities.errorOccurred(this, "Unable to create the " + TABLE_AUTHORIZE + " table.", e);
			throw e;
		}
	}

	private void createTableUsers(SQLiteDatabase db) {
		try {
			// Create the contacts table. This houses the "cache" of contacts data.
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " ("
					+ COLUMN_NSID + " TEXT PRIMARY KEY, "
					+ COLUMN_LOCATION + " TEXT, "
					+ COLUMN_BANDWIDTH_MAX + " INTEGER, "
					+ COLUMN_BANDWIDTH_USED + " INTEGER, "
					+ COLUMN_FAVORITE_DATE + " TEXT, "
					+ COLUMN_FILE_MAXSIZE + " INTEGER, "
					+ COLUMN_MBOX_SHA1SUM + " TEXT, "
					+ COLUMN_PHOTO_COUNT + " INTEGER, "
					+ COLUMN_PHOTOS_FIRSTDATE + " TEXT, "
					+ COLUMN_PHOTOS_FIRSTDATE_TAKEN + " TEXT, "
					+ COLUMN_HASH_CODE + " INTEGER, "
					+ COLUMN_ADMIN_STATUS + " BOOLEAN, "
					+ COLUMN_PRO_STATUS + " BOOLEAN, "
					+ COLUMN_LAST_UPDATE + " INTEGER);");

		} catch (SQLException e) {
			Utilities.errorOccurred(this, "Unable to create the " + TABLE_USERS + " table.", e);
			throw e;
		}
	}

	private void createTableContacts(SQLiteDatabase db) {
		try {
			// Create the users table. This houses the "cache" of user data.
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CONTACTS + " ("
					+ COLUMN_NSID + " TEXT PRIMARY KEY, "
					+ COLUMN_LOCATION + " TEXT, "
					+ COLUMN_USERNAME + " TEXT, "
					+ COLUMN_REALNAME + " TEXT, "
					+ COLUMN_USERICON_SERVER + " INTEGER, "
					+ COLUMN_USERICON_FARM + " INTEGER, "
					+ COLUMN_AWAY_MESSAGE + " TEXT, "
					+ COLUMN_ONLINE_STATUS + " INTEGER, "
					+ COLUMN_FAMILY_STATUS + " BOOLEAN, "
					+ COLUMN_FRIEND_STATUS + " BOOLEAN, "
					+ COLUMN_IGNORE_STATUS + " BOOLEAN, "
					+ COLUMN_LAST_UPDATE + " INTEGER);");

		} catch (SQLException e) {
			Utilities.errorOccurred(this, "Unable to create the " + TABLE_CONTACTS + " table.", e);
			throw e;
		}
	}

	private void createTableContactNotify(SQLiteDatabase db) {
		try {
			// Create the contact photo upload notify table.
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CONTACT_PHOTO_UPLD_NTFY + " ("
					+ COLUMN__ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ COLUMN_NSID + " TEXT, "
					+ COLUMN_USERNAME + " TEXT, "
					+ COLUMN_USERICON_SERVER + " INTEGER, "
					+ COLUMN_USERICON_FARM + " INTEGER, "
					+ COLUMN_REALNAME + " TEXT, "
					+ COLUMN_PHOTOS_UPLOADED + " INTEGER, "
					+ COLUMN_UPDATE_MSG + " TEXT, "
					+ COLUMN_INSERT_TIME + " INTEGER);");
		} catch (SQLException e) {
			Utilities.errorOccurred(this, "Unable to create the " + TABLE_CONTACT_PHOTO_UPLD_NTFY + " table.", e);
			throw e;
		}
	}

	private void createTableUpdates(SQLiteDatabase db) {
		try {
			// Create the updates table. This houses the last cache times for the sections or activities.
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_UPDATES + " ("
					+ COLUMN_SECTION + " TEXT PRIMARY KEY, "
					+ COLUMN_LAST_UPDATE + " INTEGER);");
		} catch (SQLException e) {
			Utilities.errorOccurred(this, "Unable to create the " + TABLE_UPDATES + " table.", e);
			throw e;
		}
	}

	private void createTableGroups(SQLiteDatabase db) {
		try {
			// Create the groups table.
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GROUPS + " ("
					+ COLUMN_NSID + " TEXT PRIMARY KEY, "
					+ COLUMN_USERICON_SERVER + " INTEGER, "
					+ COLUMN_USERICON_FARM + " INTEGER, "
					+ COLUMN_LANG + " TEXT, "
					+ COLUMN_NAME + " TEXT, "
					+ COLUMN_DESCRIPTION + " TEXT, "
					+ COLUMN_MEMBERS + " INTEGER, "
					+ COLUMN_PHOTO_COUNT + " INTEGER, "
					+ COLUMN_PRIVACY + " STRING, "
					+ COLUMN_ADMIN_STATUS + " BOOLEAN, "
					+ COLUMN_LAST_UPDATE + " INTEGER);");
		} catch (SQLException e) {
			Utilities.errorOccurred(this, "Unable to create the " + TABLE_GROUPS + " table.", e);
			throw e;
		}
	}

	private boolean upgradeToVersion2(SQLiteDatabase db) {
		try {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_UPDATES);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACT_PHOTO_UPLD_NTFY);
			createTableContactNotify(db);
			createTableUpdates(db);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void clearDatabase(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUTHORIZE);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_UPDATES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACT_PHOTO_UPLD_NTFY);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
	}

	/**
	 * 
	 */
	public void flushAllTables() {
		db.execSQL("DELETE FROM " + TABLE_AUTHORIZE);
		db.execSQL("DELETE FROM " + TABLE_CONTACTS);
		db.execSQL("DELETE FROM " + TABLE_UPDATES);
		db.execSQL("DELETE FROM " + TABLE_CONTACT_PHOTO_UPLD_NTFY);
		db.execSQL("DELETE FROM " + TABLE_USERS);
		db.execSQL("DELETE FROM " + TABLE_GROUPS);
	}

	/**
	 * 
	 * @param context
	 */
	public static void flushAllTable(Context context) {
		Database dbObj = new Database(context);
		dbObj.open();
		dbObj.flushAllTables();
		dbObj.close();
	}

	/**
	 * 
	 */
	public void flushAllCacheTables() {
		db.execSQL("DELETE FROM " + TABLE_CONTACTS);
		db.execSQL("DELETE FROM " + TABLE_UPDATES);
		db.execSQL("DELETE FROM " + TABLE_CONTACT_PHOTO_UPLD_NTFY);
		db.execSQL("DELETE FROM " + TABLE_USERS);
		db.execSQL("DELETE FROM " + TABLE_GROUPS);
	}

	/**
	 * 
	 * 
	 * @param context
	 */
	public static void flushAllCacheTables(Context context) {
		Database dbObj = new Database(context);
		dbObj.open();
		dbObj.flushAllCacheTables();
		dbObj.close();
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	// DATABASE HANDLE FUNCTIONS
	///////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Open a connection to the database as defined in the Database class.
	 * 
	 * @return Database handle
	 */
	public Database open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}

	/**
	 * Close the open database handle.
	 */
	public void close() {
		DBHelper.close();
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	// USERS TABLE FUNCTIONS
	///////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Get the specified user. We store user info in two places to save on space. Not sure if this
	 * is the best approach but since a lot of the data was the same...
	 * 
	 * @param nsid
	 * @return User object
	 */
	public static User getUser(Context context, String nsid) {

		Database dbObj = new Database(context);
		dbObj.open();

		User user = new User();
		// Use QueryBuilder to build the query since we're doing a join.
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(TABLE_USERS + " LEFT OUTER JOIN " + TABLE_CONTACTS +
				" ON (" + TABLE_USERS + "." + COLUMN_NSID + " = " + TABLE_CONTACTS + "." +
				COLUMN_NSID + ")");

		Cursor result = qb.query(dbObj.db, null,
				TABLE_USERS + "." + COLUMN_NSID + " = '" + nsid + "'", null, null, null, null);

		// No rows found
		if(result.getCount() < 1) {
			dbObj.close();
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
			Utilities.debugLog(dbObj, "Unable to parse date for faveDate attribute.");
		}

		user.setFilesizeMax(result.getLong(result.getColumnIndexOrThrow(COLUMN_FILE_MAXSIZE)));
		user.setMbox_sha1sum(result.getString(result.getColumnIndexOrThrow(COLUMN_MBOX_SHA1SUM)));
		user.setPhotosCount(result.getInt(result.getColumnIndexOrThrow(COLUMN_PHOTO_COUNT)));

		try {
			Date photosFirstDate = new Date(Date.parse(result.getString(result.getColumnIndexOrThrow(COLUMN_PHOTOS_FIRSTDATE))));
			user.setPhotosFirstDate(photosFirstDate);
		} catch (Exception e) {
			Utilities.debugLog(dbObj, "Unable to parse date for photosFirstDate attribute.");
		}

		try {
			Date photosFirstDateTaken = new Date(Date.parse(result.getString(result.getColumnIndexOrThrow(COLUMN_PHOTOS_FIRSTDATE_TAKEN))));
			user.setPhotosFirstDateTaken(photosFirstDateTaken);
		} catch (Exception e) {
			Utilities.debugLog(dbObj, "Unable to parse date for photosFirstDateTaken attribute.");
		}

		boolean isAdmin
		= Utilities.intToBool(result.getInt(result.getColumnIndexOrThrow(COLUMN_ADMIN_STATUS)));
		user.setAdmin(isAdmin);
		boolean isPro
		= Utilities.intToBool(result.getInt(result.getColumnIndexOrThrow(COLUMN_PRO_STATUS)));
		user.setPro(isPro);

		// Now set the parts of the user that are in the contacts table
		Contact contact = new Contact();
		contact = dbObj.loadContactFromCursor(contact, result);
		user.setAwayMessage(contact.getAwayMessage());
		user.setIconFarm(contact.getIconFarm());
		user.setIconServer(contact.getIconServer());
		user.setUsername(contact.getUsername());
		user.setRealName(contact.getRealName());
		user.setOnline(contact.getOnline());

		dbObj.close();
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
	public static long getUserUpdateTime(String nsid, Context context) {
		Database dbObj = new Database(context);
		dbObj.open();

		Cursor result = dbObj.db.query(TABLE_USERS, new String[] {COLUMN_LAST_UPDATE},
				COLUMN_NSID + " = '" + nsid + "'", null, null, null, null);

		if(result.getCount() < 1) {
			dbObj.close();
			result.close();
			return 0;
		}

		long lastUpdateTime = 0L;
		if (result.moveToFirst()){
			lastUpdateTime = result.getLong(result.getColumnIndexOrThrow(COLUMN_LAST_UPDATE));
		} else {
			// cursor was empty, keep it 0
			lastUpdateTime = 0L;
		}

		dbObj.close();
		result.close();
		return lastUpdateTime;
	}

	/**
	 * Add the user to the cache setting the update time to now().
	 * 
	 * @param user
	 */
	public static void addUser(User user, Context context) {
		Database dbObj = new Database(context);
		dbObj.open();

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

		dbObj.db.replace(TABLE_USERS, null, values);
		dbObj.close();
	}

	/**
	 * Delete all rows in the users table.
	 */
	public void flushUsersTable() {
		db.execSQL("DELETE FROM " + TABLE_USERS);
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	// AUTHORIZE TABLE FUNCTIONS
	///////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Add a user authorization to the authorize table. This is a simple insert and will put the entry to the
	 * last row hence the use of getLast... etc functions.
	 * 
	 * @param token
	 * @param nsid
	 * @return long val last row inserted or -1 on failure
	 */
	public long addAuth(String token, String nsid) {
		final ContentValues values = new ContentValues();
		values.put(COLUMN_TOKEN, token);
		values.put(COLUMN_NSID, nsid);
		values.put(COLUMN_LAST_UPDATE, System.currentTimeMillis());
		long val = db.insert(TABLE_AUTHORIZE, null, values);

		return val;
	}

	/**
	 * Query the database for the last row and grab the token. This token doesn't
	 * guarantee an authenticated session and will need to be checked in
	 * Flickrj.AuthInterface.checkToken(token)
	 * 
	 * @return token
	 */
	public String getLastAuthToken() {

		String token = "";
		Cursor result = null;

		try {
			result = db.query(TABLE_AUTHORIZE,
					new String[] {COLUMN_TOKEN}, null, null, null, null,
					COLUMN_LAST_UPDATE + " DESC", "1");
			result.moveToFirst();
			token = result.getString(result.getColumnIndexOrThrow(COLUMN_TOKEN));
			result.close();
			return token;

		} catch(Exception e) {
			Utilities.debugLog(this, "Unable to get the last auth token [Exception thrown]");
			if(result != null) {
				result.close();
			}

			Utilities.debugLog(this, "No last user token found.");
			return null;
		}
	}

	/**
	 * Query the database for the last row and grab the NSID. This can perhaps be used
	 * for users that have a token but its not authenticated.
	 * 
	 * @return nsid
	 */
	public static String getLastAuthNsid(Context context) {
		Database dbObj = new Database(context);
		dbObj.open();

		String nsid = "";
		Cursor result = null;

		try {
			result = dbObj.db.query(TABLE_AUTHORIZE,
					new String[] {COLUMN_NSID}, null, null, null, null,
					COLUMN_LAST_UPDATE + " DESC", "1");

			dbObj.close();
			if (result.moveToFirst()){
				int resultIndex = result.getColumnIndexOrThrow(COLUMN_NSID);
				nsid = result.getString(resultIndex);
				result.close();
				return nsid;
			}
		} catch (Exception e) {
			Utilities.errorOccurred(dbObj, "Unable to get the last auth nsid", e);
			if(result != null) {
				result.close();
			}

			if(dbObj != null) {
				dbObj.close();
			}
		}

		Utilities.debugLog(dbObj, "No last user nsid found.");
		return null;
	}

	/**
	 * Delete all rows in the User table and reset auto increment to 0 (1). Similar to MySQL's TRUNCATE.
	 */
	public void flushAuthTable() {
		db.execSQL("DELETE FROM " + TABLE_AUTHORIZE);
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	// CONTACT TABLE FUNCTIONS
	///////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Add a single contact to the database. All the values of a contact object are stored here
	 * for caching purposes so creating them will be complete. Use this function within loops.
	 */
	public void addContact(Contact contact) {
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
	
	/**
	 * Add a single contact to the database. All the values of a contact object are stored here
	 * for caching purposes so creating them will be complete. Use this one outside of loops.
	 */
	public static void addContact(Contact contact, Context context) {
		Database dbObj = new Database(context);
		dbObj.open();
		
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

		values.put(COLUMN_FAMILY_STATUS, contact.isFamily());
		values.put(COLUMN_FRIEND_STATUS, contact.isFriend());
		values.put(COLUMN_IGNORE_STATUS, contact.isIgnored());
		values.put(COLUMN_LAST_UPDATE, System.currentTimeMillis());

		dbObj.db.replace(TABLE_CONTACTS, null, values);
		dbObj.close();
	}
	
	/**
	 * Add a contact deriving it from a user object. Now it is obvious 
	 * but isFamily() and the likes aren't set inside user.
	 * 
	 * @param user
	 * @param context	
	 */
	public static void addContactDerivedFromUser(User user, Context context) {
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
		
		Database.addContact(contact, context);
	}

	/**
	 * Get all contacts stored in the database.
	 * 
	 * @return
	 */
	public Collection<Contact> getContacts() {
		Collection<Contact> contacts = new ArrayList<Contact>();

		// Grab all rows from the table
		Cursor result = db.query(TABLE_CONTACTS,
				null, null, null, null, null, null);

		// No result, no rows to worry about.
		if(result.getCount() < 1) {
			result.close();
			return null;
		}

		// Reset the cursor
		result.moveToFirst();
		while(!result.isAfterLast()) {
			Contact contact = new Contact();
			contact = loadContactFromCursor(contact, result);

			// Add the contact to this collection.
			contacts.add(contact);
			result.moveToNext();
		}

		result.close();
		return contacts;
	}

	/**
	 * Get a single, specific contact by NSID.
	 * 
	 * @param nsid
	 * @return
	 */
	public Contact getContact(String nsid) {
		Contact contact = new Contact();
		Cursor result = db.query(TABLE_CONTACTS,
				null, TABLE_CONTACTS + "." + COLUMN_NSID + " = '" + nsid + "'", null, null, null, null);

		if(result.getCount() < 1) {
			result.close();
			return null;
		}

		result.moveToFirst();
		contact = loadContactFromCursor(contact, result);
		result.close();
		return contact;
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

		boolean isFamily
		= Utilities.intToBool(result.getInt(result.getColumnIndexOrThrow(COLUMN_FAMILY_STATUS)));
		contact.setFamily(isFamily);
		boolean isFriend
		= Utilities.intToBool(result.getInt(result.getColumnIndexOrThrow(COLUMN_FRIEND_STATUS)));
		contact.setFriend(isFriend);
		boolean isIgnored
		= Utilities.intToBool(result.getInt(result.getColumnIndexOrThrow(COLUMN_FAMILY_STATUS)));
		contact.setIgnored(isIgnored);

		return contact;
	}

	/**
	 * Delete all rows in the Contacts table and reset auto increment to 0.
	 */
	public void flushContactsTable() {
		db.execSQL("DELETE FROM " + TABLE_CONTACTS);
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	// NOTIFICATIONS TABLE FUNCTIONS
	///////////////////////////////////////////////////////////////////////////////////////////

	public void addContactsNotify(Contact contact, String msg) {
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
		Utilities.debugLog(this, "Added contact updates row to DB");
	}

	public ArrayList<Map<String, Object>> getContactsNotify() {
		Cursor result = null;

		try {
			result = db.query(TABLE_CONTACT_PHOTO_UPLD_NTFY, null, null, null, null, null, COLUMN_INSERT_TIME + " DESC", null);
			result.moveToFirst();

			ArrayList<Map<String, Object>> temp = new ArrayList<Map<String, Object>>();
			while(!result.isAfterLast()) {
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
			Utilities.errorOccurred(this, "Could not get anything from contacts notify table", e);
			if(result != null) {
				result.close();
			}
		}

		return null;
	}

	/**
	 * Delete all rows in the Contacts table and reset auto increment to 0.
	 */
	public void flushContactsNotifyTable() {
		db.execSQL("DELETE FROM " + TABLE_CONTACT_PHOTO_UPLD_NTFY);
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	// UPDATES TABLE FUNCTIONS
	///////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Insert a new record or update the existing one with an updated current time.
	 * 
	 * @param section
	 */
	public void setUpdateTime(String section) {
		final ContentValues values = new ContentValues();
		values.put(COLUMN_SECTION, section);
		values.put(COLUMN_LAST_UPDATE, System.currentTimeMillis()/1000L);

		db.replace(TABLE_UPDATES, null, values);
	}

	/**
	 * The the last update time for the specified section. If there is no record, this function
	 * will return 0.
	 * 
	 * @param section
	 * @return
	 */
	public long getUpdateTime(String section) {
		Cursor result = db.query(TABLE_UPDATES, new String[] {COLUMN_LAST_UPDATE},
				COLUMN_SECTION + " = '" + section + "'", null, null, null, null);

		if(result.getCount() < 1) {
			result.close();
			return 0;
		}

		result.moveToFirst();
		long lastUpdateTime = result.getLong(result.getColumnIndexOrThrow(COLUMN_LAST_UPDATE));
		result.close();
		return lastUpdateTime;
	}

	/**
	 * Delete all rows in the updates table.
	 */
	public void flushUpdatesTable() {
		db.execSQL("DELETE FROM " + TABLE_UPDATES);
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	// GROUP TABLE FUNCTIONS
	///////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Add a single group to the database. All the values of a group object are stored here
	 * for caching purposes so creating them will be complete.
	 */
	public void addGroup(Group group) {
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
	}

	/**
	 * Get all groups stored in the database.
	 * 
	 * @return
	 */
	public Collection<Group> getGroups() {
		Collection<Group> groups = new ArrayList<Group>();

		// Grab all rows from the table
		Cursor result = db.query(TABLE_GROUPS,
				null, null, null, null, null, null);

		// No result, no rows to worry about.
		if(result.getCount() < 1) {
			result.close();
			return null;
		}

		// Reset the cursor
		result.moveToFirst();
		while(!result.isAfterLast()) {
			Group group = new Group();
			group = loadGroupFromCursor(group, result);

			// Add the contact to this collection.
			groups.add(group);
			result.moveToNext();
		}

		result.close();
		return groups;
	}

	/**
	 * Get a single, specific group by NSID.
	 * 
	 * @param nsid
	 * @return
	 */
	public Group getGroup(String nsid) {
		Group group = new Group();
		Cursor result = db.query(TABLE_GROUPS,
				null, TABLE_GROUPS + "." + COLUMN_NSID + " = '" + nsid + "'", null, null, null, null);

		if(result.getCount() < 1) {
			result.close();
			return null;
		}

		result.moveToFirst();
		group = loadGroupFromCursor(group, result);
		result.close();
		return group;
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
		boolean isAdmin
		= Utilities.intToBool(result.getInt(result.getColumnIndexOrThrow(COLUMN_ADMIN_STATUS)));
		group.setAdmin(isAdmin);

		return group;
	}

	/**
	 * Get the last update time for the specified user.
	 * 
	 * @see Flicka.CACHED_USER_LIMIT
	 * @param nsid
	 * @return
	 */
	public long getGroupUpdateTime(String nsid) {
		Cursor result = db.query(TABLE_GROUPS, new String[] {COLUMN_LAST_UPDATE},
				COLUMN_NSID + " = '" + nsid + "'", null, null, null, null);

		if(result.getCount() < 1) {
			result.close();
			return 0;
		}

		result.moveToFirst();
		long lastUpdateTime = result.getLong(result.getColumnIndexOrThrow(COLUMN_LAST_UPDATE));
		result.close();
		return lastUpdateTime;
	}

	/**
	 * Delete all rows in the Contacts table and reset auto increment to 0.
	 */
	public void flushGroupsTable() {
		db.execSQL("DELETE FROM " + TABLE_GROUPS);
	}
}
