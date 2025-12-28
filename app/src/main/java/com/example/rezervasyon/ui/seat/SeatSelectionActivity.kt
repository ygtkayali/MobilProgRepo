package com.example.rezervasyon.ui.seat

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.rezervasyon.R
import com.example.rezervasyon.data.local.database.AppDatabase
import com.example.rezervasyon.data.local.entities.Reservation
import com.example.rezervasyon.data.local.entities.ReservationStatus
import com.example.rezervasyon.data.local.entities.Trip
import com.example.rezervasyon.data.local.entities.TripType
import com.example.rezervasyon.databinding.ActivitySeatSelectionBinding
import com.example.rezervasyon.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * Seat selection activity with interactive seat map
 * Users can select multiple seats and confirm reservation
 */
class SeatSelectionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySeatSelectionBinding
    private lateinit var database: AppDatabase
    private lateinit var sessionManager: SessionManager
    
    private var trip: Trip? = null
    private val selectedSeats = mutableSetOf<Int>()
    private val reservedSeats = mutableSetOf<Int>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeatSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)
        
        val tripId = intent.getLongExtra("TRIP_ID", -1)
        if (tripId == -1L) {
            finish()
            return
        }
        
        loadTripAndSeats(tripId)
        setupListeners()
    }
    
    private fun loadTripAndSeats(tripId: Long) {
        lifecycleScope.launch {
            trip = database.tripDao().getTripById(tripId)
            trip?.let { currentTrip ->
                displayTripInfo(currentTrip)
                
                // Load reserved seats
                val reservations = database.reservationDao().getReservationsByTrip(tripId)
                reservedSeats.clear()
                reservations.forEach { reservation ->
                    reservation.seatNumbers.split(",").forEach { seat ->
                        reservedSeats.add(seat.toIntOrNull() ?: 0)
                    }
                }
                
                setupSeatGrid(currentTrip)
            }
        }
    }
    
    private fun displayTripInfo(trip: Trip) {
        binding.apply {
            tvTripInfo.text = "${trip.departure} → ${trip.destination}"
            tvCompanyName.text = trip.companyName
            
            // Calculate duration
            val duration = calculateDuration(trip.time, trip.arrivalTime)
            tvDateTime.text = "${trip.date}  ${trip.time} → ${trip.arrivalTime} ($duration)"
            
            tvPricePerSeat.text = "${trip.price} TL / koltuk"
        }
    }
    
    private fun calculateDuration(startTime: String, endTime: String): String {
        try {
            val (startHour, startMin) = startTime.split(":").map { it.toInt() }
            val (endHour, endMin) = endTime.split(":").map { it.toInt() }
            
            var totalMinutes = (endHour * 60 + endMin) - (startHour * 60 + startMin)
            
            // Handle overnight trips
            if (totalMinutes < 0) {
                totalMinutes += 24 * 60
            }
            
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            
            return "${hours}s ${minutes}d"
        } catch (e: Exception) {
            return "-"
        }
    }
    
    private fun setupSeatGrid(trip: Trip) {
        // Dynamic column count based on trip type
        val cols = if (trip.type == TripType.BUS) 4 else 6
        val rows = (trip.totalSeats + cols - 1) / cols // Ceiling division
        
        binding.gridSeats.apply {
            columnCount = cols
            rowCount = rows
            removeAllViews()
        }
        
        for (seatNum in 1..trip.totalSeats) {
            val seatView = createSeatView(seatNum)
            binding.gridSeats.addView(seatView)
        }
    }
    
    private fun createSeatView(seatNumber: Int): TextView {
        val size = resources.getDimensionPixelSize(R.dimen.seat_size)
        val margin = resources.getDimensionPixelSize(R.dimen.seat_margin)
        
        return TextView(this).apply {
            text = seatNumber.toString()
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(Color.WHITE)
            
            layoutParams = GridLayout.LayoutParams().apply {
                width = size
                height = size
                setMargins(margin, margin, margin, margin)
            }
            
            // Set initial state
            updateSeatAppearance(this, seatNumber)
            
            // Click listener
            setOnClickListener {
                if (!reservedSeats.contains(seatNumber)) {
                    toggleSeat(seatNumber)
                    updateSeatAppearance(this, seatNumber)
                    updateTotalPrice()
                }
            }
        }
    }
    
    private fun updateSeatAppearance(view: TextView, seatNumber: Int) {
        val background = when {
            reservedSeats.contains(seatNumber) -> R.drawable.seat_reserved
            selectedSeats.contains(seatNumber) -> R.drawable.seat_selected
            else -> R.drawable.seat_available
        }
        view.background = ContextCompat.getDrawable(this, background)
    }
    
    private fun toggleSeat(seatNumber: Int) {
        if (selectedSeats.contains(seatNumber)) {
            selectedSeats.remove(seatNumber)
        } else {
            selectedSeats.add(seatNumber)
        }
    }
    
    private fun updateTotalPrice() {
        val total = selectedSeats.size * (trip?.price ?: 0.0)
        binding.tvTotalPrice.text = "Toplam: $total TL"
        binding.tvSelectedSeats.text = "Seçili Koltuklar: ${selectedSeats.sorted().joinToString(", ")}"
    }
    
    private fun setupListeners() {
        binding.btnConfirm.setOnClickListener {
            confirmReservation()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun confirmReservation() {
        if (selectedSeats.isEmpty()) {
            Toast.makeText(this, R.string.select_at_least_one_seat, Toast.LENGTH_SHORT).show()
            return
        }
        
        val trip = trip ?: return
        val userId = sessionManager.getUserId()
        
        lifecycleScope.launch {
            val reservation = Reservation(
                userId = userId,
                tripId = trip.id,
                seatNumbers = selectedSeats.sorted().joinToString(","),
                totalPrice = selectedSeats.size * trip.price,
                status = ReservationStatus.ACTIVE
            )
            
            database.reservationDao().insert(reservation)
            
            Toast.makeText(
                this@SeatSelectionActivity,
                getString(R.string.reservation_success),
                Toast.LENGTH_SHORT
            ).show()
            
            finish()
        }
    }
}
