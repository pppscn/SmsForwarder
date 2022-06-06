/*
 * Copyright (C) 2022 xuexiangjys(xuexiangjys@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.idormy.sms.forwarder

import com.idormy.sms.forwarder.core.http.entity.TipInfo
import com.xuexiang.xhttp2.model.ApiResult
import com.xuexiang.xutil.net.JsonUtil
import org.junit.Assert
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        Assert.assertEquals(4, (2 + 2).toLong())
        val info = TipInfo()
        info.title = "微信公众号"
        info.content = "获取更多资讯，欢迎关注我的微信公众号：【我的Android开源之旅】"
        val list: MutableList<TipInfo> = ArrayList()
        for (i in 0..4) {
            list.add(info)
        }
        val result = ApiResult<List<TipInfo>>()
        result.data = list
        println(JsonUtil.toJson(result))
    }
}