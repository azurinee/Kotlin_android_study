package com.fastcampus.kotlin.tinder

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private val editTextEmail: EditText by lazy {
        findViewById(R.id.emailEditText)
    }

    private val editTextPassword: EditText by lazy {
        findViewById(R.id.passwordEditText)
    }

    private lateinit var auth: FirebaseAuth

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        initLoginButton()
        initSignUpButton()
        initEmailAndPasswordButton()
    }

    private fun initLoginButton() {
        val loginButton = findViewById<Button>(R.id.loginButton)
        loginButton.setOnClickListener {
            val email = editTextEmail
            val password = editTextPassword

            auth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful)
                        handleSuccessLogin()
                    else
                        Toast.makeText(this, "로그인에 실패하였습니다. 이메일과 비밀번호를 다시 확인해 주세요.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun initSignUpButton() {
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        signUpButton.setOnClickListener {
            val email = editTextEmail
            val password = editTextPassword

            auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful)
                        Toast.makeText(this, "회원가입에 성공했습니다.", Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(this, "회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun initEmailAndPasswordButton() {
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpButton = findViewById<Button>(R.id.signUpButton)

        editTextEmail.addTextChangedListener {
            val enable = editTextEmail.text.isNotEmpty() && editTextPassword.text.isNotEmpty()
            loginButton.isEnabled = enable
            signUpButton.isEnabled = enable
        }

        editTextPassword.addTextChangedListener {
            val enable = editTextEmail.text.isNotEmpty() && editTextPassword.text.isNotEmpty()
            loginButton.isEnabled = enable
            signUpButton.isEnabled = enable
        }
    }

    private fun handleSuccessLogin() {
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show()
        }
        var userId = auth.currentUser?.uid.orEmpty()
        var currentUserDB = Firebase.database.reference.child("Users").child(userId)
        var user = mutableMapOf<String, Any>()
        user["userId"] = userId
        currentUserDB.updateChildren(user)

        finish()
    }

}