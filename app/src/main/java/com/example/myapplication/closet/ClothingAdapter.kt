package com.example.myapplication.closet

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.model.ClothingItem
import android.content.Intent
import com.example.myapplication.DressroomActivity

class ClothingAdapter(
    private var items: List<ClothingItem>
) : RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder>() {

    private var fullList: List<ClothingItem> = items

    inner class ClothingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.clothingImage)
        val colorCirclesContainer: LinearLayout = view.findViewById(R.id.colorCirclesContainer)
        //val patternBadge: TextView = view.findViewById(R.id.patternBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clothing_card, parent, false)
        return ClothingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClothingViewHolder, position: Int) {
        val item = items[position]

        // Load image
        Glide.with(holder.image.context)
            .load(item.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.image)

        // Color circles
        holder.colorCirclesContainer.removeAllViews()
        item.color.forEach { colorHex ->
            val circle = View(holder.itemView.context)
            val size = holder.itemView.context.resources
                .getDimensionPixelSize(R.dimen.color_circle_size)

            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = 4
            circle.layoutParams = params

            try {
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(Color.parseColor(colorHex))
                drawable.setStroke(2, Color.parseColor("#DDDDDD"))
                circle.background = drawable
            } catch (e: Exception) {
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(Color.LTGRAY)
                circle.background = drawable
            }

            holder.colorCirclesContainer.addView(circle)
        }

        // Pattern badge — now from item.pattern directly
//        if (item.pattern.isNotEmpty()) {
//            holder.patternBadge.text = item.pattern
//            holder.patternBadge.visibility = View.VISIBLE
//        } else {
//            holder.patternBadge.visibility = View.GONE
//        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, DressroomActivity::class.java)
            intent.putExtra("SELECTED_CLOTHING", item)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<ClothingItem>) {
        fullList = newItems
        items = newItems
        notifyDataSetChanged()
    }

    fun filterByType(type: String) {
        items = if (type == "All") {
            fullList
        } else {
            fullList.filter { it.type.equals(type, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }
}