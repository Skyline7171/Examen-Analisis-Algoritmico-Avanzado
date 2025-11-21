package com.example.cifry

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cifry.ui.theme.CifryTheme
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.experimental.xor
import androidx.compose.material.icons.filled.Info

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CifryTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Estado para saber si mostramos "Léeme" (true) o "Principal" (false)
                    var showReadMe by androidx.compose.runtime.remember { mutableStateOf(false) }

                    if (showReadMe) {
                        // Si es true, mostramos la nueva pantalla
                        // Pasamos la función para que el botón "Volver" ponga el estado en false
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            ReadMeScreen(onBack = { showReadMe = false })
                        }
                    } else {
                        // Si es false, mostramos la pantalla de Cifrado
                        // Pasamos la función para navegar a Léeme (poner estado en true)
                        EncryptionScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToReadMe = { showReadMe = true }
                        )
                    }
                }
            }
        }
    }
}

object LuciferCipher {
    private val S_BOX = byteArrayOf(
        12, 5, 6, 11, 9, 0, 10, 13, 3, 14, 15, 8, 4, 7, 1, 2
    )

    fun encrypt(text: String, key: String): String {
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val keyBytes = key.padEnd(16, '0').toByteArray(Charsets.UTF_8).take(16).toByteArray()

        val encryptedBytes = ByteArray(textBytes.size)

        var i = 0
        while (i < textBytes.size) {
            val b1 = textBytes[i]
            val b2 = if (i + 1 < textBytes.size) textBytes[i + 1] else 0

            val (c1, c2) = feistelNetwork(b1, b2, keyBytes)

            encryptedBytes[i] = c1
            if (i + 1 < textBytes.size) {
                encryptedBytes[i + 1] = c2
            }
            i += 2
        }

        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    private fun feistelNetwork(left: Byte, right: Byte, key: ByteArray): Pair<Byte, Byte> {
        var l = left
        var r = right

        for (round in 0 until 16) {
            val temp = r
            val subKeyByte = key[round % key.size]
            val fResult = functionF(r, subKeyByte)

            r = l xor fResult
            l = temp
        }
        return Pair(r, l)
    }

    private fun functionF(data: Byte, keyByte: Byte): Byte {
        val mixed = (data.toInt() xor keyByte.toInt()) and 0xFF

        val highNibble = (mixed ushr 4) and 0x0F
        val lowNibble = mixed and 0x0F

        val sHigh = S_BOX[highNibble]
        val sLow = S_BOX[lowNibble]

        return ((sHigh.toInt() shl 4) or sLow.toInt()).toByte()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EncryptionScreen(
    modifier: Modifier = Modifier,
    onNavigateToReadMe: () -> Unit
) {

    var inputText by androidx.compose.runtime.remember { mutableStateOf("") }
    var selectedMethod by androidx.compose.runtime.remember { mutableStateOf("DES") }
    var resultText by androidx.compose.runtime.remember { mutableStateOf("") }
    var resultMethod by androidx.compose.runtime.remember { mutableStateOf("") }

    // Lista de opciones
    val encryptionMethods = listOf("DES", "Lucifer", "Función HASH")

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val gradientColors = listOf(
        Color(0xFF6A75E6),
        Color(0xFF7B4DBC)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(
            text = "Cifry",
            style = TextStyle(
                brush = Brush.horizontalGradient(colors = gradientColors),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            ),
            fontWeight = FontWeight.Bold
        )

        Image(
            painter = painterResource(id = R.drawable.cifry),
            contentDescription = null,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Campo de Entrada
        OutlinedTextField(
            value = inputText,
            onValueChange = { newValue ->
                inputText = newValue
                resultText = ""
            },
            label = { Text("Ingresa el texto a cifrar") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth(),

            shape = RoundedCornerShape(12.dp),
            singleLine = false,
            maxLines = 3
        )

        // Selector de Método (Chips)
        Text(
            text = "Método de cifrado:",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            encryptionMethods.forEach { method ->
                FilterChip(
                    selected = (method == selectedMethod),
                    onClick = {
                        selectedMethod = method
                    },
                    label = { Text(method) },
                    leadingIcon = if (method == selectedMethod) {
                        { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        // Botón de Acción
        Button(
            onClick = {
                if (inputText.isNotEmpty()) {
                    resultMethod = selectedMethod
                    resultText = mockEncrypt(inputText, selectedMethod)
                } else {
                    Toast.makeText(context, "Por favor escribe algo", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xff6e67c6),
            )
        ) {
            Text("Cifrar texto", fontSize = 16.sp)
        }

        Button(
            onClick = {
                onNavigateToReadMe()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xff6e67c6),
            )
        ) {
            Text("Léeme", fontSize = 16.sp)
        }

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        // Resultado
        AnimatedVisibility(visible = resultText.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Resultado ($resultMethod):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(resultText))
                            Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@SuppressLint("GetInstance")
@OptIn(ExperimentalEncodingApi::class)
fun mockEncrypt(text: String, method: String): String {
    return when (method) {
        "Función HASH" -> {
            val bytes = text.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.fold("") { str, it -> str + "%02x".format(it) }
        }
        "DES" -> {
            // Clave secreta para DES (8 bytes)
            val secretKeyString = "12345678"
            val keySpec = DESKeySpec(secretKeyString.toByteArray(StandardCharsets.UTF_8))
            val keyFactory = SecretKeyFactory.getInstance("DES")
            val key = keyFactory.generateSecret(keySpec)

            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val encryptedBytes = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))
            Base64.encodeToString(encryptedBytes,
                Base64.NO_WRAP)
        }
        "Lucifer" -> {
            LuciferCipher.encrypt(text, "CLAVE_LUCIFER_12")
        }
        else -> ""
    }
}

@Composable
fun ReadMeScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Gracias por exigir",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Muchas gracias por dejar como tarea algo tan interesante y desafiante como crear una aplicación móvil. Es triste, pero es lo más interesante que la carrera me ha asignado en este año...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xff6e67c6)
            )
        ) {
            Text("Volver al inicio", fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EncryptionPreview() {
    CifryTheme {
        EncryptionScreen(onNavigateToReadMe = {})
    }
}