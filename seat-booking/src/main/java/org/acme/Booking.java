package org.acme;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection="booking", database = "cinema")
public class Booking extends PanacheMongoEntityBase {
    
    @BsonId
    public String id;

    public String name;
    public Long seat;

    public boolean persist(Long seatId) {
        if (findSeatById(seatId).isPresent()) {
            return false;
        } else {
            this.seat = seatId;
            this.persist();
            return true;
        }
    }

    public static Optional<Seat> findSeatById(Long id) {
        return Booking.find("seat", id).singleResultOptional()
                    .map(b -> (Booking) b)
                    .map(b -> new Seat(b.seat));
    }

    public static List<Seat> findAllSeats() {
        try (Stream<Booking> bookings = Booking.streamAll()) {
            return bookings.map(b -> new Seat(b.seat))
                                .collect(Collectors.toList());
        }
    }

}
