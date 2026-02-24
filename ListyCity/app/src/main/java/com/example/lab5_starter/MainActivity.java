package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements
        CityDialogFragment.CityDialogListener,
        CityArrayAdapter.OnCityDeleteClickListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private CityArrayAdapter cityArrayAdapter;

    private FirebaseFirestore database;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList, this);
        cityListView.setAdapter(cityArrayAdapter);

        setupFirestore();

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
        });

    }

    private void setupFirestore() {
        database = FirebaseFirestore.getInstance();
        citiesRef = database.collection("cities");
        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", "Snapshot listener failed", error);
                return;
            }

            cityArrayList.clear();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    String name = doc.getString("name");
                    String province = doc.getString("province");
                    if (name != null && province != null) {
                        cityArrayList.add(new City(name, province));
                    }
                }
            }
            cityArrayAdapter.notifyDataSetChanged();
        });
    }

    private Map<String, Object> cityToMap(City city) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", city.getName());
        data.put("province", city.getProvince());
        return data;
    }

    @Override
    public void updateCity(City city, String title, String year) {
        if (city == null) {
            return;
        }

        String updatedName = title.trim();
        String updatedProvince = year.trim();
        if (updatedName.isEmpty() || updatedProvince.isEmpty()) {
            Toast.makeText(this, "City and province cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        City updatedCity = new City(updatedName, updatedProvince);
        String oldName = city.getName();

        if (Objects.equals(oldName, updatedName)) {
            citiesRef.document(updatedName)
                    .set(cityToMap(updatedCity))
                    .addOnFailureListener(e ->
                            Log.e("Firestore", "Failed to update city", e));
            return;
        }

        DocumentReference oldDoc = citiesRef.document(oldName);
        DocumentReference newDoc = citiesRef.document(updatedName);
        WriteBatch batch = database.batch();
        batch.delete(oldDoc);
        batch.set(newDoc, cityToMap(updatedCity));
        batch.commit().addOnFailureListener(e ->
                Log.e("Firestore", "Failed to rename city", e));
    }

    @Override
    public void addCity(City city) {
        String name = city.getName().trim();
        String province = city.getProvince().trim();
        if (name.isEmpty() || province.isEmpty()) {
            Toast.makeText(this, "City and province cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        City cleanedCity = new City(name, province);
        citiesRef.document(cleanedCity.getName())
                .set(cityToMap(cleanedCity))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Failed to add city", e));
    }

    @Override
    public void onDeleteCity(City city) {
        if (city == null || city.getName() == null) {
            return;
        }

        citiesRef.document(city.getName())
                .delete()
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Failed to delete city", e));
    }
}
