package com.arbo.gaogao.app.main_tabs.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.arbo.gaogao.Config;
import com.arbo.gaogao.R;
import com.arbo.gaogao.app.main_tabs.presenter.ZhihuStoryContract;
import com.arbo.gaogao.app.main_tabs.presenter.ZhihuStoryPresenterImpl;
import com.arbo.gaogao.model.zhihu.ZhihuStory;
import com.arbo.gaogao.util.AnimUtils;
import com.arbo.gaogao.util.ColorUtils;
import com.arbo.gaogao.util.DensityUtil;
import com.arbo.gaogao.util.GlideUtils;
import com.arbo.gaogao.util.ViewUtils;
import com.arbo.gaogao.util.WebUtil;
import com.arbo.gaogao.widget.ElasticDragDismissFrameLayout;
import com.arbo.gaogao.widget.ParallaxScrimageView;
import com.arbo.gaogao.widget.TranslateYTextView;
import com.arbo.lib.base.ui.BaseActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.lang.reflect.InvocationTargetException;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2017/1/29.
 */

public class ZhihuStoryActivity extends BaseActivity implements ZhihuStoryContract.View {

    private static final float SCRIM_ADJUSTMENT = 0.075f;

    @BindView(R.id.shot)
    ParallaxScrimageView shot;
    @BindView(R.id.title)
    TranslateYTextView mtitle;
    @BindView(R.id.webView_zhihu)
    WebView webViewZhihu;
    @BindView(R.id.nest)
    NestedScrollView nest;
    @BindView(R.id.container)
    FrameLayout container;
    @BindView(R.id.draggable_frame)
    ElasticDragDismissFrameLayout draggableFrame;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    String storyId;
    int[] mDeviceInfo;
    int width;
    int heigh;
    private Transition.TransitionListener zhihuReturnHomeListener;
    private NestedScrollView.OnScrollChangeListener scrollListener;
    private ElasticDragDismissFrameLayout.SystemChromeFader chromeFader;


    private ZhihuStoryPresenterImpl mPresenter;

    @Override
    protected int initLayoutID() {
        return R.layout.zhihu_story_activity_layout;
    }

    @Override
    protected void initViews() {
        super.initViews();
        ButterKnife.bind(this);
        mPresenter = new ZhihuStoryPresenterImpl(this, this);
        mDeviceInfo = DensityUtil.getDeviceInfo(this);
        width = mDeviceInfo[0];
        heigh = width * 3 / 4;
        //若需要更改toolbar标题，需要在setSupportAction之前就调用
        toolbar.setTitle(R.string.zhihu);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initTitleAndID();
        initToolBar();
        initWebView();
        getData(storyId);

    }

    private void initWebView() {
        WebSettings settings = webViewZhihu.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        //settings.setUseWideViewPort(true);造成文字太小
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAppCachePath(getCacheDir().getAbsolutePath() + "/webViewCache");
        settings.setAppCacheEnabled(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webViewZhihu.setWebChromeClient(new WebChromeClient());
    }

    private void initTitleAndID(){
        storyId = getIntent().getStringExtra("id");
        String title = getIntent().getStringExtra("title");
        mtitle.setText(title);
    }

    private void initToolBar(){
        toolbar.setTitleMargin(20,20,0,10);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nest.smoothScrollTo(0,0);
            }
        });
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandImageAndFinish();
            }
        });
    }

    @Override
    protected void initViewListener() {
        super.initViewListener();
        initReturnHomeListener();
        nest.setOnScrollChangeListener(scrollListener);

        chromeFader = new ElasticDragDismissFrameLayout.SystemChromeFader(this);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){

            getWindow().getSharedElementReturnTransition().addListener(zhihuReturnHomeListener);
            getWindow().setSharedElementEnterTransition(new ChangeBounds());
        }
        enterAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        draggableFrame.addListener(chromeFader);
        try {
            webViewZhihu.getClass().getMethod("onResume").invoke(webViewZhihu, (Object[]) null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        draggableFrame.removeListener(chromeFader);
        try {
            //invoke参数2 应该为为Object[]
            webViewZhihu.getClass().getMethod("onPause").invoke(webViewZhihu, (Object[]) null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            getWindow().getSharedElementEnterTransition().removeListener(zhihuReturnHomeListener);
        }
        //防止webView内存泄露
        if(webViewZhihu!=null){
            ((ViewGroup)webViewZhihu.getParent()).removeView(webViewZhihu);
            webViewZhihu.destroy();
            webViewZhihu = null;
        }
        mPresenter.unsubcrible();
        super.onDestroy();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        expandImageAndFinish();
    }

    private void expandImageAndFinish() {
        if (shot.getOffset() != 0f) {
            Animator expandImage = ObjectAnimator.ofFloat(shot, ParallaxScrimageView.OFFSET,
                    0f);
            expandImage.setDuration(80);
            expandImage.setInterpolator(new AccelerateInterpolator());
            expandImage.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {

                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                        finishAfterTransition();
                    }else {
                        finish();
                    }
                }
            });
            expandImage.start();
        } else {
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                finishAfterTransition();
            }else {
                finish();
            }
        }
    }

    private void initReturnHomeListener() {
        zhihuReturnHomeListener =
                new AnimUtils.TransitionListenerAdapter() {
                    @Override
                    public void onTransitionStart(Transition transition) {
                        super.onTransitionStart(transition);
                        // hide the fab as for some reason it jumps position??  TODO work out why
                        toolbar.animate()
                                .alpha(0f)
                                .setDuration(100)
                                .setInterpolator(new AccelerateInterpolator());
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                            shot.setElevation(1f);
                            toolbar.setElevation(0f);
                        }
                        nest.animate()
                                .alpha(0f)
                                .setDuration(50)
                                .setInterpolator(new AccelerateInterpolator());
                    }
                };
        scrollListener = new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (oldScrollY<168){
                    shot.setOffset(-oldScrollY);
                    mtitle.setOffset(-oldScrollY);
                }

            }
        };
    }

    private void enterAnimation() {
        float offSet = toolbar.getHeight();
        LinearInterpolator interpolator=new LinearInterpolator();
        viewEnterAnimation(shot, offSet, interpolator);
        viewEnterAnimationNest(nest,0f,interpolator);

    }

    private void viewEnterAnimation(View view, float offset, Interpolator interp) {
        view.setTranslationY(-offset);
        view.setAlpha(0f);
        view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500L)
                .setInterpolator(interp)
                .setListener(null)
                .start();
    }
    private void viewEnterAnimationNest(View view, float offset, Interpolator interp) {
        view.setTranslationY(-offset);
        view.setAlpha(0.3f);
        view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500L)
                .setInterpolator(interp)
                .setListener(null)
                .start();
    }

    @Override
    public void setPresenter(Object presenter) {
        this.mPresenter = (ZhihuStoryPresenterImpl) presenter;
    }

    @Override
    public void showWebView(ZhihuStory zhihuStory) {
        boolean isBodyEmpty = TextUtils.isEmpty(zhihuStory.getBody());
        if(isBodyEmpty){
            webViewZhihu.loadUrl(zhihuStory.getmShareUrl());
        } else {
            String body = zhihuStory.getBody();
            String[] css = zhihuStory.getCss();
            String data = WebUtil.buildHtmlWithCss(body,css, Config.isNight);
            webViewZhihu.loadDataWithBaseURL(WebUtil.BASE_URL,data,WebUtil.MIME_TYPE,WebUtil.ENCODING,WebUtil.FAIL_URL);
        }
    }

    @Override
    public void showTitle(String t) {
        mtitle.setText(t);
    }

    @Override
    public void showImage(String url) {
        Glide
                .with(this)
                .load(url)
                .listener(loadListener)
                .centerCrop().override(width,heigh)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(shot);
    }


    private RequestListener loadListener = new RequestListener<String, GlideDrawable>() {
        @Override
        public boolean onResourceReady(GlideDrawable resource, String model,
                                       Target<GlideDrawable> target, boolean isFromMemoryCache,
                                       boolean isFirstResource) {
            final Bitmap bitmap = GlideUtils.getBitmap(resource);
            final int twentyFourDip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    24, ZhihuStoryActivity.this.getResources().getDisplayMetrics());
            Palette.from(bitmap)
                    .maximumColorCount(3)
                    .clearFilters() /* by default palette ignore certain hues
                        (e.g. pure black/white) but we don't want this. */
                    .setRegion(0, 0, bitmap.getWidth() - 1, twentyFourDip) /* - 1 to work around
                        https://code.google.com/p/android/issues/detail?id=191013 */
                    .generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            boolean isDark;
                            @ColorUtils.Lightness int lightness = ColorUtils.isDark(palette);
                            if (lightness == ColorUtils.LIGHTNESS_UNKNOWN) {
                                isDark = ColorUtils.isDark(bitmap, bitmap.getWidth() / 2, 0);
                            } else {
                                isDark = lightness == ColorUtils.IS_DARK;
                            }

                            // color the status bar. Set a complementary dark color on L,
                            // light or dark color on M (with matching status bar icons)
                            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){


                                int statusBarColor = getWindow().getStatusBarColor();
                                final Palette.Swatch topColor =
                                        ColorUtils.getMostPopulousSwatch(palette);
                                if (topColor != null &&
                                        (isDark || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
                                    statusBarColor = ColorUtils.scrimify(topColor.getRgb(),
                                            isDark, SCRIM_ADJUSTMENT);
                                    // set a light status bar on M+
                                    if (!isDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        ViewUtils.setLightStatusBar(shot);
                                    }
                                }

                                if (statusBarColor != getWindow().getStatusBarColor()) {
                                    shot.setScrimColor(statusBarColor);
                                    ValueAnimator statusBarColorAnim = ValueAnimator.ofArgb(
                                            getWindow().getStatusBarColor(), statusBarColor);
                                    statusBarColorAnim.addUpdateListener(new ValueAnimator
                                            .AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator animation) {
                                            getWindow().setStatusBarColor(
                                                    (int) animation.getAnimatedValue());
                                        }
                                    });
                                    statusBarColorAnim.setDuration(1000L);
                                    statusBarColorAnim.setInterpolator(
                                            new AccelerateInterpolator());
                                    statusBarColorAnim.start();
                                }
                            }

                        }
                    });
            Palette.from(bitmap)
                    .clearFilters()
                    .generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {

                            // slightly more opaque ripple on the pinned image to compensate
                            // for the scrim
                            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){

                                shot.setForeground(ViewUtils.createRipple(palette, 0.3f, 0.6f,
                                        ContextCompat.getColor(ZhihuStoryActivity.this, R.color.mid_grey),
                                        true));
                            }
                        }
                    });

            // TODO should keep the background if the image contains transparency?!
            shot.setBackground(null);
            return false;
        }

        @Override
        public boolean onException(Exception e, String model, Target<GlideDrawable> target,
                                   boolean isFirstResource) {
            return false;
        }
    };


    private void getData(String id) {
        mPresenter.getZhihuStory(id);
    }


}
