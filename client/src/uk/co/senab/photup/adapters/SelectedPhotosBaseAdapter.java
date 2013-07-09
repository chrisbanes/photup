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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.views.PhotoItemLayout;
import uk.co.senab.photup.views.PhotupImageView;

public class SelectedPhotosBaseAdapter extends BaseAdapter {

    private List<PhotoUpload> mItems;

    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final PhotoUploadController mController;
    private final boolean mShowCheckbox;

    public SelectedPhotosBaseAdapter(Context context, boolean showCheckbox) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
        mShowCheckbox = showCheckbox;

        PhotupApplication app = PhotupApplication.getApplication(context);
        mController = app.getPhotoUploadController();
        mItems = mController.getSelected();
    }

    public int getCount() {
        return null != mItems ? mItems.size() : 0;
    }

    public long getItemId(int position) {
        return position;
    }

    public PhotoUpload getItem(int position) {
        return mItems.get(position);
    }

    public View getView(int position, View view, ViewGroup parent) {
        if (null == view) {
            view = mLayoutInflater.inflate(R.layout.item_grid_photo_selected, parent, false);
        }

        PhotoItemLayout layout = (PhotoItemLayout) view;
        PhotupImageView iv = layout.getImageView();

        final PhotoUpload upload = getItem(position);

        iv.requestThumbnail(upload, true);
        layout.setShowCaption(true);
        layout.setAnimateWhenChecked(false);
        layout.setPhotoSelection(upload);
        layout.setShowCheckbox(mShowCheckbox);

        // If we're showing the checkbox, then check the background too
        if (mShowCheckbox) {
            layout.setChecked(true);
        }

        return view;
    }

    @Override
    public void notifyDataSetChanged() {
        mItems = mController.getSelected();
        super.notifyDataSetChanged();
    }

}
