package com.mokasocial.flicka;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aetrion.flickr.places.Place;
import com.aetrion.flickr.places.PlacesInterface;
import com.aetrion.flickr.places.PlacesList;

public class SearchPlaces extends ListActivity {

	private PlacesList mPlaces;
	private PlacesAdapter mPlacesAdapter;
	private Activity mActivity;
	private Authorize mAuthorize;
	private Context mContext;
	private String mSearchTerms;

	// private int mResultsPerPage = 18;
	// private int mResultsPageNum = 1;

	/**
	 * These variables hold unique integers for the dismissal of progress dialog
	 * boxes invoked before starting threads.
	 * 
	 * @see progressDialogHandler
	 */
	private final static int PROGRESS_SEARCH_COMPLETE = 0;
	private final static int PROGRESS_LOAD_RESULTS = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up the various strings and view for this sub activity.
		Utilities.setupActivityView(this, R.string.search, R.layout.view_groups);

		// Set up our globals
		mContext = this;
		mActivity = this;

		// Start progress box
		Loading.start(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_search_places, Loading.ACTIVITY_LOADING_ICON);

		// Get the search terms
		Intent intent = mActivity.getIntent();
		Bundle extras = intent.getExtras();
		mSearchTerms = extras.getString(Flicka.INTENT_EXTRA_SEARCH_TERMS);

		// Start the retrieval of Groups thread.
		Thread retrieveThread = new Thread(null, rRetrievePlaces, Flicka.FLICKA_THREAD_NAME);
		retrieveThread.start();
	}

	private final Runnable rRetrievePlaces = new Runnable() {
		public void run() {
			mAuthorize = Authorize.initializeAuthObj(mContext);
			mPlaces = search(mSearchTerms);
			progressDialogHandler.sendEmptyMessage(PROGRESS_SEARCH_COMPLETE);
		}
	};

	/**
	 * This can be invoked from within a thread upon it's completion to close a
	 * progress dialog and perform other tasks. The defined progress dialogs are
	 * protected static final int variables defined in ActivityGroups.
	 */
	public Handler progressDialogHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PROGRESS_SEARCH_COMPLETE:
				Loading.update(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_loading_places);
				renderSearchResults();
				break;
			case PROGRESS_LOAD_RESULTS:
				if (mPlaces.getTotal() > 0) {
					Loading.dismiss(mActivity, Loading.ACTIVITY_LOADING_PARENT, Loading.ACTIVITY_LOADING_TARGET);
				} else {
					Loading.noDisplay(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_NO_DISPLAY);
				}
				break;
			}

			super.handleMessage(msg);
		}
	};

	private void renderSearchResults() {
		try {
			// Set up the adapter
			mPlacesAdapter = new PlacesAdapter(mContext, R.layout.row_places_search, mPlaces);
			setListAdapter(mPlacesAdapter);
			getListView().setOnItemClickListener(placeListItemClickListener);
			getListView().setVerticalScrollBarEnabled(false);
			// Update breadcrumb
			Utilities.setupActivityBreadcrumbEndText(mActivity, mPlaces.getTotal() + " " + getString(R.string.search_total_count));
			progressDialogHandler.sendEmptyMessage(PROGRESS_LOAD_RESULTS);
		} catch (Exception e) {
			e.printStackTrace();
			Loading.failed(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_FAILED_LOAD);
		}
	}

	public PlacesList search(String searchTerms) {
		PlacesInterface iFace = mAuthorize.flickr.getPlacesInterface();
		PlacesList result;
		try {
			result = iFace.find(searchTerms);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return result;
	}

	private final OnItemClickListener placeListItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Place place = (Place) mPlaces.get(position);
			Intent intent = new Intent(SearchPlaces.this, ActivityPhotoStream.class);
			intent.putExtra(Flicka.INTENT_EXTRA_ACTIVITY_NSID, place.getPlaceId().toString());
			intent.putExtra(Flicka.INTENT_EXTRA_PHOTO_STREAM_TYPE, ActivityPhotoStream.PHOTO_STREAM_TYPE_PLACE);
			startActivity(intent);
		}
	};

	public void seePhotos() {

	}

	public void seeOnMap() {

	}

	/**
	 * This is an adapter class that adds certain view functionalities to an
	 * array list.
	 */
	private class PlacesAdapter extends ArrayAdapter<Place> {
		private final PlacesList items;

		/**
		 * The constructor. Note the reference to super.
		 * 
		 * @param Context
		 *            context
		 * @param int textViewResourceId
		 * @param ArrayList
		 *            <Group> items
		 */
		@SuppressWarnings("unchecked")
		public PlacesAdapter(Context context, int textViewResourceId, PlacesList items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater viewInflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = viewInflator.inflate(R.layout.row_places_search, null);
			}

			Place place = (Place) items.get(position);
			if (place != null) {
				final TextView placeName = (TextView) view.findViewById(R.id.placename);
				placeName.setText(place.getName());
			} else {
				try {
					throw new Exception("Couldn't get Place.");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			return view;
		}
	}
}
