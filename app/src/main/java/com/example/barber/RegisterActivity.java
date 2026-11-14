package com.example.barber;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout layoutBarbershopName, layoutBarbershopDistrict, layoutName, layoutLastName, layoutAge, layoutPhone, layoutEmail, layoutPassword;
    private TextInputEditText editTextBarbershopName, editTextBarbershopDistrict, editTextName, editTextLastName, editTextAge, editTextPhone, editTextEmail, editTextPassword;
    private RadioGroup radioGroupRole;
    private View textViewRegisterTitle;
    private CardView cardRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar Firebase Auth y Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textViewRegisterTitle = findViewById(R.id.textViewRegisterTitleEM);
        cardRegister = findViewById(R.id.cardRegisterEM);

        // Ocultar e iniciar posiciones para la animación de "ENSAMBLARSE"
        if (textViewRegisterTitle != null) {
            textViewRegisterTitle.setTranslationY(-200f);
            textViewRegisterTitle.setAlpha(0f);
        }

        if (cardRegister != null) {
            cardRegister.setTranslationY(250f);
            cardRegister.setAlpha(0f);
        }

        // Animación de Ensamblarse al entrar
        assembleRegisterViews();

        // Botón regresar
        ImageButton buttonBack = findViewById(R.id.buttonBackEM);
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> animateSplitAndFinish());
        }

        // Texto volver al login
        TextView textViewLogin = findViewById(R.id.textViewLoginEM);
        if (textViewLogin != null) {
            textViewLogin.setOnClickListener(v -> animateSplitAndFinish());
        }

        // Referencias a los Layouts de campos
        layoutBarbershopName = findViewById(R.id.layoutBarbershopNameEM);
        layoutBarbershopDistrict = findViewById(R.id.layoutBarbershopDistrictEM);
        layoutName = findViewById(R.id.layoutNameEM);
        layoutLastName = findViewById(R.id.layoutLastNameEM);
        layoutAge = findViewById(R.id.layoutAgeEM);
        layoutPhone = findViewById(R.id.layoutPhoneEM);
        layoutEmail = findViewById(R.id.layoutEmailEM);
        layoutPassword = findViewById(R.id.layoutPasswordEM);

        // Referencias a los EditTexts
        editTextBarbershopName = findViewById(R.id.editTextBarbershopNameEM);
        editTextBarbershopDistrict = findViewById(R.id.editTextBarbershopDistrictEM);
        editTextName = findViewById(R.id.editTextNameEM);
        editTextLastName = findViewById(R.id.editTextLastNameEM);
        editTextAge = findViewById(R.id.editTextAgeEM);
        editTextPhone = findViewById(R.id.editTextPhoneEM);
        editTextEmail = findViewById(R.id.editTextRegisterEmailEM);
        editTextPassword = findViewById(R.id.editTextRegisterPasswordEM);

        // Configurar formato de teléfono "+51 XXX XXX XXX"
        if (editTextPhone != null) {
            editTextPhone.setText("+51 ");
            editTextPhone.setSelection(4);
            editTextPhone.addTextChangedListener(new TextWatcher() {
                boolean isFormatting = false;
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (isFormatting) return;
                    isFormatting = true;
                    String str = s.toString();
                    if (!str.startsWith("+51 ")) {
                        editTextPhone.setText("+51 ");
                        editTextPhone.setSelection(editTextPhone.getText().length());
                        isFormatting = false;
                        return;
                    }
                    String digits = str.substring(4).replaceAll("\\D", "");
                    if (digits.length() > 9) {
                        digits = digits.substring(0, 9);
                    }
                    StringBuilder formatted = new StringBuilder("+51 ");
                    for (int i = 0; i < digits.length(); i++) {
                        if (i == 3 || i == 6) {
                            formatted.append(" ");
                        }
                        formatted.append(digits.charAt(i));
                    }
                    editTextPhone.setText(formatted.toString());
                    editTextPhone.setSelection(editTextPhone.getText().length());
                    isFormatting = false;
                }
            });
        }

        // Selector de Rol
        radioGroupRole = findViewById(R.id.radioGroupRoleEM);
        if (radioGroupRole != null) {
            radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
                clearFormFields();
                updateFormFieldsForRole(checkedId);
            });
        }

        // Configuración inicial del formulario
        updateFormFieldsForRole(R.id.radioClientEM);

        // Botón Registrarse
        MaterialButton buttonRegister = findViewById(R.id.buttonRegisterEM);
        if (buttonRegister != null) {
            buttonRegister.setOnClickListener(v -> handleRegister());
        }
    }

    private void clearFormFields() {
        if (editTextBarbershopName != null) editTextBarbershopName.setText("");
        if (editTextBarbershopDistrict != null) editTextBarbershopDistrict.setText("");
        if (editTextName != null) editTextName.setText("");
        if (editTextLastName != null) editTextLastName.setText("");
        if (editTextAge != null) editTextAge.setText("");
        if (editTextPhone != null) {
            editTextPhone.setText("+51 ");
            editTextPhone.setSelection(4);
        }
        if (editTextEmail != null) editTextEmail.setText("");
        if (editTextPassword != null) editTextPassword.setText("");
    }

    private void updateFormFieldsForRole(int checkedId) {
        if (layoutPassword != null) layoutPassword.setHint("Crear una contraseña");

        if (checkedId == R.id.radioBarbershopEM) {
            // Configuración para BARBERÍA
            if (layoutBarbershopName != null) layoutBarbershopName.setVisibility(View.VISIBLE);
            if (layoutBarbershopDistrict != null) layoutBarbershopDistrict.setVisibility(View.VISIBLE);
            if (layoutName != null) layoutName.setHint("Nombre del Administrador");
            if (layoutLastName != null) layoutLastName.setHint("Apellido del Administrador");
            if (layoutAge != null) layoutAge.setVisibility(View.GONE);
        } else {
            // Configuración para CLIENTE o BARBERO
            if (layoutBarbershopName != null) layoutBarbershopName.setVisibility(View.GONE);
            if (layoutBarbershopDistrict != null) layoutBarbershopDistrict.setVisibility(View.GONE);
            if (layoutName != null) layoutName.setHint("Nombre");
            if (layoutLastName != null) layoutLastName.setHint("Apellido");
            if (layoutAge != null) layoutAge.setVisibility(View.VISIBLE);
        }
    }

    private void assembleRegisterViews() {
        if (textViewRegisterTitle != null) {
            textViewRegisterTitle.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(450)
                    .start();
        }

        if (cardRegister != null) {
            cardRegister.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(450)
                    .start();
        }
    }

    private void animateSplitAndFinish() {
        if (textViewRegisterTitle != null) {
            textViewRegisterTitle.animate()
                    .translationY(-200f)
                    .alpha(0f)
                    .setDuration(300)
                    .start();
        }

        if (cardRegister != null) {
            cardRegister.animate()
                    .translationY(250f)
                    .alpha(0f)
                    .setDuration(300)
                    .start();
        }

        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 250);
    }

    private void handleRegister() {
        int selectedRoleId = radioGroupRole.getCheckedRadioButtonId();
        boolean isBarbershop = (selectedRoleId == R.id.radioBarbershopEM);

        String barbershopName = (editTextBarbershopName != null && editTextBarbershopName.getText() != null)
                ? editTextBarbershopName.getText().toString().trim() : "";
        String district = (editTextBarbershopDistrict != null && editTextBarbershopDistrict.getText() != null)
                ? editTextBarbershopDistrict.getText().toString().trim() : "";
        String name = (editTextName != null && editTextName.getText() != null)
                ? editTextName.getText().toString().trim() : "";
        String lastName = (editTextLastName != null && editTextLastName.getText() != null)
                ? editTextLastName.getText().toString().trim() : "";
        String age = (editTextAge != null && editTextAge.getText() != null)
                ? editTextAge.getText().toString().trim() : "";
        String phone = (editTextPhone != null && editTextPhone.getText() != null)
                ? editTextPhone.getText().toString().trim() : "";
        String email = (editTextEmail != null && editTextEmail.getText() != null)
                ? editTextEmail.getText().toString().trim() : "";
        String password = (editTextPassword != null && editTextPassword.getText() != null)
                ? editTextPassword.getText().toString().trim() : "";

        if (isBarbershop) {
            if (barbershopName.isEmpty() || district.isEmpty() || name.isEmpty() || lastName.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los datos de la Barbería (incluyendo distrito)", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (name.isEmpty() || lastName.isEmpty() || age.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos tus datos personales", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Phone 9 digits validation
        String phoneDigits = phone.substring(4).replaceAll("\\D", "");
        if (phoneDigits.length() != 9) {
            Toast.makeText(this, "Por favor ingresa un número de teléfono válido de 9 dígitos (+51 XXX XXX XXX)", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadioButton = findViewById(selectedRoleId);
        String role = selectedRadioButton != null ? selectedRadioButton.getText().toString() : "Cliente";

        // Email domain validation
        if ("Cliente".equals(role) || "Barbero".equals(role)) {
            if (!email.toLowerCase().endsWith("@gmail.com")) {
                Toast.makeText(this, "Correo no encontrado, utilize un correo Gmail", Toast.LENGTH_SHORT).show();
                return;
            }
        } else if ("Barbería".equals(role)) {
            if (!email.toLowerCase().endsWith("@barberapp.com")) {
                Toast.makeText(this, "La barbería debe utilizar un correo @barberapp.com", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Check unique name & email in Firestore
        db.collection("users").get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean emailExists = false;
                    boolean nameExists = false;

                    for (DocumentSnapshot doc : querySnapshot) {
                        String existingEmail = doc.getString("email");
                        String existingName = doc.getString("name");
                        String existingBarbershopName = doc.getString("barbershopName");
                        String existingRole = doc.getString("role");

                        if (existingEmail != null && existingEmail.equalsIgnoreCase(email)) {
                            emailExists = true;
                            break;
                        }
                        if ("Barbería".equals(role) && "Barbería".equals(existingRole)) {
                            if (existingBarbershopName != null && existingBarbershopName.equalsIgnoreCase(barbershopName)) {
                                nameExists = true;
                                break;
                            }
                        } else if ("Cliente".equals(role) && "Cliente".equals(existingRole)) {
                            if (existingName != null && existingName.equalsIgnoreCase(name)) {
                                nameExists = true;
                                break;
                            }
                        } else if ("Barbero".equals(role) && "Barbero".equals(existingRole)) {
                            if (existingName != null && existingName.equalsIgnoreCase(name)) {
                                nameExists = true;
                                break;
                            }
                        }
                    }

                    if (emailExists) {
                        Toast.makeText(RegisterActivity.this, "Correo ya existente", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (nameExists) {
                        if ("Cliente".equals(role)) {
                            Toast.makeText(RegisterActivity.this, "Nombre de cliente ya existente", Toast.LENGTH_SHORT).show();
                        } else if ("Barbero".equals(role)) {
                            Toast.makeText(RegisterActivity.this, "Nombre de barbero ya existente", Toast.LENGTH_SHORT).show();
                        } else if ("Barbería".equals(role)) {
                            Toast.makeText(RegisterActivity.this, "Nombre de barbería ya existente", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    // Proceed with creating user in FirebaseAuth
                    createAuthUser(email, password, role, barbershopName, district, name, lastName, age, phone);
                })
                .addOnFailureListener(e -> createAuthUser(email, password, role, barbershopName, district, name, lastName, age, phone));
    }

    private void createAuthUser(String email, String password, String role, String barbershopName, String district, String name, String lastName, String age, String phone) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserDataToFirestore(user.getUid(), role, barbershopName, district, name, lastName, age, phone, email);
                        }
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Error al registrar usuario";
                        if (errorMsg != null && errorMsg.contains("operation is not allowed")) {
                            errorMsg = "Debes activar 'Correo/Contraseña' en Firebase Console (Authentication -> Métodos de inicio de sesión)";
                        }
                        Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserDataToFirestore(String uid, String role, String barbershopName, String district, String name, String lastName, String age, String phone, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("role", role);
        userMap.put("name", name);
        userMap.put("lastName", lastName);
        userMap.put("phone", phone);
        userMap.put("email", email);

        if ("Barbería".equals(role)) {
            userMap.put("barbershopName", barbershopName);
            userMap.put("district", district);
        } else {
            userMap.put("age", age);
        }

        db.collection("users").document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "¡Registro exitoso como " + role + "!", Toast.LENGTH_LONG).show();
                    if ("Barbería".equalsIgnoreCase(role)) {
                        Intent intent = new Intent(RegisterActivity.this, HomeBarberiaActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_in_scale, R.anim.fade_out_scale);
                        finish();
                    } else if ("Barbero".equalsIgnoreCase(role)) {
                        Intent intent = new Intent(RegisterActivity.this, HomeBarberoActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_in_scale, R.anim.fade_out_scale);
                        finish();
                    } else {
                        Intent intent = new Intent(RegisterActivity.this, HomeClienteActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_in_scale, R.anim.fade_out_scale);
                        finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Error al guardar en Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in_scale, R.anim.fade_out_scale);
    }
}
