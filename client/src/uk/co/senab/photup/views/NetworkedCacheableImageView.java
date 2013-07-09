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
package uk.co.senab.photup.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapWrapper;
import uk.co.senab.bitmapcache.CacheableImageView;

/**
 * Simple extension of CacheableImageView which allows downloading of Images of the Internet.
 *
 * This code isn't production quality, but works well enough for this sample.s
 *
 * @author Chris Banes
 */
public class NetworkedCacheableImageView extends CacheableImageView {

    public NetworkedCacheableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * This task simply gets a list of URLs of Pug Photos
     */
    private class ImageUrlAsyncTask extends AsyncTask<String, Void, CacheableBitmapWrapper> {

        @Override
        protected CacheableBitmapWrapper doInBackground(String... params) {
            try {
                String url = params[0];

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                InputStream is = new BufferedInputStream(conn.getInputStream());
                Bitmap bitmap = BitmapFactory.decodeStream(is);

                if (null != bitmap) {
                    return new CacheableBitmapWrapper(url, bitmap);
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(CacheableBitmapWrapper result) {
            super.onPostExecute(result);

            setScaleType(ScaleType.CENTER_CROP);

            // Display the image
            setImageCachedBitmap(result);

            // Add to cache
            if (null != result) {
                mCache.put(result);
            }
        }
    }

    private BitmapLruCache mCache;
    private ImageUrlAsyncTask mCurrentTask;

    public void loadImage(BitmapLruCache cache, String url) {
        // First check whether there's already a task running, if so cancel it
        if (null != mCurrentTask) {
            mCurrentTask.cancel(false);
        }

        mCache = cache;
        // Check to see if the cache already has the bitmap
        CacheableBitmapWrapper wrapper = mCache.get(url);

        if (null != wrapper && wrapper.hasValidBitmap()) {
            // The cache has it, so just display it
            setImageCachedBitmap(wrapper);
        } else {
            setImageCachedBitmap(null);
            // Cache doesn't have the URL, do network request...
            mCurrentTask = new ImageUrlAsyncTask();
            mCurrentTask.execute(url);
        }
    }

}
