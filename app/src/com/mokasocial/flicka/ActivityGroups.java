package com.mokasocial.flicka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.aetrion.flickr.groups.Group;
import com.aetrion.flickr.groups.pools.PoolsInterface;
import com.aetrion.flickr.people.PeopleInterface;
import com.aetrion.flickr.people.User;

public class ActivityGroups extends ListActivity implements OnScrollListener {

	// Contacts loaded from local DB or remotely are saved here.
	private Collection<Group> mGroups;
	// BaseAdapter can't be extended by Collection<?> so we're using this for
	// now. We'll make our own later.
	private ArrayList<Group> mGroupsArray;
	private GroupAdapter mGroupAdapter;
	private Authorize mAuthorize;
	private Context mContext;
	private ListActivity mActivity;
	private User mUser;
	private DrawableManager mDraw;

	/**
	 * These variables hold unique integers for the dismissal of progress dialog
	 * boxes invoked before starting threads.
	 * 
	 * @see progressDialogHandler
	 */
	private static final int PROGRESS_RETRIEVE_GROUPS = 0;
	private static final int PROGRESS_LOAD_GROUPS = 1;

	// private boolean mGroupsInDB = false;
	private boolean mScrolling = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up the various strings and view for this sub activity.
		Utilities.setupActivityView(this, R.string.groups, R.layout.view_groups);

		// Set up some basic variables
		mGroupsArray = new ArrayList<Group>();
		mContext = this;
		mActivity = this;
		mDraw = new DrawableManager(Flicka.GROUP_ICON_DIR);

		// Start progress box
		Loading.start(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_getting_groups, Loading.ACTIVITY_LOADING_ICON);

		mGroupAdapter = new GroupAdapter(mContext, R.layout.row_groups_list, mGroupsArray);
		setListAdapter(mGroupAdapter);
		getListView().setOnScrollListener(this);
		getListView().setOnItemClickListener(groupListItemClickListener);
		getListView().setVerticalScrollBarEnabled(false);
		getListView().setTextFilterEnabled(true);
		getListView().setFastScrollEnabled(true);

		// Start the retrieval of Groups thread.
		Thread retrieveThread = new Thread(null, rRetrieveGroups, Flicka.FLICKA_THREAD_NAME);
		retrieveThread.start();
	}

	private final OnItemClickListener groupListItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Group group = mGroupAdapter.mItems.get(position);
			Intent intent = new Intent(ActivityGroups.this, ActivityGroupInfo.class);
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
			case PROGRESS_RETRIEVE_GROUPS:
				Loading.update(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_loading_groups);
				Thread loadThread = new Thread(null, rLoadGroups, Flicka.FLICKA_THREAD_NAME);
				loadThread.start();
				break;
			case PROGRESS_LOAD_GROUPS:
				renderViewGroups();
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
			mUser = ActivityUser.initializeUser(mActivity, mAuthorize, Flicka.INTENT_EXTRA_ACTIVITY_NSID);
			mGroups = loadGroups(false);
			progressDialogHandler.sendEmptyMessage(PROGRESS_RETRIEVE_GROUPS);
		}
	};

	private final Runnable rLoadGroups = new Runnable() {
		public void run() {
			getGroups();
		}
	};

	private void renderViewGroups() {
		try {
			Loading.dismiss(mActivity, Loading.ACTIVITY_LOADING_PARENT, Loading.ACTIVITY_LOADING_TARGET);
			Utilities.setupActivityBreadcrumbEndText(mActivity, mGroups.size() + " " + getString(R.string.group_total_count));
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to initialize groups.", e);
			Loading.failed(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_FAILED_LOAD);
		}
	}

	/**
	 * Convert the groups into an ArrayList. Also, perform this in a UI thread.
	 */
	private void getGroups() {
		mGroupsArray = new ArrayList<Group>();
		try {
			if (mGroups != null) {
				Iterator<Group> groupsIterator = mGroups.iterator();
				while (groupsIterator.hasNext()) {
					Group group = groupsIterator.next();
					mGroupsArray.add(group);
				}
			}
			Utilities.debugLog(this, "Built array. Num objects: " + mGroupsArray.size());
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to get groups.", e);
		}

		runOnUiThread(returnRes);
	}

	/**
	 * This is an adapter class that adds certain view functionalities to an
	 * array list.
	 */
	private class GroupAdapter extends ArrayAdapter<Group> implements Filterable {
		private final Object mLock = new Object();
		private GroupsFilter mFilter;

		public ArrayList<Group> mItems;

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
			mItems = items;
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public Group getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public int getPosition(Group item) {
			return mItems.indexOf(item);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater viewInflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = viewInflator.inflate(R.layout.row_groups_list, null);
			}

			Group group = mItems.get(position);
			if (group != null) {
				final TextView usernameTextView = (TextView) view.findViewById(R.id.username);
				final TextView userlinetwoView = (TextView) view.findViewById(R.id.userlinetwo);

				usernameTextView.setText(group.getName());
				userlinetwoView.setText("Photos: " + group.getPhotoCount());

				final ImageView userIconImageView = (ImageView) view.findViewById(R.id.usericon);
				if (!mScrolling) {
					mDraw.fetchDrawableOnThread(group.getBuddyIconUrl(), userIconImageView);
				} else {
					userIconImageView.setImageDrawable(getResources().getDrawable(R.drawable.loading_user_icon));
				}
			} else {
				Utilities.debugLog(this, "Group should not be null! Position: " + position);
			}

			return view;
		}

		/**
		 * Implementing the Filterable interface.
		 * 
		 */
		@Override
		public Filter getFilter() {
			if (mFilter == null) {
				mFilter = new GroupsFilter();
			}
			return mFilter;
		}

		/**
		 * Custom Filter implementation for the items adapter.
		 * 
		 */
		private class GroupsFilter extends Filter {
			@Override
			protected FilterResults performFiltering(CharSequence prefix) {
				// Initiate our results object
				FilterResults results = new FilterResults();

				// If the adapter array is empty, check the actual items array
				// and use it
				if (mItems == null) {
					synchronized (mLock) { // Notice the declaration above
						mItems = new ArrayList<Group>(mGroupsArray);
					}
				}

				// No prefix is sent to filter by so we're going to send back
				// the original array
				if (prefix == null || prefix.length() == 0) {
					synchronized (mLock) {
						results.values = mGroupsArray;
						results.count = mGroupsArray.size();
					}
				} else {
					// Compare lower case strings
					String prefixString = prefix.toString().toLowerCase();

					// Local to here so we're not changing actual array
					final ArrayList<Group> groups = mItems;
					final int count = groups.size();
					final ArrayList<Group> newGroups = new ArrayList<Group>(count);

					for (int i = 0; i < count; i++) {
						final Group group = groups.get(i);
						final String groupName = group.getName().toString().toLowerCase();

						// First match against the whole, non-splitted value
						if (groupName.startsWith(prefixString)) {
							newGroups.add(group);
						}
					}

					// Set and return
					results.values = newGroups;
					results.count = newGroups.size();
				}

				return results;
			}

			@Override
			@SuppressWarnings("unchecked")
			protected void publishResults(CharSequence prefix, FilterResults results) {
				// noinspection unchecked
				mItems = (ArrayList<Group>) results.values;
				// Let the adapter know about the updated list
				if (results.count > 0) {
					notifyDataSetChanged();
				} else {
					notifyDataSetInvalidated();
				}
			}
		}
	}

	/**
	 * Load a collection of Group objects for the authorized user. We suppress
	 * the casting warning in the line assigning the groups variable because
	 * this warning is irrelevant.
	 * 
	 * @return Collection<Group> or null
	 */

	@SuppressWarnings("unchecked")
	public Collection<Group> loadGroups(boolean refreshGroups) {
		try {
			if (mUser != null) {
				PeopleInterface iface = mAuthorize.flickr.getPeopleInterface();
				mGroups = iface.getPublicGroups(mUser.getId());
			} else {
				PoolsInterface iface = mAuthorize.flickr.getPoolsInterface();
				mGroups = iface.getGroups(0, 0);
			}
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to load groups.", e);
		}

		if (mGroups == null) {
			return new ArrayList<Group>();
		}
		return mGroups;
	}

	/**
	 * Implementation of the OnScrollListener. This is called during a scroll.
	 */
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	/**
	 * Implementation of the OnScrollListener. This is called when the scroll
	 * state is changed. We really only care about the idle state.
	 */
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch (scrollState) {
		case OnScrollListener.SCROLL_STATE_IDLE:
			getListView().setVerticalScrollBarEnabled(false);
			mScrolling = false;
			mGroupAdapter.notifyDataSetChanged();
			break;
		default:
			getListView().setVerticalScrollBarEnabled(true);
			mScrolling = true;
			break;
		}
	}
}
