package pawel.hn.myplaceslog.database


import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pawel.hn.myplaceslog.model.Place
import pawel.hn.myplaceslog.utils.SortOrder

@Dao
interface PlacesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: Place)

    fun getPlaces(searchQuery: String, sortOrder: SortOrder, showFav: Boolean): Flow<List<Place>> =
        when (sortOrder) {
            SortOrder.BY_TITLE -> getPlacesOrderByTitle(searchQuery, showFav)
            SortOrder.BY_DATE -> getPlacesOrderByDate(searchQuery, showFav)
        }

    @Query("SELECT * FROM places_table WHERE (favourite = :showFav OR favourite = 1) AND name LIKE '%' || :searchQuery || '%' ORDER BY date DESC")
    fun getPlacesOrderByDate(searchQuery: String, showFav: Boolean): Flow<List<Place>>

    @Query("SELECT * FROM places_table WHERE (favourite = :showFav OR favourite = 1) AND name LIKE '%' || :searchQuery || '%' ORDER BY name")
    fun getPlacesOrderByTitle(searchQuery: String, showFav: Boolean): Flow<List<Place>>

    @Update
    suspend fun updatePlace(place: Place)

    @Delete
    suspend fun deletePlace(place: Place)

    @Query("DELETE FROM places_table")
    suspend fun deleteAllPlaces()


}