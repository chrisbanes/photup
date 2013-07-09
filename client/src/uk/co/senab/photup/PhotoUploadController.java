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
package uk.co.senab.photup;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import de.greenrobot.event.EventBus;
import uk.co.senab.photup.events.PhotoSelectionAddedEvent;
import uk.co.senab.photup.events.PhotoSelectionRemovedEvent;
import uk.co.senab.photup.events.UploadsModifiedEvent;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.FbUser;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.model.Place;
import uk.co.senab.photup.model.UploadQuality;
import uk.co.senab.photup.util.PhotoUploadDatabaseHelper;

public class PhotoUploadController {

    public static PhotoUploadController getFromContext(Context context) {
        return PhotupApplication.getApplication(context).getPhotoUploadController();
    }

    private static List<PhotoUpload> checkListForInvalid(final Context context,
            final List<PhotoUpload> uploads) {
        ArrayList<PhotoUpload> toBeRemoved = null;

        for (PhotoUpload upload : uploads) {
            if (!upload.isValid(context)) {
                if (null == toBeRemoved) {
                    toBeRemoved = new ArrayList<PhotoUpload>();
                }
                toBeRemoved.add(upload);
            }
        }

        if (null != toBeRemoved) {
            uploads.removeAll(toBeRemoved);

            // Delete from Database
            if (Flags.ENABLE_DB_PERSISTENCE) {
                PhotoUploadDatabaseHelper.deleteFromDatabase(context, toBeRemoved);
            }
        }
        return toBeRemoved;
    }

    private final Context mContext;
    private final ArrayList<PhotoUpload> mSelectedPhotoList;

    private final ArrayList<PhotoUpload> mUploadingList;

    PhotoUploadController(Context context) {
        mContext = context;
        mSelectedPhotoList = new ArrayList<PhotoUpload>();
        mUploadingList = new ArrayList<PhotoUpload>();

        populateFromDatabase();
    }

    public synchronized boolean addSelection(final PhotoUpload selection) {
        boolean result = false;

        if (!mSelectedPhotoList.contains(selection)) {
            selection.setUploadState(PhotoUpload.STATE_SELECTED);
            mSelectedPhotoList.add(selection);

            // Save to Database
            if (Flags.ENABLE_DB_PERSISTENCE) {
                PhotoUploadDatabaseHelper.saveToDatabase(mContext, selection);
            }

            postEvent(new PhotoSelectionAddedEvent(selection));
            result = true;
        }

        // Remove it from Upload list if it's there
        if (mUploadingList.contains(selection)) {
            mUploadingList.remove(selection);
            postEvent(new UploadsModifiedEvent());
        }

        return result;
    }

    public synchronized void addSelections(List<PhotoUpload> selections) {
        final HashSet<PhotoUpload> currentSelectionsSet = new HashSet<PhotoUpload>(
                mSelectedPhotoList);
        final HashSet<PhotoUpload> currentUploadSet = new HashSet<PhotoUpload>(mUploadingList);
        boolean listModified = false;

        for (final PhotoUpload selection : selections) {
            if (!currentSelectionsSet.contains(selection)) {

                // Remove it from Upload list if it's there
                if (currentUploadSet.contains(selection)) {
                    mUploadingList.remove(selection);
                }

                selection.setUploadState(PhotoUpload.STATE_SELECTED);
                mSelectedPhotoList.add(selection);
                listModified = true;
            }
        }

        if (listModified) {
            // Save to Database
            if (Flags.ENABLE_DB_PERSISTENCE) {
                PhotoUploadDatabaseHelper.saveToDatabase(mContext, mSelectedPhotoList, true);
            }

            postEvent(new PhotoSelectionAddedEvent(selections));
        }
    }

    public boolean addUpload(PhotoUpload selection) {
        if (null != selection && selection.isValid(mContext)) {
            synchronized (this) {
                if (!mUploadingList.contains(selection)) {
                    selection.setUploadState(PhotoUpload.STATE_UPLOAD_WAITING);

                    // Save to Database
                    if (Flags.ENABLE_DB_PERSISTENCE) {
                        PhotoUploadDatabaseHelper.saveToDatabase(mContext, selection);
                    }

                    mUploadingList.add(selection);
                    mSelectedPhotoList.remove(selection);

                    postEvent(new UploadsModifiedEvent());
                    return true;
                }
            }
        }

        return false;
    }

    public synchronized void addUploadsFromSelected(final Account account, final String targetId,
            final UploadQuality quality, final Place place) {

        // Check The Selected List to make sure they're all valid
        checkSelectedForInvalid(false);

        for (PhotoUpload upload : mSelectedPhotoList) {
            upload.setUploadParams(account, targetId, quality);
            upload.setUploadState(PhotoUpload.STATE_UPLOAD_WAITING);

            if (null != place) {
                upload.setPlace(place);
            }
        }

        // Update Database
        if (Flags.ENABLE_DB_PERSISTENCE) {
            PhotoUploadDatabaseHelper.saveToDatabase(mContext, mSelectedPhotoList, true);
        }

        ArrayList<PhotoUpload> eventResult = new ArrayList<PhotoUpload>(mSelectedPhotoList);

        mUploadingList.addAll(mSelectedPhotoList);
        mSelectedPhotoList.clear();

        postEvent(new PhotoSelectionRemovedEvent(eventResult));
        postEvent(new UploadsModifiedEvent());
    }

    public synchronized void clearSelected() {
        if (!mSelectedPhotoList.isEmpty()) {

            // Delete from Database
            if (Flags.ENABLE_DB_PERSISTENCE) {
                PhotoUploadDatabaseHelper.deleteAllSelected(mContext);
            }

            // Reset States (as may still be in cache)
            for (PhotoUpload upload : mSelectedPhotoList) {
                upload.setUploadState(PhotoUpload.STATE_NONE);
            }

            ArrayList<PhotoUpload> eventResult = new ArrayList<PhotoUpload>(mSelectedPhotoList);

            // Clear from memory
            mSelectedPhotoList.clear();

            postEvent(new PhotoSelectionRemovedEvent(eventResult));
        }
    }

    public synchronized int getActiveUploadsCount() {
        int count = 0;
        for (PhotoUpload upload : mUploadingList) {
            if (upload.getUploadState() != PhotoUpload.STATE_UPLOAD_COMPLETED) {
                count++;
            }
        }
        return count;
    }

    public synchronized PhotoUpload getNextUpload() {
        for (PhotoUpload selection : mUploadingList) {
            if (selection.getUploadState() == PhotoUpload.STATE_UPLOAD_WAITING) {
                return selection;
            }
        }
        return null;
    }

    public synchronized List<PhotoUpload> getSelected() {
        checkSelectedForInvalid(true);
        return new ArrayList<PhotoUpload>(mSelectedPhotoList);
    }

    public synchronized int getSelectedCount() {
        return mSelectedPhotoList.size();
    }

    public synchronized List<PhotoUpload> getUploadingUploads() {
        return new ArrayList<PhotoUpload>(mUploadingList);
    }

    public synchronized int getUploadsCount() {
        return mUploadingList.size();
    }

    public synchronized boolean hasSelections() {
        return !mSelectedPhotoList.isEmpty();
    }

    public synchronized boolean hasSelectionsWithPlace() {
        for (PhotoUpload selection : mSelectedPhotoList) {
            if (selection.hasPlace()) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean hasUploads() {
        return !mUploadingList.isEmpty();
    }

    public synchronized boolean hasWaitingUploads() {
        for (PhotoUpload upload : mUploadingList) {
            if (upload.getUploadState() == PhotoUpload.STATE_UPLOAD_WAITING) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isOnUploadList(PhotoUpload selection) {
        return mUploadingList.contains(selection);
    }

    public synchronized boolean isSelected(PhotoUpload selection) {
        return mSelectedPhotoList.contains(selection);
    }

    public synchronized boolean moveFailedToSelected() {
        boolean result = false;

        final Iterator<PhotoUpload> iterator = mUploadingList.iterator();
        PhotoUpload upload;

        while (iterator.hasNext()) {
            upload = iterator.next();

            if (upload.getUploadState() == PhotoUpload.STATE_UPLOAD_ERROR) {
                // Reset State and add to selection list
                upload.setUploadState(PhotoUpload.STATE_SELECTED);
                mSelectedPhotoList.add(upload);
                postEvent(new PhotoSelectionAddedEvent(upload));

                // Remove from Uploading list
                iterator.remove();
                result = true;
            }
        }

        if (result) {
            // Update Database, but don't force update
            if (Flags.ENABLE_DB_PERSISTENCE) {
                PhotoUploadDatabaseHelper.saveToDatabase(mContext, mSelectedPhotoList, false);
            }

            postEvent(new UploadsModifiedEvent());
        }

        return result;
    }

    public boolean removeSelection(final PhotoUpload selection) {
        boolean removed = false;
        synchronized (this) {
            removed = mSelectedPhotoList.remove(selection);
        }

        if (removed) {
            // Delete from Database
            if (Flags.ENABLE_DB_PERSISTENCE) {
                PhotoUploadDatabaseHelper.deleteFromDatabase(mContext, selection);
            }

            // Reset State (as may still be in cache)
            selection.setUploadState(PhotoUpload.STATE_NONE);

            postEvent(new PhotoSelectionRemovedEvent(selection));
        }

        return removed;
    }

    public void removeUpload(final PhotoUpload selection) {
        boolean removed = false;
        synchronized (this) {
            removed = mUploadingList.remove(selection);
        }

        if (removed) {
            // Delete from Database
            if (Flags.ENABLE_DB_PERSISTENCE) {
                PhotoUploadDatabaseHelper.deleteFromDatabase(mContext, selection);
            }

            // Reset State (as may still be in cache)
            selection.setUploadState(PhotoUpload.STATE_NONE);

            postEvent(new UploadsModifiedEvent());
        }
    }

    public void reset() {
        // Clear the cache
        PhotoUpload.clearCache();

        synchronized (this) {
            // Clear the internal lists
            mSelectedPhotoList.clear();
            mUploadingList.clear();
        }

        // Finally delete the database
        mContext.deleteDatabase(DatabaseHelper.DATABASE_NAME);
    }

    public synchronized void updateDatabase() {
        if (Flags.ENABLE_DB_PERSISTENCE) {
            PhotoUploadDatabaseHelper.saveToDatabase(mContext, mSelectedPhotoList, false);
            PhotoUploadDatabaseHelper.saveToDatabase(mContext, mUploadingList, false);
        }
    }

    void populateDatabaseItemsFromAccounts(HashMap<String, Account> accounts) {
        if (!mSelectedPhotoList.isEmpty()) {
            for (PhotoUpload upload : mSelectedPhotoList) {
                upload.populateFromAccounts(accounts);
            }
        }
        if (!mUploadingList.isEmpty()) {
            for (PhotoUpload upload : mUploadingList) {
                upload.populateFromAccounts(accounts);
            }
        }
    }

    void populateDatabaseItemsFromFriends(HashMap<String, FbUser> friends) {
        if (!mSelectedPhotoList.isEmpty()) {
            for (PhotoUpload upload : mSelectedPhotoList) {
                upload.populateFromFriends(friends);
            }
        }
        if (!mUploadingList.isEmpty()) {
            for (PhotoUpload upload : mUploadingList) {
                upload.populateFromFriends(friends);
            }
        }
    }

    void populateFromDatabase() {
        if (Flags.ENABLE_DB_PERSISTENCE) {
            final List<PhotoUpload> selectedFromDb = PhotoUploadDatabaseHelper
                    .getSelected(mContext);
            if (null != selectedFromDb) {
                mSelectedPhotoList.addAll(selectedFromDb);
                checkSelectedForInvalid(false);
                PhotoUpload.populateCache(selectedFromDb);
            }

            final List<PhotoUpload> uploadsFromDb = PhotoUploadDatabaseHelper.getUploads(mContext);
            if (null != uploadsFromDb) {
                mUploadingList.addAll(uploadsFromDb);
                checkUploadsForInvalid(false);
                PhotoUpload.populateCache(uploadsFromDb);
            }
        }
    }

    private void checkSelectedForInvalid(final boolean sendEvent) {
        if (!mSelectedPhotoList.isEmpty()) {
            List<PhotoUpload> removedUploads = checkListForInvalid(mContext, mSelectedPhotoList);

            // Delete from Database
            if (Flags.ENABLE_DB_PERSISTENCE) {
                PhotoUploadDatabaseHelper.deleteAllSelected(mContext);
            }

            if (sendEvent && null != removedUploads) {
                postEvent(new PhotoSelectionRemovedEvent(removedUploads));
            }
        }
    }

    private void checkUploadsForInvalid(final boolean sendEvent) {
        if (!mUploadingList.isEmpty()) {
            List<PhotoUpload> removedUploads = checkListForInvalid(mContext, mUploadingList);

            // Delete from Database
            if (Flags.ENABLE_DB_PERSISTENCE) {
                PhotoUploadDatabaseHelper.deleteAllSelected(mContext);
            }

            if (sendEvent && null != removedUploads) {
                postEvent(new UploadsModifiedEvent());
            }
        }
    }

    private void postEvent(Object event) {
        EventBus.getDefault().post(event);
    }

}
