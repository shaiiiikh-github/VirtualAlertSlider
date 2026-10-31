package com.virtualalertslider.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryDataStore(initialPreferences: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val mutex = Mutex()
    private val _data = MutableStateFlow(initialPreferences)
    override val data: Flow<Preferences> = _data.asStateFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        return mutex.withLock {
            val updated = transform(_data.value.toPreferences())
            _data.value = updated
            updated
        }
    }
}

