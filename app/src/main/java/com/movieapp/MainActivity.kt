package com.movieapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.movieapp.features.movielist.MediaCategory
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.movieapp.features.moviedetail.MovieDetailScreen
import com.movieapp.features.moviedetail.MovieDetailViewModel
import com.movieapp.features.movielist.MovieListScreen
import com.movieapp.features.movielist.MovieListViewModel
import com.movieapp.features.search.SearchScreen
import com.movieapp.features.search.SearchViewModel
import com.movieapp.navigation.Screen
import com.movieapp.theme.AppThemeController
import com.movieapp.theme.MovieAppTheme
import com.movieapp.theme.NeubrutalismIcons
import com.movieapp.theme.bodyFontFamily
import com.movieapp.theme.buttonFontFamily
import com.movieapp.theme.headerFontFamily
import com.movieapp.theme.neoBorder
import com.movieapp.theme.neoColors
import com.movieapp.theme.neoShadow
import com.movieapp.util.AppLanguage
import com.movieapp.util.LocalizationManager
import com.movieapp.util.t

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.isForceDarkAllowed = false
        }
        setContent {
            MovieAppTheme {
                MainAppScaffold()
            }
        }
    }
}

@Composable
fun MainAppScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val neoColors = MaterialTheme.neoColors

    val movieListViewModel: MovieListViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val movieDetailViewModel: MovieDetailViewModel = viewModel()

    Scaffold(
        containerColor = neoColors.background,
        topBar = {
            TopAppBarNeobrutalist()
        },
        bottomBar = {
            BottomNavigationNeobrutalist(
                currentRoute = currentRoute,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Movies.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Movies.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Movies.route) {
                LaunchedEffect(Unit) {
                    movieListViewModel.selectCategory(MediaCategory.MOVIES)
                }
                MovieListScreen(
                    viewModel = movieListViewModel,
                    onTitleClick = { slug, isTv ->
                        movieDetailViewModel.loadDetail(slug, isTv)
                        navController.navigate(Screen.Detail.createRoute(slug, isTv))
                    }
                )
            }

            composable(Screen.TvShows.route) {
                LaunchedEffect(Unit) {
                    movieListViewModel.selectCategory(MediaCategory.TV_SHOWS)
                }
                MovieListScreen(
                    viewModel = movieListViewModel,
                    onTitleClick = { slug, isTv ->
                        movieDetailViewModel.loadDetail(slug, isTv)
                        navController.navigate(Screen.Detail.createRoute(slug, isTv))
                    }
                )
            }

            composable(Screen.Bookmarks.route) {
                com.movieapp.features.bookmarks.BookmarkScreen(
                    onTitleClick = { slug, isTv ->
                        movieDetailViewModel.loadDetail(slug, isTv)
                        navController.navigate(Screen.Detail.createRoute(slug, isTv))
                    }
                )
            }

            composable(Screen.Downloads.route) {
                com.movieapp.features.downloads.DownloadsScreen()
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = searchViewModel,
                    onTitleClick = { slug, isTv ->
                        movieDetailViewModel.loadDetail(slug, isTv)
                        navController.navigate(Screen.Detail.createRoute(slug, isTv))
                    }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("slug") { type = NavType.StringType },
                    navArgument("isTv") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug") ?: ""
                val isTv = backStackEntry.arguments?.getBoolean("isTv") ?: false

                androidx.compose.runtime.LaunchedEffect(slug, isTv) {
                    if (slug.isNotBlank()) {
                        movieDetailViewModel.loadDetail(slug, isTv)
                    }
                }

                MovieDetailScreen(
                    viewModel = movieDetailViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun TopAppBarNeobrutalist() {
    val neoColors = MaterialTheme.neoColors
    val isDark = AppThemeController.isDarkMode
    val currentLang = LocalizationManager.currentLanguage

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(neoColors.primary)
            .neoBorder(width = 2.5.dp, color = neoColors.border, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.yoteshinzone_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .neoBorder(width = 1.5.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                    .padding(3.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = t("app_title"),
                    fontFamily = headerFontFamily(),
                    fontSize = 19.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = neoColors.onPrimary
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = t("app_subtitle"),
                    fontFamily = bodyFontFamily(),
                    fontSize = 11.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = neoColors.onPrimary
                )
            }
        }

        // Quick Controls: Theme & Language Toggles (Vector Icons Only)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme Mode Toggle (Light / Dark)
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 38.dp, minHeight = 38.dp)
                    .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                    .background(neoColors.surface, RoundedCornerShape(8.dp))
                    .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                    .clickable { AppThemeController.toggleDarkMode() }
                    .semantics {
                        role = Role.Button
                        selected = isDark
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDark) NeubrutalismIcons.LightMode else NeubrutalismIcons.DarkMode,
                    contentDescription = if (isDark) t("theme_light") else t("theme_dark"),
                    tint = neoColors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Language Toggle (Icon Only)
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 38.dp, minHeight = 38.dp)
                    .neoShadow(offsetX = 2.dp, offsetY = 2.dp, color = neoColors.shadow, shape = RoundedCornerShape(8.dp))
                    .background(neoColors.surface, RoundedCornerShape(8.dp))
                    .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(8.dp))
                    .clickable { LocalizationManager.toggleLanguage() }
                    .semantics {
                        role = Role.Button
                        selected = currentLang == AppLanguage.MY
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NeubrutalismIcons.Language,
                    contentDescription = if (currentLang == AppLanguage.EN) t("lang_my") else t("lang_en"),
                    tint = neoColors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun BottomNavigationNeobrutalist(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    val neoColors = MaterialTheme.neoColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(neoColors.surface)
            .neoBorder(width = 2.dp, color = neoColors.border, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val items = listOf(
            Triple(Screen.Movies, t("nav_movies"), NeubrutalismIcons.Movie),
            Triple(Screen.TvShows, t("nav_tv_shows"), NeubrutalismIcons.Tv),
            Triple(Screen.Bookmarks, t("nav_bookmarks"), NeubrutalismIcons.Bookmark),
            Triple(Screen.Downloads, t("nav_download"), NeubrutalismIcons.Download)
        )

        items.forEach { (screen, label, icon) ->
            val isSelected = currentRoute == screen.route
            val activeColor = neoColors.primary
            val inactiveColor = neoColors.textSecondary

            Column(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 52.dp)
                    .clickable { onNavigate(screen) }
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                    }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val contentColor = if (isSelected) activeColor else inactiveColor
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = label,
                    fontFamily = buttonFontFamily(),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 3.dp)
                            .background(activeColor, RoundedCornerShape(2.dp))
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}
