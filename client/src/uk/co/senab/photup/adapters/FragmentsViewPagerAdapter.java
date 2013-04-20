/*******************************************************************************
 * Copyright 2013 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.	
 *******************************************************************************/
package uk.co.senab.photup.adapters;

import uk.co.senab.photup.fragments.SelectedPhotosFragment;
import uk.co.senab.photup.fragments.UploadsFragment;
import uk.co.senab.photup.fragments.UserPhotosFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class FragmentsViewPagerAdapter extends FragmentPagerAdapter {
	
	public FragmentsViewPagerAdapter(FragmentManager fragmentManager) {
		super(fragmentManager);
	}

	@Override
	public Fragment getItem(int position) {
		Fragment fragment;
		switch (position) {
		case 0:
			fragment = new UserPhotosFragment();
			return fragment;
		case 1:
			fragment = new SelectedPhotosFragment();
			return fragment;
		case 2:
			fragment = new UploadsFragment();
			return fragment;
		default:
			return null;
		}
	}

	@Override
	public int getCount() {
		return 3;
	}

}
