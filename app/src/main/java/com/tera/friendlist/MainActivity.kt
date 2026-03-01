package com.tera.friendlist

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tera.friendlist.databinding.ActivityMainBinding
import com.tera.friendlist.utils.AgeCalculator
import com.tera.friendlist.utils.ModelPerson
import com.tera.friendlist.utils.MyConst
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class MainActivity : AppCompatActivity(),
    DialogAdd.ListenerDialog,
    AdapterClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var calculator: AgeCalculator

    private var listPerson = ArrayList<ModelPerson>()
    private var listDel = ArrayList<Int>()
    private lateinit var adapter: AdapterPerson
    private lateinit var sp: SharedPreferences
    private val gson = Gson()
    private var path = ""

    private var editVisibility = false // Видимость значка редактирование
    private var noteVisibility = false // Видимость значка заметок
    private var checkVisibility = false // Видимость CheckBox
    private var keyDelAll = false // Удалить все
    private var posEdit = 0
    private var keyEdit = false
    private var keyHide = false
    private var keyNote = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Цвет панели навигации
        val color = ContextCompat.getColor(this, R.color.bar)
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.light(color, color))
        // Цвет кнопок
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightNavigationBars = false

        // Запрос разрешения
        checkPermissions()

        sp = getSharedPreferences("settings", MODE_PRIVATE)
        keyHide = sp.getBoolean(MyConst.KEY_HIDE, false)
        binding.ckHide.isChecked = keyHide

        // Папка сохранения списка
        path = this.externalMediaDirs.first().toString()

        // Загрузить список
        openList()

        val keySave = intent.getBooleanExtra(MyConst.KEY_SAVE, false)
        if (keySave) {
            val note = intent.getStringExtra(MyConst.NOTE) ?: ""
            val pos = intent.getIntExtra(MyConst.POS_NOTE, 0)
            listPerson[pos].note = note
            saveList()
        }

        calculator = AgeCalculator(this)
        // Проверит разделитель
        listPerson = calculator.checkSeparator(listPerson)

        // Проверить возраст
        for (i in listPerson.indices) {
            val birthDate = listPerson[i].date
            val deathDate = listPerson[i].died
            val age = calculator.getAge(birthDate, deathDate)
            val dayLeft = calculator.dayLeft(birthDate)
            listPerson[i].age = age
            listPerson[i].dayLeft = dayLeft
        }

        binding.rcPerson.layoutManager = LinearLayoutManager(this)
        initAdapter()
        initClick()

        // Добавить слушателя обратного вызова onBackPressed.
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }

    //---------------------

    // Проверка и запрос разрешения
    fun checkPermissions() {
        val write = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (checkSelfPermission(write) != PackageManager.PERMISSION_GRANTED
        ) { // Запрос
            ActivityCompat.requestPermissions(this, arrayOf(write), 100)
        }
    }

    // Сохранить файл списка
    private fun saveList() {
        val fileName = MyConst.fileName
        val listStr = gson.toJson(listPerson).toString()
        try {
            File("$path/$fileName").writeText(listStr)
        } catch (e: IOException) {
            Log.d("mylogs", "Save file, Error: $e")
            toast(getString(R.string.error_save))
        }
    }

    // Открыть файл списка
    private fun openList() {
        val fileName = MyConst.fileName
        val file = File(path, fileName)
        var listStr: String
        try {
            listStr = FileInputStream(file).bufferedReader().use { it.readText() }
            val type = object : TypeToken<ArrayList<ModelPerson>>() {}.type
            listPerson = gson.fromJson(listStr, type)
        } catch (e: IOException) {
            Log.d("mylogs", "Open file, Error: $e")
            toast(getString(R.string.error_open))
            return
        }
    }

    // Адаптер
    private fun initAdapter() {
        adapter = AdapterPerson(
            listPerson,
            checkVisibility,
            editVisibility,
            noteVisibility,
            this
        )
        binding.rcPerson.adapter = adapter
    }

    // Кнопки
    private fun initClick() = with(binding) {
        imAdd.setOnClickListener {
            editVisibility = false
            checkVisibility = false
            noteVisibility = false
            keyEdit = false
            listDel.clear()
            setVisible()
            openDialog(false, 0)
            initAdapter()
        }

        imMinus.setOnClickListener {
            checkVisibility = !checkVisibility
            imAdd.isVisible = !checkVisibility
            chSelectAll.isVisible = checkVisibility
            editVisibility = false
            noteVisibility = false
            listDel.clear()
            for (i in listPerson.indices) {
                listPerson[i].keyDel = false
            }
            initAdapter()
        }

        // Выбрать все
        chSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                keyDelAll = true
                for (i in listPerson.indices) {
                    listPerson[i].keyDel = true
                    listDel.add(i)
                }
            } else {
                keyDelAll = false
                for (i in listPerson.indices) {
                    listPerson[i].keyDel = false
                    listDel.clear()
                }
            }
            setVisible()
            initAdapter()
        }

        // Кнопка Удалить
        imDelete.setOnClickListener {
            imMinus.isVisible = true
            imDelete.isVisible = false
            imAdd.isVisible = true
            chSelectAll.isVisible = false
            checkVisibility = false

            val countItem = listDel.size

            if (listDel.isNotEmpty()) {
                val name = listPerson[listDel[0]].name
                dialogDelete(countItem, name)
            }
            initAdapter()
        }

        // Изменить
        imEdit.setOnClickListener {
            editVisibility = !editVisibility
            checkVisibility = false
            noteVisibility = false
            keyEdit = true
            imAdd.isVisible = true
            imMinus.isVisible = true
            imDelete.isVisible = false
            chSelectAll.isVisible = false
            chSelectAll.isChecked = false
            // Диалог открывается в onCreate
            initAdapter()
        }

        // Создать заметку
        imNote.setOnClickListener {
            noteVisibility = !noteVisibility
            editVisibility = false
            checkVisibility = false
            keyNote = true
            keyEdit = false
            imAdd.isVisible = true
            imMinus.isVisible = true
            imDelete.isVisible = false
            chSelectAll.isVisible = false
            chSelectAll.isChecked = false
            initAdapter()
        }

        imMenu.setOnClickListener {
            main.openDrawer(GravityCompat.START, true)
        }
        ckHide.setOnClickListener {
            keyHide = ckHide.isChecked
            if (listPerson.isNotEmpty())
                setZeros()
            main.closeDrawer(GravityCompat.START, true)
        }
    }

    // Видимость кнопок
    private fun setVisible() = with(binding) {
        if (listDel.isNotEmpty()) {
            imMinus.isVisible = false
            imDelete.isVisible = true
        } else {
            imMinus.isVisible = true
            imDelete.isVisible = false
        }
    }

    // Открыть диалог Добавить / Изменить
    private fun openDialog(keyEdit: Boolean, pos: Int) {
        val dialogAdd = DialogAdd()
        val manager = supportFragmentManager
        dialogAdd.isCancelable = false // Модальный
        val bundle = Bundle()
        dialogAdd.arguments = bundle
        if (keyEdit) {
            val listStr = gson.toJson(listPerson[pos])
            bundle.putBoolean(MyConst.KEY_EDIT, true)
            bundle.putString(MyConst.LIST_PERSON, listStr)
        }
        bundle.putBoolean(MyConst.KEY_HIDE, keyHide)
        dialogAdd.show(manager, "dialogAdd")
    }

    // Диалог подтверждения удаления
    private fun dialogDelete(count: Int, name: String) {
        val bnCansel = getString(R.string.cancel)
        val mess: String = if (count == 1) {
            getString(R.string.mess1) + " '$name'?"
        } else getString(R.string.mess2) + " '$count'?"

        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle(getString(R.string.delete))
            .setMessage(mess)
            .setIcon(R.drawable.ic_warning_delta)
            // OK
            .setPositiveButton("OK") { _, _ ->
                deleteItems()
            }
            // Отмена
            .setNegativeButton(bnCansel) { _, _ ->
                listDel.clear()
                checkVisibility = false
                for (i in listPerson.indices) {
                    listPerson[i].keyDel = false // Снять выделение
                }
                initAdapter()
            }
        val dialog = builder.create()
        dialog.show()
    }

    // Удалить элементы
    private fun deleteItems() {
        if (listDel.isNotEmpty()) {
            for (item in listDel.asReversed()) {
                listPerson.removeAt(item)
            }
        }
        listDel.clear()
        binding.chSelectAll.isChecked = false
        saveList()
        initAdapter()
    }

    // Установить ведущие нули в дате
    private fun setZeros() {
        if (keyHide)
            for (i in listPerson.indices) {
                val date = listPerson[i].date
                listPerson[i].date = calculator.hideZero(date)
                val died = listPerson[i].died
                listPerson[i].died = calculator.hideZero(died)
            }
        else
            for (i in listPerson.indices) {
                val date = listPerson[i].date
                listPerson[i].date = calculator.showZero(date)
                val died = listPerson[i].died
                listPerson[i].died = calculator.showZero(died)
            }
        initAdapter()
    }

    // Добавить / изменить элемент
    override fun addPerson(listAdd: ArrayList<ModelPerson>, keyEditD: Boolean) {
        var pathName: String
        val id = listAdd[0].id
        val uri = listAdd[0].photo
        val name = listAdd[0].name
        val date = listAdd[0].date
        val dateDeath = listAdd[0].died
        val age = listAdd[0].age
        val note = listAdd[0].note
        val monLeft = listAdd[0].dayLeft

        val uriAdd: Uri = uri.toUri()

        pathName = saveImage(uriAdd)

        val item = ModelPerson(
            id, pathName, name, date, dateDeath, age, monLeft, note, false
        )

        val size = listPerson.size
        if (keyEditD) { // Изменить
            if (size == 1)
                listPerson.clear()
            else
                listPerson.removeAt(posEdit)
            listPerson.add(posEdit, item)
        } else { // добавить
            listPerson.add(item)
        }
        editVisibility = false
        listPerson.sortBy { it.id }
        saveList()
        initAdapter()
    }

    // Позиция для редактирования
    override fun onItemClickEdit(position: Int) {
        posEdit = position
        openDialog(true, posEdit)
    }

    // Список позиций для удаления
    override fun onItemClickDelete(list: ArrayList<Int>) {
        listDel = list
        setVisible()
        if (listDel.isEmpty())
            binding.chSelectAll.isChecked = false
    }

    // Создание заметок
    override fun onItemClickNote(position: Int) {
        val name = listPerson[position].name
        val note = listPerson[position].note

        val intent = Intent(this, NotesActivity::class.java)
        intent.putExtra(MyConst.NAME_NOTE, name)
        intent.putExtra(MyConst.NOTE, note)
        intent.putExtra(MyConst.POS_NOTE, position)

        // Анимация. Справа налево
        val enterAnim = R.anim.right_in
        val exitAnim = R.anim.left_out
        val options = ActivityOptionsCompat.makeCustomAnimation(this, enterAnim, exitAnim)
        startActivity(intent, options.toBundle())
    }

    // Сохранить фойл на диск
    private fun saveImage(uri: Uri): String {

        val str = uri.toString()
        if (str == "") return ""

        val key = str.contains("content")
        if (!key) return uri.toString()

        val allName = uri.path.toString()
        var name = File(allName).name
        name = name.substringBefore('.')
        name = name.substringAfter(':')

        val imageUri: Uri = uri
        val source = imageUri.let { ImageDecoder.createSource(this.contentResolver, it) }
        val bitmap = source.let { ImageDecoder.decodeBitmap(it) }
        val nameNew = convertBitmapToUri(bitmap, name)

        return nameNew.toString()
    }

    // Сохранить изображение из Bitmap
    private fun convertBitmapToUri(bitmap: Bitmap, nameFile: String): Uri {

        // Создайте файл для сохранения изображения
        val file = File(path, "$nameFile.jpg")

        try {
            // Получить поток вывода файла
            val stream: OutputStream = FileOutputStream(file)

            // Сжать растровое изображение
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

            // Очистить выходной поток
            stream.flush()

            // Закрыть выходной поток
            stream.close()
            // toast("Изображение сохранено.")
        } catch (e: IOException) {
            e.printStackTrace()
            toast(getString(R.string.error_save_image))
        }
        // Вернуть сохраненный путь к изображению в uri
        return file.absolutePath.toUri()
    }

    // Сохранить настрйки
    override fun onDestroy() {
        super.onDestroy()
        sp.edit {
            putBoolean(MyConst.KEY_HIDE, keyHide)
        }
        for (i in listPerson.indices)
            listPerson[i].keyDel = false
        saveList()
    }

    // Toast
    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Кнопка Back
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finishAffinity() // Закрыть все
        }
    }

}