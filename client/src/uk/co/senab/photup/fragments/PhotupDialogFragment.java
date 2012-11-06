package uk.co.senab.photup.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class PhotupDialogFragment extends SherlockDialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		return dialog;
	}

}
