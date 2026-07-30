package cn.ppps.forwarder.receiver

import androidx.work.OutOfQuotaPolicy
import cn.ppps.forwarder.utils.Worker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsReceiverTest {

    @Suppress("RestrictedApi")
    @Test
    fun buildSmsSendWorkRequestCreatesExpeditedWork() {
        val msgInfoJson = "{\"type\":\"sms\"}"
        val enqueuedAt = 1234L

        val request = buildSmsSendWorkRequest(msgInfoJson, enqueuedAt)

        assertTrue(request.workSpec.expedited)
        assertEquals(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST, request.workSpec.outOfQuotaPolicy)
        assertEquals(msgInfoJson, request.workSpec.input.getString(Worker.SEND_MSG_INFO))
        assertEquals(enqueuedAt, request.workSpec.input.getLong(Worker.SEND_ENQUEUED_AT, 0L))
    }
}
