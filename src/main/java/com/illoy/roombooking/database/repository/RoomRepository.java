package com.illoy.roombooking.database.repository;

import com.illoy.roombooking.database.entity.Room;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByIsActiveTrue();

    Page<Room> findByIsActiveTrue(Pageable pageable);

    Optional<Room> findByIdAndIsActiveTrue(Long id);

    boolean existsByNameAndIsActiveTrue(String name);

    @Query("SELECT r FROM Room r WHERE r.isActive = true AND r.capacity >= :minCapacity")
    List<Room> findActiveRoomsByCapacity(@Param("minCapacity") Integer minCapacity);

    @Modifying
    @Query("UPDATE Room r SET r.isActive = :active WHERE r.id = :roomId")
    void updateRoomStatus(@Param("roomId") Long roomId, @Param("active") boolean active);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.isActive = true")
    long countActiveRooms();

    @Query("SELECT r FROM Room r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND r.isActive = true")
    List<Room> searchActiveRoomsByName(@Param("searchTerm") String searchTerm);
}
