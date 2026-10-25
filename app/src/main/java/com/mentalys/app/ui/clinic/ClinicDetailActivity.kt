package com.mentalys.app.ui.clinic

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.mentalys.app.R
import com.mentalys.app.data.remote.response.clinic.PlaceDetails
import com.mentalys.app.databinding.ActivityClinicDetailBinding
import com.mentalys.app.ui.viewmodels.ViewModelFactory
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.showToast

class ClinicDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClinicDetailBinding
    private val viewModel: ClinicViewModel by viewModels {
        ViewModelFactory.getInstance(this)
    }

    private var mapsQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityClinicDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backButton.setOnClickListener { finish() }
        binding.btnOpenMaps.setOnClickListener { openMaps() }

        val placeId = intent.getStringExtra(EXTRA_PLACE_ID)
        if (placeId.isNullOrBlank()) {
            showToast(this, "Clinic detail is unavailable.")
            finish()
            return
        }

        observeDetail()
        viewModel.getClinicDetail(placeId)
    }

    private fun observeDetail() {
        viewModel.clinicDetail.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    bindClinic(resource.data)
                }
                is Resource.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    showToast(this, "Clinic detail is empty.")
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showToast(this, resource.error)
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    showToast(this, "Unable to load clinic detail.")
                }
            }
        }
    }

    private fun bindClinic(clinic: PlaceDetails) {
        val address = clinic.formattedAddress ?: clinic.vicinity.orEmpty()
        mapsQuery = listOf(clinic.name, address).filter { it.isNotBlank() }.joinToString(", ")

        binding.tvClinicName.text = clinic.name
        binding.tvAddress.text = "Address: ${address.ifBlank { "Not available" }}"
        binding.tvRating.text = "Rating: ${clinic.rating?.toString() ?: "Not available"}"
        binding.tvPhone.text = "Phone: ${clinic.formattedPhoneNumber ?: "Not available"}"
        binding.tvWebsite.text = "Website: ${clinic.website ?: "Not available"}"
        binding.tvOpeningHours.text = clinic.openingHours?.weekdayText
            ?.joinToString(separator = "\n", prefix = "Opening hours:\n")
            ?: "Opening hours: Not available"

        val isOpen = clinic.openingHours?.openNow
        binding.tvOpenStatus.text = when (isOpen) {
            true -> getString(R.string.open)
            false -> getString(R.string.close)
            null -> "Open status unavailable"
        }
        binding.tvOpenStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (isOpen == true) R.color.primary else R.color.error_color
            )
        )

        Glide.with(this)
            .load(clinic.photoUrl)
            .placeholder(R.drawable.img_clinic)
            .error(R.drawable.img_clinic)
            .into(binding.imgClinic)
    }

    private fun openMaps() {
        if (mapsQuery.isBlank()) return
        val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(mapsQuery)}")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    companion object {
        const val EXTRA_PLACE_ID = "extra_place_id"
    }
}
