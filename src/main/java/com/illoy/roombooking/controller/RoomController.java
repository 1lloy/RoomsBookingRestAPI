package com.illoy.roombooking.controller;

import com.illoy.roombooking.dto.response.RoomAvailabilityResponse;
import com.illoy.roombooking.dto.response.RoomResponse;
import com.illoy.roombooking.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

    //получить все активные комнаты
    @GetMapping
    public ResponseEntity<List<RoomResponse>> findAllActiveList() {
        return ResponseEntity.ok(roomService.findAllActive());
    }

    //получить все активные комнаты (с пагинацией)
    @GetMapping("/page")
    public ResponseEntity<Page<RoomResponse>> findAllActivePage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<RoomResponse> activeRooms = roomService.findAllActive(pageable);

        return ResponseEntity.ok(activeRooms);
    }

    //получить все комнаты с минимальной вместимостью
    @GetMapping("/available")
    public ResponseEntity<List<RoomResponse>> findActiveByCapacity(@RequestParam(defaultValue = "2") int minCapacity) {
        return ResponseEntity.ok(roomService.findActiveByCapacity(minCapacity));
    }

    //получить все комнаты по подстроке названия
    @GetMapping("/search")
    public ResponseEntity<List<RoomResponse>> findActiveBySearchTerm(@RequestParam(defaultValue = "meeting") String searchTerm) {
        return ResponseEntity.ok(roomService.searchActiveByName(searchTerm));
    }

    //получить комнату по id
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> findActiveById(@PathVariable("roomId") Long id) {
        return ResponseEntity.ok(roomService.findActiveRoomById(id));
    }

    //проверить доступность бронирования комнаты в определенное время
    @GetMapping("/{roomId}/availability")
    public ResponseEntity<RoomAvailabilityResponse> checkAvailability(@PathVariable("roomId") Long id,
                                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        return ResponseEntity.ok(roomService.checkAvailability(id, startTime, endTime));
    }
}
