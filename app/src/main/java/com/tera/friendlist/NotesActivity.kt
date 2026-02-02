package com.tera.friendlist

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.widget.doOnTextChanged
import com.tera.friendlist.databinding.ActivityNotesBinding
import com.tera.friendlist.utils.MyConst

class NotesActivity : AppCompatActivity() {

    lateinit var binding: ActivityNotesBinding
    private var keySave = false
    private var pos = 0
    private var keyChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = intent.getStringExtra(MyConst.NAME_NOTE) ?: ""
        val note = intent.getStringExtra(MyConst.NOTE) ?: ""
        pos = intent.getIntExtra(MyConst.POS_NOTE, 0)

        binding.apply {
            tvTittle.text = name
            edNote.setText(note)

            // Назад
            imHome.setOnClickListener {
                goHome()
            }

            // Сохранить
            imSave.setOnClickListener {
                imSave.setImageResource(R.drawable.ic_save_block)
                keySave = true
                hideKeyboard()      // Скрыть клавиатуру
                edNote.clearFocus() // Снять фокус
                keyChange = false
            }

            // Сделаны изменения
            edNote.doOnTextChanged { _, _, _, _ ->
                imSave.setImageResource(R.drawable.ic_save_write)
                keyChange = true
            }
        }

        // Добавить слушателя обратного вызова onBackPressed.
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }

    //-----------

    // Диалог
    private fun dialogSave() {
        val bnYas = getString(R.string.yes)
        val bnNo = getString(R.string.no)
        val builder = AlertDialog.Builder(this)
        builder
            .setTitle(R.string.changes_made)
            .setMessage(R.string.save_changes)
            .setIcon(R.drawable.ic_warning_delta)
            .setPositiveButton(bnYas) { _, _ ->
                keySave = true
                sentNote()
            }
            .setNegativeButton(bnNo) { _, _ ->
                keySave = false
                sentNote()
            }
        val dialog = builder.create()
        dialog.show()
    }

    private fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun goHome(){
        if (keyChange)
            dialogSave()
        else
            sentNote()
    }

    // Отправить в MainActivity
    private fun sentNote() {
        val note = binding.edNote.text.toString()
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MyConst.NOTE, note)
        intent.putExtra(MyConst.POS_NOTE, pos)
        intent.putExtra(MyConst.KEY_SAVE, keySave)

        val enterAnim = R.anim.left_in
        val exitAnim = R.anim.right_out
        val options = ActivityOptionsCompat.makeCustomAnimation(this, enterAnim, exitAnim)
        startActivity(intent, options.toBundle())
    }

    // Кнопка Back
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goHome()
        }
    }

}