package com.mentalys.app.ui.mental.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mentalys.app.R
import com.mentalys.app.data.local.entity.mental.ScreeningHistoryEntity
import com.mentalys.app.data.remote.response.history.HistoryItemResponse
import com.mentalys.app.databinding.FragmentMentalHistoryBinding
import com.mentalys.app.ui.viewmodels.ViewModelFactory
import com.mentalys.app.utils.AuthTokenProvider
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.showSnackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MentalHistoryFragment : Fragment() {

    private var _binding: FragmentMentalHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScreeningHistoryViewModel by viewModels {
        ViewModelFactory.getInstance(requireContext())
    }
    private val adapter = ScreeningHistoryAdapter()
    private var authToken: String = ""
    private var usingRemoteHistory: Boolean = false
    private var localHistoryCache: List<ScreeningHistoryEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMentalHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        binding.btnDeleteHistory.setOnClickListener {
            showDeleteConfirmation()
        }

        observeLocalHistory()
        observeRemoteHistory()
        observeDeleteResult()

        viewLifecycleOwner.lifecycleScope.launch {
            authToken = AuthTokenProvider.getFreshToken(requireContext())
            usingRemoteHistory = authToken.isNotBlank()

            if (usingRemoteHistory) {
                viewModel.getAllRemoteHistory(authToken)
            }
        }
    }

    private fun observeLocalHistory() {
        viewModel.history.observe(viewLifecycleOwner) { items ->
            localHistoryCache = items.orEmpty()
            if (!usingRemoteHistory) {
                renderHistory(localHistoryCache)
            }
        }
    }

    private fun observeRemoteHistory() {
        viewModel.remoteHistory.observe(viewLifecycleOwner) { result ->
            if (!usingRemoteHistory) return@observe

            when (result) {
                is Resource.Success -> renderHistory(
                    result.data.history.orEmpty().map { it.toScreeningHistoryEntity() }
                )
                Resource.Empty -> renderHistory(localHistoryCache)
                is Resource.Error -> {
                    renderHistory(localHistoryCache)
                    showSnackbar(binding.root, result.error)
                }
                is Resource.InvalidResponse -> {
                    renderHistory(localHistoryCache)
                    showSnackbar(binding.root, result.error)
                }
                is Resource.NetworkTimeout -> {
                    renderHistory(localHistoryCache)
                    showSnackbar(binding.root, result.error)
                }
                is Resource.Unauthorized -> {
                    renderHistory(localHistoryCache)
                    showSnackbar(binding.root, result.error)
                }
                Resource.Loading -> Unit
            }
        }
    }

    private fun observeDeleteResult() {
        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Success -> {
                    com.mentalys.app.utils.showToast(requireContext(), getString(R.string.history_deleted))
                    viewModel.clearLocalHistory()
                    renderHistory(emptyList())
                    if (authToken.isNotBlank()) viewModel.getAllRemoteHistory(authToken)
                }
                is Resource.Error -> showSnackbar(binding.root, result.error)
                is Resource.InvalidResponse -> showSnackbar(binding.root, result.error)
                is Resource.NetworkTimeout -> showSnackbar(binding.root, result.error)
                is Resource.Unauthorized -> showSnackbar(binding.root, result.error)
                Resource.Empty -> com.mentalys.app.utils.showToast(requireContext(), getString(R.string.history_deleted))
                Resource.Loading -> Unit
            }
        }
    }

    private fun renderHistory(items: List<ScreeningHistoryEntity>) {
        adapter.submitList(items)
        val isEmpty = items.isEmpty()
        binding.rvHistory.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.btnDeleteHistory.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_history_title))
            .setMessage(getString(R.string.delete_history_message))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                if (usingRemoteHistory && authToken.isNotBlank()) {
                    viewModel.deleteAllRemoteHistory(authToken)
                } else {
                    viewModel.clearLocalHistory()
                    com.mentalys.app.utils.showToast(requireContext(), getString(R.string.history_deleted))
                }
            }
            .show()
    }

    private fun HistoryItemResponse.toScreeningHistoryEntity(): ScreeningHistoryEntity {
        val meta = metadata.orEmpty()
        val additionalPrompt = meta["additionalPrompt"] as? Map<*, *> ?: emptyMap<Any, Any?>()
        val modalities = (meta["modalitiesUsed"] as? List<*>)
            ?.mapNotNull { it?.toString() }
            ?.map { if (it == "audio") "voice" else it }
            ?.joinToString(",")
            ?: type.orEmpty().replace("_requests", "").replace("audio", "voice")

        return ScreeningHistoryEntity(
            id = id ?: timestamp ?: System.currentTimeMillis().toString(),
            phqScore = additionalPrompt.numberValue("phqScore")?.toInt(),
            phqCategory = additionalPrompt.stringValue("phqCategory"),
            faceRiskScore = additionalPrompt.numberValue("faceScore")?.toFloat(),
            audioRiskScore = additionalPrompt.numberValue("audioScore")?.toFloat(),
            riskFinal = meta.numberValue("riskScore")?.toFloat()
                ?: additionalPrompt.numberValue("finalScore")?.toFloat()
                ?: 0f,
            riskCategory = meta.stringValue("riskCategory")
                ?: prediction?.result.predictionLabel()
                ?: getString(R.string.not_available),
            modalitiesUsed = modalities,
            createdAt = timestamp ?: currentUtcTimestamp()
        )
    }

    private fun Map<*, *>.stringValue(key: String): String? {
        return this[key]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun Map<*, *>.numberValue(key: String): Double? {
        return when (val value = this[key]) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun Any?.predictionLabel(): String? {
        return when (this) {
            is Map<*, *> -> this["prediction"]?.toString()
            is String -> this
            else -> null
        }?.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun currentUtcTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
