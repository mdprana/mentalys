package com.mentalys.app.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.mentalys.app.data.local.entity.ArticleEntity
import com.mentalys.app.data.local.entity.ArticleListEntity
import com.mentalys.app.data.local.entity.FoodEntity
import com.mentalys.app.data.local.room.ArticleDao
import com.mentalys.app.utils.ApiContentLocalizer
import com.mentalys.app.utils.Resource
import com.mentalys.app.data.remote.retrofit.ArticlesApiService
import com.mentalys.app.utils.getErrorMessage

class ArticleRepository(
    private val apiService: ArticlesApiService,
    private val articleDao: ArticleDao,
) {

    fun getArticle(id: String): LiveData<Resource<ArticleEntity>> = liveData {
        emit(Resource.Loading)
        var remoteError: String? = null

        try {
            val response = apiService.getArticleById(id)
            if (response.isSuccessful) {
                val article = response.body()?.data
                val articleEntities = article?.toEntity()
                if (articleEntities != null) {
                    articleDao.insertArticle(articleEntities)
                    emit(Resource.Success(ApiContentLocalizer.article(articleEntities)))
                    return@liveData
                } else {
                    remoteError = "No article found in response."
                    Log.d("ArticleRepository", remoteError ?: "")
                }
            } else {
                remoteError = response.errorBody()?.string()
                    ?: response.message()
                    ?: "Unknown error"
                Log.d("ArticleRepository", "API call failed: $remoteError")
            }
        } catch (e: Exception) {
            remoteError = getErrorMessage(e)
            Log.d("ArticleRepository", "Error: $remoteError", e)
        }

        val cachedArticle = articleDao.getArticleOnce(id)
        if (cachedArticle != null) {
            emit(Resource.Success(ApiContentLocalizer.article(cachedArticle)))
        } else {
            emit(Resource.Error(remoteError ?: "Article not found. Please refresh the article list."))
        }

    }

    // new
    fun getAllArticle(): LiveData<Resource<List<ArticleListEntity>>> = liveData {
        emit(Resource.Loading)
        var remoteError: String? = null
        try {
            val response = apiService.getAllArticle()
            if (response.isSuccessful) {
                val articles = response.body()?.data?.articles
                val articleEntities = articles?.map { it.toEntity() }
                if (!articleEntities.isNullOrEmpty()) {
                    articleDao.clearArticles()
                    articleDao.insertListArticle(articleEntities)
                    emit(Resource.Success(localizeArticles(articleEntities)))
                } else {
                    remoteError = "No article data available from the API."
                    Log.d("ArticleRepository", remoteError)
                }
            } else {
                remoteError = response.message() ?: "Unknown error"
                Log.d("ArticleRepository", "API call failed: $remoteError")
            }
        } catch (e: Exception) {
            remoteError = getErrorMessage(e)
            Log.d("ArticleRepository", "Error: $remoteError", e)
        }

        articleDao.clearDummyArticles()

        val finalRoom = articleDao.getListArticleOnce()
        if (finalRoom.isNotEmpty()) {
            emit(Resource.Success(localizeArticles(finalRoom)))
        } else {
            emit(Resource.Error(remoteError ?: "No article data available from the API."))
        }

    }

    fun get3Article(): LiveData<Resource<List<ArticleListEntity>>> = liveData {
        emit(Resource.Loading)
        var remoteError: String? = null
        try {
            val response = apiService.getAllArticle()
            if (response.isSuccessful) {
                val articles = response.body()?.data?.articles
                val articleEntities = articles?.map { it.toEntity() }
                if (!articleEntities.isNullOrEmpty()) {
                    articleDao.clearArticles()
                    articleDao.insertListArticle(articleEntities)
                    emit(Resource.Success(localizeArticles(articleEntities).take(3)))
                } else {
                    remoteError = "No article data available from the API."
                    Log.d("ArticleRepository", remoteError)
                }
            } else {
                remoteError = response.message() ?: "Unknown error"
                Log.d("ArticleRepository", "API call failed: $remoteError")
            }
        } catch (e: Exception) {
            remoteError = getErrorMessage(e)
            Log.d("ArticleRepository", "Error fetching articles: $remoteError", e)
        }

        val finalRoom = articleDao.getListArticleOnce().take(3)
        if (finalRoom.isNotEmpty()) {
            emit(Resource.Success(localizeArticles(finalRoom)))
        } else {
            emit(Resource.Error(remoteError ?: "No article data available from the API."))
        }

    }

    // Get Food
    fun getAllFood(): LiveData<Resource<List<FoodEntity>>> = liveData {
        emit(Resource.Loading)
        var remoteError: String? = null
        try {
            val response = apiService.getAllFood()
            if (response.isSuccessful) {
                val foods = response.body()?.data
                val foodEntity = foods?.map { it.toEntity() }
                if (!foodEntity.isNullOrEmpty()) {
                    articleDao.clearFoods()
                    articleDao.insertFood(foodEntity)
                    emit(Resource.Success(localizeFoods(foodEntity)))
                } else {
                    remoteError = "No food data available from the API."
                    Log.d("ArticleRepository", remoteError)
                }
            } else {
                remoteError = response.message() ?: "Unknown error"
                Log.d("ArticleRepository", "API call failed: $remoteError")
            }
        } catch (e: Exception) {
            remoteError = getErrorMessage(e)
            Log.d("ArticleRepository", "Error: $remoteError", e)
        }

        articleDao.clearDummyFoods()

        val finalFoods = articleDao.getFoodOnce()
        if (finalFoods.isNotEmpty()) {
            emit(Resource.Success(localizeFoods(finalFoods)))
        } else {
            emit(Resource.Error(remoteError ?: "No food data available from the API."))
        }

    }

    // Get Food
    fun get4Food(): LiveData<Resource<List<FoodEntity>>> = liveData {
        emit(Resource.Loading)
        var remoteError: String? = null
        try {
            val response = apiService.getAllFood()
            if (response.isSuccessful) {
                val foods = response.body()?.data
                val foodEntity = foods?.map { it.toEntity() }
                if (!foodEntity.isNullOrEmpty()) {
                    articleDao.clearFoods()
                    articleDao.insertFood(foodEntity)
                    emit(Resource.Success(localizeFoods(foodEntity).take(4)))
                } else {
                    remoteError = "No food data available from the API."
                    Log.d("ArticleRepository", remoteError)
                }
            } else {
                remoteError = response.message() ?: "Unknown error"
                Log.d("ArticleRepository", "API call failed: $remoteError")
            }
        } catch (e: Exception) {
            remoteError = getErrorMessage(e)
            Log.d("ArticleRepository", "Error fetching foods: $remoteError", e)
        }

        val finalFoods = articleDao.getFoodOnce().take(4)
        if (finalFoods.isNotEmpty()) {
            emit(Resource.Success(localizeFoods(finalFoods)))
        } else {
            emit(Resource.Error(remoteError ?: "No food data available from the API."))
        }

    }

    fun getMentalStateArticle(mentalState: String): LiveData<Resource<List<ArticleListEntity>>> = liveData {
        emit(Resource.Loading)
        var remoteError: String? = null
        try {
            val response = apiService.getMentalStateArticle(mentalState)
            if (response.isSuccessful) {
                val articles = response.body()?.data?.articles
                val articleEntities = articles?.map { it.toEntity() }
                if (!articleEntities.isNullOrEmpty()) {
                    articleDao.clearArticles()
                    articleDao.insertListArticle(articleEntities)
                    emit(Resource.Success(localizeArticles(articleEntities)))
                } else {
                    remoteError = "No article data available from the API."
                    Log.d("ArticleRepository", remoteError)
                }
            } else {
                remoteError = response.message() ?: "Unknown error"
                Log.d("ArticleRepository", "API call failed: $remoteError")
            }
        } catch (e: Exception) {
            remoteError = getErrorMessage(e)
            Log.d("ArticleRepository", "Error: $remoteError", e)
        }

        val finalRoom = articleDao.getListArticleOnce()
        if (finalRoom.isNotEmpty()) {
            emit(Resource.Success(localizeArticles(finalRoom)))
        } else {
            emit(Resource.Error(remoteError ?: "No article data available from the API."))
        }

    }

    companion object {
        @Volatile
        private var instance: ArticleRepository? = null
        fun getInstance(
            articleApiService: ArticlesApiService,
            articleDao: ArticleDao,
        ): ArticleRepository = instance ?: synchronized(this) {
            instance ?: ArticleRepository(
                articleApiService,
                articleDao,
            )
        }.also { instance = it }
    }

    private fun localizeArticles(articles: List<ArticleListEntity>): List<ArticleListEntity> {
        return articles.map(ApiContentLocalizer::articleList)
    }

    private fun localizeFoods(foods: List<FoodEntity>): List<FoodEntity> {
        return foods.map(ApiContentLocalizer::food)
    }

}
