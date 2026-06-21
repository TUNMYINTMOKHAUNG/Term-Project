package com.example.myapplication.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvToggle: TextView

    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Already signed in -> skip straight to main app
        if (auth.currentUser != null) {
            goToMain()
            return
        }

        etEmail     = findViewById(R.id.etEmail)
        etPassword  = findViewById(R.id.etPassword)
        btnLogin    = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        tvToggle    = findViewById(R.id.tvToggle)

        btnLogin.setOnClickListener    { handleLogin() }
        btnRegister.setOnClickListener { handleRegister() }
        tvToggle.setOnClickListener {
            isLoginMode = !isLoginMode
            updateFormMode()
        }

        updateFormMode()
    }

    private fun updateFormMode() {
        if (isLoginMode) {
            btnLogin.visibility    = View.VISIBLE
            btnRegister.visibility = View.GONE
            tvToggle.text          = "Don't have an account? Register"
        } else {
            btnLogin.visibility    = View.GONE
            btnRegister.visibility = View.VISIBLE
            tvToggle.text          = "Already have an account? Log in"
        }
    }

    private fun handleLogin() {
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                setLoading(false)
                goToMain()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun handleRegister() {
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                // FIX: also create a user document in Firestore so email/profile data is saved
                val userId = result.user?.uid ?: return@addOnSuccessListener
                val userDoc = hashMapOf(
                    "email" to email,
                    "createdAt" to System.currentTimeMillis(),
                    "preferredColors" to listOf<String>(),
                    "preferredStyles" to listOf<String>()
                )

                db.collection("users").document(userId)
                    .set(userDoc, SetOptions.merge())
                    .addOnSuccessListener {
                        setLoading(false)
                        // New user -> go to preferences screen first
                        startActivity(Intent(this, PreferencesActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        Toast.makeText(this, "Account created, but profile save failed: ${e.message}", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, PreferencesActivity::class.java))
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Register failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled     = !loading
        btnRegister.isEnabled  = !loading
        etEmail.isEnabled      = !loading
        etPassword.isEnabled   = !loading
    }
}
