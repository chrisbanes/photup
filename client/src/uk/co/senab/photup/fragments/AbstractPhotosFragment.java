package uk.co.senab.photup.fragments;

import uk.co.senab.photup.R;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class AbstractPhotosFragment extends SherlockFragment {

	public void setFragmentTitleVisibility(final int visibilty) {
		TextView tv = (TextView) getView().findViewById(R.id.tv_fragment_title);
		if (null != tv) {
			tv.setVisibility(visibilty);
		}
	}

	public void setFragmentTitle(CharSequence title) {
		TextView tv = (TextView) getView().findViewById(R.id.tv_fragment_title);
		if (null != tv) {
			tv.setVisibility(View.VISIBLE);
			tv.setText(title);
		}
	}

}
