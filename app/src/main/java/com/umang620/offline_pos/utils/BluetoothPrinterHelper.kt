package com.umang620.offline_pos.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class PrinterType {
    BLUETOOTH,
    USB
}

data class PrinterDevice(
    val name: String,
    val address: String,
    val type: PrinterType = PrinterType.BLUETOOTH
)

object BluetoothPrinterHelper {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun getAvailablePrinters(context: Context): List<PrinterDevice> {
        val list = mutableListOf<PrinterDevice>()

        // 1. Bluetooth Paired Printers
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        if (adapter != null && adapter.isEnabled) {
            try {
                adapter.bondedDevices?.forEach { device ->
                    list.add(
                        PrinterDevice(
                            name = "${device.name ?: "Unknown"} [Bluetooth]",
                            address = device.address,
                            type = PrinterType.BLUETOOTH
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // 2. USB Connected Thermal Printers
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        usbManager?.deviceList?.values?.forEach { usbDevice ->
            val isPrinter = isUsbPrinter(usbDevice)
            if (isPrinter) {
                val devName = if (usbDevice.productName.isNullOrBlank()) "USB Thermal Printer" else usbDevice.productName!!
                list.add(
                    PrinterDevice(
                        name = "$devName [USB Cable]",
                        address = "USB:${usbDevice.vendorId}:${usbDevice.productId}",
                        type = PrinterType.USB
                    )
                )
            }
        }

        return list
    }

    private fun isUsbPrinter(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) return true
        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) return true
        }
        return true // Fallback to treat attached USB device as printer candidate
    }

    @SuppressLint("MissingPermission")
    fun printTestReceipt(context: Context, deviceAddress: String, printerType: PrinterType = PrinterType.BLUETOOTH): Result<String> {
        if (deviceAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("No printer selected! Please select a Bluetooth or USB printer in Settings."))
        }

        return if (printerType == PrinterType.USB || deviceAddress.startsWith("USB:")) {
            printUsbTestReceipt(context, deviceAddress)
        } else {
            printBluetoothTestReceipt(context, deviceAddress)
        }
    }

    private fun printUsbTestReceipt(context: Context, deviceAddress: String): Result<String> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: return Result.failure(IllegalStateException("USB Service not available on this device."))

        val targetDevice = usbManager.deviceList.values.find {
            "USB:${it.vendorId}:${it.productId}" == deviceAddress
        } ?: usbManager.deviceList.values.firstOrNull()

        if (targetDevice == null) {
            return Result.failure(Exception("USB Printer not found! Check USB OTG cable connection."))
        }

        if (!usbManager.hasPermission(targetDevice)) {
            return Result.failure(Exception("USB Permission Denied! Re-plug USB cable and allow permission prompt."))
        }

        var usbInterface: UsbInterface? = null
        var bulkOutEndpoint: UsbEndpoint? = null

        for (i in 0 until targetDevice.interfaceCount) {
            val iface = targetDevice.getInterface(i)
            for (j in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(j)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK && ep.direction == UsbConstants.USB_DIR_OUT) {
                    usbInterface = iface
                    bulkOutEndpoint = ep
                    break
                }
            }
            if (bulkOutEndpoint != null) break
        }

        if (usbInterface == null || bulkOutEndpoint == null) {
            return Result.failure(Exception("USB Printer endpoint not compatible."))
        }

        val connection = usbManager.openDevice(targetDevice)
            ?: return Result.failure(Exception("Failed to open USB connection to printer."))

        return try {
            connection.claimInterface(usbInterface, true)

            val bytes = buildTestReceiptBytes("USB Thermal Printer")
            val bytesTransferred = connection.bulkTransfer(bulkOutEndpoint, bytes, bytes.size, 5000)

            connection.releaseInterface(usbInterface)
            connection.close()

            if (bytesTransferred >= 0) {
                Result.success("Printed successfully over USB Cable!")
            } else {
                Result.failure(Exception("USB Cable transfer failed."))
            }
        } catch (e: Exception) {
            connection.close()
            Result.failure(Exception("USB Print Error: ${e.message}"))
        }
    }

    @SuppressLint("MissingPermission")
    private fun printBluetoothTestReceipt(context: Context, deviceAddress: String): Result<String> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        if (adapter == null || !adapter.isEnabled) {
            return Result.failure(IllegalStateException("Bluetooth is OFF! Please enable Bluetooth on your device."))
        }

        return try {
            val device = adapter.getRemoteDevice(deviceAddress)
            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)

            socket.connect()
            val outputStream: OutputStream = socket.outputStream

            val bytes = buildTestReceiptBytes(device.name ?: "Bluetooth Printer")
            outputStream.write(bytes)
            outputStream.flush()

            try {
                outputStream.close()
                socket.close()
            } catch (_: Exception) {}

            Result.success("Printed successfully on ${device.name}!")
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to Bluetooth printer! Ensure power is ON and in range."))
        }
    }

    private fun buildTestReceiptBytes(printerName: String): ByteArray {
        val stream = ByteArrayOutputStream()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date())

        val escInit = byteArrayOf(0x1B, 0x40)
        val escAlignCenter = byteArrayOf(0x1B, 0x61, 0x01)
        val escAlignLeft = byteArrayOf(0x1B, 0x61, 0x00)
        val escCutPaper = byteArrayOf(0x1D, 0x56, 0x41, 0x03)

        stream.write(escInit)
        stream.write(escAlignCenter)
        stream.write("================================\n".toByteArray(Charsets.US_ASCII))
        stream.write("       OFFLINE POS v2.0.0       \n".toByteArray(Charsets.US_ASCII))
        stream.write("     THERMAL PRINTER TEST       \n".toByteArray(Charsets.US_ASCII))
        stream.write("================================\n".toByteArray(Charsets.US_ASCII))
        stream.write(escAlignLeft)
        stream.write("Printer: $printerName\n".toByteArray(Charsets.US_ASCII))
        stream.write("Date: $dateStr\n".toByteArray(Charsets.US_ASCII))
        stream.write("Status: Connected & Ready\n".toByteArray(Charsets.US_ASCII))
        stream.write("================================\n".toByteArray(Charsets.US_ASCII))
        stream.write(escAlignCenter)
        stream.write("Test Print Successful!\n\n\n\n".toByteArray(Charsets.US_ASCII))
        stream.write(escCutPaper)

        return stream.toByteArray()
    }
}
