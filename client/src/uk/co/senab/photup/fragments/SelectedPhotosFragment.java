/*
 * Copyright 2013 Chris Banes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.senab.photup.fragments;

import com.example.android.swipedismiss.SwipeDismissListViewTouchListener;
import com.example.android.swipedismiss.SwipeDismissListViewTouchListener.OnDismissCallback;

import android.app.Activity;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import de.greenrobot.event.EventBus;
import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotoViewerActivity;
import uk.co.senab.photup.R;
import uk.co.senab.photup.adapters.SelectedPhotosBaseAdapter;
import uk.co.senab.photup.events.PhotoSelectionAddedEvent;
import uk.co.senab.photup.events.PhotoSelectionRemovedEvent;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.util.Utils;

public class SelectedPhotosFragment extends AbstractPhotosFragment
        implements OnItemClickListener, OnDismissCallback {

    private GridView mGridView;
    private SelectedPhotosBaseAdapter mAdapter;
    private PhotoUploadController mPhotoSelectionController;

    @Override
    public void onAttach(Activity activity) {
        mPhotoSelectionController = PhotoUploadController.getFromContext(activity);
        super.onAttach(activity);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater
                .inflate(R.layout.fragment_selected_photos, container, false);
        mGridView = (GridView) view.findViewById(R.id.gv_photos);
        mGridView.setOnItemClickListener(this);

        final boolean swipeToDismiss = getResources().getBoolean(R.bool.swipe_selected);
        // Check if we're set to swipe
        if (swipeToDismiss) {
            SwipeDismissListViewTouchListener swipeListener = new SwipeDismissListViewTouchListener(
                    mGridView, this);
            mGridView.setOnTouchListener(swipeListener);
            mGridView.setOnScrollListener(swipeListener.makeScrollListener());
        }

        mAdapter = new SelectedPhotosBaseAdapter(getActivity(), !swipeToDismiss);
        mGridView.setAdapter(mAdapter);
        mGridView.setEmptyView(view.findViewById(android.R.id.empty));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // TODO Save Scroll position
    }

    public void onEvent(PhotoSelectionAddedEvent event) {
        mAdapter.notifyDataSetChanged();
    }

    public void onEvent(PhotoSelectionRemovedEvent event) {
        mAdapter.notifyDataSetChanged();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Bundle b = null;
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeThumbnailScaleUpAnimation(view,
                            Utils.drawViewOntoBitmap(view), 0, 0);
            b = options.toBundle();
        }

        Intent intent = new Intent(getActivity(), PhotoViewerActivity.class);
        intent.putExtra(PhotoViewerActivity.EXTRA_POSITION, position);
        intent.putExtra(PhotoViewerActivity.EXTRA_MODE, PhotoViewerActivity.MODE_SELECTED_VALUE);

        ActivityCompat.startActivity(getActivity(), intent, b);
    }

    public boolean canDismiss(AbsListView listView, int position) {
        // All can be swiped
        return true;
    }

    public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
        try {
            for (int i = 0, z = reverseSortedPositions.length; i < z; i++) {
                PhotoUpload upload = (PhotoUpload) listView
                        .getItemAtPosition(reverseSortedPositions[i]);
                mPhotoSelectionController.removeSelection(upload);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mAdapter.notifyDataSetChanged();
    }

}