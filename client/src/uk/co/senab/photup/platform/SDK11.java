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
package uk.co.senab.photup.platform;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.view.View;

@TargetApi(11)
public class SDK11 {

    public static void disableHardwareAcceleration(View view) {
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public static boolean isCanvasHardwareAccelerated(Canvas canvas) {
        return canvas.isHardwareAccelerated();
    }

}
