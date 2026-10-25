package com.mentalys.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.mentalys.app.R
import com.mentalys.app.databinding.ActivityAuthLoginBinding
import com.mentalys.app.ui.activities.MainActivity
import com.mentalys.app.ui.viewmodels.ViewModelFactory
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.showToast

class AuthLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthLoginBinding
    private val viewModel: AuthViewModel by viewModels {
        ViewModelFactory.getInstance(this)
    }

    private lateinit var googleSignInClient: GoogleSignInClient

    // Launcher for Google Sign-In intent
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.loginWithGoogle(
                    idToken = idToken,
                    googleDisplayName = account.displayName,
                    googleEmail = account.email
                )
            } else {
                showToast(this, getString(R.string.google_sign_in_failed))
            }
        } catch (e: ApiException) {
            Log.e("AuthLogin", "Google sign-in failed: code=${e.statusCode}", e)
            showToast(this, getString(R.string.google_sign_in_failed))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        setupObservers()
        setupClickListeners()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupObservers() {
        viewModel.loginResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.loginButton.isEnabled = false
                    binding.btnGoogleSignIn.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                    binding.btnGoogleSignIn.isEnabled = true
                    navigateToMain()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                    binding.btnGoogleSignIn.isEnabled = true
                    showToast(this, resource.error)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                showToast(this, getString(R.string.fill_all_fields))
                return@setOnClickListener
            }
            viewModel.loginUser(email, password)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }

        binding.forgotPasswordTextView.setOnClickListener {
            startActivity(Intent(this, AuthResetPasswordActivity::class.java))
        }

        binding.registerTextview.setOnClickListener {
            startActivity(Intent(this, AuthRegisterActivity::class.java))
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
