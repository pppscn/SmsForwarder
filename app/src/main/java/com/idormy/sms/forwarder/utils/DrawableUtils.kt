package com.idormy.sms.forwarder.utils

import android.graphics.drawable.GradientDrawable

/**
 * @author xuexiang
 * @since 2019/4/7 下午12:57
 */
@Suppress("unused")
class DrawableUtils private constructor() {
    companion object {
        /**
         * 矩形
         */
        fun createRectangleDrawable(color: Int, cornerRadius: Float): GradientDrawable {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.RECTANGLE
            gradientDrawable.cornerRadius = cornerRadius
            gradientDrawable.setColor(color)
            return gradientDrawable
        }

        /**
         * 矩形
         */
        fun createRectangleDrawable(colors: IntArray?, cornerRadius: Float): GradientDrawable {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.RECTANGLE
            gradientDrawable.cornerRadius = cornerRadius
            gradientDrawable.colors = colors
            return gradientDrawable
        }

        /**
         * 圆形
         */
        fun createOvalDrawable(color: Int): GradientDrawable {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.OVAL
            gradientDrawable.setColor(color)
            return gradientDrawable
        }

        /**
         * 圆形
         */
        fun createOvalDrawable(colors: IntArray?): GradientDrawable {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.OVAL
            gradientDrawable.colors = colors
            return gradientDrawable
        }
    }

    init {
        throw UnsupportedOperationException("Can not be instantiated.")
    }

}