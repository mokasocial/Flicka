package com.mokasocial.flicka;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aetrion.flickr.groups.Group;
import com.aetrion.flickr.groups.GroupList;
import com.aetrion.flickr.groups.GroupsInterface;

public class SearchGroups extends ListActivity {

	// Contacts loaded from local DB or remotely are saved here.
	private GroupList mGroups;
	// BaseAdapter can't be extended by Collection<?> so we're using this for
	// now. We'll make our own later.
	private ArrayList<Group> mGroupsArray;
	private GroupAdapter mGroupAdapter;
	private Activity mActivity;
	private Authorize mAuthorize;
	private Context mContext;
	private String mSearchTerms;

	private final int mResultsPerPage = 18;
	private int mResultsPageNum = 1;

	/**
	 * These variables hold unique integers for the dismissal of progress dialog
	 * boxes invoked before starting threads.
	 * 
	 * @see progressDialogHandler
	 */
	private final static int PROGRESS_SEARCH_COMPLETE = 0;
	private final static int PROGRESS_LOAD_GROUPS = 1;
	private final static int PROGRESS_SEARCH_MORE_COMPLETE = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up some basic variables
		mGroupsArray = new ArrayList<Group>();
		mContext = this;
		mActivity = this;

		// Set up the various strings and view for this sub activity.
		Utilities.setupActivityView(mActivity, R.string.search, R.layout.view_groups);

		// Start progress box
		Loading.start(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_search_groups, Loading.ACTIVITY_LOADING_ICON);

		Intent intent = mActivity.getIntent();
		Bundle extras = intent.getExtras();
		mSearchTerms = extras.getString(Flicka.INTENT_EXTRA_SEARCH_TERMS);

		mGroupAdapter = new GroupAdapter(mContext, R.layout.row_groups_list, mGroupsArray);
		setListAdapter(mGroupAdapter);
		getListView().setOnItemClickListener(groupListItemClickListener);
		getListView().setVerticalScrollBarEnabled(false);

		// Start the retrieval of Groups thread.
		Thread retrieveThread = new Thread(null, rRetrieveGroups, Flicka.FLICKA_THREAD_NAME);
		retrieveThread.start();
	}

	public GroupList search(String searchTerms) {
		GroupsInterface iFace = mAuthorize.flickr.getGroupsInterface();
		GroupList result;
		try {
			result = (GroupList) iFace.search(searchTerms, mResultsPerPage, mResultsPageNum);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return result;
	}

	private final OnItemClickListener groupListItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Group group = mGroupsArray.get(position);
			Intent intent = new Intent(SearchGroups.this, ActivityGroupInfo.class);
			intent.putExtra(Flicka.INTENT_EXTRA_ACTIVITY_NSID, group.getId());
			startActivity(intent);
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
				Loading.update(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_loading_groups);
				Thread loadThread = new Thread(null, rLoadGroups, Flicka.FLICKA_THREAD_NAME);
				loadThread.start();
				break;
			case PROGRESS_LOAD_GROUPS:
				renderViewGroups();
				break;
			case PROGRESS_SEARCH_MORE_COMPLETE:
				Thread loadMoreThread = new Thread(null, rLoadGroups, Flicka.FLICKA_THREAD_NAME);
				loadMoreThread.start();
				break;
			}

			super.handleMessage(msg);
		}
	};

	/**
	 * The primary thread for running the building of the groups list view.
	 */
	private final Runnable returnRes = new Runnable() {
		public void run() {
			if (mGroupsArray != null && mGroupsArray.size() > 0) {
				mGroupAdapter.notifyDataSetChanged();
				for (int i = 0; i < mGroupsArray.size(); i++) {
					mGroupAdapter.add(mGroupsArray.get(i));
				}
			}

			// Close dialog on completion.
			progressDialogHandler.sendEmptyMessage(PROGRESS_LOAD_GROUPS);
			mGroupAdapter.notifyDataSetChanged();
		}
	};

	private final Runnable rRetrieveGroups = new Runnable() {
		public void run() {
			mAuthorize = Authorize.initializeAuthObj(mContext);
			mGroups = search(mSearchTerms);
			progressDialogHandler.sendEmptyMessage(PROGRESS_SEARCH_COMPLETE);
		}
	};

	private final Runnable rLoadGroups = new Runnable() {
		public void run() {
			convertGroups();
		}
	};

	private void renderViewGroups() {
		try {
			if (mGroups.getTotal() > 0) {
				Loading.dismiss(mActivity, Loading.ACTIVITY_LOADING_PARENT, Loading.ACTIVITY_LOADING_PARENT);
			} else {
				Loading.noDisplay(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_NO_DISPLAY);
			}
			Utilities.setupActivityBreadcrumbEndText(mActivity, mGroups.getTotal() + " " + getString(R.string.group_total_count));
		} catch (Exception e) {
			e.printStackTrace();
			Loading.failed(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_FAILED_LOAD);
		}
	}

	/**
	 * Convert the groups into an ArrayList. Also, perform this in a UI thread.
	 */
	@SuppressWarnings("unchecked")
	private void convertGroups() {
		mGroupsArray = new ArrayList<Group>();
		try {
			if (mGroups != null) {
				Iterator<Group> groupsIterator = mGroups.iterator();
				while (groupsIterator.hasNext()) {
					Group group = groupsIterator.next();
					mGroupsArray.add(group);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		runOnUiThread(returnRes);
	}

	/**
	 * Start a thread to get more search results.
	 */
	@SuppressWarnings("unchecked")
	private void searchMoreGroups() {
		Thread searchMoreThread = new Thread() {
			@Override
			public void run() {
				mResultsPageNum++;
				GroupList temp = search(mSearchTerms);
				mGroups.clear();
				mGroups.addAll(temp);
				progressDialogHandler.sendEmptyMessage(PROGRESS_SEARCH_MORE_COMPLETE);
			}
		};
		searchMoreThread.start();
	}

	/**
	 * This is an adapter class that adds certain view functionalities to an
	 * array list.
	 */
	private class GroupAdapter extends ArrayAdapter<Group> {
		private final ArrayList<Group> items;

		/**
		 * The constructor. Note the reference to super.
		 * 
		 * @param Context
		 *            context
		 * @param int textViewResourceId
		 * @param ArrayList
		 *            <Group> items
		 */
		public GroupAdapter(Context context, int textViewResourceId, ArrayList<Group> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater viewInflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = viewInflator.inflate(R.layout.row_groups_search, null);
			}

			Group group = items.get(position);
			if (group != null) {
				final TextView groupName = (TextView) view.findViewById(R.id.groupname);
				// TextView eighteenPlus = (TextView)
				// view.findViewById(R.id.eighteenPlus);

				groupName.setText(group.getName());
			} else {
				try {
					throw new Exception("Group should not be null! Position: " + position);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			return view;
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
		case R.id.load_more_results:
			searchMoreGroups();
			return true;
		default:
			return true;
		}
	}
}
