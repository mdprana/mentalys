package com.mentalys.app.ui.specialist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mentalys.app.data.remote.response.psychiatrist.PsychiatristContactResponse
import com.mentalys.app.data.repository.PsychiatristContactRepository
import com.mentalys.app.utils.Resource
import kotlinx.coroutines.launch

class PsychiatristContactViewModel(
    private val repository: PsychiatristContactRepository,
) : ViewModel() {

    private val _psychiatrists = MutableLiveData<Resource<List<PsychiatristContactResponse>>>()
    val psychiatrists: LiveData<Resource<List<PsychiatristContactResponse>>> = _psychiatrists

    private val _psychiatrist = MutableLiveData<Resource<PsychiatristContactResponse>>()
    val psychiatrist: LiveData<Resource<PsychiatristContactResponse>> = _psychiatrist

    fun getPsychiatrists(latitude: Double, longitude: Double, radius: Int = 5000) {
        viewModelScope.launch {
            repository.getPsychiatrists(latitude, longitude, radius).observeForever { result ->
                _psychiatrists.postValue(result)
            }
        }
    }

    fun getPsychiatristDetail(placeId: String) {
        viewModelScope.launch {
            repository.getPsychiatristDetail(placeId).observeForever { result ->
                _psychiatrist.postValue(result)
            }
        }
    }
}
