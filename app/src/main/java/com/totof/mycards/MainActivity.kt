package com.totof.mycards

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import java.io.File
import java.io.FileOutputStream

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
            "botanic" -> R.drawable.botanic
            "card_icon" -> R.drawable.card_icon
            "casino" -> R.drawable.casino
            "castorama" -> R.drawable.castorama
            "dafy" -> R.drawable.dafy
            "decatclub" -> R.drawable.decatclub
            "decathlon" -> R.drawable.decathlon
            "entrepot" -> R.drawable.entrepot
            "foire_fouille" -> R.drawable.foire_fouille
            "gammvert" -> R.drawable.gammvert
            "gaumont" -> R.drawable.gaumont
            "ikea" -> R.drawable.ikea
            "intermarche" -> R.drawable.intermarche
            "kiabi" -> R.drawable.kiabi
            "picard" -> R.drawable.picard
            "vertbaudet" -> R.drawable.vertbaudet
            "villaverde" -> R.drawable.villaverde
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardDialog(
    onDismiss: () -> Unit,
    onCardAdded: (LoyaltyCard) -> Unit
) {
    val context = LocalContext.current

    var cardName by remember { mutableStateOf("") }
    var cardBitmap by remember { mutableStateOf<Bitmap?>(null) }
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
                val stream = context.contentResolver.openInputStream(tempCameraUri!!)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bitmap != null) {
                    cardBitmap = bitmap
                    // Scan automatique de code-barres sur la photo prise
                    isScanning = true
                    scanBarcodeFromBitmap(bitmap) { code, format, detectedGs1 ->
                        isScanning = false
                        if (code != null) {
                            codeValue = code
                            if (format != null) selectedFormat = format
                            if (detectedGs1) isGs1 = true
                            Toast.makeText(context, "Code-barres détecté !", Toast.LENGTH_SHORT).show()
                        }
                    }
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
                val stream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bitmap != null) {
                    cardBitmap = bitmap
                    // Scan automatique du code-barres sur l'image sélectionnée
                    isScanning = true
                    scanBarcodeFromBitmap(bitmap) { code, format, detectedGs1 ->
                        isScanning = false
                        if (code != null) {
                            codeValue = code
                            if (format != null) selectedFormat = format
                            if (detectedGs1) isGs1 = true
                            Toast.makeText(context, "Code-barres détecté !", Toast.LENGTH_SHORT).show()
                        }
                    }
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
                val stream = context.contentResolver.openInputStream(tempCameraUri!!)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()
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
                val stream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()
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
                            } catch (e: Exception) {
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
                            } catch (e: Exception) {
                                Toast.makeText(context, "Impossible d'ouvrir la galerie", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Galerie")
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
                            } catch (e: Exception) {
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
                            } catch (e: Exception) {
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
