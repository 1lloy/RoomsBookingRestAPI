package com.illoy.roombooking.controller.admin;

import com.illoy.roombooking.dto.response.RoomResponse;
import com.illoy.roombooking.dto.response.UserResponse;
import com.illoy.roombooking.service.BookingService;
import com.illoy.roombooking.service.RoomService;
import com.illoy.roombooking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AnalyticsController {

    private final UserService userService;
    private final RoomService roomService;
    private final BookingService bookingService;

    //число активных пользователей
    @GetMapping("/active-users-count")
    public ResponseEntity<Map<String, Long>> findActiveUsersCount() {
        long activeUsersCount = userService.countActiveUsers();

        Map<String, Long> response = new HashMap<>();
        response.put("activeUsersCount", activeUsersCount);

        return ResponseEntity.ok(response);
    }

    //число активных комнат
    @GetMapping("/active-rooms-count")
    public ResponseEntity<Map<String, Long>> countActiveRooms(){
        long activeRoomsCount = roomService.countActiveRooms();
        Map<String, Long> response = new HashMap<>();
        response.put("activeRoomsCount", activeRoomsCount);

        return ResponseEntity.ok(response);
    }

    //число бронирований за период
    @GetMapping("/bookings-count")
    public ResponseEntity<Map<String, Long>> findBookingsCountByPeriod(@RequestParam LocalDateTime start,
                                                                       @RequestParam LocalDateTime end){
        Map<String, Long> response = new HashMap<>();
        response.put("bookingsCount", bookingService.countByStartTimeBetween(start, end));

        return ResponseEntity.ok(response);
    }

    //число бронирований каждого статуса за период
    @GetMapping("/bookings-count-by-status")
    public ResponseEntity<Map<String, Long>> findBookingsCountByStatusAndPeriod(@RequestParam LocalDateTime start,
                                                                                @RequestParam LocalDateTime end){
        return ResponseEntity.ok(bookingService.findCountByPeriodGroupByStatus(start, end));
    }

    //число бронирований для каждой комнаты за период
    @GetMapping("/popular-rooms")
    public ResponseEntity<Map<RoomResponse, Long>> findPopularRooms(@RequestParam LocalDateTime start,
                                                                    @RequestParam LocalDateTime end,
                                                                    @RequestParam int limit){

        return ResponseEntity.ok(bookingService.findPopularRooms(start, end, limit));
    }

    //число бронирований для каждого пользователя за период
    @GetMapping("/users-bookings-count")
    public ResponseEntity<Map<UserResponse, Long>> findUsersBookingsCount(@RequestParam LocalDateTime start,
                                                                          @RequestParam LocalDateTime end){

        return ResponseEntity.ok(bookingService.findUsersBookingsCount(start, end));
    }

    //число бронирований по дням недели за период
    @GetMapping("/bookings-count-by-dow")
    public ResponseEntity<Map<String, Long>> findBookingsCountGroupByDow(@RequestParam LocalDateTime start,
                                                                         @RequestParam LocalDateTime end){

        return ResponseEntity.ok(bookingService.findBookingsCountByDow(start, end));
    }
}
