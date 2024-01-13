package com.example.lense

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.lense.ml.EnL4Fp32
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import org.tensorflow.lite.support.image.TensorImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            NavHost(navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(navController)
                }
                composable("main") {
                    LenseApp()
                }
            }
        }
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    val auth = Firebase.auth

    if (auth.currentUser != null) {
        navController.navigate("main")
    }

    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Lense",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 50.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(context, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
                        } else {
                            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    navController.navigate("main")
                                } else {
                                    // If sign in fails, display a message to the user.
                                    val message = "Incorrect email or password. Please check and try again."
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Log in")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(context, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
                        } else {
                            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // User is created
                                    val message = "Account created successfully. You can now log in."
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                } else {
                                    // If sign up fails, display a message to the user.
                                    val message = "Failed to create account. Please try again."
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign up")
                }
            }
        }
    }
}



@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn()
@Composable
fun LenseApp() {
    val navController = rememberNavController()
    val database = FirebaseDatabase.getInstance()
    val myRef = database.getReference("message")
    val context = LocalContext.current
    val model = EnL4Fp32.newInstance(context)
    val auth = Firebase.auth

    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NavHost(navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(navController)
                }
                composable("main") {
                    // Check if user is already logged in
                    if (auth.currentUser == null) {
                        navController.navigate("login")
                    } else {
                        Scaffold(
                            bottomBar = {
                                BottomNavigation(backgroundColor = Color(0xFF8BC34A)) {
                                    BottomNavigationItem(
                                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                                        label = { Text("Add Product") },
                                        selected = false,
                                        onClick = { navController.navigate("main") }
                                    )
                                    BottomNavigationItem(
                                        icon = { Icon(Icons.Default.List, contentDescription = null) },
                                        label = { Text("Display Data") },
                                        selected = false,
                                        onClick = { navController.navigate("display") }
                                    )
                                    BottomNavigationItem(
                                        icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                                        label = { Text("Logout") },
                                        selected = false,
                                        onClick = {
                                            auth.signOut()
                                            navController.navigate("login")
                                        }
                                    )
                                }
                            }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Welcome to Lense",
                                    style = MaterialTheme.typography.headlineLarge,
                                    modifier = Modifier.padding(16.dp)
                                )
                                AddToFirebaseButton(myRef, model)
                            }
                        }
                    }
                }
                composable("display") {
                    // Check if user is already logged in
                    if (auth.currentUser == null) {
                        navController.navigate("login")
                    } else {
                        Scaffold(
                            bottomBar = {
                                BottomNavigation(backgroundColor = Color(0xFF8BC34A)) {
                                    BottomNavigationItem(
                                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                                        label = { Text("Add Product") },
                                        selected = false,
                                        onClick = { navController.navigate("main") }
                                    )
                                    BottomNavigationItem(
                                        icon = { Icon(Icons.Default.List, contentDescription = null) },
                                        label = { Text("Display Data") },
                                        selected = false,
                                        onClick = { navController.navigate("display") }
                                    )
                                    BottomNavigationItem(
                                        icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                                        label = { Text("Logout") },
                                        selected = false,
                                        onClick = {
                                            auth.signOut()
                                            navController.navigate("login")
                                        }
                                    )
                                }
                            }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Product Posts",
                                    style = MaterialTheme.typography.headlineLarge,
                                    modifier = Modifier.padding(16.dp)
                                )
                                DisplayFirebaseData(myRef)
                            }
                        }
                    }
                }
            }
        }
    }
}

val API_URL = "https://api-inference.huggingface.co/models/nlpconnect/vit-gpt2-image-captioning"
val headers = mapOf("Authorization" to "Bearer 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")
val client = OkHttpClient()

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun AddToFirebaseButton(myRef: DatabaseReference, model: EnL4Fp32) {
    val context = LocalContext.current
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    val prediction = remember { mutableStateOf<String?>(null) }
    val correctPrediction = remember { mutableStateOf<String?>(null) }
    val showDialog = remember { mutableStateOf(false) }
    val price = remember { mutableStateOf<String?>(null) }
    val description = remember { mutableStateOf<String?>(null) }
    val correctDescription = remember { mutableStateOf<String?>(null) }

    //Pick image from local storage
    val imagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
        if (Build.VERSION.SDK_INT < 28) {
            bitmap.value = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
        } else {
            val source = it?.let { uri ->
                ImageDecoder.createSource(context.contentResolver, uri)
            } ?: throw IllegalArgumentException("Uri cannot be null")

            bitmap.value = ImageDecoder.decodeBitmap(source)
        }
    }

    Button(onClick = {
        imagePicker.launch("image/*")
    }) {
        Text("Pick an Image")
    }
    Spacer(modifier = Modifier.height(16.dp))
    // Capture image button
    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) {
        bitmap.value = it
    }

    Button(onClick = {
        cameraLauncher.launch(null)
    }) {
        Text("Capture an Image")
    }
    Spacer(modifier = Modifier.height(16.dp))

    bitmap.value?.let { bitmap ->
        if (bitmap != null) { // This line makes the TextField visible only when an image is selected
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.height(300.dp).width(300.dp), contentScale = ContentScale.FillBounds)
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = price.value ?: "",
                onValueChange = { price.value = it },
                label = { Text("Enter the selling price in ₹") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Classify the image
            prediction.value = classifyImage(bitmap, model)

            // Query the API for the description
            description.value = runBlocking { query(bitmap) }

            correctPrediction.value = prediction.value
            correctDescription.value = description.value

            // Show the dialog to confirm the prediction
            showDialog.value = true
        }) {
            Text("Add to Firebase")
        }

    }


    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Prediction") },
            text = { Text("If the predictions are not up to the mark, modify according to your needs.") },
            confirmButton = {
                TextField(
                    value = correctPrediction.value ?:"",
                    onValueChange = { correctPrediction.value = it },
                    label = { Text("Prediction") }
                )
                TextField(
                    value = correctDescription.value ?: "",
                    onValueChange = { correctDescription.value = it },
                    label = { Text("Description") }
                )
                Button(onClick = {
                    // Use the correct prediction from the user
                    prediction.value = correctPrediction.value
                    description.value = correctDescription.value

                    // Upload the image and correct prediction to Firebase Storage
                    uploadToFirebase(bitmap, myRef, prediction.value!!,price.value!!,description.value!!) { success ->
                        if (success) {
                            Toast.makeText(context, "Upload Successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to Upload", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showDialog.value = false
                }) {
                    Text("Submit")
                }
            }
        )
    }
}


fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

suspend fun query(bitmap: Bitmap): String? {
    val byteArray = bitmapToByteArray(bitmap)
    val requestBody = byteArray.toRequestBody()

    val request = Request.Builder()
        .url(API_URL)
        .headers(headers.toHeaders())
        .post(requestBody)
        .build()

    return withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            val jsonResponse = JSONArray(responseBody)
            val descriptionObject = jsonResponse.getJSONObject(0)
            descriptionObject.getString("generated_text")
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}



fun classifyImage(bitmap: Bitmap, model: EnL4Fp32): String {
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

    return maxProbabilityCategory?.label ?: ""
}


fun uploadToFirebase(bitmap: MutableState<Bitmap?>, myRef: DatabaseReference, prediction: String, price : String,description : String,callback: (Boolean) -> Unit) {
    val storageRef = Firebase.storage.reference
    val imageName = UUID.randomUUID().toString() // unique image name
    val imageRef = storageRef.child("images/$imageName")

    val baos = ByteArrayOutputStream()
    bitmap.value?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
    val imageData = baos.toByteArray()

    //image uploading part
    imageRef.putBytes(imageData).addOnSuccessListener { taskSnapshot ->
        taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
            val imageUrl = uri.toString()

            val price = price
            val description = description


            val key = myRef.push().key
            key?.let {
                myRef.child(it).setValue(
                    mapOf(
                        "price" to price,
                        "imageUrl" to imageUrl,
                        "prediction" to prediction,
                        "description" to description
                    )
                )
            }

            callback(true)
        }
    }.addOnFailureListener {
        callback(false)
    }
}


@Composable
fun DisplayFirebaseData(myRef: DatabaseReference) {
    val dataList = remember { mutableStateListOf<DataSnapshot>() }

    DisposableEffect(myRef) {
        val dataListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataList.clear()
                dataSnapshot.children.forEach { snapshot ->
                    dataList.add(snapshot)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }

        myRef.addValueEventListener(dataListener)

        onDispose {
            myRef.removeEventListener(dataListener)
        }
    }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(dataList) { snapshot ->
            val data = snapshot.getValue<Map<String, String>>()
            Spacer(modifier = Modifier.height(8.dp))
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 5.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(data?.get("imageUrl")),
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Category: ${data?.get("prediction") ?: ""}", fontWeight = FontWeight.Bold)
                            Text(text = "Description: ${data?.get("description")?: ""}", fontWeight = FontWeight.Normal)
                            Text(text = "Price: ₹${data?.get("price")?: ""}", fontWeight = FontWeight.Normal)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = {
                            myRef.child(snapshot.key!!).removeValue()
                        }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
        item{
            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}






