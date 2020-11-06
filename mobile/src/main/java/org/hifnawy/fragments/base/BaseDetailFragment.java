package org.hifnawy.fragments.base;

import android.content.Context;
import androidx.fragment.app.Fragment;
import android.view.View;

import org.hifnawy.activities.MediaDetailActivity;
import org.hifnawy.base.torrent.StreamInfo;

public abstract class BaseDetailFragment extends Fragment {

    protected FragmentListener mCallback;
    protected MediaDetailActivity mActivity;
    protected View mRoot;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MediaDetailActivity)
            mActivity = (MediaDetailActivity) context;
    }

    public interface FragmentListener {
        void playStream(StreamInfo streamInfo);
    }

}
