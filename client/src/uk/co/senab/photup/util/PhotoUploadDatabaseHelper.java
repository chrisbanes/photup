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
package uk.co.senab.photup.util;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;

import android.content.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import uk.co.senab.photup.DatabaseHelper;
import uk.co.senab.photup.Flags;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.tasks.PhotupThreadRunnable;

public class PhotoUploadDatabaseHelper {

    public static List<PhotoUpload> getSelected(Context context) {
        final DatabaseHelper helper = getHelper(context);
        List<PhotoUpload> uploads = null;

        try {
            final Dao<PhotoUpload, String> dao = helper.getPhotoUploadDao();
            uploads = dao.query(dao.queryBuilder().where()
                    .eq(PhotoUpload.FIELD_STATE, PhotoUpload.STATE_SELECTED)
                    .prepare());
        } catch (SQLException e) {
            if (Flags.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            OpenHelperManager.releaseHelper();
        }
        return uploads;
    }

    public static List<PhotoUpload> getUploads(Context context) {
        final DatabaseHelper helper = getHelper(context);
        List<PhotoUpload> uploads = null;

        try {
            final Dao<PhotoUpload, String> dao = helper.getPhotoUploadDao();
            uploads = dao.query(dao.queryBuilder().where()
                    .ge(PhotoUpload.FIELD_STATE, PhotoUpload.STATE_UPLOAD_WAITING).prepare());
        } catch (SQLException e) {
            if (Flags.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            OpenHelperManager.releaseHelper();
        }

        return uploads;
    }

    public static void deleteAllSelected(final Context context) {
        PhotupApplication.getApplication(context).getDatabaseThreadExecutorService()
                .submit(new PhotupThreadRunnable() {

                    public void runImpl() {
                        final DatabaseHelper helper = getHelper(context);
                        try {
                            final Dao<PhotoUpload, String> dao = helper.getPhotoUploadDao();
                            final DeleteBuilder<PhotoUpload, String> deleteBuilder = dao
                                    .deleteBuilder();
                            deleteBuilder.where()
                                    .le(PhotoUpload.FIELD_STATE, PhotoUpload.STATE_SELECTED);
                            dao.delete(deleteBuilder.prepare());
                        } catch (SQLException e) {
                            if (Flags.DEBUG) {
                                e.printStackTrace();
                            }
                        } finally {
                            OpenHelperManager.releaseHelper();
                        }
                    }
                });

    }

    public static void deleteFromDatabase(final Context context, final PhotoUpload upload) {
        PhotupApplication.getApplication(context).getDatabaseThreadExecutorService()
                .submit(new PhotupThreadRunnable() {

                    public void runImpl() {
                        final DatabaseHelper helper = getHelper(context);
                        try {
                            Dao<PhotoUpload, String> dao = helper.getPhotoUploadDao();
                            dao.delete(upload);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } finally {
                            OpenHelperManager.releaseHelper();
                        }
                    }
                });
    }

    public static void deleteFromDatabase(final Context context, final List<PhotoUpload> uploads) {
        PhotupApplication.getApplication(context).getDatabaseThreadExecutorService()
                .submit(new PhotupThreadRunnable() {

                    public void runImpl() {
                        final DatabaseHelper helper = getHelper(context);
                        try {
                            Dao<PhotoUpload, String> dao = helper.getPhotoUploadDao();
                            for (PhotoUpload upload : uploads) {
                                dao.delete(upload);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } finally {
                            OpenHelperManager.releaseHelper();
                        }
                    }
                });
    }

    public static void saveToDatabase(final Context context, final PhotoUpload upload) {
        PhotupApplication.getApplication(context).getDatabaseThreadExecutorService()
                .submit(new PhotupThreadRunnable() {

                    public void runImpl() {
                        final DatabaseHelper helper = getHelper(context);
                        try {
                            Dao<PhotoUpload, String> dao = helper.getPhotoUploadDao();
                            dao.createOrUpdate(upload);
                        } catch (SQLException e) {
                            if (Flags.DEBUG) {
                                e.printStackTrace();
                            }
                        } finally {
                            OpenHelperManager.releaseHelper();
                        }
                    }
                });
    }

    public static void saveToDatabase(final Context context, List<PhotoUpload> uploads,
            final boolean forceUpdate) {
        final ArrayList<PhotoUpload> uploadsCopy = new ArrayList<PhotoUpload>();
        uploadsCopy.addAll(uploads);

        PhotupApplication.getApplication(context).getDatabaseThreadExecutorService()
                .submit(new PhotupThreadRunnable() {

                    public void runImpl() {
                        final DatabaseHelper helper = getHelper(context);
                        try {
                            final Dao<PhotoUpload, String> dao = helper.getPhotoUploadDao();
                            dao.callBatchTasks(new Callable<Void>() {
                                public Void call() throws Exception {

                                    for (PhotoUpload upload : uploadsCopy) {
                                        if (forceUpdate || upload.requiresSaving()) {
                                            dao.createOrUpdate(upload);
                                            upload.resetSaveFlag();
                                        }
                                    }
                                    return null;
                                }
                            });
                        } catch (Exception e) {
                            if (Flags.DEBUG) {
                                e.printStackTrace();
                            }
                        } finally {
                            OpenHelperManager.releaseHelper();
                        }
                    }
                });
    }

    private static DatabaseHelper getHelper(Context context) {
        return OpenHelperManager.getHelper(context, DatabaseHelper.class);
    }

}
