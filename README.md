# GlassCalc

Native Kotlin Android calculator built with Jetpack Compose. Supports Android 11 (API 30) and newer.

## Build

Open **Actions → Build Android APK → Run workflow**. The workflow is manual-only and uploads a debug APK artifact.

## Features

- Standard and scientific calculator with expression history, parentheses, powers, modulo, square roots, trigonometry (degrees), and logarithms.
- Financial calculators:
  - Simple and compound interest with configurable compounding frequency.
  - Loan amortization estimates with monthly payment, total repayment, and total interest.
  - Reverse compound-interest rate, ROI, investment growth with recurring monthly contributions, present value, and future value.
  - Savings-goal calculator for the monthly contribution needed to reach a target.
  - VAT/tax calculator with tax-exclusive and tax-inclusive modes.
- Unit conversions:
  - Length, weight/mass, volume, temperature, speed, time, area, digital storage, pressure, and energy.
- Currency conversion using USD-base exchange rates from open.er-api.com, cached locally and refreshed weekly in the background.
- Regional defaults: Dominican Republic starts with DOP and metric units. Region selection updates the default currency and measurement system.
- Everyday tools: BMI reference calculator, age calculator, date difference and date-offset calculator, percentage calculator, and tip/bill splitter.
- Dark glass-inspired Compose interface with animated navigation.

## Notes

- Currency rates are indicative and may differ from bank, card-provider, or cash-exchange rates. The last successful rates are retained for offline use.
- Loan and investment figures are estimates. Loan calculations assume fixed monthly payments and exclude fees; investment projections are not guarantees.
- BMI is a screening measure, not a diagnosis. Adult reference ranges are not intended for children or teenagers.
- Java 17, Android Gradle Plugin 9.1.1, Kotlin 2.4.21, and Gradle 9.3.1.
