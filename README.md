# 📱 Offline POS (Point of Sale)

A **100% offline**, native Android Point of Sale (POS) and inventory management application built for cafes, coffee shops, food stands, and retail businesses. Developed using modern Android technologies: **Kotlin**, **Jetpack Compose**, **Material 3**, **Room SQLite Database**, and **MVVM Architecture**.

---

## ✨ Features & Highlights

### 🚀 1. Animated Splash Screen
* **Brand Entrance**: Smooth opening splash screen featuring the official **UMNX logo**.
* **Compose Animations**: Built entirely in Jetpack Compose with hardware-accelerated fade-in and subtle scale-up animations before transitioning into the main dashboard.

### 🛒 2. Register & Point of Sale
* **Adaptive Catalog Grid**: Adapts to phone screens (2–3 columns) and tablet/foldable screens (3–6 columns) using Compose `GridCells.Adaptive`.
* **Live Product Search & Category Chips**: Instant search by product name or SKU, and category filter chips.
* **Interactive Cart**: Slide-up bottom sheet with instant `+` / `-` quantity controls, subtotals, and total amount calculation.
* **Payment Methods**:
  * **Cash Payment**: Live change calculator (`Change = Cash Received - Total Amount`) with quick-cash amount chips.
  * **GCash Payment**: Record GCash sales with optional reference number logging.
  * **Save as UNPAID**: Save order as `UNPAID` for credit/charge sales to be settled later.

### 📋 3. Unpaid Orders System
* **Outstanding Balance Summary**: Live calculation of total pending unpaid balance.
* **Itemized Receipt Preview**: Expand any unpaid order card to view full item breakdown (`quantity × product price = subtotal`).
* **Settlement Modal**: Easily settle unpaid orders via Cash (with change calculator) or GCash (with reference number).

### 📊 4. Sales History & Void Management
* **Transaction Log**: Search and view past transactions with order numbers, timestamps, total amounts, and payment methods.
* **Status Badges**: High-contrast `PAID`, `UNPAID`, and `VOIDED` badges formatted for both Light and Dark themes.
* **Void Order**: Void transactions with confirmation dialogs (retains records marked as `VOIDED` and excludes them from sales totals).
* **Permanent Delete**: Completely remove duplicate or test orders from the database.

### 💸 5. Business Expense Tracker
* **Expense Categories**: Log expenses under presets (*Supplies*, *Ingredients*, *Transportation*, *Electricity*, *Cleaning*, *Equipment*, *Other*).
* **Payment Method Tracking**: Track expenses paid via **Cash** or **GCash**.
* **Date Filtering**: Filter expenses by date with a built-in calendar picker.

### 📈 6. Daily Summary & Financial Analytics
* **Period Filter Selector**: Quickly toggle report periods: **Today**, **Yesterday**, **This Week**, **This Month**, **All-Time**, or **Custom Date Range**.
* **Financial Calculations**:
  * **Total Sales** (Cash Sales vs GCash Sales)
  * **Total Expenses** (Cash Expenses vs GCash Expenses)
  * **Cash Net / Cash on Hand** = `Cash Sales - Cash Expenses`
  * **GCash Net** = `GCash Sales - GCash Expenses`
  * **Unpaid Total**
* **Top Products Sold**: View ranked top products sold by total quantity (`#1 Top Product`, `#2`, `#3`, etc.).

### 📦 7. Inventory & Raw Materials Management
* **Products & Raw Materials**: Manage sellable products (shown in Register) and raw materials (inventory tracking only).
* **Measurement Units**: Support for units (`pcs`, `kg`, `g`, `L`, `mL`, `box`).
* **Low Stock Warnings**: Visual low stock indicators for items with `<= 5` quantity.
* **Active Status Toggle**: Easily enable or disable items without losing sales records.

### 🎨 8. Adaptive UI & Dark Mode Support
* **Dark / Light Mode**: Built-in dark theme toggle with high-contrast, theme-aware colors across all cards, text labels, and badges.
* **Edge-to-Edge & System Bar Protection**: Protected layout insets (`WindowInsets.systemBars`) ensuring zero UI overlapping with camera punch-holes, status bars, or gesture navigation bars on all Android phone and tablet screens.

---

## 🛠️ Tech Stack

* **Language**: [Kotlin](https://kotlinlang.org/)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/)
* **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
* **Database**: [Room SQLite Database](https://developer.android.com/training/data-storage/room)
* **Concurrency**: Kotlin Coroutines, `StateFlow`, and `SharedFlow`
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
│   │   └── PosDatabase.kt          # Room Database Instance (v2)
│   └── repository/
│       └── PosRepository.kt        # Unified POS Repository
├── domain/
│   └── model/
│       └── CartItem.kt             # Active Cart Item Model
├── ui/
│   ├── register/                   # POS Register & Cart UI
│   ├── unpaid/                     # Unpaid Orders UI
│   ├── sales/                      # Sales History & Void UI
│   ├── expenses/                   # Expense Tracker UI
│   ├── summary/                    # Daily Summary Dashboard UI
│   ├── inventory/                  # Product & Raw Material Management UI
│   └── theme/                      # Material 3 Color, Type & Theme
└── MainActivity.kt                 # Animated Splash Screen & Main Navigation
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
* **Local Storage**: All product catalog items, transaction records, and expenses are persisted locally in the device's Room SQLite database.

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
