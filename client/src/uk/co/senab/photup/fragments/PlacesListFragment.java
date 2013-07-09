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
package uk.co.senab.photup.fragments;

import com.facebook.android.FacebookError;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photup.R;
import uk.co.senab.photup.adapters.PlacesAdapter;
import uk.co.senab.photup.listeners.OnPlacePickedListener;
import uk.co.senab.photup.model.Place;
import uk.co.senab.photup.tasks.PlacesAsyncTask;
import uk.co.senab.photup.tasks.PlacesAsyncTask.PlacesResultListener;
import uk.co.senab.photup.util.Utils;

public class PlacesListFragment extends PhotupDialogFragment
        implements PlacesResultListener, OnItemClickListener,
        OnCheckedChangeListener {

    private class LocationListenerImpl implements LocationListener {

        static final int TIME_THRESHOLD = 3 * 60 * 1000; // 3 minutes
        static final int DISTANCE_THRESHOLD = 150; // 150m

        public void onLocationChanged(Location location) {
            mLastLocation = location;
            refreshPlaces();

            if (Utils.newerThan(location.getTime(), TIME_THRESHOLD)
                    && location.getAccuracy() <= DISTANCE_THRESHOLD) {
                stopLocationListeners();
            }
        }

        public void onProviderDisabled(String provider) {
            // NO-OP
        }

        public void onProviderEnabled(String provider) {
            // NO-OP
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            // NO-OP
        }

    }

    static final int QUERY_CURRENT_LOCATION = 1;

    static final int QUERY_PHOTO_LOCATION = 2;

    private final ArrayList<Place> mPlaces = new ArrayList<Place>();

    private ListView mListView;
    private PlacesAdapter mAdapter;

    private EditText mFilterEditText;
    private ProgressBar mProgressBar;
    private RadioGroup mLocationSourceRg;

    private OnPlacePickedListener mPickedPlaceListener;
    private LocationManager mLocationManager;

    private LocationListener mGpsListener;
    private LocationListener mNetworkListener;

    private Location mLastLocation;
    private Location mPhotoTagLocation;

    public void onCheckedChanged(RadioGroup group, int checkedId) {
        mPlaces.clear();
        mAdapter.notifyDataSetChanged();

        switch (checkedId) {
            case R.id.rb_place_current:
                refreshPlacesFromLastLocation();
                startLocationListeners();
                break;

            case R.id.rb_place_photo:
                stopLocationListeners();
                refreshPlaces();
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new PlacesAdapter(getActivity(), mPlaces);
        mLocationManager = (LocationManager) getActivity()
                .getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_places, container, false);

        mProgressBar = (ProgressBar) view.findViewById(R.id.pb_loading);

        mListView = (ListView) view.findViewById(R.id.lv_places);
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mAdapter);

        mFilterEditText = (EditText) view.findViewById(R.id.et_places_filter);
        mFilterEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    refreshPlaces();
                    return true;
                }
                return false;
            }
        });

        mLocationSourceRg = (RadioGroup) view.findViewById(R.id.rg_place_source);
        mLocationSourceRg.setOnCheckedChangeListener(this);

        if (null != mPhotoTagLocation) {
            RadioButton photoRb = (RadioButton) mLocationSourceRg.findViewById(R.id.rb_place_photo);
            photoRb.setVisibility(View.VISIBLE);
            mLocationSourceRg.setVisibility(View.VISIBLE);
        }

        return view;
    }

    public void onFacebookError(FacebookError e) {
        // NO-OP
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Place place = (Place) parent.getItemAtPosition(position);

        if (null != mPickedPlaceListener) {
            mPickedPlaceListener.onPlacePicked(place);
        }

        dismiss();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationListeners();
    }

    public void onPlacesLoaded(final int id, String queryString, List<Place> places) {
        if (isResultValid(id, queryString)) {
            mProgressBar.setVisibility(View.GONE);
            mPlaces.clear();
            mPlaces.addAll(places);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isCurrentLocationChecked()) {
            refreshPlacesFromLastLocation();
            startLocationListeners();
        }
    }

    public void setOnPlacePickedListener(OnPlacePickedListener listener) {
        mPickedPlaceListener = listener;
    }

    public void setPhotoTagLocation(Location location) {
        mPhotoTagLocation = location;
    }

    private String getQueryString() {
        return mFilterEditText.getText().toString();
    }

    private void hideIme() {
        Context activity = getActivity();
        if (null != mFilterEditText && null != activity) {
            InputMethodManager imm = (InputMethodManager) activity
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mFilterEditText.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private boolean isCurrentLocationChecked() {
        return mLocationSourceRg.getCheckedRadioButtonId() == R.id.rb_place_current;
    }

    private boolean isPhotoTagLocationChecked() {
        return mLocationSourceRg.getCheckedRadioButtonId() == R.id.rb_place_photo;
    }

    private boolean isResultValid(int id, String queryString) {
        return getQueryString().equals(queryString)
                && ((id == QUERY_CURRENT_LOCATION && isCurrentLocationChecked()) || (
                id == QUERY_PHOTO_LOCATION && isPhotoTagLocationChecked()));
    }

    private void refreshPlaces() {
        switch (mLocationSourceRg.getCheckedRadioButtonId()) {
            case R.id.rb_place_current:
                refreshPlaces(mLastLocation, QUERY_CURRENT_LOCATION);
                break;

            case R.id.rb_place_photo:
                refreshPlaces(mPhotoTagLocation, QUERY_PHOTO_LOCATION);
                break;
        }
    }

    private void refreshPlaces(final Location location, int id) {
        hideIme();
        if (null != mProgressBar) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
        new PlacesAsyncTask(getActivity(), this, location, getQueryString(), id).execute();
    }

    private void refreshPlacesFromLastLocation() {
        if (null == mLastLocation) {
            mLastLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (null == mLastLocation) {
                mLastLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        }
        if (null != mLastLocation) {
            refreshPlaces(mLastLocation, QUERY_CURRENT_LOCATION);
        }
    }

    private void startLocationListeners() {
        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mNetworkListener = new LocationListenerImpl();
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 5,
                    mNetworkListener);
        }
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mGpsListener = new LocationListenerImpl();
            mLocationManager
                    .requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, mGpsListener);
        }
    }

    private void stopLocationListeners() {
        if (null != mGpsListener) {
            mLocationManager.removeUpdates(mGpsListener);
            mGpsListener = null;
        }
        if (null != mNetworkListener) {
            mLocationManager.removeUpdates(mNetworkListener);
            mNetworkListener = null;
        }
    }

}
