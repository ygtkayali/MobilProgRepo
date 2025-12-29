package com.example.rezervasyon.utils

import com.example.rezervasyon.data.local.entities.Trip
import com.example.rezervasyon.data.local.entities.TripType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for FilterHelper utility class
 */
class FilterHelperTest {

    private val sampleTrips = listOf(
        Trip(
            id = 1,
            type = TripType.BUS,
            companyName = "Metro Turizm",
            departure = "İstanbul",
            destination = "Ankara",
            date = "2025-01-15",
            time = "10:00",
            arrivalTime = "16:00",
            price = 500.0,
            totalSeats = 40
        ),
        Trip(
            id = 2,
            type = TripType.BUS,
            companyName = "Kamil Koç",
            departure = "Ankara",
            destination = "İzmir",
            date = "2025-01-16",
            time = "12:00",
            arrivalTime = "20:00",
            price = 600.0,
            totalSeats = 40
        ),
        Trip(
            id = 3,
            type = TripType.FLIGHT,
            companyName = "THY",
            departure = "İstanbul",
            destination = "İzmir",
            date = "2025-01-15",
            time = "14:00",
            arrivalTime = "15:00",
            price = 1500.0,
            totalSeats = 180
        ),
        Trip(
            id = 4,
            type = TripType.BUS,
            companyName = "Metro Turizm",
            departure = "İstanbul",
            destination = "Antalya",
            date = "2025-01-17",
            time = "22:00",
            arrivalTime = "08:00",
            price = 800.0,
            totalSeats = 40
        )
    )

    // ========== getUniqueCompanies Tests ==========

    @Test
    fun getUniqueCompanies_returnsSortedUniqueCompanies() {
        val companies = FilterHelper.getUniqueCompanies(sampleTrips)
        
        assertEquals(3, companies.size)
        assertEquals(listOf("Kamil Koç", "Metro Turizm", "THY"), companies)
    }

    @Test
    fun getUniqueCompanies_removeDuplicates() {
        val companies = FilterHelper.getUniqueCompanies(sampleTrips)
        
        // Metro Turizm appears twice in sampleTrips but should only appear once
        assertEquals(1, companies.count { it == "Metro Turizm" })
    }

    @Test
    fun getUniqueCompanies_emptyList_returnsEmptyList() {
        val companies = FilterHelper.getUniqueCompanies(emptyList())
        
        assertTrue(companies.isEmpty())
    }

    // ========== getUniqueDepartures Tests ==========

    @Test
    fun getUniqueDepartures_returnsSortedUniqueDepartures() {
        val departures = FilterHelper.getUniqueDepartures(sampleTrips)
        
        assertEquals(2, departures.size)
        assertEquals(listOf("Ankara", "İstanbul"), departures)
    }

    @Test
    fun getUniqueDepartures_emptyList_returnsEmptyList() {
        val departures = FilterHelper.getUniqueDepartures(emptyList())
        
        assertTrue(departures.isEmpty())
    }

    // ========== getUniqueDestinations Tests ==========

    @Test
    fun getUniqueDestinations_returnsSortedUniqueDestinations() {
        val destinations = FilterHelper.getUniqueDestinations(sampleTrips)
        
        assertEquals(3, destinations.size)
        assertEquals(listOf("Ankara", "Antalya", "İzmir"), destinations)
    }

    @Test
    fun getUniqueDestinations_emptyList_returnsEmptyList() {
        val destinations = FilterHelper.getUniqueDestinations(emptyList())
        
        assertTrue(destinations.isEmpty())
    }

    // ========== getUniqueDates Tests ==========

    @Test
    fun getUniqueDates_returnsSortedUniqueDates() {
        val dates = FilterHelper.getUniqueDates(sampleTrips)
        
        assertEquals(3, dates.size)
        assertEquals(listOf("2025-01-15", "2025-01-16", "2025-01-17"), dates)
    }

    @Test
    fun getUniqueDates_removeDuplicates() {
        val dates = FilterHelper.getUniqueDates(sampleTrips)
        
        // 2025-01-15 appears twice in sampleTrips but should only appear once
        assertEquals(1, dates.count { it == "2025-01-15" })
    }

    @Test
    fun getUniqueDates_emptyList_returnsEmptyList() {
        val dates = FilterHelper.getUniqueDates(emptyList())
        
        assertTrue(dates.isEmpty())
    }

    // ========== getPriceRange Tests ==========

    @Test
    fun getPriceRange_returnsMinAndMaxPrices() {
        val priceRange = FilterHelper.getPriceRange(sampleTrips)
        
        assertEquals(500.0, priceRange.first, 0.001)
        assertEquals(1500.0, priceRange.second, 0.001)
    }

    @Test
    fun getPriceRange_emptyList_returnsDefaultRange() {
        val priceRange = FilterHelper.getPriceRange(emptyList())
        
        assertEquals(0.0, priceRange.first, 0.001)
        assertEquals(1000.0, priceRange.second, 0.001)
    }

    @Test
    fun getPriceRange_singleTrip_returnsSamePriceForMinAndMax() {
        val singleTrip = listOf(
            Trip(
                id = 1,
                type = TripType.BUS,
                companyName = "Test",
                departure = "A",
                destination = "B",
                date = "2025-01-01",
                time = "10:00",
                arrivalTime = "12:00",
                price = 750.0,
                totalSeats = 40
            )
        )
        
        val priceRange = FilterHelper.getPriceRange(singleTrip)
        
        assertEquals(750.0, priceRange.first, 0.001)
        assertEquals(750.0, priceRange.second, 0.001)
    }
}
