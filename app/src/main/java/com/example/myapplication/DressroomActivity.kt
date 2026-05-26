package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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

class DressroomActivity : AppCompatActivity() {

    private var currentTop: ClothingItem? = null
    private var currentBottom: ClothingItem? = null
    private var currentOuterwear: ClothingItem? = null

    private var todaysPick: ClothingItem? = null
    private var allDatabaseClothes: List<ClothingItem> = emptyList()
    private var weatherFilteredClothes: List<ClothingItem> = emptyList()

    private var activePickerCategory: String = "None"
    private var matchingColors: List<String> = emptyList()
    private var isWeatherFilterOn: Boolean = true

    private lateinit var unifiedMatchesRecycler: RecyclerView
    private lateinit var topImage: ImageView
    private lateinit var bottomImage: ImageView
    private lateinit var outerwearImage: ImageView
    private lateinit var weatherToggleBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dressroom)

        setupViews()

        @Suppress("DEPRECATION")
        todaysPick = intent.getSerializableExtra("SELECTED_CLOTHING") as? ClothingItem

        // Ensure initial weather state aligns with today's pick
        isWeatherFilterOn = (todaysPick != null)
        updateWeatherButtonUI()

        if (todaysPick != null) {
            setSlotItem(todaysPick!!.type, todaysPick)

            activePickerCategory = when (todaysPick!!.type) {
                "Top" -> "Bottom"
                "Bottom" -> "Top"
                else -> "Top"
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

        weatherToggleBtn.setOnClickListener {
            isWeatherFilterOn = !isWeatherFilterOn
            updateWeatherButtonUI()
            filterAndDisplayMatches()
            unifiedMatchesRecycler.scrollToPosition(0)
        }

        findViewById<CardView>(R.id.cardTop).setOnClickListener {
            if (currentTop == null) updatePickerState("Top")
        }

        findViewById<CardView>(R.id.cardBottom).setOnClickListener {
            if (currentBottom == null) updatePickerState("Bottom")
        }

        findViewById<CardView>(R.id.cardOuterwear).setOnClickListener {
            if (currentOuterwear == null) updatePickerState("Outerwear")
        }

        findViewById<ImageView>(R.id.removeTop).setOnClickListener {
            setSlotItem("Top", null)
        }

        findViewById<ImageView>(R.id.removeBottom).setOnClickListener {
            setSlotItem("Bottom", null)
        }

        findViewById<ImageView>(R.id.removeOuterwear).setOnClickListener {
            setSlotItem("Outerwear", null)
        }
    }

    private fun updateWeatherButtonUI() {

        weatherToggleBtn.text =
            if (isWeatherFilterOn) "⛅ Weather: ON"
            else "⛅ Weather: OFF"

        weatherToggleBtn.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(
                    if (isWeatherFilterOn) "#4CAF50" else "#BDBDBD"
                )
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
        }

        val imageView = when (category) {
            "Top" -> topImage
            "Bottom" -> bottomImage
            else -> outerwearImage
        }

        val removeBtn = when (category) {
            "Top" -> findViewById<ImageView>(R.id.removeTop)
            "Bottom" -> findViewById<ImageView>(R.id.removeBottom)
            else -> findViewById<ImageView>(R.id.removeOuterwear)
        }

        val plusText = when (category) {
            "Top" -> findViewById<TextView>(R.id.plusTop)
            "Bottom" -> findViewById<TextView>(R.id.plusBottom)
            else -> findViewById<TextView>(R.id.plusOuterwear)
        }

        if (item != null) {

            Glide.with(this)
                .load(item.imageUrl)
                .into(imageView)

            removeBtn.visibility = View.VISIBLE
            plusText.visibility = View.GONE

        } else {

            imageView.setImageDrawable(null)

            removeBtn.visibility = View.GONE
            plusText.visibility = View.VISIBLE

            updatePickerState(category)
        }

        if (
            currentTop == null &&
            currentBottom == null &&
            currentOuterwear == null
        ) {
            isWeatherFilterOn = false
            updateWeatherButtonUI()
        }
    }

    private fun loadColorMatches(baseItem: ClothingItem) {

        val primaryColor =
            baseItem.color.firstOrNull { it.isNotBlank() }
                ?: "#FFFFFF"

        RetrofitInstance.colorApi
            .getColorScheme(
                hex = primaryColor.removePrefix("#"),
                mode = "triad"
            )
            .enqueue(object :
                Callback<com.example.myapplication.networkapi.ColorSchemeResponse> {

                override fun onResponse(
                    call: Call<com.example.myapplication.networkapi.ColorSchemeResponse>,
                    response: Response<com.example.myapplication.networkapi.ColorSchemeResponse>
                ) {

                    matchingColors =
                        if (response.isSuccessful) {
                            response.body()?.colors?.map { it.hex.value }
                                ?: emptyList()
                        } else {
                            baseItem.color
                        }

                    downloadClothesOnce()
                }

                override fun onFailure(
                    call: Call<com.example.myapplication.networkapi.ColorSchemeResponse>,
                    t: Throwable
                ) {

                    matchingColors = baseItem.color
                    downloadClothesOnce()
                }
            })
    }

    private fun downloadClothesOnce() {

        FirebaseManager.firestore
            .collection("clothingItems")
            .get()
            .addOnSuccessListener { result ->

                allDatabaseClothes =
                    result.map {
                        it.toObject(ClothingItem::class.java)
                    }

                // LOG THE THICKNESSES TO FIND THE TYPO
                allDatabaseClothes.forEach {
                    Log.d(
                        "THICKNESS_DEBUG",
                        "Item: ${it.imageUrl}, Thickness: '${it.thickness}'"
                    )
                }

                Log.d(
                    "THICKNESS_DEBUG",
                    "Today's Pick Thickness: '${todaysPick?.thickness}'"
                )

                // Relaxed filter: Use equals with ignoreCase = true
                weatherFilteredClothes =
                    allDatabaseClothes.filter {
                        it.thickness.equals(
                            todaysPick?.thickness,
                            ignoreCase = true
                        )
                    }

                filterAndDisplayMatches()
            }
    }

    private fun filterAndDisplayMatches() {

        if (activePickerCategory == "None") {

            unifiedMatchesRecycler.adapter =
                DressroomMatchAdapter(emptyList()) {}

            return
        }

        val sourceList =
            if (isWeatherFilterOn)
                weatherFilteredClothes
            else
                allDatabaseClothes

        // DEBUG: Print every single item in the sourceList
        Log.d("DEBUG_COUNT", "Full list size: ${sourceList.size}")

        // ADD THIS LOGIC TO FIND THE MISSING JACKET
        val jacket =
            sourceList.find {
                it.imageUrl.contains("leather")
            } // Adjust string

        if (jacket != null) {

            val matchesCategory =
                jacket.type == activePickerCategory

            val matchesThickness =
                jacket.thickness == todaysPick?.thickness

            Log.d(
                "JACKET_STATUS",
                "Found jacket! Matches Category? $matchesCategory. Matches Thickness? $matchesThickness"
            )

        } else {

            Log.d(
                "JACKET_STATUS",
                "Jacket not even in the source list!"
            )
        }

        var categoryClothes =
            sourceList.filter {
                it.type == activePickerCategory
            }

        if (isWeatherFilterOn && todaysPick != null) {

            categoryClothes =
                categoryClothes.filter {
                    it.thickness == todaysPick!!.thickness
                }
        }

        val isCanvasEmpty =
            currentTop == null &&
                    currentBottom == null &&
                    currentOuterwear == null

        // NEW LOGIC: If the canvas is empty, skip the complex math
        if (isCanvasEmpty) {

            // Just show the relevant category items directly
            unifiedMatchesRecycler.adapter =
                DressroomMatchAdapter(categoryClothes) { selectedItem ->

                    setSlotItem(activePickerCategory, selectedItem)

                    // Now it will fetch the colors for the next step
                    loadColorMatches(selectedItem)

                    updatePickerState("None")
                }

        } else {

            // ONLY run the complex math if there is actually an anchor item
            val anchorItem =
                currentTop
                    ?: currentBottom
                    ?: currentOuterwear
                    ?: todaysPick

            val searchColor =
                matchingColors.firstOrNull()
                    ?: anchorItem?.color?.firstOrNull {
                        it.isNotBlank()
                    }
                    ?: "#FFFFFF"

            // Now the sorting/scoring logic runs here,
            // knowing it has a valid anchor
            var matchedItems =
                ColorMatchingManager.findMatchingClothes(
                    searchColor,
                    categoryClothes,
                    50
                )

            // ... (keep the rest of your list injection logic here)

            unifiedMatchesRecycler.adapter =
                DressroomMatchAdapter(matchedItems) { selectedItem ->

                    setSlotItem(activePickerCategory, selectedItem)

                    updatePickerState("None")
                }
        }
    }
}