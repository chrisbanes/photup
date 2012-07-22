package uk.co.senab.photup.fragments;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photup.R;
import uk.co.senab.photup.listeners.OnPlacePickedListener;
import uk.co.senab.photup.model.Place;
import uk.co.senab.photup.tasks.PlacesAsyncTask;
import uk.co.senab.photup.tasks.PlacesAsyncTask.PlacesResultListener;
import android.app.Dialog;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.facebook.android.FacebookError;

public class PlacesListFragment extends SherlockDialogFragment implements PlacesResultListener, OnItemClickListener {

	private class LocationListenerImpl implements LocationListener {

		public void onLocationChanged(Location location) {
			mLastLocation = location;
			refreshPlaces();
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

	private final ArrayList<Place> mPlaces = new ArrayList<Place>();

	private ListView mListView;
	private EditText mFilterEditText;
	private ArrayAdapter<Place> mAdapter;

	private OnPlacePickedListener mPickedPlaceListener;
	private LocationManager mLocationManager;

	private LocationListener mGpsListener;
	private LocationListener mNetworkListener;

	private Location mLastLocation;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new ArrayAdapter<Place>(getActivity(), android.R.layout.simple_list_item_1, mPlaces);
		mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

		if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			mGpsListener = new LocationListenerImpl();
		}
		if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			mNetworkListener = new LocationListenerImpl();
		}

		mLastLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (null == mLastLocation) {
			mLastLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}

		if (null != mLastLocation) {
			refreshPlaces(null);
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.setTitle(R.string.place);
		return dialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_places, container, false);

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

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (null != mGpsListener) {
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, mGpsListener);
		}
		if (null != mNetworkListener) {
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 5, mNetworkListener);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		if (null != mGpsListener) {
			mLocationManager.removeUpdates(mGpsListener);
		}
		if (null != mNetworkListener) {
			mLocationManager.removeUpdates(mNetworkListener);
		}
	}

	public void setOnPlacePickedListener(OnPlacePickedListener listener) {
		mPickedPlaceListener = listener;
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Place place = (Place) parent.getItemAtPosition(position);

		if (null != mPickedPlaceListener) {
			mPickedPlaceListener.onPlacePicked(place);
		}

		dismiss();
	}

	public void onFacebookError(FacebookError e) {
		// NO-OP
	}

	public void onPlacesLoaded(List<Place> places) {
		mPlaces.clear();
		mPlaces.addAll(places);
		mAdapter.notifyDataSetChanged();
	}

	private void refreshPlaces() {
		refreshPlaces(mFilterEditText.getText().toString());
	}

	private void refreshPlaces(String query) {
		new PlacesAsyncTask(getActivity(), this, mLastLocation, query).execute();
	}

}
