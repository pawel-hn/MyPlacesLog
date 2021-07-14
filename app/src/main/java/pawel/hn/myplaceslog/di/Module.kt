package pawel.hn.myplaceslog.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pawel.hn.myplaceslog.database.PlacesDatabase
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object Module {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PlacesDatabase
    = Room.databaseBuilder(context, PlacesDatabase::class.java, "PlacesDatabase").build()

    @Provides
    @Singleton
    fun providePlacesDao(database: PlacesDatabase) = database.placesDao

}