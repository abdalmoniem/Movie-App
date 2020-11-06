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

package org.hifnawy.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.hifnawy.BuildConfig;
import org.hifnawy.MobileButterApplication;
import org.hifnawy.R;
import org.hifnawy.activities.base.ButterBaseActivity;
import org.hifnawy.adapters.GenreAdapter;
import org.hifnawy.adapters.decorators.DividerItemDecoration;
import org.hifnawy.base.beaming.BeamManager;
import org.hifnawy.base.beaming.BeamPlayerNotificationService;
import org.hifnawy.base.content.preferences.Prefs;
import org.hifnawy.base.manager.provider.ProviderManager;
import org.hifnawy.base.manager.youtube.YouTubeManager;
import org.hifnawy.base.providers.media.MediaProvider;
import org.hifnawy.base.providers.media.models.Genre;
import org.hifnawy.base.providers.media.models.Movie;
import org.hifnawy.base.torrent.StreamInfo;
import org.hifnawy.base.utils.PrefUtils;
import org.hifnawy.base.utils.ProviderUtils;
import org.hifnawy.base.vpn.VPNManager;
import org.hifnawy.fragments.MediaContainerFragment;
import org.hifnawy.fragments.MediaListFragment;
import org.hifnawy.fragments.NavigationDrawerFragment;
import org.hifnawy.utils.ToolbarUtils;
import org.hifnawy.widget.ScrimInsetsFrameLayout;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import timber.log.Timber;

import static org.hifnawy.activities.TermsActivity.TERMS_ACCEPTED;

/**
 * The main activity that houses the navigation drawer, and controls navigation between fragments
 */
public class MainActivity extends ButterBaseActivity implements ProviderManager.OnProviderChangeListener {

  private static final int PERMISSIONS_REQUEST = 123;

  @Inject
  ProviderManager providerManager;
  @Inject
  YouTubeManager youTubeManager;

  @BindView(R.id.toolbar)
  Toolbar mToolbar;
  @BindView(R.id.navigation_drawer_container)
  ScrimInsetsFrameLayout mNavigationDrawerContainer;
  @Nullable
  @BindView(R.id.tabs)
  TabLayout mTabs;

  private NavigationDrawerFragment mNavigationDrawerFragment;
  private MediaContainerFragment mediaFragment;

  private int mSelectedPosition = 0;

  @SuppressLint("MissingSuperCall")
  @Override
  protected void onCreate(Bundle savedInstanceState) {

    MobileButterApplication.getAppContext()
        .getComponent()
        .inject(this);

    super.onCreate(savedInstanceState, R.layout.activity_main);

    if (!PrefUtils.contains(this, TERMS_ACCEPTED)) {
      // finish();

      int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
      int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.98);

      Dialog dialog = new Dialog(this);

      dialog.setContentView(R.layout.activity_terms);

      dialog.setCanceledOnTouchOutside(false);

      dialog.getWindow().setLayout(width, height);

      ScrollView termsScrollView = dialog.findViewById(R.id.termsScrollView);

      Button acceptBtn = dialog.findViewById(R.id.acceptBtn);

      Button leaveBtn = dialog.findViewById(R.id.leaveBtn);

      acceptBtn.setOnClickListener(view -> {
        PrefUtils.save(MainActivity.this, "terms_accepted", true);
        dialog.dismiss();
      });

      leaveBtn.setOnClickListener(view -> {
        finish();
        dialog.dismiss();
      });

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        termsScrollView.setOnScrollChangeListener((view, i, i1, i2, i3) -> {
          if (!termsScrollView.canScrollVertically(1)) {
            // bottom of scroll view
            acceptBtn.setEnabled(true);
          }
          if (!termsScrollView.canScrollVertically(-1)) {
            // top of scroll view
            acceptBtn.setEnabled(false);
          }
        });
      }

      dialog.show();

      // startActivity(new Intent(this, TermsActivity.class));
    }

    if (!isFullAccessStorageAllowed()) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
    }

    String action = getIntent().getAction();
    Uri data = getIntent().getData();
    if (action != null && action.equals(Intent.ACTION_VIEW) && data != null) {
      String streamUrl = data.toString();
      try {
        streamUrl = URLDecoder.decode(streamUrl, "utf-8");
        StreamLoadingActivity.startActivity(this, new StreamInfo(streamUrl));
        finish();
        return;
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    }

    FragmentManager.enableDebugLogging(BuildConfig.DEBUG);


    setSupportActionBar(mToolbar);
    setShowCasting(true);

    ToolbarUtils.updateToolbarHeight(this, mToolbar);

    // Set up the drawer.
    DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
    drawerLayout.setStatusBarBackgroundColor(ContextCompat.getColor(MobileButterApplication.getAppContext(), R.color.primary_dark));

    mNavigationDrawerFragment =
        (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer_fragment);

    mNavigationDrawerFragment.initialise(mNavigationDrawerContainer, drawerLayout);

    if (savedInstanceState != null) {
      return;
    }

    @ProviderManager.ProviderType int provider = PrefUtils.get(this, Prefs.DEFAULT_PROVIDER, ProviderManager.PROVIDER_TYPE_MOVIE);
    mNavigationDrawerFragment.selectItem(provider);
    providerManager.setCurrentProviderType(provider);

    showProvider(provider);
  }

  @Override
  protected void onStart() {
    super.onStart();
    mVPNManager = VPNManager.start(this);
  }

  @Override
  protected void onResume() {
    super.onResume();

    setTitle(ProviderUtils.getProviderTitle(providerManager.getCurrentMediaProviderType()));
    supportInvalidateOptionsMenu();

    mNavigationDrawerFragment.initItems();

    BeamPlayerNotificationService.cancelNotification();

    providerManager.addProviderListener(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    providerManager.removeProviderListener(this);
  }

  @Override
  public void onVPNServiceReady() {
    super.onVPNServiceReady();
    mNavigationDrawerFragment.initItems();
  }

  @Override
  public void onVPNStatusUpdate(VPNManager.State state, String message) {
    super.onVPNStatusUpdate(state, message);
    Timber.d("New state: %s", state);
    NavigationDrawerFragment.AbsNavDrawerItem.VPNNavDrawerItem vpnItem = mNavigationDrawerFragment.getVPNItem();
    if (vpnItem != null) {
      if (state.equals(VPNManager.State.DISCONNECTED)) {
        vpnItem.setSwitchValue(false);
        vpnItem.showProgress(false);
      } else if (state.equals(VPNManager.State.CONNECTING)) {
        vpnItem.showProgress(true);
      } else if (state.equals(VPNManager.State.CONNECTED)) {
        vpnItem.setSwitchValue(true);
        vpnItem.showProgress(false);
      }
    }
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.activity_overview, menu);

    MenuItem playerTestMenuItem = menu.findItem(R.id.action_playertests);
    playerTestMenuItem.setVisible(BuildConfig.DEBUG);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        /* Override default {@link pct.droid.activities.BaseActivity } behaviour */
        return false;
      case R.id.action_playertests:
        openPlayerTestDialog();
        break;
      case R.id.action_filter:
        Dialog dialog = new Dialog(this);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(R.layout.filter_dialog);

        RecyclerView mRecyclerView = dialog.findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST, R.drawable.list_divider_nospacing));

        //adapter should only ever be created once on fragment initialise.
        GenreAdapter mAdapter = new GenreAdapter(this, getGenreList(), mSelectedPosition);
        mAdapter.setOnItemSelectionListener(new GenreAdapter.OnItemSelectionListener() {
          @Override
          public void onItemSelect(View v, Genre item, int position) {
            mSelectedPosition = position;
            MediaProvider mProvider = providerManager.getCurrentMediaProvider();
            mProvider.cancel();
            for (Fragment frag : mediaFragment.getChildFragmentManager().getFragments()) {
              if (frag instanceof MediaListFragment) {
                ((MediaListFragment) frag).changeGenre(item.getKey());
              }
            }
            dialog.dismiss();
          }
        });
        mRecyclerView.setAdapter(mAdapter);

        dialog.show();

        break;
      case R.id.action_search:
        //start the search activity
        SearchActivity.startActivity(this);
        break;
    }
    return super.onOptionsItemSelected(item);
  }


  @Override
  public void onProviderChanged(@ProviderManager.ProviderType int provider) {
    showProvider(provider);
  }

  public void updateTabs(MediaContainerFragment containerFragment, final int position) {
    if (mTabs == null)
      return;

    if (containerFragment != null) {
      ViewPager viewPager = containerFragment.getViewPager();
      if (viewPager == null)
        return;

      mTabs.setupWithViewPager(viewPager);
      mTabs.setTabGravity(TabLayout.GRAVITY_CENTER);
      mTabs.setTabMode(TabLayout.MODE_SCROLLABLE);
      mTabs.setVisibility(View.VISIBLE);

      viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabs));
      mTabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

      if (mTabs.getTabCount() > 0) {
        mTabs.getTabAt(0).select();
        mHandler.postDelayed(new Runnable() {
          @Override
          public void run() {
            if (mTabs.getTabCount() > position)
              mTabs.getTabAt(position).select();
          }
        }, 10);
      }

    } else {
      mTabs.setVisibility(View.GONE);
    }
  }

  private void showProvider(@ProviderManager.ProviderType int provider) {
    setTitle(ProviderUtils.getProviderTitle(provider));
    // update the main content by replacing fragments
    FragmentManager fragmentManager = getSupportFragmentManager();

    mediaFragment = MediaContainerFragment.newInstance();

    if (mTabs.getTabCount() > 0) {
      mTabs.getTabAt(0).select();
    }

    fragmentManager.beginTransaction().replace(R.id.container, mediaFragment).commit();

    updateTabs(mediaFragment, mediaFragment.getCurrentSelection());
  }

  private void openPlayerTestDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);

    final String[] file_types = getResources().getStringArray(R.array.file_types);
    final String[] files = getResources().getStringArray(R.array.files);

    builder.setTitle("Player Tests")
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
          }
        }).setSingleChoiceItems(file_types, -1, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int index) {
        dialogInterface.dismiss();
        final String location = files[index];
        if (location.equals("dialog")) {
          final EditText dialogInput = new EditText(MainActivity.this);
          dialogInput.setText(
              "http://download.wavetlan.com/SVV/Media/HTTP/MP4/ConvertedFiles/QuickTime/QuickTime_test13_5m19s_AVC_VBR_324kbps_640x480_25fps_AAC-LCv4_CBR_93.4kbps_Stereo_44100Hz.mp4");
          AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
              .setView(dialogInput)
              .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  Movie media = new Movie();

                  media.videoId = "dialogtestvideo";
                  media.title = "User input test video";

                  String location = dialogInput.getText().toString();

                  BeamManager bm = BeamManager.getInstance(MainActivity.this);
                  if (bm.isConnected()) {
                    BeamPlayerActivity.startActivity(MainActivity.this,
                        new StreamInfo(media, null, null, null, null, location), 0);
                  } else {
                    VideoPlayerActivity.startActivity(MainActivity.this,
                        new StreamInfo(media, null, null, null, null, location), 0);
                  }
                }
              });
          builder.show();
        } else if (youTubeManager.isYouTubeUrl(location)) {
          Intent i = new Intent(MainActivity.this, TrailerPlayerActivity.class);
          Movie media = new Movie();
          media.title = file_types[index];
          i.putExtra(TrailerPlayerActivity.DATA, media);
          i.putExtra(TrailerPlayerActivity.LOCATION, location);
          startActivity(i);
        } else {
          final Movie media = new Movie();
          media.videoId = "bigbucksbunny";
          media.title = file_types[index];
          media.subtitles = new HashMap<>();
          media.subtitles.put("en", "http://sv244.cf/bbb-subs.srt");

          providerManager.getCurrentSubsProvider().download(media, "en", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
              BeamManager bm = BeamManager.getInstance(MainActivity.this);

              if (bm.isConnected()) {
                BeamPlayerActivity.startActivity(MainActivity.this,
                    new StreamInfo(media, null, null, null, null, location), 0);
              } else {
                VideoPlayerActivity.startActivity(MainActivity.this,
                    new StreamInfo(media, null, null, null, null, location), 0);
              }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
              BeamManager bm = BeamManager.getInstance(MainActivity.this);
              if (bm.isConnected()) {
                BeamPlayerActivity.startActivity(MainActivity.this,
                    new StreamInfo(media, null, null, "en", null, location), 0);
              } else {
                VideoPlayerActivity.startActivity(MainActivity.this,
                    new StreamInfo(media, null, null, "en", null, location), 0);
              }
            }
          });
        }
      }
    });

    builder.show();
  }

  private List<Genre> getGenreList() {
    List<Genre> genreList = new ArrayList<>();
    genreList.add(new Genre("all", org.hifnawy.base.R.string.genre_all));
    genreList.add(new Genre("action", org.hifnawy.base.R.string.genre_action));
    genreList.add(new Genre("adventure", org.hifnawy.base.R.string.genre_adventure));
    genreList.add(new Genre("animation", org.hifnawy.base.R.string.genre_animation));
    genreList.add(new Genre("comedy", org.hifnawy.base.R.string.genre_comedy));
    genreList.add(new Genre("crime", org.hifnawy.base.R.string.genre_crime));
    genreList.add(new Genre("disaster", org.hifnawy.base.R.string.genre_disaster));
    genreList.add(new Genre("documentary", org.hifnawy.base.R.string.genre_documentary));
    genreList.add(new Genre("drama", org.hifnawy.base.R.string.genre_drama));
    genreList.add(new Genre("eastern", org.hifnawy.base.R.string.genre_eastern));
    genreList.add(new Genre("family", org.hifnawy.base.R.string.genre_family));
    genreList.add(new Genre("fantasy", org.hifnawy.base.R.string.genre_fantasy));
    genreList.add(new Genre("fan-film", org.hifnawy.base.R.string.genre_fan_film));
    genreList.add(new Genre("film-noir", org.hifnawy.base.R.string.genre_film_noir));
    genreList.add(new Genre("history", org.hifnawy.base.R.string.genre_history));
    genreList.add(new Genre("holiday", org.hifnawy.base.R.string.genre_holiday));
    genreList.add(new Genre("horror", org.hifnawy.base.R.string.genre_horror));
    genreList.add(new Genre("indie", org.hifnawy.base.R.string.genre_indie));
    genreList.add(new Genre("music", org.hifnawy.base.R.string.genre_music));
    genreList.add(new Genre("mystery", org.hifnawy.base.R.string.genre_mystery));
    genreList.add(new Genre("road", org.hifnawy.base.R.string.genre_road));
    genreList.add(new Genre("romance", org.hifnawy.base.R.string.genre_romance));
    genreList.add(new Genre("science-fiction", org.hifnawy.base.R.string.genre_sci_fi));
    genreList.add(new Genre("short", org.hifnawy.base.R.string.genre_short));
    genreList.add(new Genre("sports", org.hifnawy.base.R.string.genre_sport));
    genreList.add(new Genre("suspense", org.hifnawy.base.R.string.genre_suspense));
    genreList.add(new Genre("thriller", org.hifnawy.base.R.string.genre_thriller));
    genreList.add(new Genre("tv-movie", org.hifnawy.base.R.string.genre_tv_movie));
    genreList.add(new Genre("war", org.hifnawy.base.R.string.genre_war));
    genreList.add(new Genre("western", org.hifnawy.base.R.string.genre_western));

    return genreList;
  }

  private boolean isFullAccessStorageAllowed() {
    int read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
    int write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    switch (requestCode) {
      case PERMISSIONS_REQUEST: {
        if (grantResults.length < 1 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
          Toast.makeText(this, "Permissions granted, now you can read and write the storage", Toast.LENGTH_LONG).show();
          finish();
        }
      }
    }
  }

}