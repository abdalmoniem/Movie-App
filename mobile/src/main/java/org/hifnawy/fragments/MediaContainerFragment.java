package org.hifnawy.fragments;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import org.hifnawy.MobileButterApplication;
import org.hifnawy.R;
import org.hifnawy.activities.MainActivity;
import org.hifnawy.adapters.MediaPagerAdapter;
import org.hifnawy.base.manager.provider.ProviderManager;
import org.hifnawy.base.providers.media.MediaProvider;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Fragment that contains a viewpager tabs for {@link org.hifnawy.fragments.MediaListFragment}
 */
public class MediaContainerFragment extends Fragment {

    @Inject
    ProviderManager providerManager;

    private Integer mSelection = 0;

    @BindView(R.id.pager)
    ViewPager mViewPager;

    public static MediaContainerFragment newInstance() {
        return new MediaContainerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_container, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        MobileButterApplication.getAppContext()
                .getComponent()
                .inject(this);

        MediaProvider mProvider = providerManager.getCurrentMediaProvider();
        MediaPagerAdapter mAdapter = new MediaPagerAdapter(mProvider, getChildFragmentManager(), mProvider.getNavigation());
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mSelection = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        mSelection = mProvider.getDefaultNavigationIndex();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((MainActivity) getActivity()).updateTabs(this, mSelection);
    }

    public ViewPager getViewPager() {
        return mViewPager;
    }

    public Integer getCurrentSelection() {
        return mSelection;
    }

}
