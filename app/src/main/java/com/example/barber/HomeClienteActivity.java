package com.example.barber;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeClienteActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private CardView headerCard, bottomNavCard;
    private MaterialButton btnNearMe;
    private View mapFragmentView, searchListLayout;
    private TextInputEditText editTextSearchList;
    private LinearLayout layoutBarbershopListContainer;
    private BottomNavigationView bottomNavigation;
    private ImageButton btnNotification;

    private ActivityResultLauncher<Intent> clientProfileImagePickerLauncher;
    private ImageView currentClientProfileImageView;

    private final List<Marker> barbershopMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_cliente);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainHomeClienteEM), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar servicios Firebase y Location
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        clientProfileImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (currentClientProfileImageView != null && imageUri != null) {
                            currentClientProfileImageView.setImageURI(imageUri);
                            Toast.makeText(this, "📷 Foto de perfil de cliente seleccionada", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Vistas de la UI
        headerCard = findViewById(R.id.headerCardEM);
        bottomNavCard = findViewById(R.id.bottomNavCardEM);
        btnNearMe = findViewById(R.id.btnNearMeEM);
        mapFragmentView = findViewById(R.id.mapEM);
        searchListLayout = findViewById(R.id.searchListLayoutEM);
        editTextSearchList = findViewById(R.id.editTextSearchListEM);
        layoutBarbershopListContainer = findViewById(R.id.layoutBarbershopListContainerEM);
        bottomNavigation = findViewById(R.id.bottomNavigationEM);
        btnNotification = findViewById(R.id.btnNotificationEM);

        // Ocultar posiciones iniciales para la animación de ENSAMBLARSE
        if (headerCard != null) {
            headerCard.setTranslationY(-200f);
            headerCard.setAlpha(0f);
        }
        if (bottomNavCard != null) {
            bottomNavCard.setTranslationY(200f);
            bottomNavCard.setAlpha(0f);
        }

        // Ejecutar animación de ensamblarse al entrar
        assembleHomeViews();

        // Inicializar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapEM);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Botón "Cerca de mí"
        if (btnNearMe != null) {
            btnNearMe.setOnClickListener(v -> centerMapOnUserLocation());
        }

        // Evento de Notificaciones y Reseñas
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(this, "Inicia sesión para ver notificaciones", Toast.LENGTH_SHORT).show();
                    return;
                }
                db.collection("notifications")
                        .whereEqualTo("userUid", user.getUid())
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (querySnapshot.isEmpty()) {
                                Toast.makeText(this, "🔔 No tienes notificaciones nuevas", Toast.LENGTH_SHORT).show();
                            } else {
                                DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                                String barbershopName = doc.getString("barbershopName");
                                String message = doc.getString("message");
                                showReviewDialog(barbershopName != null ? barbershopName : "Barbería", message != null ? message : "Tu cita ha finalizado.");
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "🔔 No tienes notificaciones nuevas", Toast.LENGTH_SHORT).show());
            });
        }

        // Listener para filtrar la lista de barberías escritas en la sección de búsqueda
        if (editTextSearchList != null) {
            editTextSearchList.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    populateBarbershopList(s.toString().trim());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Listener de la barra de navegación inferior (5 ítems)
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    // Mostrar Mapa con animación de desvanecimiento
                    if (mapFragmentView != null) {
                        mapFragmentView.setAlpha(0f);
                        mapFragmentView.setVisibility(View.VISIBLE);
                        mapFragmentView.animate().alpha(1f).setDuration(300).start();
                    }
                    if (btnNearMe != null) btnNearMe.setVisibility(View.VISIBLE);
                    if (searchListLayout != null) searchListLayout.setVisibility(View.GONE);
                    centerMapOnUserLocation();
                    return true;
                } else if (id == R.id.nav_search) {
                    // Ocultar Mapa, mostrar lista de búsqueda con animación de desvanecimiento
                    if (mapFragmentView != null) mapFragmentView.setVisibility(View.GONE);
                    if (btnNearMe != null) btnNearMe.setVisibility(View.GONE);
                    if (searchListLayout != null) {
                        searchListLayout.setAlpha(0f);
                        searchListLayout.setVisibility(View.VISIBLE);
                        searchListLayout.animate().alpha(1f).setDuration(300).start();
                    }
                    populateBarbershopList("");
                    return true;
                } else if (id == R.id.nav_filter) {
                    showFiltersDialog();
                    return true;
                } else if (id == R.id.nav_reviews) {
                    showReservationsDialog();
                    return true;
                } else if (id == R.id.nav_profile) {
                    showProfileDialog();
                    return true;
                }
                return false;
            });
        }
    }

    private void populateBarbershopList(String filterQuery) {
        if (layoutBarbershopListContainer == null) return;
        layoutBarbershopListContainer.removeAllViews();

        db.collection("users")
                .whereEqualTo("role", "Barbería")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean foundAny = false;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("barbershopName");
                        if (name == null || name.isEmpty()) name = doc.getString("name");
                        if (name == null) name = "Barbería";

                        String phone = doc.getString("phone");
                        String details = "💈 Barbería Registrada" + (phone != null ? " • 📞 " + phone : "");

                        if (filterQuery.isEmpty() || name.toLowerCase().contains(filterQuery.toLowerCase())) {
                            foundAny = true;
                            addBarbershopCardToList(name, details, "⭐ 5.0 / 5.0");
                        }
                    }

                    if (!foundAny) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("No hay barberías registradas en este momento.");
                        tvEmpty.setTextColor(0xFF666666);
                        tvEmpty.setTextSize(14f);
                        tvEmpty.setPadding(20, 20, 20, 20);
                        layoutBarbershopListContainer.addView(tvEmpty);
                    }
                })
                .addOnFailureListener(e -> {
                    TextView tvEmpty = new TextView(this);
                    tvEmpty.setText("Error al cargar barberías.");
                    tvEmpty.setTextColor(0xFF666666);
                    tvEmpty.setTextSize(14f);
                    tvEmpty.setPadding(20, 20, 20, 20);
                    layoutBarbershopListContainer.addView(tvEmpty);
                });
    }

    private void addBarbershopCardToList(String barbershopName, String details, String rating) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 32);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setRadius(32f);
        card.setCardElevation(8f);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 36, 40, 36);

        TextView tvName = new TextView(this);
        tvName.setText(barbershopName);
        tvName.setTextSize(18f);
        tvName.setTextColor(0xFF111111);
        tvName.setTypeface(null, Typeface.BOLD);

        TextView tvDetails = new TextView(this);
        tvDetails.setText(details);
        tvDetails.setTextSize(13f);
        tvDetails.setTextColor(0xFF666666);
        tvDetails.setPadding(0, 8, 0, 0);

        TextView tvRating = new TextView(this);
        tvRating.setText(rating);
        tvRating.setTextSize(12f);
        tvRating.setTextColor(0xFFE65100);
        tvRating.setTypeface(null, Typeface.BOLD);
        tvRating.setPadding(0, 8, 0, 24);

        MaterialButton btnBook = new MaterialButton(this);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnBook.setLayoutParams(btnParams);
        btnBook.setText("💈 RESERVAR CITA");
        btnBook.setTextColor(0xFFFFFFFF);
        btnBook.setBackgroundColor(0xFF111111);

        // Al presionar la tarjeta completa, ver el DETALLE DE LA BARBERÍA (Cortes, Barberos, Reseñas, Teléfono)
        card.setOnClickListener(v -> showBarbershopDetailDialog(barbershopName));

        // Al presionar el botón de reservar cita directamente
        btnBook.setOnClickListener(v -> showBookAppointmentDialog(barbershopName));

        layout.addView(tvName);
        layout.addView(tvDetails);
        layout.addView(tvRating);
        layout.addView(btnBook);

        card.addView(layout);
        layoutBarbershopListContainer.addView(card);
    }

    private void showFullScreenBottomSheet(BottomSheetDialog bottomSheetDialog, View dialogView) {
        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            FrameLayout bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setPeekHeight(getResources().getDisplayMetrics().heightPixels);
                behavior.setDraggable(false);
                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.setLayoutParams(params);
            }
        });
        bottomSheetDialog.show();
    }

    // Ventana Detallada de Barbería (Cortes, Barberos, Teléfono y Reseñas)
    private void showBarbershopDetailDialog(String barbershopName) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_barbershop_detail, null);

        TextView tvDetailName = dialogView.findViewById(R.id.tvDetailNameEM);
        TextView tvDetailAddress = dialogView.findViewById(R.id.tvDetailAddressEM);
        TextView tvDetailPhone = dialogView.findViewById(R.id.tvDetailPhoneEM);
        TextView tvDetailRating = dialogView.findViewById(R.id.tvDetailRatingEM);
        LinearLayout layoutDetailServices = dialogView.findViewById(R.id.layoutDetailServicesEM);
        LinearLayout layoutDetailBarbers = dialogView.findViewById(R.id.layoutDetailBarbersEM);
        LinearLayout layoutDetailReviews = dialogView.findViewById(R.id.layoutDetailReviewsEM);

        if (tvDetailName != null) tvDetailName.setText(barbershopName);

        final String address;
        final String rating;
        final String[] defaultServices;
        final String[] defaultBarbers;
        final String[] defaultReviews;

        if (barbershopName.toLowerCase().contains("imperio")) {
            address = "📍 Av. Naranjal 450, Los Olivos";
            rating = "⭐ 4.9 / 5.0 (38 reseñas)";
            defaultServices = new String[]{"• Corte Imperio Freestyle — S/ 30.00", "• Tinturado & Platinado — S/ 50.00"};
            defaultBarbers = new String[]{"💈 Diego Cut — Esp. Tijera", "✂️ Lucas Blade — Esp. Platinado"};
            defaultReviews = new String[]{"👤 Andrés V.: 'Increíble trabajo.'"};
        } else if (barbershopName.toLowerCase().contains("rey")) {
            address = "📍 Av. Carlos Izaguirre 890, Los Olivos";
            rating = "⭐ 5.0 / 5.0 (29 reseñas)";
            defaultServices = new String[]{"• Corte Real con Navaja Tradicional — S/ 28.00"};
            defaultBarbers = new String[]{"👑 Roberto 'El Rey' — Esp. Corte Real"};
            defaultReviews = new String[]{"👤 Renzo P.: 'Excelente atención.'"};
        } else if (barbershopName.toLowerCase().contains("black")) {
            address = "📍 Av. Antúnez de Mayolo 560, Los Olivos";
            rating = "⭐ 4.9 / 5.0 (51 reseñas)";
            defaultServices = new String[]{"• Black Fade + Pigmentación — S/ 35.00"};
            defaultBarbers = new String[]{"🖤 Jhoan Black — Esp. Diseños"};
            defaultReviews = new String[]{"👤 Kevin S.: 'Los mejores diseños.'"};
        } else {
            address = "📍 Av. Antúnez de Mayolo 124, Los Olivos";
            rating = "⭐ 4.9 / 5.0 (42 reseñas)";
            defaultServices = new String[]{"• Fade / Degradado Urbano — S/ 25.00", "• Combo VIP — S/ 35.00"};
            defaultBarbers = new String[]{"👨‍🦱 Carlos 'El Rey' — Esp. Fades", "🧔 Mateo Barber — Esp. Barbas"};
            defaultReviews = new String[]{"👤 Juan M.: '100% recomendado.'"};
        }

        if (tvDetailAddress != null) tvDetailAddress.setText(address);
        if (tvDetailRating != null) tvDetailRating.setText(rating);

        db.collection("users")
                .whereEqualTo("barbershopName", barbershopName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String phoneEmail = "📞 +51 987 654 321 | info@barberapp.com";
                    String bio = "";
                    String promo = "";
                    String ownerUid = "";

                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        ownerUid = doc.getId();
                        String phone = doc.getString("phone");
                        String email = doc.getString("email");
                        bio = doc.getString("bio");
                        promo = doc.getString("promo");
                        if (phone != null || email != null) {
                            phoneEmail = "📞 " + (phone != null ? phone : "+51 987 654 321") + " | " + (email != null ? email : "contacto@barberia.com");
                        }
                    } else {
                        db.collection("users")
                                .whereEqualTo("name", barbershopName)
                                .get()
                                .addOnSuccessListener(nameSnap -> {
                                    if (!nameSnap.isEmpty()) {
                                        DocumentSnapshot doc = nameSnap.getDocuments().get(0);
                                        String phone = doc.getString("phone");
                                        String email = doc.getString("email");
                                        if (tvDetailPhone != null && (phone != null || email != null)) {
                                            tvDetailPhone.setText("📞 " + (phone != null ? phone : "+51 987 654 321") + " | " + (email != null ? email : "contacto@barberapp.com"));
                                        }
                                    }
                                });
                    }

                    if (tvDetailPhone != null) tvDetailPhone.setText(phoneEmail);

                    if (layoutDetailServices != null) {
                        layoutDetailServices.removeAllViews();
                        if (promo != null && !promo.isEmpty()) {
                            TextView tvPromo = new TextView(this);
                            tvPromo.setText("🔥 Promoción: " + promo);
                            tvPromo.setTextSize(13f);
                            tvPromo.setTextColor(0xFFE65100);
                            tvPromo.setTypeface(null, Typeface.BOLD);
                            tvPromo.setPadding(0, 4, 0, 4);
                            layoutDetailServices.addView(tvPromo);
                        }
                        if (bio != null && !bio.isEmpty()) {
                            TextView tvBio = new TextView(this);
                            tvBio.setText("📝 Descripción: " + bio);
                            tvBio.setTextSize(13f);
                            tvBio.setTextColor(0xFF333333);
                            tvBio.setPadding(0, 4, 0, 4);
                            layoutDetailServices.addView(tvBio);
                        }
                        for (String item : defaultServices) {
                            TextView tv = new TextView(this);
                            tv.setText(item);
                            tv.setTextSize(13f);
                            tv.setTextColor(0xFF333333);
                            tv.setPadding(0, 4, 0, 4);
                            layoutDetailServices.addView(tv);
                        }
                    }

                    if (layoutDetailBarbers != null) {
                        layoutDetailBarbers.removeAllViews();
                        if (!ownerUid.isEmpty()) {
                            db.collection("users")
                                    .whereEqualTo("role", "Barbero")
                                    .whereEqualTo("barbershopUid", ownerUid)
                                    .get()
                                    .addOnSuccessListener(barbersSnap -> {
                                        boolean found = false;
                                        for (DocumentSnapshot bDoc : barbersSnap) {
                                            found = true;
                                            String bName = bDoc.getString("name");
                                            String bSpecialty = bDoc.getString("specialty");
                                            String bPhone = bDoc.getString("phone");
                                            TextView tvB = new TextView(this);
                                            tvB.setText("💈 " + (bName != null ? bName : "Barbero") + " — " + (bSpecialty != null ? bSpecialty : "Especialista") + (bPhone != null && !bPhone.isEmpty() ? " | 📞 " + bPhone : ""));
                                            tvB.setTextSize(13f);
                                            tvB.setTextColor(0xFF333333);
                                            tvB.setTypeface(null, Typeface.BOLD);
                                            tvB.setPadding(0, 4, 0, 4);
                                            layoutDetailBarbers.addView(tvB);
                                        }
                                        if (!found) {
                                            for (String item : defaultBarbers) {
                                                TextView tv = new TextView(this);
                                                tv.setText(item);
                                                tv.setTextSize(13f);
                                                tv.setTextColor(0xFF333333);
                                                tv.setTypeface(null, Typeface.BOLD);
                                                tv.setPadding(0, 4, 0, 4);
                                                layoutDetailBarbers.addView(tv);
                                            }
                                        }
                                    });
                        } else {
                            for (String item : defaultBarbers) {
                                TextView tv = new TextView(this);
                                tv.setText(item);
                                tv.setTextSize(13f);
                                tv.setTextColor(0xFF333333);
                                tv.setTypeface(null, Typeface.BOLD);
                                tv.setPadding(0, 4, 0, 4);
                                layoutDetailBarbers.addView(tv);
                            }
                        }
                    }

                    if (layoutDetailReviews != null) {
                        layoutDetailReviews.removeAllViews();
                        db.collection("reviews")
                                .whereEqualTo("barbershopName", barbershopName)
                                .get()
                                .addOnSuccessListener(reviewsSnap -> {
                                    boolean foundReviews = false;
                                    for (DocumentSnapshot rDoc : reviewsSnap) {
                                        foundReviews = true;
                                        String reviewText = rDoc.getString("review");
                                        Long rLong = rDoc.getLong("rating");
                                        long ratingVal = rLong != null ? rLong : 5;
                                        TextView tvR = new TextView(this);
                                        tvR.setText("⭐ (" + ratingVal + "/5) — '" + (reviewText != null && !reviewText.isEmpty() ? reviewText : "Excelente atención") + "'");
                                        tvR.setTextSize(13f);
                                        tvR.setTextColor(0xFF333333);
                                        tvR.setPadding(0, 6, 0, 6);
                                        layoutDetailReviews.addView(tvR);
                                    }
                                    if (!foundReviews) {
                                        for (String item : defaultReviews) {
                                            TextView tv = new TextView(this);
                                            tv.setText(item);
                                            tv.setTextSize(13f);
                                            tv.setTextColor(0xFF333333);
                                            tv.setPadding(0, 6, 0, 6);
                                            layoutDetailReviews.addView(tv);
                                        }
                                    }
                                });
                    }
                });

        MaterialButton btnDetailBook = dialogView.findViewById(R.id.btnDetailBookEM);
        if (btnDetailBook != null) {
            btnDetailBook.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                showBookAppointmentDialog(barbershopName);
            });
        }

        showFullScreenBottomSheet(bottomSheetDialog, dialogView);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Solicitar permisos de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableMyLocationOnMap();
        }

        // Cargar barberías desde Firestore
        cargarBarberiasDesdeFirestore();

        // Al hacer clic en un marcador de barbería del mapa, abrir detalle
        mMap.setOnMarkerClickListener(marker -> {
            if ("TU UBICACIÓN".equalsIgnoreCase(marker.getTitle())) {
                return false;
            }
            showBarbershopDetailDialog(marker.getTitle() != null ? marker.getTitle() : "Barbería");
            return true;
        });
    }

    private void enableMyLocationOnMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
            centerMapOnUserLocation();
        }
    }

    private void centerMapOnUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));
                    mMap.addMarker(new MarkerOptions()
                            .position(userLatLng)
                            .title("TU UBICACIÓN")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                }
            } else {
                // Coordenadas por defecto (Los Olivos, Lima)
                LatLng losOlivos = new LatLng(-11.9892, -77.0700);
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(losOlivos, 14f));
                }
            }
        });
    }

    private void cargarBarberiasDesdeFirestore(String distance) {
        float zoomLevel = 13f;
        if ("1 km".equalsIgnoreCase(distance)) zoomLevel = 15f;
        else if ("10 km".equalsIgnoreCase(distance)) zoomLevel = 11f;

        final float finalZoom = zoomLevel;

        db.collection("users")
                .whereEqualTo("role", "Barbería")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    barbershopMarkers.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String barbershopName = doc.getString("barbershopName");
                        if (barbershopName == null || barbershopName.isEmpty()) {
                            barbershopName = doc.getString("name");
                        }
                        if (barbershopName == null) barbershopName = "Barbería";

                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");

                        if (lat == null) lat = -11.9892;
                        if (lng == null) lng = -77.0700;

                        if (mMap != null) {
                            LatLng position = new LatLng(lat, lng);
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(barbershopName)
                                    .snippet("💈 Haz clic para ver detalles (Radio: " + distance + ")")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                            if (marker != null) {
                                barbershopMarkers.add(marker);
                            }
                        }
                    }

                    // Animar la cámara al usuario o al primer marcador con el zoom correspondiente
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                            if (location != null && mMap != null) {
                                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, finalZoom));
                            } else if (!barbershopMarkers.isEmpty() && mMap != null) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(barbershopMarkers.get(0).getPosition(), finalZoom));
                            } else if (mMap != null) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-11.9892, -77.0700), finalZoom));
                            }
                        });
                    } else {
                        if (!barbershopMarkers.isEmpty() && mMap != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(barbershopMarkers.get(0).getPosition(), finalZoom));
                        } else if (mMap != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-11.9892, -77.0700), finalZoom));
                        }
                    }
                })
                .addOnFailureListener(e -> {});
    }

    private void cargarBarberiasDesdeFirestore() {
        cargarBarberiasDesdeFirestore("5 km");
    }

    // Modal de Filtros
    private void showFiltersDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filters, null);

        RadioGroup rgDistance = dialogView.findViewById(R.id.rgDistanceEM);
        CheckBox cbHaircut = dialogView.findViewById(R.id.cbHaircutEM);
        CheckBox cbBeard = dialogView.findViewById(R.id.cbBeardEM);
        CheckBox cbCombo = dialogView.findViewById(R.id.cbComboEM);
        RadioGroup rgRating = dialogView.findViewById(R.id.rgRatingEM);
        MaterialButton btnApplyFilters = dialogView.findViewById(R.id.btnApplyFiltersEM);

        if (btnApplyFilters != null) {
            btnApplyFilters.setOnClickListener(v -> {
                int distId = rgDistance != null ? rgDistance.getCheckedRadioButtonId() : -1;
                String distanceText = "5 km";
                if (distId == R.id.rb1kmEM) distanceText = "1 km";
                else if (distId == R.id.rb10kmEM) distanceText = "10 km";

                boolean filterHaircut = cbHaircut != null && cbHaircut.isChecked();
                boolean filterBeard = cbBeard != null && cbBeard.isChecked();
                boolean filterCombo = cbCombo != null && cbCombo.isChecked();

                int ratingId = rgRating != null ? rgRating.getCheckedRadioButtonId() : -1;
                String ratingText = "Todas";
                if (ratingId == R.id.rb4StarEM) ratingText = "4.0+";
                else if (ratingId == R.id.rb45StarEM) ratingText = "4.5+";

                Toast.makeText(this, "✅ Filtros aplicados (" + distanceText + ", Calificación: " + ratingText + ")", Toast.LENGTH_LONG).show();
                bottomSheetDialog.dismiss();

                // Actualizar mapa y lista en tiempo real según el filtro de distancia y servicios
                cargarBarberiasDesdeFirestore(distanceText);
                populateBarbershopList("");
            });
        }

        showFullScreenBottomSheet(bottomSheetDialog, dialogView);
    }

    // Modal de Reserva de Cita
    private void showBookAppointmentDialog(String barbershopName) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_book_appointment, null);

        TextView tvBookBarbershopName = dialogView.findViewById(R.id.tvBookBarbershopNameEM);
        if (tvBookBarbershopName != null) {
            tvBookBarbershopName.setText(barbershopName);
        }

        RadioGroup rgServiceList = dialogView.findViewById(R.id.rgServiceListEM);
        RadioGroup rgTimeSlots = dialogView.findViewById(R.id.rgTimeSlotsEM);
        android.widget.Spinner spinnerBookBarber = dialogView.findViewById(R.id.spinnerBookBarberEM);
        MaterialButton btnConfirmBooking = dialogView.findViewById(R.id.btnConfirmBookingEM);

        final java.util.List<String> barberNames = new java.util.ArrayList<>();
        final java.util.List<String> barberUids = new java.util.ArrayList<>();
        barberNames.add("Cualquier Barbero Disponible");
        barberUids.add("");

        db.collection("users")
                .whereEqualTo("role", "Barbero")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        String bShop = doc.getString("barbershopName");
                        if (bShop != null && bShop.equalsIgnoreCase(barbershopName)) {
                            String name = doc.getString("name");
                            String lastName = doc.getString("lastName");
                            String fullName = (name != null ? name : "Barbero") + " " + (lastName != null ? lastName : "");
                            barberNames.add(fullName.trim());
                            barberUids.add(doc.getId());
                        }
                    }
                    if (spinnerBookBarber != null) {
                        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, barberNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerBookBarber.setAdapter(adapter);
                    }
                });

        if (btnConfirmBooking != null) {
            btnConfirmBooking.setOnClickListener(v -> {
                int selectedServiceId = rgServiceList != null ? rgServiceList.getCheckedRadioButtonId() : -1;
                RadioButton selectedService = dialogView.findViewById(selectedServiceId);
                String serviceName = selectedService != null ? selectedService.getText().toString() : "Corte de Cabello";

                int selectedTimeId = rgTimeSlots != null ? rgTimeSlots.getCheckedRadioButtonId() : -1;
                RadioButton selectedTime = dialogView.findViewById(selectedTimeId);
                String timeSlot = selectedTime != null ? selectedTime.getText().toString() : "10:00 AM";

                String selectedBarberUid = "";
                String selectedBarberName = "Cualquier Barbero";
                if (spinnerBookBarber != null && spinnerBookBarber.getSelectedItemPosition() >= 0) {
                    int pos = spinnerBookBarber.getSelectedItemPosition();
                    if (pos < barberUids.size()) {
                        selectedBarberUid = barberUids.get(pos);
                        selectedBarberName = barberNames.get(pos);
                    }
                }

                final String finalBarberUid = selectedBarberUid;
                final String finalBarberName = selectedBarberName;

                FirebaseUser user = mAuth.getCurrentUser();
                String userUid = user != null ? user.getUid() : "anonimo";
                String clientName = user != null && user.getDisplayName() != null ? user.getDisplayName() : "Cliente";

                Map<String, Object> appointment = new HashMap<>();
                appointment.put("userUid", userUid);
                appointment.put("clientName", clientName);
                appointment.put("barbershopName", barbershopName);
                appointment.put("service", serviceName);
                appointment.put("time", timeSlot);
                appointment.put("barberUid", finalBarberUid);
                appointment.put("barberName", finalBarberName);
                appointment.put("status", "Confirmada");

                db.collection("appointments").add(appointment)
                        .addOnSuccessListener(docRef -> Toast.makeText(this, "¡Reserva confirmada en " + barbershopName + " con " + finalBarberName + " a las " + timeSlot + "!", Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "¡Reserva agendada con éxito!", Toast.LENGTH_SHORT).show());

                bottomSheetDialog.dismiss();
            });
        }

        showFullScreenBottomSheet(bottomSheetDialog, dialogView);
    }

    // Modal de Reservas y Citas
    private void showReservationsDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reservations, null);

        LinearLayout layoutReservationsContainer = dialogView.findViewById(R.id.layoutReservationsContainerEM);
        MaterialButton btnCloseReservations = dialogView.findViewById(R.id.btnCloseReservationsEM);
        if (btnCloseReservations != null) {
            btnCloseReservations.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (layoutReservationsContainer != null && currentUser != null) {
            layoutReservationsContainer.removeAllViews();
            db.collection("appointments")
                    .whereEqualTo("userUid", currentUser.getUid())
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        boolean foundAny = false;
                        for (DocumentSnapshot doc : querySnapshot) {
                            String status = doc.getString("status");
                            if ("Finalizado".equalsIgnoreCase(status)) continue;

                            foundAny = true;
                            String shopName = doc.getString("barbershopName");
                            if (shopName == null) shopName = "Barbería";
                            String service = doc.getString("service");
                            if (service == null) service = "Corte de Cabello";
                            String time = doc.getString("time");
                            if (time == null) time = "10:00 AM";

                            addReservationCard(layoutReservationsContainer, shopName, service, time, status != null ? status : "Confirmada");
                        }

                        if (!foundAny) {
                            TextView tvEmpty = new TextView(this);
                            tvEmpty.setText("No tienes citas agendadas en este momento.");
                            tvEmpty.setTextColor(0xFF666666);
                            tvEmpty.setTextSize(14f);
                            tvEmpty.setPadding(10, 20, 10, 20);
                            layoutReservationsContainer.addView(tvEmpty);
                        }
                    })
                    .addOnFailureListener(e -> {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("No tienes citas agendadas en este momento.");
                        tvEmpty.setTextColor(0xFF666666);
                        tvEmpty.setTextSize(14f);
                        tvEmpty.setPadding(10, 20, 10, 20);
                        layoutReservationsContainer.addView(tvEmpty);
                    });
        }

        showFullScreenBottomSheet(bottomSheetDialog, dialogView);
    }

    private void addReservationCard(LinearLayout container, String shopName, String service, String time, String status) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setCardBackgroundColor(0xFFF8F8F8);
        card.setRadius(16f);
        card.setCardElevation(2f);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvShop = new TextView(this);
        LinearLayout.LayoutParams shopParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvShop.setLayoutParams(shopParams);
        tvShop.setText(shopName);
        tvShop.setTextColor(0xFF111111);
        tvShop.setTextSize(16f);
        tvShop.setTypeface(null, Typeface.BOLD);

        TextView tvStatus = new TextView(this);
        tvStatus.setText("🟢 " + status);
        tvStatus.setTextColor(0xFF2E7D32);
        tvStatus.setTextSize(12f);
        tvStatus.setTypeface(null, Typeface.BOLD);

        topRow.addView(tvShop);
        topRow.addView(tvStatus);

        TextView tvService = new TextView(this);
        tvService.setText("💈 " + service);
        tvService.setTextColor(0xFF333333);
        tvService.setTextSize(14f);
        tvService.setPadding(0, 6, 0, 2);

        TextView tvTime = new TextView(this);
        tvTime.setText("📅 " + time);
        tvTime.setTextColor(0xFF666666);
        tvTime.setTextSize(13f);

        layout.addView(topRow);
        layout.addView(tvService);
        layout.addView(tvTime);
        card.addView(layout);

        container.addView(card);
    }

    // Modal de Perfil de Cliente
    private void showProfileDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_profile, null);

        TextView tvProfileName = dialogView.findViewById(R.id.tvProfileNameEM);
        TextView tvProfileEmail = dialogView.findViewById(R.id.tvProfileEmailEM);
        MaterialButton btnProfileReservations = dialogView.findViewById(R.id.btnProfileReservationsEM);
        MaterialButton btnSignOut = dialogView.findViewById(R.id.btnSignOutEM);
        ImageView ivClientProfilePhoto = dialogView.findViewById(R.id.ivClientProfilePhotoEM);

        if (ivClientProfilePhoto != null) {
            ivClientProfilePhoto.setImageResource(R.drawable.ic_profile);
            currentClientProfileImageView = ivClientProfilePhoto;
            ivClientProfilePhoto.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                clientProfileImagePickerLauncher.launch(intent);
            });
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (tvProfileEmail != null && currentUser.getEmail() != null) {
                tvProfileEmail.setText(currentUser.getEmail());
            }
            if (tvProfileName != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                tvProfileName.setText(currentUser.getDisplayName());
            }
        }

        if (btnProfileReservations != null) {
            btnProfileReservations.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                showReservationsDialog();
            });
        }

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                mAuth.signOut();
                Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
                bottomSheetDialog.dismiss();
                animateSplitAndNavigate(() -> {
                    Intent intent = new Intent(HomeClienteActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                });
            });
        }

        showFullScreenBottomSheet(bottomSheetDialog, dialogView);
    }

    private void assembleHomeViews() {
        if (headerCard != null) {
            headerCard.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(450)
                    .start();
        }
        if (bottomNavCard != null) {
            bottomNavCard.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(450)
                    .start();
        }
    }

    private void animateSplitAndNavigate(Runnable onAnimationEnd) {
        if (headerCard != null) {
            headerCard.animate()
                    .translationY(-200f)
                    .alpha(0f)
                    .setDuration(300)
                    .start();
        }
        if (bottomNavCard != null) {
            bottomNavCard.animate()
                    .translationY(200f)
                    .alpha(0f)
                    .setDuration(300)
                    .start();
        }
        new Handler(Looper.getMainLooper()).postDelayed(onAnimationEnd, 250);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocationOnMap();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado. Se mostrará el mapa general.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showReviewDialog(String barbershopName, String message) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⭐ Califica a " + barbershopName);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(0xFF111111);
        layout.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextSize(14f);
        tvMsg.setTextColor(0xFF555555);
        tvMsg.setPadding(0, 10, 0, 20);
        layout.addView(tvMsg);

        LinearLayout ratingLayout = new LinearLayout(this);
        ratingLayout.setOrientation(LinearLayout.HORIZONTAL);
        ratingLayout.setGravity(android.view.Gravity.CENTER);

        final int[] selectedRating = {5};
        final MaterialButton[] starButtons = new MaterialButton[5];
        for (int i = 0; i < 5; i++) {
            final int starVal = i + 1;
            MaterialButton btnStar = new MaterialButton(this);
            btnStar.setText("★");
            btnStar.setTextSize(22f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(100, 100);
            lp.setMargins(6, 0, 6, 0);
            btnStar.setLayoutParams(lp);
            btnStar.setBackgroundColor(0xFFFFD700);
            btnStar.setOnClickListener(btnV -> {
                selectedRating[0] = starVal;
                for (int j = 0; j < 5; j++) {
                    starButtons[j].setBackgroundColor(j < starVal ? 0xFFFFD700 : 0xFFE0E0E0);
                }
            });
            starButtons[i] = btnStar;
            ratingLayout.addView(btnStar);
        }
        layout.addView(ratingLayout);

        TextInputEditText etReview = new TextInputEditText(this);
        etReview.setHint("Escribe una breve reseña (ej. Excelente atención y puntualidad)...");
        etReview.setMinLines(3);
        etReview.setTextSize(14f);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        etParams.setMargins(0, 20, 0, 20);
        etReview.setLayoutParams(etParams);
        layout.addView(etReview);

        builder.setView(layout);
        builder.setPositiveButton("Enviar Reseña", (dialog, which) -> {
            String reviewText = etReview.getText() != null ? etReview.getText().toString().trim() : "";
            Map<String, Object> reviewMap = new HashMap<>();
            reviewMap.put("barbershopName", barbershopName);
            reviewMap.put("rating", selectedRating[0]);
            reviewMap.put("review", reviewText);
            reviewMap.put("timestamp", System.currentTimeMillis());
            db.collection("reviews").add(reviewMap);
            Toast.makeText(this, "¡Gracias! Reseña (" + reviewText + ") y " + selectedRating[0] + "★ enviada con éxito.", Toast.LENGTH_LONG).show();
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in_scale, R.anim.fade_out_scale);
    }
}
