package com.tera.friendlist

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tera.friendlist.databinding.ItemPersonBinding
import com.tera.friendlist.utils.AgeCalculator
import com.tera.friendlist.utils.ModelPerson
import java.io.IOException

interface AdapterClickListener {
    fun onItemClickEdit(position: Int)
    fun onItemClickDelete(list: ArrayList<Int>)
    fun onItemClickNote(position: Int)
}

class AdapterPerson(
    private var list: ArrayList<ModelPerson>,
    private val keyDeleteVisi: Boolean, // Видимость chDelete
    private val keyEditVisi: Boolean,   // Видимость imEdit
    private val keyNoteVisi: Boolean,   // Видимость imNote
    private val listener: AdapterClickListener
) : RecyclerView.Adapter<AdapterPerson.ViewHolder>() {

    class ViewHolder(
        view: View,
        private val context: Context
    ) : RecyclerView.ViewHolder(view) {
        val binding = ItemPersonBinding.bind(view)

        private val strBirth = context.getString(R.string.date_birth)
        private val strDied = context.getString(R.string.died)
        private val handler = Handler(Looper.getMainLooper())

        fun bind(item: ModelPerson) = with(binding) {

            // Фото
            var uriStr: String
            handler.post {
                val uri = item.photo.toUri()

                uriStr = uri.toString()
                if (uriStr != "")
                    try {
                        imPerson.setImageURI(uri)
                    } catch (e: IOException) {
                        Log.d("mylogs", "Adapter, Error: $e")
                    }
            }

            // Имя
            val name = item.name
            tvName.text = name

            // Дата рождения
            var dateAll = ""
            var dateDeathAll = ""
            val date = item.date
            val dateDeath = item.died

            if (dateDeath.isEmpty()) {
                if (date.isNotEmpty())
                    dateAll = "$strBirth $date"  // Дата рождения
            } else if (date.isNotEmpty()) {
                if (date.length < 6) { // Нет года
                    dateAll = "$strBirth $date"          // Дата рождения
                    dateDeathAll = "$strDied $dateDeath" // Умер
                } else {
                    val dash = "\u2012"
                    dateAll = "$date $dash $dateDeath"
                }
            } else {
                dateAll = "$dateDeath $dateDeath"
            }

            // Дата рождения
            if (dateAll.isEmpty())
                tvDate.isVisible = false
            tvDate.text = dateAll

            // Умер
            if (dateDeathAll.isEmpty())
                tvDied.isVisible = false
            tvDied.text = dateDeathAll

            // Возраст
            val age = item.age
            if (age.isEmpty())
                tvAge.isVisible = false
            else if (dateDeath.isNotEmpty()) {
                val calculator = AgeCalculator(context)
                val keyWake = calculator.getWake(age, dateDeath)
                if (keyWake) {
                    tvAge.typeface = Typeface.DEFAULT_BOLD // Поминки
                }
            }
            tvAge.text = age

            // До дня рождения осталось ? дн.
            val dayLeft = item.dayLeft
            if (dayLeft.isEmpty())
                tvLeft.isVisible = false
            else {
                val sym = dayLeft.last()
                if ('.' == sym)
                    tvLeft.setTextColor(Color.RED)

            }
            tvLeft.text = dayLeft

//            chDelete.isChecked = item.keyDel
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_person, parent, false)
        return ViewHolder(view, parent.context)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private val selectDel = ArrayList<Int>()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
        val context = holder.itemView.context
        val colorItem = ContextCompat.getColor(context, R.color.item)
        val colorSel = ContextCompat.getColor(context, R.color.item_sel)

        with(holder.binding) {

            val item = list[position]
            tvName.text = item.name

            chDelete.isVisible = keyDeleteVisi
            imEdit.isVisible = keyEditVisi
            imNote.isVisible = keyNoteVisi

            imEdit.setOnClickListener {
                listener.onItemClickEdit(position)
            }

            var key = item.keyDel
            chDelete.isChecked = key

            if (key) root.setBackgroundColor(colorSel)
            else root.setBackgroundColor(colorItem)

            // Выбрать элементы для удаления
            chDelete.setOnClickListener {
                key = chDelete.isChecked
                list[position].keyDel = key
                selectDel.clear()
                // Выбранные позиции
                if (list.isNotEmpty()) {
                    for (i in list.indices) {
                        val check = list[i].keyDel

                        if (check) {
                            selectDel.add(i)
                        }
                    }
                }
                if (key) root.setBackgroundColor(colorSel)
                else root.setBackgroundColor(colorItem)

                listener.onItemClickDelete(selectDel)
            }

            imNote.setOnClickListener {
                listener.onItemClickNote(position)
            }

            holder.itemView.setOnClickListener {
                if (!keyDeleteVisi && !keyEditVisi)
                    listener.onItemClickNote(position)
            }
        }
    }

}












