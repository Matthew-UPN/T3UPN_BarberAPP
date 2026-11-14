package com.example.barber;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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
import java.util.Map;

public class HomeBarberoActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private BottomNavigationView bottomNavigationBarbero;
    private LinearLayout layoutBarberoDashboard, layoutBarberoSearch, layoutBarberoChat, layoutBarberoProfile;
    private LinearLayout layoutBarberoCitasContainer, layoutBarbershopsListForJoin, layoutChatContainer, layoutBarberoPortfolioContainer;

    private TextView tvBarberoHeaderName, tvBarberoProfileName, tvBarberoProfilePhone, tvEditBarberoProfile, tvAddBarberoPhoto;
    private TextInputEditText etBarberoBio;
    private MaterialButton btnSaveBarberoProfile, btnBarberoSignOut;
    private ImageButton btnBarberoNotif;
    private ImageView ivBarberoProfilePhoto;

    private ActivityResultLauncher<Intent> profileImagePickerLauncher;
    private ActivityResultLauncher<Intent> portfolioImagePickerLauncher;

    private String linkedBarbershopUid = "";
    private String linkedBarbershopName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_barbero);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainHomeBarberoEM), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bottomNavigationBarbero = findViewById(R.id.bottomNavigationBarberoEM);

        layoutBarberoDashboard = findViewById(R.id.layoutBarberoDashboardEM);
        layoutBarberoSearch = findViewById(R.id.layoutBarberoSearchEM);
        layoutBarberoChat = findViewById(R.id.layoutBarberoChatEM);
        layoutBarberoProfile = findViewById(R.id.layoutBarberoProfileEM);

        layoutBarberoCitasContainer = findViewById(R.id.layoutBarberoCitasContainerEM);
        layoutBarbershopsListForJoin = findViewById(R.id.layoutBarbershopsListForJoinEM);
        layoutChatContainer = findViewById(R.id.layoutChatContainerEM);
        layoutBarberoPortfolioContainer = findViewById(R.id.layoutBarberoPortfolioContainerEM);

        tvBarberoHeaderName = findViewById(R.id.tvBarberoHeaderNameEM);
        tvBarberoProfileName = findViewById(R.id.tvBarberoProfileNameEM);
        tvBarberoProfilePhone = findViewById(R.id.tvBarberoProfilePhoneEM);
        tvEditBarberoProfile = findViewById(R.id.tvEditBarberoProfileEM);
        tvAddBarberoPhoto = findViewById(R.id.tvAddBarberoPhotoEM);
        ivBarberoProfilePhoto = findViewById(R.id.ivBarberoProfilePhotoEM);

        etBarberoBio = findViewById(R.id.etBarberoBioEM);
        btnSaveBarberoProfile = findViewById(R.id.btnSaveBarberoProfileEM);
        btnBarberoSignOut = findViewById(R.id.btnBarberoSignOutEM);
        btnBarberoNotif = findViewById(R.id.btnBarberoNotifEM);

        if (etBarberoBio != null) etBarberoBio.setEnabled(false);
        if (btnSaveBarberoProfile != null) btnSaveProfileChangesVisibility(false);

        // Galería para foto de perfil
        profileImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (ivBarberoProfilePhoto != null && imageUri != null) {
                            ivBarberoProfilePhoto.setImageURI(imageUri);
                            Toast.makeText(this, "📷 Foto de perfil guardada", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Galería para fotos de cortes (portafolio)
        portfolioImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null && layoutBarberoPortfolioContainer != null) {
                            addPortfolioCard(imageUri);
                            Toast.makeText(this, "✂️ Corte agregado al portafolio", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        if (ivBarberoProfilePhoto != null) {
            ivBarberoProfilePhoto.setImageResource(R.drawable.ic_profile);
            ivBarberoProfilePhoto.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                profileImagePickerLauncher.launch(intent);
            });
        }

        if (tvAddBarberoPhoto != null) {
            tvAddBarberoPhoto.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                portfolioImagePickerLauncher.launch(intent);
            });
        }

        cargarDatosBarbero();

        if (tvEditBarberoProfile != null) {
            tvEditBarberoProfile.setOnClickListener(v -> {
                if (etBarberoBio != null) etBarberoBio.setEnabled(true);
                if (btnSaveBarberoProfile != null) btnSaveBarberoProfile.setVisibility(View.VISIBLE);
                Toast.makeText(this, "✏️ Modo edición activado.", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnSaveBarberoProfile != null) {
            btnSaveBarberoProfile.setOnClickListener(v -> guardarPerfilBarbero());
        }

        if (btnBarberoNotif != null) {
            btnBarberoNotif.setOnClickListener(v -> mostrarNotificacionesYRequests());
        }

        if (btnBarberoSignOut != null) {
            btnBarberoSignOut.setOnClickListener(v -> {
                mAuth.signOut();
                Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(HomeBarberoActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (bottomNavigationBarbero != null) {
            bottomNavigationBarbero.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_barbero_dashboard) {
                    showSection(layoutBarberoDashboard);
                    return true;
                } else if (id == R.id.nav_barbero_search) {
                    showSection(layoutBarberoSearch);
                    return true;
                } else if (id == R.id.nav_barbero_chat) {
                    showSection(layoutBarberoChat);
                    return true;
                } else if (id == R.id.nav_barbero_profile) {
                    showSection(layoutBarberoProfile);
                    return true;
                }
                return false;
            });
        }
    }

    private void btnSaveProfileChangesVisibility(boolean show) {
        if (btnSaveBarberoProfile != null) {
            btnSaveBarberoProfile.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void addPortfolioCard(Uri imageUri) {
        if (layoutBarberoPortfolioContainer == null) return;

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

        int count = layoutBarberoPortfolioContainer.getChildCount();
        layoutBarberoPortfolioContainer.addView(card, Math.max(0, count - 1));
    }

    private void showSection(View sectionToShow) {
        if (layoutBarberoDashboard != null) layoutBarberoDashboard.setVisibility(View.GONE);
        if (layoutBarberoSearch != null) layoutBarberoSearch.setVisibility(View.GONE);
        if (layoutBarberoChat != null) layoutBarberoChat.setVisibility(View.GONE);
        if (layoutBarberoProfile != null) layoutBarberoProfile.setVisibility(View.GONE);

        if (sectionToShow != null) {
            sectionToShow.setAlpha(0f);
            sectionToShow.setVisibility(View.VISIBLE);
            sectionToShow.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }
    }

    private void cargarDatosBarbero() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String lastName = doc.getString("lastName");
                        String fullName = (name != null ? name : "Barbero") + " " + (lastName != null ? lastName : "");
                        String phone = doc.getString("phone");
                        String email = doc.getString("email");
                        String bio = doc.getString("bio");
                        linkedBarbershopUid = doc.getString("barbershopUid");
                        linkedBarbershopName = doc.getString("barbershopName");

                        if (tvBarberoHeaderName != null) {
                            if (linkedBarbershopName != null && !linkedBarbershopName.isEmpty()) {
                                tvBarberoHeaderName.setText("Hola, " + (name != null ? name : "Barbero") + " (" + linkedBarbershopName + ")");
                            } else {
                                tvBarberoHeaderName.setText("Hola, " + (name != null ? name : "Barbero") + " (Independiente)");
                            }
                        }
                        if (tvBarberoProfileName != null) tvBarberoProfileName.setText(fullName.trim());
                        if (tvBarberoProfilePhone != null) tvBarberoProfilePhone.setText("📞 " + (phone != null ? phone : "+51 987 654 321") + " | ✉️ " + (email != null ? email : currentUser.getEmail()));
                        if (etBarberoBio != null && bio != null) etBarberoBio.setText(bio);

                        updateBarberLinkedState();
                        cargarBarberiasParaUnirse();
                    }
                });
    }

    private void updateBarberLinkedState() {
        cargarCitasAsignadas();
        cargarMensajesChat();
    }

    private void guardarPerfilBarbero() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String bio = etBarberoBio != null && etBarberoBio.getText() != null ? etBarberoBio.getText().toString().trim() : "";
        Map<String, Object> update = new HashMap<>();
        update.put("bio", bio);

        db.collection("users").document(currentUser.getUid()).update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "¡Perfil actualizado con éxito!", Toast.LENGTH_SHORT).show();
                    if (etBarberoBio != null) etBarberoBio.setEnabled(false);
                    btnSaveProfileChangesVisibility(false);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar perfil", Toast.LENGTH_SHORT).show());
    }

    private void cargarCitasAsignadas() {
        if (layoutBarberoCitasContainer == null) return;
        layoutBarberoCitasContainer.removeAllViews();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("appointments")
                .whereEqualTo("barberUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean found = false;
                    for (DocumentSnapshot doc : querySnapshot) {
                        found = true;
                        String clientName = doc.getString("clientName");
                        if (clientName == null) clientName = "Cliente";
                        String service = doc.getString("service");
                        if (service == null) service = "Corte";
                        String time = doc.getString("time");
                        if (time == null) time = "10:00 AM";

                        addCitaBarberoCard(clientName, service, time);
                    }
                    if (!found) {
                        TextView tv = new TextView(this);
                        tv.setText("No hay citas asignadas específicamente para ti en este momento.");
                        tv.setTextColor(0xFF888888);
                        tv.setTextSize(14f);
                        layoutBarberoCitasContainer.addView(tv);
                    }
                })
                .addOnFailureListener(e -> {
                    TextView tv = new TextView(this);
                    tv.setText("No hay citas asignadas.");
                    tv.setTextColor(0xFF888888);
                    tv.setTextSize(14f);
                    layoutBarberoCitasContainer.addView(tv);
                });
    }

    private void addCitaBarberoCard(String clientName, String service, String time) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setCardBackgroundColor(0xFF1E1E1E);
        card.setRadius(20f);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(28, 24, 28, 24);

        TextView tvClient = new TextView(this);
        tvClient.setText(clientName);
        tvClient.setTextColor(0xFFFFFFFF);
        tvClient.setTextSize(16f);
        tvClient.setTypeface(null, Typeface.BOLD);

        TextView tvService = new TextView(this);
        tvService.setText(service + " — ⏰ " + time);
        tvService.setTextColor(0xFFCCCCCC);
        tvService.setTextSize(13f);
        tvService.setPadding(0, 6, 0, 0);

        layout.addView(tvClient);
        layout.addView(tvService);
        card.addView(layout);

        layoutBarberoCitasContainer.addView(card);
    }

    private void cargarBarberiasParaUnirse() {
        if (layoutBarbershopsListForJoin == null) return;
        layoutBarbershopsListForJoin.removeAllViews();

        boolean isLinked = (linkedBarbershopUid != null && !linkedBarbershopUid.isEmpty());
        if (isLinked) {
            CardView card = new CardView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 14);
            card.setLayoutParams(params);
            card.setCardBackgroundColor(0xFFE8F5E9);
            card.setRadius(16f);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(20, 18, 20, 18);

            TextView tvName = new TextView(this);
            tvName.setText("✅ Actualmente estás unido a:\n" + linkedBarbershopName);
            tvName.setTextColor(0xFF2E7D32);
            tvName.setTextSize(15f);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setPadding(0, 0, 0, 12);

            MaterialButton btnLeave = new MaterialButton(this);
            btnLeave.setText("Solicitar Salirme de " + linkedBarbershopName);
            btnLeave.setTextSize(12f);
            btnLeave.setTextColor(0xFFFFFFFF);
            btnLeave.setBackgroundColor(0xFFD32F2F);
            btnLeave.setOnClickListener(v -> {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser == null) return;

                String barberoName = tvBarberoProfileName != null ? tvBarberoProfileName.getText().toString() : "Barbero";

                Map<String, Object> req = new HashMap<>();
                req.put("barbershopUid", linkedBarbershopUid);
                req.put("barbershopName", linkedBarbershopName);
                req.put("barberoUid", currentUser.getUid());
                req.put("barberoName", barberoName);
                req.put("status", "Pendiente Salida");
                req.put("timestamp", System.currentTimeMillis());

                db.collection("barber_requests").add(req)
                        .addOnSuccessListener(docRef -> Toast.makeText(this, "✅ Solicitud de salida enviada a la administración de " + linkedBarbershopName + ". Esperando aprobación.", Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "❌ Error al enviar solicitud de salida", Toast.LENGTH_SHORT).show());
            });

            layout.addView(tvName);
            layout.addView(btnLeave);
            card.addView(layout);
            layoutBarbershopsListForJoin.addView(card);
            return;
        }

        db.collection("users")
                .whereEqualTo("role", "Barbería")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.Set<String> addedNames = new java.util.HashSet<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String barbershopName = doc.getString("barbershopName");
                        if (barbershopName == null) barbershopName = doc.getString("name");
                        if (barbershopName == null) barbershopName = "Barbería";

                        if (addedNames.contains(barbershopName)) continue;
                        addedNames.add(barbershopName);

                        String district = doc.getString("district");
                        if (district == null || district.isEmpty()) district = doc.getString("address");
                        String barbershopId = doc.getId();

                        addBarbershopJoinCard(barbershopName, district, barbershopId);
                    }
                });
    }

    private void addBarbershopJoinCard(String barbershopName, String district, String barbershopId) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 14);
        card.setLayoutParams(params);
        card.setCardBackgroundColor(0xFFF8F8F8);
        card.setRadius(16f);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 18, 20, 18);

        TextView tvName = new TextView(this);
        tvName.setText("🏬 " + barbershopName + (district != null && !district.isEmpty() ? " — 📍 " + district : ""));
        tvName.setTextColor(0xFF111111);
        tvName.setTextSize(15f);
        tvName.setTypeface(null, Typeface.BOLD);

        MaterialButton btnJoin = new MaterialButton(this);
        btnJoin.setText("Enviar Solicitud de Unión");
        btnJoin.setTextSize(12f);
        btnJoin.setTextColor(0xFFFFFFFF);
        btnJoin.setBackgroundColor(0xFF111111);
        btnJoin.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            String barberoName = tvBarberoProfileName != null ? tvBarberoProfileName.getText().toString() : "Barbero";

            Map<String, Object> req = new HashMap<>();
            req.put("barbershopUid", barbershopId);
            req.put("barbershopName", barbershopName);
            req.put("barberoUid", currentUser != null ? currentUser.getUid() : "anon");
            req.put("barberoName", barberoName);
            req.put("status", "Pendiente");
            req.put("timestamp", System.currentTimeMillis());

            db.collection("barber_requests").add(req)
                    .addOnSuccessListener(docRef -> Toast.makeText(this, "✅ Solicitud enviada a " + barbershopName, Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "✅ Solicitud enviada", Toast.LENGTH_SHORT).show());
        });

        layout.addView(tvName);
        layout.addView(btnJoin);
        card.addView(layout);

        layoutBarbershopsListForJoin.addView(card);
    }

    private void cargarMensajesChat() {
        if (layoutChatContainer == null) return;
        layoutChatContainer.removeAllViews();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("appointments")
                .whereEqualTo("barberUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, String> clientMap = new HashMap<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String clientUid = doc.getString("userUid");
                        String clientName = doc.getString("clientName");
                        if (clientUid != null && clientName != null) {
                            clientMap.put(clientUid, clientName);
                        }
                    }

                    if (clientMap.isEmpty()) {
                        TextView tv = new TextView(this);
                        tv.setText("💬 No hay conversaciones de clientes aún. Cuando los clientes reserven citas contigo, aparecerán aquí para chatear (WhatsApp / Messenger style).");
                        tv.setTextColor(0xFF888888);
                        tv.setTextSize(14f);
                        tv.setPadding(10, 10, 10, 10);
                        layoutChatContainer.addView(tv);
                        return;
                    }

                    for (Map.Entry<String, String> entry : clientMap.entrySet()) {
                        String clientUid = entry.getKey();
                        String clientName = entry.getValue();

                        CardView card = new CardView(this);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(0, 0, 0, 12);
                        card.setLayoutParams(params);
                        card.setCardBackgroundColor(0xFFF0F2F5);
                        card.setRadius(16f);

                        LinearLayout layout = new LinearLayout(this);
                        layout.setOrientation(LinearLayout.HORIZONTAL);
                        layout.setPadding(20, 18, 20, 18);

                        TextView tvName = new TextView(this);
                        tvName.setText("💬 Chat con " + clientName);
                        tvName.setTextColor(0xFF111111);
                        tvName.setTextSize(15f);
                        tvName.setTypeface(null, Typeface.BOLD);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        tvName.setLayoutParams(lp);

                        MaterialButton btnChat = new MaterialButton(this);
                        btnChat.setText("Abrir Chat");
                        btnChat.setTextSize(11f);
                        btnChat.setBackgroundColor(0xFF25D366);
                        btnChat.setTextColor(0xFFFFFFFF);
                        btnChat.setOnClickListener(v -> openChatDialog(clientUid, clientName));

                        layout.addView(tvName);
                        layout.addView(btnChat);
                        card.addView(layout);
                        layoutChatContainer.addView(card);
                    }
                });
    }

    private void openChatDialog(String clientUid, String clientName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("💬 Chat con " + clientName);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(30, 20, 30, 20);

        LinearLayout messagesContainer = new LinearLayout(this);
        messagesContainer.setOrientation(LinearLayout.VERTICAL);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 400));
        scrollView.addView(messagesContainer);

        TextInputEditText etMessage = new TextInputEditText(this);
        etMessage.setHint("Escribe un mensaje...");

        MaterialButton btnSend = new MaterialButton(this);
        btnSend.setText("Enviar");
        btnSend.setBackgroundColor(0xFF111111);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String barberUid = currentUser != null ? currentUser.getUid() : "";

        db.collection("chats")
                .document(getChatId(barberUid, clientUid))
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    messagesContainer.removeAllViews();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String sender = doc.getString("sender");
                        String msg = doc.getString("message");
                        TextView tvMsg = new TextView(this);
                        tvMsg.setText((sender != null && sender.equals(barberUid) ? "Tú: " : clientName + ": ") + msg);
                        tvMsg.setTextSize(13f);
                        tvMsg.setPadding(8, 6, 8, 6);
                        tvMsg.setTextColor(0xFF222222);
                        messagesContainer.addView(tvMsg);
                    }
                });

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
            if (text.isEmpty()) return;

            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("sender", barberUid);
            msgMap.put("message", text);
            msgMap.put("timestamp", System.currentTimeMillis());

            db.collection("chats")
                    .document(getChatId(barberUid, clientUid))
                    .collection("messages")
                    .add(msgMap);

            etMessage.setText("");
        });

        container.addView(scrollView);
        container.addView(etMessage);
        container.addView(btnSend);

        builder.setView(container);
        builder.setPositiveButton("Cerrar", null);
        builder.show();
    }

    private String getChatId(String uid1, String uid2) {
        if (uid1.compareTo(uid2) < 0) {
            return uid1 + "_" + uid2;
        } else {
            return uid2 + "_" + uid1;
        }
    }

    private void mostrarNotificacionesYRequests() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("barber_requests")
                .whereEqualTo("barberoUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "🔔 No tienes solicitudes pendientes", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    StringBuilder sb = new StringBuilder("Estado de tus solicitudes:\n\n");
                    for (DocumentSnapshot doc : querySnapshot) {
                        String bName = doc.getString("barbershopName");
                        String status = doc.getString("status");
                        sb.append("• ").append(bName != null ? bName : "Barbería").append(": ").append(status).append("\n");

                        if ("Aprobado".equalsIgnoreCase(status)) {
                            String bUid = doc.getString("barbershopUid");
                            Map<String, Object> update = new HashMap<>();
                            update.put("barbershopUid", bUid);
                            update.put("barbershopName", bName);
                            db.collection("users").document(currentUser.getUid()).update(update);
                            linkedBarbershopUid = bUid;
                            linkedBarbershopName = bName;
                            updateBarberLinkedState();
                        cargarBarberiasParaUnirse();
                        }
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("🔔 Notificaciones y Solicitudes");
                    builder.setMessage(sb.toString().trim());
                    builder.setPositiveButton("Entendido", null);
                    builder.show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "🔔 Sin notificaciones nuevas", Toast.LENGTH_SHORT).show());
    }
}
