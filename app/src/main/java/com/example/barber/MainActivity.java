package com.example.barber;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private View splashLayout;
    private CardView cardLogin;
    private View textViewTitle;
    private ImageView imageViewBackground;
    private TextInputEditText editTextEmail, editTextPassword;
    private MaterialButton buttonLogin, buttonGoogle;
    private TextView textViewForgotPassword;
    private boolean isSplashDone = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainEM), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configurar inicio de sesión con Google usando el Web Client ID
        String webClientId;
        try {
            webClientId = getString(R.string.default_web_client_id);
        } catch (Exception e) {
            webClientId = "531390213985-fljbdjrgj418o1e3t1jg1f8vmoeuajki.apps.googleusercontent.com";
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Vistas de la UI
        splashLayout = findViewById(R.id.splashLayoutEM);
        cardLogin = findViewById(R.id.cardLoginEM);
        textViewTitle = findViewById(R.id.textViewTitleEM);
        imageViewBackground = findViewById(R.id.imageViewBackgroundEM);

        // Campos de entrada de login y botones
        editTextEmail = findViewById(R.id.editTextEmailEM);
        editTextPassword = findViewById(R.id.editTextPasswordEM);
        buttonLogin = findViewById(R.id.buttonLoginEM);
        buttonGoogle = findViewById(R.id.buttonGoogleEM);
        textViewForgotPassword = findViewById(R.id.textViewForgotPasswordEM);

        // Ocultar la imagen de fondo grande durante la pantalla de carga inicial
        if (imageViewBackground != null) {
            imageViewBackground.setVisibility(View.GONE);
            imageViewBackground.setAlpha(0f);
        }

        // Ocultar título y tarjeta de login inicialmente durante el splash
        if (textViewTitle != null) {
            textViewTitle.setVisibility(View.GONE);
            textViewTitle.setTranslationY(-200f);
            textViewTitle.setAlpha(0f);
        }

        if (cardLogin != null) {
            cardLogin.setVisibility(View.GONE);
            cardLogin.setTranslationY(250f);
            cardLogin.setAlpha(0f);
        }

        // Eventos de botones
        if (buttonLogin != null) {
            buttonLogin.setOnClickListener(v -> handleEmailPasswordLogin());
        }

        if (textViewForgotPassword != null) {
            textViewForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        }

        if (buttonGoogle != null) {
            buttonGoogle.setOnClickListener(v -> signInWithGoogle());
        }

        // Navegación al pulsar "Regístrate" con animación de "SEPARARSE"
        TextView textViewRegister = findViewById(R.id.textViewRegisterEM);
        if (textViewRegister != null) {
            textViewRegister.setOnClickListener(v -> animateSplitAndOpenRegister());
        }

        // Carga de 2.5 segundos con comprobación de sesión existente
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isSplashDone = true;
            // Ocultar pantalla de carga
            if (splashLayout != null) {
                splashLayout.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction(() -> splashLayout.setVisibility(View.GONE))
                        .start();
            }

            // Comprobar si ya hay una sesión activa en Firebase
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                checkUserRoleAndNotify(currentUser.getUid(), "Sesión activa iniciada como ");
            } else {
                // Unir y ensamblar elementos en pantalla (Imagen + Título + Tarjeta)
                assembleLoginViews();
            }
        }, 2500);
    }

    private void handleEmailPasswordLogin() {
        String email = editTextEmail != null && editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
        String password = editTextPassword != null && editTextPassword.getText() != null ? editTextPassword.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserRoleAndNotify(user.getUid(), "Inicio de sesión exitoso como ");
                        }
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Error al iniciar sesión";
                        if (errorMsg != null && errorMsg.contains("operation is not allowed")) {
                            errorMsg = "Debes activar 'Correo/Contraseña' en Firebase Console (Authentication -> Métodos de inicio de sesión)";
                        }
                        Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Restablecer Contraseña");
        builder.setMessage("Ingresa tu correo electrónico registrado para enviarte un enlace de recuperación.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        if (editTextEmail != null && editTextEmail.getText() != null) {
            input.setText(editTextEmail.getText().toString().trim());
        }
        builder.setView(input);

        builder.setPositiveButton("Enviar", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(MainActivity.this, "Ingresa un correo válido", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Se ha enviado un correo de recuperación a " + email, Toast.LENGTH_LONG).show();
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Error al enviar correo";
                            Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void signInWithGoogle() {
        if (mGoogleSignInClient != null) {
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    Toast.makeText(this, "No se pudo obtener el token de la cuenta seleccionada", Toast.LENGTH_LONG).show();
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Error al iniciar sesión con Google (Código " + e.getStatusCode() + "): " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            handleGoogleUserFirestore(user);
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Error de credencial";
                        Toast.makeText(MainActivity.this, "Fallo en autenticación con Firebase: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleGoogleUserFirestore(FirebaseUser user) {
        String uid = user.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        Toast.makeText(MainActivity.this, "Bienvenido de nuevo como " + (role != null ? role : "Cliente"), Toast.LENGTH_LONG).show();
                        navigateToRoleHome(role);
                    } else {
                        // Crear registro inicial por defecto para Google
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("uid", uid);
                        userMap.put("role", "Cliente");
                        userMap.put("name", user.getDisplayName() != null ? user.getDisplayName() : "");
                        userMap.put("email", user.getEmail() != null ? user.getEmail() : "");

                        db.collection("users").document(uid).set(userMap)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(MainActivity.this, "Inicio exitoso con Google como Cliente", Toast.LENGTH_SHORT).show();
                                    navigateToRoleHome("Cliente");
                                })
                                .addOnFailureListener(e -> navigateToRoleHome("Cliente"));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Bienvenido como Cliente", Toast.LENGTH_SHORT).show();
                    navigateToRoleHome("Cliente");
                });
    }

    private void checkUserRoleAndNotify(String uid, String messagePrefix) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = "Cliente";
                    if (documentSnapshot.exists()) {
                        role = documentSnapshot.getString("role");
                    }
                    Toast.makeText(MainActivity.this, messagePrefix + (role != null ? role : "Cliente"), Toast.LENGTH_SHORT).show();
                    navigateToRoleHome(role);
                })
                .addOnFailureListener(e -> navigateToRoleHome("Cliente"));
    }

    private void navigateToRoleHome(String role) {
        if ("Barbería".equalsIgnoreCase(role)) {
            Intent intent = new Intent(MainActivity.this, HomeBarberiaActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in_scale, R.anim.fade_out_scale);
        } else if ("Barbero".equalsIgnoreCase(role)) {
            Intent intent = new Intent(MainActivity.this, HomeBarberoActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in_scale, R.anim.fade_out_scale);
        } else {
            Intent intent = new Intent(MainActivity.this, HomeClienteActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in_scale, R.anim.fade_out_scale);
        }
    }

    private void clearLoginFields() {
        if (editTextEmail != null) editTextEmail.setText("");
        if (editTextPassword != null) editTextPassword.setText("");
    }

    private void animateSplitAndOpenRegister() {
        if (imageViewBackground != null) {
            imageViewBackground.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .start();
        }

        if (textViewTitle != null) {
            textViewTitle.animate()
                    .translationY(-250f)
                    .alpha(0f)
                    .setDuration(300)
                    .start();
        }

        if (cardLogin != null) {
            cardLogin.animate()
                    .translationY(300f)
                    .alpha(0f)
                    .setDuration(300)
                    .start();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            clearLoginFields();
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in_scale, R.anim.fade_out_scale);
        }, 250);
    }

    private void assembleLoginViews() {
        if (imageViewBackground != null) {
            imageViewBackground.setVisibility(View.VISIBLE);
            imageViewBackground.animate()
                    .alpha(0.85f)
                    .setDuration(450)
                    .start();
        }

        if (textViewTitle != null) {
            textViewTitle.setVisibility(View.VISIBLE);
            textViewTitle.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(450)
                    .start();
        }

        if (cardLogin != null) {
            cardLogin.setVisibility(View.VISIBLE);
            cardLogin.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(450)
                    .start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSplashDone) {
            clearLoginFields();
            assembleLoginViews();
        }
    }
}
