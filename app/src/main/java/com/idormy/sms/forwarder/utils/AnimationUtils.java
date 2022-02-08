package com.idormy.sms.forwarder.utils;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

/**
 * 动画工具
 */
public class AnimationUtils {
    public enum AnimationState {
        STATE_SHOW,
        STATE_HIDDEN
    }

    /**
     * 渐隐渐现动画
     *
     * @param viewGroup 需要实现动画的对象
     * @param state     需要实现的状态
     * @param duration  动画实现的时长（ms）
     */
    public static void showAndHiddenCenterAnimation(final View viewGroup, AnimationState state, long duration) {
        float start = 0f;
        float end = 0f;
        if (state == AnimationState.STATE_SHOW) {
            end = 1.0f;
            viewGroup.setVisibility(View.VISIBLE);
        } else if (state == AnimationState.STATE_HIDDEN) {
            start = 1.0f;
            viewGroup.setVisibility(View.GONE);
        }
        AlphaAnimation animation = new AlphaAnimation(start, end);
        animation.setDuration(duration);
        animation.setFillAfter(true);
        animation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                viewGroup.clearAnimation();
            }
        });
        viewGroup.setAnimation(animation);
        animation.start();
    }

    public static void showAndHiddenCloseVideoAnimation(final View view, AnimationState state, long duration) {
        float start = 0f;
        float end = 0f;
        if (state == AnimationState.STATE_SHOW) {
            end = 1.0f;
            view.setVisibility(View.VISIBLE);
        } else if (state == AnimationState.STATE_HIDDEN) {
            start = 1.0f;
            view.setVisibility(View.GONE);
        }
        AlphaAnimation animation = new AlphaAnimation(start, end);
        animation.setDuration(duration);
        animation.setFillAfter(true);
        animation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.clearAnimation();
            }
        });
        view.setAnimation(animation);
        animation.start();
    }

    /**
     * 移动动画
     *
     * @param viewGroup 需要实现动画的对象
     * @param state     需要实现的状态
     * @param duration  动画实现的时长（ms）
     */
    public static void showAndHiddenTopAnimation(final View viewGroup, AnimationState state, long duration, boolean isTop) {
        float start = 0.0f;
        float end = 0.0f;
        if (state == AnimationState.STATE_SHOW) {
            if (isTop) {
                start = -1.0f;
            } else {
                start = 1.0f;
            }
            viewGroup.setVisibility(View.VISIBLE);
        } else if (state == AnimationState.STATE_HIDDEN) {
            if (isTop) {
                end = -1.0f;
            } else {
                end = 1.0f;
            }
            viewGroup.setVisibility(View.GONE);
        }
        Animation translateAnimation;
        translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                start, Animation.RELATIVE_TO_SELF, end);
        translateAnimation.setDuration(duration);
        translateAnimation.setFillEnabled(true);//使其可以填充效果从而不回到原地
        translateAnimation.setFillAfter(true);//不回到起始位置
        //如果不添加setFillEnabled和setFillAfter则动画执行结束后会自动回到远点
        translateAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                viewGroup.clearAnimation();
            }
        });
        viewGroup.setAnimation(translateAnimation);
        translateAnimation.start();
    }

    public static void liveGiftAnimation(final View viewGroup, AnimationState state, long duration) {
        float start = 0.0f;
        float end = 0.0f;
        if (state == AnimationState.STATE_SHOW) {
            start = -1.0f;
            viewGroup.setVisibility(View.VISIBLE);
        } else if (state == AnimationState.STATE_HIDDEN) {
            end = -1.0f;
            viewGroup.setVisibility(View.GONE);
        }
        Animation translateAnimation;
        translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, start,
                Animation.RELATIVE_TO_SELF, end, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        translateAnimation.setDuration(duration);
        translateAnimation.setFillEnabled(true);//使其可以填充效果从而不回到原地
        translateAnimation.setFillAfter(true);//不回到起始位置
        //如果不添加setFillEnabled和setFillAfter则动画执行结束后会自动回到远点
        translateAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                viewGroup.clearAnimation();
            }
        });
        viewGroup.setAnimation(translateAnimation);
        translateAnimation.start();
    }

    //通知滚动
    public static void liveLotteryAnimation(final TextView viewGroup, AnimationState state, long duration) {

        float start = 0.0f;
        float end = 0.0f;
        if (state == AnimationState.STATE_SHOW) {
            start = 1.0f;
            end = -1.0f;
            viewGroup.setVisibility(View.VISIBLE);
        } else if (state == AnimationState.STATE_HIDDEN) {
            end = -1.0f;
            viewGroup.setVisibility(View.GONE);
        }
        Animation translateAnimation;
        translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, start,
                Animation.RELATIVE_TO_PARENT, end, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        translateAnimation.setDuration(duration);
        translateAnimation.setFillEnabled(true);//使其可以填充效果从而不回到原地
        translateAnimation.setFillAfter(true);//不回到起始位置
        translateAnimation.setInterpolator(new LinearInterpolator());
        if (viewGroup.getAnimation() != null) {
            translateAnimation.setStartOffset(duration);
        }
        //如果不添加setFillEnabled和setFillAfter则动画执行结束后会自动回到远点
        translateAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                viewGroup.clearAnimation();
                viewGroup.setVisibility(View.GONE);
            }
        });
        viewGroup.setAnimation(translateAnimation);
        translateAnimation.start();
    }
}
