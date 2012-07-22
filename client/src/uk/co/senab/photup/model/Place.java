package uk.co.senab.photup.model;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

public class Place extends AbstractFacebookObject {

	static final double EARTH_RADIUS_KM = 6371;

	private final double mLatitude, mLongitude;

	public Place(JSONObject object) throws JSONException {
		super(object);

		JSONObject location = object.getJSONObject("location");
		mLatitude = location.getDouble("latitude");
		mLongitude = location.getDouble("longitude");
	}

	public int distanceFrom(Location location) {
		float[] results = new float[1];
		Location.distanceBetween(mLatitude, mLongitude, location.getLatitude(), location.getLongitude(), results);
		return Math.round(results[0]);
	}

}
