# GlassCalc

Native Android calculator built with Kotlin and Jetpack Compose. Minimum Android version: Android 11 (API 30).

## Build
Open **Actions → Build Android APK → Run workflow**. This workflow is manual-only and uploads a debug APK artifact.

## Features
- Standard and scientific calculator with parentheses, powers, modulo, square roots, trigonometry (degrees), and logarithms.
- Simple and compound interest, loan payment estimate, ROI, and reverse principal using the simple-interest equation.
- Length, weight, volume, speed, time, area, and data conversions.
- Currency conversion using cached USD-base rates, initial fetch, and weekly background refresh.
- Dominican Republic defaults: DOP and metric units.
- BMI and VAT/tax tools.
- Dark glass-inspired Compose UI with animated screen transitions.

## Technical details
- Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Gradle 8.9, Java 17.
- Exchange rates from open.er-api.com. Indicative only; bank/card rates may differ.
- Currency conversion works offline with the last successful cache.
- Loan estimates assume a fixed annual rate, monthly payments, and no fees.
- BMI is an adult screening measure, not a medical diagnosis.
