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

public final class Flags {

    public static final boolean DEBUG = BuildConfig.DEBUG;

    public static final boolean ENABLE_BUG_TRACKING = !DEBUG;

    public static final boolean ENABLE_DB_PERSISTENCE = true;

    public static final boolean USE_INTERNAL_DECODER = false;

}
