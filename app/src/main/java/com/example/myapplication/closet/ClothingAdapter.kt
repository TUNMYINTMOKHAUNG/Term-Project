package com.example.myapplication.closet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.model.ClothingItem

class ClothingAdapter(
    private var items: List<ClothingItem>
) : RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder>() {

    inner class ClothingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.clothingImage)
        val type: TextView = view.findViewById(R.id.clothingType)
        val color: TextView = view.findViewById(R.id.clothingColor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clothing_card, parent, false)
        return ClothingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClothingViewHolder, position: Int) {
        val item = items[position]
        holder.type.text = item.type
        holder.color.text = item.color
        Glide.with(holder.image.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.ic_placeholder)
            .into(holder.image)
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<ClothingItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}