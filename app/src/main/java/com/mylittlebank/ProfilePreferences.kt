package com.mylittlebank

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

internal val Context.profileDataStore by preferencesDataStore(name = "profile")

internal object ProfilePreferenceKeys {
    val childName = stringPreferencesKey("child_name")
    val monthlySavingsInterestPercent = intPreferencesKey("monthly_savings_interest_percent")
}
