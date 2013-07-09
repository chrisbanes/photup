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
package uk.co.senab.photup.adapters;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import java.util.List;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.listeners.OnPickFriendRequestListener;
import uk.co.senab.photup.listeners.OnSingleTapListener;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.views.MultiTouchImageView;
import uk.co.senab.photup.views.PhotoTagItemLayout;

public class SelectedPhotosViewPagerAdapter extends PagerAdapter {

    private final Context mContext;
    private final PhotoUploadController mController;
    private final OnSingleTapListener mTapListener;
    private final OnPickFriendRequestListener mFriendPickRequestListener;

    private List<PhotoUpload> mItems;

    public SelectedPhotosViewPagerAdapter(Context context, OnSingleTapListener tapListener,
            OnPickFriendRequestListener friendRequestListener) {
        mContext = context;
        mTapListener = tapListener;
        mFriendPickRequestListener = friendRequestListener;

        PhotupApplication app = PhotupApplication.getApplication(context);
        mController = app.getPhotoUploadController();

        refreshData();
    }

    @Override
    public void destroyItem(View container, int position, Object object) {
        PhotoTagItemLayout view = (PhotoTagItemLayout) object;

        MultiTouchImageView imageView = view.getImageView();
        imageView.cancelRequest();

        ((ViewPager) container).removeView(view);
    }

    @Override
    public int getCount() {
        return null != mItems ? mItems.size() : 0;
    }

    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    public PhotoUpload getItem(int position) {
        if (position >= 0 && position < getCount()) {
            return mItems.get(position);
        }
        return null;
    }

    @Override
    public Object instantiateItem(View container, int position) {
        PhotoUpload upload = mItems.get(position);

        PhotoTagItemLayout view = new PhotoTagItemLayout(mContext, mController, upload,
                mFriendPickRequestListener);
        view.setPosition(position);

        upload.setFaceDetectionListener(view);

        MultiTouchImageView imageView = view.getImageView();
        imageView.requestFullSize(upload, true, null);
        imageView.setSingleTapListener(mTapListener);

        ((ViewPager) container).addView(view);

        return view;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((View) object);
    }

    @Override
    public void notifyDataSetChanged() {
        refreshData();
        super.notifyDataSetChanged();
    }

    protected Context getContext() {
        return mContext;
    }

    private void refreshData() {
        mItems = mController.getSelected();
    }

}
