package com.example.calc_app9

import kotlin.math.pow


// Abstract base class for mathematical operations
abstract class MathOperation {
    abstract fun calculate(a: Double, b: Double): Double

    // Concrete classes for different mathematical operations
    class AdditionOperation : MathOperation() {
        override fun calculate(a: Double, b: Double): Double = a + b
    }

    class SubtractionOperation : MathOperation() {
        override fun calculate(a: Double, b: Double): Double = a - b
    }

    class MultiplicationOperation : MathOperation() {
        override fun calculate(a: Double, b: Double): Double = a * b
    }

    class DivisionOperation : MathOperation() {
        override fun calculate(a: Double, b: Double): Double {
            if (b == 0.0) throw ArithmeticException("Division by zero is not allowed")
            return a / b
        }
    }

    class ModulusOperation : MathOperation() {
        override fun calculate(a: Double, b: Double): Double = a % b
    }

    class PowerOperation : MathOperation() {
        override fun calculate(a: Double, b: Double): Double = a.pow(b)
    }

}

// Main Calculator class
open class Calculator {

    fun performOperation(a: Double, b: Double, operation: MathOperation): Double {
        return operation.calculate(a, b)
    }

}
