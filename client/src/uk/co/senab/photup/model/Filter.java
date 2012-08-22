package uk.co.senab.photup.model;

import uk.co.senab.photup.R;

public enum Filter {

	FILTER_ORIGINAL(R.string.filter_original),
	FILTER_INSTAFIX(R.string.filter_instafix),
	FILTER_ANSEL(R.string.filter_ansel),
	FILTER_TESTINO(R.string.filter_testino),
	FILTER_XPRO(R.string.filter_xpro),
	FILTER_RETRO(R.string.filter_retro),
	FILTER_BW(R.string.filter_bw),
	FILTER_SEPIA(R.string.filter_sepia),
	FILTER_CYANO(R.string.filter_cyano),
	FILTER_GEORGIA(R.string.filter_georgia),
	FILTER_SAHARA(R.string.filter_sahara),
	FILTER_HDR(R.string.filter_hdr);

	private final int mLabelId;

	private Filter(int labelId) {
		mLabelId = labelId;
	}

	public int getLabelId() {
		return mLabelId;
	}

	public int getId() {
		return ordinal();
	}

	public static Filter mapFromId(int id) {
		try {
			return values()[id];
		} catch (Exception e) {
			return null;
		}
	}

	public static Filter mapFromPref(String preference) {
		try {
			return Filter.valueOf(preference);
		} catch (Exception e) {
			return null;
		}
	}
}
