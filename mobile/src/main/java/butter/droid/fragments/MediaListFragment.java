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

package butter.droid.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butter.droid.GridRecyclerView;
import butter.droid.MobileButterApplication;
import butter.droid.R;
import butter.droid.activities.MediaDetailActivity;
import butter.droid.adapters.MediaGridAdapter;
import butter.droid.base.ButterApplication;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.manager.provider.ProviderManager;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.utils.LocaleUtils;
import butter.droid.base.utils.NetworkUtils;
import butter.droid.base.utils.PrefUtils;
import butter.droid.base.utils.ThreadUtils;
import butter.droid.fragments.dialog.LoadingDetailDialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;
import okhttp3.OkHttpClient;

/**
 * This fragment is the main screen for viewing a collection of media items.
 * <p/>
 * LOADING
 * <p/>
 * This fragment has 2 ways of representing a loading state; If the data is being loaded for the first time, or the media detail for the
 * detail screen is being loaded,a progress layout is displayed with a message.
 * <p/>
 * If a page is being loaded, the adapter will display a progress item.
 * <p/>
 * MODE
 * <p/>
 * This fragment can be instantiated with ether a SEARCH mode, or a NORMAL mode. SEARCH mode simply does not load any initial data.
 */
public class MediaListFragment extends Fragment implements LoadingDetailDialogFragment.Callback {

    public static final String EXTRA_SORT = "extra_sort";
    public static final String EXTRA_ORDER = "extra_order";
    public static final String EXTRA_GENRE = "extra_genre";
    public static final String EXTRA_MODE = "extra_mode";
    public static final String DIALOG_LOADING_DETAIL = "DIALOG_LOADING_DETAIL";
    public static final int LOADING_DIALOG_FRAGMENT = 1;
    @Inject
    ProviderManager providerManager;
    @Inject
    OkHttpClient client;
    View mRootView;
    @BindView(R.id.progressOverlay)
    LinearLayout mProgressOverlay;
    @BindView(R.id.recyclerView)
    GridRecyclerView mRecyclerView;
    @BindView(R.id.emptyView)
    TextView mEmptyView;
    @BindView(R.id.progress_textview)
    TextView mProgressTextView;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    private Parcelable mRecyclerViewState;
    private Context mContext;
    private MediaGridAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private Integer mColumns = 2;
    //overrides the default loading message
    private int mLoadingMessage = R.string.loading_data;
    private State mState = State.UNINITIALISED;
    private Mode mMode = Mode.NORMAL;
    private ArrayList<Media> mItems = new ArrayList<>();
    private boolean mEndOfListReached = false;
    private int mTotalItemCount = 0, mLoadingTreshold = mColumns * 3, mPreviousTotal = 0;
    private int mPage = 1;
    private MediaProvider.Filters mFilters = new MediaProvider.Filters();
    private MediaProvider.Callback mMediaListCallback = new MediaProvider.Callback() {
        @Override
        @DebugLog
        public void onSuccess(MediaProvider.Filters filters, final ArrayList<Media> items) {
            items.removeAll(mItems);
            if (mState == State.REFRESHING) {
                mItems.clear();
                ThreadUtils.runOnUiThread(() -> mAdapter.clearItems());
            }
            mEndOfListReached = items.size() == 0;
            if (!mEndOfListReached) {
                mItems.addAll(items);
                if (isAdded()) {
                    mPage = mPage + 1;
                    ThreadUtils.runOnUiThread(() -> {
                        mAdapter.setItems(mItems);
                        mPreviousTotal = mTotalItemCount = mAdapter.getItemCount();
                    });
                }
            }
            setState(State.LOADED);
        }

        @Override
        @DebugLog
        public void onFailure(Exception e) {
            if (!isDetached() || !e.getMessage().equals("Canceled")) {
                e.printStackTrace();
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Snackbar.make(mRootView, R.string.unknown_error, Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
            mEndOfListReached = true;
            if (mAdapter != null) {
                mAdapter.removeLoading();
            }
            setState(State.LOADED);
        }
    };

    private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            int visibleItemCount = mLayoutManager.getChildCount();
            mTotalItemCount = mLayoutManager.getItemCount() - (mAdapter.isLoading() ? 1 : 0);
            int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();

            if (mState == State.LOADING_PAGE) {
                if (mTotalItemCount > mPreviousTotal) {
                    mPreviousTotal = mTotalItemCount;
                    mPreviousTotal = mTotalItemCount = mLayoutManager.getItemCount();
                    setState(State.LOADED);
                }
            }

            if (!recyclerView.canScrollVertically(1)) {
                if (!mEndOfListReached && mState != State.SEARCHING && mState != State.LOADING_PAGE && mState != State.LOADING && (mTotalItemCount - visibleItemCount) <= (firstVisibleItem +
                        mLoadingTreshold)) {

                    mFilters.setPage(mPage);
                    providerManager.getCurrentMediaProvider().getList(mItems, new MediaProvider.Filters(mFilters), mMediaListCallback);

                    mPreviousTotal = mTotalItemCount = mLayoutManager.getItemCount();
                    setState(State.LOADING_PAGE);
                }
            }
        }
    };
    private MediaGridAdapter.OnItemClickListener mOnItemClickListener = new MediaGridAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(final View view, final Media item, final int position) {
            /**
             * We shouldn't really be doing the palette loading here without any ui feedback,
             * but it should be really quick
             */
            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
            if (holder instanceof MediaGridAdapter.ViewHolder) {
                ImageView coverImage = ((MediaGridAdapter.ViewHolder) holder).getCoverImage();

                if (coverImage.getDrawable() == null) {
                    showLoadingDialog(position);
                    return;
                }

                Bitmap cover = ((BitmapDrawable) coverImage.getDrawable()).getBitmap();
                new Palette.Builder(cover).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        int vibrantColor = palette.getVibrantColor(-1);
                        int paletteColor;
                        if (vibrantColor == -1) {
                            paletteColor = palette.getMutedColor(ContextCompat.getColor(getContext(), R.color.primary));
                        } else {
                            paletteColor = vibrantColor;
                        }
                        item.color = paletteColor;
                        showLoadingDialog(position);
                    }
                });
            } else {
                showLoadingDialog(position);
            }

        }
    };

    public static MediaListFragment newInstance(Mode mode, MediaProvider.Filters.Sort filter, MediaProvider.Filters.Order defOrder) {
        return newInstance(mode, filter, defOrder, null);
    }

    public static MediaListFragment newInstance(Mode mode, MediaProvider.Filters.Sort sort, MediaProvider.Filters.Order order, String genre) {
        MediaListFragment frag = new MediaListFragment();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_MODE, mode);
        args.putSerializable(EXTRA_SORT, sort);
        args.putSerializable(EXTRA_ORDER, order);
        args.putString(EXTRA_GENRE, genre);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MobileButterApplication.getAppContext()
                .getComponent()
                .inject(this);

        if (getArguments() != null) {
            String language = PrefUtils.get(getActivity(), Prefs.LOCALE, ButterApplication.getSystemLanguage());
            mFilters.setSort((MediaProvider.Filters.Sort) getArguments().getSerializable(EXTRA_SORT));
            mFilters.setOrder((MediaProvider.Filters.Order) getArguments().getSerializable(EXTRA_ORDER));
            mFilters.setGenre(getArguments().getString(EXTRA_GENRE));
            mFilters.setLangCode(LocaleUtils.toLocale(language).getLanguage());

            mMode = (Mode) getArguments().getSerializable(EXTRA_MODE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContext = getActivity();

        mRootView = inflater.inflate(R.layout.fragment_media, container, false);
        ButterKnife.bind(this, mRootView);

        mColumns = Integer.parseInt(PrefUtils.get(getContext(), Prefs.GRID_SIZE, "2"));
        mLoadingTreshold = mColumns * 3;

        mLayoutManager = new GridLayoutManager(mContext, mColumns);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mAdapter.resetItemAnimationLastPosition();

            mFilters.setGenre(mFilters.getGenre());
            mFilters.setPage(1);

            setState(State.REFRESHING);
            providerManager.getCurrentMediaProvider()
                    .getList(new MediaProvider.Filters(mFilters), mMediaListCallback);

            mSwipeRefreshLayout.setRefreshing(false);
        });

        return mRootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addOnScrollListener(mScrollListener);
        //adapter should only ever be created once on fragment initialise.
        mAdapter = new MediaGridAdapter(mContext, mItems, mColumns);
        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mAdapter.resetItemAnimationLastPosition();

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerViewState = mRecyclerView.getLayoutManager().onSaveInstanceState();

        if (mMode == Mode.SEARCH) {
            mEmptyView.setText(getString(R.string.no_search_results));
        } else if (mAdapter.getItemCount() == 0) { //don't load initial data in search mode
            setState(State.LOADING);
            providerManager.getCurrentMediaProvider().getList(new MediaProvider.Filters(mFilters), mMediaListCallback);/* fetch new items */
        } else {
            updateUI();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mRecyclerViewState = mRecyclerView.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void onResume() {
        super.onResume();

        mColumns = Integer.parseInt(PrefUtils.get(getContext(), Prefs.GRID_SIZE, "2"));
        mLoadingTreshold = mColumns * 3;

        mLayoutManager = new GridLayoutManager(mContext, mColumns);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.setHasFixedSize(true);

        //adapter should only ever be created once on fragment initialise.
        mAdapter = new MediaGridAdapter(mContext, mItems, mColumns);
        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mAdapter.resetItemAnimationLastPosition();

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.getLayoutManager().onRestoreInstanceState(mRecyclerViewState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mAdapter.setOnItemClickListener(mOnItemClickListener);
    }

    public void changeGenre(String genre) {
        if (!(mFilters.getGenre() == null ? "" : mFilters.getGenre()).equals(genre == null ? "" : genre)) {
            mAdapter.resetItemAnimationLastPosition();
            mFilters.setGenre(genre);
            mFilters.setPage(1);

            setState(State.REFRESHING);
            providerManager.getCurrentMediaProvider()
                    .getList(new MediaProvider.Filters(mFilters), mMediaListCallback);
        }
    }

    private void updateLoadingMessage() {
        mProgressTextView.setText(mLoadingMessage);
    }

    @DebugLog
    private void setState(State state) {
        if (mState != state) {
            mState = state;

            updateUI();
        }
    }

    /**
     * Responsible for updating the UI based on the state of this fragment
     */
    private void updateUI() {
        if (!isAdded()) return;

        ThreadUtils.runOnUiThread(() -> {
            switch (mState) {
                case LOADING_DETAIL:
                    mLoadingMessage = R.string.loading_details;
                    break;
                case SEARCHING:
                    mLoadingMessage = R.string.searching;
                    break;
                case REFRESHING:
                case LOADING:
                    if (mAdapter.isLoading()) mAdapter.removeLoading();
                    //show the progress bar
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mRecyclerView.animate().alpha(0.5f).setDuration(500).start();
                    mEmptyView.setVisibility(View.GONE);
                    mProgressOverlay.setVisibility(View.VISIBLE);
                    break;
                case LOADED:
                    if (mAdapter.isLoading()) mAdapter.removeLoading();
                    mProgressOverlay.setVisibility(View.GONE);
                    boolean hasItems = mItems.size() > 0;
                    //show either the recyclerview or the empty view
                    mRecyclerView.animate().alpha(1.0f).setDuration(100).start();
                    mRecyclerView.setVisibility(hasItems ? View.VISIBLE : View.INVISIBLE);
                    mEmptyView.setVisibility(hasItems ? View.GONE : View.VISIBLE);
                    break;
                case LOADING_PAGE:
                    //add a loading view to the adapter
                    if (!mAdapter.isLoading()) mAdapter.addLoading();
                    mEmptyView.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                    break;
                default:
                    int providerMessage = providerManager.getCurrentMediaProvider().getLoadingMessage();
                    mLoadingMessage = providerMessage > 0 ? providerMessage : R.string.loading_data;
                    break;
            }
            updateLoadingMessage();
        });
    }

    private void showLoadingDialog(Integer position) {
        LoadingDetailDialogFragment loadingFragment = LoadingDetailDialogFragment.newInstance(position);
        loadingFragment.setTargetFragment(MediaListFragment.this, LOADING_DIALOG_FRAGMENT);
        loadingFragment.show(getFragmentManager(), DIALOG_LOADING_DETAIL);
    }

    public void triggerSearch(String searchQuery) {
        if (!isAdded()) return;
        if (null == mAdapter) return;
        if (!NetworkUtils.isNetworkConnected(getActivity())) {
            Toast.makeText(getActivity(), R.string.network_message, Toast.LENGTH_SHORT).show();
            return;
        }

        providerManager.getCurrentMediaProvider().cancel();

        mEndOfListReached = false;

        mItems.clear();
        mAdapter.clearItems();//clear out adapter

        if (searchQuery.equalsIgnoreCase("")) {
            setState(State.LOADED);
            return; //don't do a search for empty queries
        }

        setState(State.SEARCHING);
        mPage = 1;
        mFilters.setPage(mPage);
        mFilters.setKeywords(searchQuery);

        providerManager.getCurrentMediaProvider().getList(new MediaProvider.Filters(mFilters), mMediaListCallback);
    }

    /**
     * Called when loading media details fails
     */
    @Override
    public void onDetailLoadFailure() {
        Snackbar.make(mRootView, R.string.unknown_error, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Called when media details have been loaded. This should be called on a background thread.
     *
     * @param item The media item
     */
    @Override
    public void onDetailLoadSuccess(final Media item) {
        MediaDetailActivity.startActivity(mContext, item);
    }

    /**
     * Called when loading media details
     *
     * @return mItems
     */
    @Override
    public ArrayList<Media> getCurrentList() {
        return mItems;
    }

    public enum Mode {
        NORMAL, SEARCH
    }

    private enum State {
        UNINITIALISED, LOADING, SEARCHING, LOADING_PAGE, LOADED, LOADING_DETAIL, REFRESHING
    }
}
