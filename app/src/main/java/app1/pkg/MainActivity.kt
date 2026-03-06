package app1.pkg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import app1.pkg.ui.CalendarScreen
import app1.pkg.ui.theme.App1Theme
import app1.pkg.viewmodel.CalendarViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CalendarViewModel = viewModel()
            val settings by viewModel.settings
            
            App1Theme(
                theme = settings.theme,
                darkTheme = settings.isDarkMode
            ) {
                CalendarScreen(viewModel)
            }
        }
    }
}
