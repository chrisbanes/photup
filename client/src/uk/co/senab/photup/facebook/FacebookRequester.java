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
import com.facebook.android.FacebookError;
import com.facebook.android.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.Album;
import uk.co.senab.photup.model.Event;
import uk.co.senab.photup.model.FbUser;
import uk.co.senab.photup.model.Group;
import uk.co.senab.photup.model.Place;

public class FacebookRequester {

    // private final Context mContext;
    private final Account mAccount;
    private final Facebook mFacebook;

    public FacebookRequester(Account account) {
        mAccount = account;
        mFacebook = mAccount.getFacebook();
    }

    public List<Place> getPlaces(Location location, String searchQuery)
            throws FacebookError, JSONException {
        Bundle b = new Bundle();
        b.putString("date_format", "U");
        b.putString("limit", "75");
        b.putString("type", "place");

        if (null != location) {
            b.putString("center", location.getLatitude() + "," + location.getLongitude());
        }

        if (TextUtils.isEmpty(searchQuery)) {
            b.putString("distance", "2000");
        } else {
            b.putString("q", searchQuery);
        }

        String response = null;
        try {
            response = mFacebook.request("search", b);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (null == response) {
            return null;
        }

        JSONObject document = Util.parseJson(response);
        JSONArray data = document.getJSONArray("data");
        ArrayList<Place> places = new ArrayList<Place>(data.length());

        JSONObject object;
        for (int i = 0, z = data.length(); i < z; i++) {
            try {
                object = data.getJSONObject(i);
                places.add(new Place(object, mAccount));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return places;
    }

    public List<FbUser> getFriends() throws FacebookError, JSONException {
        Bundle b = new Bundle();
        b.putString("date_format", "U");
        b.putString("limit", "3000");

        String response = null;
        try {
            response = mFacebook.request("me/friends", b);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (null == response) {
            return null;
        }

        JSONObject document = Util.parseJson(response);

        JSONArray data = document.getJSONArray("data");
        ArrayList<FbUser> friends = new ArrayList<FbUser>(data.length() * 2);
        friends.add(FbUser.getMeFromAccount(mAccount));

        JSONObject object;
        for (int i = 0, z = data.length(); i < z; i++) {
            try {
                object = data.getJSONObject(i);
                friends.add(new FbUser(object, mAccount));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Collections.sort(friends, FbUser.getComparator());

        return friends;
    }

    public List<Group> getGroups() throws FacebookError, JSONException {
        Bundle b = new Bundle();
        b.putString("date_format", "U");
        b.putString("limit", "3000");
        b.putString("fields", Group.GRAPH_FIELDS);

        String response = null;
        try {
            response = mFacebook.request("me/groups", b);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (null == response) {
            return null;
        }

        JSONObject document = Util.parseJson(response);
        JSONArray data = document.getJSONArray("data");
        ArrayList<Group> groups = new ArrayList<Group>(data.length() * 2);

        JSONObject object;
        for (int i = 0, z = data.length(); i < z; i++) {
            try {
                object = data.getJSONObject(i);
                groups.add(new Group(object, mAccount));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return groups;
    }

    public List<Event> getEvents() throws FacebookError, JSONException {
        Bundle b = new Bundle();
        b.putString("date_format", "U");
        b.putString("limit", "3000");
        b.putString("fields", Event.GRAPH_FIELDS);

        String response = null;
        try {
            response = mFacebook.request("me/events", b);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (null == response) {
            return null;
        }

        JSONObject document = Util.parseJson(response);
        JSONArray data = document.getJSONArray("data");
        ArrayList<Event> events = new ArrayList<Event>(data.length() * 2);

        JSONObject object;
        for (int i = 0, z = data.length(); i < z; i++) {
            try {
                object = data.getJSONObject(i);
                events.add(new Event(object, mAccount));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return events;
    }

    public List<Account> getAccounts() throws FacebookError, JSONException {
        Bundle b = new Bundle();
        b.putString("date_format", "U");
        b.putString("limit", "3000");

        String response = null;
        try {
            response = mFacebook.request("me/accounts", b);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (null == response) {
            return null;
        }

        JSONObject document = Util.parseJson(response);

        JSONArray data = document.getJSONArray("data");
        ArrayList<Account> accounts = new ArrayList<Account>(data.length() * 2);
        accounts.add(mAccount);

        JSONObject object;
        Account account;
        for (int i = 0, z = data.length(); i < z; i++) {
            try {
                object = data.getJSONObject(i);
                account = new Account(object);
                if (account.hasAccessToken()) {
                    accounts.add(account);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return accounts;

    }

    public List<Album> getUploadableAlbums() throws FacebookError, JSONException {
        Bundle b = new Bundle();
        b.putString("date_format", "U");
        b.putString("limit", "3000");

        String response = null;
        try {
            response = mFacebook.request("me/albums", b);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (null == response) {
            return null;
        }

        JSONObject document = Util.parseJson(response);

        JSONArray data = document.getJSONArray("data");
        ArrayList<Album> albums = new ArrayList<Album>(data.length());

        JSONObject object;
        for (int i = 0, z = data.length(); i < z; i++) {
            try {
                object = data.getJSONObject(i);
                Album album = new Album(object, mAccount);
                if (album.canUpload()) {
                    albums.add(album);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return albums;
    }

    public String createNewAlbum(String albumName, String description, String privacy) {
        Bundle b = new Bundle();
        b.putString("name", albumName);

        if (!TextUtils.isEmpty(description)) {
            b.putString("message", description);
        }

        if (!TextUtils.isEmpty(privacy)) {
            try {
                JSONObject object = new JSONObject();
                object.put("value", privacy);
                b.putString("privacy", object.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        String response = null;
        try {
            response = mFacebook.request("me/albums", b, "POST");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (null == response) {
            return null;
        }

        try {
            JSONObject document = Util.parseJson(response);
            return document.getString("id");
        } catch (FacebookError e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

}
