package com.idormy.sms.forwarder.utils

import android.graphics.Color
import android.text.TextUtils
import java.util.*

/**
 * <pre>
 * desc   : Random Utils
 * author : xuexiang
 * time   : 2018/4/28 上午12:41
</pre> *
 *
 * Shuffling algorithm
 *  * [.shuffle] Shuffling algorithm, Randomly permutes the specified array using a default source of
 * randomness
 *  * [.shuffle] Shuffling algorithm, Randomly permutes the specified array
 *  * [.shuffle] Shuffling algorithm, Randomly permutes the specified int array using a default source of
 * randomness
 *  * [.shuffle] Shuffling algorithm, Randomly permutes the specified int array
 *
 *
 * get random int
 *  * [.getRandom] get random int between 0 and max
 *  * [.getRandom] get random int between min and max
 *
 *
 * get random numbers or letters
 *  * [.getRandomCapitalLetters] get a fixed-length random string, its a mixture of uppercase letters
 *  * [.getRandomLetters] get a fixed-length random string, its a mixture of uppercase and lowercase letters
 *
 *  * [.getRandomLowerCaseLetters] get a fixed-length random string, its a mixture of lowercase letters
 *  * [.getRandomNumbers] get a fixed-length random string, its a mixture of numbers
 *  * [.getRandomNumbersAndLetters] get a fixed-length random string, its a mixture of uppercase, lowercase
 * letters and numbers
 *  * [.getRandom] get a fixed-length random string, its a mixture of chars in source
 *  * [.getRandom] get a fixed-length random string, its a mixture of chars in sourceChar
 *
 *
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class RandomUtils private constructor() {
    companion object {
        private const val NUMBERS_AND_LETTERS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val NUMBERS = "0123456789"
        private const val LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val CAPITAL_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val LOWER_CASE_LETTERS = "abcdefghijklmnopqrstuvwxyz"

        /**
         * 在数字和英文字母中获取一个定长的随机字符串
         *
         * @param length 长度
         * @return 随机字符串
         * @see RandomUtils.getRandom
         */
        @JvmStatic
        fun getRandomNumbersAndLetters(length: Int): String? {
            return getRandom(NUMBERS_AND_LETTERS, length)
        }

        /**
         * 在数字中获取一个定长的随机字符串
         *
         * @param length 长度
         * @return 随机数字符串
         * @see RandomUtils.getRandom
         */
        fun getRandomNumbers(length: Int): String? {
            return getRandom(NUMBERS, length)
        }

        /**
         * 在英文字母中获取一个定长的随机字符串
         *
         * @param length 长度
         * @return 随机字母字符串
         * @see RandomUtils.getRandom
         */
        fun getRandomLetters(length: Int): String? {
            return getRandom(LETTERS, length)
        }

        /**
         * 在大写英文字母中获取一个定长的随机字符串
         *
         * @param length 长度
         * @return 随机字符串 只包含大写字母
         * @see RandomUtils.getRandom
         */
        fun getRandomCapitalLetters(length: Int): String? {
            return getRandom(CAPITAL_LETTERS, length)
        }

        /**
         * 在小写英文字母中获取一个定长的随机字符串
         *
         * @param length 长度
         * @return 随机字符串 只包含小写字母
         * @see RandomUtils.getRandom
         */
        fun getRandomLowerCaseLetters(length: Int): String? {
            return getRandom(LOWER_CASE_LETTERS, length)
        }

        /**
         * 在一个字符数组源中获取一个定长的随机字符串
         *
         * @param source 源字符串
         * @param length 长度
         * @return
         *  * if source is null or empty, return null
         *  * else see [RandomUtils.getRandom]
         *
         */
        fun getRandom(source: String, length: Int): String? {
            return if (TextUtils.isEmpty(source)) null else getRandom(source.toCharArray(), length)
        }

        /**
         * 在一个字符数组源中获取一个定长的随机字符串
         *
         * @param sourceChar 字符数组源
         * @param length     长度
         * @return
         *  * if sourceChar is null or empty, return null
         *  * if length less than 0, return null
         *
         */
        fun getRandom(sourceChar: CharArray?, length: Int): String? {
            if (sourceChar == null || sourceChar.isEmpty() || length < 0) {
                return null
            }
            val str = StringBuilder(length)
            val random = Random()
            for (i in 0 until length) {
                str.append(sourceChar[random.nextInt(sourceChar.size)])
            }
            return str.toString()
        }

        /**
         * get random int between 0 and max
         *
         * @param max 最大随机数
         * @return
         *  * if max <= 0, return 0
         *  * else return random int between 0 and max
         *
         */
        fun getRandom(max: Int): Int {
            return getRandom(0, max)
        }

        /**
         * get random int between min and max
         *
         * @param min 最小随机数
         * @param max 最大随机数
         * @return
         *  * if min > max, return 0
         *  * if min == max, return min
         *  * else return random int between min and max
         *
         */
        fun getRandom(min: Int, max: Int): Int {
            if (min > max) {
                return 0
            }
            return if (min == max) {
                min
            } else min + Random().nextInt(max - min)
        }

        /**
         * 获取随机颜色
         *
         * @return
         */
        val randomColor: Int
            get() {
                val random = Random()
                val r = random.nextInt(256)
                val g = random.nextInt(256)
                val b = random.nextInt(256)
                return Color.rgb(r, g, b)
            }

        /**
         * 随机打乱数组中的内容
         *
         * @param objArray
         * @return
         */
        fun shuffle(objArray: Array<Any?>?): Boolean {
            return if (objArray == null) {
                false
            } else shuffle(
                objArray,
                getRandom(objArray.size)
            )
        }

        /**
         * 随机打乱数组中的内容
         *
         * @param objArray
         * @param shuffleCount
         * @return
         */
        private fun shuffle(objArray: Array<Any?>?, shuffleCount: Int): Boolean {
            var length = 0
            if (objArray == null || shuffleCount < 0 || objArray.size.also {
                    length = it
                } < shuffleCount) {
                return false
            }
            for (i in 1..shuffleCount) {
                val random = getRandom(length - i)
                val temp = objArray[length - i]
                objArray[length - i] = objArray[random]
                objArray[random] = temp
            }
            return true
        }

        /**
         * 随机打乱数组中的内容
         *
         * @param intArray
         * @return
         */
        fun shuffle(intArray: IntArray?): IntArray? {
            return if (intArray == null) {
                null
            } else shuffle(
                intArray,
                getRandom(intArray.size)
            )
        }

        /**
         * 随机打乱数组中的内容
         *
         * @param intArray
         * @param shuffleCount
         * @return
         */
        fun shuffle(intArray: IntArray?, shuffleCount: Int): IntArray? {
            var length = 0
            if (intArray == null || shuffleCount < 0 || intArray.size.also {
                    length = it
                } < shuffleCount) {
                return null
            }
            val out = IntArray(shuffleCount)
            for (i in 1..shuffleCount) {
                val random = getRandom(length - i)
                out[i - 1] = intArray[random]
                val temp = intArray[length - i]
                intArray[length - i] = intArray[random]
                intArray[random] = temp
            }
            return out
        }
    }

    /**
     * Don't let anyone instantiate this class.
     */
    init {
        throw Error("Do not need instantiate!")
    }
}