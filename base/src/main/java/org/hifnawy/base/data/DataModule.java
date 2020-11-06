/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package org.hifnawy.base.data;

import android.content.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.hifnawy.base.content.preferences.Prefs;
import org.hifnawy.base.utils.PrefUtils;
import org.hifnawy.base.utils.StorageUtils;
import dagger.Module;
import dagger.Provides;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

@Module
public class DataModule {

    @Provides
    @Singleton
    public Cache provideCache(Context context) {
        int cacheSize = 10 * 1024 * 1024;
        File cacheLocation = new File(
                PrefUtils.get(context, Prefs.STORAGE_LOCATION, StorageUtils.getIdealCacheDirectory(context)
                        .toString()));
        cacheLocation.mkdirs();

        return new Cache(cacheLocation, cacheSize);
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(Cache cache) {
        return new Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .cache(cache)
                .build();
    }

    @Provides
    @Singleton
    public Picasso providePicasso(Context context) {
        return new Picasso.Builder(context).build();
    }

    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }

}
