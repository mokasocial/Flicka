package com.mokasocial.flicka;

import com.aetrion.flickr.people.PeopleInterface;
import com.aetrion.flickr.people.User;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class SearchPeople extends Activity {

	private Authorize mAuthorize;
	private Context mContext;
	private Activity mActivity;
	private String mSearchTerms;
	private User mUser;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		mActivity = this;
		mAuthorize = Authorize.initializeAuthObj(mContext);


		Intent intent = mActivity.getIntent();
		Bundle extras = intent.getExtras();
		mSearchTerms = extras.getString(Flicka.INTENT_EXTRA_SEARCH_TERMS);

		mUser = search(mSearchTerms);

		if(mUser == null) {
			Utilities.alertUser(mContext, "None found", null);
			finish();
			return;
		}

		intent = new Intent(SearchPeople.this, ActivityUser.class);
		intent.putExtra(Flicka.INTENT_EXTRA_ACTIVITY_NSID, mUser.getId());
		startActivity(intent);
	}

	/**
	 * Get the user that matches the search string. Return null if none is found.
	 * 
	 * @param searchTerms
	 * @return
	 */
	public User search(String searchTerms)  {
		Utilities.debugLog(this, "Searching people.");
		PeopleInterface iFace = mAuthorize.flickr.getPeopleInterface();
		try {
			return iFace.findByUsername(searchTerms);
		} catch (Exception e) {
			Utilities.errorOccurred(this, "Unable to search for: " + searchTerms, e);
		}

		return null;
	}
}
