package ru.takeshiko.matuleme.presentation.category

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import ru.takeshiko.matuleme.R
import ru.takeshiko.matuleme.data.local.AppPreferencesManager
import ru.takeshiko.matuleme.data.remote.SupabaseClientManager
import ru.takeshiko.matuleme.data.repository.ProductCategoryRepositoryImpl
import ru.takeshiko.matuleme.data.repository.ProductRepositoryImpl
import ru.takeshiko.matuleme.data.repository.UserCartItemRepositoryImpl
import ru.takeshiko.matuleme.data.repository.UserFavoriteRepositoryImpl
import ru.takeshiko.matuleme.domain.models.database.Product
import ru.takeshiko.matuleme.domain.models.database.ProductCategory
import ru.takeshiko.matuleme.domain.models.database.UserCartItem
import ru.takeshiko.matuleme.domain.models.database.UserFavorite
import ru.takeshiko.matuleme.domain.models.result.DataResult

class CategoryViewModel (
    private val appPreferencesManager: AppPreferencesManager,
    private val supabaseClientManager: SupabaseClientManager
) : ViewModel() {

    private val _productCategoryResult = MutableLiveData<DataResult<List<ProductCategory>>>()
    val productCategoryResult: LiveData<DataResult<List<ProductCategory>>> = _productCategoryResult

    private val _productsResult = MutableLiveData<DataResult<List<Product>>>()
    val productsResult: LiveData<DataResult<List<Product>>> = _productsResult

    private val _favorites = MutableLiveData<Set<String>>(emptySet())
    val favorites: LiveData<Set<String>> = _favorites

    private val _cartItems = MutableLiveData<Set<String>>(emptySet())
    val cartItems: LiveData<Set<String>> = _cartItems

    private val productCategoryRepository = ProductCategoryRepositoryImpl(supabaseClientManager)
    private val productRepository = ProductRepositoryImpl(supabaseClientManager)
    private val userFavoriteRepository = UserFavoriteRepositoryImpl(supabaseClientManager)
    private val userCartItemRepository = UserCartItemRepositoryImpl(supabaseClientManager)

    fun getAllProductCategories() {
        viewModelScope.launch {
            when (val result = productCategoryRepository.getAll()) {
                is DataResult.Success -> {
                    val allCategory = ProductCategory(
                        id = "all",
                        name = appPreferencesManager.getString(R.string.all_shoes),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )
                    val categoriesWithAll = listOf(allCategory) + result.data
                    _productCategoryResult.value = DataResult.Success(categoriesWithAll)
                }
                is DataResult.Error -> _productCategoryResult.value = DataResult.Error(result.message)
            }
        }
    }

    fun getProductsByCategory(categoryId: String) {
        viewModelScope.launch {
            if (categoryId == "all") {
                when (val result = productRepository.getAll()) {
                    is DataResult.Success -> _productsResult.value = DataResult.Success(result.data)
                    is DataResult.Error -> _productsResult.value = DataResult.Error(result.message)
                }
                return@launch
            }

            when (val result = productRepository.getByCategory(categoryId)) {
                is DataResult.Success -> _productsResult.value = DataResult.Success(result.data)
                is DataResult.Error -> _productsResult.value = DataResult.Error(result.message)
            }
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            supabaseClientManager.auth.currentUserOrNull()?.id?.let { userId ->
                when (val result = userFavoriteRepository.getByUserId(userId)) {
                    is DataResult.Success -> {
                        _favorites.value = result.data.map { it.productId }.toSet()
                    }
                    is DataResult.Error -> Log.e(javaClass.name, "Error loading favorites!")
                }
            } ?: run {
                Log.d(javaClass.name, "User not authenticated!")
            }
        }
    }

    fun loadCartItems() {
        viewModelScope.launch {
            supabaseClientManager.auth.currentUserOrNull()?.id?.let { userId ->
                when (val result = userCartItemRepository.getByUserId(userId)) {
                    is DataResult.Success -> {
                        _cartItems.value = result.data.map { it.productId }.toSet()
                    }
                    is DataResult.Error -> Log.e(javaClass.name, "Error loading cart!")
                }
            } ?: run {
                Log.d(javaClass.name, "User not authenticated!")
            }
        }
    }

    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            supabaseClientManager.auth.currentUserOrNull()?.id?.let { userId ->
                if (_favorites.value?.contains(productId) == true) {
                    userFavoriteRepository.removeFavorite(userId, productId)
                } else {
                    userFavoriteRepository.addFavorite(
                        UserFavorite(
                            userId = userId,
                            productId = productId,
                            addedAt = Clock.System.now()
                        )
                    )
                }
            } ?: run {
                Log.d(javaClass.name, "User not authenticated!")
            }
            loadFavorites()
        }
    }

    fun toggleCartItem(productId: String) {
        viewModelScope.launch {
            supabaseClientManager.auth.currentUserOrNull()?.id?.let { userId ->
                if (_cartItems.value?.contains(productId) == true) {
                    userCartItemRepository.removeCartItem(userId, productId)
                } else {
                    userCartItemRepository.addCartItem(
                        UserCartItem(
                            userId = userId,
                            productId = productId,
                            quantity = 1,
                            addedAt = Clock.System.now()
                        )
                    )
                }
            } ?: run {
                Log.d(javaClass.name, "User not authenticated!")
            }
            loadCartItems()
        }
    }
}