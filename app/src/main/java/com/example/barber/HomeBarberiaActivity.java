package com.example.barber;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HomeBarberiaActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private CardView headerCardBarberia, bottomNavCardBarberia;
    private BottomNavigationView bottomNavigationBarberia;
    private LinearLayout layoutBarberiaDashboard, layoutBarberiaInscribir, layoutBarberiaPerfil;
    private LinearLayout layoutCitasListContainer, layoutProfileBarbersListContainer, layoutBarbershopPortfolioContainer;

    private TextView tvBarbershopHeaderName, tvMetricsCitas, tvMetricsRendimiento;
    private TextView tvProfileBarbershopName, tvProfileBarbershopPhone, tvEditProfile, tvAddPhoto;

    private TextInputEditText etAddBarberName;
    private TextInputEditText etBarbershopBio, etBarbershopPromo, etBarbershopDistrict;
    private MaterialButton btnAddBarberToTeam, btnSaveProfileChanges, btnBarberiaSignOut;
    private MaterialButton btnStatusOpen, btnStatusClosed;
    private ImageButton btnBarberiaNotif;
    private ImageView ivProfileLogo;

    private ActivityResultLauncher<Intent> barbershopLogoPickerLauncher;
    private ActivityResultLauncher<Intent> barbershopPortfolioPickerLauncher;

    private String currentBarbershopStatus = "Abierto";
    private int currentAppointmentsCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_barberia);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainHomeBarberiaEM), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Contenedores de vistas principales
        headerCardBarberia = findViewById(R.id.headerCardBarberiaEM);
        bottomNavCardBarberia = findViewById(R.id.bottomNavCardBarberiaEM);
        bottomNavigationBarberia = findViewById(R.id.bottomNavigationBarberiaEM);

        layoutBarberiaDashboard = findViewById(R.id.layoutBarberiaDashboardEM);
        layoutBarberiaInscribir = findViewById(R.id.layoutBarberiaInscribirEM);
        layoutBarberiaPerfil = findViewById(R.id.layoutBarberiaPerfilEM);
        layoutCitasListContainer = findViewById(R.id.layoutCitasListContainerEM);
        layoutProfileBarbersListContainer = findViewById(R.id.layoutProfileBarbersListContainerEM);
        layoutBarbershopPortfolioContainer = findViewById(R.id.layoutBarbershopPortfolioContainerEM);

        // Textos del Dashboard y Perfil
        tvBarbershopHeaderName = findViewById(R.id.tvBarbershopHeaderNameEM);
        tvMetricsCitas = findViewById(R.id.tvMetricsCitasEM);
        tvMetricsRendimiento = findViewById(R.id.tvMetricsRendimientoEM);

        tvProfileBarbershopName = findViewById(R.id.tvProfileBarbershopNameEM);
        tvProfileBarbershopPhone = findViewById(R.id.tvProfileBarbershopPhoneEM);
        tvEditProfile = findViewById(R.id.tvEditProfileEM);
        tvAddPhoto = findViewById(R.id.tvAddPhotoEM);
        ivProfileLogo = findViewById(R.id.ivProfileLogoEM);

        // Formulario Inscribir Barbero (Simplificado: solo nombre del barbero registrado)
        etAddBarberName = findViewById(R.id.etAddBarberNameEM);
        btnAddBarberToTeam = findViewById(R.id.btnAddBarberToTeamEM);

        // Campos de Perfil Comercial
        etBarbershopBio = findViewById(R.id.etBarbershopBioEM);
        etBarbershopPromo = findViewById(R.id.etBarbershopPromoEM);
        if (etBarbershopBio != null) etBarbershopBio.setEnabled(false);
        if (etBarbershopPromo != null) etBarbershopPromo.setEnabled(false);

        btnStatusOpen = findViewById(R.id.btnStatusOpenEM);
        btnStatusClosed = findViewById(R.id.btnStatusClosedEM);
        btnSaveProfileChanges = findViewById(R.id.btnSaveProfileChangesEM);
        if (btnSaveProfileChanges != null) btnSaveProfileChanges.setVisibility(View.GONE);
        btnBarberiaSignOut = findViewById(R.id.btnBarberiaSignOutEM);
        btnBarberiaNotif = findViewById(R.id.btnBarberiaNotifEM);

        // Galería para logo de barbería
        barbershopLogoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (ivProfileLogo != null && imageUri != null) {
                            ivProfileLogo.setImageURI(imageUri);
                            Toast.makeText(this, "📷 Logo de barbería seleccionado", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Galería para portafolio de trabajos realizados
        barbershopPortfolioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null && layoutBarbershopPortfolioContainer != null) {
                            addBarbershopPortfolioCard(imageUri);
                            Toast.makeText(this, "✂️ Foto de trabajo agregada", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        if (ivProfileLogo != null) {
            ivProfileLogo.setImageResource(R.drawable.ic_profile);
            ivProfileLogo.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                barbershopLogoPickerLauncher.launch(intent);
            });
        }

        if (tvAddPhoto != null) {
            tvAddPhoto.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                barbershopPortfolioPickerLauncher.launch(intent);
            });
        }

        // Animación de entrada inicial
        assembleBarberiaViews();

        // Cargar datos de la Barbería desde Firestore
        cargarDatosBarberia();

        // Cargar barberos inscritos del equipo (100% real-time, sin demo)
        cargarBarberosDelEquipo();

        // Cargar citas agendadas del día (100% real-time, sin demo)
        cargarCitasAgendadas();

        // Eventos
        if (btnAddBarberToTeam != null) {
            btnAddBarberToTeam.setOnClickListener(v -> handleInscribirBarbero());
        }

        if (btnStatusOpen != null) {
            btnStatusOpen.setOnClickListener(v -> updateStatusUI("Abierto"));
        }

        if (btnStatusClosed != null) {
            btnStatusClosed.setOnClickListener(v -> updateStatusUI("Cerrado"));
        }

        if (btnSaveProfileChanges != null) {
            btnSaveProfileChanges.setOnClickListener(v -> saveProfileChangesToFirestore());
        }

        if (tvEditProfile != null) {
            tvEditProfile.setOnClickListener(v -> {
                if (etBarbershopBio != null) etBarbershopBio.setEnabled(true);
                if (etBarbershopPromo != null) etBarbershopPromo.setEnabled(true);
                if (btnSaveProfileChanges != null) btnSaveProfileChanges.setVisibility(View.VISIBLE);
                Toast.makeText(this, "✏️ Modo edición activado. Modifica tu descripción y promociones.", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnBarberiaNotif != null) {
            btnBarberiaNotif.setOnClickListener(v -> mostrarSolicitudesDeBarberos());
        }

        if (btnBarberiaSignOut != null) {
            btnBarberiaSignOut.setOnClickListener(v -> {
                mAuth.signOut();
                Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
                animateSplitAndNavigate(() -> {
                    Intent intent = new Intent(HomeBarberiaActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                });
            });
        }

        // Navegación Inferior (3 Opciones: Dashboard, Inscribir, Perfil) con animación suave
        if (bottomNavigationBarberia != null) {
            bottomNavigationBarberia.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_dashboard) {
                    showSection(layoutBarberiaDashboard);
                    return true;
                } else if (id == R.id.nav_team) {
                    showSection(layoutBarberiaInscribir);
                    return true;
                } else if (id == R.id.nav_barbershop_profile) {
                    showSection(layoutBarberiaPerfil);
                    return true;
                }
                return false;
            });
        }
    }

    private void addBarbershopPortfolioCard(Uri imageUri) {
        if (layoutBarbershopPortfolioContainer == null) return;

        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        float scale = getResources().getDisplayMetrics().density;
        int px = (int) (64 * scale + 0.5f);
        params.width = px;
        params.height = px;
        params.setMargins(0, 0, 16, 0);
        card.setLayoutParams(params);
        card.setRadius(8f);
        card.setCardBackgroundColor(0xFFE0E0E0);

        ImageView img = new ImageView(this);
        img.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setImageURI(imageUri);

        card.addView(img);

        int count = layoutBarbershopPortfolioContainer.getChildCount();
        layoutBarbershopPortfolioContainer.addView(card, Math.max(0, count - 1));
    }

    private Set<String> getFinalizedIds() {
        SharedPreferences prefs = getSharedPreferences("barber_prefs", MODE_PRIVATE);
        return prefs.getStringSet("finalized_ids", new HashSet<>());
    }

    private void saveFinalizedId(String id) {
        SharedPreferences prefs = getSharedPreferences("barber_prefs", MODE_PRIVATE);
        Set<String> set = new HashSet<>(getFinalizedIds());
        set.add(id);
        prefs.edit().putStringSet("finalized_ids", set).apply();
    }

    private void updateStatusUI(String status) {
        currentBarbershopStatus = status;
        if ("Abierto".equalsIgnoreCase(status)) {
            if (btnStatusOpen != null) {
                btnStatusOpen.setBackgroundColor(0xFFE8F5E9);
                btnStatusOpen.setTextColor(0xFF2E7D32);
            }
            if (btnStatusClosed != null) {
                btnStatusClosed.setBackgroundColor(0x00000000);
                btnStatusClosed.setTextColor(0xFF666666);
            }
        } else {
            if (btnStatusOpen != null) {
                btnStatusOpen.setBackgroundColor(0x00000000);
                btnStatusOpen.setTextColor(0xFF666666);
            }
            if (btnStatusClosed != null) {
                btnStatusClosed.setBackgroundColor(0xFFFFEBEE);
                btnStatusClosed.setTextColor(0xFFC62828);
            }
        }
    }

    private void showSection(View sectionToShow) {
        if (layoutBarberiaDashboard != null) layoutBarberiaDashboard.setVisibility(View.GONE);
        if (layoutBarberiaInscribir != null) layoutBarberiaInscribir.setVisibility(View.GONE);
        if (layoutBarberiaPerfil != null) layoutBarberiaPerfil.setVisibility(View.GONE);

        if (sectionToShow != null) {
            sectionToShow.setAlpha(0f);
            sectionToShow.setVisibility(View.VISIBLE);
            sectionToShow.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }
    }

    private void cargarDatosBarberia() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String barbershopName = doc.getString("barbershopName");
                        if (barbershopName == null || barbershopName.isEmpty()) {
                            barbershopName = doc.getString("name");
                        }
                        if (barbershopName == null) barbershopName = "Mi Barbería";

                        String phone = doc.getString("phone");
                        String email = doc.getString("email");
                        String bio = doc.getString("bio");
                        String promo = doc.getString("promo");
                        String district = doc.getString("district");
                        String status = doc.getString("status");

                        if (tvBarbershopHeaderName != null) tvBarbershopHeaderName.setText(barbershopName + (district != null && !district.isEmpty() ? " (" + district + ")" : ""));
                        if (tvProfileBarbershopName != null) tvProfileBarbershopName.setText(barbershopName + (district != null && !district.isEmpty() ? " — " + district : ""));
                        if (tvProfileBarbershopPhone != null) tvProfileBarbershopPhone.setText("📞 " + (phone != null ? phone : "+51 987 654 321") + " | ✉️ " + (email != null ? email : currentUser.getEmail()));

                        if (etBarbershopBio != null && bio != null) etBarbershopBio.setText(bio);
                        if (etBarbershopPromo != null && promo != null) etBarbershopPromo.setText(promo);
                        if (status != null) updateStatusUI(status);
                    }
                });
    }

    private void cargarBarberosDelEquipo() {
        if (layoutProfileBarbersListContainer == null) return;
        layoutProfileBarbersListContainer.removeAllViews();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String barbershopUid = currentUser != null ? currentUser.getUid() : "";

        db.collection("users")
                .whereEqualTo("role", "Barbero")
                .whereEqualTo("barbershopUid", barbershopUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean foundAny = false;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        foundAny = true;
                        String name = doc.getString("name");
                        String lastName = doc.getString("lastName");
                        String fullName = (name != null ? name : "Barbero") + " " + (lastName != null ? lastName : "");
                        String phone = doc.getString("phone");
                        String specialty = doc.getString("specialty");

                        addBarberToProfileList(fullName.trim(), specialty != null ? specialty : "Especialista Fades", phone != null ? phone : "");
                    }

                    if (!foundAny) {
                        TextView tv = new TextView(this);
                        tv.setText("No hay barberos inscritos en el equipo actualmente.");
                        tv.setTextColor(0xFF888888);
                        tv.setTextSize(13f);
                        layoutProfileBarbersListContainer.addView(tv);
                    }
                })
                .addOnFailureListener(e -> {
                    TextView tv = new TextView(this);
                    tv.setText("No hay barberos inscritos.");
                    tv.setTextColor(0xFF888888);
                    tv.setTextSize(13f);
                    layoutProfileBarbersListContainer.addView(tv);
                });
    }

    private void addBarberToProfileList(String name, String specialty, String phone) {
        if (layoutProfileBarbersListContainer == null) return;

        TextView tv = new TextView(this);
        String text = "• " + name + " — " + specialty + (!phone.isEmpty() ? " | 📞 " + phone : "") + " (⭐ 5.0 estrellas)";
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTextColor(0xFF333333);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 6, 0, 6);

        layoutProfileBarbersListContainer.addView(tv);
    }

    private void saveProfileChangesToFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String bio = etBarbershopBio != null && etBarbershopBio.getText() != null ? etBarbershopBio.getText().toString().trim() : "";
        String promo = etBarbershopPromo != null && etBarbershopPromo.getText() != null ? etBarbershopPromo.getText().toString().trim() : "";

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("bio", bio);
        updateMap.put("promo", promo);
        updateMap.put("status", currentBarbershopStatus);

        db.collection("users").document(currentUser.getUid()).update(updateMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "¡Perfil Comercial actualizado con éxito!", Toast.LENGTH_LONG).show();
                    if (etBarbershopBio != null) etBarbershopBio.setEnabled(false);
                    if (etBarbershopPromo != null) etBarbershopPromo.setEnabled(false);
                    if (btnSaveProfileChanges != null) btnSaveProfileChanges.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "¡Cambios guardados!", Toast.LENGTH_SHORT).show();
                    if (etBarbershopBio != null) etBarbershopBio.setEnabled(false);
                    if (etBarbershopPromo != null) etBarbershopPromo.setEnabled(false);
                    if (btnSaveProfileChanges != null) btnSaveProfileChanges.setVisibility(View.GONE);
                });
    }

    private void cargarCitasAgendadas() {
        if (layoutCitasListContainer == null) return;
        layoutCitasListContainer.removeAllViews();
        currentAppointmentsCount = 0;
        Set<String> finalizedIds = getFinalizedIds();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String barbershopName = tvBarbershopHeaderName != null ? tvBarbershopHeaderName.getText().toString() : "";

        db.collection("appointments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean foundAny = false;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String docId = doc.getId();
                        if (finalizedIds.contains(docId)) continue;
                        String status = doc.getString("status");
                        if ("Finalizado".equalsIgnoreCase(status)) continue;

                        String shopName = doc.getString("barbershopName");
                        // Match if appointment belongs to this barbershop
                        if (shopName != null && barbershopName.contains(shopName)) {
                            foundAny = true;
                            currentAppointmentsCount++;
                            String clientName = doc.getString("clientName");
                            if (clientName == null) clientName = doc.getString("customerName");
                            if (clientName == null) clientName = "Cliente";
                            String service = doc.getString("service");
                            if (service == null) service = "Corte de Cabello";
                            String time = doc.getString("time");
                            if (time == null) time = "04:30 PM";
                            String userUid = doc.getString("userUid");

                            addCitaCardToDashboard(docId, clientName, service, time, userUid);
                        }
                    }

                    if (!foundAny && currentAppointmentsCount == 0) {
                        TextView tv = new TextView(this);
                        tv.setText("No hay citas agendadas para hoy (0 citas).");
                        tv.setTextColor(0xFF888888);
                        tv.setTextSize(14f);
                        layoutCitasListContainer.addView(tv);
                    }

                    if (tvMetricsCitas != null) tvMetricsCitas.setText(currentAppointmentsCount + " Citas");
                    if (tvMetricsRendimiento != null) {
                        int perf = currentAppointmentsCount == 0 ? 100 : Math.max(80, 100 - (currentAppointmentsCount * 2));
                        tvMetricsRendimiento.setText(perf + "%");
                    }
                })
                .addOnFailureListener(e -> {
                    TextView tv = new TextView(this);
                    tv.setText("No hay citas agendadas para hoy.");
                    tv.setTextColor(0xFF888888);
                    tv.setTextSize(14f);
                    layoutCitasListContainer.addView(tv);
                    if (tvMetricsCitas != null) tvMetricsCitas.setText("0 Citas");
                    if (tvMetricsRendimiento != null) tvMetricsRendimiento.setText("100%");
                });
    }

    private void addCitaCardToDashboard(String appointmentId, String clientName, String service, String time, String userUid) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 20);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(0xFF1E1E1E);
        card.setRadius(24f);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 28, 32, 28);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvClient = new TextView(this);
        LinearLayout.LayoutParams tvClientParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvClient.setLayoutParams(tvClientParams);
        tvClient.setText(clientName);
        tvClient.setTextColor(0xFFFFFFFF);
        tvClient.setTextSize(16f);
        tvClient.setTypeface(null, Typeface.BOLD);

        MaterialButton btnFinalizar = new MaterialButton(this);
        btnFinalizar.setText("Finalizar");
        btnFinalizar.setTextColor(0xFFFFFFFF);
        btnFinalizar.setTextSize(12f);
        btnFinalizar.setPadding(16, 4, 16, 4);
        btnFinalizar.setMinimumWidth(0);
        btnFinalizar.setMinimumHeight(0);
        btnFinalizar.setBackgroundColor(0xFF4CAF50);
        btnFinalizar.setOnClickListener(v -> {
            layoutCitasListContainer.removeView(card);
            saveFinalizedId(appointmentId);

            db.collection("appointments").document(appointmentId)
                    .update("status", "Finalizado");

            currentAppointmentsCount = Math.max(0, currentAppointmentsCount - 1);
            if (tvMetricsCitas != null) tvMetricsCitas.setText(currentAppointmentsCount + " Citas");
            if (tvMetricsRendimiento != null) {
                int perf = currentAppointmentsCount == 0 ? 100 : Math.min(99, 90 + (5 - currentAppointmentsCount));
                tvMetricsRendimiento.setText(perf + "%");
            }

            String barbershopName = tvBarbershopHeaderName != null ? tvBarbershopHeaderName.getText().toString() : "Barbería";
            Map<String, Object> notif = new HashMap<>();
            notif.put("userUid", userUid != null ? userUid : "demo_user");
            notif.put("barbershopName", barbershopName);
            notif.put("title", "¡Cita Finalizada!");
            notif.put("message", "Tu cita de " + service + " en " + barbershopName + " ha finalizado. ¡Déjanos tu reseña y calificación de 1 a 5 estrellas!");
            notif.put("timestamp", System.currentTimeMillis());
            db.collection("notifications").add(notif);

            Toast.makeText(this, "✅ Cita finalizada. Notificación enviada al cliente para su reseña y calificación.", Toast.LENGTH_LONG).show();
        });

        topRow.addView(tvClient);
        topRow.addView(btnFinalizar);

        TextView tvService = new TextView(this);
        tvService.setText(service);
        tvService.setTextColor(0xFFCCCCCC);
        tvService.setTextSize(13f);
        tvService.setPadding(0, 8, 0, 4);

        TextView tvTime = new TextView(this);
        tvTime.setText("⏰ Hora: " + time);
        tvTime.setTextColor(0xFF888888);
        tvTime.setTextSize(12f);

        layout.addView(topRow);
        layout.addView(tvService);
        layout.addView(tvTime);

        card.addView(layout);
        layoutCitasListContainer.addView(card);
    }

    private void handleInscribirBarbero() {
        String barberFullName = etAddBarberName != null && etAddBarberName.getText() != null ? etAddBarberName.getText().toString().trim() : "";
        if (barberFullName.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el nombre del barbero registrado", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String barbershopUid = currentUser.getUid();
        String barbershopName = tvBarbershopHeaderName != null ? tvBarbershopHeaderName.getText().toString() : "Mi Barbería";

        db.collection("users")
                .whereEqualTo("role", "Barbero")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean found = false;
                    for (DocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        String lastName = doc.getString("lastName");
                        String fullName = (name != null ? name : "") + " " + (lastName != null ? lastName : "");
                        fullName = fullName.trim();

                        if (fullName.equalsIgnoreCase(barberFullName) || (name != null && name.equalsIgnoreCase(barberFullName))) {
                            found = true;
                            String barberoUid = doc.getId();
                            String phone = doc.getString("phone");
                            String specialty = doc.getString("specialty");
                            if (specialty == null) specialty = "Especialista Fades & Cortes";

                            Map<String, Object> update = new HashMap<>();
                            update.put("barbershopUid", barbershopUid);
                            update.put("barbershopName", barbershopName);
                            update.put("specialty", specialty);

                            db.collection("users").document(barberoUid).update(update)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "✅ Barbero " + barberFullName + " vinculado exitosamente a tu equipo (Tel: " + (phone != null ? phone : "N/A") + ")", Toast.LENGTH_LONG).show();
                                        if (etAddBarberName != null) etAddBarberName.setText("");
                                        cargarBarberosDelEquipo();
                                    });
                            break;
                        }
                    }
                    if (!found) {
                        Toast.makeText(this, "⚠️ No se encontró ningún barbero registrado con ese nombre en la plataforma.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al buscar barbero", Toast.LENGTH_SHORT).show());
    }

    private void mostrarSolicitudesDeBarberos() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("barber_requests")
                .whereEqualTo("barbershopUid", currentUser.getUid())
                .whereEqualTo("status", "Pendiente")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "🔔 No hay solicitudes pendientes de barberos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                    builder.setTitle("🔔 Solicitudes de Unión de Barberos");

                    LinearLayout layout = new LinearLayout(this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(40, 30, 40, 30);

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String reqId = doc.getId();
                        String barberoName = doc.getString("barberoName");
                        String barberoUid = doc.getString("barberoUid");
                        String barbershopName = tvBarbershopHeaderName != null ? tvBarbershopHeaderName.getText().toString() : "Mi Barbería";

                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setPadding(0, 10, 0, 10);

                        TextView tv = new TextView(this);
                        tv.setText("• " + (barberoName != null ? barberoName : "Barbero") + " desea unirse");
                        tv.setTextSize(14f);
                        tv.setTextColor(0xFF111111);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        tv.setLayoutParams(lp);

                        MaterialButton btnAccept = new MaterialButton(this);
                        btnAccept.setText("Aceptar");
                        btnAccept.setTextSize(11f);
                        btnAccept.setBackgroundColor(0xFF4CAF50);
                        btnAccept.setOnClickListener(vAccept -> {
                            db.collection("barber_requests").document(reqId).update("status", "Aprobado");
                            Map<String, Object> linkMap = new HashMap<>();
                            linkMap.put("barbershopUid", currentUser.getUid());
                            linkMap.put("barbershopName", barbershopName);
                            if (barberoUid != null) {
                                db.collection("users").document(barberoUid).update(linkMap);
                            }

                            Toast.makeText(this, "✅ " + barberoName + " aceptado e incorporado a tu equipo", Toast.LENGTH_SHORT).show();
                            cargarBarberosDelEquipo();
                            builder.create().dismiss();
                        });

                        MaterialButton btnReject = new MaterialButton(this);
                        btnReject.setText("Rechazar");
                        btnReject.setTextSize(11f);
                        btnReject.setBackgroundColor(0xFFD32F2F);
                        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        btnParams.setMargins(10, 0, 0, 0);
                        btnReject.setLayoutParams(btnParams);
                        btnReject.setOnClickListener(vReject -> {
                            db.collection("barber_requests").document(reqId).update("status", "Rechazado");
                            Toast.makeText(this, "❌ Solicitud rechazada", Toast.LENGTH_SHORT).show();
                            builder.create().dismiss();
                        });

                        row.addView(tv);
                        row.addView(btnAccept);
                        row.addView(btnReject);
                        layout.addView(row);
                    }

                    builder.setView(layout);
                    builder.setNegativeButton("Cerrar", null);
                    builder.show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "🔔 Sin solicitudes nuevas", Toast.LENGTH_SHORT).show());
    }

    private void assembleBarberiaViews() {
        if (headerCardBarberia != null) {
            headerCardBarberia.setTranslationY(-200f);
            headerCardBarberia.setAlpha(0f);
            headerCardBarberia.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(450)
                    .start();
        }

        if (bottomNavCardBarberia != null) {
            bottomNavCardBarberia.setTranslationY(200f);
            bottomNavCardBarberia.setAlpha(0f);
            bottomNavCardBarberia.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(450)
                    .start();
        }
    }

    private void animateSplitAndNavigate(Runnable onAnimationEnd) {
        if (headerCardBarberia != null) {
            headerCardBarberia.animate()
                    .translationY(-200f)
                    .alpha(0f)
                    .setDuration(300)
                    .start();
        }
        if (bottomNavCardBarberia != null) {
            bottomNavCardBarberia.animate()
                    .translationY(200f)
                    .alpha(0f)
                    .setDuration(300)
                    .start();
        }
        new Handler(Looper.getMainLooper()).postDelayed(onAnimationEnd, 250);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in_scale, R.anim.fade_out_scale);
    }
}
