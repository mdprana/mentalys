package com.mentalys.app.ui.mental.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mentalys.app.R
import com.mentalys.app.data.local.entity.mental.ScreeningHistoryEntity
import com.mentalys.app.databinding.ItemScreeningHistoryBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ScreeningHistoryAdapter :
    ListAdapter<ScreeningHistoryEntity, ScreeningHistoryAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScreeningHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val b: ItemScreeningHistoryBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: ScreeningHistoryEntity) {
            val ctx = itemView.context
            b.tvRiskCategory.text = localizeRiskCategory(item.riskCategory)
            b.tvFinalScore.text = ctx.getString(R.string.risk_score_label) + "%.2f".format(item.riskFinal)
            b.tvPhqScore.text = if (item.phqScore != null)
                ctx.getString(R.string.phq_score_formatted, item.phqScore, item.phqCategory ?: "")
            else
                ctx.getString(R.string.not_available)
            b.tvFaceScore.text = if (item.faceRiskScore != null) "%.2f".format(item.faceRiskScore)
            else ctx.getString(R.string.not_available)
            b.tvVoiceScore.text = if (item.audioRiskScore != null) "%.2f".format(item.audioRiskScore)
            else ctx.getString(R.string.not_available)
            b.tvDate.text = formatDate(item.createdAt)
            b.riskColorBar.setBackgroundColor(riskColor(item.riskCategory))
        }

        private fun formatDate(iso: String): String = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(iso) ?: return iso.take(10)
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            iso.take(10)
        }

        private fun riskColor(category: String): Int = when {
            category.contains("Minimal") -> Color.parseColor("#4CAF50")
            category.contains("Mild") -> Color.parseColor("#8BC34A")
            category.contains("Moderately Severe") -> Color.parseColor("#FF5722")
            category.contains("Moderate") -> Color.parseColor("#FF9800")
            category.contains("Severe") || category.contains("High") -> Color.parseColor("#F44336")
            else -> Color.parseColor("#9E9E9E")
        }

        private fun localizeRiskCategory(category: String): String {
            val ctx = itemView.context
            return when {
                category == "Minimal" -> ctx.getString(R.string.risk_minimal)
                category == "Mild" -> ctx.getString(R.string.risk_mild)
                category == "Moderately Severe" -> ctx.getString(R.string.risk_moderately_severe)
                category == "Moderate" -> ctx.getString(R.string.risk_moderate)
                category == "Severe" -> ctx.getString(R.string.risk_severe)
                category.contains("High") -> ctx.getString(R.string.risk_high_override)
                else -> category
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ScreeningHistoryEntity>() {
            override fun areItemsTheSame(a: ScreeningHistoryEntity, b: ScreeningHistoryEntity) =
                a.id == b.id

            override fun areContentsTheSame(a: ScreeningHistoryEntity, b: ScreeningHistoryEntity) =
                a == b
        }
    }
}
