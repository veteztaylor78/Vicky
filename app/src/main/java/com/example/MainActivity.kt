package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.EmpireScreen
import com.example.ui.EmpireViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: EmpireViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        EmpireScreen(viewModel = viewModel)
      }
    }
  }
}
