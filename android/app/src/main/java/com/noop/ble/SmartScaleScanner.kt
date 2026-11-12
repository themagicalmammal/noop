package com.noop.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.noop.analytics.ScaleAdvertisement
import com.noop.analytics.ScaleBroadcastDriver
import com.noop.analytics.ScaleBroadcastResult
import com.noop.analytics.ScaleMeasurement
import com.noop.analytics.ScaleUser
import com.noop.analytics.SmartScaleBroadcastCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Foreground, user-started, receive-only scan for supported scale advertisements. */
class SmartScaleScanner(context: Context) {
    enum class Status { IDLE, SCANNING, STEPPED_ON, COMPLETE, TIMED_OUT, UNAVAILABLE, FAILED }

    data class State(
        val status: Status = Status.IDLE,
        val scaleName: String? = null,
        val liveKg: Double? = null,
        val measurement: ScaleMeasurement? = null,
        val capturedAtMs: Long? = null,
    )

    private val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private val main = Handler(Looper.getMainLooper())
    private var scanner: BluetoothLeScanner? = null
    private var callback: ScanCallback? = null
    private var drivers: List<ScaleBroadcastDriver> = SmartScaleBroadcastCatalog.create()
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val timeout = Runnable {
        stopScan()
        if (_state.value.status == Status.SCANNING || _state.value.status == Status.STEPPED_ON) {
            _state.value = _state.value.copy(status = Status.TIMED_OUT)
        }
    }

    @Suppress("MissingPermission")
    fun start(user: ScaleUser) {
        stopScan()
        drivers = SmartScaleBroadcastCatalog.create()
        _state.value = State(status = Status.SCANNING)
        try {
            val sc = adapter?.bluetoothLeScanner
            if (adapter?.isEnabled != true || sc == null) {
                _state.value = State(status = Status.UNAVAILABLE)
                return
            }
            scanner = sc
            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) = accept(result, user)
                override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach { accept(it, user) }
                override fun onScanFailed(errorCode: Int) {
                    stopScan()
                    _state.value = State(status = Status.FAILED)
                }
            }
            callback = scanCallback
            sc.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback)
            main.postDelayed(timeout, 90_000L)
        } catch (_: SecurityException) {
            stopScan()
            _state.value = State(status = Status.UNAVAILABLE)
        } catch (_: IllegalStateException) {
            stopScan()
            _state.value = State(status = Status.UNAVAILABLE)
        }
    }

    fun stop() {
        stopScan()
        _state.value = State()
    }

    @Suppress("MissingPermission")
    private fun accept(result: ScanResult, user: ScaleUser) {
        val record = result.scanRecord ?: return
        val name = record.deviceName ?: runCatching { result.device.name }.getOrNull().orEmpty()
        val manufacturer = buildMap {
            val sparse = record.manufacturerSpecificData
            for (index in 0 until sparse.size()) put(sparse.keyAt(index), sparse.valueAt(index))
        }
        val ad = ScaleAdvertisement(
            name = name.orEmpty(),
            manufacturerData = manufacturer,
            serviceUuids = record.serviceUuids.orEmpty().map { it.uuid.toString().uppercase().let { uuid -> uuid.substring(4, 8) } }.toSet(),
        )
        for (driver in drivers) {
            if (!driver.matches(ad)) continue
            when (val decoded = driver.handle(ad, user)) {
                ScaleBroadcastResult.Ignored -> Unit
                is ScaleBroadcastResult.Measuring -> {
                    _state.value = State(Status.STEPPED_ON, driver.displayName, decoded.liveKg)
                    return
                }
                is ScaleBroadcastResult.Finished -> {
                    _state.value = State(
                        status = Status.COMPLETE,
                        scaleName = driver.displayName,
                        liveKg = decoded.measurement.weightKg,
                        measurement = decoded.measurement,
                        capturedAtMs = System.currentTimeMillis(),
                    )
                    stopScan()
                    return
                }
            }
        }
    }

    @Suppress("MissingPermission")
    private fun stopScan() {
        main.removeCallbacks(timeout)
        val oldScanner = scanner
        val oldCallback = callback
        scanner = null
        callback = null
        if (oldScanner != null && oldCallback != null) {
            runCatching { oldScanner.stopScan(oldCallback) }
        }
    }
}
