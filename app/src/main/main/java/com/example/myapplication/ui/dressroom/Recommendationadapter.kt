package com.example.myapplication.ui.dressroom

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ItemClothingCardBinding
import com.example.myapplication.model.ClothingItem

/**
 * Lightweight adapter used by DressroomActivity to display MatchingEngine suggestions.
 * Re-uses item_clothing_card.xml so no new layout is needed.
 */
class RecommendationAdapter(
    private val items: List<ClothingItem>,
    private val onItemClick: (ClothingItem) -> Unit
) : RecyclerView.Adapter<RecommendationAdapter.VH>() {

    inner class VH(val binding: ItemClothingCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemClothingCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            clothingLabel.text = "${item.type}  ·  ${item.color}"
            Glide.with(root.context)
                .load(item.imageUrl)
                .centerCrop()
                .into(clothingImage)

            // The "+" button adds the item directly to the matching slot
            btnAdd.setOnClickListener { onItemClick(item) }
            root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun getItemCount() = items.size
}