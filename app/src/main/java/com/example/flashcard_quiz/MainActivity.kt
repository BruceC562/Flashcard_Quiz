package com.example.flashcard_quiz

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flashcard_quiz.ui.theme.Flashcard_QuizTheme
import kotlinx.coroutines.delay
import org.xmlpull.v1.XmlPullParser

data class Flashcard(val question: String, val answer: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableEdgeToEdge()
        setContent {
            Flashcard_QuizTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.LightGray) { innerPadding ->
                    FlashcardQuiz(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Loads flashcards from the flashcards.xml file
fun loadFlashcards(xmlFile: Int, context: Context): List<Flashcard> {
    val flashcards = mutableListOf<Flashcard>()
    val parser = context.resources.getXml(xmlFile)
    var eventType = parser.eventType
    var question = ""
    var answer = ""

    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.START_TAG -> {
                when (parser.name) {
                    "question" -> { question = parser.nextText() }
                    "answer" -> { answer = parser.nextText() }
                }
            }
            XmlPullParser.END_TAG -> {
                when (parser.name) {
                    "card" -> { flashcards.add(Flashcard(question, answer)) }
                }
            }
        }
        eventType = parser.next()
    }

    return flashcards
}

@Composable
fun FlashcardQuiz(modifier: Modifier = Modifier) {
    val flashcards = loadFlashcards(R.xml.flashcards, LocalContext.current)
    var shuffledFlashcards by remember { mutableStateOf(flashcards.shuffled()) }
    //Shuffles the flashcards every 15 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000)
            println("Shuffling flashcards...")
            shuffledFlashcards = shuffledFlashcards.shuffled()
        }
    }

    LazyRow(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center
    ) {
        items(shuffledFlashcards) { flashcard ->
            Box(
                modifier = Modifier
                    .fillParentMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CreateFlashcard(flashcard)
            }
        }
    }
}

/*
* Rotating card based on Medium article:
* https://fvilarino.medium.com/creating-a-rotating-card-in-jetpack-compose-ba94c7dd76fb
* */

// Represents which side the card is facing
enum class FlashcardState (val angle: Float) {
    Front(0f) {
        override val next: FlashcardState
            get() = Back
    },
    Back(180f) {
        override val next: FlashcardState
            get() = Front
    };

    abstract val next: FlashcardState
}

@Composable
fun CreateFlashcard(flashcard: Flashcard) {
    var cardFace by remember { mutableStateOf(FlashcardState.Front) }

    FlipCard(
        cardFace = cardFace,
        onClick = { cardFace = cardFace.next },
        front = { BuildFace(flashcard.question) },
        back = { BuildFace(flashcard.answer, flipped = true) }
    )
}

@Composable
fun FlipCard(
    cardFace: FlashcardState,
    onClick: (FlashcardState) -> Unit,
    back: @Composable () -> Unit,
    front: @Composable () -> Unit
) {
    val rotation = animateFloatAsState(
        targetValue = cardFace.angle,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing)
    )

    Box(
        modifier = Modifier
            .fillMaxSize() // Ensures uniform size
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            onClick = { onClick(cardFace) },
            modifier = Modifier
                .fillMaxWidth(0.95f) // Ensure same width
                .fillMaxHeight(1f) // Ensure same height
                .graphicsLayer {
                    rotationX = rotation.value
                    cameraDistance = 12f * density
                }
                .clip(RoundedCornerShape(10.dp)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (rotation.value <= 90f) {
                    front()
                } else {
                    back()
                }
            }
        }
    }
}

@Composable
fun BuildFace(text: String, flipped: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationX = if (flipped) 180f else 0f // Flips the text if an answer is showing
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 35.sp,
            textAlign = TextAlign.Center,
            softWrap = true,
            lineHeight = 40.sp,
            maxLines = Int.MAX_VALUE,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 5.dp, bottom = 5.dp) // Ensures consistent text area size
        )
    }
}

@Preview(
    showSystemUi = true,
    device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape"
)
@Composable
fun FlashcardPreview() {
    Flashcard_QuizTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.LightGray) { innerPadding ->
            FlashcardQuiz(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}