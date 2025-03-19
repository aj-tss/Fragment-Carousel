package com.example.calc_app9


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var displayTextView: TextView
    private val calculationHistory = mutableListOf<String>()

    // Other variables for calculator functionality
    private var currentNumber = ""
    private var operator = ""
    private var firstOperand = 0.0
    private var isOperatorClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the display TextView
        displayTextView = findViewById(R.id.displayTextView)

        // Set up the buttons
        setupNumericButtons()
        setupOperationButtons()
        setupOtherButtons()

        // Set up the share button
        val shareButton = findViewById<Button>(R.id.shareButton)
        shareButton.setOnClickListener {
            shareCalculationHistory()
        }
    }

    // Method to set up numeric buttons
    private fun setupNumericButtons() {
        val numericButtonIds = intArrayOf(
            R.id.button0, R.id.button1, R.id.button2, R.id.button3, R.id.button4,
            R.id.button5, R.id.button6, R.id.button7, R.id.button8, R.id.button9
        )

        for (id in numericButtonIds) {
            findViewById<Button>(id).setOnClickListener { v ->
                val digit = (v as Button).text.toString()
                if (isOperatorClicked) {
                    currentNumber = digit
                    isOperatorClicked = false
                } else {
                    currentNumber += digit
                }
                displayTextView.text = currentNumber
            }
        }
    }

    // Method to set up operation buttons
    private fun setupOperationButtons() {
        val operationButtonIds = intArrayOf(
            R.id.buttonAdd, R.id.buttonSubtract, R.id.buttonMultiply,
            R.id.buttonDivide, R.id.buttonModulus, R.id.buttonPower
        )

        for (id in operationButtonIds) {
            findViewById<Button>(id).setOnClickListener { v ->
                if (currentNumber.isNotEmpty()) {
                    firstOperand = currentNumber.toDouble()
                    operator = (v as Button).text.toString()
                    isOperatorClicked = true
                }
            }
        }
    }

    // Method to set up other buttons
    private fun setupOtherButtons() {
        // Clear button
        val clearButton = findViewById<Button>(R.id.buttonClear)
        clearButton.setOnClickListener {
            currentNumber = ""
            operator = ""
            firstOperand = 0.0
            displayTextView.text = "0"
        }

        // Decimal button
        val decimalButton = findViewById<Button>(R.id.buttonDecimal)
        decimalButton.setOnClickListener {
            if (!currentNumber.contains(".")) {
                if (currentNumber.isEmpty()) {
                    currentNumber = "0."
                } else {
                    currentNumber += "."
                }
                displayTextView.text = currentNumber
            }
        }

        // Delete button
        val deleteButton = findViewById<Button>(R.id.buttonDelete)
        deleteButton.setOnClickListener {
            if (currentNumber.isNotEmpty()) {
                currentNumber = currentNumber.substring(0, currentNumber.length - 1)
                if (currentNumber.isEmpty()) {
                    displayTextView.text = "0"
                } else {
                    displayTextView.text = currentNumber
                }
            }
        }

        // Equals button
        val equalsButton = findViewById<Button>(R.id.buttonEquals)
        equalsButton.setOnClickListener {
            if (currentNumber.isNotEmpty() && operator.isNotEmpty()) {
                val secondOperand = currentNumber.toDouble()
                val expression = "$firstOperand $operator $secondOperand"

                val result = when (operator) {
                    "+" -> firstOperand + secondOperand
                    "-" -> firstOperand - secondOperand
                    "*" -> firstOperand * secondOperand
                    "/" -> {
                        if (secondOperand != 0.0) {
                            firstOperand / secondOperand
                        } else {
                            displayTextView.text = "Error"
                            return@setOnClickListener
                        }
                    }
                    "%" -> firstOperand % secondOperand
                    "^" -> Math.pow(firstOperand, secondOperand)
                    else -> 0.0
                }

                // Format result
                val resultString = formatResult(result)
                displayTextView.text = resultString

                // Add to history
                calculationHistory.add("$expression = $resultString")

                // Reset for next calculation
                currentNumber = resultString
                operator = ""
            }
        }
    }

    // Format result to handle integers vs decimals
    private fun formatResult(result: Double): String {
        return if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            result.toString()
        }
    }

    // Method to share calculation history
    private fun shareCalculationHistory() {
        if (calculationHistory.isEmpty()) {
            // If there's no history yet, add the current display as a basic entry
            val currentDisplay = displayTextView.text.toString()
            if (currentDisplay != "0") {
                calculationHistory.add("Result: $currentDisplay")
            }
        }

        if (calculationHistory.isNotEmpty()) {
            val historyText = StringBuilder("My Calculator History:\n\n")
            for (calculation in calculationHistory) {
                historyText.append(calculation).append("\n")
            }

            // Create the sharing intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Calculator History")
                putExtra(Intent.EXTRA_TEXT, historyText.toString())
            }

            // Create and start the chooser
            val chooserIntent = Intent.createChooser(shareIntent, "Share via")
            if (shareIntent.resolveActivity(packageManager) != null) {
                startActivity(chooserIntent)
            }
        }
    }
}