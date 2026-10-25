package com.mentalys.app.ui.mental.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mentalys.app.data.local.entity.mental.ScreeningHistoryEntity
import com.mentalys.app.data.remote.response.history.HistoryListResponse
import com.mentalys.app.data.remote.response.history.SaveHistoryResponse
import com.mentalys.app.data.repository.HistoryRepository
import com.mentalys.app.utils.Resource
import kotlinx.coroutines.launch

class ScreeningHistoryViewModel(
    private val repository: HistoryRepository
) : ViewModel() {

    val history: LiveData<List<ScreeningHistoryEntity>> = repository.getLocalHistory()

    private val _remoteHistory = MutableLiveData<Resource<HistoryListResponse>>()
    val remoteHistory: LiveData<Resource<HistoryListResponse>> = _remoteHistory

    private val _deleteResult = MutableLiveData<Resource<SaveHistoryResponse>>()
    val deleteResult: LiveData<Resource<SaveHistoryResponse>> = _deleteResult

    fun getAllRemoteHistory(token: String) {
        repository.getAllRemoteHistory(token).observeForever { result ->
            _remoteHistory.postValue(result)
        }
    }

    fun getRemoteHistory(token: String, type: String) {
        repository.getRemoteHistory(token, type).observeForever { result ->
            _remoteHistory.postValue(result)
        }
    }

    fun clearLocalHistory() {
        viewModelScope.launch {
            repository.clearLocalHistory()
        }
    }

    fun deleteAllRemoteHistory(token: String) {
        repository.deleteAllRemoteHistory(token).observeForever { result ->
            _deleteResult.postValue(result)
        }
    }
}
