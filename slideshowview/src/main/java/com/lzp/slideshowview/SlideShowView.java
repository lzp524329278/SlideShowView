package com.lzp.slideshowview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * ViewPager实现的轮播图广告自定义视图，如京东首页的广告轮播图效果；
 * 既支持自动轮播页面也支持手势滑动切换页面
 */

public class SlideShowView extends FrameLayout {

    private volatile boolean isStart = true;
    private DisplayImageOptions options;

    private ImageLoader imageLoader;

    //自动轮播的时间间隔
    private  static int TIME_INTERVAL = 3000;

    //自定义轮播图的资源
    private String[] imageUrls;
    //放轮播图片的ImageView 的list
    private List<ImageView> imageViewsList;
    //放圆点的View的list
    private List<View> dotViewsList;

    private ViewPager viewPager;
    //当前轮播页
    private int currentItem = 0;

    private LinearLayout dotLayout;
    //上一次轮播结束的时间
    private volatile long lastTime;

    private Context context;

    private RelativeLayout rl_normal_show;

    private LinearLayout rl_loading_show;

    private UrlLoading urlLoading;

    private TextView tv;
    //Handler
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if (isStart) {
                        lastTime = System.currentTimeMillis();
                        viewPager.setCurrentItem(currentItem);
                        handler.postDelayed(new SlideShowTask(), TIME_INTERVAL);
                    }
                    break;
                case 2:
                    initLoadingUI(context);
                    break;
            }
        }
    };


    public SlideShowView(Context context) {
        this(context, null);
        // TODO Auto-generated constructor stub
    }

    public SlideShowView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        // TODO Auto-generated constructor stub
    }

    public SlideShowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (isInEditMode()) {
            return;
        }
        this.context = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.SlideShowView);

        Drawable drawable = typedArray.getDrawable(R.styleable.SlideShowView_DrawableOnLoading);
        int color = attrs.getAttributeIntValue("android", "color", 0xffc1c1c1);
        initImageLoader(context);
        initUI(context, drawable, color);
    }

    private void initUI(Context context, Drawable drawable, int color) {

        FrameLayout fl = new FrameLayout(context);
        fl.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        fl.setBackgroundColor(color);

        viewPager = new ViewPager(context);
        viewPager.setLayoutParams(new ViewPager.LayoutParams());
        dotLayout = new LinearLayout(context);

        dotLayout.setOrientation(LinearLayout.HORIZONTAL);
        RelativeLayout.LayoutParams rpInter = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        rpInter.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        dotLayout.setLayoutParams(rpInter);
        dotLayout.setGravity(Gravity.CENTER);

        dotLayout.setPadding(8, 8, 8, 8);

        rl_normal_show = new RelativeLayout(context);
        rl_normal_show.addView(viewPager);
        rl_normal_show.addView(dotLayout);
        rl_normal_show.setVisibility(GONE);
        fl.addView(rl_normal_show);

        rl_loading_show = new LinearLayout(context);
        rl_loading_show.setOrientation(LinearLayout.VERTICAL);
        rl_loading_show.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        rl_loading_show.setGravity(Gravity.CENTER);
        ImageView iv_loading = new ImageView(context);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(100, 100);
        iv_loading.setLayoutParams(layoutParams);
        if (drawable != null)
            iv_loading.setImageDrawable(drawable);

        tv = new TextView(context);
        tv.setText("loading...");
        RelativeLayout.LayoutParams rl_tv = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        tv.setLayoutParams(rl_tv);
        rl_loading_show.addView(iv_loading);
        rl_loading_show.addView(tv);
        fl.addView(rl_loading_show);

        addView(fl);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus && !isStart) {
            startPlay();
        } else if (!hasWindowFocus && isStart) {
            stopPlay();
        }
    }

    /**
     * 开始轮播图切换
     */
    public void startPlay(int perTime) {
       TIME_INTERVAL=perTime;
        startPlay();
    }
    public void startPlay() {
        if (imageViewsList == null || imageViewsList.size() == 0)
            return;
        lastTime = System.currentTimeMillis();
        handler.postDelayed(new SlideShowTask(), TIME_INTERVAL);
        isStart = true;
    }

    public void init() {
        initData();
    }

    public void setTimeInterval(int perTime){
        TIME_INTERVAL=perTime;
    }
    public int getTimeInterval(){
        return TIME_INTERVAL;
    }


    /**
     * 停止轮播图切换
     */
    public void stopPlay() {
        isStart = false;
    }

    /**
     * 初始化相关Data
     */
    private void initData() {
        imageViewsList = new ArrayList<ImageView>();
        dotViewsList = new ArrayList<View>();

        // 一步任务获取图片
        new GetListTask().start();
    }

    /**
     * 初始化Views等UI
     */
    private void initLoadingUI(Context context) {

        if (imageUrls == null || imageUrls.length == 0){
            tv.setText("拉取服务端数据失败...");
            return;
        }

        //隐藏loading页面显示图片
        rl_normal_show.setVisibility(VISIBLE);
        rl_loading_show.setVisibility(GONE);

        /// 热点个数与图片数相等
        for (int i = 0; i < imageUrls.length; i++) {
            ImageView view = new ImageView(context);
            view.setTag(imageUrls[i]);
            view.setScaleType(ImageView.ScaleType.FIT_XY);
            imageViewsList.add(view);
            imageLoader.displayImage(imageUrls[i], view, options);
            ImageView dotView = new ImageView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.leftMargin = 8;
            params.rightMargin = 8;
            dotLayout.addView(dotView, params);
            dotViewsList.add(dotView);
            if (i == 0) {
                dotView.setBackgroundResource(R.drawable.dot_focus);
            } else {
                dotView.setBackgroundResource(R.drawable.dot_blur);
            }
            final int finalI = i;
            dotView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread() {
                        @Override
                        public void run() {
                            synchronized (viewPager) {
                                currentItem = (finalI) % imageViewsList.size();
                                Message msg = handler.obtainMessage();
                                msg.what = 1;
                                msg.sendToTarget();
                            }
                        }
                    }.start();
                }
            });
        }
        //viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setFocusable(true);
        viewPager.setAdapter(new MyPagerAdapter());
        viewPager.setOnPageChangeListener(new MyPageChangeListener());
        startPlay();
    }


    /**
     * 填充ViewPager的页面适配器
     */
    private class MyPagerAdapter extends PagerAdapter {

        @Override
        public void destroyItem(View container, int position, Object object) {
            // TODO Auto-generated method stub
            //((ViewPag.er)container).removeView((View)object);
            ((ViewPager) container).removeView(imageViewsList.get(position));
        }

        @Override
        public Object instantiateItem(View container, int position) {
            // ImageView imageView = imageViewsList.get(position);
            // //更新
            // imageLoader.displayImage(imageView.getTag() + "", imageView, options);
            ((ViewPager) container).addView(imageViewsList.get(position));
            return imageViewsList.get(position);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return imageViewsList.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            // TODO Auto-generated method stub
            return arg0 == arg1;
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public Parcelable saveState() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void startUpdate(View arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void finishUpdate(View arg0) {
            // TODO Auto-generated method stub

        }

    }

    /**
     * ViewPager的监听器
     * 当ViewPager中页面的状态发生改变时调用
     */
    private class MyPageChangeListener implements OnPageChangeListener {

        boolean isAutoPlay = false;

        @Override
        public void onPageScrollStateChanged(int arg0) {
            // TODO Auto-generated method stub
            switch (arg0) {
                case 1:// 手势滑动，空闲中
                    isAutoPlay = false;
                    break;
                case 2:// 界面切换中
                    isAutoPlay = true;
                    break;
                case 0:// 滑动结束，即切换完毕或者加载完毕
                    // 当前为最后一张，此时从右向左滑，则切换到第一张
                    if (viewPager.getCurrentItem() == viewPager.getAdapter().getCount() - 1 && !isAutoPlay) {
                        viewPager.setCurrentItem(0);
                    }
                    // 当前为第一张，此时从左向右滑，则切换到最后一张
                    else if (viewPager.getCurrentItem() == 0 && !isAutoPlay) {
                        viewPager.setCurrentItem(viewPager.getAdapter().getCount() - 1);
                    }
                    break;
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPageSelected(int pos) {
            // TODO Auto-generated method stub

            lastTime = System.currentTimeMillis();
            currentItem = pos;
            for (int i = 0; i < dotViewsList.size(); i++) {
                if (i == pos) {
                    ((View) dotViewsList.get(pos)).setBackgroundResource(R.drawable.dot_focus);
                } else {
                    ((View) dotViewsList.get(i)).setBackgroundResource(R.drawable.dot_blur);
                }
            }
        }

    }

    /**
     * 执行轮播图切换任务
     */
    private class SlideShowTask implements Runnable {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            long time = TIME_INTERVAL - (System.currentTimeMillis() - lastTime);
            if (time <= 0) {
                synchronized (viewPager) {
                    currentItem = (currentItem + 1) % imageViewsList.size();
                    Message msg = handler.obtainMessage();
                    msg.what = 1;
                    msg.sendToTarget();
                }
            } else {
                handler.postDelayed(new SlideShowTask(), time);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destoryBitmaps();
        handler.removeCallbacks(null);
    }

    /**
     * 销毁ImageView资源，回收内存
     */
    private void destoryBitmaps() {
        if (imageViewsList == null)
            return;
        for (int i = 0; i < imageViewsList.size(); i++) {
            ImageView imageView = imageViewsList.get(i);
            Drawable drawable = imageView.getDrawable();
            releaseDrawable(drawable);
        }
    }

    private void releaseDrawable(Drawable drawable) {
        if (drawable != null && drawable instanceof BitmapDrawable) {
            //解除drawable对view的引用
            drawable.setCallback(null);
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();

            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    /**
     * 异步任务,获取数据
     */

    class GetListTask extends Thread {
        @Override
        public void run() {
            super.run();
            if (urlLoading != null) {
                imageUrls = urlLoading.loading();
            }
            Message msg = handler.obtainMessage();
            msg.what = 2;
            msg.sendToTarget();
        }
    }

    public interface UrlLoading {
        String[] loading();
    }

    public void setUrlLoading(UrlLoading urlLoading) {
        this.urlLoading = urlLoading;
    }

    /**
     * ImageLoader 图片组件初始化
     *
     * @param context
     */
    public void initImageLoader(Context context) {

        File cacheDir = StorageUtils.getOwnCacheDirectory(context, "imageloader/Cache");
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCache(new UsingFreqLimitedMemoryCache(2 * 1024 * 1024))
                .memoryCacheSize(2 * 1024 * 1024)
                .discCacheSize(50 * 1024 * 1024)
                .discCacheFileNameGenerator(new Md5FileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .discCacheFileCount(100)
                .discCache(new UnlimitedDiskCache(cacheDir))
                .build();
        // Initialize ImageLoader with configuration.
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(config);
        //显示图片的配置
        options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }


}