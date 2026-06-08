package dhp.thl.tpl.unlock.photos

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val PIN_KEY = stringPreferencesKey("pin")
        val IMAGE_URI_KEY = stringPreferencesKey("image_uri")
        val POINTS_KEY = stringPreferencesKey("points") // Stored as "x1,y1;x2,y2"
        val OPEN_IMMEDIATELY_KEY = booleanPreferencesKey("open_immediately")
        val SHOW_ORDER_KEY = booleanPreferencesKey("show_order")
        val TAP_TOLERANCE_KEY = floatPreferencesKey("tap_tolerance")
        val STEALTH_MODE_KEY = booleanPreferencesKey("stealth_mode")
        val SHOW_CLICKED_POINTS_KEY = booleanPreferencesKey("show_clicked_points")
        val POINT_COUNT_KEY = intPreferencesKey("point_count")
    }

    val pinFlow: Flow<String?> = context.dataStore.data.map { it[PIN_KEY] }
    val imageUriFlow: Flow<String?> = context.dataStore.data.map { it[IMAGE_URI_KEY] }
    val pointsFlow: Flow<String?> = context.dataStore.data.map { it[POINTS_KEY] }
    val openImmediatelyFlow: Flow<Boolean> = context.dataStore.data.map { it[OPEN_IMMEDIATELY_KEY] ?: false }
    val showOrderFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_ORDER_KEY] ?: false }
    val tapToleranceFlow: Flow<Float> = context.dataStore.data.map { it[TAP_TOLERANCE_KEY] ?: 0.08f }
    val stealthModeFlow: Flow<Boolean> = context.dataStore.data.map { it[STEALTH_MODE_KEY] ?: false }
    val showClickedPointsFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_CLICKED_POINTS_KEY] ?: true }
    val pointCountFlow: Flow<Int> = context.dataStore.data.map { it[POINT_COUNT_KEY] ?: 3 }

    suspend fun savePin(pin: String) {
        context.dataStore.edit { it[PIN_KEY] = pin }
    }

    suspend fun saveImageUri(uri: String) {
        context.dataStore.edit { it[IMAGE_URI_KEY] = uri }
    }

    suspend fun savePoints(pointsStr: String) {
        context.dataStore.edit { it[POINTS_KEY] = pointsStr }
    }

    suspend fun setOpenImmediately(value: Boolean) {
        context.dataStore.edit { it[OPEN_IMMEDIATELY_KEY] = value }
    }

    suspend fun setShowOrder(value: Boolean) {
        context.dataStore.edit { it[SHOW_ORDER_KEY] = value }
    }

    suspend fun setTapTolerance(value: Float) {
        context.dataStore.edit { it[TAP_TOLERANCE_KEY] = value }
    }

    suspend fun setStealthMode(value: Boolean) {
        context.dataStore.edit { it[STEALTH_MODE_KEY] = value }
    }

    suspend fun setShowClickedPoints(value: Boolean) {
        context.dataStore.edit { it[SHOW_CLICKED_POINTS_KEY] = value }
    }

    suspend fun setPointCount(value: Int) {
        context.dataStore.edit { it[POINT_COUNT_KEY] = value }
    }
}
