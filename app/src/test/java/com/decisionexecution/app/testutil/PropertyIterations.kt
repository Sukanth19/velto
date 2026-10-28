package com.decisionexecution.app.testutil

/**
 * Iterations per property test. Defaults to 100; run with -PpropIterations=500 for a deeper pass.
 */
val PROPERTY_ITERATIONS: Int = System.getProperty("propIterations")?.toIntOrNull() ?: 100
