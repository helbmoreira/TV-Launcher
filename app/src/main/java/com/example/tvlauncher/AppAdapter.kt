package com.example.tvlauncher

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class AppEntry(
    val label: String,
    val packageName: String,
    val intent: Intent,
    val banner: Drawable?,
    val icon: Drawable
)

class AppAdapter(
    private val onClick: (AppEntry) -> Unit,
    private val onLongClick: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppAdapter.Holder>() {

    private var items: List<AppEntry> = emptyList()

    fun submit(list: List<AppEntry>) {
        items = list
        notifyDataSetChanged()
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.image)
        val label: TextView = view.findViewById(R.id.label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        val holder = Holder(view)

        // Efeito de foco no estilo tvOS: o card cresce e o nome aparece embaixo.
        view.setOnFocusChangeListener { v, hasFocus ->
            v.animate()
                .scaleX(if (hasFocus) 1.12f else 1f)
                .scaleY(if (hasFocus) 1.12f else 1f)
                .translationZ(if (hasFocus) 24f else 0f)
                .setDuration(160)
                .start()
            holder.label.animate().alpha(if (hasFocus) 1f else 0f).setDuration(160).start()
        }
        view.setOnClickListener { items.getOrNull(holder.adapterPosition)?.let(onClick) }
        view.setOnLongClickListener {
            items.getOrNull(holder.adapterPosition)?.let(onLongClick)
            true
        }
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = items[position]
        holder.label.text = app.label

        val banner = app.banner
        if (banner != null) {
            holder.image.setPadding(0, 0, 0, 0)
            holder.image.scaleType = ImageView.ScaleType.CENTER_CROP
            holder.image.setImageDrawable(banner)
        } else {
            val pad = (22 * holder.image.resources.displayMetrics.density).toInt()
            holder.image.setPadding(pad, pad, pad, pad)
            holder.image.scaleType = ImageView.ScaleType.FIT_CENTER
            holder.image.setImageDrawable(app.icon)
        }
    }

    override fun getItemCount() = items.size
}
