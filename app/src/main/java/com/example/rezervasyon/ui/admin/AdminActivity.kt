package com.example.rezervasyon.ui.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rezervasyon.R
import com.example.rezervasyon.data.local.database.AppDatabase
import com.example.rezervasyon.data.local.entities.Trip
import com.example.rezervasyon.data.local.entities.TripType
import com.example.rezervasyon.databinding.ActivityAdminBinding
import com.example.rezervasyon.ui.trips.TripsAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Admin panel for managing trips
 * Allows adding and deleting trips
 */
class AdminActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAdminBinding
    private lateinit var database: AppDatabase
    private lateinit var tripsAdapter: TripsAdapter
    private var allTrips: List<Trip> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getDatabase(this)
        
        setupTripTypeSpinner()
        setupRecyclerView()
        setupListeners()
        setupSearchListener()
        loadTrips()
    }
    
    private fun setupTripTypeSpinner() {
        val tripTypes = arrayOf("OTOBÜS", "UÇAK")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tripTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTripType.adapter = adapter
    }
    
    private fun setupRecyclerView() {
        tripsAdapter = TripsAdapter(database, this) { trip ->
            showDeleteConfirmation(trip)
        }
        
        binding.recyclerViewTrips.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = tripsAdapter
        }
    }
    
    private fun setupListeners() {
        // Date picker
        binding.etDate.setOnClickListener {
            showDatePicker()
        }
        
        // Departure time picker
        binding.etTime.setOnClickListener {
            showTimePicker(true)
        }
        
        // Arrival time picker
        binding.etArrivalTime.setOnClickListener {
            showTimePicker(false)
        }
        
        binding.btnAddTrip.setOnClickListener {
            addTrip()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun showDatePicker() {
        val calendar = java.util.Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                binding.etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun showTimePicker(isDeparture: Boolean) {
        val calendar = java.util.Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                val timeString = String.format("%02d:%02d", hour, minute)
                if (isDeparture) {
                    binding.etTime.setText(timeString)
                } else {
                    binding.etArrivalTime.setText(timeString)
                }
            },
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            true
        ).show()
    }
    
    private fun loadTrips() {
        lifecycleScope.launch {
            database.tripDao().getAllTrips().collectLatest { trips ->
                allTrips = trips
                filterTrips(binding.etSearch.text.toString())
            }
        }
    }
    
    private fun filterTrips(query: String) {
        val filtered = if (query.isBlank()) {
            allTrips
        } else {
            allTrips.filter {
                it.companyName.contains(query, ignoreCase = true) ||
                it.departure.contains(query, ignoreCase = true) ||
                it.destination.contains(query, ignoreCase = true)
            }
        }
        tripsAdapter.submitList(filtered)
    }
    
    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterTrips(s.toString())
            }
        })
    }
    
    private fun addTrip() {
        binding.apply {
            val company = etCompanyName.text.toString().trim()
            val departure = etDeparture.text.toString().trim()
            val destination = binding.etDestination.text.toString()
            val date = binding.etDate.text.toString()
            val time = binding.etTime.text.toString()
            val arrivalTime = binding.etArrivalTime.text.toString()
            val priceText = binding.etPrice.text.toString()
            val seatsText = binding.etTotalSeats.text.toString()
            
            // Validate inputs
            if (company.isEmpty() || departure.isEmpty() || destination.isEmpty() ||
                date.isEmpty() || time.isEmpty() || arrivalTime.isEmpty() || priceText.isEmpty() || seatsText.isEmpty()) {
                Toast.makeText(this@AdminActivity, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
                return
            }
            
            val tripType = if (spinnerTripType.selectedItemPosition == 0) TripType.BUS else TripType.FLIGHT
            val seatCount = seatsText.toIntOrNull() ?: 0
            
            // Validate seat count based on trip type
            val isValidSeatCount = when (tripType) {
                TripType.BUS -> seatCount in 20..50
                TripType.FLIGHT -> seatCount in 100..200
            }
            
            if (!isValidSeatCount) {
                val range = if (tripType == TripType.BUS) "20-50" else "100-200"
                Toast.makeText(this@AdminActivity, "$tripType için koltuk sayısı $range arasında olmalı", Toast.LENGTH_LONG).show()
                return
            }
            
            // Validate price based on trip type
            val price = priceText.toDoubleOrNull() ?: 0.0
            val maxPrice = if (tripType == TripType.BUS) 5000.0 else 10000.0
            if (price <= 0 || price > maxPrice) {
                val typeStr = if (tripType == TripType.BUS) "Otobüs" else "Uçak"
                Toast.makeText(this@AdminActivity, "$typeStr için fiyat 0-$maxPrice TL arasında olmalı", Toast.LENGTH_LONG).show()
                return
            }
            
            val trip = Trip(
                type = tripType,
                companyName = company,
                departure = departure,
                destination = destination,
                date = date,
                time = time,
                arrivalTime = arrivalTime,
                price = priceText.toDoubleOrNull() ?: 0.0,
                totalSeats = seatCount
            )
            
            lifecycleScope.launch {
                database.tripDao().insert(trip)
                Toast.makeText(this@AdminActivity, R.string.trip_added, Toast.LENGTH_SHORT).show()
                clearForm()
            }
        }
    }
    
    private fun clearForm() {
        binding.apply {
            etCompanyName.text?.clear()
            etDeparture.text?.clear()
            etDestination.text?.clear()
            etDate.text?.clear()
            etTime.text?.clear()
            etArrivalTime.text?.clear()
            etPrice.text?.clear()
            etTotalSeats.text?.clear()
        }
    }
    
    private fun showDeleteConfirmation(trip: Trip) {
        AlertDialog.Builder(this)
            .setTitle("Sefer Sil")
            .setMessage("${trip.departure} → ${trip.destination} seferini silmek istediğinizden emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                deleteTrip(trip)
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    private fun deleteTrip(trip: Trip) {
        lifecycleScope.launch {
            database.tripDao().delete(trip)
            Toast.makeText(this@AdminActivity, R.string.trip_deleted, Toast.LENGTH_SHORT).show()
        }
    }
}
