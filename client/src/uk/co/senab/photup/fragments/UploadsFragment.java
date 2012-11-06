package uk.co.senab.photup.fragments;

import uk.co.senab.photup.Flags;
import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.R;
import uk.co.senab.photup.adapters.UploadsListBaseAdapter;
import uk.co.senab.photup.events.PhotoSelectionRemovedEvent;
import uk.co.senab.photup.model.PhotoUpload;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.example.android.swipedismiss.SwipeDismissListViewTouchListener;
import com.example.android.swipedismiss.SwipeDismissListViewTouchListener.OnDismissCallback;

import de.greenrobot.event.EventBus;

public class UploadsFragment extends PhotupDialogFragment implements OnDismissCallback, OnItemClickListener {

	private PhotoUploadController mPhotoSelectionController;
	private UploadsListBaseAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EventBus.getDefault().register(this);

		mAdapter = new UploadsListBaseAdapter(getActivity());
		mPhotoSelectionController = PhotoUploadController.getFromContext(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_uploads, container, false);

		ListView listView = (ListView) view.findViewById(android.R.id.list);
		SwipeDismissListViewTouchListener swipeListener = new SwipeDismissListViewTouchListener(listView, this);

		listView.setOnItemClickListener(this);
		listView.setOnTouchListener(swipeListener);
		listView.setOnScrollListener(swipeListener.makeScrollListener());
		listView.setSelector(R.drawable.selectable_background_photup);
		listView.setAdapter(mAdapter);

		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		EventBus.getDefault().unregister(this);
	}

	public void onItemClick(AdapterView<?> l, View view, int position, long id) {
		PhotoUpload upload = (PhotoUpload) l.getItemAtPosition(position);
		if (null != upload && upload.getUploadState() == PhotoUpload.STATE_UPLOAD_COMPLETED) {

			String postId = upload.getResultPostId();
			if (null != postId) {
				final Intent intent = new Intent(Intent.ACTION_VIEW);

				try {
					intent.setData(Uri.parse("fb://post/" + postId));
					startActivity(intent);
					return;
				} catch (Exception e) {
					// Facebook not installed
				}

				try {
					intent.setData(Uri.parse("fplusfree://post?id=" + postId));
					startActivity(intent);
					return;
				} catch (Exception e) {
					// Friendcaster Free not installed
				}

				try {
					intent.setData(Uri.parse("fplus://post?id=" + postId));
					startActivity(intent);
					return;
				} catch (Exception e) {
					// Friendcaster Pro not installed
				}
			}
		}
	}

	public void onEvent(PhotoSelectionRemovedEvent event) {
		mAdapter.notifyDataSetChanged();
	}

	public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
		try {
			for (int i = 0, z = reverseSortedPositions.length; i < z; i++) {
				PhotoUpload upload = (PhotoUpload) listView.getItemAtPosition(reverseSortedPositions[i]);
				mPhotoSelectionController.removeUpload(upload);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		mAdapter.notifyDataSetChanged();

		// If we're now empty, and we're in a dialog, dismiss us
		if (mAdapter.isEmpty() && getShowsDialog()) {
			dismiss();
		}
	}

	public boolean canDismiss(AbsListView listView, int position) {
		try {
			PhotoUpload upload = (PhotoUpload) listView.getItemAtPosition(position);
			if (Flags.DEBUG) {
				Log.d("UploadsFragment", "canDismiss. State: " + upload.getUploadState());
			}
			return upload.getUploadState() != PhotoUpload.STATE_UPLOAD_IN_PROGRESS;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
