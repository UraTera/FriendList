package com.tera.friendlist

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import com.tera.friendlist.databinding.DialogAddBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tera.friendlist.utils.AgeCalculator
import com.tera.friendlist.utils.DateControl
import com.tera.friendlist.utils.ModelPerson
import com.tera.friendlist.utils.MyConst
import com.tera.friendlist.utils.TextWatcherDate

class DialogAdd() : DialogFragment() {

    interface ListenerDialog {
        fun addPerson(listAdd: ArrayList<ModelPerson>, keyEditD: Boolean)
    }

    private lateinit var binding: DialogAddBinding
    private var uriAdd: Uri = "".toUri()
    private lateinit var control: DateControl
    private lateinit var calculator: AgeCalculator

    private var nameAdd = ""
    private var note: String? = null
//    private var dateAdd = "" // Вычисление индекс
//    private var dateDeathAdd = ""

    private var keyEdit = false
    private var keyHide = false
    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAddBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ширина диалога
        setFullScreen()
        val context = requireContext()
        control = DateControl(context)
        calculator = AgeCalculator(context)
        // Скрыть курсор
//        binding.edDate.isCursorVisible = false

        var listOld = ArrayList<ModelPerson>()

        if (arguments != null) {
            keyEdit = arguments?.getBoolean(MyConst.KEY_EDIT, false) == true
            if (keyEdit) {
                var listStr = arguments?.getString(MyConst.LIST_PERSON, "")
                listStr = "[$listStr]"
                val type = object : TypeToken<ArrayList<ModelPerson>>() {}.type
                listOld = gson.fromJson(listStr, type)
            }

            keyHide = arguments?.getBoolean(MyConst.KEY_HIDE, false) == true
        }

        binding.apply {
            if (keyEdit) {
                tvTittle.text = context.getString(R.string.edit)
                val uriStr = listOld[0].photo
                val uri = uriStr.toUri()
                val name = listOld[0].name
                var date = listOld[0].date
                var dateDeath = listOld[0].died
                note = listOld[0].note
                date = calculator.showZero(date)
                dateDeath = calculator.showZero(dateDeath)

                imPhoto.setImageURI(uri)
                edName.setText(name)
                edDate.setText(date)
                edDied.setText(dateDeath)
                uriAdd = uri

            } else {
                tvTittle.text = context.getString(R.string.add)
            }

            val getContent =
                registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    imPhoto.setImageURI(uri)    // Загрузить картинку
                    if (uri != null) {
                        imPhoto.setImageURI(uri)
                        uriAdd = uri
                    }
                }

            imPhoto.setOnClickListener {
                getContent.launch("image/*")
            }

            // Кнопка ОК
            tvOk.setOnClickListener {
                val name = edName.text.toString()

                if (name.isEmpty()) {
                    emName.error = context.getString(R.string.errorName)
                } else {
                    nameAdd = name
                    // Проверить дату рождения
                    val date = edDate.text.toString()
                    val dateDeath = edDied.text.toString()
                    val errorDay: String = if (dateDeath.isEmpty())
                        control.checkDate(date)
                    else control.checkDate(date)
                    if (errorDay != "") {
                        emDate.error = errorDay
                    }
                    // Проверить дату смерти
                    val errorDateDeath = control.checkDate(dateDeath)
                    val errorYear = control.checkYearDeath(date, dateDeath)
                    if (errorDay == "" && errorDateDeath == "" && errorYear == "") {
                        setList()
                        dismiss()
                    } else {
                        if (errorYear.isEmpty())
                            emDied.error = errorDateDeath
                        else
                            emDied.error = errorYear
                    }
                }
            }


            tvCancel.setOnClickListener {
                dismiss()
            }

            var keyHideEd = false
            tvBnDied.setOnClickListener {
                keyHideEd = !keyHideEd
                if (keyHideEd) {
                    tvDied.visibility = View.VISIBLE
                    emDied.visibility = View.VISIBLE
                } else {
                    tvDied.visibility = View.GONE
                    emDied.visibility = View.GONE
                }
            }

            // Проверка заполнения поля имени
            edName.doOnTextChanged { text, _, _, _ ->
                val str = text.toString()
                if (str.isNotEmpty()) {
                    emName.isErrorEnabled = false
                }
            }

            // Ввод даты рождения по шаблону
            edDate.addTextChangedListener(TextWatcherDate(requireContext()))

            // Проверка ввода даты
            edDate.doOnTextChanged { text, _, _, _ ->
                val date = text.toString()
                emDate.isErrorEnabled = false
                val error = control.inputDate(date)
                emDate.error = error
            }

            // Ввод даты смерти по шаблону
            edDied.addTextChangedListener(TextWatcherDate(requireContext()))

            // Проверка ввода даты
            edDied.doOnTextChanged { text, _, _, _ ->
                val date = text.toString()
                emDied.isErrorEnabled = false
                val error = control.inputDate(date)
                emDied.error = error
            }


        }
    }

    // Загрузить список
    private fun setList() {

        val listNew = ArrayList<ModelPerson>()
        val pathName = uriAdd.toString()
        val name = binding.edName.text.toString()
        var date = binding.edDate.text.toString()
        var dateDeath = binding.edDied.text.toString()
        val age = calculator.getAge(date, dateDeath)
        val monLeft = calculator.dayLeft(date)
        val id = setIndex(date) // Получить индекс

        if (keyHide) {
            date = calculator.hideZero(date)
            dateDeath = calculator.hideZero(dateDeath)
        }

        if (note == null)
            note = ""

        val item = ModelPerson(
            id, pathName, name, date, dateDeath, age, monLeft, note!!, false
        )
        listNew.add(item)
        sendData(listNew)
    }

    // Получить индекс
    private fun setIndex(date: String): Int {
        if (date.isEmpty()) return 0
        val day = date.take(2)
        val mon = date.substring(3, 5)
        return "$mon$day".toInt()
    }

    // Передать данные
    private fun sendData(list: ArrayList<ModelPerson>) {
        val listener = activity as ListenerDialog
        listener.addPerson(list, keyEdit)
    }

    private fun setFullScreen() {
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

}