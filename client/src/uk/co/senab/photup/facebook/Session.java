/*
 * Copyright 2013 Chris Banes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.senab.photup.facebook;

import com.facebook.android.Facebook;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.Flags;

/**
 * A utility class for storing and retrieving Facebook session data.
 *
 * @author yariv
 */
public class Session {

    static final String TOKEN = "access_token";
    static final String EXPIRES = "expires_in";
    static final String KEY = "facebook-session";
    static final String UID = "uid";
    static final String NAME = "name";

    // The Facebook object
    private Facebook fb;

    // The user id of the logged in user
    private String uid;

    // The user name of the logged in user
    private String name;

    /**
     * Constructor
     */
    public Session(Facebook fb, String uid, String name) {
        this.fb = fb;
        this.uid = uid;
        this.name = name;
    }

    /**
     * Returns the Facebook object
     */
    public Facebook getFb() {
        return fb;
    }

    /**
     * Returns the session user's id
     */
    public String getUid() {
        return uid;
    }

    /**
     * Returns the session user's name
     */
    public String getName() {
        return name;
    }

    /**
     * Stores the session data on disk.
     */
    public void save(Context context) {
        if (Flags.DEBUG) {
            Log.d(getClass().getSimpleName(), "Saving Session! Expires: " + fb.getAccessExpires());
        }
        Editor editor = context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.putString(TOKEN, fb.getAccessToken());
        editor.putLong(EXPIRES, fb.getAccessExpires());
        editor.putString(UID, uid);
        editor.putString(NAME, name);
        editor.commit();
    }

    /**
     * Loads the session data from disk.
     */
    public static Session restore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);

        Facebook fb = new Facebook(Constants.FACEBOOK_APP_ID);
        fb.setAccessToken(prefs.getString(TOKEN, null));
        fb.setAccessExpires(prefs.getLong(EXPIRES, 0));

        String uid = prefs.getString(UID, null);
        String name = prefs.getString(NAME, null);

        if (fb.isSessionValid() && uid != null && name != null) {
            return new Session(fb, uid, name);
        }

        return null;
    }

    /**
     * Clears the saved session data.
     */
    public static void clearSavedSession(Context context) {
        Editor editor = context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.clear().commit();
    }

}