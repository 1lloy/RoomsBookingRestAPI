-- liquibase formatted sql

-- changeset romanh:1
create table if not exists users(
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    email VARCHAR(64) NOT NULL UNIQUE,
    role VARCHAR(32) NOT NULL DEFAULT 'ROLE_USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- changeset romanh:2
create table if not exists rooms(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(256),
    capacity INTEGER NOT NULL CHECK (capacity > 0 AND capacity <= 100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- changeset romanh:3
create table if not exists bookings(
    id BIGSERIAL PRIMARY KEY,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bookings_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_booking_time CHECK (end_time > start_time)
);

-- changeset romanh:4
CREATE INDEX idx_bookings_room_id ON bookings(room_id);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_start_time ON bookings(start_time);
CREATE INDEX idx_bookings_end_time ON bookings(end_time);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_rooms_active ON rooms(is_active) WHERE is_active = true;

-- changeset romanh:5
CREATE UNIQUE INDEX idx_bookings_room_time_unique
    ON bookings(room_id, start_time, end_time)
    WHERE status IN ('CONFIRMED', 'PENDING');

-- changeset romanh:6
INSERT INTO rooms (id, name, description, capacity) VALUES
    (1,'Conference Room A', 'Large conference room with projector', 20),
    (2,'Meeting Room B', 'Medium meeting room with whiteboard', 10),
    (3,'Small Room C', 'Small room for 1-2 people', 2),
    (4,'Training Room D', 'Room for training sessions', 15);

INSERT INTO users (id, username, email, password, role) VALUES
    (1,'admin', 'admin@company.com', '{noop}123', 'ROLE_ADMIN'),
    (2,'john_doe', 'john@company.com', '{noop}345', 'ROLE_USER'),
    (3,'jane_smith', 'jane@company.com', '{noop}678', 'ROLE_USER');

-- changeset romanh:7
INSERT INTO bookings (room_id, user_id, start_time, end_time, status) VALUES
-- Текущие и будущие бронирования
(1, 2, '2024-01-20 09:00:00', '2024-01-20 10:30:00', 'CONFIRMED'),  -- john_doe в Conference Room A
(2, 3, '2024-01-20 10:00:00', '2024-01-20 11:00:00', 'CONFIRMED'),  -- jane_smith в Meeting Room B
(1, 3, '2024-01-20 11:00:00', '2024-01-20 12:30:00', 'CONFIRMED'),  -- jane_smith в Conference Room A
(3, 2, '2024-01-20 14:00:00', '2024-01-20 15:00:00', 'CONFIRMED'),  -- john_doe в Small Room C
(4, 2, '2024-01-20 15:30:00', '2024-01-20 17:00:00', 'CONFIRMED'),  -- john_doe в Training Room D

-- Бронирования на разные дни
(1, 3, '2024-01-21 08:30:00', '2024-01-21 10:00:00', 'CONFIRMED'),
(2, 2, '2024-01-21 10:30:00', '2024-01-21 12:00:00', 'CONFIRMED'),
(3, 3, '2024-01-21 13:00:00', '2024-01-21 14:00:00', 'CONFIRMED'),

-- Бронирования на следующую неделю
(1, 2, '2024-01-22 09:00:00', '2024-01-22 11:00:00', 'CONFIRMED'),
(4, 3, '2024-01-22 11:30:00', '2024-01-22 13:30:00', 'CONFIRMED'),
(2, 2, '2024-01-23 14:00:00', '2024-01-23 16:00:00', 'CONFIRMED'),

-- Прошедшие бронирования (для истории и аналитики)
(1, 2, '2024-01-15 09:00:00', '2024-01-15 10:00:00', 'COMPLETED'),
(2, 3, '2024-01-15 10:30:00', '2024-01-15 11:30:00', 'COMPLETED'),
(3, 2, '2024-01-16 14:00:00', '2024-01-16 15:00:00', 'COMPLETED'),
(4, 3, '2024-01-17 08:00:00', '2024-01-17 10:00:00', 'COMPLETED'),

-- Отмененные бронирования
(1, 2, '2024-01-18 11:00:00', '2024-01-18 12:00:00', 'CANCELLED'),
(2, 3, '2024-01-19 15:00:00', '2024-01-19 16:00:00', 'CANCELLED'),

-- Бронирования с пересекающимся временем (для демонстрации работы UNIQUE индекса)
-- Эти должны работать, потому что в разных комнатах
(1, 2, '2024-01-24 09:00:00', '2024-01-24 11:00:00', 'CONFIRMED'),
(2, 3, '2024-01-24 09:00:00', '2024-01-24 11:00:00', 'CONFIRMED'),

-- Бронирования для демонстрации пагинации и фильтрации
(1, 2, '2024-01-25 08:00:00', '2024-01-25 09:00:00', 'CONFIRMED'),
(1, 3, '2024-01-25 10:00:00', '2024-01-25 12:00:00', 'CONFIRMED'),
(2, 2, '2024-01-25 13:00:00', '2024-01-25 14:00:00', 'CONFIRMED'),
(3, 3, '2024-01-25 15:00:00', '2024-01-25 16:00:00', 'CONFIRMED'),
(4, 2, '2024-01-26 09:00:00', '2024-01-26 11:00:00', 'CONFIRMED');