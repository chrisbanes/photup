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
import android.database.Cursor;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.listeners.OnPickFriendRequestListener;
import uk.co.senab.photup.listeners.OnSingleTapListener;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.util.CursorPagerAdapter;
import uk.co.senab.photup.util.MediaStoreCursorHelper;
import uk.co.senab.photup.views.MultiTouchImageView;
import uk.co.senab.photup.views.PhotoTagItemLayout;

public class UserPhotosViewPagerAdapter extends CursorPagerAdapter {

    private final PhotoUploadController mController;
    private final OnSingleTapListener mTapListener;
    private final OnPickFriendRequestListener mFriendPickRequestListener;

    public UserPhotosViewPagerAdapter(Context context, OnSingleTapListener tapListener,
            OnPickFriendRequestListener friendRequestListener) {
        super(context, null, 0);
        mTapListener = tapListener;
        mFriendPickRequestListener = friendRequestListener;

        PhotupApplication app = PhotupApplication.getApplication(context);
        mController = app.getPhotoUploadController();
    }

    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((View) object);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final PhotoUpload upload = MediaStoreCursorHelper.photosCursorToSelection(
                MediaStoreCursorHelper.MEDIA_STORE_CONTENT_URI, cursor);

        PhotoTagItemLayout view = new PhotoTagItemLayout(mContext, mController, upload,
                mFriendPickRequestListener);
        view.setPosition(cursor.getPosition());

        if (null != upload) {
            upload.setFaceDetectionListener(view);

            MultiTouchImageView imageView = view.getImageView();
            imageView.requestFullSize(upload, true, null);
            imageView.setSingleTapListener(mTapListener);
        }

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // NO-OP
    }

    @Override
    public void destroyItem(View container, int position, Object object) {
        PhotoTagItemLayout view = (PhotoTagItemLayout) object;

        MultiTouchImageView imageView = view.getImageView();
        imageView.cancelRequest();

        ((ViewPager) container).removeView(view);
    }

}
