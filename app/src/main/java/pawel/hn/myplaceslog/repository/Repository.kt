package pawel.hn.myplaceslog.repository

import pawel.hn.myplaceslog.database.PlacesDao
import pawel.hn.myplaceslog.model.Place
import pawel.hn.myplaceslog.utils.SortOrder
import javax.inject.Inject


/**
 * Repository to mediates between database and viewModels
 */
class Repository @Inject constructor(private val placesDao: PlacesDao) {

    fun placesList(searchQuery: String, sortOrder: SortOrder, showFav: Boolean)
    = placesDao.getPlaces(searchQuery,sortOrder, showFav)

    suspend fun insertPlace(place: Place) = placesDao.insertPlace(place)

    suspend fun updateFavourite(place: Place, isFav: Boolean)
    = placesDao.updatePlace(place.copy(favourite = isFav))

    suspend fun update(place: Place) = placesDao.updatePlace(place.copy())

    suspend fun deletePlace(place: Place) = placesDao.deletePlace(place)

    suspend fun deleteAll() = placesDao.deleteAllPlaces()
}