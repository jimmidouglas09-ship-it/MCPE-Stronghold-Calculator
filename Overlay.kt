package com.example.strongholdcalculator

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isExpanded = false

    // UI Components
    private lateinit var btnToggle: Button
    private lateinit var expandedLayout: LinearLayout
    private lateinit var etX1: EditText
    private lateinit var etZ1: EditText
    private lateinit var etX2: EditText
    private lateinit var etZ2: EditText
    private lateinit var etPixelChange: EditText
    private lateinit var btnCalculate: Button
    private lateinit var btnClose: Button
    private lateinit var tvResults: TextView

    private val strongholdCalculator = StrongholdCalculator()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        strongholdCalculator.generateStrongholdCells()
        createOverlay()
    }

    private fun createOverlay() {
        // Create the overlay view
        overlayView = createOverlayView()

        // Set up window parameters
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 50
        params.y = 100

        // Add the view to window manager
        windowManager.addView(overlayView, params)

        // Make it draggable
        makeDraggable(overlayView!!, params)
    }

    private fun createOverlayView(): View {
        val inflater = LayoutInflater.from(this)
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@OverlayService, android.R.drawable.dialog_holo_dark_frame)
            alpha = 0.95f
            setPadding(30, 30, 30, 30)
            minimumWidth = 500
        }

        // Toggle button (always visible)
        btnToggle = Button(this).apply {
            text = "SH"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(80, 60)
            background = ContextCompat.getDrawable(this@OverlayService, android.R.drawable.btn_default_small)
            alpha = 0.8f
            setOnClickListener { toggleExpanded() }
        }
        view.addView(btnToggle)

        // Expanded layout (initially hidden)
        expandedLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(25, 25, 25, 25)
            background = ContextCompat.getDrawable(this@OverlayService, android.R.drawable.dialog_holo_dark_frame)
            minimumWidth = 470
        }

        // Close button (red and prominent)
        btnClose = Button(this).apply {
            text = "✕ CLOSE SPEEDRUN OVERLAY"
            textSize = 12f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#D32F2F"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50
            ).apply {
                bottomMargin = 10
            }
            setOnClickListener {
                stopSelf()
            }
        }
        expandedLayout.addView(btnClose)


        // First position
        val pos1Label = TextView(this).apply {
            text = "Position 1:"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 0, 0, 5)
        }
        expandedLayout.addView(pos1Label)

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 10)
        }
        etX1 = createCompactEditText("X1")
        etZ1 = createCompactEditText("Z1")
        row1.addView(etX1)
        row1.addView(etZ1)
        expandedLayout.addView(row1)

        // Second position
        val pos2Label = TextView(this).apply {
            text = "Position 2:"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 8, 0, 5)
        }
        expandedLayout.addView(pos2Label)

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 10)
        }
        etX2 = createCompactEditText("X2")
        etZ2 = createCompactEditText("Z2")
        row2.addView(etX2)
        row2.addView(etZ2)
        expandedLayout.addView(row2)

        // Pixel change (optional)
        val pixelLabel = TextView(this).apply {
            text = "Pixel Δ (optional):"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 8, 0, 5)
        }
        expandedLayout.addView(pixelLabel)

        etPixelChange = createCompactEditText("Pixel Δ", isFullWidth = true)
        etPixelChange.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            80
        ).apply {
            bottomMargin = 20
        }
        expandedLayout.addView(etPixelChange)

        // Calculate button
        btnCalculate = Button(this).apply {
            text = "CALCULATE STRONGHOLD"
            textSize = 18f
            setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                100
            ).apply {
                topMargin = 25
                bottomMargin = 15
            }
            setOnClickListener {
                android.util.Log.d("StrongholdCalc", "Calculate button clicked!")
                calculateStronghold()
            }
        }
        expandedLayout.addView(btnCalculate)

        // Results
        tvResults = TextView(this).apply {
            textSize = 12f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#2E2E2E"))
            setPadding(15, 15, 15, 15)
            typeface = android.graphics.Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 15
            }
            visibility = View.GONE
        }
        expandedLayout.addView(tvResults)

        view.addView(expandedLayout)
        return view
    }

    private fun createCompactEditText(hint: String, isFullWidth: Boolean = false): EditText {
        return EditText(this).apply {
            this.hint = hint
            textSize = 15f
            setPadding(18, 15, 18, 15)
            layoutParams = if (isFullWidth) {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 80).apply {
                    bottomMargin = 15
                }
            } else {
                LinearLayout.LayoutParams(180, 80).apply {
                    marginEnd = 20
                }
            }
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            background = ContextCompat.getDrawable(this@OverlayService, android.R.drawable.editbox_background_normal)

            // Enable keyboard focus and make it focusable
            isFocusable = true
            isFocusableInTouchMode = true

            // Set input method options to show number keyboard
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_NEXT

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    // Force show keyboard when focused
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded
        if (isExpanded) {
            expandedLayout.visibility = View.VISIBLE
            btnToggle.text = "−"
            btnToggle.alpha = 1.0f

            // Make overlay focusable when expanded so keyboard can work
            overlayView?.let { view ->
                val params = view.layoutParams as WindowManager.LayoutParams
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                windowManager.updateViewLayout(view, params)
            }
        } else {
            expandedLayout.visibility = View.GONE
            btnToggle.text = "SH"
            btnToggle.alpha = 0.8f

            // Make overlay not focusable when collapsed
            overlayView?.let { view ->
                val params = view.layoutParams as WindowManager.LayoutParams
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                windowManager.updateViewLayout(view, params)
            }
        }

        // Update window size
        overlayView?.let { view ->
            val params = view.layoutParams as WindowManager.LayoutParams
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            windowManager.updateViewLayout(view, params)
        }
    }

    private fun makeDraggable(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun calculateStronghold() {
        android.util.Log.d("StrongholdCalc", "calculateStronghold() called")

        try {
            val x1 = etX1.text.toString().toDoubleOrNull()
            val z1 = etZ1.text.toString().toDoubleOrNull()
            val x2 = etX2.text.toString().toDoubleOrNull()
            val z2 = etZ2.text.toString().toDoubleOrNull()
            val pixelChange = etPixelChange.text.toString().toDoubleOrNull()

            android.util.Log.d("StrongholdCalc", "Input values: x1=$x1, z1=$z1, x2=$x2, z2=$z2, pixel=$pixelChange")

            if (x1 == null || z1 == null || x2 == null || z2 == null) {
                android.util.Log.d("StrongholdCalc", "Missing required coordinates")
                showError("Fill all coordinate fields")
                return
            }

            android.util.Log.d("StrongholdCalc", "Calling stronghold calculator...")

            val result = try {
                if (pixelChange != null && pixelChange > 0) {
                    strongholdCalculator.calculateStrongholdWithDistance(x2, z2, x1, z1, 3655.0 / pixelChange)
                } else {
                    strongholdCalculator.calculateStrongholdFromCoordinates(x1, z1, x2, z2)
                }
            } catch (e: Exception) {
                android.util.Log.e("StrongholdCalc", "Calculator error: ${e.message}", e)
                // Show a simple test result to verify display works
                showError("Calculator Error: ${e.message}")
                return
            }

            android.util.Log.d("StrongholdCalc", "Calculator returned result")

            displayResults(result)

        } catch (e: Exception) {
            android.util.Log.e("StrongholdCalc", "Error in calculateStronghold: ${e.message}", e)
            showError("Error: ${e.message}")
        }
    }

    private fun displayResults(result: TriangulationResult) {
        android.util.Log.d("StrongholdCalc", "Displaying results with ${result.candidates.size} candidates")

        if (result.candidates.isEmpty()) {
            showError("No candidates found")
            return
        }

        val best = result.candidates[0]
        val sb = StringBuilder()

        // Format the best result
        sb.append("BEST STRONGHOLD:\n")
        sb.append("Coordinates: (${best.projectionX}, ${best.projectionZ})\n")
        sb.append("Nether: (${best.netherX}, ${best.netherZ})\n")
        sb.append("Probability: ${String.format("%.1f%%", best.conditionalProb * 100)}\n")
        sb.append("Distance: ${best.distance} blocks\n\n")

        // Show top 3 candidates
        val maxCandidates = kotlin.math.min(3, result.candidates.size)
        sb.append("Top ${maxCandidates} Results:\n")
        for (i in 0 until maxCandidates) {
            val c = result.candidates[i]
            sb.append("${i+1}. (${c.projectionX}, ${c.projectionZ}) - ")
            sb.append("${String.format("%.1f%%", c.conditionalProb * 100)}\n")
        }

        val resultText = sb.toString()
        android.util.Log.d("StrongholdCalc", "Setting result text: $resultText")

        // Set the results and make visible
        tvResults.text = resultText
        tvResults.visibility = View.VISIBLE

        // Hide input elements after calculation
        hideInputs()

        // Force update the overlay layout
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            overlayView?.let { view ->
                val params = view.layoutParams as WindowManager.LayoutParams
                windowManager.updateViewLayout(view, params)
            }
        }

        android.util.Log.d("StrongholdCalc", "Results display completed")
    }

    private fun hideInputs() {
        // Hide input fields but keep results, close button and toggle button
        val inputViews = listOf(
            expandedLayout.getChildAt(1), // title
            expandedLayout.getChildAt(2), // pos1 label
            expandedLayout.getChildAt(3), // row1
            expandedLayout.getChildAt(4), // pos2 label
            expandedLayout.getChildAt(5), // row2
            expandedLayout.getChildAt(6), // pixel label
            expandedLayout.getChildAt(7), // pixel input
            expandedLayout.getChildAt(8)  // calculate button
        )

        for (view in inputViews) {
            view?.visibility = View.GONE
        }

        // Keep the close button visible and update its text
        btnClose.text = "✕ CLOSE"
        btnClose.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            40
        ).apply {
            bottomMargin = 10
        }
    }

    private fun showError(message: String) {
        android.util.Log.d("StrongholdCalc", "Showing error: $message")

        tvResults.text = "ERROR: $message"
        tvResults.visibility = View.VISIBLE

        // Force update the overlay layout
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            overlayView?.let { view ->
                val params = view.layoutParams as WindowManager.LayoutParams
                windowManager.updateViewLayout(view, params)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}