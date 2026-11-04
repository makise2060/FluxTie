package com.huanchengfly.tieba.post

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object DataStoreConst {
    const val DATA_STORE_NAME = "app_preferences"
}

private val dataStoreInstance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    preferencesDataStore(
        name = DataStoreConst.DATA_STORE_NAME,
        produceMigrations = { context ->
            listOf(
                SharedPreferencesMigration(context, "settings"),
                object : DataMigration<Preferences> {
                    override suspend fun cleanUp() {}

                    override suspend fun migrate(currentData: Preferences): Preferences {
                        return currentData.toMutablePreferences().apply {
                            set(stringPreferencesKey("dark_theme"), "grey_dark")
                        }.toPreferences()
                    }

                    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
                        return currentData[stringPreferencesKey("dark_theme")] == "dark"
                    }
                }
            )
        }
    )
}

val Context.dataStore: DataStore<Preferences> by dataStoreInstance

@Composable
fun <T> rememberPreferenceAsMutableState(
    key: Preferences.Key<T>,
    defaultValue: T
): MutableState<T> {
    val dataStore = LocalContext.current.dataStore
    val state = remember { mutableStateOf(defaultValue) }
    val loaded = remember { mutableStateOf(false) }

    // 一次性读取初值，读盘完成后才放行写回，避免把默认值抢先落盘
    LaunchedEffect(Unit) {
        state.value = dataStore.data.first()[key] ?: defaultValue
        loaded.value = true
    }

    // 变更即写盘；不持续收集 data 回灌——快速连续写入时收集器会把中间值灌回 state，
    // 造成开关状态回跳/颜色残留（issue #20）
    LaunchedEffect(state.value) {
        if (loaded.value) {
            dataStore.edit { it[key] = state.value }
        }
    }

    return state
}

@Composable
fun <T> rememberPreferenceAsState(
    key: Preferences.Key<T>,
    defaultValue: T
): State<T> {
    val dataStore = LocalContext.current.dataStore
    val state = remember { mutableStateOf(defaultValue) }

    // 纯读取：一次性读盘，不写回也不持续收集
    LaunchedEffect(Unit) {
        state.value = dataStore.data.first()[key] ?: defaultValue
    }

    return state
}

@SuppressLint("FlowOperatorInvokedInComposition")
@Composable
fun <T> DataStore<Preferences>.collectPreferenceAsState(
    key: Preferences.Key<T>,
    defaultValue: T
): State<T> {
    return data.map { it[key] ?: defaultValue }.collectAsState(initial = defaultValue)
}

fun DataStore<Preferences>.putString(key: String, value: String? = null) {
    MainScope().launch(Dispatchers.IO) {
        edit {
            if (value == null) {
                it.remove(stringPreferencesKey(key))
            } else {
                it[stringPreferencesKey(key)] = value
            }
        }
    }
}

fun DataStore<Preferences>.putBoolean(key: String, value: Boolean) {
    MainScope().launch(Dispatchers.IO) {
        edit {
            it[booleanPreferencesKey(key)] = value
        }
    }
}

fun DataStore<Preferences>.putInt(key: String, value: Int) {
    MainScope().launch(Dispatchers.IO) {
        edit {
            it[intPreferencesKey(key)] = value
        }
    }
}

fun DataStore<Preferences>.getInt(key: String, defaultValue: Int): Int {
    var resultValue = defaultValue

    runBlocking {
        data.first {
            // 历史 key 类型被改写时（如 string 写入后改读 int）按默认值兜底，避免启动即崩
            resultValue = runCatching { it[intPreferencesKey(key)] ?: resultValue }.getOrDefault(resultValue)
            true
        }
    }

    return resultValue
}

fun DataStore<Preferences>.getString(key: String): String? {
    var resultValue: String? = null

    runBlocking {
        data.first {
            resultValue = it[stringPreferencesKey(key)]
            true
        }
    }

    return resultValue
}

fun DataStore<Preferences>.getString(key: String, defaultValue: String): String {
    var resultValue = defaultValue

    runBlocking {
        data.first {
            resultValue = it[stringPreferencesKey(key)] ?: resultValue
            true
        }
    }

    return resultValue
}

fun DataStore<Preferences>.getStringSet(
    key: String,
    defaultValues: MutableSet<String>? = null
): MutableSet<String>? {
    var resultValue = defaultValues

    runBlocking {
        data.first {
            resultValue = it[stringSetPreferencesKey(key)]?.toMutableSet() ?: resultValue
            true
        }
    }

    return resultValue
}

fun DataStore<Preferences>.getBoolean(key: String, defaultValue: Boolean): Boolean {
    var resultValue = defaultValue

    runBlocking {
        data.first {
            resultValue = runCatching { it[booleanPreferencesKey(key)] ?: resultValue }.getOrDefault(resultValue)
            true
        }
    }

    return resultValue
}

fun DataStore<Preferences>.getFloat(key: String, defaultValue: Float): Float {
    var resultValue = defaultValue

    runBlocking {
        data.first {
            resultValue = it[floatPreferencesKey(key)] ?: resultValue
            true
        }
    }

    return resultValue
}

fun DataStore<Preferences>.getLong(key: String, defaultValue: Long): Long {
    var resultValue = defaultValue

    runBlocking {
        data.first {
            resultValue = runCatching { it[longPreferencesKey(key)] ?: resultValue }.getOrDefault(resultValue)
            true
        }
    }

    return resultValue
}

class DataStorePreference : PreferenceDataStore() {
    override fun putString(key: String, value: String?) {
        MainScope().launch(Dispatchers.IO) {
            App.INSTANCE.dataStore.edit {
                if (value == null) {
                    it.remove(stringPreferencesKey(key))
                } else {
                    it[stringPreferencesKey(key)] = value
                }
            }
        }
    }

    override fun putStringSet(key: String, values: MutableSet<String>?) {
        MainScope().launch(Dispatchers.IO) {
            App.INSTANCE.dataStore.edit {
                if (values == null) {
                    it.remove(stringSetPreferencesKey(key))
                } else {
                    it[stringSetPreferencesKey(key)] = values
                }
            }
        }
    }

    override fun putInt(key: String, value: Int) {
        MainScope().launch(Dispatchers.IO) {
            App.INSTANCE.dataStore.edit {
                it[intPreferencesKey(key)] = value
            }
        }
    }

    override fun putLong(key: String, value: Long) {
        MainScope().launch(Dispatchers.IO) {
            App.INSTANCE.dataStore.edit {
                it[longPreferencesKey(key)] = value
            }
        }
    }

    override fun putFloat(key: String, value: Float) {
        MainScope().launch(Dispatchers.IO) {
            App.INSTANCE.dataStore.edit {
                it[floatPreferencesKey(key)] = value
            }
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        MainScope().launch(Dispatchers.IO) {
            App.INSTANCE.dataStore.edit {
                it[booleanPreferencesKey(key)] = value
            }
        }
    }

    override fun getString(key: String, defValue: String?): String? {
        return App.INSTANCE.dataStore.getString(key) ?: defValue
    }

    override fun getStringSet(
        key: String,
        defValues: MutableSet<String>?
    ): MutableSet<String>? {
        return App.INSTANCE.dataStore.getStringSet(key, defValues)
    }

    override fun getInt(key: String, defValue: Int): Int {
        return App.INSTANCE.dataStore.getInt(key, defValue)
    }

    override fun getLong(key: String, defValue: Long): Long {
        return App.INSTANCE.dataStore.getLong(key, defValue)
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return App.INSTANCE.dataStore.getFloat(key, defValue)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return App.INSTANCE.dataStore.getBoolean(key, defValue)
    }
}