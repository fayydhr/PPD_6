package com.example.currencyconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.currencyconverter.ui.theme.CurrencyConverterTheme
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CurrencyConverterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CurrencyConverterScreen()
                }
            }
        }
    }
}

// API Interface
interface CurrencyApiService {
    @GET("convert")
    suspend fun convertCurrency(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("amount") amount: Double,
        @Header("apikey") apiKey: String = "Pc2x29YaTWrgYTTiMQRtbaXd0Vcy2IRh"
    ): ConversionResponse
}

// Data Response
data class ConversionResponse(
    val success: Boolean,
    val result: Double
)

@Composable
fun CurrencyConverterScreen() {
    val currencies = listOf("USD", "EUR", "IDR", "JPY", "GBP")
    var amount by remember { mutableStateOf("") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("IDR") }
    var result by remember { mutableStateOf<Double?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.apilayer.com/currency_data/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val api = retrofit.create(CurrencyApiService::class.java)

    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Currency Converter", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        CurrencyDropdown("From", currencies, fromCurrency) { fromCurrency = it }
        CurrencyDropdown("To", currencies, toCurrency) { toCurrency = it }

        Button(
            onClick = {
                val amt = amount.toDoubleOrNull()
                if (amt != null) {
                    isLoading = true
                    scope.launch {
                        try {
                            val response = api.convertCurrency(fromCurrency, toCurrency, amt)
                            result = if (response.success) response.result else null
                        } catch (e: Exception) {
                            result = null
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text("Convert")
        }

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            result?.let {
                Text(
                    "$amount $fromCurrency = $it $toCurrency",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun CurrencyDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            onSelect(it)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
