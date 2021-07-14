package pawel.hn.myplaceslog.viewmodels

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import pawel.hn.myplaceslog.*
import pawel.hn.myplaceslog.model.Place
import pawel.hn.myplaceslog.repository.Repository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


@HiltViewModel
class AddEditPlaceViewModel @Inject constructor(
    private val repository: Repository,
    private val savedState: SavedStateHandle
) : ViewModel() {


    /**
     * Channel used to send instructions from viewModel to fragment, observed from fragment as Flow.
     */
    private val addEditPlaceEventChannel = Channel<AddEditPlaceEvent>()
    val addEditPlaceEvent = addEditPlaceEventChannel.receiveAsFlow()

    /**
    Place item taken from SavedStateHandle passed via SafeArgs. Key must be the same as argument name in nav graph.
    Can be null, then this viewModel will now its a new place being created.
     */
    val place = savedState.get<Place>("place")


    /**
    Custom setters for below properties are set in order to keep values entered by user if he navigate away from app.
     */
    var placeName = savedState.get<String>(PLACE_NAME) ?: place?.name ?: ""
        set(value) {
            field = value
            savedState.set(PLACE_NAME, value)
        }

    var placeDescription = savedState.get<String>(PLACE_DESCRIPTION) ?: place?.description ?: ""
        set(value) {
            field = value
            savedState.set(PLACE_DESCRIPTION, value)
        }

    var placeFavourite = savedState.get<Boolean>(PLACE_FAVOURITE) ?: place?.favourite ?: false
        set(value) {
            field = value
            savedState.set(PLACE_FAVOURITE, value)
        }

    /**
     * No custom setters for below properties as they can be null or date
     * is set automatically to current day
     */
    var placeImage = place?.image
    var placeDate = place?.date ?: System.currentTimeMillis()
    var placeLocation = place?.location
    var placeLatitude = place?.latitude
    var placeLongitude = place?.longitude

    /**
     * Response to save button clicked. Whether place is null or not, it creates new or updates current.
     */
    fun onSaveClick() {
        if (placeName.isBlank()) {
            showMessage("Provide title at least.?")
            return
        }

        if (place != null) {
            val updatedPlace = place.copy(
                name = placeName,
                description = placeDescription,
                favourite = placeFavourite,
                date = placeDate,
                image = placeImage,
                location = placeLocation,
                latitude = placeLatitude,
                longitude = placeLongitude
            )
            updatePlace(updatedPlace)
        } else {
            val newPlace = Place(
                name = placeName,
                description = placeDescription,
                favourite = placeFavourite,
                date = placeDate,
                image = placeImage,
                location = placeLocation,
                latitude = placeLatitude,
                longitude = placeLongitude
            )
            insertPlace(newPlace)
        }
    }

    private fun showMessage(msg: String) = viewModelScope.launch {
        addEditPlaceEventChannel.send(AddEditPlaceEvent.ShowMessage(msg))
    }

    private fun insertPlace(newPlace: Place) = viewModelScope.launch {
        repository.insertPlace(newPlace)
        addEditPlaceEventChannel.send(AddEditPlaceEvent.NavigateBackWithResult(ADD_PLACE_RESULT_OK))
    }

    private fun updatePlace(updatedPlace: Place) = viewModelScope.launch {
        repository.update(updatedPlace)
        addEditPlaceEventChannel.send(AddEditPlaceEvent.NavigateBackWithResult(EDIT_PLACE_RESULT_OK))
    }

    fun onDateClick() = viewModelScope.launch {
        addEditPlaceEventChannel.send(AddEditPlaceEvent.ShowDataPickerDialog)
    }


    fun chooseFromGallerySelected(context: Context, fragment: Fragment) {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED -> {
                viewModelScope.launch {
                    addEditPlaceEventChannel.send(AddEditPlaceEvent.AccessPhoneGallery)
                }
            }
            fragment.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                viewModelScope.launch {
                    addEditPlaceEventChannel.send(
                        AddEditPlaceEvent
                            .ShouldShowRequestPermissionRationale(
                                context.getString(R.string.permission_gallery)
                            )
                    )
                }
            }
            else -> {
                viewModelScope.launch {
                    addEditPlaceEventChannel.send(
                        AddEditPlaceEvent
                            .RequestPermission(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                    )
                }
            }
        }
    }

    /**
     * Creates Uri MediaStore and content resolver. In this uri photo captured with camera is saved.
     */
    fun createPhotoUri(context: Context): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, placeName)
            put(MediaStore.Images.Media.DISPLAY_NAME, "MyPlace_${timeStamp}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000L)
        }
        return context.contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    }


    fun capturePhotoSelected(context: Context, fragment: Fragment) {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    == PackageManager.PERMISSION_GRANTED -> {
                viewModelScope.launch {
                    addEditPlaceEventChannel.send(AddEditPlaceEvent.LaunchCamera)
                }
            }
            fragment.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                viewModelScope.launch {
                    addEditPlaceEventChannel.send(
                        AddEditPlaceEvent
                            .ShouldShowRequestPermissionRationale(
                                context.getString(R.string.permission_gallery)
                            )
                    )
                }
            }
            else -> {
                viewModelScope.launch {
                    addEditPlaceEventChannel.send(
                        AddEditPlaceEvent
                            .RequestPermission(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                            )
                    )
                }
            }
        }
    }

    fun onLocationClick() = viewModelScope.launch {
        addEditPlaceEventChannel.send(AddEditPlaceEvent.LaunchPlaces)
    }

    fun onViewOnMapClick(context: Context) = viewModelScope.launch {
        if (placeLatitude != null && placeLongitude != null) {
            addEditPlaceEventChannel.send(
                AddEditPlaceEvent
                    .LaunchSeeOnMap(placeLatitude!!, placeLongitude!!, placeLocation!!)
            )
        } else {
            addEditPlaceEventChannel.send(AddEditPlaceEvent.ShowMessage(
                context.getString(R.string.missing_location)
            ))
        }
    }

    fun onSetCurrentLocationClick(context: Context, fragment: Fragment) {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED -> {
                viewModelScope.launch {
                    addEditPlaceEventChannel.send(AddEditPlaceEvent.CurrentLocation)
                }
            }
            fragment.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                viewModelScope.launch {
                    addEditPlaceEventChannel.send(
                        AddEditPlaceEvent
                            .ShouldShowRequestPermissionRationale(
                                context.getString(R.string.permission_location)
                            )
                    )
                }
            }
            else -> {
                viewModelScope.launch {
                    addEditPlaceEventChannel.send(
                        AddEditPlaceEvent
                            .RequestPermission(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                    )
                }
            }
        }
    }


    /**
     * Sealed class with data which is send via channel and received as flow in fragment. This data
     * tells fragment how to react to particular user interactions.
     */
    sealed class AddEditPlaceEvent {
        object ShowDataPickerDialog : AddEditPlaceEvent()
        object AccessPhoneGallery : AddEditPlaceEvent()
        object LaunchCamera : AddEditPlaceEvent()
        object LaunchPlaces : AddEditPlaceEvent()
        object CurrentLocation : AddEditPlaceEvent()
        data class LaunchSeeOnMap(
            val latitude: Double, val longitude: Double, val placeLocation: String
        ) : AddEditPlaceEvent()
        data class ShouldShowRequestPermissionRationale(val msg: String) : AddEditPlaceEvent()
        data class ShowMessage(val msg: String) : AddEditPlaceEvent()
        data class NavigateBackWithResult(val result: Int) : AddEditPlaceEvent()
        data class RequestPermission(val permissions: Array<String>) : AddEditPlaceEvent() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as RequestPermission
                if (!permissions.contentEquals(other.permissions)) return false
                return true
            }

            override fun hashCode(): Int {
                return permissions.contentHashCode()
            }
        }
    }
}