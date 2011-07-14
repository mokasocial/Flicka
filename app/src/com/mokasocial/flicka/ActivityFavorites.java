package com.mokasocial.flicka;

import com.aetrion.flickr.favorites.FavoritesInterface;
import com.aetrion.flickr.people.User;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;

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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

/**
 * The Favorites display activity class.
 * 
 * @author Michael Hradek mhradek@gmail.com, mhradek@mokasocial.com, mhradek@flicka.mobi
 * @date 2010.02.06
 */
public class ActivityFavorites extends Activity implements OnScrollListener{

	private PhotoList mFavorites;
	private Context mContext;
	private Activity mActivity;
	private ImageAdapter mImageAdapter;
	private User mUser;
	//private static Animation mFadeIn, mFadeOut;
	private DrawableManager mDraw;
	private Resources mResources;
	private PrefsMgmt mPrefsMgmt;
	private Thread mGetMoreThread;

	private final static int PROGRESS_AUTH_SET_COMPLETE = 0;
	private final static int PROGRESS_GET_FAVES_INITIAL = 1;
	private final static int PROGRESS_GET_FAVES_CONTINUE = 2;
	private final static int PROGRESS_GET_FAVES_COMPLETE = 3;

	private final int mFavesPerPage = 25;
	private int mFavesPageNum = 1;
	private boolean mScrolling = false;

	final private static int USERNAME_MAX_LENGTH = 16;

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
		
		// Set up the various strings and view for this sub activity.
		Utilities.setupActivityView(mActivity, R.string.favorites, R.layout.view_favorites);

		// Start progress box
		Loading.start(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_getting_favorites, Loading.ACTIVITY_LOADING_ICON);

		// Start the lengthy work
		Thread favoritesThread = new Thread(){
			@Override
			public void run() {
				Authorize authorize = Authorize.initializeAuthObj(mContext);
				mUser = ActivityUser.initializeUser(mActivity, authorize, Flicka.INTENT_EXTRA_ACTIVITY_NSID);
				favoritesHandler.sendEmptyMessage(PROGRESS_AUTH_SET_COMPLETE);
				mFavorites = getFavorites(authorize, mFavesPerPage, mFavesPageNum);
				favoritesHandler.sendEmptyMessage(PROGRESS_GET_FAVES_INITIAL);
				getMoreFavorites(mFavorites);
				favoritesHandler.sendEmptyMessage(PROGRESS_GET_FAVES_COMPLETE);
			}
		};
		favoritesThread.start();
	}

	/**
	 * This handle expects messages from the thread from within it is called.
	 * 
	 */
	private final Handler favoritesHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PROGRESS_AUTH_SET_COMPLETE:
				Loading.update(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_loading_favorites);
				break;
			case PROGRESS_GET_FAVES_INITIAL:
				renderViewFavorites();
				break;
			case PROGRESS_GET_FAVES_CONTINUE:
				if(mImageAdapter != null) {
					mImageAdapter.notifyDataSetChanged();
				}

				if(mFavorites != null) {
					Utilities.debugLog(mContext, "Continued getting favorites. Total: " + mFavorites.size());
				}
				break;
			case PROGRESS_GET_FAVES_COMPLETE:
				if(mImageAdapter != null) {
					mImageAdapter.notifyDataSetChanged();
				}

				if(mFavorites != null) {
					Utilities.debugLog(mContext, "Finished getting favorites. Total: " + mFavorites.size());
				}
				break;
			}
		}
	};

	/**
	 * Load the grid view and set up the click listener.
	 */
	private void renderViewFavorites() {
		try {
			if(mUser != null && mUser.getUsername() != null) {
				String username;
				if(mUser.getUsername().length() > USERNAME_MAX_LENGTH) {
					username = mUser.getUsername().substring(0, USERNAME_MAX_LENGTH) + Flicka.ELLIPSIS;
				} else {
					username = mUser.getUsername();
				}

				Utilities.setupActivityBreadcrumbEndText(mActivity, username);
			} else {
				Utilities.setupActivityBreadcrumbEndText(mActivity, getString(R.string.generic_breadcrumb_name));
			}

			if(mFavorites == null) {
				throw new Exception();
			}

			if(mFavorites.size() < 1) {
				throw new NoFavoritesException();
			}

			Utilities.debugLog(mContext, "Got favorites. Count: " + mFavorites.size());

			final GridView gridView = (GridView) findViewById(R.id.favorites_grid_view);
			mImageAdapter = new ImageAdapter(mContext);
			gridView.setAdapter(mImageAdapter);
			gridView.setOnScrollListener(this);

			gridView.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
					Photo photo = (Photo) mFavorites.get(position);
					Intent intent = new Intent(ActivityFavorites.this, ActivityPhoto.class);
					intent.putExtra(Flicka.INTENT_EXTRA_VIEWPHOTO_PHOTOID, photo.getId());

					// Send the details so we can do a slideshow if the user wants
					Bundle slideShow = new Bundle();
					slideShow.putInt(SlideShow.CURRENT_ITEM, position + 1);
					slideShow.putInt(SlideShow.CURRENT_STREAM, SlideShow.STREAM_FAVORITES);
					String nsid = (mUser == null) ? null : mUser.getId();
					slideShow.putString(SlideShow.CURRENT_IDENTIFIER, nsid);
					intent.putExtra(Flicka.INTENT_EXTRA_SLIDESHOW, slideShow);

					startActivity(intent);
				}
			});
			Loading.dismiss(mActivity, Loading.ACTIVITY_LOADING_PARENT, Loading.ACTIVITY_LOADING_TARGET);

		} catch (NoFavoritesException e) {
			Loading.noDisplay(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_NO_DISPLAY);
		} catch (Exception e) {
			Utilities.errorOccurred(mContext, "Unable to initialize favorites.", e);
			Loading.failed(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_FAILED_LOAD);
		}
	}

	/**
	 * Get more favorites starting at the page after the initial one set above.
	 * When more images are found append them to the passed Collection and
	 * notify the ImageAdapter of the additional data. You must use the Handler
	 * to do this.
	 * 
	 * @param favorites
	 */
	@SuppressWarnings("unchecked")
	private void getMoreFavorites(final PhotoList favorites) {
		// If the user has less than a full page we can safely assume no more exist.
		if(favorites == null || favorites.size() < mFavesPerPage) {
			return;
		}
		
		// We are going to wait for the current thread to finish if it is running
		if(mGetMoreThread == null || mGetMoreThread.isAlive() == false) {
			
			// Next page
			mFavesPageNum++;
		
			mGetMoreThread = new Thread() {
				@Override
				public void run() {
					PhotoList result = null;
					Utilities.debugLog(mContext, "Getting page " + mFavesPageNum);
					Authorize authObj = Authorize.initializeAuthObj(mContext);
					result = getFavorites(authObj, mFavesPerPage, mFavesPageNum);
					
					if(result != null && result.size() > 0) {
						Utilities.debugLog(mContext, "Images recieved: " + result.size());
						// Add everything to the end of the passed favorites Collection
						favorites.addAll(result);
						favoritesHandler.sendEmptyMessage(PROGRESS_GET_FAVES_CONTINUE);
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
	private PhotoList getFavorites(Authorize autorize, int perPage, int pageNum) {
		try {
			FavoritesInterface iFace = autorize.flickr.getFavoritesInterface();
			String userId = (mUser == null) ? null : mUser.getId();
			return iFace.getList(userId, perPage, pageNum, null);
		} catch (Exception e) {
			Utilities.errorOccurred(mContext, "Unable to load favorites.", e);
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
			return mFavorites.size();
		}

		public Object getItem(int position) {
			return mFavorites.get(position);
		}

		@SuppressWarnings("unused")
		public int getPosition(Photo item) {
			return mFavorites.indexOf(item);
		}

		public long getItemId(int position) {
			return position;
		}

		// Create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {
			Utilities.debugLog(mContext, "Gridview position requested: " + position);
			ImageView imageView;
			// If it's not recycled, initialize some attributes
			//if (convertView == null) {
				imageView = new ImageView(mContext);
				imageView.setLayoutParams(new GridView.LayoutParams(75, 75));
				imageView.setBackgroundResource(R.drawable.opacity_25);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(5, 5, 5, 5);
			//} else {
			//	imageView = (ImageView) convertView;
			//}

			Photo photo = (Photo) getItem(position);
			
			if(!mScrolling) {
				mDraw.fetchDrawableOnThread(photo.getSmallSquareUrl(), imageView);
			} else {
				imageView.setImageDrawable(mResources.getDrawable(R.drawable.loading_user_icon));
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
		inflater.inflate(R.menu.menu_favorites, menu);
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
			getMoreFavorites(mFavorites);
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
		
		if(mFavorites == null || mFavorites.size() < mFavesPerPage) {
			Utilities.debugLog(mContext, "Skipping load more images automatically. Not enough items");
			return;
		}
		
        boolean loadMore = (firstVisibleItem + visibleItemCount) >= totalItemCount;

        if(loadMore == true && mPrefsMgmt.isLoadMoreStreamAutoEnabled() && mScrolling == false) {
        	Utilities.debugLog(mContext, "Going to try and load more images automatically");
        	getMoreFavorites(mFavorites);
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

class NoFavoritesException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
}
