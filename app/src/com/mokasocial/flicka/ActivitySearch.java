package com.mokasocial.flicka;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;

/**
 * The search Activity.
 * 
 * @author Michael Hradek mhradek@gmail.com, mhradek@mokasocial.com, mhradek@flicka.mobi
 * @date 2010.04.13
 */
public class ActivitySearch extends Activity {
	
	final public static int SEARCH_TYPE_PHOTOS = 0;
	final public static int SEARCH_TYPE_GROUPS = 1;
	final public static int SEARCH_TYPE_PEOPLE = 2;
	final public static int SEARCH_TYPE_PLACES = 3;
	
	Activity mActivity;
	
    @Override
	public void onCreate(Bundle savedInstanceState) {    
		super.onCreate(savedInstanceState);
		
		mActivity = this;
		
		// Set up the various strings and view for this sub activity.
		Utilities.setupActivityView(mActivity, R.string.search, R.layout.view_search);
		Utilities.setupActivityBreadcrumbEndText(mActivity, null);
		
		final Spinner spinner = (Spinner) findViewById(R.id.search_type_spinner);
	    ArrayAdapter<?> adapter = ArrayAdapter.createFromResource(
	            this, R.array.search_terms, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    
	    // Show and hide parts of the advanced search options as needed.
	    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){ 
			public void onItemSelected(AdapterView<?> adapter, View v, int i, long lng) {
				
		    	if("Photos".equals(adapter.getItemAtPosition(i).toString())){
		    		final RelativeLayout advSearchOptions = (RelativeLayout) findViewById(R.id.search_advanced);
		    		advSearchOptions.setVisibility(RelativeLayout.VISIBLE);
		    	}
		    	else if("Groups".equals(adapter.getItemAtPosition(i).toString())){
		    		final RelativeLayout advSearchOptions = (RelativeLayout) findViewById(R.id.search_advanced);
		    		advSearchOptions.setVisibility(RelativeLayout.INVISIBLE);
		    	}
		    	else if("People".equals(adapter.getItemAtPosition(i).toString())){
		    		final RelativeLayout advSearchOptions = (RelativeLayout) findViewById(R.id.search_advanced);
		    		advSearchOptions.setVisibility(RelativeLayout.INVISIBLE);
		    	}
		    	else if("Places".equals(adapter.getItemAtPosition(i).toString())){
		    		final RelativeLayout advSearchOptions = (RelativeLayout) findViewById(R.id.search_advanced);
		    		advSearchOptions.setVisibility(RelativeLayout.INVISIBLE);
		    	}
		    }

			public void onNothingSelected(AdapterView<?> arg0) {
				final RelativeLayout advSearchOptions = (RelativeLayout) findViewById(R.id.search_advanced);
				advSearchOptions.setVisibility(RelativeLayout.INVISIBLE);
			}	     
	    });
	}
    
    public void search(View view) {
    	// Grab the basic parameters
    	final Spinner searchTypeSpinner = (Spinner) findViewById(R.id.search_type_spinner);
    	final EditText searchTermsField = (EditText) findViewById(R.id.search_terms);
    	
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(searchTermsField.getWindowToken(), 0);
    	int searchType = searchTypeSpinner.getSelectedItemPosition();
    	Utilities.debugLog(this, "Search for: [" + searchType + "]");
    	
    	switch(searchType) {
    		case SEARCH_TYPE_PHOTOS:
    			Bundle searchParams = new Bundle();
    			final RadioButton radioFullTextSearch = (RadioButton) findViewById(R.id.radio_full_text);
			 	final RadioButton radioTagsOnlySearch = (RadioButton) findViewById(R.id.radio_tags_only);
    			
			 	if(radioFullTextSearch.isChecked()) {
			 		searchParams.putString(SearchPhotos.SEARCH_PARAM_TEXT, searchTermsField.getText().toString());
			 	} else if (radioTagsOnlySearch.isChecked()) {
			 		String arrayTags[] = searchTermsField.getText().toString().split(Flicka.REGEX_TAGS_SPLIT);
			 		searchParams.putStringArray(SearchPhotos.SEARCH_PARAM_TAGS, arrayTags);
			 	}
    			
    			searchPhotos(searchParams);
    			break;
    		case SEARCH_TYPE_GROUPS:
    			searchGroups(searchTermsField.getText().toString());
    			break;
    		case SEARCH_TYPE_PEOPLE:
    			searchPeople(searchTermsField.getText().toString());
    			break;    			
    		case SEARCH_TYPE_PLACES:
    			searchPlaces(searchTermsField.getText().toString());
    			break;
    		default:
    			break;    	
    	}
    }

    // For now just search the string. Eventually we can use Bundle to save
    // all the advanced search options.
	public void searchPhotos(Bundle searchParams) {
 	   	Intent intent = new Intent(mActivity, SearchPhotos.class);
 	   	intent.putExtra(Flicka.INTENT_EXTRA_SEARCH_PARAMS, searchParams);
 	   	startActivity(intent);
    }
    
	public void searchGroups(String searchTerms) {
  	   	Intent intent = new Intent(mActivity, SearchGroups.class);
  	   	intent.putExtra(Flicka.INTENT_EXTRA_SEARCH_TERMS, searchTerms);
  	   	startActivity(intent);
    }
    
	public void searchPeople(String searchTerms) {
   	   	Intent intent = new Intent(mActivity, SearchPeople.class);
   	   	intent.putExtra(Flicka.INTENT_EXTRA_SEARCH_TERMS, searchTerms);
   	   	startActivity(intent);
    }
	
	public void searchPlaces(String searchTerms) {
   	   	Intent intent = new Intent(mActivity, SearchPlaces.class);
   	   	intent.putExtra(Flicka.INTENT_EXTRA_SEARCH_TERMS, searchTerms);
   	   	startActivity(intent);
    }
}
