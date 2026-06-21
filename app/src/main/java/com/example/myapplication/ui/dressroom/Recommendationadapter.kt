package com.example.myapplication.ui.dressroom

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ItemClothingCardBinding
import com.example.myapplication.ui.upload.PatternCircleDrawable
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
            Glide.with(root.context)
                .load(item.imageUrl)
                .centerCrop()
                .into(clothingImage)

            // Ensure the size matches your color circles (48dp)
            // and apply the drawable
            // Inside onBindViewHolder
            // Use the pattern string directly from your database object
            val patternName = item.pattern ?: "Other"

            // This triggers the draw() method in PatternCircleDrawable
            // based on the string value (e.g., "Stripes", "Floral")
            holder.binding.patternIndicator.background = PatternCircleDrawable(patternName)

            root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun getItemCount() = items.size
}