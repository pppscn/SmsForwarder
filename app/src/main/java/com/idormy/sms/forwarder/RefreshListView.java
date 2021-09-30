package com.idormy.sms.forwarder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * 自定义listview
 */
@SuppressWarnings({"CommentedOutCode", "unused"})
public class RefreshListView extends ListView implements AbsListView.OnScrollListener {
    //private static final String TAG = "RefreshListView";
    final int NONE = 0;// 正常状态；
    final int PULL = 1;// 提示下拉状态；
    final int RELEASE = 2;// 提示释放状态；
    final int REFLASHING = 3;// 刷新状态；
    View header;// 顶部布局文件；
    int headerHeight;// 顶部布局文件的高度；
    int firstVisibleItem;// 当前第一个可见的item的位置；
    int scrollState;// listview 当前滚动状态；
    boolean isRemark;// 标记，当前是在listview最顶端摁下的；
    int startY;// 摁下时的Y值；
    int state;// 当前的状态；
    IRefreshListener iRefreshListener;//刷新数据的接口

    public RefreshListView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        initView(context);
    }

    public RefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        initView(context);
    }

    public RefreshListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
        initView(context);
    }

    /**
     * 初始化界面，添加顶部布局文件到 listview
     */
    @SuppressLint("InflateParams")
    private void initView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        header = inflater.inflate(R.layout.header, null);
        measureView(header);
        headerHeight = header.getMeasuredHeight();
        Log.i("tag", "headerHeight = " + headerHeight);
        topPadding(-headerHeight);
        this.addHeaderView(header);
        this.setOnScrollListener(this);
    }

    /**
     * 通知父布局，占用的宽，高；
     */
    private void measureView(View view) {
        ViewGroup.LayoutParams p = view.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int width = ViewGroup.getChildMeasureSpec(0, 0, p.width);
        int height;
        int tempHeight = p.height;
        if (tempHeight > 0) {
            height = MeasureSpec.makeMeasureSpec(tempHeight,
                    MeasureSpec.EXACTLY);
        } else {
            height = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        view.measure(width, height);
    }

    /**
     * 设置header 布局 上边距；
     */
    private void topPadding(int topPadding) {
        header.setPadding(header.getPaddingLeft(), topPadding,
                header.getPaddingRight(), header.getPaddingBottom());
        header.invalidate();
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        // TODO Auto-generated method stub
        this.firstVisibleItem = firstVisibleItem;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // TODO Auto-generated method stub
        this.scrollState = scrollState;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // TODO Auto-generated method stub
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (firstVisibleItem == 0) {
                    isRemark = true;
                    startY = (int) ev.getY();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                onMove(ev);
                break;
            case MotionEvent.ACTION_UP:
                if (state == RELEASE || state == PULL) {
                    state = REFLASHING;
                    // 加载最新数据；
                    refreshViewByState();
                    iRefreshListener.onRefresh();
                }
//                if (state == RELEASE) {
//                    Log.d(TAG, "onTouchEvent: up release");
//                    state = REFLASHING;
//                    // 加载最新数据；
//                    refreshViewByState();
//                    iRefreshListener.onRefresh();
//                } else if (state == PULL) {
//                    Log.d(TAG, "onTouchEvent: up pull");
//                    state = NONE;
//                    isRemark = false;
//                    refreshViewByState();
//                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    /**
     * 判断移动过程操作；
     */
    private void onMove(MotionEvent ev) {
        if (!isRemark) {
            return;
        }
        int tempY = (int) ev.getY();
        int space = tempY - startY;
        int topPadding = space - headerHeight;
        switch (state) {
            case NONE:
                if (space > 0) {
                    state = PULL;
                    refreshViewByState();
                }
                break;
            case PULL:
                topPadding(topPadding);
                if (space > headerHeight + 30
                        && scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    state = RELEASE;
                    refreshViewByState();
                }
                break;
            case RELEASE:
                topPadding(topPadding);
                if (space < headerHeight + 30) {
                    state = PULL;
                    refreshViewByState();
                } else if (space <= 0) {
                    state = NONE;
                    isRemark = false;
                    refreshViewByState();
                }
                break;
        }
    }

    /**
     * 根据当前状态，改变界面显示；
     */
    private void refreshViewByState() {
        TextView tip = header.findViewById(R.id.tip);
        ImageView arrow = header.findViewById(R.id.arrow);
        ProgressBar progress = header.findViewById(R.id.progress);
        RotateAnimation anim = new RotateAnimation(0, 180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(500);
        anim.setFillAfter(true);
        RotateAnimation anim1 = new RotateAnimation(180, 0,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        anim1.setDuration(500);
        anim1.setFillAfter(true);
        switch (state) {
            case NONE:
                arrow.clearAnimation();
                topPadding(-headerHeight);
                break;

            case PULL:
                arrow.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
                tip.setText(R.string.pull_tips);
                arrow.clearAnimation();
                arrow.setAnimation(anim1);
                break;
            case RELEASE:
                arrow.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
                tip.setText(R.string.release_tips);
                arrow.clearAnimation();
                arrow.setAnimation(anim);
                break;
            case REFLASHING:
                topPadding(50);
                arrow.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
                tip.setText(R.string.reflashing_tips);
                arrow.clearAnimation();
                break;
        }
    }

    /**
     * 获取完数据；
     */
    public void refreshComplete() {
        state = NONE;
        isRemark = false;
        refreshViewByState();
        TextView lastUpdateTime = header
                .findViewById(R.id.lastUpdateTime);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String time = sdf.format(new java.util.Date());
        lastUpdateTime.setText(time);
    }

    public void setInterface(IRefreshListener iRefreshListener) {
        this.iRefreshListener = iRefreshListener;
    }

    /**
     * 刷新数据接口
     *
     * @author Administrator
     */
    public interface IRefreshListener {
        void onRefresh();
    }
}