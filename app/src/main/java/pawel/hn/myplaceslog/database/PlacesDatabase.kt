package pawel.hn.myplaceslog.database

import androidx.room.Database
import androidx.room.RoomDatabase
import pawel.hn.myplaceslog.model.Place

@Database(entities = [Place::class], version = 1, exportSchema = false)
abstract class PlacesDatabase : RoomDatabase() {
    abstract val placesDao: PlacesDao
}