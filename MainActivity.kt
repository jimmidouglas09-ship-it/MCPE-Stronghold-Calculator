package com.example.strongholdcalculator

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import android.content.Context
import kotlin.math.*

class MainActivity : Activity() {

    private lateinit var etPlayerX1: EditText
    private lateinit var etPlayerZ1: EditText
    private lateinit var etPlayerX2: EditText
    private lateinit var etPlayerZ2: EditText
    private lateinit var etPixelChange: EditText
    private lateinit var btnCalculate: Button
    private lateinit var btnStartOverlay: Button
    private lateinit var tvResults: TextView

    private val strongholdCalculator = StrongholdCalculator()
    private val OVERLAY_PERMISSION_REQ_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createLayout()
        strongholdCalculator.generateStrongholdCells()

        btnCalculate.setOnClickListener {
            calculateStronghold()
        }

        btnStartOverlay.setOnClickListener {
            if (canDrawOverlays()) {
                startOverlayService()
            } else {
                requestOverlayPermission()
            }
        }
    }

    private fun createLayout() {
        val scrollView = ScrollView(this)
        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setPadding(32, 32, 32, 32)

        // Title
        val titleView = TextView(this)
        titleView.text = "Minecraft Stronghold Calculator"
        titleView.textSize = 24f
        titleView.gravity = android.view.Gravity.CENTER
        titleView.setPadding(0, 0, 0, 32)
        mainLayout.addView(titleView)

        // Overlay button (main feature)
        btnStartOverlay = Button(this)
        btnStartOverlay.text = "Start Speedrun Overlay"
        btnStartOverlay.textSize = 18f
        btnStartOverlay.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
        btnStartOverlay.setTextColor(android.graphics.Color.WHITE)
        val overlayBtnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        overlayBtnParams.setMargins(0, 0, 0, 32)
        btnStartOverlay.layoutParams = overlayBtnParams
        mainLayout.addView(btnStartOverlay)

        // Separator
        val separator = TextView(this)
        separator.text = "── OR USE TRADITIONAL MODE ──"
        separator.gravity = android.view.Gravity.CENTER
        separator.textSize = 12f
        separator.setPadding(0, 16, 0, 32)
        mainLayout.addView(separator)

        // First position
        val firstPosLabel = TextView(this)
        firstPosLabel.text = "First Position"
        firstPosLabel.textSize = 18f
        firstPosLabel.setTypeface(null, android.graphics.Typeface.BOLD)
        firstPosLabel.setPadding(0, 0, 0, 16)
        mainLayout.addView(firstPosLabel)

        val firstRow = LinearLayout(this)
        firstRow.orientation = LinearLayout.HORIZONTAL

        etPlayerX1 = EditText(this)
        etPlayerX1.hint = "X coordinate"
        etPlayerX1.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        val x1Params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        x1Params.setMargins(0, 0, 16, 0)
        etPlayerX1.layoutParams = x1Params

        etPlayerZ1 = EditText(this)
        etPlayerZ1.hint = "Z coordinate"
        etPlayerZ1.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        etPlayerZ1.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        firstRow.addView(etPlayerX1)
        firstRow.addView(etPlayerZ1)
        mainLayout.addView(firstRow)

        // Second position
        val secondPosLabel = TextView(this)
        secondPosLabel.text = "Second Position"
        secondPosLabel.textSize = 18f
        secondPosLabel.setTypeface(null, android.graphics.Typeface.BOLD)
        secondPosLabel.setPadding(0, 32, 0, 16)
        mainLayout.addView(secondPosLabel)

        val secondRow = LinearLayout(this)
        secondRow.orientation = LinearLayout.HORIZONTAL

        etPlayerX2 = EditText(this)
        etPlayerX2.hint = "X coordinate"
        etPlayerX2.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        val x2Params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        x2Params.setMargins(0, 0, 16, 0)
        etPlayerX2.layoutParams = x2Params

        etPlayerZ2 = EditText(this)
        etPlayerZ2.hint = "Z coordinate"
        etPlayerZ2.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        etPlayerZ2.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        secondRow.addView(etPlayerX2)
        secondRow.addView(etPlayerZ2)
        mainLayout.addView(secondRow)

        // Pixel change (optional)
        val pixelLabel = TextView(this)
        pixelLabel.text = "Pixel Change (Optional - for distance calculation)"
        pixelLabel.textSize = 14f
        pixelLabel.setPadding(0, 32, 0, 8)
        mainLayout.addView(pixelLabel)

        etPixelChange = EditText(this)
        etPixelChange.hint = "Pixel change (leave empty for coordinate-only calculation)"
        etPixelChange.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        val pixelParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        pixelParams.setMargins(0, 0, 0, 32)
        etPixelChange.layoutParams = pixelParams
        mainLayout.addView(etPixelChange)

        // Calculate button
        btnCalculate = Button(this)
        btnCalculate.text = "Calculate Stronghold Location"
        btnCalculate.textSize = 16f
        val btnParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        btnParams.setMargins(0, 0, 0, 32)
        btnCalculate.layoutParams = btnParams
        mainLayout.addView(btnCalculate)

        // Results
        tvResults = TextView(this)
        tvResults.text = "For speedrunning: Use the 'Start Speedrun Overlay' button above!\n\nOr enter two coordinates below and tap Calculate for traditional mode.\nPixel change is optional - leave empty for coordinate-only calculation."
        tvResults.setBackgroundColor(android.graphics.Color.BLACK)
        tvResults.setTextColor(android.graphics.Color.WHITE)
        tvResults.setPadding(24, 24, 24, 24)
        tvResults.typeface = android.graphics.Typeface.MONOSPACE
        tvResults.textSize = 12f
        mainLayout.addView(tvResults)

        scrollView.addView(mainLayout)
        setContentView(scrollView)
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
            Toast.makeText(
                this,
                "Please enable 'Display over other apps' permission for the speedrun overlay to work",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        startService(intent)
        Toast.makeText(this, "Speedrun overlay started! Look for the 'SH' button.", Toast.LENGTH_SHORT).show()

        // Minimize the app
        moveTaskToBack(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (canDrawOverlays()) {
                startOverlayService()
            } else {
                Toast.makeText(this, "Overlay permission is required for speedrun mode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateStronghold() {
        try {
            val playerX1 = etPlayerX1.text.toString().toDoubleOrNull()
            val playerZ1 = etPlayerZ1.text.toString().toDoubleOrNull()
            val playerX2 = etPlayerX2.text.toString().toDoubleOrNull()
            val playerZ2 = etPlayerZ2.text.toString().toDoubleOrNull()
            val pixelChange = etPixelChange.text.toString().toDoubleOrNull()

            if (playerX1 == null || playerZ1 == null || playerX2 == null || playerZ2 == null) {
                showError("Please fill in all coordinate fields with valid numbers")
                return
            }

            val result = if (pixelChange != null && pixelChange > 0) {
                // Use pixel change method - calculate distance from second position
                val distance = 3655.0 / pixelChange
                strongholdCalculator.calculateStrongholdWithDistance(
                    playerX2, playerZ2, playerX1, playerZ1, distance
                )
            } else {
                // Use coordinate-only method
                strongholdCalculator.calculateStrongholdFromCoordinates(
                    playerX1, playerZ1, playerX2, playerZ2
                )
            }

            displayResults(result, pixelChange != null)

        } catch (e: Exception) {
            showError("Error: ${e.message}")
        }
    }

    private fun displayResults(result: TriangulationResult, usedPixelChange: Boolean) {
        if (result.candidates.isEmpty()) {
            tvResults.text = "No candidates found.\n\nTips:\n- Check coordinates\n- Make sure positions are 50+ blocks apart\n- Verify pixel change value (usually 0.5-10)"
            return
        }

        val sb = StringBuilder()
        sb.append("=== STRONGHOLD RESULTS ===\n\n")

        if (usedPixelChange) {
            sb.append("METHOD: Distance-based (using pixel change)\n\n")
        } else {
            sb.append("METHOD: Coordinate-only calculation\n\n")
        }

        val best = result.candidates[0]
        sb.append("BEST ESTIMATE:\n")
        sb.append("Overworld: (${best.projectionX}, ${best.projectionZ})\n")
        sb.append("Nether: (${best.netherX}, ${best.netherZ})\n")
        sb.append("Probability: ${String.format("%.1f%%", best.conditionalProb * 100)}\n")
        sb.append("Distance: ${best.distance} blocks\n\n")

        sb.append("TOP CANDIDATES:\n")
        val maxCandidates = kotlin.math.min(6, result.candidates.size)
        for (i in 0 until maxCandidates) {
            val c = result.candidates[i]
            sb.append("${i+1}. (${c.projectionX}, ${c.projectionZ}) ")
            sb.append("${String.format("%.1f%%", c.conditionalProb * 100)}\n")
            sb.append("   Nether: (${c.netherX}, ${c.netherZ})\n")
            if (i < maxCandidates - 1) sb.append("\n")
        }

        tvResults.text = sb.toString()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        tvResults.text = "ERROR: $message"
    }
}