package com.buddy.quickchat.ui.profile

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.buddy.quickchat.util.QrCodeGenerator
import com.buddy.quickchat.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userState by viewModel.userState.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadProfilePicture(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = userState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Success -> {
                    val user = state.data
                    AsyncImage(
                        model = user.photoUrl.ifEmpty { "https://ui-avatars.com/api/?name=${user.name}" },
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { launcher.launch("image/*") }) {
                        Text("Change Picture")
                    }
                    if (uploadState is UiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = user.name, style = MaterialTheme.typography.headlineMedium)
                    Text(text = user.email, style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = "My QR Code", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val qrBitmap = remember(user.uid) { QrCodeGenerator.generateQrCode("quickchat:${user.uid}") }
                    qrBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(200.dp)
                        )
                    }
                }
                is UiState.Error -> Text(text = state.message, color = MaterialTheme.colorScheme.error)
                else -> {}
            }
        }
    }
}
