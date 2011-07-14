package com.mokasocial.flicka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.aetrion.flickr.contacts.Contact;
import com.aetrion.flickr.contacts.ContactsInterface;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

public class ActivityContacts extends ListActivity implements OnScrollListener{

	// Contacts loaded from local DB or remotely are saved here.
	private Collection<Contact> mContacts;
	// BaseAdapter can't be extended by Collection<?> so we're using this for now. We'll make our own later.
	private ArrayList<Contact> mContactsArray;
	private ContactAdapter mContactAdapter;
	private Authorize mAuthorize;
	private Context mContext;
	private ListActivity mActivity;
	private DrawableManager mDraw;
	private Resources mResources;

	/**
	 * These variables hold unique integers for the dismissal of progress dialog
	 * boxes invoked before starting threads.
	 * 
	 * @see progressDialogHandler
	 */
	private static final int PROGRESS_RETRIEVE_CONTACTS = 0;
	private static final int PROGRESS_LOAD_CONTACTS = 1;

	private boolean mContactsInDB = false;
	private boolean mScrolling = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up the various strings and view for this sub activity.
		Utilities.setupActivityView(this, R.string.contacts, R.layout.view_contacts);

		// Set up some basic variables
		mContactsArray = new ArrayList<Contact>();
		mContext = this;
		mActivity = this;
		mDraw = new DrawableManager(Flicka.CONTACT_ICON_DIR);
		mResources = getResources();

		// Start progress box
		Loading.start(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_getting_contacts, Loading.ACTIVITY_LOADING_ICON);

		mContactAdapter = new ContactAdapter(mContext, R.layout.row_contacts_list, mContactsArray);
		setListAdapter(mContactAdapter);
		getListView().setOnScrollListener(this);
		getListView().setOnItemClickListener(contactListItemClickListener);
		getListView().setVerticalScrollBarEnabled(false);
		getListView().setTextFilterEnabled(true);

		// Start the retrieval of contacts thread.
		Thread retrieveThread =  new Thread(null, rRetrieveContacts, Flicka.FLICKA_THREAD_NAME);
		retrieveThread.start();
	}

	private final OnItemClickListener contactListItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Contact contact = mContactAdapter.mItems.get(position);
			Intent intent = new Intent(ActivityContacts.this, ActivityUser.class);
			intent.putExtra(Flicka.INTENT_EXTRA_ACTIVITY_NSID, contact.getId());
			startActivity(intent);
		}
	};

	/**
	 * This can be invoked from within a thread upon it's completion to close a progress
	 * dialog and perform other tasks. The defined progress dialogs are protected static final
	 * int variables defined in ActivityContacts.
	 */
	public Handler progressDialogHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PROGRESS_RETRIEVE_CONTACTS:
				Loading.update(mActivity, Loading.ACTIVITY_LOADING_TEXT, R.string.progress_loading_contacts);
				Thread loadThread =  new Thread(null, rLoadContacts, Flicka.FLICKA_THREAD_NAME);
				loadThread.start();
				break;
			case PROGRESS_LOAD_CONTACTS:
				renderViewContacts();
				break;
			}

			super.handleMessage(msg);
		}
	};

	/**
	 * The primary thread for running the building of the contacts list view.
	 */
	private final Runnable returnRes = new Runnable() {
		public void run() {
			if(mContactsArray != null && mContactsArray.size() > 0) {
				mContactAdapter.notifyDataSetChanged();
				for(int i=0; i < mContactsArray.size(); i++) {
					mContactAdapter.add(mContactsArray.get(i));
				}
			}

			// Close dialog on completion.
			progressDialogHandler.sendEmptyMessage(PROGRESS_LOAD_CONTACTS);
			mContactAdapter.notifyDataSetChanged();
		}
	};

	private final Runnable rRetrieveContacts = new Runnable() {
		public void run() {
			mAuthorize = Authorize.initializeAuthObj(mContext);
			mContacts = loadContacts(false);
			progressDialogHandler.sendEmptyMessage(PROGRESS_RETRIEVE_CONTACTS);
		}
	};

	private final Runnable rLoadContacts = new Runnable(){
		public void run() {
			processContacts();
		}
	};

	private void renderViewContacts() {
		try {
			Loading.dismiss(mActivity, Loading.ACTIVITY_LOADING_PARENT, Loading.ACTIVITY_LOADING_TARGET);
			Utilities.setupActivityBreadcrumbEndText(mActivity, mContacts.size() + " " + getString(R.string.contacts_total_count));
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to initialize contacts.", e);
			Loading.failed(mActivity, Loading.ACTIVITY_LOADING_LAYOUT, Loading.ACTIVITY_FAILED_LOAD);
		}
	}

	/**
	 * Convert the contacts into an ArrayList. Also, perform this in a UI thread.
	 */
	private void processContacts() {
		try {
			mContactsArray = new ArrayList<Contact>();
			if(mContacts != null) {
				Database dbObj = new Database(mContext);
				dbObj.open();
				Iterator<Contact> contactsIterator = mContacts.iterator();
				while(contactsIterator.hasNext()) {
					Contact contact = contactsIterator.next();
					if(mContactsInDB == false) {
						dbObj.addContact(contact);
					}
					mContactsArray.add(contact);
				}
				dbObj.setUpdateTime(Flicka.CACHED_SECTION_CONTACTS);
				dbObj.close();
			}
			Utilities.debugLog(this, "Built array. Num objects: " + mContactsArray.size());
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to get contacts.", e);
		}

		runOnUiThread(returnRes);
	}

	/**
	 * This is an adapter class that adds certain view functionalities to an array list.
	 */
	private class ContactAdapter extends ArrayAdapter<Contact> implements Filterable {
		private final Object mLock = new Object();
		private ContactsFilter mFilter;

		public ArrayList<Contact> mItems;

		/**
		 * The constructor. Note the reference to super.
		 * 
		 * @param Context context
		 * @param int textViewResourceId
		 * @param ArrayList<Contact> items
		 */
		public ContactAdapter(Context context, int textViewResourceId, ArrayList<Contact> items) {
			super(context, textViewResourceId, items);
			mItems = items;
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public Contact getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public int getPosition(Contact item) {
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
				LayoutInflater viewInflator = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = viewInflator.inflate(R.layout.row_contacts_list, null);
			}

			Contact contact = mItems.get(position);
			if (contact != null) {
				final TextView usernameTextView = (TextView) view.findViewById(R.id.username);
				final TextView userlinetwoView = (TextView) view.findViewById(R.id.userlinetwo);

				// Prefer real name. If none is set, use the user name.
				if(contact.getRealName().length() != 0) {
					userlinetwoView.setText(contact.getRealName());
				} else {
					userlinetwoView.setText("");
				}

				usernameTextView.setText(contact.getUsername());

				final ImageView userIconImageView = (ImageView) view.findViewById(R.id.usericon);
				if(!mScrolling) {
					mDraw.fetchDrawableOnThread(contact.getBuddyIconUrl(), userIconImageView);
				} else {
					userIconImageView.setImageDrawable(mResources.getDrawable(R.drawable.loading_user_icon));
				}
			} else {
				Utilities.debugLog(this, "Contact should not be null! Position: " + position);
			}

			return view;
		}

		/**
		 * Implementing the Filterable interface.
		 */
		@Override
		public Filter getFilter() {
			if (mFilter == null) {
				mFilter = new ContactsFilter();
			}
			return mFilter;
		}

		/**
		 * Custom Filter implementation for the contacts adapter.
		 * 
		 * http://www.netmite.com/android/mydroid/frameworks/base/core/java/android/widget/ArrayAdapter.java
		 *
		 */
		private class ContactsFilter extends Filter {
			@Override
			protected FilterResults performFiltering(CharSequence prefix) {
				// Initiate our results object
				FilterResults results = new FilterResults();

				// If the adapter array is empty, check the actual contacts array and use it
				if (mItems == null) {
					synchronized (mLock) {
						mItems = new ArrayList<Contact>(mContactsArray);
					}
				}

				// No prefix is sent to filter by so we're going to send back the original array
				if (prefix == null || prefix.length() == 0) {
					synchronized (mLock) {
						results.values = mContactsArray;
						results.count = mContactsArray.size();
					}
				} else {
					// Compare lower case strings
					String prefixString = prefix.toString().toLowerCase();

					// Local to here so we're not changing actual array
					final ArrayList<Contact> contacts = mItems;
					final int count = contacts.size();
					final ArrayList<Contact> newContacts = new ArrayList<Contact>(count);

					for (int i = 0; i < count; i++) {
						final Contact contact = contacts.get(i);
						final String contactName = contact.getUsername().toString().toLowerCase();

						// First match against the whole, non-splitted value
						if (contactName.startsWith(prefixString)) {
							newContacts.add(contact);
						}
					}

					// Set and return
					results.values = newContacts;
					results.count = newContacts.size();
				}

				return results;
			}

			@Override
			@SuppressWarnings("unchecked")
			protected void publishResults(CharSequence prefix, FilterResults results) {
				//noinspection unchecked
				mItems = (ArrayList<Contact>) results.values;
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
	 * Load a collection of Contact objects for the authorized user. We suppress the casting warning
	 * in the line assigning the contacts variable because this warning is irrelevant.
	 * 
	 * @return Collection<Contact> or null
	 */
	@SuppressWarnings("unchecked")
	public Collection<Contact> loadContacts(boolean refreshContacts) {
		// If contacts exist in the Contacts table, use them otherwise load a new set
		Database dbObj = new Database(mContext);
		dbObj.open();
		long lastUpdate = dbObj.getUpdateTime(Flicka.CACHED_SECTION_CONTACTS);
		if(lastUpdate > (System.currentTimeMillis() - Flicka.CACHED_SECTION_LIMIT)) {
			mContacts = dbObj.getContacts();
		}
		dbObj.close();

		// Something came back from the database, use it.
		if(mContacts != null) {
			Utilities.debugLog(this, "Number of contacts loaded: " + mContacts.size());
			mContactsInDB = true;
			return mContacts;
		}

		try {
			ContactsInterface iface = mAuthorize.flickr.getContactsInterface();
			mContacts = iface.getList(0, 0);
			Utilities.debugLog(this, "Number of contacts fetched: " + mContacts.size());
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to load contacts.", e);
		}

		return mContacts;
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
			getListView().setVerticalScrollBarEnabled(false);
			mScrolling = false;
			mContactAdapter.notifyDataSetChanged();
			break;
		default:
			getListView().setVerticalScrollBarEnabled(true);
			mScrolling = true;
			break;
		}
	}
}