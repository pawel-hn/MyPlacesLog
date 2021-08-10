package pawel.hn.myplaceslog.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import pawel.hn.myplaceslog.utils.ADD_EDIT_REQUEST_KEY
import pawel.hn.myplaceslog.utils.ADD_EDIT_RESULT
import pawel.hn.myplaceslog.utils.DATE_FORMAT
import pawel.hn.myplaceslog.R
import pawel.hn.myplaceslog.databinding.FragmentAddEditPlaceBinding
import pawel.hn.myplaceslog.viewmodels.AddEditPlaceViewModel
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class AddEditPlaceFragment : Fragment(R.layout.fragment_add_edit_place),
    DatePickerDialog.OnDateSetListener {

    private lateinit var binding: FragmentAddEditPlaceBinding
    private val addEditViewModel: AddEditPlaceViewModel by viewModels()
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var launchGallery: ActivityResultLauncher<Intent>
    private lateinit var launchCamera: ActivityResultLauncher<Uri>
    private lateinit var launchPlaces: ActivityResultLauncher<Intent>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Places.isInitialized()) {
            Places.initialize(
                requireContext().applicationContext,
                getString(R.string.google_maps_key)
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddEditPlaceBinding.bind(view)

        setActivityForResultListeners()
        setUi(binding)
        setClickListeners(binding)
        subscribeToEvent()
        setHasOptionsMenu(true)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 0
            fastestInterval = 0
            numUpdates = 1
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locCallback,
            Looper.myLooper()!!
        )
    }

    private val locCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val lastLocation = locationResult.lastLocation

            val geocoder = Geocoder(context, Locale.getDefault())
            val addressList = geocoder.getFromLocation(
                lastLocation.latitude, lastLocation.longitude, 1
            )
            if (!addressList.isNullOrEmpty()) {
                val address = addressList[0]
                val sb = StringBuilder()
                for (i in 0..address.maxAddressLineIndex) {
                    sb.append(address.getAddressLine(i)).append(",")
                }
                sb.deleteCharAt(sb.length - 1)
                binding.editTextLocation.setText(sb.toString())
                addEditViewModel.apply {
                    placeLatitude = lastLocation.latitude
                    placeLongitude = lastLocation.longitude
                    placeLocation = sb.toString()
                }
            }
        }
    }

    /**
     *Called to populate UI elements with data.
     */
    private fun setUi(binding: FragmentAddEditPlaceBinding) {
        binding.apply {
            editTextName.setText(addEditViewModel.placeName)
            editTextDescription.setText(addEditViewModel.placeDescription)
            editTextDate.setText(
                SimpleDateFormat(
                    DATE_FORMAT, Locale.getDefault()
                ).format(addEditViewModel.placeDate)
            )
            editTextLocation.setText(addEditViewModel.placeLocation)
            addEditViewModel.placeImage?.let { placeImage ->
                imageViewAddImage.setImageURI(Uri.parse(placeImage))
            }

            editTextName.addTextChangedListener {
                addEditViewModel.placeName = it.toString()
            }
            editTextDescription.addTextChangedListener {
                addEditViewModel.placeDescription = it.toString()
            }
        }
    }

    private fun setClickListeners(binding: FragmentAddEditPlaceBinding) {
        binding.apply {
            buttonSave.setOnClickListener {
                addEditViewModel.onSaveClick()
            }
            editTextDate.setOnClickListener {
                addEditViewModel.onDateClick()
            }
            imageViewAddImage.setOnClickListener {
                val dialogOptions = arrayOf("Choose from gallery", "Capture photo")
                AlertDialog.Builder(requireContext())
                    .setTitle("Choose action:")
                    .setItems(dialogOptions) { _, which ->
                        setDialogOptions(which)
                    }
                    .show()
            }
            editTextLocation.setOnClickListener {
                addEditViewModel.onLocationClick()
            }
            buttonSeeOnMap.setOnClickListener {
                addEditViewModel.onViewOnMapClick(requireContext())
            }
            buttonCurrentLocation.setOnClickListener {
                if (isLocationEnabled()) {
                    addEditViewModel
                        .onSetCurrentLocationClick(requireContext(), this@AddEditPlaceFragment)
                } else {
//                    AlertDialog.Builder(requireContext())
//                        .setTitle(requireContext().getString(R.string.location_enable))
//                        .setMessage(requireContext().getString(R.string.location_enable_msg)
//                        )
//                        .setPositiveButton("Go to settings"){ _, _ ->
//                            this@AddEditPlaceFragment.requireContext().startActivity(
//                                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//                            )
//                        }
//                        .setNegativeButton("Cancel"){ dialog, _ ->
//                            dialog.dismiss()
//                        }
//                        .show()

                    startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                }
            }
        }
    }

    /**
    Check if Location on phone is enabled. If false, user will be directed to settings.
     */
    private fun isLocationEnabled(): Boolean {
        val locManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locManager.isLocationEnabled

    }

    /**
    Dialog options called when user clicks image. Then he can choose between photo from gallery
    or capture one with camera
     */
    private fun setDialogOptions(which: Int) {
        when (which) {
            0 -> {
                addEditViewModel
                    .chooseFromGallerySelected(
                        requireContext(),
                        this@AddEditPlaceFragment
                    )
            }
            1 -> {
                if (requireContext().packageManager
                        .hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                ) {
                    addEditViewModel.capturePhotoSelected(
                        requireContext(),
                        this@AddEditPlaceFragment
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        requireContext().getString(R.string.missing_camera),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
    Subscribes to all events emitted by Flow defined in viewModel, where is decided what and when to show to user,
    or how to handle clicks.
     */
    private fun subscribeToEvent() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            addEditViewModel.addEditPlaceEvent.collect { event ->
                when (event) {
                    is AddEditPlaceViewModel.AddEditPlaceEvent.NavigateBackWithResult -> {
                        binding.root.clearFocus()
                        setFragmentResult(
                            ADD_EDIT_REQUEST_KEY,
                            bundleOf(ADD_EDIT_RESULT to event.result)
                        )
                        findNavController().popBackStack()
                    }
                    is AddEditPlaceViewModel.AddEditPlaceEvent.ShowMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()

                    }
                    is AddEditPlaceViewModel.AddEditPlaceEvent.ShowDataPickerDialog -> {
                        DatePickerFragmentDialog(this@AddEditPlaceFragment)
                            .show(parentFragmentManager, "Date")
                    }

                    is AddEditPlaceViewModel.AddEditPlaceEvent.AccessPhoneGallery -> {
                        val galleryIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            type = ("image/*")
                        }
                        launchGallery.launch(galleryIntent)
                    }
                    is AddEditPlaceViewModel.AddEditPlaceEvent.ShouldShowRequestPermissionRationale -> {

                        AlertDialog.Builder(requireContext())
                            .setTitle(requireContext().getString(R.string.permission_denied))
                            .setMessage("${event.msg} " +
                                    requireContext().getString(R.string.permission_provide)
                            )
                            .setPositiveButton("Go to settings"){ _, _ ->
                                startActivity(
                                    Intent(Settings.ACTION_APPLICATION_SETTINGS)
                                )
                            }
                            .setNegativeButton("Cancel"){ dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                    is AddEditPlaceViewModel.AddEditPlaceEvent.RequestPermission -> {
                        requestPermissionsLauncher.launch(event.permissions)
                    }
                    is AddEditPlaceViewModel.AddEditPlaceEvent.LaunchCamera -> {
                        photoUri = addEditViewModel.createPhotoUri(requireContext())
                        launchCamera.launch(photoUri)
                    }
                    is AddEditPlaceViewModel.AddEditPlaceEvent.LaunchPlaces -> {
                        val fields = listOf(
                            Place.Field.ID, Place.Field.NAME,
                            Place.Field.LAT_LNG, Place.Field.ADDRESS
                        )
                        val intent =
                            Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                                .build(requireContext())
                        launchPlaces.launch(intent)
                    }
                    is AddEditPlaceViewModel.AddEditPlaceEvent.LaunchSeeOnMap -> {
                        val action = AddEditPlaceFragmentDirections
                            .actionAddItemFragmentToMapsFragment(
                                event.latitude.toString(),
                                event.longitude.toString(),
                                event.placeLocation
                            )
                        findNavController().navigate(action)
                    }
                    is AddEditPlaceViewModel.AddEditPlaceEvent.CurrentLocation -> {
                        requestNewLocation()
                    }
                }
            }
        }
    }

    /**
     *Listeners for intents which are expected to return back result.
     */
    private fun setActivityForResultListeners() {
        requestPermissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                Log.d("PHN", "$permissions")
            }

        launchGallery =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                var imageUri: Uri? = null
                if (result.resultCode == Activity.RESULT_OK) {
                    imageUri = result.data?.data
                }
                imageUri?.let {
                    binding.imageViewAddImage.setImageURI(it)
                    addEditViewModel.placeImage = it.toString()
                }
            }

        launchCamera =
            registerForActivityResult(ActivityResultContracts.TakePicture()) {
                if (it) {
                    binding.imageViewAddImage.setImageURI(photoUri)
                    addEditViewModel.placeImage = photoUri.toString()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error while taking a photo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        launchPlaces =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val place: Place = Autocomplete.getPlaceFromIntent(result.data!!)
                    binding.editTextLocation.setText(place.address)
                    addEditViewModel.apply {
                        placeLocation = place.address
                        placeLatitude = place.latLng?.latitude
                        placeLongitude = place.latLng?.longitude
                    }
                }
            }
    }

    /**
    * Set proper format of data chosen by user form DatPicker, function from OnDateSetListener interface
     */
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {

        val newMonth = if (month > 8) month + 1 else "0${month+1}"
        val newDay = if (dayOfMonth > 9) dayOfMonth else "0$dayOfMonth"

        val newDate =  "$newDay:$newMonth:$year"

        binding.editTextDate.setText(newDate)
        addEditViewModel.placeDate =
            SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).parse(newDate)!!.time
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.addedit_menu, menu)
        menu.findItem(R.id.menu_favourite).apply {
            isChecked = addEditViewModel.placeFavourite
            setMenuIconColor(this)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_favourite -> {
                item.isChecked = !item.isChecked
                addEditViewModel.placeFavourite = item.isChecked
                setMenuIconColor(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Change icon color on appBar if item is favourite.
     */
    private fun setMenuIconColor(item: MenuItem) {
        item.icon.setTint(
            if (item.isChecked) {
                ContextCompat.getColor(requireContext(), R.color.red)
            } else {
                ContextCompat.getColor(requireContext(), R.color.white)
            }
        )
    }
}