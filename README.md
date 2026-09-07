# 📱 Offline POS (Point of Sale) — Version 2.0.0

A **100% offline**, native Android Point of Sale (POS), Inventory, and Financial Management application built for cafes, coffee shops, food stands, and retail businesses. Developed using modern Android technologies: **Kotlin**, **Jetpack Compose**, **Material 3**, **Room SQLite Database**, and **MVVM Architecture**.

---

## ✨ Version 2.0.0 Features & Highlights

### 🖨️ 1. Dual Thermal Printer Engine (USB Cable & Bluetooth)
* **Dual Connection Support**: Supports ESC/POS 58mm and 80mm thermal receipt printers over **Bluetooth SPP** and **USB OTG Direct Cable**.
* **Printer Device Selector & Auto-Connect**: Select your active Bluetooth or USB printer once in Settings for automatic connection on every print.
* **Real Connection Error Detection**: Real-time error handling on Test Print and checkout receipts:
  * ❌ *Bluetooth OFF / Unreachable*
  * ❌ *USB Cable Disconnected / Permission Denied*
  * ❌ *No Printer Selected*
  * ✅ *Printed Successfully*
* **Step-by-Step Printer Help Guide**: Built-in 3-step setup guide with a **"Test Print"** action button.

### 🔒 2. Manager PIN Security & Anti-Cheating Controls
* **4-Digit Manager PIN**: Protects sensitive store operations:
  * **Permanent Order Deletion**: Requires Manager PIN to permanently delete transactions.
  * **Cash Float Modifications**: Requires Manager PIN to alter default starting cash float.
  * **Security Settings Toggles**: Requires Manager PIN to change security toggle switches.
* **Configurable Void Security**: Toggle switch in Settings (*`Require PIN for Void Orders` [ ON / OFF ]*) so voiding can be fast for cashiers or locked for security.
* **Current PIN & Confirmation Validation**: Changing an existing PIN requires entering the **Current PIN** and **confirming the New 4-digit PIN**.

### 💵 3. Shift Cash Drawer Reconciliation
* **End-of-Shift Reconciliation**: Tap **"Reconcile Cash Drawer"** on the Summary screen to enter physical counted cash.
* **Automatic Expected Cash Calculation**:
  $$\text{Expected Cash} = \text{Starting Float} + \text{Cash Sales} - \text{Cash Expenses}$$
* **Instant Audit Result**: Automatically detects and highlights:
  * ✅ **Exact Match**
  * 🔴 **Shortage** (*e.g., −₱50.00*)
  * 🟠 **Overage** (*e.g., +₱50.00*)
* **Manager PIN Protection**: Prompts for Manager PIN if staff attempts to override the starting cash float during reconciliation.

### 📊 4. 1-Tap Voided Audit Modal & Summary Badge
* **Voided Audit Row**: Displays a compact audit badge on the Financial Summary card:  
  `"Voided (Audit): X orders (₱Y.YY) • Excluded from sales"`
* **1-Tap Itemized Audit Modal**: Tap the Voided Audit row to inspect full void details:
  * 🕒 **Voided Timestamp** (*e.g., Today at 2:15 PM*)
  * 📦 **Itemized Voided Products & Quantities** (*e.g., 2x Cheeseburger, 1x Iced Coffee*)
  * 💳 **Payment Method** (*Cash / GCash*)

### 📈 5. Live Top Header Revenue & Net Profit Dashboard
* **Today's Live Revenue Badge**: Top-right header displaying live **Today's Revenue** (`Today: ₱X.XX`), bound strictly to today's date (`System.currentTimeMillis()`).
* **Net Profit / Loss Banner**: Prominent green/red calculation banner (`Net Profit = Sales − Expenses`) inside the Summary screen.
* **Period Filter Selector**: Quickly toggle report periods: **Today**, **Yesterday**, **This Week**, **Last Week**, **This Month**, **Last Month**, **This Year**, or **Custom Date Range**.

### 🔎 6. Sales History Search & Filter Chips
* **Real-time Search Bar**: Search transactions by receipt #, order ID, product name, or payment method.
* **Filter Chips**: Quick filter tabs (*`All`, `Cash`, `GCash`, `Voided`*) with horizontal scroll support on small screens.

### 🛒 7. Register & Point of Sale
* **Adaptive Catalog Grid**: Adapts to phone screens (2–3 columns) and tablet/foldable screens (3–6 columns) using Compose `GridCells.Adaptive`.
* **Live Product Search & Category Chips**: Instant search by product name or SKU, and category filter chips.
* **Interactive Cart**: Slide-up bottom sheet with instant `+` / `-` quantity controls, subtotals, and total amount calculation.
* **Payment Methods**:
  * **Cash Payment**: Live change calculator (`Change = Cash Received - Total Amount`) with quick-cash amount chips.
  * **GCash Payment**: Record GCash sales with optional reference number logging.
  * **Save as UNPAID**: Save order as `UNPAID` for credit/charge sales to be settled later.

### 📋 8. Unpaid Orders System
* **Outstanding Balance Summary**: Live calculation of total pending unpaid balance.
* **Itemized Receipt Preview**: Expand any unpaid order card to view full item breakdown (`quantity × product price = subtotal`).
* **Settlement Modal**: Easily settle unpaid orders via Cash (with change calculator) or GCash (with reference number).

### 📦 9. Inventory & Raw Materials Management
* **Products & Raw Materials**: Manage sellable products (shown in Register) and raw materials (inventory tracking only).
* **Measurement Units**: Support for units (`pcs`, `kg`, `g`, `L`, `mL`, `box`).
* **Low Stock Warnings**: Visual low stock indicators for items with `<= 5` quantity.

### 🎨 10. Adaptive UI, Highlights & Dark Mode Support
* **Highlighted Outlined Text Fields**: Input fields feature bold primary outlines, soft tinted background fills, and `₱` currency indicators.
* **Dark / Light Mode**: Built-in dark theme toggle with high-contrast, theme-aware colors across all cards, text labels, and badges.
* **Edge-to-Edge & System Bar Protection**: Protected layout insets (`WindowInsets.statusBars`) ensuring zero UI overlapping with camera punch-holes, status bars, or gesture navigation bars on all Android phone and tablet screens.

---

## 🛠️ Tech Stack

* **Language**: [Kotlin](https://kotlinlang.org/)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/)
* **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
* **Database**: [Room SQLite Database](https://developer.android.com/training/data-storage/room)
* **Concurrency**: Kotlin Coroutines, `StateFlow`, and `SharedFlow`
* **Connectivity**: Android Bluetooth SPP & USB OTG Direct Hardware Transfer
* **Icons**: Material AutoMirrored & Extended Icons

---

## 📂 Project Structure

```
com.umang620.offline_pos/
├── data/
│   ├── local/
│   │   ├── ProductEntity.kt        # Room Product Entity (Products & Raw Materials)
│   │   ├── OrderEntity.kt          # Room Order Entity
│   │   ├── OrderItemEntity.kt      # Room Order Item Entity
│   │   ├── ExpenseEntity.kt        # Room Expense Entity
│   │   ├── OrderWithItems.kt       # Relation Data Model
│   │   ├── ProductDao.kt           # Product Database DAO
│   │   ├── OrderDao.kt             # Order Database DAO
│   │   ├── ExpenseDao.kt           # Expense Database DAO
│   │   └── PosDatabase.kt          # Room Database Instance
│   └── repository/
│       └── PosRepository.kt        # Unified POS Repository
├── domain/
│   └── model/
│       └── CartItem.kt             # Active Cart Item Model
├── utils/
│   ├── FormatUtils.kt              # Currency Formatting Utility
│   └── BluetoothPrinterHelper.kt   # Dual Bluetooth & USB Thermal ESC/POS Engine
├── ui/
│   ├── register/                   # POS Register & Cart UI
│   ├── unpaid/                     # Unpaid Orders UI
│   ├── sales/                      # Sales History & Void UI
│   ├── expenses/                   # Expense Tracker UI
│   ├── summary/                    # Daily Summary Dashboard & Cash Reconciliation UI
│   ├── inventory/                  # Product & Raw Material Management UI
│   ├── settings/                   # Settings, Manager PIN & Printer Setup UI
│   └── theme/                      # Material 3 Color, Type & Theme
└── MainActivity.kt                 # Animated Splash Screen, Top Header & Navigation
```

---

## 🚀 Getting Started

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
2. Open **Android Studio** and select **Open**, choosing the `Offline-POS` folder.
3. Allow Gradle to finish syncing project dependencies.
4. Connect an Android device (with USB Debugging enabled) or start an Android Emulator.
5. Click **Run (►)** or press `Shift + F10`.

---

## 🔒 Offline & Data Security

* **100% Offline**: Requires no internet connection, cloud server, or external API dependencies.
* **Local Storage**: All product catalog items, transaction records, expenses, and manager security settings are persisted locally in the device's Room SQLite database and encrypted SharedPreferences.
* **Data Migration**: Upgrading version codes preserves all existing saved products, orders, and expenses.

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
