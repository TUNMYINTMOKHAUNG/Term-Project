package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.data.FirebaseManager
import com.example.myapplication.model.ClothingItem
import com.example.myapplication.networkapi.RetrofitInstance
import com.example.myapplication.recommendation.ColorMatchingManager
import com.example.myapplication.dressroom.DressroomMatchAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Intent
import android.widget.Toast
import android.widget.FrameLayout
import com.example.myapplication.FeedbackActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView

class DressroomActivity : AppCompatActivity() {

    private var currentTop: ClothingItem? = null
    private var currentBottom: ClothingItem? = null
    private var currentOuterwear: ClothingItem? = null
    private var currentOnePiece: ClothingItem? = null

    private var todaysPick: ClothingItem? = null
    private var allDatabaseClothes: List<ClothingItem> = emptyList()
    private var weatherFilteredClothes: List<ClothingItem> = emptyList()

    private var activePickerCategory: String = "None"
    private var matchingColors: List<String> = emptyList()
    private var isWeatherFilterOn: Boolean = true
    private var currentOutfitMode: String = "Separates"

    private lateinit var unifiedMatchesRecycler: RecyclerView
    private lateinit var topImage: ImageView
    private lateinit var bottomImage: ImageView
    private lateinit var outerwearImage: ImageView
    private lateinit var weatherToggleBtn: Button
    private lateinit var outfitTypeToggleGroup: MaterialButtonToggleGroup

    private lateinit var cardBottomSlot: MaterialCardView
    private lateinit var tvPlusBottomLabel: TextView
    private lateinit var tvPlusTopLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dressroom)

        setupViews()

        todaysPick = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("SELECTED_CLOTHING", ClothingItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("SELECTED_CLOTHING") as? ClothingItem
        }

        isWeatherFilterOn = (todaysPick != null)
        updateWeatherButtonUI()

        if (todaysPick != null) {
            if (todaysPick!!.type.equals("One-piece", ignoreCase = true) || todaysPick!!.type.equals("Dress", ignoreCase = true)) {
                currentOutfitMode = "OnePiece"
                outfitTypeToggleGroup.check(R.id.btnModeOnePiece)
                setSlotItem("One-piece", todaysPick)
                updateUiForCurrentMode()
            } else {
                currentOutfitMode = "Separates"
                outfitTypeToggleGroup.check(R.id.btnModeSeparates)
                setSlotItem(todaysPick!!.type, todaysPick)
                updateUiForCurrentMode()
            }
            loadColorMatches(todaysPick!!)
        } else {
            downloadClothesOnce()
        }
    }

    private fun setupViews() {
        unifiedMatchesRecycler = findViewById(R.id.unifiedMatchesRecycler)
        unifiedMatchesRecycler.layoutManager = LinearLayoutManager(this)

        topImage = findViewById(R.id.topImage)
        bottomImage = findViewById(R.id.bottomImage)
        outerwearImage = findViewById(R.id.outerwearImage)
        weatherToggleBtn = findViewById(R.id.weatherToggleBtn)
        outfitTypeToggleGroup = findViewById(R.id.outfitTypeToggleGroup)

        cardBottomSlot = findViewById(R.id.cardBottom)
        tvPlusBottomLabel = findViewById(R.id.plusBottom)
        tvPlusTopLabel = findViewById(R.id.plusTop)

        outfitTypeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btnModeSeparates) {
                    currentOutfitMode = "Separates"
                    updateUiForCurrentMode()
                    updatePickerState("Top")
                } else {
                    currentOutfitMode = "OnePiece"
                    updateUiForCurrentMode()
                    updatePickerState("One-piece")
                }
            }
        }

        weatherToggleBtn.setOnClickListener {
            isWeatherFilterOn = !isWeatherFilterOn
            updateWeatherButtonUI()
            filterAndDisplayMatches()
            unifiedMatchesRecycler.scrollToPosition(0)
        }

        findViewById<MaterialCardView>(R.id.cardTop).setOnClickListener {
            if (currentOutfitMode == "Separates") {
                if (currentTop == null) updatePickerState("Top")
            } else {
                if (currentOnePiece == null) updatePickerState("One-piece")
            }
        }

        // FIXED: Bottom remains fully clickable and interactive across both modes
        cardBottomSlot.setOnClickListener {
            if (currentBottom == null) {
                updatePickerState("Bottom")
            }
        }

        findViewById<MaterialCardView>(R.id.cardOuterwear).setOnClickListener {
            if (currentOuterwear == null) updatePickerState("Outerwear")
        }

        findViewById<ImageView>(R.id.removeTop).setOnClickListener {
            if (currentOutfitMode == "Separates") setSlotItem("Top", null) else setSlotItem("One-piece", null)
        }
        findViewById<ImageView>(R.id.removeBottom).setOnClickListener { setSlotItem("Bottom", null) }
        findViewById<ImageView>(R.id.removeOuterwear).setOnClickListener { setSlotItem("Outerwear", null) }

        findViewById<Button>(R.id.getFeedbackButton).setOnClickListener {
            val intent = Intent(this, FeedbackActivity::class.java)

            if (currentOutfitMode == "Separates") {
                // Mode 1: 2-Piece require BOTH elements
                if (currentTop == null || currentBottom == null) {
                    Toast.makeText(this, "Please select both Top and Bottom components", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                intent.putExtra("TOP_TYPE", currentTop?.type ?: "Top")
                intent.putExtra("TOP_COLOR", currentTop?.color?.firstOrNull() ?: "#FFFFFF")
                intent.putExtra("TOP_THICKNESS", currentTop?.thickness ?: "Medium")

                intent.putExtra("BOTTOM_TYPE", currentBottom?.type ?: "Bottom")
                intent.putExtra("BOTTOM_COLOR", currentBottom?.color?.firstOrNull() ?: "#FFFFFF")
                intent.putExtra("BOTTOM_THICKNESS", currentBottom?.thickness ?: "Medium")
            } else {
                // Mode 2: One-piece mode ONLY requires the dress to move forward
                if (currentOnePiece == null) {
                    Toast.makeText(this, "Please select a One-piece garment", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                intent.putExtra("TOP_TYPE", "One-piece")
                intent.putExtra("TOP_COLOR", currentOnePiece?.color?.firstOrNull() ?: "#FFFFFF")
                intent.putExtra("TOP_THICKNESS", currentOnePiece?.thickness ?: "Medium")

                // If they picked an optional bottom with the dress, send it to the AI! Otherwise pass "None"
                if (currentBottom != null) {
                    intent.putExtra("BOTTOM_TYPE", currentBottom?.type ?: "Bottom")
                    intent.putExtra("BOTTOM_COLOR", currentBottom?.color?.firstOrNull() ?: "#FFFFFF")
                    intent.putExtra("BOTTOM_THICKNESS", currentBottom?.thickness ?: "Medium")
                } else {
                    intent.putExtra("BOTTOM_TYPE", "None")
                    intent.putExtra("BOTTOM_COLOR", "None")
                    intent.putExtra("BOTTOM_THICKNESS", "None")
                }
            }

            currentOuterwear?.let {
                intent.putExtra("OUTER_TYPE", it.type)
                intent.putExtra("OUTER_COLOR", it.color.firstOrNull() ?: "#FFFFFF")
                intent.putExtra("OUTER_THICKNESS", it.thickness)
            }
            startActivity(intent)
        }
    }

    private fun updateUiForCurrentMode() {
        if (currentOutfitMode == "Separates") {
            tvPlusBottomLabel.text = "+ Add Bottom"

            if (currentTop != null) {
                Glide.with(this).load(currentTop!!.imageUrl).into(topImage)
                findViewById<ImageView>(R.id.removeTop).visibility = View.VISIBLE
                tvPlusTopLabel.visibility = View.GONE
            } else {
                topImage.setImageDrawable(null)
                findViewById<ImageView>(R.id.removeTop).visibility = View.GONE
                tvPlusTopLabel.visibility = View.VISIBLE
                tvPlusTopLabel.text = "+ Add Top"
            }
        } else {
            // In One-piece mode, change label text to signify it's optional, but leave it fully functional
            if (currentBottom == null) {
                tvPlusBottomLabel.text = "+ Add Bottom (Optional)"
            }

            if (currentOnePiece != null) {
                Glide.with(this).load(currentOnePiece!!.imageUrl).into(topImage)
                findViewById<ImageView>(R.id.removeTop).visibility = View.VISIBLE
                tvPlusTopLabel.visibility = View.GONE
            } else {
                topImage.setImageDrawable(null)
                findViewById<ImageView>(R.id.removeTop).visibility = View.GONE
                tvPlusTopLabel.visibility = View.VISIBLE
                tvPlusTopLabel.text = "+ Add One-piece"
            }
        }
    }

    private fun updateWeatherButtonUI() {
        weatherToggleBtn.text = if (isWeatherFilterOn) "CC Weather: ON" else "CC Weather: OFF"
        weatherToggleBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(if (isWeatherFilterOn) "#4CAF50" else "#BDBDBD")
        )
    }

    private fun updatePickerState(category: String) {
        activePickerCategory = category
        filterAndDisplayMatches()
    }

    private fun setSlotItem(category: String, item: ClothingItem?) {
        when (category) {
            "Top" -> currentTop = item
            "Bottom" -> currentBottom = item
            "Outerwear" -> currentOuterwear = item
            "One-piece" -> currentOnePiece = item
        }

        val imageView = when (category) {
            "Top", "One-piece" -> topImage
            "Bottom" -> bottomImage
            else -> outerwearImage
        }

        val removeBtn = when (category) {
            "Top", "One-piece" -> findViewById<ImageView>(R.id.removeTop)
            "Bottom" -> findViewById<ImageView>(R.id.removeBottom)
            else -> findViewById<ImageView>(R.id.removeOuterwear)
        }

        val plusText = when (category) {
            "Top", "One-piece" -> tvPlusTopLabel
            "Bottom" -> tvPlusBottomLabel
            else -> findViewById<TextView>(R.id.plusOuterwear)
        }

        if (item != null) {
            Glide.with(this).load(item.imageUrl).into(imageView)
            removeBtn.visibility = View.VISIBLE
            plusText.visibility = View.GONE
        } else {
            imageView.setImageDrawable(null)
            removeBtn.visibility = View.GONE
            plusText.visibility = View.VISIBLE
            updateUiForCurrentMode()
            updatePickerState(category)
        }
    }

    private fun loadColorMatches(baseItem: ClothingItem) {
        val primaryColor = baseItem.color.firstOrNull { it.isNotBlank() } ?: "#FFFFFF"

        RetrofitInstance.colorApi
            .getColorScheme(hex = primaryColor.removePrefix("#"), mode = "triad")
            .enqueue(object : Callback<com.example.myapplication.networkapi.ColorSchemeResponse> {
                override fun onResponse(
                    call: Call<com.example.myapplication.networkapi.ColorSchemeResponse>,
                    response: Response<com.example.myapplication.networkapi.ColorSchemeResponse>
                ) {
                    matchingColors = if (response.isSuccessful) {
                        response.body()?.colors?.map { it.hex.value } ?: emptyList()
                    } else {
                        baseItem.color
                    }
                    downloadClothesOnce()
                }

                override fun onFailure(call: Call<com.example.myapplication.networkapi.ColorSchemeResponse>, t: Throwable) {
                    matchingColors = baseItem.color
                    downloadClothesOnce()
                }
            })
    }

    private fun downloadClothesOnce() {
        FirebaseManager.firestore.collection("clothingItems")
            .get()
            .addOnSuccessListener { result ->
                allDatabaseClothes = result.map { it.toObject(ClothingItem::class.java) }
                weatherFilteredClothes = allDatabaseClothes.filter {
                    it.thickness.equals(todaysPick?.thickness, ignoreCase = true)
                }
                filterAndDisplayMatches()
            }
    }

    private fun filterAndDisplayMatches() {
        if (activePickerCategory == "None") {
            unifiedMatchesRecycler.adapter = DressroomMatchAdapter(emptyList()) {}
            return
        }

        val sourceList = if (isWeatherFilterOn) weatherFilteredClothes else allDatabaseClothes

        var categoryClothes = sourceList.filter {
            if (activePickerCategory == "One-piece") {
                it.type.equals("One-piece", ignoreCase = true) || it.type.equals("Dress", ignoreCase = true)
            } else {
                it.type == activePickerCategory
            }
        }

        if (isWeatherFilterOn && todaysPick != null) {
            categoryClothes = categoryClothes.filter { it.thickness == todaysPick!!.thickness }
        }

        val isCanvasEmpty = currentTop == null && currentBottom == null && currentOuterwear == null && currentOnePiece == null

        if (isCanvasEmpty) {
            unifiedMatchesRecycler.adapter = DressroomMatchAdapter(categoryClothes) { selectedItem ->
                setSlotItem(activePickerCategory, selectedItem)
                loadColorMatches(selectedItem)
                updatePickerState("None")
            }
        } else {
            val anchorItem = currentTop ?: currentBottom ?: currentOuterwear ?: currentOnePiece ?: todaysPick
            val searchColor = matchingColors.firstOrNull() ?: anchorItem?.color?.firstOrNull { it.isNotBlank() } ?: "#FFFFFF"

            val matchedItems = ColorMatchingManager.findMatchingClothes(searchColor, categoryClothes, 50)

            unifiedMatchesRecycler.adapter = DressroomMatchAdapter(matchedItems) { selectedItem ->
                setSlotItem(activePickerCategory, selectedItem)
                updatePickerState("None")
            }
        }
    }
}