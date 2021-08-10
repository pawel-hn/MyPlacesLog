package pawel.hn.myplaceslog.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import java.util.*


class DatePickerFragmentDialog(private val listener: DatePickerDialog.OnDateSetListener) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val cal = Calendar.getInstance()
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val dateDialog = DatePickerDialog(requireContext(), listener, year, month, dayOfMonth)
        dateDialog.datePicker.maxDate = System.currentTimeMillis()

        return dateDialog
    }
}



