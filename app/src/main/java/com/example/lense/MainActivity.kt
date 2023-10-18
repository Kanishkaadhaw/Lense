package com.example.lense

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.lense.ui.theme.LenseTheme
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.lense.ml.EnL4Fp32
import org.tensorflow.lite.support.image.TensorImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LenseApp()
        }
    }
}

@Composable
fun LenseApp() {
    LenseTheme {
        Surface(
            Modifier.fillMaxSize(1f)
        )
        {
            SelectPhoto()
        }
    }
}

@Composable
fun SelectPhoto() {
    var photoUri: Uri? by remember { mutableStateOf(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        photoUri = uri
    }
    val model = EnL4Fp32.newInstance(LocalContext.current)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //Displays Image if Selected
        if (photoUri != null) {
            val painter = rememberAsyncImagePainter(
                ImageRequest
                    .Builder(LocalContext.current)
                    .data(data = photoUri)
                    .build()
            )
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxWidth()
                    .border(6.0.dp, Color.Gray),
                contentScale = ContentScale.Crop
            )
            //Pass Image to EN_L4_fp32.tflite
            val context = LocalContext.current // Get the current context.
            val contentResolver = context.contentResolver // Get the ContentResolver from the context.
            val source = ImageDecoder.createSource(contentResolver, photoUri!!)
            val bitmap = ImageDecoder.decodeBitmap(source)
            ImageClassifier(bitmap, model)
        }
        //Selects Image
        Button(onClick = {
            launcher.launch(PickVisualMediaRequest(
                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
            ))
        })
        {
            Text("Select Photo")
        }
    }
}

@Composable
fun ImageClassifier(bitmap: Bitmap, model: EnL4Fp32) {
    // Convert the bitmap to ARGB_8888 format
    val argb8888Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    // Creates inputs for reference.
    val image = TensorImage.fromBitmap(argb8888Bitmap)

    // Runs model inference and gets result.
    val outputs = model.process(image)
    val probability = outputs.probabilityAsCategoryList

    val maxProbabilityCategory = probability.maxByOrNull { it.score }

    // Releases model resources if no longer used.
    model.close()

    // Display the category with the maximum probability
    Text(text = "Max Probability Category: ${maxProbabilityCategory?.label}")
}

@Preview
@Composable
fun LensePreview() {
    LenseApp()
}