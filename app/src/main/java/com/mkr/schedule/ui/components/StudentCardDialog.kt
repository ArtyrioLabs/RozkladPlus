package com.mkr.schedule.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCardDialog(
    initialBarcode: String,
    initialCodeType: String = "BARCODE", // "BARCODE" or "QR"
    photoPath: String? = null,
    onSaveBarcode: (String) -> Unit,
    onSaveCodeType: (String) -> Unit,
    onSavePhoto: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var barcodeValue by remember { mutableStateOf(initialBarcode) }
    var codeType by remember { mutableStateOf(initialCodeType) }
    var isEditing by remember { mutableStateOf(initialBarcode.isEmpty()) }
    var showFullscreen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Copy selected image to internal storage
            val fileName = "student_card_photo_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                onSavePhoto(file.absolutePath)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    if (showFullscreen) {
        FullscreenCodeDialog(
            value = barcodeValue,
            type = codeType,
            onDismiss = { showFullscreen = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Пропуск",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                if (isEditing) {
                    OutlinedTextField(
                        value = barcodeValue,
                        onValueChange = { barcodeValue = it },
                        label = { Text("Номер пропуска") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Button(
                        onClick = {
                            isEditing = false
                            onSaveBarcode(barcodeValue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Зберегти")
                    }
                } else {
                    // Student card photo section
                    if (photoPath != null) {
                        val photoFile = File(photoPath)
                        if (photoFile.exists()) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = photoFile,
                                        contentDescription = "Фото пропуска",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { onSavePhoto(null) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Видалити", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Додати фото пропуска")
                        }
                    }

                    // Barcode / QR rendering section
                    if (barcodeValue.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val bitmap = generateCode(barcodeValue, codeType)
                            if (bitmap != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showFullscreen = true },
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                                ) {
                                    Box(modifier = Modifier.padding(16.dp)) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Код пропуска",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(if (codeType == "QR") 180.dp else 80.dp),
                                            contentScale = if (codeType == "QR") ContentScale.Fit else ContentScale.FillBounds
                                        )
                                        Icon(
                                            Icons.Default.Fullscreen,
                                            contentDescription = "Збільшити",
                                            tint = Color.Gray.copy(alpha = 0.5f),
                                            modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = barcodeValue,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp),
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        // Toggle Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isBarcode = codeType == "BARCODE"
                            FilterChip(
                                selected = isBarcode,
                                onClick = { codeType = "BARCODE"; onSaveCodeType("BARCODE") },
                                label = { Text("Штрихкод") },
                                leadingIcon = { Icon(Icons.Default.ViewHeadline, null, Modifier.size(18.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = !isBarcode,
                                onClick = { codeType = "QR"; onSaveCodeType("QR") },
                                label = { Text("QR-код") },
                                leadingIcon = { Icon(Icons.Default.QrCode, null, Modifier.size(18.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    TextButton(onClick = { isEditing = true }) {
                        Text("Змінити номер пропуска")
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenCodeDialog(
    value: String,
    type: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    
    // Temporarily request maximum screen brightness for optical barcode scanners
    DisposableEffect(Unit) {
        val params = activity?.window?.attributes
        val originalBrightness = params?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        params?.screenBrightness = 1.0f
        activity?.window?.attributes = params
        
        onDispose {
            val p = activity?.window?.attributes
            p?.screenBrightness = originalBrightness
            activity?.window?.attributes = p
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val bitmap = generateCode(value, type)
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Код на весь екран",
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(if (type == "QR") 300.dp else 120.dp),
                            contentScale = if (type == "QR") ContentScale.Fit else ContentScale.FillBounds
                        )
                    }
                    Text(
                        text = value,
                        color = Color.Black,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 32.dp),
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = "Натисніть, щоб закрити",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

private fun generateCode(value: String, type: String): Bitmap? {
    if (value.isEmpty()) return null
    return try {
        val format = if (type == "QR") BarcodeFormat.QR_CODE else BarcodeFormat.CODE_128
        val width = if (type == "QR") 512 else 600
        val height = if (type == "QR") 512 else 200
        
        val bitMatrix = MultiFormatWriter().encode(value, format, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
