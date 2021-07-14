package pawel.hn.myplaceslog

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


/**
 *Class to let know that Hilt is responsible for dependency injection.
 */
@HiltAndroidApp
class PlacesApplication : Application() {
}