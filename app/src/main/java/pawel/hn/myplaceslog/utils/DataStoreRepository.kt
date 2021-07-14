package pawel.hn.myplaceslog.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import pawel.hn.myplaceslog.DATA_STORE_LAST_VIEWED
import pawel.hn.myplaceslog.DATA_STORE_SHOW_FAVS
import pawel.hn.myplaceslog.DATA_STORE_SORT_ORDER
import pawel.hn.myplaceslog.LAST_VIEWED_STARTING_POSITION
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data store, alternative for SharedPreferences, stores values selected by user, which defines a way how
 * data is displayed to user.
 */
@Singleton
class DataStoreRepository @Inject constructor(@ApplicationContext val context: Context) {

    /**
     * DataStore for sort order and show favourites
     */
    private val Context.myPlacesDataStore: DataStore<Preferences> by preferencesDataStore("user_preferences")

    /**
     * DataStore for position of last viewed item through horizontal recycler view.
     */
    private val Context.myPlacesDataStorePosition: DataStore<Preferences> by preferencesDataStore("last_position")

    val preferencesFlow = context.myPlacesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val sortOrder = SortOrder.valueOf(
                preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.BY_DATE.name
            )
            val showFavs = preferences[PreferencesKeys.SHOW_FAVOURITES] ?: false

            FilterPreferences(
                sortOrder, showFavs
            )
        }

    val lastViewedFlow = context.myPlacesDataStorePosition.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val lastViewed = preferences[PreferencesKeys.LAST_VIEWED] ?: LAST_VIEWED_STARTING_POSITION

            LastViewedPlaceDetail(
                lastViewed
            )
        }

    suspend fun updateSortOrder(sortOrder: SortOrder) =
        context.myPlacesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = sortOrder.name
        }

    suspend fun updateShowFavs(showFavs: Boolean) = context.myPlacesDataStore.edit { preferences ->
        preferences[PreferencesKeys.SHOW_FAVOURITES] = showFavs
    }

    suspend fun updateLastViewed(position: Int) = context.myPlacesDataStorePosition.edit { preferences ->
        preferences[PreferencesKeys.LAST_VIEWED] = position
    }

    private object PreferencesKeys {
        val SORT_ORDER = stringPreferencesKey(DATA_STORE_SORT_ORDER)
        val SHOW_FAVOURITES = booleanPreferencesKey(DATA_STORE_SHOW_FAVS)
        val LAST_VIEWED = intPreferencesKey(DATA_STORE_LAST_VIEWED)
    }
}

data class FilterPreferences(
    val sortOrder: SortOrder,
    val showFavs: Boolean
)

data class LastViewedPlaceDetail(val position: Int)

enum class SortOrder { BY_TITLE, BY_DATE }