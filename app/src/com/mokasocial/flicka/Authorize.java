package com.mokasocial.flicka;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.Context;
import android.util.Log;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.AuthInterface;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.util.IOUtilities;

public class Authorize {
	/** OBJECT DEFINITIONS */
	Flickr flickr;
	RequestContext requestContext;
	Auth authObj;
	Properties properties;

	/** VARIABLE DEFINITIONS */
	private String frob;
	private boolean authTokenValid = false;

	final static String PROPERTIES_API_KEY = "api_key";
	final static String PROPERTIES_SECRET_KEY = "api_secret";

	/** CONTEXT VARIABLE */
	private final Context mContext;

	// /////////////////////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	// /////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * The Authorize object constructor.
	 * 
	 * @param context
	 * @throws Exception
	 */
	public Authorize(Context context) throws Exception {
		initilizeFlickr();
		mContext = context;
	}

	// /////////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC FUNCTIONS
	// /////////////////////////////////////////////////////////////////////////////////////////

	public static Authorize initializeAuthObj(Context context) {
		Authorize authorize = null;
		try {
			authorize = new Authorize(context);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		boolean result = authorize.initializeToken();
		if (result == false) {
			authorize.authTokenValid = false;
			return authorize;
		}

		authorize.authTokenValid = true;
		return authorize;
	}

	/**
	 * Initialize the FlickrJ library via the Flickr object.
	 * 
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public void initilizeFlickr() throws IOException, ParserConfigurationException {
		loadProperties();
		flickr = new Flickr(properties.getProperty(Authorize.PROPERTIES_API_KEY), properties.getProperty(Authorize.PROPERTIES_SECRET_KEY), new REST());

		requestContext = RequestContext.getRequestContext();
		requestContext.setAuth(null);
		Flickr.debugRequest = false;
		Flickr.debugStream = false;
	}

	/**
	 * Initialize the stored token into the auth object. If one can't be found
	 * or an error occurs, return false.
	 * 
	 * @return boolean
	 */
	public boolean initializeToken() {
		try {
			String token = loadToken();
			if (token == null) {
				Log.d("Auth", "No token found.");
				return false;
			}
			AuthInterface authInterface = flickr.getAuthInterface();
			authObj = authInterface.checkToken(token);
			requestContext.setAuth(authObj);
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}
		return true;
	}

	/**
	 * Create the authentication URL to authorize Flicka against the Flickr API
	 * using the client created frob and requesting the permission level of
	 * Flicka (DELETE).
	 * 
	 * @return String url
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	public String createAuthUrl() throws IOException, SAXException, FlickrException {
		AuthInterface authInterface = flickr.getAuthInterface();
		frob = authInterface.getFrob();
		URL url = authInterface.buildAuthenticationUrl(Permission.DELETE, frob);
		Log.d("Auth", url.toString());

		// The Flickrj library only returns the non-mobile URLs. Fix here.
		return url.toString().replaceFirst(Flicka.FLICKR_MAIN_URL, Flicka.FLICKR_MOBILE_URL);
	}

	/**
	 * With a newly created frob which has been properly authorized by Flickr
	 * website interaction, grab the auth object. This will only work if the
	 * user authorized Flicka and otherwise will throw an exception.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws FlickrException
	 */
	public void fetchNewUserInfo() throws IOException, SAXException, FlickrException {
		AuthInterface authInterface = flickr.getAuthInterface();
		authObj = authInterface.getToken(frob);
		requestContext.setAuth(authObj);
	}

	/**
	 * Load the properties for Flicka which are things like API keys, version
	 * stuff, and other details configurable only by the devs.
	 * 
	 * @see this.properties
	 * @throws IOException
	 */
	public void loadProperties() throws IOException {
		InputStream in = null;
		try {
			in = Flicka.class.getResourceAsStream(Flicka.FLICKA_PROPERTIES_FILE);

			if (in == null) {
				throw new FileNotFoundException("File " + Flicka.FLICKA_PROPERTIES_FILE + " does not exist");
			}

			properties = new Properties();
			properties.load(in);
		} finally {
			IOUtilities.close(in);
		}
	}

	/**
	 * Load the last token in the database.
	 * 
	 * @return String token
	 */
	public String loadToken() {
		Database dbObj = new Database(mContext);
		String result = dbObj.getLastAuthToken();
		return result;
	}

	/**
	 * This function takes the token and user (NSID) and saves them to the
	 * Flicka database.
	 */
	public void saveToken() {
		Database dbObj = new Database(mContext);
		dbObj.addAuth(authObj.getToken(), authObj.getUser().getId());
	}

	public boolean isAuthTokenValid() {
		return authTokenValid;
	}
}
