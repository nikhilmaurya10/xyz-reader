package com.example.xyzreader.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.util.Pair;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends DialogFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    private static final String ARG_POSITION = "transition_string_position";
    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 2.55f;
    int orientation = Configuration.ORIENTATION_UNDEFINED;
    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private ObservableScrollView mScrollView;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private ColorDrawable mStatusBarColorDrawable;
    private int mTopInset;
    private ImageView mPhotoView;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;
    private int mPosition;
    private FloatingActionButton fab;
    TextView bodyView;
    Date publishedDate;
    TextView bylineView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId, int position) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        arguments.putInt(ARG_POSITION, position);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        initOnCreate();
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initOnCreate();
    }

    void initOnCreate() {
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        if (getArguments().containsKey(ARG_POSITION)) {
            mPosition = getArguments().getInt(ARG_POSITION);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);

    }
    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mRootView.findViewById(R.id.photo).setTransitionName(getString(R.string.transition_photo)+String.valueOf(mPosition));
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout)
                mRootView.findViewById(R.id.draw_insets_frame_layout);
        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                mTopInset = insets.top;
            }
        });
        fab = (FloatingActionButton) mRootView.findViewById(R.id.share_fab);
        mScrollView = (ObservableScrollView) mRootView.findViewById(R.id.scrollview);

        mScrollView.setCallbacks(new ObservableScrollView.Callbacks() {
            @Override
            public void onScrollChanged(int dx, int dy) {
                mScrollY = mScrollView.getScrollY();
                int totalHeight = mScrollView.getChildAt(0).getMeasuredHeight() - mScrollView.getMeasuredHeight();

                getActivityCast().onUpButtonFloorChanged(mItemId, ArticleDetailFragment.this);

                DisplayMetrics displaymetrics = new DisplayMetrics();
                getActivityCast().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                int screenWidth = displaymetrics.widthPixels;
                int screenHeight = displaymetrics.heightPixels;

                if(screenWidth == screenHeight){
                    orientation = Configuration.ORIENTATION_SQUARE;
                } else{
                    if(screenWidth < screenHeight){
                        orientation = Configuration.ORIENTATION_PORTRAIT;
                        mPhotoView.setTranslationY((int) (mScrollY - mScrollY / PARALLAX_FACTOR));
                    }else {
                        orientation = Configuration.ORIENTATION_LANDSCAPE;
                        if (mIsCard) {
                            mPhotoView.setTranslationY((int) (mScrollY - mScrollY / PARALLAX_FACTOR));
                        }
                    }
                }

                if (mScrollY == 0 ) {
                    fab.show();                     // show fab on top
                } else if( dy<0 ) fab.hide();       // hide fab on scroll down
                if (mScrollY == totalHeight) {
                    fab.show();                     // show fab when at bottom
                } else if (dy > 0 && dy <100 && mScrollY != 0) fab.hide(); // hide fab when slowly scrolling upward
                if (dy > 300) fab.show(); //show fab on fast upward fling


                updateStatusBar();
            }
        });

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);

        mStatusBarColorDrawable = new ColorDrawable(0);

        fab = (FloatingActionButton) mRootView.findViewById(R.id.share_fab);
        fab.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        fab.startAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.fab_rotate));
        startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                .setType("text/plain")
                .setText("Some sample text")
                .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();
        updateStatusBar();
        return mRootView;
    }

    private void updateStatusBar() {
        int color = 0;
        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);
            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mMutedColor) * 0.9),
                    (int) (Color.green(mMutedColor) * 0.9),
                    (int) (Color.blue(mMutedColor) * 0.9));
        }
        mStatusBarColorDrawable.setColor(color);
        if (getActivity() != null) getActivity().getWindow().setStatusBarColor(color);
        mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        bodyView = (TextView) mRootView.findViewById(R.id.article_body);

        titleView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Montserrat-Medium.ttf"));
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Montserrat-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setAlpha(1);
            mRootView.setVisibility(View.VISIBLE);

            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(final ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                                    public void onGenerated(Palette p) {
                                        mMutedColor = p.getDarkMutedColor(0xFF333333);
                                        mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                        mRootView.findViewById(R.id.meta_bar)
                                                .setBackgroundColor(mMutedColor);
                                        updateStatusBar();
                                    }
                                });

                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
            AsyncTask task = new BodyTextLoaderTask();
            task.execute();
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor cursor) {
        getActivityCast().scheduleStartPostponedTransition(mPhotoView);
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        mCursor = null;
        bindViews();
    }


    public int getUpButtonFloor() {
        if (mPhotoView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) mPhotoView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }

    class BodyTextLoaderTask extends AsyncTask<Object,Void,Pair<Spanned,Spanned>> {

        @Override
        protected Pair<Spanned,Spanned> doInBackground(Object... params) {

            publishedDate = parsePublishedDate();
            Spanned byline;
            String htmlString;
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                htmlString = DateUtils.getRelativeTimeSpanString(
                        publishedDate.getTime(),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString()
                        + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                        + "</font>";
            } else {
                // If date is before 1902, just show the string
                htmlString = outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                        + "</font>";
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                byline = Html.fromHtml(htmlString, 0);
            } else byline = Html.fromHtml(htmlString) ;

            Spanned bodyText;
            String bodyHtmlString = mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                bodyText = Html.fromHtml(bodyHtmlString, 0);
            } else bodyText = Html.fromHtml(bodyHtmlString) ;

            return new Pair(bodyText , byline);
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Pair spannedText) {
            if(bodyView != null) {
                bodyView.setText((Spanned)spannedText.first);
                bodyView.setMovementMethod(LinkMovementMethod.getInstance());

            }
            if(bylineView !=null) {
                bylineView.setText((Spanned)spannedText.second);
            }


        }

    }
}
