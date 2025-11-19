package com.illoy.roombooking.mapper;

import com.illoy.roombooking.database.entity.Room;
import com.illoy.roombooking.dto.request.RoomCreateEditRequest;
import com.illoy.roombooking.dto.response.RoomResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoomMapper {

    RoomResponse toResponse(Room room);

    @Mapping(target = "isActive", constant = "true")
    Room toEntity(RoomCreateEditRequest request);

    void updateEntity(RoomCreateEditRequest request, @MappingTarget Room room);
}
