package uk.co.senab.photup.fragments;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photup.R;
import uk.co.senab.photup.adapters.PlacesAdapter;
import uk.co.senab.photup.listeners.OnPlacePickedListener;
import uk.co.senab.photup.model.Place;
import uk.co.senab.photup.tasks.PlacesAsyncTask;
import uk.co.senab.photup.tasks.PlacesAsyncTask.PlacesResultListener;
import uk.co.senab.photup.util.Utils;
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
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.facebook.android.FacebookError;

public class PlacesListFragment extends SherlockDialogFragment implements PlacesResultListener, OnItemClickListener {

	private class LocationListenerImpl implements LocationListener {

		static final int TIME_THRESHOLD = 3 * 60 * 1000; // 3 minutes
		static final int DISTANCE_THRESHOLD = 150; // 150m

		public void onLocationChanged(Location location) {
			mLastLocation = location;
			refreshPlaces();

			if (Utils.newerThan(location.getTime(), TIME_THRESHOLD) && location.getAccuracy() <= DISTANCE_THRESHOLD) {
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

	private final ArrayList<Place> mPlaces = new ArrayList<Place>();

	private ListView mListView;
	private EditText mFilterEditText;
	private ProgressBar mProgressBar;

	private PlacesAdapter mAdapter;

	private OnPlacePickedListener mPickedPlaceListener;
	private LocationManager mLocationManager;

	private LocationListener mGpsListener;
	private LocationListener mNetworkListener;

	private Location mLastLocation;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setStyle(STYLE_NORMAL, R.style.Theme_Sherlock_Dialog);

		mAdapter = new PlacesAdapter(getActivity(), mPlaces);
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

		Window window = dialog.getWindow();
		window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

		return dialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
		stopLocationListeners();
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
		mProgressBar.setVisibility(View.GONE);

		mPlaces.clear();
		mPlaces.addAll(places);
		mAdapter.notifyDataSetChanged();
	}

	private void hideIme() {
		Context activity = getActivity();
		if (null != mFilterEditText && null != activity) {
			InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mFilterEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

	private void refreshPlaces() {
		refreshPlaces(mFilterEditText.getText().toString());
	}

	private void refreshPlaces(String query) {
		hideIme();
		if (null != mProgressBar) {
			mProgressBar.setVisibility(View.VISIBLE);
		}
		new PlacesAsyncTask(getActivity(), this, mLastLocation, query).execute();
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
