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

package org.hifnawy.tv.fragments;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DetailsOverviewLogoPresenter;
import androidx.leanback.widget.DetailsOverviewRow;
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnActionClickedListener;
import androidx.leanback.widget.PresenterSelector;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;

import org.hifnawy.base.providers.media.MediaProvider;
import org.hifnawy.base.providers.media.models.Media;
import org.hifnawy.base.utils.ThreadUtils;
import org.hifnawy.base.utils.VersionUtils;
import org.hifnawy.tv.activities.TVMediaDetailActivity;

public abstract class TVBaseDetailsFragment extends DetailsSupportFragment
        implements MediaProvider.Callback,
        OnActionClickedListener {

    public static final String EXTRA_ITEM = "item";
    public static final String EXTRA_HERO_URL = "hero_url";

    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;
    private Media mItem;
    private String mHeroImage;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mItem = getArguments().getParcelable(EXTRA_ITEM);
        mHeroImage = mItem.image;

        setupAdapter();
        setupDetailsOverviewRowPresenter();

        final DetailsOverviewRow detailRow = createDetailsOverviewRow();
        mAdapter.add(detailRow);

        loadDetails();
    }

    abstract void loadDetails();

    abstract AbstractDetailsDescriptionPresenter getDetailPresenter();

    abstract void onDetailLoaded();

    protected ArrayObjectAdapter getObjectArrayAdapter() {
        return mAdapter;
    }

    public Media getMediaItem() {
        return mItem;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setupAdapter() {
        mPresenterSelector = new ClassPresenterSelector();
        createPresenters(mPresenterSelector);

        mAdapter = createAdapter(mPresenterSelector);
        setAdapter(mAdapter);
    }

    abstract ClassPresenterSelector createPresenters(ClassPresenterSelector selector);

    protected ArrayObjectAdapter createAdapter(PresenterSelector selector) {
        return new ArrayObjectAdapter(selector);
    }

    private void setupDetailsOverviewRowPresenter() {
        // Set detail background and style.
        FullWidthDetailsOverviewRowPresenter headerPresenter = new FullWidthDetailsOverviewRowPresenter(getDetailPresenter(),
                new DetailsOverviewLogoPresenter());
        headerPresenter.setBackgroundColor(mItem.color);
        headerPresenter.setOnActionClickedListener(this);

        FullWidthDetailsOverviewSharedElementHelper helper = new FullWidthDetailsOverviewSharedElementHelper();
        helper.setSharedElementEnterTransition(getActivity(), TVMediaDetailActivity.SHARED_ELEMENT_NAME);
        headerPresenter.setListener(helper);
        headerPresenter.setParticipatingEntranceTransition(false);

        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, headerPresenter);
        mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
    }

    private DetailsOverviewRow createDetailsOverviewRow() {
        final DetailsOverviewRow detailsRow = new DetailsOverviewRow(mItem);

        Picasso.get().load(mHeroImage).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                detailsRow.setImageBitmap(getActivity(), bitmap);
                mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());

            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onBitmapFailed(Exception exc, Drawable errorDrawable) {
                if(VersionUtils.isLollipop())
                    getActivity().startPostponedEnterTransition();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        });
        return detailsRow;
    }

    protected void addAction(Action action) {
        DetailsOverviewRow detailRow = (DetailsOverviewRow) mAdapter.get(0);
        detailRow.addAction(action);
    }

    abstract void addActions(Media item);

    @Override
    public void onSuccess(MediaProvider.Filters filters, ArrayList<Media> items) {
        if (!isAdded()) return;

        if (null == items || items.size() == 0) return;

        Media itemDetail = items.get(0);

        mItem = itemDetail;

        ThreadUtils.runOnUiThread(new Runnable() {
            @Override public void run() {
                final DetailsOverviewRow detailRow = createDetailsOverviewRow();
                mAdapter.replace(0, detailRow);
                onDetailLoaded();
            }
        });
    }

    @Override
    public void onFailure(Exception e) {
        //todo: on load failure
    }

    public interface Callback { }
}
