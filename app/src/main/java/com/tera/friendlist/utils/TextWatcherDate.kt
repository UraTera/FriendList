package com.tera.friendlist.utils

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import com.tera.friendlist.R
import kotlin.text.iterator

class TextWatcherDate(private val context: Context): TextWatcher {

    var sb = StringBuilder()
    var ignore = false
    private val numPlace = 'X'

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    override fun afterTextChanged(p0: Editable?) {
        if (!ignore) {
            removeFormat(p0.toString())
            applyFormat(sb.toString())
            ignore = true
            p0?.replace(0, p0.length, sb.toString())
            ignore = false
        }
    }

    // Удалить лишние символы. Ввод только цифры.
    private fun removeFormat(text: String) {
        sb.setLength(0)
        for (element in text) {
            if (isNumberChar(element)) {
                sb.append(element)
            }
        }
    }

    // Форматирование по шаблону
    private fun applyFormat(text: String) {
        val template: String = getTemplate()
        sb.setLength(0)
        var i = 0
        var textIndex = 0
        while (i < template.length && textIndex < text.length) {
            if (template[i] == numPlace) {
                sb.append(text[textIndex])
                textIndex++
            } else {
                sb.append(template[i])
            }
            i++
        }
    }

    private fun isNumberChar(c: Char): Boolean {
        return c in '0'..'9'
    }

    // Шаблоны
    private fun getTemplate(): String {
//            return "XX.XX.XXXX"
            return context.getString(R.string.template)
    }

}