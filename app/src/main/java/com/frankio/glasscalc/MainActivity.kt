package com.frankio.glasscalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.*
import org.json.JSONObject
import java.net.URL
import java.text.DecimalFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.*

private val BG = Color(0xFF090B12)
private val PANEL = Color(0xCC171C2A)
private val RAISED = Color(0xFF252D42)
private val PURPLE = Color(0xFFAA9BFF)
private val AQUA = Color(0xFF70DDD3)
private val MUTED = Color(0xFF9AA5BA)
private val WHITE = Color(0xFFF4F5FA)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val wm = WorkManager.getInstance(this)
  wm.enqueueUniqueWork("rates-first-fetch", ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<RateWorker>().build())
  wm.enqueueUniquePeriodicWork("rates-weekly", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<RateWorker>(7, TimeUnit.DAYS).build())
  setContent { GlassCalcApp() }
 }
}

class RateWorker(ctx: android.content.Context, params: WorkerParameters) : Worker(ctx, params) {
 override fun doWork(): Result = try {
  val json = JSONObject(URL("https://open.er-api.com/v6/latest/USD").readText())
  if (json.optString("result") != "success") return Result.retry()
  applicationContext.getSharedPreferences("rates", 0).edit()
   .putString("table", json.getJSONObject("rates").toString())
   .putLong("updated", System.currentTimeMillis()).apply()
  Result.success()
 } catch (_: Exception) { Result.retry() }
}

@Composable
private fun GlassCalcApp() {
 val context = LocalContext.current
 val settings = remember(context) { context.getSharedPreferences("settings", 0) }
 var page by remember { mutableStateOf("Calculator") }
 var country by remember { mutableStateOf(settings.getString("country", "Dominican Republic") ?: "Dominican Republic") }
 var currency by remember { mutableStateOf(settings.getString("currency", "DOP") ?: "DOP") }
 var metric by remember { mutableStateOf(settings.getBoolean("metric", true)) }
 MaterialTheme(colorScheme = darkColorScheme(primary = PURPLE, secondary = AQUA, background = BG, surface = PANEL)) {
  Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF1B1D31), BG, Color(0xFF10212A))))) {
   Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
     Column(Modifier.weight(1f)) {
      Text("GLASSCALC", color = MUTED, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
      Text("Precision, without friction.", color = WHITE, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
     }
     Text((if (currency == "DOP") "RD$ / DOP" else currency) + " · " + country, color = AQUA, fontSize = 10.sp)
    }
    AnimatedContent(page, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") { p ->
     when (p) {
      "Calculator" -> CalculatorPage()
      "Finance" -> EnhancedFinancePage(currency)
      "Convert" -> EnhancedConvertPage(currency, metric)
      "Tools" -> EnhancedToolsPage(metric, currency)
      else -> EnhancedSettingsPage(country, currency, metric, { selected -> country = selected; currency = currencyFor(selected); metric = selected !in listOf("United States", "Liberia", "Myanmar"); settings.edit().putString("country", selected).putString("currency", currency).putBoolean("metric", metric).apply() }, { selectedCurrency -> currency = selectedCurrency; settings.edit().putString("currency", selectedCurrency).apply() })
     }
    }
    NavigationBar(containerColor = Color(0xEE101522)) {
     listOf("Calculator", "Finance", "Convert", "Tools", "Settings").forEach { item ->
      NavigationBarItem(page == item, { page = item }, icon = {
       Icon(when (item) {
        "Calculator" -> Icons.Rounded.Calculate
        "Finance" -> Icons.Rounded.AccountBalance
        "Convert" -> Icons.Rounded.SwapHoriz
        "Tools" -> Icons.Rounded.Straighten
        else -> Icons.Rounded.Tune
       }, contentDescription = item)
      }, label = { Text(item, fontSize = 9.sp) },
       colors = NavigationBarItemDefaults.colors(selectedIconColor = PURPLE, selectedTextColor = WHITE, indicatorColor = RAISED, unselectedIconColor = MUTED, unselectedTextColor = MUTED))
     }
    }
   }
  }
 }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
 Column(modifier.clip(RoundedCornerShape(24.dp))
  .background(Brush.linearGradient(listOf(Color(0xDD252C40), Color(0xB8141926))))
  .border(1.dp, Color.White.copy(.10f), RoundedCornerShape(24.dp)).padding(16.dp), content = content)
}
@Composable private fun Heading(title: String, subtitle: String) {
 Column(Modifier.padding(bottom = 14.dp)) {
  Text(title, color = WHITE, fontSize = 25.sp, fontWeight = FontWeight.Bold)
  Text(subtitle, color = MUTED, fontSize = 12.sp)
 }
}
@Composable private fun Screen(content: @Composable ColumnScope.() -> Unit) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), content = content)
}
@Composable private fun Field(label: String, value: String, onValue: (String) -> Unit) {
 Column(Modifier.padding(vertical = 5.dp)) {
  Text(label, color = MUTED, fontSize = 11.sp)
  BasicTextField(value, onValue, Modifier.fillMaxWidth().padding(top = 5.dp)
   .clip(RoundedCornerShape(12.dp)).background(Color(0x552A3246)).padding(12.dp),
   singleLine = true, textStyle = TextStyle(color = WHITE, fontSize = 16.sp),
   keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
 }
}
private fun fmt(v: Double) = if (!v.isFinite()) "Error" else DecimalFormat("#,##0.########").format(v)

@Composable
private fun CalculatorPage() {
 var expr by remember { mutableStateOf("0") }
 var answer by remember { mutableStateOf("0") }
 var sci by remember { mutableStateOf(false) }
 var history by remember { mutableStateOf("") }
 Screen {
  Row(verticalAlignment = Alignment.CenterVertically) {
   Heading("Calculator", "Standard and scientific calculations")
   Spacer(Modifier.weight(1f))
   FilterChip(sci, { sci = !sci }, label = { Text(if (sci) "Scientific" else "Standard") })
  }
  GlassCard(Modifier.fillMaxWidth()) {
   Spacer(Modifier.height(30.dp))
   Text(expr, Modifier.fillMaxWidth(), color = MUTED, fontSize = 22.sp, textAlign = TextAlign.End)
   Text(answer, Modifier.fillMaxWidth(), color = WHITE, fontSize = 42.sp, textAlign = TextAlign.End)
   if (history.isNotEmpty()) Text(history, color = MUTED, fontSize = 10.sp)
  }
  Spacer(Modifier.height(12.dp))
  val rows = if (sci) listOf(listOf("sin","cos","tan","√","⌫"), listOf("("," )","π","^","÷"), listOf("7","8","9","×","AC"), listOf("4","5","6","−","%"), listOf("1","2","3","+","="), listOf("0",".","ln","log","Ans"))
   else listOf(listOf("AC","⌫","%","÷"), listOf("7","8","9","×"), listOf("4","5","6","−"), listOf("1","2","3","+"), listOf("0",".","(","="))
  rows.forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   row.forEach { key ->
    val op = key in listOf("AC","⌫","%","÷","×","−","+","=")
    Box(Modifier.weight(1f).padding(vertical = 4.dp).height(53.dp).clip(RoundedCornerShape(16.dp))
     .background(if (key == "=") Brush.linearGradient(listOf(PURPLE,AQUA)) else Brush.linearGradient(listOf(RAISED,PANEL)))
     .border(1.dp, Color.White.copy(.07f), RoundedCornerShape(16.dp)).clickable {
      when (key.trim()) {
       "AC" -> { expr = "0"; answer = "0" }
       "⌫" -> expr = expr.dropLast(1).ifEmpty { "0" }
       "=" -> try { answer = fmt(Expression.eval(expr)); history = expr + " = " + answer; expr = answer } catch (_: Exception) { answer = "Error" }
       "Ans" -> expr += answer
       "π" -> expr += "pi"
       "√" -> expr += "sqrt("
       "sin","cos","tan","ln","log" -> expr += key.trim() + "("
       "×" -> expr += "*"
       "÷" -> expr += "/"
       "−" -> expr += "-"
       else -> expr = if (expr == "0" && key.trim().firstOrNull()?.isDigit() == true) key.trim() else expr + key.trim()
      }
     }, contentAlignment = Alignment.Center) {
     Text(key.trim(), color = if (key == "=") BG else if (op) AQUA else WHITE, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
   }
  } }
 }
}

private object Expression {
 fun eval(s: String) = Parser(s.replace("π","pi")).parse()
 private class Parser(private val s: String) {
  var i = 0
  fun parse(): Double { val v = add(); skip(); require(i == s.length); return v }
  fun add(): Double { var x = mul(); while (true) { skip(); when { eat('+') -> x += mul(); eat('-') -> x -= mul(); else -> return x } } }
  fun mul(): Double { var x = power(); while (true) { skip(); when { eat('*') -> x *= power(); eat('/') -> x /= power(); eat('%') -> x %= power(); else -> return x } } }
  fun power(): Double { var x = unary(); skip(); if (eat('^')) x = x.pow(power()); return x }
  fun unary(): Double {
   skip()
   if (eat('-')) return -unary()
   if (eat('+')) return unary()
   if (eat('(')) { val v = add(); skip(); require(eat(')')); return v }
   if (i < s.length && s[i].isLetter()) {
    val start = i; while (i < s.length && s[i].isLetter()) i++
    val fn = s.substring(start,i)
    if (fn == "pi") return Math.PI
    skip(); require(eat('(')); val a = add(); skip(); require(eat(')'))
    return when(fn) { "sqrt" -> sqrt(a); "sin" -> sin(Math.toRadians(a)); "cos" -> cos(Math.toRadians(a)); "tan" -> tan(Math.toRadians(a)); "ln" -> ln(a); "log" -> log10(a); else -> error("Function") }
   }
   val start = i; while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
   require(i > start); return s.substring(start,i).toDouble()
  }
  fun skip() { while (i < s.length && s[i].isWhitespace()) i++ }
  fun eat(c: Char) = if (i < s.length && s[i] == c) { i++; true } else false
 }
}

@Composable
private fun FinancePage(currency: String) {
 var mode by remember { mutableStateOf("Compound interest") }
 var principal by remember { mutableStateOf("10000") }
 var rate by remember { mutableStateOf("8") }
 var years by remember { mutableStateOf("5") }
 var compounds by remember { mutableStateOf("12") }
 val p = principal.toDoubleOrNull() ?: 0.0
 val r = (rate.toDoubleOrNull() ?: 0.0) / 100
 val t = years.toDoubleOrNull() ?: 0.0
 val n = (compounds.toDoubleOrNull() ?: 1.0).coerceAtLeast(1.0)
 val result = when(mode) {
  "Simple interest" -> p * (1 + r*t)
  "Compound interest" -> p * (1 + r/n).pow(n*t)
  "Loan payment" -> if (t <= 0) 0.0 else if (r == 0.0) p/(t*12) else { val m=r/12; p*m/(1-(1+m).pow(-t*12)) }
  "ROI" -> p*(1+r)
  else -> if (1+r*t == 0.0) 0.0 else p/(1+r*t)
 }
 Screen {
  Heading("Financial lab", "Interest, loans, investments and returns")
  GlassCard(Modifier.fillMaxWidth()) {
   Text("CALCULATION TYPE", color = AQUA, fontSize = 10.sp, fontWeight = FontWeight.Bold)
   listOf("Simple interest","Compound interest","Loan payment","ROI","Reverse principal").forEach {
    Row(Modifier.fillMaxWidth().clickable { mode = it }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
     RadioButton(mode == it, { mode = it }, colors = RadioButtonDefaults.colors(selectedColor = PURPLE))
     Text(it, color = WHITE, fontSize = 13.sp)
    }
   }
   Field("Principal / amount ("+currency+")", principal) { principal = it }
   Field("Annual rate / return (%)", rate) { rate = it }
   Field("Duration (years)", years) { years = it }
   if (mode == "Compound interest") Field("Compounds per year", compounds) { compounds = it }
   Spacer(Modifier.height(10.dp))
   Text(if (mode == "Loan payment") "MONTHLY PAYMENT" else if (mode == "Reverse principal") "ORIGINAL PRINCIPAL" else "RESULT / FUTURE VALUE", color = MUTED, fontSize = 10.sp)
   Text(currency + " " + fmt(result), color = AQUA, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
   Text("Difference: " + currency + " " + fmt(result-p), color = MUTED, fontSize = 11.sp)
  }
  Spacer(Modifier.height(12.dp))
  GlassCard(Modifier.fillMaxWidth()) { Text("Loan estimate assumes a fixed annual rate, monthly payments and no fees. Reverse principal uses the simple-interest equation.", color = MUTED, fontSize = 12.sp) }
 }
}

private val groups = mapOf(
 "Length" to listOf("Meter","Kilometer","Centimeter","Millimeter","Mile","Foot","Inch"),
 "Weight" to listOf("Kilogram","Gram","Milligram","Pound","Ounce"),
 "Volume" to listOf("Liter","Milliliter","Cubic meter","US gallon"),
 "Speed" to listOf("m/s","km/h","mph"),
 "Time" to listOf("Second","Minute","Hour","Day"),
 "Area" to listOf("Square meter","Square kilometer","Hectare","Acre"),
 "Data" to listOf("Byte","Kilobyte","Megabyte","Gigabyte")
)
private val factors = mapOf("Meter" to 1.0,"Kilometer" to 1000.0,"Centimeter" to .01,"Millimeter" to .001,"Mile" to 1609.344,"Foot" to .3048,"Inch" to .0254,
 "Kilogram" to 1.0,"Gram" to .001,"Milligram" to .000001,"Pound" to .45359237,"Ounce" to .0283495,
 "Liter" to 1.0,"Milliliter" to .001,"Cubic meter" to 1000.0,"US gallon" to 3.785411784,
 "m/s" to 1.0,"km/h" to .277777778,"mph" to .44704,"Second" to 1.0,"Minute" to 60.0,"Hour" to 3600.0,"Day" to 86400.0,
 "Square meter" to 1.0,"Square kilometer" to 1e6,"Hectare" to 10000.0,"Acre" to 4046.856,
 "Byte" to 1.0,"Kilobyte" to 1024.0,"Megabyte" to 1048576.0,"Gigabyte" to 1073741824.0)

@Composable
private fun ConvertPage() {
 var group by remember { mutableStateOf("Length") }
 var input by remember { mutableStateOf("1") }
 var from by remember { mutableStateOf("Kilometer") }
 var to by remember { mutableStateOf("Meter") }
 var currencyMode by remember { mutableStateOf(false) }
 var base by remember { mutableStateOf("USD") }
 var target by remember { mutableStateOf("DOP") }
 val prefs = LocalContext.current.getSharedPreferences("rates", 0)
 val rates = remember { runCatching { JSONObject(prefs.getString("table","{}") ?: "{}") }.getOrDefault(JSONObject()) }
 val amount = input.toDoubleOrNull() ?: 0.0
 val output = if (currencyMode) {
  val a = rates.optDouble(base, Double.NaN); val b = rates.optDouble(target, Double.NaN)
  if (a.isFinite() && b.isFinite() && a != 0.0) amount/a*b else Double.NaN
 } else amount*(factors[from] ?: 1.0)/(factors[to] ?: 1.0)
 Screen {
  Heading("Converters", "Units and exchange rates")
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   FilterChip(!currencyMode, { currencyMode = false }, label = { Text("Units") })
   FilterChip(currencyMode, { currencyMode = true }, label = { Text("Currency") })
  }
  GlassCard(Modifier.fillMaxWidth()) {
   Field("Amount", input) { input = it }
   if (currencyMode) {
    val currencies = listOf("USD","DOP","EUR","GBP","CAD","MXN","JPY","BRL","ARS","INR","AUD","CHF","CNY")
    Picker("From currency", base, currencies) { base = it }
    Picker("To currency", target, currencies) { target = it }
    val updated = prefs.getLong("updated",0L)
    Text(if (updated > 0) "Last updated: " + java.text.DateFormat.getDateTimeInstance().format(Date(updated)) else "No cached rate yet. Connect to the internet and reopen this screen.", color = MUTED, fontSize = 10.sp)
   } else {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
     groups.keys.forEach { g -> FilterChip(group == g, { group = g; from = groups.getValue(g).first(); to = groups.getValue(g).getOrElse(1) { groups.getValue(g).first() } }, label = { Text(g, fontSize = 9.sp) }) }
    }
    Picker("From", from, groups.getValue(group)) { from = it }
    Picker("To", to, groups.getValue(group)) { to = it }
   }
   Spacer(Modifier.height(12.dp))
   Text("RESULT", color = MUTED, fontSize = 10.sp)
   Text(if (output.isFinite()) fmt(output) else "Rate unavailable", color = AQUA, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
   Text(if (currencyMode) base + " → " + target else from + " → " + to, color = MUTED, fontSize = 11.sp)
  }
 }
}

@Composable
private fun Picker(label: String, selected: String, choices: List<String>, onPick: (String) -> Unit) {
 var expanded by remember { mutableStateOf(false) }
 Column(Modifier.padding(vertical = 5.dp)) {
  Text(label, color = MUTED, fontSize = 11.sp)
  Box {
   Surface(Modifier.fillMaxWidth().clickable { expanded = true }, color = Color(0x552A3246), shape = RoundedCornerShape(12.dp)) {
    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
     Text(selected, Modifier.weight(1f), color = WHITE)
     Icon(Icons.Rounded.ExpandMore, null, tint = AQUA)
    }
   }
   DropdownMenu(expanded, { expanded = false }) {
    choices.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onPick(it); expanded = false }) }
   }
  }
 }
}

@Composable
private fun ToolsPage() {
 var weight by remember { mutableStateOf("70") }
 var height by remember { mutableStateOf("1.75") }
 var amount by remember { mutableStateOf("1000") }
 var tax by remember { mutableStateOf("18") }
 val bmi = (weight.toDoubleOrNull() ?: 0.0)/(height.toDoubleOrNull() ?: 1.0).pow(2)
 val a = amount.toDoubleOrNull() ?: 0.0
 val r = (tax.toDoubleOrNull() ?: 0.0)/100
 Screen {
  Heading("Everyday tools", "Quick calculations for daily use")
  GlassCard(Modifier.fillMaxWidth()) {
   Text("BMI CALCULATOR", color = AQUA, fontSize = 10.sp, fontWeight = FontWeight.Bold)
   Field("Weight (kg)", weight) { weight = it }
   Field("Height (meters)", height) { height = it }
   Text("BMI  " + fmt(bmi), color = WHITE, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
   Text(when { bmi < 18.5 -> "Below adult reference range"; bmi < 25 -> "Within adult reference range"; bmi < 30 -> "Above adult reference range"; else -> "High adult BMI range" }, color = MUTED, fontSize = 11.sp)
   Text("BMI is an adult screening measure, not a diagnosis.", color = MUTED, fontSize = 10.sp)
  }
  Spacer(Modifier.height(12.dp))
  GlassCard(Modifier.fillMaxWidth()) {
   Text("TAX / VAT", color = AQUA, fontSize = 10.sp, fontWeight = FontWeight.Bold)
   Field("Amount before tax", amount) { amount = it }
   Field("Tax rate (%)", tax) { tax = it }
   Text("Tax amount: " + fmt(a*r), color = WHITE, fontSize = 16.sp)
   Text("Total: " + fmt(a*(1+r)), color = AQUA, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
  }
 }
}

@Composable
private fun SettingsPage(country: String, currency: String, metric: Boolean, onCountry: (String) -> Unit, onCurrency: (String) -> Unit) {
 val countries = listOf("Dominican Republic","United States","United Kingdom","Canada","Mexico","Spain","France","Germany","Japan","Brazil","Argentina","India","Australia")
 var expanded by remember { mutableStateOf(false) }
 Screen {
  Heading("Preferences", "Regional defaults and exchange-rate settings")
  GlassCard(Modifier.fillMaxWidth()) {
   Text("COUNTRY / REGION", color = AQUA, fontSize = 10.sp, fontWeight = FontWeight.Bold)
   Box {
    Surface(Modifier.fillMaxWidth().clickable { expanded = true }, color = Color(0x552A3246), shape = RoundedCornerShape(12.dp)) {
     Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(country, Modifier.weight(1f), color = WHITE)
      Icon(Icons.Rounded.ExpandMore, null, tint = AQUA)
     }
    }
    DropdownMenu(expanded, { expanded = false }) {
     countries.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onCountry(it); onCurrency(currencyFor(it)); expanded = false }) }
    }
   }
   Spacer(Modifier.height(12.dp))
   Text("DEFAULT CURRENCY", color = MUTED, fontSize = 10.sp)
   Text(currency, color = WHITE, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
   Text("Measurement system", color = MUTED, fontSize = 11.sp)
   Text(if (metric) "Metric · km, m, kg, L, °C" else "Imperial · mi, ft, lb, gal, °F", color = AQUA)
  }
  Spacer(Modifier.height(12.dp))
  GlassCard(Modifier.fillMaxWidth()) {
   Text("CURRENCY DATA", color = AQUA, fontSize = 10.sp, fontWeight = FontWeight.Bold)
   Text("Rates are fetched from open.er-api.com when online, cached locally, and refreshed weekly by Android WorkManager. Cached rates remain available offline.", color = MUTED, fontSize = 12.sp)
   Text("Rates are indicative and may differ from bank or card-provider rates.", color = MUTED, fontSize = 10.sp)
  }
 }
}
private fun currencyFor(country: String): String = when(country) {
 "Dominican Republic" -> "DOP"; "United States" -> "USD"; "United Kingdom" -> "GBP"; "Canada" -> "CAD"
 "Mexico" -> "MXN"; "Spain", "France", "Germany" -> "EUR"; "Japan" -> "JPY"; "Brazil" -> "BRL"
 "Argentina" -> "ARS"; "India" -> "INR"; "Australia" -> "AUD"; else -> "USD"
}
