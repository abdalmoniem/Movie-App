package org.hifnawy.fragments;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hifnawy.R;
import org.hifnawy.adapters.ShowDetailPagerAdapter;
import org.hifnawy.base.fragments.dialog.StringArraySelectorDialogFragment;
import org.hifnawy.base.providers.media.models.Episode;
import org.hifnawy.base.providers.media.models.Show;
import org.hifnawy.base.utils.PixelUtils;
import org.hifnawy.base.utils.VersionUtils;
import org.hifnawy.fragments.base.BaseDetailFragment;
import org.hifnawy.widget.ObservableParallaxScrollView;
import org.hifnawy.widget.WrappingViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ShowDetailFragment extends BaseDetailFragment {

    private static Show sShow;
    private Boolean mIsTablet = false;

    @BindView(R.id.pager)
    WrappingViewPager mViewPager;
    @BindView(R.id.tabs)
    TabLayout mTabs;
    @Nullable
    @BindView(R.id.background)
    View mBackground;
    @Nullable
    @BindView(R.id.top)
    View mShadow;
    @Nullable
    @BindView(R.id.title)
    TextView mTitle;
    @Nullable
    @BindView(R.id.aired)
    TextView mMeta;
    @Nullable
    @BindView(R.id.synopsis)
    TextView mSynopsis;
    @Nullable
    @BindView(R.id.rating)
    RatingBar mRating;
    @Nullable
    @BindView(R.id.cover_image)
    ImageView mCoverImage;

    public static ShowDetailFragment newInstance(Show show) {
        sShow = show;
        return new ShowDetailFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.fragment_showdetail, container, false);
        ButterKnife.bind(this, mRoot);
        if (VersionUtils.isJellyBean() && container != null) {
            int minHeight = container.getMinimumHeight() + PixelUtils.getPixelsFromDp(mActivity, 48);
            mRoot.setMinimumHeight(minHeight);
            mViewPager.setMinimumHeight(minHeight);
        }

        if(sShow == null)
            return mRoot;

        mTabs.setTabMode(TabLayout.MODE_SCROLLABLE);
        mTabs.setTabGravity(TabLayout.GRAVITY_CENTER);

        mIsTablet = mCoverImage != null;

        List<Fragment> fragments = new ArrayList<>();
        if (mIsTablet) {
            Double rating = Double.parseDouble(sShow.rating);
            mTitle.setText(sShow.title);
            mRating.setProgress(rating.intValue());

            String metaDataStr = sShow.year;

            if (sShow.status != null) {
                metaDataStr += " • ";
                if (sShow.status == Show.Status.CONTINUING) {
                    metaDataStr += getString(R.string.continuing);
                } else {
                    metaDataStr += getString(R.string.ended);
                }
            }

            if (!TextUtils.isEmpty(sShow.genre)) {
                metaDataStr += " • ";
                metaDataStr += sShow.genre;
            }

            mMeta.setText(metaDataStr);

            if (!TextUtils.isEmpty(sShow.synopsis)) {
                mSynopsis.setText(sShow.synopsis);
            } else {
                mSynopsis.setClickable(false);
            }

            Picasso.get().load(sShow.image).into(mCoverImage);

            // Use reflection to set indicator color
            try {
                Field field = TabLayout.class.getDeclaredField("mTabStrip");
                field.setAccessible(true);
                Object ob = field.get(mTabs);
                Class<?> c = Class.forName("android.support.design.widget.TabLayout$SlidingTabStrip");
                Method method = c.getDeclaredMethod("setSelectedIndicatorColor", int.class);
                method.setAccessible(true);
                method.invoke(ob, sShow.color);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            mBackground.post(new Runnable() {
                @Override
                public void run() {
                    mBackground.getLayoutParams().height = mBackground.getLayoutParams().height - mTabs.getHeight();
                }
            });
            fragments.add(ShowDetailAboutFragment.newInstance(sShow));
        }

        final ArrayList<Integer> availableSeasons = new ArrayList<>();
        for (Episode episode : sShow.episodes) {
            if (!availableSeasons.contains(episode.season)) {
                availableSeasons.add(episode.season);
            }
        }
        Collections.sort(availableSeasons);

        boolean hasSpecial = availableSeasons.indexOf(0) > -1;
        if (hasSpecial)
            availableSeasons.remove(availableSeasons.indexOf(0));

        for (int seasonInt : availableSeasons) {
            fragments.add(ShowDetailSeasonFragment.newInstance(sShow, seasonInt));
        }
        if (hasSpecial)
            fragments.add(ShowDetailSeasonFragment.newInstance(sShow, 0));

        ShowDetailPagerAdapter fragmentPagerAdapter = new ShowDetailPagerAdapter(mActivity, getChildFragmentManager(), fragments);

        mViewPager.setAdapter(fragmentPagerAdapter);
        mTabs.setupWithViewPager(mViewPager);
        mTabs.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabs));

        if(fragmentPagerAdapter.getCount() == 1) {
            mTabs.setTabMode(TabLayout.MODE_FIXED);
        }

        mActivity.setSubScrollListener(mOnScrollListener);

        return mRoot;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity.setSubScrollListener(null);
    }

    public void openDialog(String title, String[] items, DialogInterface.OnClickListener onClickListener) {
        StringArraySelectorDialogFragment.show(mActivity.getSupportFragmentManager(), title, items, -1, onClickListener);
    }

    private ObservableParallaxScrollView.Listener mOnScrollListener = new ObservableParallaxScrollView.Listener() {
        @Override
        public void onScroll(int scrollY, ObservableParallaxScrollView.Direction direction) {
            if (!mIsTablet) {
                if (scrollY > 0) {
                    int headerHeight = mActivity.getHeaderHeight();
                    if (scrollY < headerHeight) {
                        float alpha = 1.0f - ((float) scrollY / (float) headerHeight);
                        mShadow.setAlpha(alpha);
                        mTabs.setBackgroundColor(mActivity.getResources().getColor(android.R.color.transparent));
                        mTabs.setTranslationY(0);
                    } else {
                        mShadow.setAlpha(0);
                        mTabs.setBackgroundColor(sShow.color);
                        mTabs.setTranslationY(scrollY - headerHeight);
                    }
                }
            }
        }
    };

}
