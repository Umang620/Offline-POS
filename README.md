# Cafe Mocktails — Offline Point of Sale (POS)

A 100% offline, native Android Point of Sale (POS) application built for cafes, coffee shops, and retail stores. Developed using modern Android technologies including **Kotlin**, **Jetpack Compose**, **Material 3**, **Room SQLite Database**, and **MVVM Architecture**.

---

## 📱 Features

### 1. 🛒 Register & Point of Sale (POS)
* **Adaptive Catalog Grid**: Automatically adapts to phones (2 columns) and tablets (3–6 columns) using Compose `GridCells.Adaptive`.
* **Live Search & Category Filtering**: Quickly search products by name or SKU, or filter by product categories.
* **Interactive Cart**: Slide-up bottom sheet with quick quantity adjusters (`+` / `-`), itemized subtotals, and total amount calculation.
* **Cash Payment & Change Calculator**: Input cash received with live **Change calculation** (`Change = Cash Received - Total Amount`) and insufficient cash validation.
* **GCash Payment Logging**: Log GCash transactions with optional GCash reference number recording.
* **Save as Unpaid**: Easily log orders as `UNPAID` for later payment settlement.

### 2. 📋 Unpaid Orders System
* **Unpaid Balance Summary**: Overview of total unpaid customer balance.
* **Expandable Order Preview**: Click any unpaid order to expand and preview itemized receipt breakdowns (`quantity x product = ₱ subtotal`).
* **Settlement Modal**: Settle unpaid orders via Cash (with change calculation) or GCash (with reference number logging).

### 3. 📊 Sales Log & Transaction History
* **Transaction History**: View all completed sales with order numbers, dates, times, total amounts, and payment methods.
* **Status Tracking**: Clear status badges (`PAID`, `VOIDED`).
* **Void Order**: Void legitimate transactions with confirmation dialogs (retains the transaction record marked as `VOIDED` and excludes it from sales totals).
* **Permanent Delete**: Completely remove accidental, test, or duplicate orders and related order items from the database.

### 4. 💸 Expense Tracker
* **Category Tracking**: Record business expenses under preset categories:
  * *Beans*, *Ice*, *Transportation*, *Electricity*, *Cleaning supplies*, *Ingredients*, *Other*.
* **Method Breakdown**: Separate expenses paid via **Cash** vs **GCash**.

### 5. 📈 Daily Summary & Analytics Dashboard
* **Real-time Financial Calculations**:
  * **Cash Sales** & **GCash Sales**
  * **Cash Expenses** & **GCash Expenses**
  * **Total Sales** & **Total Expenses**
  * **Cash on Hand** = `Cash Sales - Cash Expenses`
  * **GCash Recorded** = `GCash Sales - GCash Expenses`
  * **Unpaid Total Balance**

### 6. 📦 Inventory Management
* **Stock Control**: Live product stock tracking with visual **Low Stock Warnings** (<= 5 items).
* **Active / Inactive Status Toggle**: Toggle products active or inactive. Inactive products are automatically hidden from the Register screen while preserving sales history.
* **Add & Edit Forms**: Form dialogs for updating product names, prices in Pesos (`₱`), categories, stock quantities, and SKUs.

---

## 🛠️ Tech Stack

* **Language**: [Kotlin](https://kotlinlang.org/)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/)
* **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
* **Database**: [Room SQLite Database](https://developer.android.com/training/data-storage/room) (Version 2)
* **Concurrency**: Kotlin Coroutines & StateFlow / SharedFlow
* **Image/Icon System**: Material Extended AutoMirrored Icons

---

## 📂 Project Structure

```
com.umang620.offline_pos/
├── data/
│   ├── local/
│   │   ├── ProductEntity.kt        # Room Product Entity
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
│   ├── inventory/                  # Product Management UI
│   └── theme/                      # Material 3 Color & Theme
└── MainActivity.kt                 # Main Scaffold & Bottom Navigation
```

---

## 🚀 Getting Started

### Requirements
* **Android Studio**: Ladybug (2024.2.1) or newer
* **JDK**: 17 or higher
* **Min Android SDK**: API 26 (Android 8.0 Oreo)
* **Target Android SDK**: API 35 / Compile SDK 37

### Installation
1. Clone or download this repository to your local machine:
   ```bash
   git clone https://github.com/your-username/Offline-POS.git
   ```
2. Open **Android Studio** and select **Open**, navigating to the project folder.
3. Allow Gradle to sync dependencies automatically.
4. Connect an Android phone/tablet via USB (with USB Debugging enabled) or start an Android Emulator (API 26+).
5. Click **Run (►)** or press `Shift + F10` to build and launch the application.

---

## 🔒 Data Privacy & Offline Security

* **100% Offline**: Operates completely without internet, cloud backend, Firebase, or external servers.
* **Data Security**: All store sales, product inventory, and expense records are stored locally in the device's internal SQLite database.
* **Git Safety**: Contains a pre-configured `.gitignore` excluding sensitive keystores, build caches, and local machine properties.

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
