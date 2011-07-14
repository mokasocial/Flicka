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

import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photos.SearchParameters;

public class SearchPhotos extends Activity implements OnScrollListener {

	static final String SEARCH_PARAM_TEXT = "param_text";
	static final String SEARCH_PARAM_TAGS = "param_tags";

	private PhotoList mPhotos;
	private ImageAdapter mImageAdapter;

	private Authorize mAuthorize;
	private Context mContext;
	private Activity mActivity;
	private Resources mResources;
	private Bundle mRawSearchParams;
	private SearchParameters mSearchParams;
	private DrawableManager mDraw;

	private final int mResultsPerPage = 25;
	private int mResultsPageNum = 1;

	private boolean mScrolling = false;

	/**
	 * These variables hold unique integers for the dismissal of progress dialog
	 * boxes invoked before starting threads.
	 * 
	 * @see progressDialogHandler
	 */
	private final static int PROGRESS_SEARCH_COMPLETE = 0;
	private final static int PROGRESS_LOAD_PHOTOS = 1;
	private final static int PROGRESS_SEARCH_MORE_COMPLETE = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		mActivity = this;
		mResources = mActivity.getResources();
		mDraw = new DrawableManager(Flicka.PHOTO_ICON_DIR);

		// Set up the various strings and view for this sub activity.
		Utilities.setupActivityView(mActivity, R.string.search, R.layout.view_photo_stream);

		// Start progress box
		Loading.start(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_search_photos, Loading.ACTIVITY_LOADING_ICON);

		Intent intent = mActivity.getIntent();
		Bundle extras = intent.getExtras();
		mRawSearchParams = extras.getBundle(Flicka.INTENT_EXTRA_SEARCH_PARAMS);
		mSearchParams = new SearchParameters();

		// Grab stuff from bundle and convert it to the Search Parameters format.
		String searchParamText = mRawSearchParams.getString(SearchPhotos.SEARCH_PARAM_TEXT);
		if(searchParamText != null) {
			mSearchParams.setText(searchParamText);
		}

		String[] searchParamTags = mRawSearchParams.getStringArray(SearchPhotos.SEARCH_PARAM_TAGS);
		if(searchParamTags != null) {
			mSearchParams.setTags(searchParamTags);
		}

		// Start the retrieval of Groups thread.
		Thread retrieveThread =  new Thread(null, rRetrievePhotos, Flicka.FLICKA_THREAD_NAME);
		retrieveThread.start();
	}

	public PhotoList search(SearchParameters searchParameters)  {
		Utilities.debugLog(this, "Searching photos.");

		PhotosInterface iFace = mAuthorize.flickr.getPhotosInterface();
		PhotoList result;
		try {
			result = iFace.search(searchParameters, mResultsPerPage, mResultsPageNum);
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to search for: " + searchParameters.getText(), e);
			return null;
		}

		return result;
	}
	
	/**
	 * Start a thread to get more search results.
	 */
	@SuppressWarnings("unchecked")
	private void searchMorePhotos() {
		Thread searchMoreThread = new Thread(){
			@Override
			public void run() {
				mResultsPageNum++;
				PhotoList temp = search(mSearchParams);
				if (temp != null){
					mPhotos.addAll(temp);
				}				
				progressDialogHandler.sendEmptyMessage(PROGRESS_SEARCH_MORE_COMPLETE);
			}
		};
		searchMoreThread.start();
	}

	private final OnItemClickListener photoListItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Photo photo = (Photo) mPhotos.get(position);
			Intent intent = new Intent(SearchPhotos.this, ActivityPhoto.class);
			intent.putExtra(Flicka.INTENT_EXTRA_VIEWPHOTO_PHOTOID, photo.getId());
			startActivity(intent);
		}
	};

	/**
	 * This can be invoked from within a thread upon it's completion to close a progress
	 * dialog and perform other tasks. The defined progress dialogs are protected static final
	 * int variables defined in ActivityGroups.
	 */
	public Handler progressDialogHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case PROGRESS_SEARCH_COMPLETE:
					Loading.update(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_loading_photos);
					renderViewPhotos();
					break;
				case PROGRESS_LOAD_PHOTOS:	
					break;
				case PROGRESS_SEARCH_MORE_COMPLETE:
					mImageAdapter.notifyDataSetChanged();
					break;
			}

			super.handleMessage(msg);
		}
	};

	private final Runnable rRetrievePhotos = new Runnable() {
		public void run() {
			mAuthorize = Authorize.initializeAuthObj(mContext);
			mPhotos = search(mSearchParams);
			progressDialogHandler.sendEmptyMessage(PROGRESS_SEARCH_COMPLETE);
		}
	};

	private void renderViewPhotos() {
		try {
			final GridView photosGridView = (GridView) findViewById(R.id.photo_stream_grid_view);
			mImageAdapter = new ImageAdapter(mContext);
			photosGridView.setAdapter(mImageAdapter);
			photosGridView.setOnItemClickListener(photoListItemClickListener);	
			photosGridView.setVerticalScrollBarEnabled(false);
			
			Loading.dismiss(mActivity, Loading.ACTIVITY_LOADING_PARENT, Loading.ACTIVITY_LOADING_TARGET);
			Utilities.setupActivityBreadcrumbEndText(mActivity, mPhotos.getTotal() + " " + getString(R.string.group_total_count));
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to initialize photos.", e);
			Loading.failed(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_FAILED_LOAD);
		}
	}
	
	/**
	 * A custom adapter designed to lazy load images while the user scrolls and they become visible in
	 * the view.
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
			imageView = new ImageView(mContext);
			imageView.setLayoutParams(new GridView.LayoutParams(75, 75));
			imageView.setBackgroundResource(R.drawable.opacity_25);
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setPadding(5, 5, 5, 5);

			final Photo photo = (Photo) getItem(position);

			if(!mScrolling) {
				mDraw.fetchDrawableOnThread(photo.getSmallSquareUrl(), imageView);
			} else {
				imageView.setImageDrawable(mResources.getDrawable(R.drawable.loading_user_icon));
			}

			return imageView;
		}
	}

	/**
	 * Implementation of the OnScrollListener. This is called during a scroll.
	 */
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
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
	
	/**
	 * Show the appropriate layout when the menu button is pressed.
	 * 
	 * @return boolean
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_more_results, menu);
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
		case R.id.load_more_results:
			searchMorePhotos();
			return true;
		default:
			return true;
		}
	}
}
