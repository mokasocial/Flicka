package com.mokasocial.flicka;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class ActivityNotifications extends Activity {
    @Override
	public void onCreate(Bundle savedInstanceState) {    
		super.onCreate(savedInstanceState);
	
		// Set up the various strings and view for this sub activity.
		Utilities.setupActivityView(this, R.string.notifications, R.layout.view_notifications);
		
		// Remove extra arrow in breadcrumb nav as we won't have sub-categories.
		this.findViewById(R.id.arrow_right_2).setVisibility(View.GONE);
	}
}
