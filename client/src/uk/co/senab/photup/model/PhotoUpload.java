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
package uk.co.senab.photup.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.lightbox.android.photoprocessing.PhotoProcessing;
import com.lightbox.android.photoprocessing.utils.BitmapUtils;
import com.lightbox.android.photoprocessing.utils.BitmapUtils.BitmapSize;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;
import android.location.Location;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.Images.Thumbnails;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import de.greenrobot.event.EventBus;
import uk.co.senab.photup.Constants;
import uk.co.senab.photup.Flags;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.events.UploadStateChangedEvent;
import uk.co.senab.photup.events.UploadsModifiedEvent;
import uk.co.senab.photup.listeners.OnFaceDetectionListener;
import uk.co.senab.photup.listeners.OnPhotoTagsChangedListener;
import uk.co.senab.photup.util.Utils;

@DatabaseTable(tableName = "photo_upload")
public class PhotoUpload {

    private static final HashMap<Uri, PhotoUpload> SELECTION_CACHE
            = new HashMap<Uri, PhotoUpload>();

    public static final int STATE_UPLOAD_COMPLETED = 5;
    public static final int STATE_UPLOAD_ERROR = 4;
    public static final int STATE_UPLOAD_IN_PROGRESS = 3;
    public static final int STATE_UPLOAD_WAITING = 2;
    public static final int STATE_SELECTED = 1;
    public static final int STATE_NONE = 0;

    public static final String FIELD_STATE = "state";
    static final String FIELD_URI = "uri";
    static final String FIELD_COMPLETED_DETECTION = "tag_detection";
    static final String FIELD_USER_ROTATION = "user_rotation";
    static final String FIELD_FILTER = "filter";
    static final String FIELD_CROP_L = "crop_l";
    static final String FIELD_CROP_T = "crop_t";
    static final String FIELD_CROP_R = "crop_r";
    static final String FIELD_CROP_B = "crop_b";
    static final String FIELD_ACCOUNT_ID = "acc_id";
    static final String FIELD_TARGET_ID = "target_id";
    static final String FIELD_QUALITY = "quality";
    static final String FIELD_RESULT_POST_ID = "r_post_id";
    static final String FIELD_CAPTION = "caption";
    static final String FIELD_TAGS_JSON = "tags";
    static final String FIELD_PLACE_NAME = "place_name";
    static final String FIELD_PLACE_ID = "place_id";

    static final String LOG_TAG = "PhotoUpload";
    static final float CROP_THRESHOLD = 0.01f; // 1%
    static final int MINI_THUMBNAIL_SIZE = 300;
    static final int MICRO_THUMBNAIL_SIZE = 96;
    static final float MIN_CROP_VALUE = 0.0f;
    static final float MAX_CROP_VALUE = 1.0f;

    public static PhotoUpload getSelection(Uri uri) {
        // Check whether we've already got a Selection cached
        PhotoUpload item = SELECTION_CACHE.get(uri);

        if (null == item) {
            item = new PhotoUpload(uri);
            SELECTION_CACHE.put(uri, item);
        }

        return item;
    }

    public static void clearCache() {
        SELECTION_CACHE.clear();
    }

    public static void populateCache(List<PhotoUpload> uploads) {
        for (PhotoUpload upload : uploads) {
            SELECTION_CACHE.put(upload.getOriginalPhotoUri(), upload);
        }
    }

    public static PhotoUpload getSelection(Uri baseUri, long id) {
        return getSelection(Uri.withAppendedPath(baseUri, String.valueOf(id)));
    }

    private static boolean checkCropValues(float left, float top, float right, float bottom) {
        return Math.max(left, top) >= (MIN_CROP_VALUE + CROP_THRESHOLD)
                || Math.min(right, bottom) <= (MAX_CROP_VALUE - CROP_THRESHOLD);
    }

    private static float santizeCropValue(float value) {
        return Math.min(1f, Math.max(0f, value));
    }

    /**
     * Uri and Database Key
     */
    private Uri mFullUri;
    @DatabaseField(columnName = FIELD_URI, id = true)
    private String mFullUriString;

    /**
     * Edit Variables
     */
    @DatabaseField(columnName = FIELD_COMPLETED_DETECTION)
    private boolean mCompletedDetection;
    @DatabaseField(columnName = FIELD_USER_ROTATION)
    private int mUserRotation;
    @DatabaseField(columnName = FIELD_FILTER)
    private Filter mFilter;
    @DatabaseField(columnName = FIELD_CROP_L)
    private float mCropLeft;
    @DatabaseField(columnName = FIELD_CROP_T)
    private float mCropTop;
    @DatabaseField(columnName = FIELD_CROP_R)
    private float mCropRight;
    @DatabaseField(columnName = FIELD_CROP_B)
    private float mCropBottom;

    /**
     * Upload Variables
     */
    @DatabaseField(columnName = FIELD_ACCOUNT_ID)
    private String mAccountId;
    @DatabaseField(columnName = FIELD_TARGET_ID)
    private String mTargetId;
    @DatabaseField(columnName = FIELD_QUALITY)
    private UploadQuality mQuality;
    @DatabaseField(columnName = FIELD_RESULT_POST_ID)
    private String mResultPostId;
    @DatabaseField(columnName = FIELD_STATE)
    private int mState;
    @DatabaseField(columnName = FIELD_CAPTION)
    private String mCaption;
    @DatabaseField(columnName = FIELD_TAGS_JSON, useGetSet = true)
    String tagJson;
    @DatabaseField(columnName = FIELD_PLACE_NAME)
    private String mPlaceName;
    @DatabaseField(columnName = FIELD_PLACE_ID)
    private String mPlaceId;

    private HashSet<PhotoTag> mTags;
    private Account mAccount;
    private int mProgress;
    private Bitmap mBigPictureNotificationBmp;

    /**
     * Listeners
     */
    private WeakReference<OnFaceDetectionListener> mFaceDetectListener;
    private WeakReference<OnPhotoTagsChangedListener> mTagChangedListener;

    private boolean mNeedsSaveFlag = false;

    PhotoUpload() {
        // NO-Arg for Ormlite
    }

    private PhotoUpload(Uri uri) {
        mFullUri = uri;
        mFullUriString = uri.toString();
        reset();
    }

    public void addPhotoTag(PhotoTag tag) {
        if (null == mTags) {
            mTags = new HashSet<PhotoTag>();
        }
        mTags.add(tag);
        notifyTagListener(tag, true);

        setRequiresSaveFlag();
    }

    public boolean beenCropped() {
        return checkCropValues(mCropLeft, mCropTop, mCropRight, mCropBottom);
    }

    public boolean beenFiltered() {
        return null != mFilter && mFilter != Filter.ORIGINAL;
    }

    public boolean requiresNativeEditing(Context context) {
        return beenCropped() || beenFiltered() || getTotalRotation(context) != 0;
    }

    public void detectPhotoTags(final Bitmap originalBitmap) {
        // If we've already done Face detection, don't do it again...
        if (mCompletedDetection) {
            return;
        }

        final OnFaceDetectionListener listener = mFaceDetectListener.get();
        if (null != listener) {
            listener.onFaceDetectionStarted(this);
        }

        final int bitmapWidth = originalBitmap.getWidth();
        final int bitmapHeight = originalBitmap.getHeight();

        Bitmap bitmap = originalBitmap;

        // The Face detector only accepts 565 bitmaps, so create one if needed
        if (Bitmap.Config.RGB_565 != bitmap.getConfig()) {
            bitmap = originalBitmap.copy(Bitmap.Config.RGB_565, false);
        }

        final FaceDetector detector = new FaceDetector(bitmapWidth, bitmapHeight,
                Constants.FACE_DETECTOR_MAX_FACES);
        final FaceDetector.Face[] faces = new FaceDetector.Face[Constants.FACE_DETECTOR_MAX_FACES];
        final int detectedFaces = detector.findFaces(bitmap, faces);

        // We must have created a converted 565 bitmap
        if (bitmap != originalBitmap) {
            bitmap.recycle();
            bitmap = null;
        }

        if (Flags.DEBUG) {
            Log.d(LOG_TAG, "Detected Faces: " + detectedFaces);
        }

        FaceDetector.Face face;
        final PointF point = new PointF();
        for (int i = 0, z = faces.length; i < z; i++) {
            face = faces[i];
            if (null != face) {
                if (Flags.DEBUG) {
                    Log.d(LOG_TAG, "Detected Face with confidence: " + face.confidence());
                }
                face.getMidPoint(point);
                addPhotoTag(new PhotoTag(point.x, point.y, bitmapWidth, bitmapWidth));
            }
        }

        if (null != listener) {
            listener.onFaceDetectionFinished(this);
        }
        mFaceDetectListener = null;

        mCompletedDetection = true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PhotoUpload) {
            return getOriginalPhotoUri().equals(((PhotoUpload) obj).getOriginalPhotoUri());
        }
        return false;
    }

    public boolean isValid(Context context) {
        final String path = Utils
                .getPathFromContentUri(context.getContentResolver(), getOriginalPhotoUri());
        if (null != path) {
            File file = new File(path);
            return file.exists();
        }
        return false;
    }

    public Account getAccount() {
        return mAccount;
    }

    public Bitmap getBigPictureNotificationBmp() {
        return mBigPictureNotificationBmp;
    }

    public String getCaption() {
        return mCaption;
    }

    public RectF getCropValues() {
        return new RectF(mCropLeft, mCropTop, mCropRight, mCropBottom);
    }

    public RectF getCropValues(final int width, final int height) {
        return new RectF(mCropLeft * width, mCropTop * height, mCropRight * width,
                mCropBottom * height);
    }

    public Bitmap getDisplayImage(Context context) {
        try {
            final int size = PhotupApplication.getApplication(context).getSmallestScreenDimension();
            Bitmap bitmap = Utils
                    .decodeImage(context.getContentResolver(), getOriginalPhotoUri(), size);
            bitmap = Utils.rotate(bitmap, getExifRotation(context));
            return bitmap;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getDisplayImageKey() {
        return "dsply_" + getOriginalPhotoUri();
    }

    public Location getExifLocation(Context context) {
        final String filePath = Utils
                .getPathFromContentUri(context.getContentResolver(), getOriginalPhotoUri());
        if (null != filePath) {
            return Utils.getExifLocation(filePath);
        }
        return null;
    }

    public int getExifRotation(Context context) {
        return Utils
                .getOrientationFromContentUri(context.getContentResolver(), getOriginalPhotoUri());
    }

    public Filter getFilterUsed() {
        if (null == mFilter) {
            mFilter = Filter.ORIGINAL;
        }
        return mFilter;
    }

    public Uri getOriginalPhotoUri() {
        if (null == mFullUri && !TextUtils.isEmpty(mFullUriString)) {
            mFullUri = Uri.parse(mFullUriString);
        }
        return mFullUri;
    }

    public List<PhotoTag> getPhotoTags() {
        if (null != mTags) {
            return new ArrayList<PhotoTag>(mTags);
        }
        return null;
    }

    public int getPhotoTagsCount() {
        return null != mTags ? mTags.size() : 0;
    }

    public int getFriendPhotoTagsCount() {
        int count = 0;
        if (getPhotoTagsCount() > 0) {
            FbUser friend;
            for (PhotoTag tag : mTags) {
                friend = tag.getFriend();
                if (null != friend) {
                    count++;
                }
            }
        }
        return count;
    }

    public String getPlaceId() {
        return mPlaceId;
    }

    public String getResultPostId() {
        return mResultPostId;
    }

    public HashSet<FbUser> getTaggedFriends() {
        HashSet<FbUser> friends = new HashSet<FbUser>();

        if (getPhotoTagsCount() > 0) {
            FbUser friend;
            for (PhotoTag tag : mTags) {
                friend = tag.getFriend();
                if (null != friend) {
                    friends.add(friend);
                }
            }
        }

        return friends;
    }

    public Bitmap getThumbnailImage(Context context) {
        if (ContentResolver.SCHEME_CONTENT.equals(getOriginalPhotoUri().getScheme())) {
            return getThumbnailImageFromMediaStore(context);
        }

        final Resources res = context.getResources();
        int size = res.getBoolean(R.bool.load_mini_thumbnails) ? MINI_THUMBNAIL_SIZE
                : MICRO_THUMBNAIL_SIZE;
        if (size == MINI_THUMBNAIL_SIZE && res.getBoolean(R.bool.sample_mini_thumbnails)) {
            size /= 2;
        }

        try {
            Bitmap bitmap = Utils
                    .decodeImage(context.getContentResolver(), getOriginalPhotoUri(), size);
            bitmap = Utils.rotate(bitmap, getExifRotation(context));
            return bitmap;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getThumbnailImageKey() {
        return "thumb_" + getOriginalPhotoUri();
    }

    public int getTotalRotation(Context context) {
        return (getExifRotation(context) + getUserRotation()) % 360;
    }

    public Bitmap getUploadImage(Context context, final UploadQuality quality) {
        return getUploadImageNative(context, quality);
    }

    public int getUploadProgress() {
        return mProgress;
    }

    public File getUploadSaveFile() {
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "photup");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return new File(dir, System.currentTimeMillis() + ".jpg");
    }

    public UploadQuality getUploadQuality() {
        return null != mQuality ? mQuality : UploadQuality.MEDIUM;
    }

    public int getUploadState() {
        return mState;
    }

    public String getUploadTargetId() {
        return mTargetId;
    }

    public int getUserRotation() {
        return mUserRotation % 360;
    }

    @Override
    public int hashCode() {
        return getOriginalPhotoUri().hashCode();
    }

    public boolean hasPlace() {
        return null != mPlaceId && null != mPlaceName;
    }

    public void populateFromFriends(HashMap<String, FbUser> friends) {
        if (getPhotoTagsCount() > 0) {
            for (PhotoTag tag : mTags) {
                tag.populateFromFriends(friends);
            }
        }
    }

    public void populateFromAccounts(HashMap<String, Account> accounts) {
        if (null == mAccount && !TextUtils.isEmpty(mAccountId)) {
            mAccount = accounts.get(mAccountId);
        }
    }

    public Bitmap processBitmap(Bitmap bitmap, final boolean fullSize,
            final boolean modifyOriginal) {
        if (requiresProcessing(fullSize)) {
            return processBitmapUsingFilter(bitmap, mFilter, fullSize, modifyOriginal);
        } else {
            return bitmap;
        }
    }

    public Bitmap processBitmapUsingFilter(final Bitmap bitmap, final Filter filter,
            final boolean fullSize,
            final boolean modifyOriginal) {
        Utils.checkPhotoProcessingThread();

        PhotoProcessing.sendBitmapToNative(bitmap);
        if (modifyOriginal) {
            bitmap.recycle();
        }

        if (fullSize && beenCropped()) {
            RectF rect = getCropValues();
            PhotoProcessing.nativeCrop(rect.left, rect.top, rect.right, rect.bottom);
        }

        if (null != filter) {
            PhotoProcessing.filterPhoto(filter.getId());
        }

        switch (getUserRotation()) {
            case 90:
                PhotoProcessing.nativeRotate90();
                break;
            case 180:
                PhotoProcessing.nativeRotate180();
                break;
            case 270:
                PhotoProcessing.nativeRotate180();
                PhotoProcessing.nativeRotate90();
                break;
        }

        return PhotoProcessing.getBitmapFromNative(null);
    }

    public void removePhotoTag(PhotoTag tag) {
        if (null != mTags) {
            mTags.remove(tag);
            notifyTagListener(tag, false);

            if (mTags.isEmpty()) {
                mTags = null;
            }
        }
        setRequiresSaveFlag();
    }

    public boolean requiresFaceDetectPass() {
        return !mCompletedDetection;
    }

    public boolean requiresProcessing(final boolean fullSize) {
        return getUserRotation() != 0 || beenFiltered() || (fullSize && beenCropped());
    }

    public boolean requiresSaving() {
        return mNeedsSaveFlag;
    }

    public void reset() {
        mState = STATE_NONE;
        mUserRotation = 0;
        mCaption = null;
        mCropLeft = mCropTop = MIN_CROP_VALUE;
        mCropRight = mCropBottom = MAX_CROP_VALUE;
        mFilter = null;
        mTags = null;
        mCompletedDetection = false;

        setRequiresSaveFlag();
    }

    public void resetSaveFlag() {
        mNeedsSaveFlag = false;
    }

    public void rotateClockwise() {
        mUserRotation += 90;
        setRequiresSaveFlag();
    }

    public void setBigPictureNotificationBmp(Context context, Bitmap bigPictureNotificationBmp) {
        if (null == bigPictureNotificationBmp) {
            mBigPictureNotificationBmp = BitmapFactory
                    .decodeResource(context.getResources(), R.drawable.ic_logo);
        } else {
            mBigPictureNotificationBmp = bigPictureNotificationBmp;
        }
    }

    public void setCaption(String caption) {
        if (TextUtils.isEmpty(caption)) {
            mCaption = null;
        } else {
            mCaption = caption;
        }

        setRequiresSaveFlag();
    }

    public void setCropValues(RectF cropValues) {
        if (checkCropValues(cropValues.left, cropValues.top, cropValues.right, cropValues.bottom)) {

            mCropLeft = santizeCropValue(cropValues.left);
            mCropTop = santizeCropValue(cropValues.top);
            mCropRight = santizeCropValue(cropValues.right);
            mCropBottom = santizeCropValue(cropValues.bottom);
            if (Flags.DEBUG) {
                Log.d(LOG_TAG, "Valid Crop Values: " + cropValues.toString());
            }
        } else {
            if (Flags.DEBUG) {
                Log.d(LOG_TAG, "Invalid Crop Values: " + cropValues.toString());
            }
            mCropLeft = mCropTop = MIN_CROP_VALUE;
            mCropRight = mCropBottom = MAX_CROP_VALUE;
        }

        setRequiresSaveFlag();
    }

    public void setFaceDetectionListener(OnFaceDetectionListener listener) {
        // No point keeping listener if we've already done a pass
        if (!mCompletedDetection) {
            mFaceDetectListener = new WeakReference<OnFaceDetectionListener>(listener);
        }
    }

    public void setFilterUsed(Filter filter) {
        mFilter = filter;
        setRequiresSaveFlag();
    }

    public void setPlace(Place place) {
        if (null != place) {
            mPlaceId = place.getId();
            mPlaceName = place.getName();
        } else {
            mPlaceId = mPlaceName = null;
        }
        setRequiresSaveFlag();
    }

    public void setResultPostId(String resultPostId) {
        mResultPostId = resultPostId;
        setRequiresSaveFlag();
    }

    public void setTagChangedListener(OnPhotoTagsChangedListener tagChangedListener) {
        mTagChangedListener = new WeakReference<OnPhotoTagsChangedListener>(tagChangedListener);
    }

    public void setUploadParams(Account account, String targetId, UploadQuality quality) {
        mAccount = account;
        mAccountId = account.getId();
        mTargetId = targetId;
        mQuality = quality;
        setRequiresSaveFlag();
    }

    public void setUploadProgress(int progress) {
        if (progress != mProgress) {
            mProgress = progress;
            notifyUploadStateListener();
        }
    }

    public void setUploadState(final int state) {
        if (mState != state) {
            mState = state;

            switch (state) {
                case STATE_UPLOAD_ERROR:
                case STATE_UPLOAD_COMPLETED:
                    mBigPictureNotificationBmp = null;
                    EventBus.getDefault().post(new UploadsModifiedEvent());
                    break;
                case STATE_SELECTED:
                case STATE_UPLOAD_WAITING:
                    mProgress = -1;
                    break;
            }

            notifyUploadStateListener();
            setRequiresSaveFlag();
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String caption = getCaption();
        if (null != caption) {
            sb.append(caption).append(" ");
        }

        if (hasPlace()) {
            sb.append("(").append(mPlaceName).append(")");
        }

        return sb.toString();
    }

    /**
     * Used only for ORMLite
     */
    public void setTagJson(final String json) {
        if (null == json) {
            return;
        }

        try {
            JSONArray document = new JSONArray(json);

            if (null == mTags) {
                mTags = new HashSet<PhotoTag>();
            }
            mTags.clear();

            for (int i = 0, z = document.length(); i < z; i++) {
                mTags.add(new PhotoTag(document.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used only for ORMLite
     */
    public String getTagJson() {
        if (getPhotoTagsCount() > 0) {
            JSONArray document = new JSONArray();
            for (PhotoTag tag : mTags) {
                try {
                    document.put(tag.toJsonObject());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return document.toString();
        }
        return null;
    }

    private Bitmap getThumbnailImageFromMediaStore(Context context) {
        Resources res = context.getResources();

        final int kind = res.getBoolean(R.bool.load_mini_thumbnails) ? Thumbnails.MINI_KIND
                : Thumbnails.MICRO_KIND;

        BitmapFactory.Options opts = null;
        if (kind == Thumbnails.MINI_KIND && res.getBoolean(R.bool.sample_mini_thumbnails)) {
            opts = new BitmapFactory.Options();
            opts.inSampleSize = 2;
        }

        try {
            final long id = Long.parseLong(getOriginalPhotoUri().getLastPathSegment());

            Bitmap bitmap = Thumbnails.getThumbnail(context.getContentResolver(), id, kind, opts);
            bitmap = Utils.rotate(bitmap, getExifRotation(context));
            return bitmap;
        } catch (Exception e) {
            if (Flags.DEBUG) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private Bitmap getUploadImageNative(final Context context, final UploadQuality quality) {
        Utils.checkPhotoProcessingThread();
        try {
            String path = Utils
                    .getPathFromContentUri(context.getContentResolver(), getOriginalPhotoUri());
            if (null != path) {
                BitmapSize size = BitmapUtils.getBitmapSize(path);

                if (quality.requiresResizing()) {
                    final float resizeRatio = Math.max(size.width, size.height) / (float) quality
                            .getMaxDimension();
                    size = new BitmapSize(Math.round(size.width / resizeRatio),
                            Math.round(size.height / resizeRatio));
                }

                boolean doAndroidDecode = true;

                if (Flags.USE_INTERNAL_DECODER) {
                    doAndroidDecode =
                            PhotoProcessing.nativeLoadResizedBitmap(path, size.width * size.height)
                                    != 0;

                    if (Flags.DEBUG) {
                        if (doAndroidDecode) {
                            Log.d("MediaStorePhotoUpload",
                                    "getUploadImage. Native decode failed :(");
                        } else {
                            Log.d("MediaStorePhotoUpload",
                                    "getUploadImage. Native decode complete!");
                        }
                    }
                }

                if (doAndroidDecode) {
                    if (Flags.DEBUG) {
                        Log.d("MediaStorePhotoUpload", "getUploadImage. Doing Android decode");
                    }

                    // Just in case
                    PhotoProcessing.nativeDeleteBitmap();

                    // Decode in Android and send to native
                    Bitmap bitmap = Utils
                            .decodeImage(context.getContentResolver(), getOriginalPhotoUri(),
                                    quality.getMaxDimension());

                    if (null != bitmap) {
                        PhotoProcessing.sendBitmapToNative(bitmap);
                        bitmap.recycle();

                        // Resize image to correct size
                        PhotoProcessing.nativeResizeBitmap(size.width, size.height);
                    } else {
                        return null;
                    }
                }

                /**
                 * Apply crop if needed
                 */
                if (beenCropped()) {
                    RectF rect = getCropValues();
                    PhotoProcessing.nativeCrop(rect.left, rect.top, rect.right, rect.bottom);
                }

                /**
                 * Apply filter if needed
                 */
                if (beenFiltered()) {
                    PhotoProcessing.filterPhoto(getFilterUsed().getId());
                    if (Flags.DEBUG) {
                        Log.d("MediaStorePhotoUpload", "getUploadImage. Native filter complete!");
                    }
                }

                /**
                 * Rotate if needed
                 */
                final int rotation = getTotalRotation(context);
                switch (rotation) {
                    case 90:
                        PhotoProcessing.nativeRotate90();
                        break;
                    case 180:
                        PhotoProcessing.nativeRotate180();
                        break;
                    case 270:
                        PhotoProcessing.nativeRotate180();
                        PhotoProcessing.nativeRotate90();
                        break;
                }
                if (Flags.DEBUG) {
                    Log.d("MediaStorePhotoUpload",
                            "getUploadImage. " + rotation + " degree rotation complete!");
                }

                if (Flags.DEBUG) {
                    Log.d("MediaStorePhotoUpload", "getUploadImage. Native worked!");
                }

                return PhotoProcessing.getBitmapFromNative(null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Just in case...
            PhotoProcessing.nativeDeleteBitmap();
        }

        return null;
    }

    private void notifyTagListener(PhotoTag tag, boolean added) {
        if (null != mTagChangedListener) {
            OnPhotoTagsChangedListener listener = mTagChangedListener.get();
            if (null != listener) {
                listener.onPhotoTagsChanged(tag, added);
            }
        }
    }

    private void notifyUploadStateListener() {
        EventBus.getDefault().post(new UploadStateChangedEvent(this));
    }

    private void setRequiresSaveFlag() {
        mNeedsSaveFlag = true;
    }
}
