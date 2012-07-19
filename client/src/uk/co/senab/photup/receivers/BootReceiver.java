package uk.co.senab.photup.receivers;

import uk.co.senab.photup.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Utils.startInstantUploadService(context);
	}

}
