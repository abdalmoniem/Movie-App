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

package org.hifnawy.base.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstreamserver.TorrentServerListener;

import java.util.Map;

import javax.inject.Inject;

import org.hifnawy.base.R;
import org.hifnawy.base.activities.TorrentActivity;
import org.hifnawy.base.content.preferences.Prefs;
import org.hifnawy.base.manager.provider.ProviderManager;
import org.hifnawy.base.providers.media.models.Episode;
import org.hifnawy.base.providers.media.models.Media;
import org.hifnawy.base.providers.media.models.Movie;
import org.hifnawy.base.providers.subs.SubsProvider;
import org.hifnawy.base.subs.SubtitleDownloader;
import org.hifnawy.base.subs.TimedTextObject;
import org.hifnawy.base.torrent.StreamInfo;
import org.hifnawy.base.torrent.TorrentService;
import org.hifnawy.base.utils.PrefUtils;
import org.hifnawy.base.utils.ThreadUtils;
import hugo.weaving.DebugLog;


/**
 * This fragment handles starting a stream of a torrent.
 * <p/>
 * <p/>
 * It does multiple things:
 * <p/>
 * <pre>
 * 1. Downloads torrent file
 * 2. Downloads subtitles file
 * (1 and 2 happen asynchronously. Generally the torrent will take much longer than downloading subs)
 *
 * 3. Starts downloading (buffering) the torrent
 * 4. Starts the Video activity
 * </pre>
 * <p/>
 * <p/>
 * <p/>
 * //todo: most of this logic should probably be factored out into its own service at some point
 */
public abstract class BaseStreamLoadingFragment extends Fragment
        implements TorrentServerListener,
        SubtitleDownloader.ISubtitleDownloaderListener,
        SubsProvider.Callback {

    @Inject
    ProviderManager providerManager;

    protected FragmentListener mCallback;
    protected boolean mPlayingExternal = false;
    protected Boolean mPlayerStarted = false;
    private Boolean mHasSubs = false;
    private TorrentService mService;
    private Context streamLoadingActivityContext;

    protected StreamInfo mStreamInfo;
    private State mState;

    private enum SubsStatus {SUCCESS, FAILURE, DOWNLOADING}

    private SubsStatus mSubsStatus = SubsStatus.DOWNLOADING;
    private String mVideoLocation = "";

    public enum State {
        UNINITIALISED, WAITING_TORRENT, WAITING_SUBTITLES, BUFFERING, STREAMING, ERROR
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStreamInfo = mCallback.getStreamInformation();
                if (mStreamInfo == null) {
                    getActivity().finish();
                    return;
                }
                loadSubtitles();
            }
        });

        if (!(getActivity() instanceof TorrentActivity)) {
            throw new IllegalStateException("Parent activity is not a TorrentBaseActivity");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentListener) mCallback = (FragmentListener) context;
    }

    public void onTorrentServiceConnected() {
        if(getActivity() == null)
            return;

        mService = ((TorrentActivity)getActivity()).getTorrentService();

        if (streamLoadingActivityContext != null) {
            mService.setStreamLoadingActivityContext(streamLoadingActivityContext);
        }
        if(mService != null) {
            mService.addListener(this);
            startStream();
        }
    }

    public void setStreamLoadingActivityContext(Context context) {
        streamLoadingActivityContext = context;
    }

    public void onTorrentServiceDisconnected() {
        if(mService != null) {
            mService.removeListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null != mService) {
            mService.removeListener(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (null != mService) {
            mService.stopStreaming();
            mService.removeListener(this);
            mService.stopService();
        }
    }

    /**
     * Update the view based on a state.
     *
     * @param state View state
     * @param extra - an optional extra piece of data relating to the state, such as an error message, or status data
     */
    protected abstract void updateView(State state, Object extra);

    /**
     * Start the internal player for a streaming torrent
     *
     * @param location Location
     * @param resumePosition Resume position
     */
    protected abstract void startPlayerActivity(String location, int resumePosition);

    @DebugLog
    protected void setState(final State state) {
        setState(state, null);
    }

    @DebugLog
    private void setState(final State state, final Object extra) {
        mState = state;

        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateView(state, extra);
            }
        });
    }

    /**
     * Starts the player for a torrent stream.
     * <p/>
     * Will either start an external player, or the internal one
     */
    @DebugLog
    private void startPlayer(String location) {
        if (mHasSubs && mSubsStatus == SubsStatus.DOWNLOADING) {
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setState(State.WAITING_SUBTITLES);
                }
            });
            return;
        }

        if (!mPlayerStarted) {
            mService.removeListener(BaseStreamLoadingFragment.this);
            startPlayerActivity(location, 0);

            mPlayerStarted = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPlayerStarted) {
            if(!mPlayingExternal)
                getActivity().onBackPressed();
        }

        if(mService != null && mService.isStreaming() && mService.isReady()) {
            onStreamReady(mService.getCurrentTorrent());
            onServerReady(mService.getCurrentStreamUrl());
        }

        if(mState == null) {
            setState(State.WAITING_TORRENT);
        } else {
            setState(mState);
        }
    }

    /**
     * Starts the torrent service streaming a torrent url
     */
    private void startStream() {
        if (null == mService) throw new IllegalStateException("Torrent service must be bound");
        String torrentUrl = mStreamInfo.getTorrentUrl();

        //if the torrent service is currently streaming another file, stop it.
        if (mService.isStreaming() && !mService.getCurrentTorrentUrl().equals(torrentUrl)) {
            mService.stopStreaming();
        } else if(mService.isReady()) {
            onStreamReady(mService.getCurrentTorrent());
            onServerReady(mService.getCurrentStreamUrl());
        }

        //start streaming the new file
        mService.streamTorrent(torrentUrl);
    }

    /**
     * Stops the torrent service streaming
     */
    @DebugLog
    public void cancelStream() {
        if (mService != null) {
            mService.stopStreaming();
        }
    }

    @Override
    @DebugLog
    public void onStreamStarted(Torrent torrent) {
        setState(State.BUFFERING);
    }

    @Override
    @DebugLog
    public void onStreamError(Torrent torrent, final Exception e) {
        if (e.getMessage().equals("Write error")) {
            setState(State.ERROR, getString(R.string.error_files));
        } else if (e.getMessage().equals("Torrent error")) {
            setState(State.ERROR, getString(R.string.torrent_failed));
        } else {
            setState(State.ERROR, getString(R.string.unknown_error));
        }
    }

    /**
     * Called when torrent buffering has reached 100%
     *
     * @param torrent The torrent file
     */
    @Override
    @DebugLog
    public void onStreamReady(Torrent torrent) { }

    /**
     * Called when the torrent server is running
     * @param url
     */
    @Override
    @DebugLog
    public void onServerReady(String url) {
        startPlayer(url);
    }

    /**
     * Called when the torrent buffering status has been updated
     *
     * @param torrent The torrent
     * @param status Stream status
     */
    @Override
    @DebugLog
    public void onStreamProgress(Torrent torrent, StreamStatus status) {
        if (!mVideoLocation.isEmpty()) {
            startPlayer(mVideoLocation);
        }
        setState(State.STREAMING, status);
    }

    @Override
    public void onStreamPrepared(Torrent torrent) { }

    @Override
    public void onStreamStopped() {

    }

    /**
     * Downloads the subs file
     */
    private void loadSubtitles() {
        Media media = mStreamInfo.getMedia();
        if (media == null) return;

        SubsProvider subsProvider = providerManager.getCurrentSubsProvider();
        if (subsProvider == null) return;

        if (mStreamInfo.isShow()) {
            subsProvider.getList((Episode) media, this);
        }
        else {
            subsProvider.getList((Movie) media, this);
        }
    }

    @Override
    public void onSuccess(Map<String, String> items) {
        Media media = mStreamInfo.getMedia();
        media.subtitles = items;

        mSubsStatus = SubsStatus.SUCCESS;
        mHasSubs = false;

        if (media.subtitles == null || media.subtitles.size() == 0) return;

        if (mStreamInfo.getSubtitleLanguage() == null) {
            if (media.subtitles.containsKey(PrefUtils.get(getActivity(), Prefs.SUBTITLE_DEFAULT, SubsProvider.SUBTITLE_LANGUAGE_NONE))) {
                mStreamInfo.setSubtitleLanguage(PrefUtils.get(getActivity(), Prefs.SUBTITLE_DEFAULT, SubsProvider.SUBTITLE_LANGUAGE_NONE));
            }
            else {
                mStreamInfo.setSubtitleLanguage(SubsProvider.SUBTITLE_LANGUAGE_NONE);
            }
        }

        if (mStreamInfo.getSubtitleLanguage() != null && !mStreamInfo.getSubtitleLanguage().equals(SubsProvider.SUBTITLE_LANGUAGE_NONE)) {
            String mSubtitleLanguage = mStreamInfo.getSubtitleLanguage();
            mSubsStatus = SubsStatus.DOWNLOADING;
            mHasSubs = true;
            SubtitleDownloader subtitleDownloader = new SubtitleDownloader(providerManager.getCurrentSubsProvider(),
                    getActivity(), mStreamInfo, mSubtitleLanguage);
            subtitleDownloader.setSubtitleDownloaderListener(this);
            subtitleDownloader.downloadSubtitle();
        }
    }

    @Override
    public void onFailure(Exception e) {
        mSubsStatus = SubsStatus.FAILURE;
    }

    @Override
    public void onSubtitleDownloadCompleted(boolean isSuccessful, TimedTextObject subtitleFile) {
        mSubsStatus = isSuccessful ? SubsStatus.SUCCESS : SubsStatus.FAILURE;
    }

    public interface FragmentListener {
        StreamInfo getStreamInformation();
    }

}
