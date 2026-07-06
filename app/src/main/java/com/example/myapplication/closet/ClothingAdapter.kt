package com.example.myapplication.closet

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ItemClothingCardBinding
import com.example.myapplication.ui.upload.PatternCircleDrawable
import com.example.myapplication.model.ClothingItem
import com.example.myapplication.R

class ClothingAdapter(
    private var originalList: List<ClothingItem>,
    private val onItemClick: ((ClothingItem) -> Unit)? = null,
    private var isEditMode: Boolean = false,
    private val onDeleteClick: ((ClothingItem) -> Unit)? = null,
    private val isHorizontal: Boolean = false
) : RecyclerView.Adapter<ClothingAdapter.ViewHolder>() {

    private var displayList: List<ClothingItem> = originalList

    inner class ViewHolder(val binding: ItemClothingCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClothingCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        val params = binding.root.layoutParams

        if (isHorizontal) {
            val displayMetrics = parent.context.resources.displayMetrics
            params.width = (displayMetrics.widthPixels * 0.45f).toInt()
        } else {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
        }

        binding.root.layoutParams = params

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = displayList[position]

        Log.d("DEBUG_STAR", "Binding item ${item.id} | Favorite status is: ${item.isFavorite}")

        val context = holder.binding.root.context

        with(holder.binding) {
            Glide.with(context).load(item.imageUrl).centerCrop().into(clothingImage)

            // Favorite Logic (State Reset & Click)
            val btnHeart = btnHeart

            btnHeart.clearColorFilter()
            btnHeart.setImageResource(android.R.drawable.btn_star_big_off)
            if (item.isFavorite) {
                btnHeart.setImageResource(android.R.drawable.btn_star_big_on)
                btnHeart.setColorFilter(Color.parseColor("#FF4B4B"))
            }

            btnHeart.setOnClickListener {
                val newFavoriteState = !item.isFavorite
                item.isFavorite = newFavoriteState

                if (newFavoriteState) {
                    btnHeart.setImageResource(android.R.drawable.btn_star_big_on)
                    btnHeart.setColorFilter(Color.parseColor("#FF4B4B"))
                } else {
                    btnHeart.setImageResource(android.R.drawable.btn_star_big_off)
                    btnHeart.clearColorFilter()
                }

                // Database update
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("clothingItems")
                    .document(item.id)
                    .update("isFavorite", newFavoriteState)
            }

            // Color Circles Loop
            colorCirclesContainer.removeAllViews()
            item.color.forEach { colorString ->
                if (colorString.isNotBlank()) {
                    val circleView = View(context).apply {
                        val density = resources.displayMetrics.density
                        val pixelSize = (32 * density).toInt()
                        layoutParams = LinearLayout.LayoutParams(pixelSize, pixelSize).apply {
                            setMargins(0, 0, (6 * density).toInt(), 0)
                        }
                        val baseDrawable = ContextCompat.getDrawable(context, R.drawable.color_circle)?.mutate() as? GradientDrawable
                        try {
                            val cleanHex = if (colorString.startsWith("#")) colorString else "#$colorString"
                            baseDrawable?.setColor(Color.parseColor(cleanHex))
                        } catch (e: Exception) {
                            baseDrawable?.setColor(Color.LTGRAY)
                        }
                        background = baseDrawable
                    }
                    colorCirclesContainer.addView(circleView)
                }
            }

            //Pattern & UI
            patternIndicator.apply {
                val density = resources.displayMetrics.density
                val pixelSize = (32 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(pixelSize, pixelSize)
                background = PatternCircleDrawable(item.pattern ?: "Other")
            }

            deleteButton.visibility = if (isEditMode) View.VISIBLE else View.GONE
            deleteButton.setOnClickListener { onDeleteClick?.invoke(item) }
            root.setOnClickListener { onItemClick?.invoke(item) }
        }
    }

    override fun getItemCount(): Int = displayList.size

    fun updateList(newList: List<ClothingItem>) {
        this.originalList = newList
        this.displayList = newList
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

    fun setEditMode(editMode: Boolean) {
        isEditMode = editMode
        notifyDataSetChanged()
    }
}