package com.mokasocial.flicka;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.SAXException;

import com.aetrion.flickr.contacts.OnlineStatus;
import com.aetrion.flickr.favorites.FavoritesInterface;
import com.aetrion.flickr.people.PeopleInterface;
import com.aetrion.flickr.people.User;
import com.aetrion.flickr.photos.Extras;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photosets.Photoset;
import com.aetrion.flickr.photosets.Photosets;
import com.aetrion.flickr.photosets.PhotosetsInterface;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

// THIS IS ALL SORTS OF NEWB. NEEDS WORK, CACHING, ETC.

public class ActivityUser extends Activity {
	private User mUser;
	private Authorize mAuthorize;
	private Context mContext;
	private Activity mActivity;
	private PhotoList mFavorites;
	private PhotoList mPhotos;
	private Photosets mSets;
	private FavesAdapter mFavesAdapter;
	private PhotosAdapter mPhotosAdapter;
	private DrawableManager mDraw;

	private final static int PROGRESS_AUTH_SET_COMPLETE = 0;
	private final static int PROGRESS_GET_USER_COMPLETE = 1;

	private final static float USER_ICON_CORNER_RADIUS = 4;

	private final static int RECENT_PHOTOS_LIMIT = 4;
	private final static int RECENT_FAVES_LIMIT = 4;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up the various strings and view for this sub activity.
		Utilities.setupActivityView(this, R.string.user, R.layout.view_user);

		// Set up some basic variables
		mContext = this;
		mActivity = this;
		mDraw = new DrawableManager(Flicka.PHOTO_ICON_DIR);

		// Start progress box
		Loading.start(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_getting_user, Loading.ACTIVITY_LOADING_ICON);

		// Start the lengthy work
		Thread userThread = new Thread() {
			@Override
			public void run() {
				mAuthorize = Authorize.initializeAuthObj(mContext);
				userHandler.sendEmptyMessage(PROGRESS_AUTH_SET_COMPLETE);
				try {
					mUser = ActivityUser.initializeUser(mActivity, mAuthorize,
							Flicka.INTENT_EXTRA_ACTIVITY_NSID);
					mFavorites = getRecentFaves();
					mPhotos = getRecentPhotos();
					mSets = getSets();
				} catch (Exception e) {
					Utilities.errorOccurred(this, "Failed to initialize user",
							e);
				}
				userHandler.sendEmptyMessage(PROGRESS_GET_USER_COMPLETE);
			}
		};
		userThread.start();
	}

	private final Handler userHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PROGRESS_AUTH_SET_COMPLETE:
				final TextView loadingTextView = (TextView) findViewById(R.id.activity_loading_text);
				loadingTextView
				.setText(getString(R.string.progress_loading_user));
				break;
			case PROGRESS_GET_USER_COMPLETE:
				renderViewUser();
				break;
			}
		}
	};

	private void renderViewUser() {
		try {
			populateUserDetails(mUser);
			Loading.dismiss(mActivity, Loading.ACTIVITY_LOADING_PARENT, Loading.ACTIVITY_LOADING_TARGET);
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to initialize user.", e);
			Loading.failed(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_FAILED_LOAD);
		}
	}

	/**
	 * Grab the incoming user and attempt to load it form cache. If that fails,
	 * try fetching a new object from Flickr.
	 * 
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	public static User initializeUser(Activity activity, Authorize authorize,
			String extrasName) {
		try {
			// Grab the passed NSID.
			Intent intent = activity.getIntent();
			Bundle extras = intent.getExtras();
			String nsid = extras.getString(extrasName);
			Utilities.debugLog(activity, "Populated user object with nsid: "
					+ nsid);

			if (nsid == null) {
				return null;
			}

			// Try DB first.
			long lastUpdate = Database.getUserUpdateTime(nsid, activity);

			// Return the cached user if its within the accepted time limit.
			if (lastUpdate != 0
					&& lastUpdate > (System.currentTimeMillis()/1000L - Flicka.CACHED_USER_LIMIT)) {
				Utilities.debugLog(activity, "User was found in cache.");
				return Database.getUser(activity, nsid);
			}

			// Grab the user from Flickr, cache, and return.
			Utilities.debugLog(activity, "Grabbing fresh user details.");
			PeopleInterface pIface = authorize.flickr.getPeopleInterface();
			User freshUser = pIface.getInfo(nsid);
			Database.addUser(freshUser, activity);
			Database.addContactDerivedFromUser(freshUser, activity);
			
			return freshUser;
		} catch (Exception e) {
			Utilities.errorOccurred(activity, "Problem initializing user.", e);
			return null;
		}
	}

	/**
	 * This is a basic crap function to demonstrate what date is available.
	 * Images will need to get loaded, etc. It should probably remain but I
	 * imagine it will be much cleaner.
	 * 
	 * @param user
	 */
	private void populateUserDetails(User user) {
		final TextView userNameTextView = (TextView) findViewById(R.id.details_user_name);
		final TextView userPhotoCount = (TextView) findViewById(R.id.details_user_photo_count);
		final ImageView userIconView = (ImageView) findViewById(R.id.details_user_icon);
		final ImageView userIsProView = (ImageView) findViewById(R.id.details_user_pro);

		userNameTextView.setText(user.getUsername());
		Utilities.debugLog(this, user.getBuddyIconUrl());

		// Load image from cache
		InputStream is = ImageMgmt.loadImage(user.getBuddyIconUrl(), new File(
				Flicka.CONTACT_ICON_DIR));

		// If it fails (missing SDCard)
		if (is == null) {
			is = ImageMgmt.fetchImage(user.getBuddyIconUrl());
			ImageMgmt.saveImage(is, user.getBuddyIconUrl(), new File(
					Flicka.CONTACT_ICON_DIR));
			is = ImageMgmt.loadImage(user.getBuddyIconUrl(), new File(
					Flicka.CONTACT_ICON_DIR));
		}

		String breadcrumb;
		if (user.getRealName() != null && user.getRealName().length() != 0) {
			breadcrumb = user.getRealName();
		} else {
			breadcrumb = user.getUsername();
		}

		Utilities.setupActivityBreadcrumbEndText(mActivity, breadcrumb);

		// Load into Bitmap, let's add some rounded corners
		Bitmap userIcon;
		if (is != null) {
			userIcon = ImageMgmt.getRndedCornerBtmp(BitmapFactory
					.decodeStream(is), USER_ICON_CORNER_RADIUS);
			try {
				is.close();
			} catch (IOException e) {
			}
		} else {
			userIcon = ImageMgmt.getRndedCornerBtmp(BitmapFactory
					.decodeResource(this.getResources(),
							R.drawable.loading_user_icon),
							USER_ICON_CORNER_RADIUS);
		}

		userIconView.setImageBitmap(userIcon);

		if (mUser.getLocation() != null && mUser.getLocation() != "") {
			populateUserLocation();
		}

		if (mFavorites != null && mFavorites.size() > 0) {
			populateUserRecentFaves();
		}

		if (mPhotos != null && mPhotos.size() > 0) {
			populateUserRecentPhotos();
		}

		if (mSets != null && mSets.getPhotosets().size() > 0) {
			populateUserPhotosets();
		}

		int isProVisible = ImageView.GONE;
		if (user.isPro()) {
			isProVisible = ImageView.VISIBLE;
		}
		userIsProView.setVisibility(isProVisible);
		userPhotoCount.setText(Integer.toString(user.getPhotosCount()));
	}

	private void populateUserLocation() {
		final RelativeLayout locationLayout = (RelativeLayout) findViewById(R.id.user_location);
		locationLayout.setVisibility(RelativeLayout.VISIBLE);
		final TextView location = (TextView) findViewById(R.id.user_location_text);
		location.setText(mUser.getLocation());
	}

	private void populateUserRecentFaves() {
		final RelativeLayout favesLayout = (RelativeLayout) findViewById(R.id.user_favorites);
		favesLayout.setVisibility(RelativeLayout.VISIBLE);
		final GridView favesGridView = (GridView) findViewById(R.id.favorites_grid_view);
		mFavesAdapter = new FavesAdapter(mContext);
		favesGridView.setAdapter(mFavesAdapter);

		favesGridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				Photo photo = (Photo) mFavorites.get(position);
				Intent intent = new Intent(ActivityUser.this,
						ActivityPhoto.class);
				intent.putExtra(Flicka.INTENT_EXTRA_VIEWPHOTO_PHOTOID, photo
						.getId());

				// Send the details so we can do a slideshow if the user wants
				Bundle slideShow = new Bundle();
				slideShow.putInt(SlideShow.CURRENT_ITEM, position + 1);
				slideShow.putInt(SlideShow.CURRENT_STREAM,
						SlideShow.STREAM_FAVORITES);
				slideShow
				.putString(SlideShow.CURRENT_IDENTIFIER, mUser.getId());
				intent.putExtra(Flicka.INTENT_EXTRA_SLIDESHOW, slideShow);

				startActivity(intent);
			}
		});
	}

	private void populateUserPhotosets() {

		LayoutInflater viewInflator = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final RelativeLayout setsLayout = (RelativeLayout) findViewById(R.id.user_sets);
		setsLayout.setVisibility(RelativeLayout.VISIBLE);

		Object[] setsArray = mSets.getPhotosets().toArray();
		final LinearLayout photosetList = (LinearLayout) findViewById(R.id.set_list);

		for (int i=0;i<setsArray.length;i++){
			View setView = viewInflator.inflate(R.layout.row_photosets_list, null);

			final Photoset photoset = (Photoset) setsArray[i];
			final TextView usernameTextView = (TextView) setView.findViewById(R.id.username);
			final TextView userlinetwoView = (TextView) setView.findViewById(R.id.userlinetwo);

			// Prefer real name. If none is set, use the user name.
			if(photoset.getTitle().length() != 0) {
				usernameTextView.setText(photoset.getTitle());
			} else {
				usernameTextView.setText("");
			}

			userlinetwoView.setText(photoset.getPhotoCount() + " photos");

			final ImageView userIconImageView = (ImageView) setView.findViewById(R.id.usericon);
			mDraw.fetchDrawableOnThread(photoset.getPrimaryPhoto().getSmallSquareUrl(), userIconImageView);

			// set clicky
			setView.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Intent intent = new Intent(ActivityUser.this, ActivityPhotoStream.class);
					intent.putExtra(Flicka.INTENT_EXTRA_PHOTO_STREAM_TYPE, ActivityPhotoStream.PHOTO_STREAM_TYPE_SET);
					intent.putExtra(Flicka.INTENT_EXTRA_ACTIVITY_NSID, photoset.getId());

					startActivity(intent);
				}
			});

			photosetList.addView(setView);
		}
	}

	private void populateUserRecentPhotos() {
		final RelativeLayout recentPhotosLayout = (RelativeLayout) findViewById(R.id.user_photos);
		recentPhotosLayout.setVisibility(RelativeLayout.VISIBLE);
		final GridView photosGridView = (GridView) findViewById(R.id.photos_grid_view);
		mPhotosAdapter = new PhotosAdapter(mContext);
		photosGridView.setAdapter(mPhotosAdapter);

		photosGridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Photo photo = (Photo) mPhotos.get(position);
				Intent intent = new Intent(ActivityUser.this, ActivityPhoto.class);
				intent.putExtra(Flicka.INTENT_EXTRA_VIEWPHOTO_PHOTOID, photo.getId());

				// Send the details so we can do a slideshow if the user wants
				Bundle slideShow = new Bundle();
				slideShow.putInt(SlideShow.CURRENT_ITEM, position + 1);
				slideShow.putInt(SlideShow.CURRENT_STREAM,
						SlideShow.STREAM_USER_PHOTOS);
				slideShow
				.putString(SlideShow.CURRENT_IDENTIFIER, mUser.getId());
				intent.putExtra(Flicka.INTENT_EXTRA_SLIDESHOW, slideShow);

				startActivity(intent);
			}
		});
	}

	/**
	 * Turns out that sometimes online status isn't even set hence the missing
	 * return value. Otherwise, this function converts the OnlineStatus object
	 * of the User and gives a string. We will have to put these into the string
	 * values file but for now they can stick here until we settle on how to
	 * show them.
	 * 
	 * Deprecating this since it doesn't appear that the Flickr API gives us
	 * Online Status anyway.
	 * 
	 * @deprecated
	 * @param onlineStatus
	 * @return
	 */
	@Deprecated
	@SuppressWarnings("unused")
	private String getOnlineStatus(OnlineStatus onlineStatus) {
		String stringStatus = "";

		if (onlineStatus == null) {
			Utilities.debugLog(this, "OnlineStatus object was null.");
			return "missing";
		}

		switch (onlineStatus.getType()) {
		case OnlineStatus.OFFLINE_TYPE:
			stringStatus = "offline";
			break;
		case OnlineStatus.ONLINE_TYPE:
			stringStatus = "online";
			break;
		case OnlineStatus.AWAY_TYPE:
			stringStatus = "away";
			break;
		default:
			stringStatus = "unknown";
			break;
		}

		return stringStatus;
	}

	public void viewFavorites(View view) {
		if (mFavorites == null || mFavorites.isEmpty()) {
			Utilities.alertUser(mContext, "There are no favorites.", null);
			return;
		}
		Intent intent = new Intent(ActivityUser.this, ActivityFavorites.class);
		intent.putExtra(Flicka.INTENT_EXTRA_ACTIVITY_NSID, mUser.getId());
		startActivity(intent);
	}

	public void viewPhotos(View view) {
		if (mPhotos == null || mPhotos.isEmpty()) {
			Utilities.alertUser(mContext, "There are no photos.", null);
			return;
		}
		Intent intent = new Intent(ActivityUser.this, ActivityPhotoStream.class);
		intent.putExtra(Flicka.INTENT_EXTRA_ACTIVITY_NSID, mUser.getId());
		intent.putExtra(Flicka.INTENT_EXTRA_PHOTO_STREAM_TYPE, ActivityPhotoStream.PHOTO_STREAM_TYPE_USER);
		startActivity(intent);
	}

	private PhotoList getRecentFaves() {
		try {
			FavoritesInterface iFace = mAuthorize.flickr.getFavoritesInterface();
			return iFace.getList(mUser.getId(), RECENT_FAVES_LIMIT, 1, Extras.MIN_EXTRAS);
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to get recent faves", e);
			return null;
		}
	}

	private Photosets getSets() {
		try {
			PhotosetsInterface sFace = mAuthorize.flickr.getPhotosetsInterface();
			return sFace.getList(mUser.getId());
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to get user Photosets", e);
			return null;
		}
	}

	private PhotoList getRecentPhotos() {
		try {
			PeopleInterface iFace = mAuthorize.flickr.getPeopleInterface();
			String userNsid = mUser.getId() == mAuthorize.authObj.getUser().getId() ? "me" : mUser.getId();
			return iFace.getPhotos(userNsid, RECENT_PHOTOS_LIMIT, 1, Extras.MIN_EXTRAS);
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to get recent photos", e);
			return null;
		}
	}

	/**
	 * A custom adapter designed to lazy load images while the user scrolls and
	 * they become visible in the view.
	 * 
	 */
	private class FavesAdapter extends BaseAdapter {
		private final Context mContext;

		public FavesAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return mFavorites.size();
		}

		public Object getItem(int position) {
			return mFavorites.get(position);
		}

		public long getItemId(int position) {
			return 0;
		}

		// Create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;
			// If it's not recycled, initialize some attributes
			if (convertView == null) {
				imageView = new ImageView(mContext);
				imageView.setLayoutParams(new GridView.LayoutParams(65, 65));
				imageView.setBackgroundResource(R.drawable.opacity_25_green);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(5, 5, 5, 5);
			} else {
				imageView = (ImageView) convertView;
			}

			Photo photo = (Photo) getItem(position);
			mDraw.fetchDrawableOnThread(photo.getSmallSquareUrl(), imageView);
			return imageView;
		}
	}

	private class PhotosAdapter extends BaseAdapter {
		private final Context mContext;

		public PhotosAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return mPhotos.size();
		}

		public Object getItem(int position) {
			return mPhotos.get(position);
		}

		public long getItemId(int position) {
			return 0;
		}

		// Create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;
			// If it's not recycled, initialize some attributes
			if (convertView == null) {

				imageView = new ImageView(mContext);
				imageView.setLayoutParams(new GridView.LayoutParams(65, 65));
				imageView.setBackgroundResource(R.drawable.opacity_25_green);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(5, 5, 5, 5);
			} else {
				imageView = (ImageView) convertView;
			}

			Photo photo = (Photo) getItem(position);
			mDraw.fetchDrawableOnThread(photo.getSmallSquareUrl(), imageView);
			return imageView;
		}
	}
}
