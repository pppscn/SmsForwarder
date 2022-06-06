package com.idormy.sms.forwarder.utils

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import com.idormy.sms.forwarder.R
import me.samlss.broccoli.PlaceholderParameter

/**
 * 占位控件
 *
 * @author xuexiang
 * @since 2019/4/7 下午1:02
 */
@Suppress("SameParameterValue", "unused")
class PlaceholderHelper private constructor() {
    companion object {
        fun getParameter(view: View?): PlaceholderParameter? {
            return if (view == null) {
                null
            } else getParameter(view.id, view)
        }

        private fun getParameter(viewId: Int, view: View): PlaceholderParameter? {
            val placeHolderColor = Color.parseColor("#DDDDDD")
            when (viewId) {
                R.id.tv_ver_name, R.id.tv_time -> {
                    val summaryAnimation: Animation = ScaleAnimation(0.4f, 1f, 1f, 1f)
                    summaryAnimation.duration = 600
                    return getAnimationRectanglePlaceholder(view, summaryAnimation, placeHolderColor, 5)
                }
                R.id.tv_app_name, R.id.tv_from, R.id.tv_name -> {
                    val titleAnimation: Animation = ScaleAnimation(0.3f, 1f, 1f, 1f)
                    titleAnimation.duration = 600
                    return getAnimationRectanglePlaceholder(view, titleAnimation, placeHolderColor, 5)
                }
                R.id.tv_pkg_name, R.id.tv_duration, R.id.tv_phone_number, R.id.tv_content -> {
                    val summaryAnimation2: Animation = ScaleAnimation(0.5f, 1f, 1f, 1f)
                    summaryAnimation2.duration = 400
                    return getAnimationRectanglePlaceholder(view, summaryAnimation2, placeHolderColor, 5)
                }
                R.id.iv_app_icon, R.id.iv_image, R.id.iv_sim_image, R.id.sb_letter, R.id.iv_copy, R.id.iv_call, R.id.iv_reply -> {
                    val imageAnimation: Animation = ScaleAnimation(0.5f, 1f, 0.5f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
                    imageAnimation.duration = 800
                    return getAnimationOvalPlaceholder(view, imageAnimation, placeHolderColor)
                }
                else -> {}
            }
            return null
        }

        /**
         * 圆形的动画占位
         */
        private fun getAnimationOvalPlaceholder(view: View, animation: Animation, placeHolderColor: Int): PlaceholderParameter {
            animation.repeatMode = Animation.REVERSE
            animation.repeatCount = Animation.INFINITE
            return PlaceholderParameter.Builder()
                .setView(view)
                .setAnimation(animation)
                .setDrawable(DrawableUtils.createOvalDrawable(placeHolderColor))
                .build()
        }

        /**
         * 矩形的动画占位
         */
        private fun getAnimationRectanglePlaceholder(view: View, animation: Animation, placeHolderColor: Int, cornerRadius: Int): PlaceholderParameter {
            animation.repeatMode = Animation.REVERSE
            animation.repeatCount = Animation.INFINITE
            return PlaceholderParameter.Builder()
                .setView(view)
                .setAnimation(animation)
                .setDrawable(DrawableUtils.createRectangleDrawable(placeHolderColor, cornerRadius.toFloat()))
                .build()
        }

        /**
         * 圆形的占位
         */
        private fun getOvalPlaceholder(view: View, placeHolderColor: Int): PlaceholderParameter {
            return getPlaceholder(view, DrawableUtils.createOvalDrawable(placeHolderColor))
        }

        /**
         * 矩形的占位
         */
        private fun getRectanglePlaceholder(view: View, placeHolderColor: Int, cornerRadius: Int): PlaceholderParameter {
            return getPlaceholder(view, DrawableUtils.createRectangleDrawable(placeHolderColor, cornerRadius.toFloat()))
        }

        private fun getPlaceholder(view: View, ovalDrawable: GradientDrawable): PlaceholderParameter {
            return PlaceholderParameter.Builder()
                .setView(view)
                .setDrawable(ovalDrawable)
                .build()
        }
    }

    init {
        throw UnsupportedOperationException("Can not be instantiated.")
    }
}