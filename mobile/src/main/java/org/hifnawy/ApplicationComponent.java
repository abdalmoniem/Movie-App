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

package org.hifnawy;

import javax.inject.Singleton;

import org.hifnawy.activities.AboutActivity;
import org.hifnawy.activities.BeamPlayerActivity;
import org.hifnawy.activities.MainActivity;
import org.hifnawy.activities.MediaDetailActivity;
import org.hifnawy.activities.PreferencesActivity;
import org.hifnawy.activities.SearchActivity;
import org.hifnawy.activities.StreamLoadingActivity;
import org.hifnawy.activities.TermsActivity;
import org.hifnawy.activities.TrailerPlayerActivity;
import org.hifnawy.activities.VideoPlayerActivity;
import org.hifnawy.fragments.MediaContainerFragment;
import org.hifnawy.fragments.MediaGenreSelectionFragment;
import org.hifnawy.fragments.MediaListFragment;
import org.hifnawy.fragments.MovieDetailFragment;
import org.hifnawy.fragments.NavigationDrawerFragment;
import org.hifnawy.fragments.StreamLoadingFragment;
import org.hifnawy.fragments.VideoPlayerFragment;
import org.hifnawy.fragments.dialog.EpisodeDialogFragment;
import org.hifnawy.fragments.dialog.LoadingDetailDialogFragment;
import dagger.Component;

@Singleton
@Component(
        modules = ApplicationModule.class
)
public interface ApplicationComponent {

    void inject(MobileButterApplication application);

    void inject(MainActivity activity);

    void inject(TrailerPlayerActivity activity);

    void inject(PreferencesActivity activity);

    void inject(AboutActivity activity);

    void inject(BeamPlayerActivity activity);

    void inject(MediaDetailActivity activity);

    void inject(SearchActivity activity);

    void inject(StreamLoadingActivity activity);

    void inject(TermsActivity activity);

    void inject(VideoPlayerActivity activity);

    void inject(NavigationDrawerFragment fragment);

    void inject(MediaContainerFragment fragment);

    void inject(MediaListFragment fragment);

    void inject(MediaGenreSelectionFragment fragment);

    void inject(LoadingDetailDialogFragment fragment);

    void inject(StreamLoadingFragment fragment);

    void inject(EpisodeDialogFragment fragment);

    void inject(MovieDetailFragment fragment);

    void inject(VideoPlayerFragment fragment);

}
