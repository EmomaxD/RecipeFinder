package com.example.recipefinder

import android.util.Log
import androidx.compose.material.MaterialTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.recipefinder.api.Meal
import com.example.recipefinder.api.RetrofitClient
import com.example.recipefinder.api.jsonObjectToMeal
import com.example.recipefinder.firebase.AuthHandler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun FavouritePage(navController: NavController) {
    var favoriteMeals by remember { mutableStateOf<List<Meal>>(emptyList()) }
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val orangeColor = Color(0xFFFF8C00)
    val orangeGradient = Brush.verticalGradient(
        colors = listOf(orangeColor, orangeColor.copy(alpha = 0.7f))
    )
    val userId = user?.uid
    if (userId == null) {

        Text("User not logged in", color = Color.Red)
        return
    }

    LaunchedEffect(user) {
        if (user != null) {
            val favoriteMealIds = db.collection("Favorites")
                .whereEqualTo("userId", user.uid)
                .get()
                .await()
                .documents.map { it.getString("mealId") ?: "" }

            if (favoriteMealIds.isNotEmpty()) {
                val mealList = mutableListOf<Meal>()
                favoriteMealIds.forEach { mealId ->
                    makeRequest(mealId, RetrofitClient.instance::getById) { meals ->
                        if (meals != null && meals.isNotEmpty()) {
                            mealList.addAll(meals)
                            favoriteMeals = mealList
                        } else {
                            Log.e("FavouritePage", "No meal found for ID: $mealId")
                        }
                    }
                }
            } else {
                Log.d("FavouritePage", "No favorite meals found for user: ${user.uid}")
            }
        } else {
            Log.d("FavouritePage", "User is null")
        }
    }

    Column(modifier = Modifier.padding(16.dp)
        .fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(orangeGradient)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                contentDescription = "Back",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { navController.popBackStack() },
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Favorites",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                color = Color.White


            )
            }

        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(favoriteMeals) { meal ->
                RecipeItem(navController = navController, meal = meal, modifier = Modifier)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun RecipeItem(
    navController: NavController,
    meal: Meal,
    modifier: Modifier = Modifier
) {
    var isFavorite by remember { mutableStateOf(false) }
    val user = FirebaseAuth.getInstance().currentUser
    val userId = user?.uid
    val db = FirebaseFirestore.getInstance()
    val firebaseManager = AuthHandler(LocalContext.current)


    LaunchedEffect(meal.idMeal, userId) {
        if (userId != null && meal.idMeal != null) {
            val favoriteRef = db.collection("Favorites")
                .whereEqualTo("userId", userId)
                .whereEqualTo("mealId", meal.idMeal)
                .get()
                .await()

            isFavorite = !favoriteRef.isEmpty
        }
    }

    Card(
        modifier = Modifier
            .clickable {
                MainActivity.staticMeal = meal
                navController.navigate("recipe")
            }
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                val painter = rememberImagePainter(data = meal.strMealThumb)

                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = meal.strMeal.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "4.5 ★",
                            color = Color.Gray
                        )
                    }
                }
            }
            IconButton(
                onClick = {
                    isFavorite = !isFavorite
                    if (userId != null) {
                        if (isFavorite) {
                            meal.idMeal?.let {
                                firebaseManager.addFavorite(
                                    mealId = it,
                                    userId = userId,
                                    onSuccess = {},
                                    onFailure = {}
                                )
                            }
                        } else {
                            meal.idMeal?.let {
                                firebaseManager.removeFavorite(
                                    mealId = it,
                                    userId = userId,
                                    onSuccess = {},
                                    onFailure = {}
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(50.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.favorite_full_shape),
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFFF4081) else Color.Gray
                )
            }
        }
    }
}

