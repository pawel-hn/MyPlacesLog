package pawel.hn.myplaceslog

import android.app.Activity

const val DATE_FORMAT = "dd:MM:yyyy"

//constants to identify what massage on Snackbar should be shown to user after he adds/edits place
const val ADD_PLACE_RESULT_OK = Activity.RESULT_FIRST_USER
const val EDIT_PLACE_RESULT_OK = Activity.RESULT_FIRST_USER + 1

//key for identify result passed between fragments
const val ADD_EDIT_REQUEST_KEY = "addEditRequestKey"

//key for identify result in the bundle passed between fragments
const val ADD_EDIT_RESULT = "addEditResult"

//const used as keys for values store via SavedStateHandle
const val PLACE_NAME = "placeName"
const val PLACE_DESCRIPTION = "placeDescription"
const val PLACE_FAVOURITE = "placeFavourite"

//DataStore names
const val DATA_STORE_SORT_ORDER = "sortOrder"
const val DATA_STORE_SHOW_FAVS = "showFavs"
const val DATA_STORE_LAST_VIEWED = "lastViewed"

//int used o identify that user hasn't scrolled yet between detail items within horizontal recycler view.
const val LAST_VIEWED_STARTING_POSITION = -1