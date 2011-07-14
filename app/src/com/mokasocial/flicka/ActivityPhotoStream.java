package com.mokasocial.flicka;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.aetrion.flickr.groups.Group;
import com.aetrion.flickr.groups.pools.PoolsInterface;
import com.aetrion.flickr.people.PeopleInterface;
import com.aetrion.flickr.people.User;
import com.aetrion.flickr.photos.Extras;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photos.SearchParameters;
import com.aetrion.flickr.photosets.Photoset;
import com.aetrion.flickr.photosets.PhotosetsInterface;
import com.aetrion.flickr.places.Place;

/**
 * The generic photostream activity class.
 * 
 * Works for group, people, place, and self photostreams!!!
 * 
 * @author Michael Hradek mhradek@gmail.com, mhradek@mokasocial.com, mhradek@flicka.mobi
 * @date 2010.02.06
 */
public class ActivityPhotoStream extends Activity implements OnScrollListener {

	private PhotoList mPhotos;
	private Authorize mAuthorize;
	private Context mContext;
	private Activity mActivity;
	private ImageAdapter mImageAdapter;
	private User mUser;
	private Group mGroup;
	private Place mPlace;
	private Photoset mSet;
	// private static Animation mFadeIn, mFadeOut;
	private int mStreamType;
	private DrawableManager mDraw;
	private Resources mResources;
	private PrefsMgmt mPrefsMgmt;
	private Thread mGetMoreThread;

	// private final static int MENU_ANIMATION_TIME = 150;
	//private final static int MENU_ITEM_SLIDESHOW = 0;
	//private final static int MENU_ITEM_YOUR_PHOTOS = 1;

	private final static int PROGRESS_AUTH_SET_COMPLETE = 0;
	private final static int PROGRESS_GET_PHOTOS_INITIAL = 1;
	private final static int PROGRESS_GET_PHOTOS_CONTINUE = 2;
	private final static int PROGRESS_GET_PHOTOS_COMPLETE = 3;

	final public static int PHOTO_STREAM_TYPE_USER = 0;
	final public static int PHOTO_STREAM_TYPE_GROUP = 1;
	final public static int PHOTO_STREAM_TYPE_PLACE = 2;
	final public static int PHOTO_STREAM_TYPE_SET = 3;

	private final int mPhotosPerPage = 25;
	private int mPhotosPageNum = 1;
	private boolean mScrolling = false;

	final private static int USERNAME_MAX_LENGTH = 19;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up some basic variables
		mContext = this;
		mActivity = this;
		mDraw = new DrawableManager(Flicka.PHOTO_ICON_DIR);
		mResources = getResources();
		mPrefsMgmt = new PrefsMgmt(mContext);
		mPrefsMgmt.restorePreferences();
		mAuthorize = Authorize.initializeAuthObj(mContext);
		
		// Set up the various strings and view for this sub activity.
		Utilities.setupActivityView(mActivity, R.string.photo_stream, R.layout.view_photo_stream);

		// Start progress box
		Loading.start(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_getting_photos, Loading.ACTIVITY_LOADING_ICON);

		// Start the lengthy work
		Thread favoritesThread = new Thread(){
			@Override
			public void run() {
				Authorize authObj = Authorize.initializeAuthObj(mContext);
				initializeStreamType();
				photosHandler.sendEmptyMessage(PROGRESS_AUTH_SET_COMPLETE);
				mPhotos = getPhotos(authObj, mPhotosPerPage, mPhotosPageNum);
				photosHandler.sendEmptyMessage(PROGRESS_GET_PHOTOS_INITIAL);
				getMorePhotos(mPhotos);
				photosHandler.sendEmptyMessage(PROGRESS_GET_PHOTOS_COMPLETE);
			}
		};
		favoritesThread.start();
	}

	/**
	 * This handle expects messages from the thread from within it is called.
	 * 
	 */
	private final Handler photosHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PROGRESS_AUTH_SET_COMPLETE:
				Loading.update(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_loading_photos);
				break;
			case PROGRESS_GET_PHOTOS_INITIAL:
				renderViewPhotos();
				break;
			case PROGRESS_GET_PHOTOS_CONTINUE:
				if(mImageAdapter != null) {
					mImageAdapter.notifyDataSetChanged();
				}

				if(mPhotos != null) {
					Utilities.debugLog(this, "Continued getting photos. Total: " + mPhotos.size());
				}
				break;
			case PROGRESS_GET_PHOTOS_COMPLETE:
				if(mImageAdapter != null) {
					mImageAdapter.notifyDataSetChanged();
				}

				if(mPhotos != null) {
					Utilities.debugLog(this, "Finished getting photos. Total: " + mPhotos.size());
				}
				break;
			}
		}
	};

	private void initializeStreamType() {
		// Grab the passed stream type.
		Intent intent = mActivity.getIntent();
		Bundle extras = intent.getExtras();
		mStreamType = extras.getInt(Flicka.INTENT_EXTRA_PHOTO_STREAM_TYPE);
		Utilities.debugLog(mActivity, "Determined to go with stream type: " + mStreamType);

		if(mStreamType == PHOTO_STREAM_TYPE_USER) {
			try {
				mUser = ActivityUser.initializeUser(mActivity, mAuthorize, Flicka.INTENT_EXTRA_ACTIVITY_NSID);
			} catch (Exception e) {
				mUser = null;
			}
		} else if (mStreamType == PHOTO_STREAM_TYPE_GROUP) {
			try {
				mGroup = ActivityGroupInfo.initializeGroup(mActivity, mAuthorize, Flicka.INTENT_EXTRA_ACTIVITY_NSID);
			} catch (Exception e) {
				mGroup = null;
			}
		} else if (mStreamType == PHOTO_STREAM_TYPE_PLACE) {
			try {
				mPlace = ActivityPhotoStream.initializePlace(mActivity, mAuthorize, Flicka.INTENT_EXTRA_ACTIVITY_NSID);
			} catch (Exception e) {
				mPlace = null;
			}
		} else if (mStreamType == PHOTO_STREAM_TYPE_SET) {
			try {
				mSet = ActivityPhotoStream.initializeSet(mActivity, mAuthorize, Flicka.INTENT_EXTRA_ACTIVITY_NSID);
			} catch (Exception e) {
				mSet = null;
			}
		}
	}

	/**
	 * Load the grid view and set up the click listener.
	 */
	private void renderViewPhotos() {
		try {
			String username;
			if (mUser != null) {
				if(mUser.getUsername().length() > USERNAME_MAX_LENGTH) {
					username = mUser.getUsername().substring(0, USERNAME_MAX_LENGTH) + Flicka.ELLIPSIS;
				} else {
					username = mUser.getUsername();
				}
				Utilities.setupActivityBreadcrumbEndText(mActivity, username);
			} else if (mGroup != null) {
				// @todo different const here
				if(mGroup.getName().length() > USERNAME_MAX_LENGTH) {
					username = mGroup.getName().substring(0, USERNAME_MAX_LENGTH) + Flicka.ELLIPSIS;
				} else {
					username = mGroup.getName();
				}
				Utilities.setupActivityBreadcrumbEndText(mActivity, username);
			} else if (mPlace != null) {
				username = "Place search";
				Utilities.setupActivityBreadcrumbEndText(mActivity, username);
			} else if (mSet != null) {
				if(mSet.getTitle().length() > USERNAME_MAX_LENGTH) {
					username = mSet.getTitle().substring(0, USERNAME_MAX_LENGTH) + Flicka.ELLIPSIS;
				} else {
					username = mSet.getTitle();
				}
				Utilities.setupActivityBreadcrumbEndText(mActivity, username);
			} else {
				// My Photos, then
				username = getString(R.string.generic_breadcrumb_name);
				Utilities.setupActivityBreadcrumbEndText(mActivity, username);
			}

			if(mPhotos == null) {
				throw new Exception();
			}

			if(mPhotos.size() < 1) {
				throw new NoPhotosException();
			}

			Utilities.debugLog(mContext, "Got photos. Count: " + mPhotos.size());

			GridView gridView = (GridView) findViewById(R.id.photo_stream_grid_view);
			mImageAdapter = new ImageAdapter(mContext);
			gridView.setAdapter(mImageAdapter);
			gridView.setOnScrollListener(this);

			gridView.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
					final Photo photo = (Photo) mPhotos.get(position);
					Intent intent = new Intent(ActivityPhotoStream.this, ActivityPhoto.class);
					intent.putExtra(Flicka.INTENT_EXTRA_VIEWPHOTO_PHOTOID, photo.getId());

					String identifier = null;
					int stream = SlideShow.STREAM_USER_PHOTOS;
					if(mStreamType == PHOTO_STREAM_TYPE_USER) {
						if(mUser != null) {
							identifier = mUser.getId();
						} else {
							User thisUser = mAuthorize.authObj.getUser();
							if (thisUser != null){
								identifier = thisUser.getId();
							}
						}						
					} else if(mStreamType == PHOTO_STREAM_TYPE_GROUP) {
						identifier = mGroup.getId();
						stream = SlideShow.STREAM_GROUP_PHOTOS;
					} else if(mStreamType == PHOTO_STREAM_TYPE_PLACE) {
						identifier = mPlace.getPlaceId();
						stream = SlideShow.STREAM_PLACE_PHOTOS;
					} else if(mStreamType == PHOTO_STREAM_TYPE_SET) {
						identifier = mSet.getId();
						stream = SlideShow.STREAM_SET_PHOTOS;
					}

					// Send the details so we can do a slideshow if the user wants
					Bundle slideShow = new Bundle();
					slideShow.putInt(SlideShow.CURRENT_ITEM, position + 1);
					slideShow.putInt(SlideShow.CURRENT_STREAM, stream);
					slideShow.putString(SlideShow.CURRENT_IDENTIFIER, identifier);
					intent.putExtra(Flicka.INTENT_EXTRA_SLIDESHOW, slideShow);

					startActivity(intent);
				}
			});
			Loading.dismiss(mActivity, Loading.ACTIVITY_LOADING_PARENT, Loading.ACTIVITY_LOADING_TARGET);

		} catch (NoPhotosException e) {
			Loading.noDisplay(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_NO_DISPLAY);
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to initialize photo stream.", e);
			Loading.failed(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_FAILED_LOAD);
		}
	}

	/**
	 * No cachey on Places!
	 * @param activity
	 * @param authorize
	 * @param extrasName
	 * @return Place
	 */
	public static Place initializePlace(Activity activity, Authorize authorize, String extrasName) {
		try {
			// Grab the passed NSID.
			Intent intent = activity.getIntent();
			Bundle extras = intent.getExtras();
			String placeId = extras.getString(extrasName);
			try{
				Place freshPlace = new Place();
				freshPlace.setPlaceId(placeId);
				return freshPlace;
			} catch (Exception e){
				e.printStackTrace();
				return null;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * No cachey on Sets!
	 * @param activity
	 * @param authorize
	 * @param extrasName
	 * @return Photoset
	 */
	public static Photoset initializeSet(Activity activity, Authorize authorize, String extrasName) {
		try {
			// Grab the passed NSID.
			Intent intent = activity.getIntent();
			Bundle extras = intent.getExtras();
			String setId = extras.getString(extrasName);
			try{
				// no cachey, retrieve again.
				PhotosetsInterface pFace = authorize.flickr.getPhotosetsInterface();
				Photoset freshSet = pFace.getInfo(setId);

				return freshSet;
			} catch (Exception e){
				Utilities.errorOccurred(activity, "Unable to initialize photoset stream.", e);
				return null;
			}

		} catch (Exception e) {
			Utilities.errorOccurred(activity, "Problem initializing place.", e);
			return null;
		}
	}

	/**
	 * Get more favorites starting at the page after the initial one set above.
	 * When more images are found append them to the passed Collection and
	 * notify the ImageAdapter of the additional data. You must use the Handler
	 * to do this.
	 * 
	 * @param photos
	 */
	@SuppressWarnings("unchecked")
	private void getMorePhotos(final PhotoList photos) {
		// If the user has less than a full page we can safely assume no more exist.
		if(photos == null || photos.size() < mPhotosPerPage) {
			return;
		}
		
		// We are going to wait for the current thread to finish if it is running
		if(mGetMoreThread == null || mGetMoreThread.isAlive() == false) {
			
			// Next page
			mPhotosPageNum++;
			
			mGetMoreThread = new Thread() {
				@Override
				public void run() {
			
					PhotoList result = null;
					Utilities.debugLog(mContext, "Getting page " + mPhotosPageNum);
					Authorize authObj = Authorize.initializeAuthObj(mContext);
					result = getPhotos(authObj, mPhotosPerPage, mPhotosPageNum);		
					
					if(result != null && result.size() > 0) {
						Utilities.debugLog(mContext, "Images received: " + result.size());
						// Add everything to the end of the passed photos Collection
						photos.addAll(result);
						photosHandler.sendEmptyMessage(PROGRESS_GET_PHOTOS_CONTINUE);
					}
				}
			};
			mGetMoreThread.start();
		} else {
			Utilities.debugLog(mContext, "Thread is busy, skipping getting more automatically");
		}
	}

	/**
	 * Get favorites with the passed parameters specific which page and how many per page.
	 * 
	 * @param perPage
	 * @param pageNum
	 * @return
	 */
	private PhotoList getPhotos(Authorize autorize, int perPage, int pageNum) {
		if(mUser != null) {
			try {
				PeopleInterface iFace = autorize.flickr.getPeopleInterface();
				// @todo Something is wonky when using stuff from the DB. Only "fresh" users work. Hmm.
				return iFace.getPhotos(mUser.getId(), mPhotosPerPage, mPhotosPageNum, Extras.MIN_EXTRAS);
			} catch (Exception e) {
				Utilities.errorOccurred(this, "Unable to load user photos.", e);
			}
		} else if (mGroup != null) {
			try {
				PoolsInterface iFace = autorize.flickr.getPoolsInterface();
				return iFace.getPhotos(mGroup.getId(), null, mPhotosPerPage, mPhotosPageNum);
			} catch (Exception e) {
				Utilities.errorOccurred(this, "Unable to load group photos.", e);
			}
		} else if (mPlace != null) {
			try {
				PhotosInterface iFace = autorize.flickr.getPhotosInterface();
				SearchParameters params = new SearchParameters();
				params.setPlaceId(mPlace.getPlaceId());
				return iFace.search(params, mPhotosPerPage, mPhotosPageNum);
			} catch (Exception e) {
				Utilities.errorOccurred(mContext, "Unable to load place photos.", e);
			}
		} else if (mSet != null) {
			try {
				PhotosetsInterface iFace = autorize.flickr.getPhotosetsInterface();
				Utilities.debugLog(mContext, "Set id: " + mSet.getId() + " Page: " + mPhotosPageNum);
				return iFace.getPhotos(mSet.getId(), mPhotosPerPage, mPhotosPageNum);
			} catch (Exception e) {
				Utilities.errorOccurred(mContext, "Unable to load set photos.", e);
			}
		} else {
			try {
				PeopleInterface iFace = autorize.flickr.getPeopleInterface();
				return iFace.getPhotos("me", mPhotosPerPage, mPhotosPageNum, Extras.MIN_EXTRAS);
			} catch (Exception e) {
				Utilities.errorOccurred(mContext, "Unable to load user (you) photos.", e);
			}
		}

		return null;
	}

	/**
	 * A custom adapter designed to lazy load images while the user scrolls and they become visible in
	 * the view.
	 *
	 */
	private class ImageAdapter extends BaseAdapter {
		private final Context mContext;

		public ImageAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return mPhotos.size();
		}

		public Object getItem(int position) {
			return mPhotos.get(position);
		}

		@SuppressWarnings("unused")
		public int getPosition(Photo item) {
			return mPhotos.indexOf(item);
		}

		public long getItemId(int position) {
			return position;
		}

		// Create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {

			ImageView imageView;
			// If it's not recycled, initialize some attributes
			//			if (convertView == null) {
			imageView = new ImageView(mContext);
			imageView.setLayoutParams(new GridView.LayoutParams(75, 75));
			imageView.setBackgroundResource(R.drawable.opacity_25);
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setPadding(5, 5, 5, 5);
			//			} else {
			//				Utilities.debugLog(this, "john said convertview NOT NULL");
			//				// if it IS recycled, need to reset everything about it
			//				// or the following will use the wrong resource
			//				imageView = (ImageView) convertView;
			//			}

			Photo photo = (Photo) getItem(position);

			if(!mScrolling) {
				mDraw.fetchDrawableOnThread(photo.getSmallSquareUrl(), imageView);
			} else {
				imageView.setImageDrawable(mResources.getDrawable(R.drawable.txt_loading));
				imageView.setScaleType(ScaleType.CENTER_INSIDE);
			}

			return imageView;
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
		inflater.inflate(R.menu.menu_photo_stream, menu);
		return true;
	}

	/**
	 * Determine and execute which action to take when a menu item has been selected from
	 * the menu that is shown when the menu button is pressed.
	 * 
	 * @return boolean
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// This needs some logic help. The user may click on home before authenticate is finished.
		case R.id.get_more_favorites:
			getMorePhotos(mPhotos);
			return true;
		default:
			return true;
		}
	}

	/**
	 * Implementation of the OnScrollListener. This is called during a scroll.
	 */
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		
		if(mPhotos == null || mPhotos.size() < mPhotosPerPage) {
			Utilities.debugLog(mContext, "Skipping load more images automatically. Not enough items");
			return;
		}
		
        boolean loadMore = (firstVisibleItem + visibleItemCount) >= totalItemCount;

        if(loadMore == true && mPrefsMgmt.isLoadMoreStreamAutoEnabled() && mScrolling == false) {
        	Utilities.debugLog(mContext, "Going to try and load more images automatically");
        	getMorePhotos(mPhotos);
        	mImageAdapter.notifyDataSetChanged();
        }
	}

	/**
	 * Implementation of the OnScrollListener. This is called when the scroll state is changed.
	 * We really only care about the idle state.
	 */
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch(scrollState) {
		case OnScrollListener.SCROLL_STATE_IDLE:
			mScrolling = false;
			mImageAdapter.notifyDataSetChanged();
			break;
		default:
			mScrolling = true;
			break;
		}
	}
}

class NoPhotosException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
}
