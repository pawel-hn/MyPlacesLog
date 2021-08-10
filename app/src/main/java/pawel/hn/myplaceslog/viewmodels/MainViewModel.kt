package pawel.hn.myplaceslog.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import pawel.hn.myplaceslog.*
import pawel.hn.myplaceslog.model.Place
import pawel.hn.myplaceslog.repository.PlacesRepository
import pawel.hn.myplaceslog.utils.ADD_PLACE_RESULT_OK
import pawel.hn.myplaceslog.utils.DataStoreRepository
import pawel.hn.myplaceslog.utils.EDIT_PLACE_RESULT_OK
import pawel.hn.myplaceslog.utils.SortOrder
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltViewModel
class MainViewModel @Inject constructor(
    private val placesRepository: PlacesRepository,
    private val myPlacesDataStoreRepository: DataStoreRepository
) : ViewModel() {


    private val placesEventChannel = Channel<PlacesEvent>()
    val placesEvent = placesEventChannel.receiveAsFlow()

    private val placeDetailEventChannel = Channel<PlacesEvent>()
    val placeDetailEvent = placeDetailEventChannel.receiveAsFlow()


    val searchQuery = MutableStateFlow("")
    val preferencesFlow = myPlacesDataStoreRepository.preferencesFlow

    val lastViewedPlaceDetailFlow = myPlacesDataStoreRepository.lastViewedFlow

    private val placesFlow =
        combine(searchQuery, preferencesFlow) {searchQuery, preferencesFlow ->
            Pair(searchQuery, preferencesFlow)
    }.flatMapLatest {(searchQuery, preferencesFlow) ->
            placesRepository.placesList(searchQuery, preferencesFlow.sortOrder, preferencesFlow.showFavs )
        }
    val placesObservable = placesFlow.asLiveData()



    fun lastViewedPlaceDetail(position: Int) = viewModelScope.launch {
        myPlacesDataStoreRepository.updateLastViewed(position)
    }


    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        myPlacesDataStoreRepository.updateSortOrder(sortOrder)
    }


    fun onShowFavsChecked(showFavs: Boolean) = viewModelScope.launch {
        myPlacesDataStoreRepository.updateShowFavs(showFavs)
    }

    fun insertPlace(place: Place) = viewModelScope.launch {
        placesRepository.insertPlace(place)
    }

    fun placeFavouriteChecked(place: Place, isFav: Boolean) = viewModelScope.launch{
        placesRepository.updateFavourite(place, isFav)
    }

    fun deletePlace(place: Place) = viewModelScope.launch {
        placesRepository.deletePlace(place)
    }

    fun deleteAll() = viewModelScope.launch {
        placesRepository.deleteAll()
    }

    fun onPlaceClicked(place: Place) = viewModelScope.launch {
        placesEventChannel.send(PlacesEvent.NavigateToDetailScreen(place))
    }

    fun onAddNewPlaceClick() = viewModelScope.launch {
        placesEventChannel.send(PlacesEvent.NavigateToAddScreen)
    }

    fun onPlaceSwipeRightDelete(place: Place) = viewModelScope.launch {
        placesRepository.deletePlace(place)
        placesEventChannel.send(PlacesEvent.ShowUndoDeleteMessage(place))
    }

    fun onPlaceSwipeLeftEdit(place: Place) = viewModelScope.launch {
        placesEventChannel.send(PlacesEvent.NavigateToEditScreen(place))
    }


    fun onAddEditResult(result: Int, context: Context) {
        when(result) {
            ADD_PLACE_RESULT_OK -> showMsgOnPlacesList(context.getString(R.string.place_added))
            EDIT_PLACE_RESULT_OK -> showMsgOnPlacesList(context.getString(R.string.place_edited))
        }
    }

    fun showMsgOnPlacesList(msg: String) = viewModelScope.launch {
        placesEventChannel.send(PlacesEvent.ShowMsg(msg))
    }

    fun showMsgOnDetail(msg: String) = viewModelScope.launch {
        placeDetailEventChannel.send(PlacesEvent.ShowMsg(msg))
    }

    fun onViewOnMapClicked(place: Place) = viewModelScope.launch {
        placeDetailEventChannel.send(PlacesEvent.NavigateToMaps(place))
    }


    sealed class PlacesEvent {
        object NavigateToAddScreen : PlacesEvent()
        data class NavigateToEditScreen (val place: Place) : PlacesEvent()
        data class NavigateToMaps (val place: Place) : PlacesEvent()
        data class NavigateToDetailScreen (val place: Place) : PlacesEvent()
        data class ShowUndoDeleteMessage (val place: Place) : PlacesEvent()
        data class ShowMsg (val msg: String) : PlacesEvent()
    }
}



