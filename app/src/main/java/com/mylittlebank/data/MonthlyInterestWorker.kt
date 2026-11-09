package com.mylittlebank.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mylittlebank.ProfilePreferenceKeys
import com.mylittlebank.profileDataStore
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.TimeUnit

internal object MonthlyInterestScheduler {
    private const val MONTH_KEY = "interest_period"

    fun scheduleCurrentMonth(context: Context) {
        enqueueMonth(context.applicationContext, YearMonth.now())
    }

    fun scheduleFollowingMonth(context: Context, completedMonth: YearMonth) {
        enqueueMonth(context.applicationContext, completedMonth.plusMonths(1))
    }

    private fun enqueueMonth(context: Context, month: YearMonth) {
        val zone = ZoneId.systemDefault()
        val dueAt = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val delay = (dueAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<MonthlySavingsInterestWorker>()
            .setInputData(workDataOf(MONTH_KEY to month.toString()))
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "monthly-savings-interest-${month}",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun monthFrom(input: Data): YearMonth? = input.getString(MONTH_KEY)?.let {
        runCatching { YearMonth.parse(it) }.getOrNull()
    }
}

internal class MonthlySavingsInterestWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val month = MonthlyInterestScheduler.monthFrom(inputData) ?: return Result.failure()
        return try {
            val rate = applicationContext.profileDataStore.data.first()[ProfilePreferenceKeys.monthlySavingsInterestPercent] ?: 5
            val monthEnd = month.plusMonths(1).atDay(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            BankDatabase.get(applicationContext).bankDao().postMonthlyInterest(month, rate, monthEnd)
            MonthlyInterestScheduler.scheduleFollowingMonth(applicationContext, month)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
