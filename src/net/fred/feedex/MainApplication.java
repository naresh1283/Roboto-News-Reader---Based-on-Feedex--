/**
 * FeedEx
 *
 * Copyright (c) 2012-2013 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fred.feedex;

import com.hughes.android.dictionary.DictionaryApplication;
import com.roboto.app.RobotoApplication;
import net.fred.feedex.utils.PrefUtils;

public class MainApplication extends DictionaryApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, false); // init
    }

}
