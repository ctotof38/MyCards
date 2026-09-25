package com.totof.mycards

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.zIndex
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.totof.mycards.ui.theme.MyCardsTheme

/**
 * Modèle de données pour une carte de fidélité.
 * imageName correspond au nom du fichier dans res/drawable (sans l'extension).
 */
data class LoyaltyCard(
    val id: Int,
    val name: String,
    val imageName: String,
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
 * Charge la liste des cartes depuis le stockage interne ou le fichier assets/cards.json
 */
fun loadCards(context: Context): List<LoyaltyCard> {
    val file = context.getFileStreamPath("cards_order.json")
    return try {
        val jsonString = if (file.exists()) {
            file.bufferedReader().use { it.readText() }
        } else {
            context.assets.open("cards.json").bufferedReader().use { it.readText() }
        }
        val listType = object : TypeToken<List<LoyaltyCard>>() {}.type
        Gson().fromJson(jsonString, listType)
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
 * Récupère l'ID de la ressource drawable à partir de son nom.
 */
@SuppressLint("LocalContextResourcesRead", "DiscouragedApi")
@Composable
fun getDrawableId(name: String): Int {
    val context = LocalContext.current
    return remember(name) {
        context.resources.getIdentifier(name, "drawable", context.packageName)
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
                            IconButton(onClick = { /* Action modifier */ }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Modifier",
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
        }
    ) { innerPadding ->
        if (selectedCard == null) {
            CardGrid(
                cards = cards,
                modifier = Modifier.padding(innerPadding),
                onOrderChanged = { saveCards(context, cards) }
            ) { card ->
                selectedCard = card
            }
        } else {
            CardDetail(card = selectedCard!!, modifier = Modifier.padding(innerPadding))
            BackHandler {
                selectedCard = null
            }
        }
    }
}

@Composable
fun CardGrid(
    cards: MutableList<LoyaltyCard>,
    modifier: Modifier = Modifier,
    onOrderChanged: () -> Unit,
    onCardClick: (LoyaltyCard) -> Unit
) {
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
                        // On cherche quel item est sous le doigt au début du long press
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
                        
                        // Trouver l'info de l'item déplacé
                        val draggedItemInfo = gridState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == draggedIndex } ?: return@detectDragGesturesAfterLongPress
                            
                        val currentCenterX = draggedItemInfo.offset.x + draggedItemInfo.size.width / 2 + draggingOffset.x
                        val currentCenterY = draggedItemInfo.offset.y + draggedItemInfo.size.height / 2 + draggingOffset.y
                        
                        // On cherche si le centre de l'item déplacé survole un autre item
                        val targetItem = gridState.layoutInfo.visibleItemsInfo
                            .find { item ->
                                item.index != draggedIndex &&
                                currentCenterX.toInt() in item.offset.x..item.offset.x + item.size.width &&
                                currentCenterY.toInt() in item.offset.y..item.offset.y + item.size.height
                            }
                            
                        if (targetItem != null) {
                            val targetIndex = targetItem.index
                            // Swap dans la liste
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
                    .animateItem() // Anime le déplacement des autres cartes
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

@Composable
fun CardItem(
    card: LoyaltyCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageRes = getDrawableId(card.imageName)
    
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
            if (imageRes != 0) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = card.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.58f),
                    contentScale = ContentScale.FillBounds
                )
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (card.code != null) {
            // Si un format est spécifié, on génère le code-barres
            if (card.format != null) {
                val barcodeBitmap = remember(card.code, card.format, card.isGs1) {
                    generateBarcode(card.code, card.format, card.isGs1)
                }
                barcodeBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Code généré pour ${card.name}",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            // On affiche toujours le texte du code
            Text(
                text = card.code,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                textAlign = TextAlign.Center
            )
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
