package com.illoy.roombooking.mapper;

import com.illoy.roombooking.database.entity.Booking;
import com.illoy.roombooking.dto.request.BookingCreateRequest;
import com.illoy.roombooking.dto.request.BookingStatusUpdateRequest;
import com.illoy.roombooking.dto.response.BookingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BookingMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.username")
    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomName", source = "room.name")
    @Mapping(target = "roomCapacity", source = "room.capacity")
    BookingResponse toResponse(Booking booking);

    Booking toEntity(BookingCreateRequest request);
}
