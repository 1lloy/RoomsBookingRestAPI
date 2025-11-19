package com.illoy.roombooking.mapper;

import com.illoy.roombooking.database.entity.Booking;
import com.illoy.roombooking.database.entity.User;
import com.illoy.roombooking.dto.request.BookingCreateRequest;
import com.illoy.roombooking.dto.request.BookingStatusUpdateRequest;
import com.illoy.roombooking.dto.request.RegisterRequest;
import com.illoy.roombooking.dto.request.UserEditRequest;
import com.illoy.roombooking.dto.response.BookingResponse;
import com.illoy.roombooking.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BookingMapper {

    BookingResponse toResponse(Booking booking);

    @Mapping(target = "isActive", constant = "true")
    Booking toEntity(BookingCreateRequest request);

    void updateEntity(BookingStatusUpdateRequest request, @MappingTarget Booking booking);
}
