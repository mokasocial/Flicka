/*
 * Copyright (c) 2009, MokaSocial, LLC
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mokasocial.flicka;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.TextView;

import com.aetrion.flickr.contacts.Contact;
import com.aetrion.flickr.people.User;

///////////////////////////////////////////////////////////////////////////////////////////
// MAIN ENTRY POINT TO FLICKA
///////////////////////////////////////////////////////////////////////////////////////////

public class Flicka extends ListActivity {

	/**
	 * CONSTANTS
	 * 
	 * These are generally application globals that may not be called in this
	 * file but are used somewhere within the entire Flicka application.
	 */
	static final String FLICKA_PROPERTIES_FILE = File.separator + "assets" + File.separator + "Flicka.properties";
	static final String FLICKA_PREFERENCES_FILE = "Flicka.preferences";

	static final String FLICKA_BASE_STORAGE_DIR = Environment.getExternalStorageDirectory() + File.separator + "Flicka" + File.separator;

	static final String PHOTO_ICON_DIR = FLICKA_BASE_STORAGE_DIR + ".photo_icons";
	static final String CONTACT_ICON_DIR = FLICKA_BASE_STORAGE_DIR + ".contact_icons";
	static final String GROUP_ICON_DIR = FLICKA_BASE_STORAGE_DIR + ".group_icons";
	static final String PHOTO_CACHE_DIR = FLICKA_BASE_STORAGE_DIR + ".photo_cache";

	static final String FLICKR_MOBILE_URL = "http://m.flickr.com";
	static final String FLICKR_MAIN_URL = "http://www.flickr.com";
	static final String FLICKR_API_URL = "http://api.flickr.com";

	static final String FLICKA_THREAD_NAME = "FlickaBackgroundThread";

	static final String PLACEHOLDER_USERNAME = "{USRNAME}";
	static final String PLACEHOLDER_VERSION = "{VERSION}";
	static final String PLACEHOLDER_INTEGER = "{INTEGER}";

	static final String ELLIPSIS = " \u2026";

	static final String INTENT_EXTRA_WEBAUTH_URL = "WebAuthActivity.authURL";
	static final String INTENT_EXTRA_WEBAUTH_RESULTS = "WebAuthActivity.authResults";
	static final String INTENT_EXTRA_ACTIVITY_NSID = "ViewActivity.nsid";
	static final String INTENT_EXTRA_VIEWPHOTO_PHOTOID = "ViewPhoto.photoId";
	static final String INTENT_EXTRA_PHOTO_STREAM_TYPE = "ViewPhotoStream.streamType";
	static final String INTENT_EXTRA_SEARCH_PARAMS = "SearchPhoto.params";
	static final String INTENT_EXTRA_SEARCH_TERMS = "SearchGroupsPlaces.terms";
	static final String INTENT_EXTRA_FILE_URI = "FileUpload.uri";
	static final String INTENT_EXTRA_SLIDESHOW = "SlideShow.bundle";

	static final String REGEX_TAGS_SPLIT = "\\s|,";

	static final int WEB_AUTHENTICATE = 69;
	static final int SETTINGS = 70;
	static final int UPLOAD_IMAGE = 71;

	static final int BROADCAST_RQST_UPDATE_NOTIFICATIONS = 1000;

	public static final String CAMERA_FILE_PATH = FLICKA_BASE_STORAGE_DIR;
	private static final int REQUEST_CAMERA_IMAGE = 42;
	private static final int REQUEST_PHONE_IMAGE = 43;

	static final String CACHED_SECTION_CONTACTS = "contacts";
	static final String CACHED_SECTION_FAVORITES = "favorites";
	static final String CACHED_SECTION_GROUPS = "groups";

	static final long CACHED_SECTION_LIMIT = 100000;
	static final long CACHED_USER_LIMIT = 100000;
	static final long CACHED_GROUP_LIMIT = 100000;

	static final int NETWORK_TIMEOUT_LIMIT = 4000;

	/** ATTRIBUTES */
	private Authorize mAuthorize;
	private ProgressDialog mProgressDialog;
	private Resources mResources;
	private Activity mActivity;
	private Context mContext;

	private String mCameraFilePath;

	/** NOTIFICATIONS */
	private ArrayList<Map<String, Object>> mNotificationsArray;
	private NotificationsAdapter mNotificationsAdapter;
	private SlidingDrawer mSlidingDrawer;

	/** DEFINITIONS */
	boolean NETWORK_AVAILABLE = false;
	boolean ERROR_SAVE_AUTH_TOKEN = false;

	/**
	 * These variables hold unique integers for the dismissal of progress dialog
	 * boxes invoked before starting threads.
	 * 
	 * @see progressDialogHandler
	 */
	protected static final int PROGRESS_HOME_NETWORK_TEST = 0;
	protected static final int PROGRESS_SAVE_TOKEN = 1;
	protected static final int PROGRESS_LOAD_CONTACTS = 2;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/**
		 * The decision on what to do is done here.
		 * 
		 * 1. Determine if the user has a stored token and if it's valid. 2. 1st
		 * time running: Show setup controls. Do the auth work, etc. 3. Show the
		 * user's preferred layout (ex: Home, Contacts, Favorites, etc.). 4.
		 * Default to "Home". If setup failed or something went wrong show
		 * disabled "Home"
		 */

		mActivity = this;
		mContext = this;

		// Initialize resource
		mResources = mActivity.getResources();
		// Initialize some preferences
		Utilities.setupActivityScreen(mActivity);
		// Show the basic Flicka splash screen
		setContentView(R.layout.view_blank_logo);
		// Begin Flicka!
		initialNetworkTest();
	}

	/**
	 * This can be invoked from within a thread upon it's completion to close a
	 * progress dialog and perform other tasks. The defined progress dialogs are
	 * protected static final int variables defined in Flicka.
	 */
	public Handler progressDialogHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PROGRESS_HOME_NETWORK_TEST:
				// If the network test failed show the appropriate dialog.
				if (NETWORK_AVAILABLE == false) {
					Utilities.alertUser(mContext, getString(R.string.home_welcome_msg), null);
				}

				launchFlicka(NETWORK_AVAILABLE);
				break;

			case PROGRESS_SAVE_TOKEN:
				mProgressDialog.dismiss();
				// If these was a failure saving or initializing the auth token
				// allow the user to retry.
				if (ERROR_SAVE_AUTH_TOKEN == true) {
					saveAuthTokenErrorDialog();
				} else {
					showHomeView();
				}

				break;

			case PROGRESS_LOAD_CONTACTS:
				mProgressDialog.dismiss();
				break;
			}

			super.handleMessage(msg);
		}
	};

	/**
	 * Do a generic network test. Start a progress dialog. Try the connectivity
	 * manager's poll of the devices network state. Then try and connect to the
	 * flickr URL. If both pass set the global to true. This might not be the
	 * best way to do it as it couples this function with this class; we do this
	 * because the test takes a variable amount of time and to avoid the UI from
	 * surpassing this test.
	 * 
	 * @return void
	 */
	public void initialNetworkTest() {
		new Thread() {
			@Override
			public void run() {
				// Using the Android system to determine network.
				try {
					ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo networkInfo = conMan.getActiveNetworkInfo();

					// We have a connection but we still need to test the route
					if (networkInfo != null && networkInfo.isConnected()) {
						Log.d("Flicka", "Android network state: " + networkInfo.getState().toString());
						NETWORK_AVAILABLE = true;
					} else {
						Log.d("Flicka", "Android network state: null");
					}

					// Only do this part of the test if the device thinks it has
					// network.
					if (NETWORK_AVAILABLE == true) {
						HttpURLConnection conn = null;
						try {
							URL url = new URL(FLICKR_MOBILE_URL);
							conn = (HttpURLConnection) url.openConnection();
							conn.setRequestMethod("GET");
							conn.setConnectTimeout(NETWORK_TIMEOUT_LIMIT);
							conn.setReadTimeout(NETWORK_TIMEOUT_LIMIT);
							conn.connect();
							int httpResponse = conn.getResponseCode();
							if (httpResponse != HttpURLConnection.HTTP_OK) {
								throw new Exception("Response from '" + FLICKR_MOBILE_URL + "' invalid.");
							} else {
								Log.d("Flicka", "Trace route to Flickr API successful");
							}
						} catch (Exception e) {
							NETWORK_AVAILABLE = false;
							Log.d("Flicka", e.getMessage());
						} finally {
							if (conn != null) {
								conn.disconnect();
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Start Flicka or display network error message.
				progressDialogHandler.sendEmptyMessage(PROGRESS_HOME_NETWORK_TEST);
			}
		}.start();
	}

	/**
	 * Launching involves setting up the Flicka.authorize object and then
	 * setting up the view. The logic surrounding the Flicka.authorize object's
	 * success needs to be fixed. The view will probably undergo some updating.
	 * If the instantiation of Flicka.authorize fails.... no network or
	 * something else bad happened. If Flicka.authorize.iniailizeAuthObj() fails
	 * then the token is bad or no record of it exists.
	 * 
	 * @return void
	 */
	public void launchFlicka(boolean startNetOps) {
		// If network is available do stuff
		if (startNetOps == true) {
			mAuthorize = Authorize.initializeAuthObj(mContext);
			if (mAuthorize.isAuthTokenValid() == true) {
				try {
					// Initialize the notifications
					NotifyMgmt.initNotifications(mContext);
					// Update the name below the status bar.
					String username = mAuthorize.authObj.getUser().getUsername();
					Log.d("Flicka", "User permission level: " + mAuthorize.authObj.getPermission().toString());
					// Set up the layout and title bar
					setTitle(getTitle() + " Home");
					setContentView(R.layout.view_home);
					final TextView currentRankText = (TextView) findViewById(R.id.home_welcome_msg);
					currentRankText.setText(getString(R.string.home_welcome_msg).replace(PLACEHOLDER_USERNAME, username));

					initSlidingDrawer();

					Intent intent = getIntent();
					Bundle extras = intent.getExtras();
					Object cameFromNotifications = extras.get(NotifyMgmt.JUMP_TO_NOTIFICATIONS);
					if (cameFromNotifications != null) {
						// the user came here from a notification!
						mSlidingDrawer.animateOpen();
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				setContentView(R.layout.view_new_user);
				final Button button = (Button) findViewById(R.id.login_button);
				button.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						beginAuthProcess();
					}
				});
			}
		} else {
			// Check to see if ever signed in
			Database db = new Database(mContext);
			String nsid = db.getLastAuthNsid();
			// If no, show disabled sign in button
			if (nsid == null) {
				setContentView(R.layout.view_new_user);
				final Button button = (Button) findViewById(R.id.login_button);
				button.setEnabled(false);
				// If yes show the home screen
			} else {
				setTitle(getTitle() + " Home");

				setContentView(R.layout.view_home);

				User you = db.getUser(nsid);
				if (you != null) {
					String username = db.getUser(nsid).getUsername();
					final TextView currentRankText = (TextView) findViewById(R.id.home_welcome_msg);
					currentRankText.setText(getString(R.string.home_welcome_msg).replace(PLACEHOLDER_USERNAME, username));
				}
			}
		}
	}

	/**
	 * Show a dialog indicating a save auth token operation failed. The user
	 * will have two options. The first is to retry the auth process. The second
	 * is to cancel and exit out of the application.
	 * 
	 * @return void
	 */
	public void saveAuthTokenErrorDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage(mResources.getString(R.string.failure_save_auth_token)).setTitle(mResources.getString(R.string.failure_save_auth_token_title)).setCancelable(false).setPositiveButton(mResources.getString(R.string.option_retry), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				ERROR_SAVE_AUTH_TOKEN = false;
				openFlickrForAuth();
			}
		}).setNegativeButton(mResources.getString(R.string.option_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
				finish();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Show a generic error message explaining something unusual has happen
	 * perhaps even an error code and exit the application. This is mainly to
	 * avoid not showing any details and just continueing.
	 * 
	 * @return void
	 */
	public void genericErrorDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage(mResources.getString(R.string.failure_generic)).setTitle(mResources.getString(R.string.failure_generic_title)).setCancelable(false).setPositiveButton(mResources.getString(R.string.option_okay), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				finish();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * The auth process begins with a dialog box that explains the upcoming
	 * procedure to have the user Flicka.authorize Flicka. It then launches the
	 * appropriate functions depending on the user's request.
	 * 
	 * @return void
	 */
	public void beginAuthProcess() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage(mResources.getString(R.string.modal_auth_launch_browser)).setTitle(mResources.getString(R.string.modal_auth_launch_browser_title)).setCancelable(false).setPositiveButton(mResources.getString(R.string.option_yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				openFlickrForAuth();
			}
		}).setNegativeButton(mResources.getString(R.string.option_no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
				finish();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	private void populateNotifications() {
		Log.d("Flicka", "Attempting to populate notifications");
		Database dbObj = new Database(mContext);
		mNotificationsArray = dbObj.getContactsNotify();
		Log.d("Flicka", "Checked DB, found # contacts to notify about: " + mNotificationsArray.size());

		if (mNotificationsArray != null) {
			mNotificationsAdapter = new NotificationsAdapter(mContext, R.layout.row_groups_list, mNotificationsArray);
			setListAdapter(mNotificationsAdapter);
			getListView().setOnItemClickListener(notificationListItemClickListener);
			getListView().setVerticalScrollBarEnabled(false);
		}
	}

	private final OnItemClickListener notificationListItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Map<String, Object> notifyMap = mNotificationsAdapter.items.get(position);
			Contact contact = (Contact) notifyMap.get(Database.MAP_KEY_CONTACT);
			Intent intent = new Intent(mContext, ActivityUser.class);
			intent.putExtra(Flicka.INTENT_EXTRA_ACTIVITY_NSID, contact.getId());
			startActivity(intent);
		}
	};

	private void initSlidingDrawer() {
		// When the drawer opens it, populate it
		mSlidingDrawer = (SlidingDrawer) findViewById(R.id.home_sliding_drawer);
		mSlidingDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener() {
			public void onDrawerOpened() {
				populateNotifications();
			}
		});
	}

	/**
	 * This is an adapter class that adds certain view functionalities to an
	 * array list.
	 */
	private class NotificationsAdapter extends ArrayAdapter<Map<String, Object>> {
		public ArrayList<Map<String, Object>> items;

		/**
		 * The constructor. Note the reference to super.
		 * 
		 * @param Context
		 *            context
		 * @param int textViewResourceId
		 * @param ArrayList
		 *            <Group> items
		 */
		public NotificationsAdapter(Context context, int textViewResourceId, ArrayList<Map<String, Object>> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater viewInflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = viewInflator.inflate(R.layout.row_contact_photo_upld_ntfy, null);
			}

			if (items != null) {
				Map<String, Object> notifyMap = items.get(position);
				// int columnId = Integer.parseInt((String)
				// notifyMap.get(Database.COLUMN__ID));
				Contact contact = (Contact) notifyMap.get(Database.MAP_KEY_CONTACT);

				final TextView username = (TextView) view.findViewById(R.id.username);
				final TextView userlinetwo = (TextView) view.findViewById(R.id.userlinetwo);

				final int photosUploaded = contact.getPhotosUploaded();
				final String updateMsg = "Uploaded " + ((photosUploaded > 1) ? photosUploaded + " photos" : " a photo");

				username.setText(contact.getUsername());
				userlinetwo.setText(updateMsg);

			} else {
				Log.d("Flicka", "NotifyMap should not be null! Position: " + position);
			}

			return view;
		}
	}

	/**
	 * Start sharing! Start a modal for the user to decide if they want to
	 * upload files or start the camera.
	 * 
	 * @param view
	 */
	public void startSharing(View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage(mResources.getString(R.string.modal_share_upload_camera_query)).setCancelable(true).setPositiveButton(mResources.getString(R.string.option_upload), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				showPickUploadImageIntent();
			}
		}).setNegativeButton(mResources.getString(R.string.option_camera), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				showCameraIntent();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Start a progress dialog, fetch, and save the new token information to the
	 * user database.
	 * 
	 * @return void
	 */
	public void grabAndSaveToken() {
		mProgressDialog = ProgressDialog.show(mContext, mResources.getString(R.string.progress_fetch_token_title), mResources.getString(R.string.progress_fetch_token), true);
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					e.printStackTrace();
				}

				runOnUiThread(new Runnable() {
					public void run() {
						// Do work
						try {
							mAuthorize.fetchNewUserInfo();
							mAuthorize.saveToken();
							Database db = new Database(mContext);
							db.addUser(mAuthorize.authObj.getUser());
						} catch (Exception e) {
							e.printStackTrace();
							ERROR_SAVE_AUTH_TOKEN = true;
						}
					}
				});

				progressDialogHandler.sendEmptyMessage(PROGRESS_SAVE_TOKEN);
			}
		}.start();
	}

	/**
	 * Show the home view, fill strings, etc.
	 * 
	 * @return void
	 */
	public void showHomeView() {
		String username = mAuthorize.authObj.getUser().getUsername();
		setTitle(getTitle() + " Home");
		setContentView(R.layout.view_home);
		final TextView welcomeMessage = (TextView) findViewById(R.id.home_welcome_msg);
		welcomeMessage.setText(mResources.getString(R.string.home_welcome_msg).replace(PLACEHOLDER_USERNAME, username));
	}

	/**
	 * Show the contacts view, fill strings, etc.
	 * 
	 * @return void
	 */
	public void showContactsView(View view) {
		Intent ContactsIntent = new Intent(mContext, ActivityContacts.class);
		startActivity(ContactsIntent);
	}

	/**
	 * Show the upload intent.
	 * 
	 */
	public void showPickUploadImageIntent() {
		Intent pickUploadImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
		pickUploadImageIntent.setType("image/*");
		startActivityForResult(pickUploadImageIntent, REQUEST_PHONE_IMAGE);
	}

	/**
	 * Show the camera intent.
	 * 
	 */
	public void showCameraIntent() {
		Intent takePictureFromCameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		mCameraFilePath = CAMERA_FILE_PATH + "camera_" + Long.toString(System.currentTimeMillis()) + ".jpg";
		Log.d("Flicka", "Camera to use file name: " + mCameraFilePath);
		takePictureFromCameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mCameraFilePath)));
		startActivityForResult(takePictureFromCameraIntent, REQUEST_CAMERA_IMAGE);
	}

	/**
	 * Show the favorites view, fill strings, etc.
	 * 
	 * @return void
	 */
	public void showFavoritesView(View view) {
		// Launch the sub activity.
		Intent favoritesIntent = new Intent(mContext, ActivityFavorites.class);
		favoritesIntent.putExtra(Flicka.INTENT_EXTRA_ACTIVITY_NSID, mAuthorize.authObj.getUser().getId());
		startActivity(favoritesIntent);
	}

	/**
	 * Show the groups view, fill strings, etc.
	 * 
	 * @return void
	 */
	public void showGroupsView(View view) {
		Intent GroupsIntent = new Intent(mContext, ActivityGroups.class);
		startActivity(GroupsIntent);
	}

	/**
	 * Show the search view, fill strings, etc.
	 * 
	 * @return void
	 */
	public void showSearchView(View view) {
		Intent SearchIntent = new Intent(mContext, ActivitySearch.class);
		startActivity(SearchIntent);
	}

	/**
	 * Show the notifications view, fill strings, etc.
	 * 
	 * @return void
	 * @deprecated
	 */
	@Deprecated
	public void showNotificationsView(View view) {
		Intent NotificationsIntent = new Intent(mContext, ActivityNotifications.class);
		startActivity(NotificationsIntent);
	}

	/**
	 * Show the photo stream view, fill strings, etc.
	 * 
	 * @return void
	 */
	public void showPhotosStreamView(View view) {
		Intent PhotoStreamIntent = new Intent(mContext, ActivityPhotoStream.class);
		PhotoStreamIntent.putExtra(Flicka.INTENT_EXTRA_PHOTO_STREAM_TYPE, ActivityPhotoStream.PHOTO_STREAM_TYPE_USER);
		startActivity(PhotoStreamIntent);
	}

	/**
	 * Show the settings view, fill strings, etc.
	 * 
	 * @return void
	 */
	public void showSettingsView() {
		Intent SettingsIntent = new Intent(mContext, ActivitySettings.class);
		startActivityForResult(SettingsIntent, SETTINGS);
	}

	/**
	 * Open a browser activity which loads the Flickr auth URL. This is a URL
	 * that contains GET parameters that contain the FROB.
	 * 
	 * @return void
	 */
	public void openFlickrForAuth() {
		// Disable the begin button so users can't reclick it.
		try {
			final Button beginButton = (Button) findViewById(R.id.login_button);
			beginButton.setEnabled(false);

			Intent flickrAuthWebIntent = new Intent(mContext, ActivityWebAuth.class);
			String authUrl = mAuthorize.createAuthUrl();
			flickrAuthWebIntent.putExtra(INTENT_EXTRA_WEBAUTH_URL, authUrl);
			Log.d("Flicka", authUrl);

			startActivityForResult(flickrAuthWebIntent, WEB_AUTHENTICATE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Start a dialog and then either proceed or cancel the completion of the
	 * auth process with Flickr.
	 * 
	 * @return void
	 */
	public void completeAuthProcess() {
		// Set view to splash screen
		setContentView(R.layout.view_blank_logo);
		// Start dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage(mResources.getString(R.string.modal_auth_browser_complete)).setTitle(mResources.getString(R.string.modal_auth_browser_complete_title)).setCancelable(false).setPositiveButton(mResources.getString(R.string.option_continue), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				grabAndSaveToken();
			}
		}).setNegativeButton(mResources.getString(R.string.option_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
				finish();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// BUTTON DRIVEN MENU
	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Capture the result of any activity which was slated to return a result.
	 * 
	 * @return void
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch (requestCode) {
		case WEB_AUTHENTICATE:
			if (resultCode == -1) {
				completeAuthProcess();
			} else {
				finish();
			}
			break;
		case SETTINGS:
			launchFlicka(NETWORK_AVAILABLE);
			break;
		case REQUEST_CAMERA_IMAGE:
			if (resultCode == Activity.RESULT_OK) {
				Log.d("Flicka", "Returned from activity. Starting upload activity");

				Uri imageUri = Uri.fromFile(new File(mCameraFilePath));
				try {
					android.provider.MediaStore.Images.Media.insertImage(getContentResolver(), imageUri.toString(), null, null);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}

				Intent uploadIntent = new Intent(mContext, ActivityUpload.class);
				uploadIntent.putExtra(INTENT_EXTRA_FILE_URI, imageUri);
				Log.d("Flicka", "URI: " + imageUri.getPath());
				startActivityForResult(uploadIntent, UPLOAD_IMAGE);
			}
			break;
		case REQUEST_PHONE_IMAGE:
			if (resultCode == Activity.RESULT_OK) {
				Log.d("Flicka", "Returned from activity. Starting upload activity");

				Uri imageUri = intent.getData();
				imageUri = Uri.parse(Utilities.getRealPathFromURI(mActivity, imageUri));

				Intent uploadIntent = new Intent(mContext, ActivityUpload.class);
				uploadIntent.putExtra(INTENT_EXTRA_FILE_URI, imageUri);
				Log.d("Flicka", "URI: " + imageUri.getPath());
				startActivityForResult(uploadIntent, UPLOAD_IMAGE);
			}
			break;
		case UPLOAD_IMAGE:
			Log.d("Flicka", "Returned from activity ActivityUpload");
			break;
		default:
			Log.d("Flicka", "onActivityResult was triggered but no case was valid.");
			break;
		}
	}

	/**
	 * Show the appropriate layout when the menu button is pressed.
	 * 
	 * @return boolean
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_home, menu);
		return true;
	}

	/**
	 * Determine and execute which action to take when a menu item has been
	 * selected from the menu that is shown when the menu button is pressed.
	 * 
	 * @return boolean
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// This needs some logic help. The user may click on home before
		// authenticate is finished.
		case R.id.about:
			showAboutDialog();
			return true;
		case R.id.settings:
			showSettingsView();
			return true;
		default:
			return true;
		}
	}

	/**
	 * Show the about dialog box.
	 * 
	 * @return void
	 */
	public void showAboutDialog() {
		Dialog dialog = new Dialog(mContext);
		dialog.setContentView(R.layout.dialog_about);
		String version = Utilities.getSoftwareVersion(mContext).versionName;
		dialog.setTitle(mResources.getString(R.string.about_title).replace(PLACEHOLDER_VERSION, version));
		dialog.show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// Close the sliding drawer when the back button is pushed, if
			// opened.
			if (mSlidingDrawer != null && mSlidingDrawer.isOpened()) {
				mSlidingDrawer.animateClose();
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}
}