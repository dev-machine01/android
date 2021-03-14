package com.journeyapps.barcodescanner

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.google.zxing.MultiFormatReader
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.DecodeFormatManager
import com.google.zxing.client.android.DecodeHintManager
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.camera.CameraParametersCallback
import com.journeyapps.barcodescanner.camera.CameraSettings

/**
 * Encapsulates BarcodeView, ViewfinderView and status text.
 *
 * To customize the UI, use BarcodeView and ViewfinderView directly.
 */
class DecoratedBarcodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    var barcodeView: BarcodeView

    var viewFinder: ViewfinderView
        private set

    var statusView: TextView? = null
        private set

    /**
     * The instance of @link TorchListener to send events callback.
     */
    var torchListener: TorchListener? = null

    /**
     * Convenience method to initialize camera id, decode formats and prompt message from an intent.
     *
     * @param intent the intent, as generated by IntentIntegrator
     */
    fun initializeFromIntent(intent: Intent) {
        // Scan the formats the intent requested, and return the result to the calling activity.
        val decodeFormats = DecodeFormatManager.parseDecodeFormats(intent)
        val decodeHints = DecodeHintManager.parseDecodeHints(intent)
        val settings = CameraSettings()
        if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
            val cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1)
            if (cameraId >= 0) {
                settings.requestedCameraId = cameraId
            }
        }
        val customPromptMessage = intent.getStringExtra(Intents.Scan.PROMPT_MESSAGE)
        customPromptMessage?.let { setStatusText(it) }

        // Check what type of scan. Default: normal scan
        val scanType = intent.getIntExtra(Intents.Scan.SCAN_TYPE, 0)
        val characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET)
        val reader = MultiFormatReader()
        reader.setHints(decodeHints)
        barcodeView.cameraSettings = settings
        barcodeView.decoderFactory = DefaultDecoderFactory(decodeFormats, decodeHints, characterSet, scanType)
    }

    fun setStatusText(text: String?) {
        // statusView is optional when using a custom layout
        statusView?.text = text
    }

    /**
     * @see BarcodeView.pause
     */
    fun pause() {
        barcodeView.pause()
    }

    /**
     * @see BarcodeView.pauseAndWait
     */
    fun pauseAndWait() {
        barcodeView.pauseAndWait()
    }

    /**
     * @see BarcodeView.resume
     */
    fun resume() {
        barcodeView.resume()
    }

    /**
     * @param callback object to call
     * @see BarcodeView.decodeSingle
     */
    fun decodeSingle(callback: BarcodeCallback) {
        barcodeView.decodeSingle(WrappedCallback(callback))
    }

    /**
     * @param callback object to call
     * @see BarcodeView.decodeContinuous
     */
    fun decodeContinuous(callback: BarcodeCallback) {
        barcodeView.decodeContinuous(WrappedCallback(callback))
    }

    /**
     * Turn on the device's flashlight.
     */
    fun setTorchOn() {
        barcodeView.setTorch(true)
        torchListener?.onTorchOn()
    }

    /**
     * Turn off the device's flashlight.
     */
    fun setTorchOff() {
        barcodeView.setTorch(false)
        torchListener?.onTorchOff()
    }

    /**
     * Changes the settings for Camera.
     * Must be called after [.resume].
     *
     * @param callback [CameraParametersCallback]
     */
    fun changeCameraParameters(callback: CameraParametersCallback) {
        barcodeView.changeCameraParameters(callback)
    }

    /**
     * Handles focus, camera, volume up and volume down keys.
     *
     *
     * Note that this view is not usually focused, so the Activity should call this directly.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_FOCUS, KeyEvent.KEYCODE_CAMERA -> return true // Handle these events so they don't launch the Camera app
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                setTorchOff()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                setTorchOn()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * The Listener to torch/flashlight events (turn on, turn off).
     */
    interface TorchListener {
        fun onTorchOn()

        fun onTorchOff()
    }

    private inner class WrappedCallback(private val delegate: BarcodeCallback) : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            delegate.barcodeResult(result)
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {
            for (point in resultPoints) {
                viewFinder.addPossibleResultPoint(point)
            }
            delegate.possibleResultPoints(resultPoints)
        }
    }

    init {
        // Get attributes set on view
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.zxing_view)
        val scannerLayout = attributes.getResourceId(
            R.styleable.zxing_view_zxing_scanner_layout, R.layout.zxing_barcode_scanner
        )
        attributes.recycle()
        inflate(context, scannerLayout, this)
        barcodeView = findViewById(R.id.zxing_barcode_surface) ?: throw NullPointerException(
            "There is no a com.journeyapps.barcodescanner.BarcodeView on provided layout " +
                    "with the id \"zxing_barcode_surface\"."
        )

        // Pass on any preview-related attributes
        barcodeView.initializeAttributes(attrs)

        viewFinder = findViewById(R.id.zxing_viewfinder_view) ?: throw NullPointerException(
            "There is no a com.journeyapps.barcodescanner.ViewfinderView on provided layout " +
                    "with the id \"zxing_viewfinder_view\"."
        )
        viewFinder.cameraPreview = barcodeView

        // statusView is optional
        statusView = findViewById<View>(R.id.zxing_status_view) as TextView?
    }
}