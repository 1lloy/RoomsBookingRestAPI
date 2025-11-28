package com.illoy.roombooking.integration.repository;

import com.illoy.roombooking.database.entity.Room;
import com.illoy.roombooking.database.repository.RoomRepository;
import com.illoy.roombooking.integration.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class RoomRepositoryTest extends IntegrationTestBase {

    private final RoomRepository roomRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFindOnlyActiveRooms(){
        // given: создаём несколько комнат
        Room activeRoom1 = Room.builder().name("Conference A").isActive(true).build();
        Room activeRoom2 = Room.builder().name("Conference B").isActive(true).build();
        Room inactiveRoom = Room.builder().name("Maintenance Room").isActive(false).build();

        roomRepository.saveAll(List.of(activeRoom1, activeRoom2, inactiveRoom));

        // when
        List<Room> activeRooms = roomRepository.findByIsActiveTrue();

        // then
        assertThat(activeRooms)
                .hasSize(2)
                .extracting(Room::getName)
                .containsExactlyInAnyOrder("Conference A", "Conference B");

        // дополнительная проверка: нет неактивных комнат
        assertThat(activeRooms).noneMatch(room -> !room.isActive());
    }

    @Test
    void shouldFindActiveRoomById() {
        // given
        Room activeRoom = Room.builder().name("Conference A").isActive(true).build();

        activeRoom = roomRepository.save(activeRoom);

        // when
        Optional<Room> foundRoom = roomRepository.findByIdAndIsActiveTrue(activeRoom.getId());

        // then
        assertThat(foundRoom)
                .isPresent()
                .get()
                .satisfies(room -> {
                    assertThat(room.getName()).isEqualTo("Conference A");
                    assertThat(room.isActive()).isTrue();
                });
    }

    @Test
    void existsByNameAndActiveTrue(){
        Room activeRoom = Room.builder().name("Conference A").isActive(true).build();

        roomRepository.save(activeRoom);

        boolean result = roomRepository.existsByNameAndIsActiveTrue("Conference A");
        assertThat(result).isTrue();

        boolean resultUsername = roomRepository.existsByNameAndIsActiveTrue("randomRoomNameThatDontExists");
        assertThat(resultUsername).isFalse();
    }

    @Test
    void shouldFindActiveRoomsByCapacity() {
        // given
        Room room1 = Room.builder().name("Small Room").capacity(5).isActive(true).build();
        Room room2 = Room.builder().name("Medium Room").capacity(10).isActive(true).build();
        Room room3 = Room.builder().name("Large Room").capacity(20).isActive(true).build();
        Room inactiveRoom = Room.builder().name("Inactive Room").capacity(15).isActive(false).build();

        roomRepository.saveAll(List.of(room1, room2, room3, inactiveRoom));

        // when
        List<Room> result = roomRepository.findActiveRoomsByCapacity(10);

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(Room::getName)
                .containsExactlyInAnyOrder("Medium Room", "Large Room");

        // дополнительная проверка: все комнаты активные
        assertThat(result).allMatch(Room::isActive);
    }

    @Test
    void shouldUpdateRoomStatus() {
        // given: создаём активную комнату
        Room room = Room.builder().name("Conference Room").capacity(10).isActive(true).build();
        room = roomRepository.save(room);

        // when: деактивируем комнату
        roomRepository.updateRoomStatus(room.getId(), false);

        entityManager.refresh(room);

        // then
        assertThat(room.isActive()).isFalse();
    }

    @Test
    void shouldCountOnlyActiveRooms() {
        // given: создаём комнаты
        Room active1 = Room.builder().name("Room A").isActive(true).capacity(5).build();
        Room active2 = Room.builder().name("Room B").isActive(true).capacity(10).build();
        Room inactive = Room.builder().name("Room C").isActive(false).capacity(8).build();

        roomRepository.saveAll(List.of(active1, active2, inactive));

        // when
        long count = roomRepository.countActiveRooms();

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldFindActiveRoomsByNameIgnoringCase() {
        // given
        Room room1 = Room.builder().name("Conference A").isActive(true).capacity(10).build();
        Room room2 = Room.builder().name("Meeting Room B").isActive(true).capacity(5).build();
        Room room3 = Room.builder().name("Inactive Room").isActive(false).capacity(8).build();

        roomRepository.saveAll(List.of(room1, room2, room3));

        // when
        List<Room> result = roomRepository.searchActiveRoomsByName("conference");

        // then
        assertThat(result)
                .hasSize(1)
                .extracting(Room::getName)
                .containsExactly("Conference A");

        // проверка, что все комнаты активные
        assertThat(result).allMatch(Room::isActive);
    }
}
