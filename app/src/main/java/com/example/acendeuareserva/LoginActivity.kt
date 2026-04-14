package com.example.acendeuareserva

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.acendeuareserva.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    /* Inicializa a interface, o Firebase Auth e configura os eventos de clique dos botões */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val senha = binding.editSenha.text.toString()

            if (email.isNotEmpty() && senha.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, senha)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            irParaMapa()
                        } else {
                            Toast.makeText(this, "Erro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCadastrar.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val senha = binding.editSenha.text.toString()

            if (email.isNotEmpty() && senha.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, senha)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                            irParaMapa()
                        } else {
                            Toast.makeText(this, "Erro ao criar: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Preencha e-mail e senha para criar conta", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /* Verifica se existe uma sessão ativa de usuário ao iniciar a atividade */
    override fun onStart() {
        super.onStart()
        val usuarioAtual = auth.currentUser
        if (usuarioAtual != null) {
            irParaMapa()
        }
    }

    /* Realiza a transição da tela de autenticação para a tela principal do mapa */
    private fun irParaMapa() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}