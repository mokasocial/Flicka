package com.mokasocial.flicka;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class ActivityWebAuth extends Activity {

	private WebView mWebView;
	private final String compareAuth1 = "http://m.flickr.com/services/auth/";
	private final String compareAuth2 = "http://m.flickr.com:80/services/auth/";

	private Activity mActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mActivity = this;

		// Initialize some preferences
		Utilities.setupActivityScreen(mActivity);
		// Set the content view
		setContentView(R.layout.view_web_auth);

		mWebView = (WebView) findViewById(R.id.webview);

		WebSettings webSettings = mWebView.getSettings();
		webSettings.setSavePassword(false);
		webSettings.setSaveFormData(false);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(false);

		mWebView.setWebViewClient(new MyWebViewClient());

		final String url = initializeAuthURL();
		mWebView.loadUrl(url);
	}

	private String initializeAuthURL() {
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		return extras.getString(Flicka.INTENT_EXTRA_WEBAUTH_URL);
	}

	public void cancelWebAuth(View view) {
		finish();
	}

	public void returnToFlicka(View view) {
		Intent resultIntent = new Intent();
		resultIntent.putExtra(Flicka.INTENT_EXTRA_WEBAUTH_RESULTS, Flicka.WEB_AUTHENTICATE);
		setResult(Activity.RESULT_OK, resultIntent);
		finish();
	}

	private class MyWebViewClient extends WebViewClient {
		// we need to override the url loading
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		/**
		 * by doing this, we at least know the user did something so, on the
		 * first call of this, enable the return button, decided not to disable
		 * cancel. what if they don't click the ok button?
		 * 
		 */
		@Override
		public void onLoadResource(WebView view, String url) {
			// see if the url is an ajax of some sort
			// Utilities.debugLog(this, "Got a web response: " + url);
			if (url.equals(compareAuth1) || url.equals(compareAuth2)) {
				// They did an ajax of some sort. Enable the button.
				final Button button = (Button) findViewById(R.id.auth_complete_button);
				button.setEnabled(true);
			}
		}
	}
}