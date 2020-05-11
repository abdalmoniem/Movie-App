package butter.droid.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import androidx.fragment.app.FragmentManager;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butter.droid.R;
import butter.droid.base.fragments.dialog.StringArraySelectorDialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;

public class OptionSelector extends LinearLayout {

    View mView;
    @BindView(android.R.id.text1)
    TextView mText1;
    @BindView(android.R.id.text2)
    TextView mText2;
    @BindView(android.R.id.icon)
    ImageView mIcon;

    private FragmentManager mFragmentManager;
    private String[] mData = new String[0];
    private int mDefaultOption = -1, mTitle;
    private SelectorListener mListener;

    public OptionSelector(Context context) {
        super(context);
    }

    public OptionSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, android.R.style.Widget_Button);
    }

    public OptionSelector(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public void init(Context context, AttributeSet attrs, int defStyle) {
        setClickable(true);
        setFocusable(true);

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = layoutInflater.inflate(R.layout.optionselector, this);
        ButterKnife.bind(this, mView);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OptionSelector, defStyle, 0);

        String str = a.getString(R.styleable.OptionSelector_optionText);
        if (!TextUtils.isEmpty(str)) {
            mText1.setText(str);
            setContentDescription(str);
        }

        int res = a.getResourceId(R.styleable.OptionSelector_optionIcon, R.mipmap.ic_launcher);
        mIcon.setImageResource(res);

        setOnClickListener(mOnClickListener);

        a.recycle();
    }

    public void setText(String str) {
        mText1.setText(str);
    }

    public void setError(String errorMessage, String error) {
        mText1.setTextColor(Color.RED);
        mText1.setTypeface(Typeface.DEFAULT_BOLD);
        mText1.setText(errorMessage);

        mText2.setText(error);
        mText2.setVisibility(VISIBLE);
    }

    public void setText(int strRes) {
        mText1.setText(strRes);
    }

    public void setTitle(int strRes) {
        mTitle = strRes;
    }

    public void setIcon(int iconRes) {
        mIcon.setImageResource(iconRes);
    }

    public void setListener(SelectorListener listener) {
        mListener = listener;
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    public void setData(String[] data) {
        mData = data.clone();
    }

    public void setDefault(int defaultOption) {
        mDefaultOption = defaultOption;
    }

    OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mFragmentManager == null) return;
            StringArraySelectorDialogFragment.showSingleChoice(mFragmentManager, mTitle, mData, mDefaultOption,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int position) {
                            if (mListener != null)
                                mListener.onSelectionChanged(position, mData[position]);
                            mDefaultOption = position;
                            setText(mData[position]);
                            dialog.dismiss();
                        }
                    }
            );
        }
    };

    public interface SelectorListener {
        public void onSelectionChanged(int position, String value);
    }

}
