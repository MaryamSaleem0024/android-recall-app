package com.example.selftalker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.selftalker.ui.theme.SelfTalkerTheme
import com.example.selftalker.navBar.MainScreenNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SelfTalkerTheme{
                MainScreenNavHost()
            }
        }
    }
}
