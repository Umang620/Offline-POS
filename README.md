# Offline POS (Point of Sale) — Version 2.0.0

A **100% offline**, native Android Point of Sale (POS), Inventory, and Financial Management application engineered for cafes, restaurants, food stands, and retail businesses. Built using modern Android architecture: **Kotlin**, **Jetpack Compose**, **Material 3**, **Room SQLite Database**, and **MVVM Architecture**.

---

## System Capabilities

### Point of Sale & Order Register
* **Adaptive Catalog Grid**: Dynamically scales columns for phones (2–3 columns) and tablets/foldables (3–6 columns).
* **Search & Categorization**: Real-time product search by name/SKU and instant category filtering.
* **Interactive Cart Management**: Slide-up bottom sheet with real-time item quantity controls, subtotals, and total calculations.
* **Flexible Checkout Options**:
  * **Cash Payment**: Live change calculator (`Change = Cash Received - Total`) with quick-amount chips.
  * **GCash Digital Payment**: Record GCash sales with reference number tracking.
  * **Unpaid Charge Sales**: Save transactions as `UNPAID` for credit/charge accounts to be settled later.

### Dual Thermal Receipt Printing Engine
* **Universal ESC/POS Protocol**: Formats and transmits thermal receipts over **USB OTG Cable** and **Bluetooth SPP**.
* **Device Selector & Auto-Connect**: Configure your active printer once in Settings for automatic connection.
* **Non-Blocking Background Printing**: Receipt byte transmission executes asynchronously on a background thread (`Dispatchers.IO`), keeping the POS interface responsive without delays.
* **Configurable Auto-Print**: Toggle `Auto-Print Receipt on Checkout [ON/OFF]` in Settings.
* **Dynamic Receipt Layouts**:
  * **Official Receipt (PAID)**: Printed for completed cash/GCash sales and settled unpaid bills.
  * **Charge Slip (UNPAID)**: Printed when saving new unpaid orders with customer/table name and pending balance.
  * **Updated Charge Slip**: Printed when modifying items in an existing unpaid order.
* **On-Demand Manual Reprinting**: Reprint buttons inside Sales History and Unpaid Order cards.

### Hardware & Printer Compatibility
* **Command Protocol**: ESC/POS (Epson Standard Code for POS).
* **Interfaces**: Wireless Bluetooth SPP and USB OTG Direct Cable.
* **Media Sizes**: Standard 58mm (2-inch) and 80mm (3-inch) thermal receipt paper.
* **Hardware Brands**: Xprinter, Goojprt, MUNBYN, Sunmi, POS-58, ZJ-5809, Netum, Epson, Rongta, Milestone, and generic ESC/POS mini thermal printers.

### Manager Security & Fraud Prevention
* **4-Digit Manager PIN**: Restricts sensitive operations:
  * Permanent transaction deletion
  * Default starting cash float modifications
  * Security settings toggle switches
* **PIN Modification Security**: Changing an existing PIN requires entering the **Current PIN** and **confirming the New 4-digit PIN**.
* **Configurable Void Security**: Optional toggle switch (*Require PIN for Void Orders*) to allow cashier speed or enforce manager authorization.

### Shift Cash Drawer Reconciliation
* **End-of-Shift Reconciliation**: Reconcile physical cash drawer contents at shift close.
* **Calculated Expected Cash**:
  $$\text{Expected Cash} = \text{Starting Float} + \text{Cash Sales Only} - \text{Cash Expenses Only}$$
* **Audit Detection**: Automatically detects and flags **Exact Match**, **Shortage**, or **Overage**.
* **Override Security**: Requires Manager PIN authorization to modify the starting cash float.

### Financial Reporting & CSV Export
* **Live Revenue Badge**: Top header displays real-time **Today's Revenue** (`Today: ₱X.XX`) bound to current calendar date (`System.currentTimeMillis()`).
* **Net Profit Dashboard**: Executive calculation banner (`Net Profit = Sales − Expenses`) inside Summary.
* **1-Tap Voided Audit Modal**: Tappable audit badge in Summary displaying itemized voided order lists, product quantities, and timestamps.
* **Direct Downloads CSV Export**: Exports sales and expense reports directly to the device's public `Downloads/` directory for **Microsoft Excel** or **Google Sheets**. Supports custom date ranges (*e.g., Nov 1 to Nov 15*).

### Inventory & Expense Management
* **Products & Raw Materials**: Track sellable catalog items and raw inventory materials.
* **Measurement Units**: Support for units (`pcs`, `kg`, `g`, `L`, `mL`, `box`).
* **Low-Stock Alerts**: Visual warning indicators for items with `<= 5` quantity.
* **Categorized Expense Tracker**: Log business expenses under presets (*Supplies*, *Ingredients*, *Transportation*, *Electricity*, *Equipment*, *Other*).

### Adaptive UI & Design Architecture
* **High-Contrast Text Fields**: Outlined input fields featuring primary borders, soft tinted background fills, and currency indicators.
* **Dark / Light Theme**: Built-in theme toggle with high-contrast, theme-aware styling across all components.
* **Edge-to-Edge Protection**: Layout insets (`WindowInsets.statusBars`) prevent UI overlapping with notches, status bars, or gesture navigation bars across all phone and tablet form factors.

---

## Technical Stack

* **Language**: [Kotlin](https://kotlinlang.org/)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/)
* **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
* **Database**: [Room SQLite Database](https://developer.android.com/training/data-storage/room)
* **Concurrency**: Kotlin Coroutines, `StateFlow`, and `SharedFlow`
* **Hardware Connectivity**: Android Bluetooth SPP & USB OTG Direct Transfer

---

## Getting Started

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer
* **JDK**: 17 or higher
* **Minimum SDK**: API 26 (Android 8.0 Oreo)
* **Target SDK**: API 35 (Android 15)

### Building & Running
1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/Offline-POS.git
   ```
2. Open **Android Studio** and select **Open**, choosing the `Offline-POS` directory.
3. Allow Gradle to sync project dependencies.
4. Connect an Android device or start an emulator.
5. Click **Run (►)** or press `Shift + F10`.

---

## Security & Offline Architecture

* **100% Offline Operation**: Functions without internet connectivity, cloud servers, or third-party APIs.
* **Local Persistence**: All catalog items, order records, expenses, and security PINs are persisted locally in the Room SQLite database and encrypted SharedPreferences.
* **Data Preservation**: Application version upgrades preserve all existing user data.

---

## License
This project is open-source and available under the [MIT License](LICENSE).
