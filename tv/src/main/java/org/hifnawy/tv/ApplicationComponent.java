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

package org.hifnawy.tv;

import javax.inject.Singleton;

import org.hifnawy.tv.activities.TVMainActivity;
import org.hifnawy.tv.activities.TVMediaDetailActivity;
import org.hifnawy.tv.activities.TVMediaGridActivity;
import org.hifnawy.tv.activities.TVPreferencesActivity;
import org.hifnawy.tv.activities.TVSearchActivity;
import org.hifnawy.tv.activities.TVStreamLoadingActivity;
import org.hifnawy.tv.activities.TVTrailerPlayerActivity;
import org.hifnawy.tv.activities.TVUpdateActivity;
import org.hifnawy.tv.activities.TVVideoPlayerActivity;
import org.hifnawy.tv.activities.TVWelcomeActivity;
import org.hifnawy.tv.fragments.TVMediaGridFragment;
import org.hifnawy.tv.fragments.TVMovieDetailsFragment;
import org.hifnawy.tv.fragments.TVOverviewFragment;
import org.hifnawy.tv.fragments.TVPreferencesFragment;
import org.hifnawy.tv.fragments.TVSearchFragment;
import org.hifnawy.tv.fragments.TVShowDetailsFragment;
import org.hifnawy.tv.fragments.TVStreamLoadingFragment;
import org.hifnawy.tv.fragments.TVVideoPlayerFragment;
import org.hifnawy.tv.service.RecommendationService;
import org.hifnawy.tv.service.recommendation.RecommendationContentProvider;
import dagger.Component;

@Singleton
@Component(
        modules = ApplicationModule.class
)
public interface ApplicationComponent {

    void inject(TVButterApplication application);

    void inject(TVMainActivity activity);

    void inject(TVTrailerPlayerActivity activity);

    void inject(TVMediaDetailActivity activity);

    void inject(TVMediaGridActivity activity);

    void inject(TVPreferencesActivity activity);

    void inject(TVSearchActivity activity);

    void inject(TVStreamLoadingActivity activity);

    void inject(TVUpdateActivity activity);

    void inject(TVVideoPlayerActivity activity);

    void inject(TVWelcomeActivity activity);

    void inject(RecommendationService service);

    void inject(TVOverviewFragment fragment);

    void inject(TVShowDetailsFragment fragment);

    void inject(TVMovieDetailsFragment fragment);

    void inject(TVMediaGridFragment fragment);

    void inject(TVStreamLoadingFragment fragment);

    void inject(TVPreferencesFragment fragment);

    void inject(TVSearchFragment fragment);

    void inject(TVVideoPlayerFragment fragment);

    void inject(RecommendationContentProvider contentProvider);

}
