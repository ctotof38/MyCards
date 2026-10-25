package com.totof.mycards

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.totof.mycards.ui.theme.MyCardsTheme
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.hypot

/**
 * Modèle de données pour une carte de fidélité.
 */
data class LoyaltyCard(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val imageName: String? = null,
    val imagePath: String? = null,
    val code: String? = null,
    val format: BarcodeFormat? = null,
    val isGs1: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyCardsTheme {
                MyCardsApp()
            }
        }
    }
}

/**
 * Charge la liste des cartes depuis le stockage interne (cards_order.json).
 * Si le fichier n'existe pas, retourne une liste vide.
 */
fun loadCards(context: Context): List<LoyaltyCard> {
    val file = context.getFileStreamPath("cards_order.json")
    return try {
        if (file.exists()) {
            val jsonString = file.bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<LoyaltyCard>>() {}.type
            Gson().fromJson(jsonString, listType) ?: emptyList()
        } else {
            emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Sauvegarde la liste des cartes dans le stockage interne.
 */
fun saveCards(context: Context, cards: List<LoyaltyCard>) {
    try {
        val jsonString = Gson().toJson(cards)
        context.openFileOutput("cards_order.json", Context.MODE_PRIVATE).use {
            it.write(jsonString.toByteArray())
        }
    } catch (_: Exception) {
    }
}

/**
 * Exporte l'ensemble des cartes et de leurs images sous la forme d'une archive ZIP.
 */
fun exportBackup(context: Context, destUri: Uri): Boolean {
    return try {
        val cards = loadCards(context)
        val preparedCards = cards.map { card ->
            if (!card.imagePath.isNullOrEmpty()) {
                val file = File(card.imagePath)
                if (file.exists()) {
                    card.copy(imagePath = "images/${file.name}")
                } else {
                    card.copy(imagePath = null)
                }
            } else {
                card
            }
        }

        context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
            ZipOutputStream(outputStream).use { zipOut ->
                val jsonString = Gson().toJson(preparedCards)
                val jsonEntry = ZipEntry("cards_data.json")
                zipOut.putNextEntry(jsonEntry)
                zipOut.write(jsonString.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                cards.forEach { card ->
                    if (!card.imagePath.isNullOrEmpty()) {
                        val imgFile = File(card.imagePath)
                        if (imgFile.exists()) {
                            val imgEntry = ZipEntry("images/${imgFile.name}")
                            zipOut.putNextEntry(imgEntry)
                            imgFile.inputStream().use { input ->
                                input.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    }
                }
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/**
 * Importe une sauvegarde ZIP, restaure les images et retourne la nouvelle liste de cartes.
 */
fun importBackup(context: Context, sourceUri: Uri): List<LoyaltyCard>? {
    return try {
        var jsonContent: String? = null
        val imagesDir = File(context.filesDir, "card_images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val entryName = entry.name.replace("\\", "/")
                    if (entryName.endsWith("cards_data.json") || entryName.endsWith("cards_order.json") || entryName.endsWith("cards.json")) {
                        val baos = ByteArrayOutputStream()
                        zipIn.copyTo(baos)
                        jsonContent = baos.toString("UTF-8")
                    } else if (!entry.isDirectory && (entryName.startsWith("images/") || entryName.endsWith(".jpg") || entryName.endsWith(".png"))) {
                        val fileName = File(entryName).name
                        val targetFile = File(imagesDir, fileName)
                        FileOutputStream(targetFile).use { out ->
                            zipIn.copyTo(out)
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        }

        if (jsonContent != null) {
            val listType = object : TypeToken<List<LoyaltyCard>>() {}.type
            val importedList: List<LoyaltyCard> = Gson().fromJson(jsonContent, listType) ?: emptyList()

            val restoredList = importedList.map { card ->
                if (!card.imagePath.isNullOrEmpty()) {
                    val fileName = File(card.imagePath).name
                    val localFile = File(imagesDir, fileName)
                    if (localFile.exists()) {
                        card.copy(imagePath = localFile.absolutePath)
                    } else {
                        card.copy(imagePath = null)
                    }
                } else {
                    card
                }
            }

            saveCards(context, restoredList)
            restoredList
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Sauvegarde un Bitmap dans le répertoire interne de l'application.
 */
fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
    return try {
        val imagesDir = File(context.filesDir, "card_images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val file = File(imagesDir, "card_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    } catch (_: Exception) {
        null
    }
}

/**
 * Supprime le fichier image associé à une carte.
 */
fun deleteCardImage(imagePath: String?) {
    if (!imagePath.isNullOrEmpty()) {
        try {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }
}

/**
 * Pivote un Bitmap d'un angle donné en degrés.
 */
fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
    if (degrees == 0f) return source
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

/**
 * Lit une image depuis un Uri et applique la rotation EXIF appropriée.
 */
fun decodeAndRotateBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream1 = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream1)
        inputStream1.close()

        if (bitmap == null) return null

        var rotationDegrees = 0f
        try {
            val inputStream2 = context.contentResolver.openInputStream(uri)
            if (inputStream2 != null) {
                val exifInterface = ExifInterface(inputStream2)
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                inputStream2.close()

                rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        } catch (_: Exception) {
        }

        if (rotationDegrees != 0f) {
            rotateBitmap(bitmap, rotationDegrees)
        } else {
            bitmap
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Crée un URI temporaire dans le cache pour la capture photo avec la caméra.
 */
fun createTempImageUri(context: Context): Uri? {
    return try {
        val cacheDir = File(context.cacheDir, "images")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val file = File(cacheDir, "temp_capture_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(
            context.applicationContext,
            "${context.applicationContext.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Nettoie la valeur d'un code-barres lu (en supprimant par exemple le préfixe symbo ISO/IEC 15424 comme ']C1')
 * et détecte si le code-barres est au format GS1.
 */
fun cleanBarcodeValue(raw: String?): Pair<String, Boolean> {
    if (raw.isNullOrBlank()) return Pair("", false)
    var cleaned = raw.trim()
    var isGs1 = false
    if (cleaned.startsWith("]C1")) {
        cleaned = cleaned.substring(3)
        isGs1 = true
    } else if (cleaned.matches(Regex("^][A-Za-z0-9]{2}.*"))) {
        cleaned = cleaned.substring(3)
    }
    cleaned = cleaned.removePrefix("\u00f1")
    return Pair(cleaned, isGs1)
}

/**
 * Analyse un Bitmap à la recherche d'un code-barres avec ML Kit.
 */
fun scanBarcodeFromBitmap(
    bitmap: Bitmap,
    onResult: (code: String?, format: BarcodeFormat?, isGs1: Boolean) -> Unit
) {
    try {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        val scanner = BarcodeScanning.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val barcode = barcodes.first()
                    val rawValue = barcode.rawValue ?: barcode.displayValue
                    val (cleanedCode, isGs1) = cleanBarcodeValue(rawValue)
                    val format = mapMlKitFormatToZXing(barcode.format)
                    onResult(cleanedCode, format, isGs1)
                } else {
                    onResult(null, null, false)
                }
            }
            .addOnFailureListener {
                onResult(null, null, false)
            }
    } catch (e: Exception) {
        e.printStackTrace()
        onResult(null, null, false)
    }
}

/**
 * Mappe le format entier ML Kit vers un BarcodeFormat ZXing.
 */
fun mapMlKitFormatToZXing(mlKitFormat: Int): BarcodeFormat {
    return when (mlKitFormat) {
        Barcode.FORMAT_CODE_128 -> BarcodeFormat.CODE_128
        Barcode.FORMAT_CODE_39 -> BarcodeFormat.CODE_39
        Barcode.FORMAT_CODE_93 -> BarcodeFormat.CODE_93
        Barcode.FORMAT_EAN_13 -> BarcodeFormat.EAN_13
        Barcode.FORMAT_EAN_8 -> BarcodeFormat.EAN_8
        Barcode.FORMAT_UPC_A -> BarcodeFormat.UPC_A
        Barcode.FORMAT_UPC_E -> BarcodeFormat.UPC_E
        Barcode.FORMAT_QR_CODE -> BarcodeFormat.QR_CODE
        Barcode.FORMAT_DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
        Barcode.FORMAT_PDF417 -> BarcodeFormat.PDF_417
        Barcode.FORMAT_ITF -> BarcodeFormat.ITF
        Barcode.FORMAT_AZTEC -> BarcodeFormat.AZTEC
        else -> BarcodeFormat.CODE_128
    }
}

/**
 * Récupère l'ID de la ressource drawable à partir de son nom.
 */
@SuppressLint("LocalContextResourcesRead", "DiscouragedApi")
@Composable
fun getDrawableId(name: String?): Int {
    if (name.isNullOrEmpty()) return 0
    val context = LocalContext.current
    val cleanName = name.substringBeforeLast(".")
    return remember(cleanName) {
        val resId = when (cleanName) {
            "bandeau" -> R.drawable.bandeau
            "card_icon" -> R.drawable.card_icon
            else -> 0
        }
        if (resId != 0) resId else context.resources.getIdentifier(cleanName, "drawable", context.packageName)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCardsApp() {
    val context = LocalContext.current
    val cards = remember {
        val loaded = loadCards(context)
        mutableStateListOf<LoyaltyCard>().apply { addAll(loaded) }
    }
    var selectedCard by remember { mutableStateOf<LoyaltyCard?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            val success = exportBackup(context, uri)
            if (success) {
                Toast.makeText(context, "Sauvegarde exportée avec succès !", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Erreur lors de l'exportation", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val restored = importBackup(context, uri)
            if (restored != null) {
                cards.clear()
                cards.addAll(restored)
                Toast.makeText(context, "Données importées (${restored.size} carte(s))", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Fichier de sauvegarde invalide", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            Box {
                Image(
                    painter = painterResource(id = R.drawable.bandeau),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                TopAppBar(
                    title = {
                        Text(
                            selectedCard?.name ?: "Mes Cartes",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (selectedCard != null) {
                            IconButton(onClick = { selectedCard = null }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Retour",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    actions = {
                        if (selectedCard != null) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = Color.White
                                )
                            }
                        } else {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Exporter les cartes") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                                        exportLauncher.launch("MyCards_Sauvegarde_$dateStr.zip")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Importer des cartes") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedCard == null) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ajouter une carte"
                    )
                }
            }
        }
    ) { innerPadding ->
        if (selectedCard == null) {
            CardGrid(
                cards = cards,
                modifier = Modifier.padding(innerPadding),
                onOrderChanged = { saveCards(context, cards) },
                onCardClick = { card -> selectedCard = card },
                onAddCardClick = { showAddDialog = true }
            )
        } else {
            CardDetail(card = selectedCard!!, modifier = Modifier.padding(innerPadding))
            BackHandler {
                selectedCard = null
            }
        }

        if (showAddDialog) {
            AddCardDialog(
                onDismiss = { showAddDialog = false },
                onCardAdded = { newCard ->
                    cards.add(newCard)
                    saveCards(context, cards)
                    showAddDialog = false
                }
            )
        }

        if (showDeleteDialog && selectedCard != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Supprimer la carte") },
                text = { Text("Voulez-vous vraiment supprimer la carte \"${selectedCard?.name}\" ?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val cardToDelete = selectedCard
                            if (cardToDelete != null) {
                                cards.remove(cardToDelete)
                                deleteCardImage(cardToDelete.imagePath)
                                saveCards(context, cards)
                                selectedCard = null
                            }
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Supprimer", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}

@Composable
fun CardGrid(
    cards: MutableList<LoyaltyCard>,
    modifier: Modifier = Modifier,
    onOrderChanged: () -> Unit,
    onCardClick: (LoyaltyCard) -> Unit,
    onAddCardClick: () -> Unit
) {
    if (cards.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Aucune carte enregistrée",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Appuyez sur le bouton + ci-dessous pour ajouter votre première carte de fidélité.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onAddCardClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter une carte")
                }
            }
        }
    } else {
        val gridState = rememberLazyGridState()
        var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
        var draggingOffset by remember { mutableStateOf(Offset.Zero) }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            gridState.layoutInfo.visibleItemsInfo
                                .firstOrNull { item ->
                                    offset.x.toInt() in item.offset.x..item.offset.x + item.size.width &&
                                            offset.y.toInt() in item.offset.y..item.offset.y + item.size.height
                                }
                                ?.let { item ->
                                    draggedItemIndex = item.index
                                }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            draggingOffset += dragAmount

                            val draggedIndex = draggedItemIndex ?: return@detectDragGesturesAfterLongPress

                            val draggedItemInfo = gridState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.index == draggedIndex } ?: return@detectDragGesturesAfterLongPress

                            val currentCenterX = draggedItemInfo.offset.x + draggedItemInfo.size.width / 2 + draggingOffset.x
                            val currentCenterY = draggedItemInfo.offset.y + draggedItemInfo.size.height / 2 + draggingOffset.y

                            val targetItem = gridState.layoutInfo.visibleItemsInfo
                                .find { item ->
                                    item.index != draggedIndex &&
                                            currentCenterX.toInt() in item.offset.x..item.offset.x + item.size.width &&
                                            currentCenterY.toInt() in item.offset.y..item.offset.y + item.size.height
                                }

                            if (targetItem != null) {
                                val targetIndex = targetItem.index
                                cards.add(targetIndex, cards.removeAt(draggedIndex))
                                draggedItemIndex = targetIndex
                                draggingOffset = Offset.Zero
                                onOrderChanged()
                            }
                        },
                        onDragEnd = {
                            draggedItemIndex = null
                            draggingOffset = Offset.Zero
                        },
                        onDragCancel = {
                            draggedItemIndex = null
                            draggingOffset = Offset.Zero
                        }
                    )
                }
        ) {
            items(cards, key = { it.id }) { card ->
                val index = cards.indexOf(card)
                val isDragging = index == draggedItemIndex

                CardItem(
                    card = card,
                    onClick = { onCardClick(card) },
                    modifier = Modifier
                        .animateItem()
                        .graphicsLayer {
                            if (isDragging) {
                                translationX = draggingOffset.x
                                translationY = draggingOffset.y
                                scaleX = 1.1f
                                scaleY = 1.1f
                                alpha = 0.8f
                            }
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                )
            }
        }
    }
}

@Composable
fun CardItem(
    card: LoyaltyCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(card.imagePath) {
        card.imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }
    }
    val resId = getDrawableId(card.imageName)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            when {
                bitmap != null -> {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = card.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.58f),
                        contentScale = ContentScale.Crop
                    )
                }
                resId != 0 -> {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = card.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.58f),
                        contentScale = ContentScale.FillBounds
                    )
                }
                else -> {
                    Image(
                        painter = painterResource(id = R.drawable.card_icon),
                        contentDescription = card.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.58f),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
        Text(
            text = card.name,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CardDetail(card: LoyaltyCard, modifier: Modifier = Modifier) {
    val bitmap = remember(card.imagePath) {
        card.imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }
    }
    val resId = getDrawableId(card.imageName)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Affichage de l'image de la carte si disponible
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(bottom = 24.dp)
        ) {
            when {
                bitmap != null -> {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = card.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.58f),
                        contentScale = ContentScale.Crop
                    )
                }
                resId != 0 -> {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = card.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.58f),
                        contentScale = ContentScale.FillBounds
                    )
                }
                else -> {
                    Image(
                        painter = painterResource(id = R.drawable.card_icon),
                        contentDescription = card.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.58f)
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        if (card.code != null) {
            if (card.format != null) {
                val barcodeBitmap = remember(card.code, card.format, card.isGs1) {
                    generateBarcode(card.code, card.format, card.isGs1)
                }
                barcodeBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Code-barres pour ${card.name}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            val fontSize = when {
                card.code.length > 18 -> 15.sp
                card.code.length > 14 -> 18.sp
                card.code.length > 10 -> 22.sp
                else -> 26.sp
            }

            Text(
                text = card.code,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    letterSpacing = 1.sp
                ),
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Dialogue de recadrage interactif pour détourer précisément la carte sur la photo.
 */
@Composable
fun CropDialog(
    rawBitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropped: (Bitmap) -> Unit
) {
    var currentBitmap by remember(rawBitmap) { mutableStateOf(rawBitmap) }

    var left by remember { mutableFloatStateOf(0.05f) }
    var top by remember { mutableFloatStateOf(0.15f) }
    var right by remember { mutableFloatStateOf(0.95f) }
    var bottom by remember { mutableFloatStateOf(0.85f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = Color.Black
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recadrer la carte",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val imageBitmap = remember(currentBitmap) { currentBitmap.asImageBitmap() }

                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Image à recadrer",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val containerWidth = constraints.maxWidth.toFloat()
                        val containerHeight = constraints.maxHeight.toFloat()

                        val imageAspect = currentBitmap.width.toFloat() / currentBitmap.height.toFloat()
                        val containerAspect = containerWidth / containerHeight

                        val displayedWidth: Float
                        val displayedHeight: Float
                        val offsetX: Float
                        val offsetY: Float

                        if (imageAspect > containerAspect) {
                            displayedWidth = containerWidth
                            displayedHeight = containerWidth / imageAspect
                            offsetX = 0f
                            offsetY = (containerHeight - displayedHeight) / 2f
                        } else {
                            displayedHeight = containerHeight
                            displayedWidth = containerHeight * imageAspect
                            offsetX = (containerWidth - displayedWidth) / 2f
                            offsetY = 0f
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(displayedWidth, displayedHeight) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()

                                        val dx = dragAmount.x / displayedWidth
                                        val dy = dragAmount.y / displayedHeight

                                        val touchX = (change.position.x - offsetX) / displayedWidth
                                        val touchY = (change.position.y - offsetY) / displayedHeight

                                        val distTL = hypot(touchX - left, touchY - top)
                                        val distTR = hypot(touchX - right, touchY - top)
                                        val distBL = hypot(touchX - left, touchY - bottom)
                                        val distBR = hypot(touchX - right, touchY - bottom)

                                        val minDist = minOf(distTL, distTR, distBL, distBR)

                                        if (minDist < 0.25f) {
                                            when (minDist) {
                                                distTL -> {
                                                    left = (left + dx).coerceIn(0f, right - 0.1f)
                                                    top = (top + dy).coerceIn(0f, bottom - 0.1f)
                                                }
                                                distTR -> {
                                                    right = (right + dx).coerceIn(left + 0.1f, 1f)
                                                    top = (top + dy).coerceIn(0f, bottom - 0.1f)
                                                }
                                                distBL -> {
                                                    left = (left + dx).coerceIn(0f, right - 0.1f)
                                                    bottom = (bottom + dy).coerceIn(top + 0.1f, 1f)
                                                }
                                                distBR -> {
                                                    right = (right + dx).coerceIn(left + 0.1f, 1f)
                                                    bottom = (bottom + dy).coerceIn(top + 0.1f, 1f)
                                                }
                                            }
                                        } else {
                                            val w = right - left
                                            val h = bottom - top

                                            var newLeft = left + dx
                                            var newTop = top + dy

                                            if (newLeft < 0f) newLeft = 0f
                                            if (newLeft + w > 1f) newLeft = 1f - w
                                            if (newTop < 0f) newTop = 0f
                                            if (newTop + h > 1f) newTop = 1f - h

                                            left = newLeft
                                            right = newLeft + w
                                            top = newTop
                                            bottom = newTop + h
                                        }
                                    }
                                }
                        ) {
                            val rectPxLeft = offsetX + left * displayedWidth
                            val rectPxTop = offsetY + top * displayedHeight
                            val rectPxRight = offsetX + right * displayedWidth
                            val rectPxBottom = offsetY + bottom * displayedHeight

                            val overlayColor = Color.Black.copy(alpha = 0.6f)
                            drawRect(
                                color = overlayColor,
                                topLeft = Offset(offsetX, offsetY),
                                size = Size(displayedWidth, (rectPxTop - offsetY).coerceAtLeast(0f))
                            )
                            drawRect(
                                color = overlayColor,
                                topLeft = Offset(offsetX, rectPxBottom),
                                size = Size(displayedWidth, (offsetY + displayedHeight - rectPxBottom).coerceAtLeast(0f))
                            )
                            drawRect(
                                color = overlayColor,
                                topLeft = Offset(offsetX, rectPxTop),
                                size = Size((rectPxLeft - offsetX).coerceAtLeast(0f), rectPxBottom - rectPxTop)
                            )
                            drawRect(
                                color = overlayColor,
                                topLeft = Offset(rectPxRight, rectPxTop),
                                size = Size((offsetX + displayedWidth - rectPxRight).coerceAtLeast(0f), rectPxBottom - rectPxTop)
                            )

                            drawRect(
                                color = Color.White,
                                topLeft = Offset(rectPxLeft, rectPxTop),
                                size = Size(rectPxRight - rectPxLeft, rectPxBottom - rectPxTop),
                                style = Stroke(width = 3.dp.toPx())
                            )

                            val w = rectPxRight - rectPxLeft
                            val h = rectPxBottom - rectPxTop
                            drawLine(Color.White.copy(alpha = 0.4f), Offset(rectPxLeft + w / 3, rectPxTop), Offset(rectPxLeft + w / 3, rectPxBottom))
                            drawLine(Color.White.copy(alpha = 0.4f), Offset(rectPxLeft + 2 * w / 3, rectPxTop), Offset(rectPxLeft + 2 * w / 3, rectPxBottom))
                            drawLine(Color.White.copy(alpha = 0.4f), Offset(rectPxLeft, rectPxTop + h / 3), Offset(rectPxRight, rectPxTop + h / 3))
                            drawLine(Color.White.copy(alpha = 0.4f), Offset(rectPxLeft, rectPxTop + 2 * h / 3), Offset(rectPxRight, rectPxTop + 2 * h / 3))

                            val handleRadius = 12.dp.toPx()
                            drawCircle(Color.White, handleRadius, Offset(rectPxLeft, rectPxTop))
                            drawCircle(Color.White, handleRadius, Offset(rectPxRight, rectPxTop))
                            drawCircle(Color.White, handleRadius, Offset(rectPxLeft, rectPxBottom))
                            drawCircle(Color.White, handleRadius, Offset(rectPxRight, rectPxBottom))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            left = 0f
                            top = 0f
                            right = 1f
                            bottom = 1f
                        }
                    ) {
                        Text("Tout", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            val isLandscape = currentBitmap.width >= currentBitmap.height
                            val targetRatio = if (isLandscape) 1.58f else (1f / 1.58f)
                            val centerX = (left + right) / 2f
                            val centerY = (top + bottom) / 2f

                            val newH = 0.6f
                            val newW = (newH * currentBitmap.height * targetRatio) / currentBitmap.width

                            left = (centerX - newW / 2f).coerceIn(0f, (1f - newW).coerceAtLeast(0f))
                            right = (left + newW).coerceIn(0f, 1f)
                            top = (centerY - newH / 2f).coerceIn(0f, (1f - newH).coerceAtLeast(0f))
                            bottom = (top + newH).coerceIn(0f, 1f)
                        }
                    ) {
                        Text("Format Carte", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            currentBitmap = rotateBitmap(currentBitmap, 90f)
                            left = 0.05f
                            top = 0.15f
                            right = 0.95f
                            bottom = 0.85f
                        }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pivoter", color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            try {
                                val pX = (left * currentBitmap.width).toInt().coerceIn(0, currentBitmap.width - 1)
                                val pY = (top * currentBitmap.height).toInt().coerceIn(0, currentBitmap.height - 1)
                                val pW = ((right - left) * currentBitmap.width).toInt().coerceIn(1, currentBitmap.width - pX)
                                val pH = ((bottom - top) * currentBitmap.height).toInt().coerceIn(1, currentBitmap.height - pY)

                                val cropped = Bitmap.createBitmap(currentBitmap, pX, pY, pW, pH)
                                onCropped(cropped)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Crop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Valider")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardDialog(
    onDismiss: () -> Unit,
    onCardAdded: (LoyaltyCard) -> Unit
) {
    val context = LocalContext.current

    var cardName by remember { mutableStateOf("") }
    var cardBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rawCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }

    var codeValue by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(BarcodeFormat.EAN_13) }
    var isGs1 by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val formatsList = listOf(
        BarcodeFormat.EAN_13,
        BarcodeFormat.CODE_128,
        BarcodeFormat.CODE_39,
        BarcodeFormat.UPC_A,
        BarcodeFormat.QR_CODE,
        BarcodeFormat.EAN_8,
        BarcodeFormat.CODE_93
    )
    var formatDropdownExpanded by remember { mutableStateOf(false) }

    // Launcher pour la caméra (carte ou code-barres)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            try {
                val bitmap = decodeAndRotateBitmapFromUri(context, tempCameraUri!!)
                if (bitmap != null) {
                    rawCapturedBitmap = bitmap
                    showCropDialog = true
                }
            } catch (_: Exception) {
            }
        }
    }

    // Launcher pour la galerie
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = decodeAndRotateBitmapFromUri(context, uri)
                if (bitmap != null) {
                    rawCapturedBitmap = bitmap
                    showCropDialog = true
                }
            } catch (_: Exception) {
            }
        }
    }

    // Launcher dédié pour photographier uniquement le code-barres
    val barcodeCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            try {
                val bitmap = decodeAndRotateBitmapFromUri(context, tempCameraUri!!)
                if (bitmap != null) {
                    isScanning = true
                    scanBarcodeFromBitmap(bitmap) { code, format, detectedGs1 ->
                        isScanning = false
                        if (code != null) {
                            codeValue = code
                            if (format != null) selectedFormat = format
                            if (detectedGs1) isGs1 = true
                            Toast.makeText(context, "Code-barres détecté : $code", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Aucun code-barres trouvé sur la photo", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    // Launcher dédié pour lire un code-barres depuis une image de la galerie
    val barcodeGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = decodeAndRotateBitmapFromUri(context, uri)
                if (bitmap != null) {
                    isScanning = true
                    scanBarcodeFromBitmap(bitmap) { code, format, detectedGs1 ->
                        isScanning = false
                        if (code != null) {
                            codeValue = code
                            if (format != null) selectedFormat = format
                            if (detectedGs1) isGs1 = true
                            Toast.makeText(context, "Code-barres détecté : $code", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Aucun code-barres trouvé dans cette image", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Erreur lors de la lecture de l'image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showCropDialog && rawCapturedBitmap != null) {
        CropDialog(
            rawBitmap = rawCapturedBitmap!!,
            onDismiss = { showCropDialog = false },
            onCropped = { cropped ->
                cardBitmap = cropped
                showCropDialog = false
                isScanning = true
                scanBarcodeFromBitmap(cropped) { code, format, detectedGs1 ->
                    isScanning = false
                    if (code != null) {
                        codeValue = code
                        if (format != null) selectedFormat = format
                        if (detectedGs1) isGs1 = true
                        Toast.makeText(context, "Code-barres détecté !", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nouvelle Carte",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Champ Nom de la carte
                OutlinedTextField(
                    value = cardName,
                    onValueChange = { cardName = it },
                    label = { Text("Nom de la carte (ex: Carrefour)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Aperçu / Choix de la photo
                Text(
                    text = "Photo de la carte",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (cardBitmap != null) {
                        Image(
                            bitmap = cardBitmap!!.asImageBitmap(),
                            contentDescription = "Aperçu photo carte",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                "Prendre une photo ou choisir dans la galerie",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val uri = createTempImageUri(context)
                                if (uri != null) {
                                    tempCameraUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    Toast.makeText(context, "Erreur d'initialisation de la caméra", Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                                Toast.makeText(context, "Impossible d'ouvrir l'appareil photo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Photo")
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                galleryLauncher.launch("image/*")
                            } catch (_: Exception) {
                                Toast.makeText(context, "Impossible d'ouvrir la galerie", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Galerie")
                    }

                    if (cardBitmap != null || rawCapturedBitmap != null) {
                        OutlinedButton(
                            onClick = {
                                if (rawCapturedBitmap == null) {
                                    rawCapturedBitmap = cardBitmap
                                }
                                showCropDialog = true
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recadrer")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section Code-barres
                Text(
                    text = "Code-barres",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val uri = createTempImageUri(context)
                                if (uri != null) {
                                    tempCameraUri = uri
                                    barcodeCameraLauncher.launch(uri)
                                } else {
                                    Toast.makeText(context, "Erreur d'initialisation de la caméra", Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                                Toast.makeText(context, "Impossible d'ouvrir l'appareil photo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Caméra")
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                barcodeGalleryLauncher.launch("image/*")
                            } catch (_: Exception) {
                                Toast.makeText(context, "Impossible d'ouvrir la galerie", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Galerie")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = codeValue,
                    onValueChange = { input ->
                        val (cleaned, detectedGs1) = cleanBarcodeValue(input)
                        codeValue = cleaned
                        if (detectedGs1) isGs1 = true
                    },
                    label = { Text("Numéro / Valeur du code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Selecteur de Format de code-barres
                ExposedDropdownMenuBox(
                    expanded = formatDropdownExpanded,
                    onExpandedChange = { formatDropdownExpanded = !formatDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedFormat.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Format du code-barres") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = formatDropdownExpanded,
                        onDismissRequest = { formatDropdownExpanded = false }
                    ) {
                        formatsList.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format.name) },
                                onClick = {
                                    selectedFormat = format
                                    formatDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Aperçu du code-barres généré s'il y a un code
                if (codeValue.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val previewBitmap = remember(codeValue, selectedFormat, isGs1) {
                        generateBarcode(codeValue, selectedFormat, isGs1)
                    }
                    previewBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Aperçu code-barres",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Boutons d'action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = cardName.isNotBlank(),
                        onClick = {
                            val savedPath = cardBitmap?.let { saveBitmapToInternalStorage(context, it) }
                            val newCard = LoyaltyCard(
                                id = System.currentTimeMillis(),
                                name = cardName.trim(),
                                imagePath = savedPath,
                                code = codeValue.ifBlank { null },
                                format = selectedFormat,
                                isGs1 = isGs1
                            )
                            onCardAdded(newCard)
                        }
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

/**
 * Génère un bitmap de code-barres à partir d'un contenu, d'un format et du flag GS1.
 */
fun generateBarcode(content: String, format: BarcodeFormat?, isGs1: Boolean): Bitmap? {
    if (format == null) return null
    return try {
        val barcodeEncoder = BarcodeEncoder()
        val cleanedContent = content.replace(" ", "")

        val hints = mutableMapOf<EncodeHintType, Any>()

        var finalContent = cleanedContent

        if (isGs1 && format == BarcodeFormat.CODE_128) {
            hints[EncodeHintType.GS1_FORMAT] = true
            if (!cleanedContent.startsWith("\u00f1")) {
                finalContent = "\u00f1$cleanedContent"
            }
        }

        barcodeEncoder.encodeBitmap(finalContent, format, 800, 400, hints)
    } catch (_: Exception) {
        null
    }
}
