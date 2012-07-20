package uk.co.senab.photup.model;

import uk.co.senab.photup.R;

public class Filter {

	public static final int FILTER_ORIGINAL = 0;
	public static final int FILTER_INSTAFIX = 1;
	public static final int FILTER_ANSEL = 2;
	public static final int FILTER_TESTINO = 3;
	public static final int FILTER_XPRO = 4;
	public static final int FILTER_RETRO = 5;
	public static final int FILTER_BW = 6;
	public static final int FILTER_SEPIA = 7;
	public static final int FILTER_CYANO = 8;
	public static final int FILTER_GEORGIA = 9;
	public static final int FILTER_SAHARA = 10;
	public static final int FILTER_HDR = 11;

	public static final Filter[] FILTERS = new Filter[] { new Filter(FILTER_ORIGINAL, R.string.filter_original),
			new Filter(FILTER_INSTAFIX, R.string.filter_instafix), new Filter(FILTER_ANSEL, R.string.filter_ansel),
			new Filter(FILTER_TESTINO, R.string.filter_testino), new Filter(FILTER_XPRO, R.string.filter_xpro),
			new Filter(FILTER_RETRO, R.string.filter_retro), new Filter(FILTER_BW, R.string.filter_bw),
			new Filter(FILTER_SEPIA, R.string.filter_sepia), new Filter(FILTER_CYANO, R.string.filter_cyano),
			new Filter(FILTER_GEORGIA, R.string.filter_georgia), new Filter(FILTER_SAHARA, R.string.filter_sahara),
			new Filter(FILTER_HDR, R.string.filter_hdr) };

	private final int mId;
	private final int mLabelId;

	private Filter(int id, int labelId) {
		mId = id;
		mLabelId = labelId;
	}

	public int getId() {
		return mId;
	}

	public int getLabelId() {
		return mLabelId;
	}

	public static Filter mapFromPref(String preference) {
		int id;
		try {
			id = Integer.parseInt(preference);
		} catch (Exception e) {
			id = FILTER_ORIGINAL;
		}

		return FILTERS[id];
	}

}
