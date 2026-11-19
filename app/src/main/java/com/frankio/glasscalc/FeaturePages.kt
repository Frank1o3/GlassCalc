package com.frankio.glasscalc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

private val C_BG = Color(0xFF090B12)
private val C_PANEL = Color(0xCC171C2A)
private val C_AQUA = Color(0xFF70DDD3)
private val C_MUTED = Color(0xFF9AA5BA)
private val C_WHITE = Color(0xFFF4F5FA)

@Composable private fun FScreen(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), content = content)
}
@Composable private fun FHeading(title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 14.dp)) {
        Text(title, color = C_WHITE, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = C_MUTED, fontSize = 12.sp)
    }
}
@Composable private fun FCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(C_PANEL, RoundedCornerShape(22.dp)).padding(16.dp), content = content)
}
@Composable private fun FField(label: String, value: String, change: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = C_MUTED, fontSize = 11.sp)
        BasicTextField(value, change, Modifier.fillMaxWidth().padding(top = 5.dp)
            .background(Color(0x552A3246), RoundedCornerShape(12.dp)).padding(12.dp),
            singleLine = true, textStyle = TextStyle(color = C_WHITE, fontSize = 16.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
    }
}
@Composable private fun FPicker(label: String, value: String, options: List<String>, change: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = C_MUTED, fontSize = 11.sp)
        Box {
            Row(Modifier.fillMaxWidth().padding(top = 5.dp).background(Color(0x552A3246), RoundedCornerShape(12.dp))
                .clickable { open = true }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value, Modifier.weight(1f), color = C_WHITE)
                Icon(Icons.Rounded.ExpandMore, null, tint = C_AQUA)
            }
            DropdownMenu(open, { open = false }) {
                options.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { change(it); open = false }) }
            }
        }
    }
}
private fun v(s: String, default: Double = 0.0) = s.replace(",", "").toDoubleOrNull() ?: default
private fun f(n: Double) = if (n.isFinite()) String.format(Locale.US, "%,.2f", n) else "Not available"
@Composable private fun FResult(label: String, result: String, detail: String = "") {
    Spacer(Modifier.height(12.dp))
    Text(label.uppercase(), color = C_MUTED, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    Text(result, color = C_AQUA, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
    if (detail.isNotBlank()) Text(detail, color = C_MUTED, fontSize = 11.sp)
}

private val financeOptions = listOf("Simple interest", "Compound interest", "Loan amortization", "Reverse interest rate",
    "ROI / investment return", "Investment growth", "Present value", "Future value", "Savings goal", "VAT / tax")

@Composable
fun EnhancedFinancePage(currency: String) {
    var mode by remember { mutableStateOf("Compound interest") }
    var principal by remember { mutableStateOf("10000") }
    var rate by remember { mutableStateOf("8") }
    var years by remember { mutableStateOf("5") }
    var target by remember { mutableStateOf("25000") }
    var contribution by remember { mutableStateOf("200") }
    var compounds by remember { mutableStateOf("12") }
    var inclusive by remember { mutableStateOf(false) }
    val p=v(principal); val r=v(rate)/100; val t=v(years); val goal=v(target)
    val monthly=r/12; val months=(t*12).roundToInt().coerceAtLeast(0)
    val n=v(compounds,12.0).coerceAtLeast(1.0); val deposit=v(contribution)
    val result: Double
    val label: String
    val detail: String
    when(mode) {
        "Simple interest" -> { result=p*(1+r*t); label="Future value"; detail="Interest earned: ${f(result-p)} $currency" }
        "Compound interest" -> { result=if(t<0) Double.NaN else p*(1+r/n).pow(n*t); label="Future value"; detail="Interest earned: ${f(result-p)} $currency" }
        "Loan amortization" -> {
            result=when { months==0 -> Double.NaN; monthly==0.0 -> p/months; else -> p*monthly/(1-(1+monthly).pow(-months)) }
            label="Monthly payment"; detail="Total repaid: ${f(result*months)} $currency · Interest: ${f(result*months-p)} $currency"
        }
        "Reverse interest rate" -> { result=if(p<=0||goal<=0||t<=0) Double.NaN else ((goal/p).pow(1/t)-1)*100; label="Required annual rate"; detail="Compound annual rate needed to reach the target." }
        "ROI / investment return" -> { result=if(p==0.0) Double.NaN else (goal-p)/p*100; label="Return on investment"; detail="Net gain/loss: ${f(goal-p)} $currency" }
        "Investment growth" -> {
            val initial=if(monthly==0.0) p else p*(1+monthly).pow(months)
            val recurring=if(months==0) 0.0 else if(monthly==0.0) deposit*months else deposit*((1+monthly).pow(months)-1)/monthly
            result=initial+recurring; label="Projected value"; detail="Total contributed: ${f(p+deposit*months)} $currency"
        }
        "Present value" -> { result=if(t<0) Double.NaN else goal/(1+r).pow(t); label="Present value"; detail="Amount required today to reach the future value." }
        "Future value" -> { result=p*(1+r).pow(t); label="Future value"; detail="Annual compounding, excluding additional deposits." }
        "Savings goal" -> {
            result=when { months==0 -> Double.NaN; monthly==0.0 -> (goal-p)/months; else -> (goal-p*(1+monthly).pow(months))*monthly/((1+monthly).pow(months)-1) }
            label="Required monthly saving"; detail="Estimated deposits over $months months."
        }
        else -> { result=if(inclusive) p-p/(1+r) else p*r; label=if(inclusive) "Tax included" else "Tax amount"; detail=if(inclusive) "Pre-tax amount: ${f(p-result)} $currency" else "Total with tax: ${f(p+result)} $currency" }
    }
    FScreen {
        FHeading("Financial lab", "Borrowing, saving, investing and tax calculations")
        FCard {
            FPicker("Calculator", mode, financeOptions) { mode=it }
            when(mode) {
                "Reverse interest rate" -> { FField("Starting principal ($currency)",principal){principal=it}; FField("Target future value ($currency)",target){target=it}; FField("Duration (years)",years){years=it} }
                "ROI / investment return" -> { FField("Initial investment ($currency)",principal){principal=it}; FField("Final value ($currency)",target){target=it} }
                "Present value" -> { FField("Future amount ($currency)",target){target=it}; FField("Discount rate (%)",rate){rate=it}; FField("Duration (years)",years){years=it} }
                "VAT / tax" -> {
                    FField("Amount ($currency)",principal){principal=it}; FField("Tax rate (%)",rate){rate=it}
                    Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(inclusive,{inclusive=it}); Text("Amount already includes tax",color=C_WHITE,fontSize=12.sp) }
                }
                "Savings goal" -> { FField("Current savings ($currency)",principal){principal=it}; FField("Target ($currency)",target){target=it}; FField("Annual interest (%)",rate){rate=it}; FField("Time (years)",years){years=it} }
                "Investment growth" -> { FField("Initial investment ($currency)",principal){principal=it}; FField("Monthly contribution ($currency)",contribution){contribution=it}; FField("Annual return (%)",rate){rate=it}; FField("Duration (years)",years){years=it} }
                else -> {
                    FField("Principal / amount ($currency)",principal){principal=it}; FField("Annual rate (%)",rate){rate=it}; FField("Duration (years)",years){years=it}
                    if(mode=="Compound interest") FField("Compounds per year",compounds){compounds=it}
                }
            }
            FResult(label, "${f(result)} ${if(mode=="Reverse interest rate"||mode=="ROI / investment return") "%" else currency}", detail)
        }
        Spacer(Modifier.height(12.dp))
        FCard { Text("Estimates assume fixed rates. Loan calculations use monthly payments and exclude fees. Investment growth is illustrative and is not a guarantee of future returns.",color=C_MUTED,fontSize=12.sp) }
        Spacer(Modifier.height(16.dp))
    }
}

private data class U(val name:String,val factor:Double=1.0)
private val unitSets=linkedMapOf(
 "Length" to listOf(U("Meter"),U("Kilometer",1000.0),U("Centimeter",.01),U("Millimeter",.001),U("Mile",1609.344),U("Yard",.9144),U("Foot",.3048),U("Inch",.0254)),
 "Weight / mass" to listOf(U("Kilogram"),U("Gram",.001),U("Milligram",.000001),U("Metric ton",1000.0),U("Pound",.45359237),U("Ounce",.028349523)),
 "Volume" to listOf(U("Liter"),U("Milliliter",.001),U("Cubic meter",1000.0),U("US gallon",3.785411784),U("Imperial gallon",4.54609),U("US cup",.2365882365),U("US fluid ounce",.0295735296)),
 "Temperature" to listOf(U("Celsius"),U("Fahrenheit"),U("Kelvin")),
 "Speed" to listOf(U("Meters/second"),U("Kilometers/hour",.277777778),U("Miles/hour",.44704),U("Knots",.514444)),
 "Time" to listOf(U("Second"),U("Millisecond",.001),U("Minute",60.0),U("Hour",3600.0),U("Day",86400.0),U("Week",604800.0),U("Year (365 days)",31536000.0)),
 "Area" to listOf(U("Square meter"),U("Square kilometer",1e6),U("Square centimeter",.0001),U("Hectare",10000.0),U("Acre",4046.8564224),U("Square foot",.09290304)),
 "Data" to listOf(U("Byte"),U("Bit",.125),U("Kilobyte (KB)",1024.0),U("Megabyte (MB)",1048576.0),U("Gigabyte (GB)",1073741824.0),U("Terabyte (TB)",1099511627776.0)),
 "Pressure" to listOf(U("Pascal"),U("Kilopascal",1000.0),U("Bar",100000.0),U("Atmosphere",101325.0),U("PSI",6894.757),U("mmHg",133.322)),
 "Energy" to listOf(U("Joule"),U("Kilojoule",1000.0),U("Calorie",4.184),U("Kilocalorie",4184.0),U("Watt-hour",3600.0),U("Kilowatt-hour",3600000.0),U("BTU",1055.056))
)
private fun tempConvert(x:Double,a:String,b:String):Double {
    val c=when(a) { "Fahrenheit"->(x-32)*5/9; "Kelvin"->x-273.15; else->x }
    return when(b) { "Fahrenheit"->c*9/5+32; "Kelvin"->c+273.15; else->c }
}
@Composable
fun EnhancedConvertPage(currency:String,metric:Boolean) {
    var category by remember { mutableStateOf("Length") }
    var amount by remember { mutableStateOf("1") }
    var from by remember { mutableStateOf(if(metric) "Kilometer" else "Mile") }
    var to by remember { mutableStateOf(if(metric) "Meter" else "Foot") }
    var money by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf(currency) }
    LaunchedEffect(currency) { source = currency }
    var destination by remember { mutableStateOf("USD") }
    val ctx= LocalContext.current
    val prefs=remember(ctx){ctx.getSharedPreferences("rates",0)}
    var rates by remember { mutableStateOf(JSONObject(prefs.getString("table","{}")?:"{}")) }
    var updated by remember { mutableLongStateOf(prefs.getLong("updated",0L)) }
    val x=v(amount)
    val out=if(money) {
        val a=rates.optDouble(source,Double.NaN); val b=rates.optDouble(destination,Double.NaN)
        if(a.isFinite()&&b.isFinite()&&a>0) x/a*b else Double.NaN
    } else if(category=="Temperature") tempConvert(x,from,to) else {
        val list=unitSets.getValue(category); x*(list.find{it.name==from}?.factor?:1.0)/(list.find{it.name==to}?.factor?:1.0)
    }
    val codes=remember(rates){ val k=rates.keys().asSequence().toList().sorted(); if(k.isEmpty()) listOf("USD","DOP","EUR","GBP","CAD","MXN","JPY") else k }
    FScreen {
        FHeading("Converters","Measurement, science, storage and currency")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(!money,{money=false},label={Text("Units")})
            FilterChip(money,{money=true},label={Text("Currency")})
        }
        Spacer(Modifier.height(8.dp))
        FCard {
            FField("Amount",amount){amount=it}
            if(money) {
                FPicker("From currency",source,codes){source=it}
                FPicker("To currency",destination,codes){destination=it}
                Text(if(updated>0) "Rates last updated: "+DateFormat.getDateTimeInstance().format(Date(updated)) else "No exchange-rate cache yet.",color=C_MUTED,fontSize=10.sp)
                TextButton(onClick={rates=JSONObject(prefs.getString("table","{}")?:"{}");updated=prefs.getLong("updated",0L)}){Text("Reload cached rates")}
            } else {
                FPicker("Category",category,unitSets.keys.toList()){ selected ->
                    category=selected
                    val list=unitSets.getValue(selected)
                    from=when(selected) {
                        "Length"->if(metric)"Kilometer" else "Mile"
                        "Weight / mass"->if(metric)"Kilogram" else "Pound"
                        "Volume"->if(metric)"Liter" else "US gallon"
                        "Temperature"->if(metric)"Celsius" else "Fahrenheit"
                        "Speed"->if(metric)"Kilometers/hour" else "Miles/hour"
                        else->list.first().name
                    }
                    to=list.getOrElse(1){list.first()}.name
                }
                val choices=unitSets.getValue(category).map{it.name}
                FPicker("From",from,choices){from=it}
                FPicker("To",to,choices){to=it}
            }
            FResult("Converted value",f(out),if(money)"$source → $destination" else "$from → $to")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun EnhancedToolsPage(metric:Boolean,currency:String) {
    var weight by remember { mutableStateOf(if(metric)"70" else "154") }
    var height by remember { mutableStateOf(if(metric)"1.75" else "69") }
    var amount by remember { mutableStateOf("100") }
    var percent by remember { mutableStateOf("15") }
    var bill by remember { mutableStateOf("50") }
    var tip by remember { mutableStateOf("15") }
    var people by remember { mutableStateOf("2") }
    var birth by remember { mutableStateOf("2000-01-01") }
    var start by remember { mutableStateOf("2026-01-01") }
    var end by remember { mutableStateOf("2026-12-31") }
    val bmi=if(metric) v(weight)/v(height,1.0).pow(2) else v(weight)*703/v(height,1.0).pow(2)
    val percentValue=v(amount)*v(percent)/100
    val tipValue=v(bill)*v(tip)/100
    val split=(v(bill)+tipValue)/v(people,1.0).coerceAtLeast(1.0)
    val age=runCatching{Period.between(LocalDate.parse(birth),LocalDate.now()).let{"${it.years} years, ${it.months} months, ${it.days} days"}}.getOrNull()
    val days=runCatching{ChronoUnit.DAYS.between(LocalDate.parse(start),LocalDate.parse(end))}.getOrNull()
    FScreen {
        FHeading("Everyday tools","Quick calculations for daily use")
        FCard {
            Text("BMI",color=C_AQUA,fontSize=11.sp,fontWeight=FontWeight.Bold)
            FField(if(metric)"Weight (kg)" else "Weight (lb)",weight){weight=it}
            FField(if(metric)"Height (meters)" else "Height (inches)",height){height=it}
            FResult("Body mass index",f(bmi),when{!bmi.isFinite()||bmi<=0->"Enter valid measurements.";bmi<18.5->"Below adult reference range.";bmi<25->"Within adult reference range.";bmi<30->"Above adult reference range.";else->"High adult reference range."})
            Text("BMI is a screening measure, not a diagnosis. Adult reference ranges are not suitable for children or teens.",color=C_MUTED,fontSize=10.sp)
        }
        Spacer(Modifier.height(12.dp))
        FCard {
            Text("PERCENTAGE",color=C_AQUA,fontSize=11.sp,fontWeight=FontWeight.Bold)
            FField("Base amount ($currency)",amount){amount=it}
            FField("Percentage (%)",percent){percent=it}
            FResult("Percentage value",f(percentValue),"Increase: ${f(v(amount)+percentValue)} · Decrease: ${f(v(amount)-percentValue)}")
        }
        Spacer(Modifier.height(12.dp))
        FCard {
            Text("TIP AND BILL SPLITTER",color=C_AQUA,fontSize=11.sp,fontWeight=FontWeight.Bold)
            FField("Bill amount ($currency)",bill){bill=it}
            FField("Tip (%)",tip){tip=it}
            FField("People",people){people=it}
            FResult("Total with tip","${f(v(bill)+tipValue)} $currency","Tip: ${f(tipValue)} · Each person: ${f(split)} $currency")
        }
        Spacer(Modifier.height(12.dp))
        FCard {
            Text("AGE CALCULATOR",color=C_AQUA,fontSize=11.sp,fontWeight=FontWeight.Bold)
            FField("Birth date (YYYY-MM-DD)",birth){birth=it}
            FResult("Age",age?:"Enter a valid date")
        }
        Spacer(Modifier.height(12.dp))
        FCard {
            Text("DATE DIFFERENCE",color=C_AQUA,fontSize=11.sp,fontWeight=FontWeight.Bold)
            FField("Start date (YYYY-MM-DD)",start){start=it}
            FField("End date (YYYY-MM-DD)",end){end=it}
            FResult("Difference",days?.let{"$it days"}?:"Enter valid dates")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun EnhancedSettingsPage(country:String,currency:String,metric:Boolean,onCountry:(String)->Unit,onCurrency:(String)->Unit) {
    val countries=listOf("Dominican Republic","United States","United Kingdom","Canada","Mexico","Spain","France","Germany","Japan","Brazil","Argentina","India","Australia","Liberia","Myanmar")
    val currencies=mapOf("Dominican Republic" to "DOP","United States" to "USD","United Kingdom" to "GBP","Canada" to "CAD","Mexico" to "MXN","Spain" to "EUR","France" to "EUR","Germany" to "EUR","Japan" to "JPY","Brazil" to "BRL","Argentina" to "ARS","India" to "INR","Australia" to "AUD","Liberia" to "LRD","Myanmar" to "MMK")
    FScreen {
        FHeading("Preferences","Regional defaults and exchange-rate settings")
        FCard {
            FPicker("Country / region",country,countries){selected->onCountry(selected);onCurrency(currencies[selected]?: "USD")}
            FResult("Default currency",currency)
            Text("Measurement system",color=C_MUTED,fontSize=11.sp)
            Text(if(metric)"Metric · km, m, kg, L, °C" else "Imperial · mi, ft, lb, gal, °F",color=C_AQUA)
        }
        Spacer(Modifier.height(12.dp))
        FCard {
            Text("CURRENCY DATA",color=C_AQUA,fontSize=11.sp,fontWeight=FontWeight.Bold)
            Text("Exchange rates are fetched from open.er-api.com, cached locally, and refreshed weekly by Android WorkManager. The last successful rates remain available offline.",color=C_MUTED,fontSize=12.sp)
            Text("Rates are indicative and can differ from bank, card-provider or cash-exchange rates.",color=C_MUTED,fontSize=10.sp)
        }
        Spacer(Modifier.height(12.dp))
        FCard {
            Text("ABOUT GLASSCALC",color=C_AQUA,fontSize=11.sp,fontWeight=FontWeight.Bold)
            Text("Native Android app for Android 11 and newer. Regional preferences and cached exchange rates are stored on this device.",color=C_MUTED,fontSize=12.sp)
        }
        Spacer(Modifier.height(16.dp))
    }
}
