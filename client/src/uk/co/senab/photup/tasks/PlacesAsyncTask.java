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
package uk.co.senab.photup.tasks;

import com.facebook.android.FacebookError;

import org.json.JSONException;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import uk.co.senab.photup.facebook.FacebookRequester;
import uk.co.senab.photup.listeners.FacebookErrorListener;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.Place;

public class PlacesAsyncTask extends AsyncTask<Void, Void, List<Place>> {

    public static interface PlacesResultListener extends FacebookErrorListener {

        void onPlacesLoaded(int id, String query, List<Place> albums);
    }

    private final WeakReference<Context> mContext;
    private final WeakReference<PlacesResultListener> mListener;

    private final Location mLocation;
    private final String mSearchQuery;
    private final int mId;

    public PlacesAsyncTask(Context context, PlacesResultListener listener, Location location,
            String searchQuery, int id) {
        mContext = new WeakReference<Context>(context);
        mListener = new WeakReference<PlacesResultListener>(listener);

        mLocation = location;
        mSearchQuery = searchQuery;
        mId = id;
    }

    @Override
    protected List<Place> doInBackground(Void... params) {
        Context context = mContext.get();
        if (null != context) {
            Account account = Account.getAccountFromSession(context);
            if (null != account) {
                try {
                    FacebookRequester requester = new FacebookRequester(account);
                    List<Place> places = requester.getPlaces(mLocation, mSearchQuery);

                    // If we have places and a location, sort using it
                    if (null != places && !places.isEmpty() && null != mLocation) {
                        for (Place place : places) {
                            place.calculateDistanceFrom(mLocation);
                        }
                        Collections.sort(places, Place.getComparator());
                    }

                    return places;
                } catch (FacebookError e) {
                    PlacesResultListener listener = mListener.get();
                    if (null != listener) {
                        listener.onFacebookError(e);
                    } else {
                        e.printStackTrace();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<Place> result) {
        super.onPostExecute(result);

        PlacesResultListener listener = mListener.get();
        if (null != listener && null != result) {
            listener.onPlacesLoaded(mId, mSearchQuery, result);
        }
    }

}
