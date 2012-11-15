package uk.co.senab.photup.model;

import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

public class Place extends AbstractFacebookObject {

	static final double EARTH_RADIUS_KM = 6371;

	private final double mLatitude, mLongitude;
	private final String mCategory;
	
	private int mDistanceFromLocation;

	public Place(JSONObject object, Account account) throws JSONException {
		super(object, account);
		mCategory = object.getString("category");

		JSONObject location = object.getJSONObject("location");
		mLatitude = location.getDouble("latitude");
		mLongitude = location.getDouble("longitude");
	}
	
	public String getCategory() {
		return mCategory;
	}

	public void calculateDistanceFrom(Location location) {
		float[] results = new float[1];
		Location.distanceBetween(mLatitude, mLongitude, location.getLatitude(), location.getLongitude(), results);
		mDistanceFromLocation = Math.round(results[0]);
	}
	
	public int getDistanceFromLocation() {
		return mDistanceFromLocation;
	}

	public static Comparator<Place> getComparator() {
		return new Comparator<Place>() {
			public int compare(Place lhs, Place rhs) {
				return lhs.mDistanceFromLocation - rhs.mDistanceFromLocation;
			}
		};
	}

}
