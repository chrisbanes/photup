package uk.co.senab.photup.util;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.CursorLoader;

public class PhotupCursorLoader extends CursorLoader {

	private final boolean mRequeryOnChange;

	public PhotupCursorLoader(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder, boolean requeryOnChange) {
		super(context, uri, projection, selection, selectionArgs, sortOrder);
		mRequeryOnChange = requeryOnChange;
	}

	@Override
	public void onContentChanged() {
		if (mRequeryOnChange) {
			super.onContentChanged();
		}
	}

}
