package com.illoy.roombooking.controller.admin;

import com.illoy.roombooking.dto.request.RoomCreateEditRequest;
import com.illoy.roombooking.dto.response.RoomResponse;
import com.illoy.roombooking.group.CreateAction;
import com.illoy.roombooking.group.UpdateAction;
import com.illoy.roombooking.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminRoomController {

    private final RoomService roomService;

    //получить все комнаты
    @GetMapping
    public ResponseEntity<List<RoomResponse>> findAll(){
        return ResponseEntity.ok(roomService.findAll());
    }

    //получить отдельную любую комнату по id
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> findById(@PathVariable("roomId") Long id){
        return ResponseEntity.ok(roomService.findById(id));
    }

    //создание комнаты
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@RequestBody @Validated(CreateAction.class) RoomCreateEditRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(request));
    }

    //обновление комнаты
    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable("roomId") Long id,
                                                   @RequestBody @Validated(UpdateAction.class) RoomCreateEditRequest request) {

        return ResponseEntity.ok(roomService.update(id, request));
    }

    //обновление статуса комнаты
    @PatchMapping("/{roomId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable("roomId") Long id,
                                             @RequestParam boolean newStatus){

        roomService.updateRoomStatus(id, newStatus);
        return ResponseEntity.noContent().build();
    }
}
