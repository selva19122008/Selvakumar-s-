// Top-level build file where you can add configuration options common to all sub-projects/modules.
Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
    val isKspIssue = throwable.message?.contains("ksp.com.intellij") == true || 
                     throwable.stackTrace?.any { it.className.contains("ksp.com.intellij") } == true
    if (isKspIssue) {
        // Silence the background KSP AWT EventQueue NullPointerException
    } else {
        System.err.println("Exception in thread \"${thread.name}\" ${throwable::class.java.name}: ${throwable.message}")
        throwable.printStackTrace(System.err)
    }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}
