package com.example.myapplication.closet

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ItemClothingCardBinding
import com.example.myapplication.model.ClothingItem

class ClothingAdapter(
    private var originalList: List<ClothingItem>,
    private val onItemClick: ((ClothingItem) -> Unit)? = null
) : RecyclerView.Adapter<ClothingAdapter.ViewHolder>() {

    private var displayList: List<ClothingItem> = originalList

    inner class ViewHolder(val binding: ItemClothingCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClothingCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = displayList[position]
        val context = holder.binding.root.context

        with(holder.binding) {
            // 1. Text Update: Show category title ONLY (Color text removed)
            //clothingLabel.text = item.type

            // 2. Load Item Image
            Glide.with(context)
                .load(item.imageUrl)
                .centerCrop()
                .into(clothingImage)

            // 3. Clear any recycled container leftover circle views
            colorCirclesContainer.removeAllViews()

            // 4. Dynamically generate circle color tokens
            item.color.forEach { colorString ->
                if (colorString.isNotBlank()) {
                    // Instantiating a generic framework view element
                    val circleView = View(context).apply {
                        // Matching your exact 32dp layout design configuration specifications
                        val density = resources.displayMetrics.density
                        val pixelSize = (32 * density).toInt()

                        layoutParams = LinearLayout.LayoutParams(pixelSize, pixelSize).apply {
                            // Add a subtle 6dp spacing margin right after each circle token item element
                            setMargins(0, 0, (6 * density).toInt(), 0)
                        }

                        // Inflate your circle drawable layout asset frame properties safely
                        val baseDrawable = ContextCompat.getDrawable(
                            context,
                            com.example.myapplication.R.drawable.color_circle
                        )?.mutate() as? GradientDrawable

                        // Handle and color-parse Hex codes safely (#FFFFFF vs FFFFFF)
                        try {
                            val cleanHex = if (colorString.startsWith("#")) colorString else "#$colorString"
                            baseDrawable?.setColor(Color.parseColor(cleanHex))
                        } catch (e: Exception) {
                            // Fallback default safe state representation tokens if parsing fails
                            when (colorString.lowercase()) {
                                "black" -> baseDrawable?.setColor(Color.BLACK)
                                "white" -> baseDrawable?.setColor(Color.WHITE)
                                "gray" -> baseDrawable?.setColor(Color.GRAY)
                                else -> baseDrawable?.setColor(Color.LTGRAY)
                            }
                        }

                        background = baseDrawable
                    }

                    // Inject the view straight into the horizontal row arrangement layout
                    colorCirclesContainer.addView(circleView)
                }
            }

            // Click interaction assignment mappings
            root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    override fun getItemCount(): Int = displayList.size

    fun updateList(newList: List<ClothingItem>) {
        originalList = newList
        displayList = newList
        notifyDataSetChanged()
    }

    fun filterByType(type: String) {
        displayList = if (type.equals("All", ignoreCase = true)) {
            originalList
        } else {
            originalList.filter { it.type.equals(type, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }
}