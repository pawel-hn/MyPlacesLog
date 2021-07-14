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
import pawel.hn.myplaceslog.repository.Repository
import pawel.hn.myplaceslog.utils.DataStoreRepository
import pawel.hn.myplaceslog.utils.SortOrder
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository,
    private val myPlacesDataStoreRepository: DataStoreRepository
) : ViewModel() {

    /**
     * Channel used to send instructions from viewModel to fragment, observed from fragment as Flow.
     */
    private val placesEventChannel = Channel<PlacesEvent>()
    val placesEvent = placesEventChannel.receiveAsFlow()

    private val placeDetailEventChannel = Channel<PlacesEvent>()
    val placeDetailEvent = placeDetailEventChannel.receiveAsFlow()

    /**
     * Criteria which decides to show searched items or favourites, or both.
     */
    val searchQuery = MutableStateFlow("")
    val preferencesFlow = myPlacesDataStoreRepository.preferencesFlow

    val lastViewedPlaceDetailFlow = myPlacesDataStoreRepository.lastViewedFlow

    /**
     * Main flow combined of two criteria, and then converted to livedata.
     */
    private val placesFlow =
        combine(searchQuery, preferencesFlow) {searchQuery, preferencesFlow ->
            Pair(searchQuery, preferencesFlow)
    }.flatMapLatest {(searchQuery, preferencesFlow) ->
            repository.placesList(searchQuery, preferencesFlow.sortOrder, preferencesFlow.showFavs )
        }
    val placesObservable = placesFlow.asLiveData()


    /**
     * Called each time when user scrolls horizontally through detail screen, in order to have position
     * when navigating back from edit screen.
     */
    fun lastViewedPlaceDetail(position: Int) = viewModelScope.launch {
        myPlacesDataStoreRepository.updateLastViewed(position)
    }


    /**
     * Called to save via DataStore (new alternative for SharedPreferences, selected sort order
     */
    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        myPlacesDataStoreRepository.updateSortOrder(sortOrder)
    }

    /**
     * Called to save via DataStore (new alternative for SharedPreferences, selected show favs only
     */
    fun onShowFavsChecked(showFavs: Boolean) = viewModelScope.launch {
        myPlacesDataStoreRepository.updateShowFavs(showFavs)
    }

    fun insertPlace(place: Place) = viewModelScope.launch {
        repository.insertPlace(place)
    }

    fun placeFavouriteChecked(place: Place, isFav: Boolean) = viewModelScope.launch{
        repository.updateFavourite(place, isFav)
    }

    fun deletePlace(place: Place) = viewModelScope.launch {
        repository.deletePlace(place)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }

    fun onPlaceClicked(place: Place) = viewModelScope.launch {
        placesEventChannel.send(PlacesEvent.NavigateToDetailScreen(place))
    }

    fun onAddNewPlaceClick() = viewModelScope.launch {
        placesEventChannel.send(PlacesEvent.NavigateToAddScreen)
    }

    fun onPlaceSwipeRightDelete(place: Place) = viewModelScope.launch {
        repository.deletePlace(place)
        placesEventChannel.send(PlacesEvent.ShowUndoDeleteMessage(place))
    }

    fun onPlaceSwipeLeftEdit(place: Place) = viewModelScope.launch {
        placesEventChannel.send(PlacesEvent.NavigateToEditScreen(place))
    }

    /**
     * Whether which result was passed from AddEdit screen proper msg is shown to user.
     */
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

    /**
     * Sealed class with data which is send via channel and recived as flow in fragment. This data
     * tells fragment how to react to particular user interactions.
     */
    sealed class PlacesEvent {
        object NavigateToAddScreen : PlacesEvent()
        data class NavigateToEditScreen (val place: Place) : PlacesEvent()
        data class NavigateToMaps (val place: Place) : PlacesEvent()
        data class NavigateToDetailScreen (val place: Place) : PlacesEvent()
        data class ShowUndoDeleteMessage (val place: Place) : PlacesEvent()
        data class ShowMsg (val msg: String) : PlacesEvent()
    }
}



