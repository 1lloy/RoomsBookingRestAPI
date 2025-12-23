package com.illoy.roombooking.mapper;

import com.illoy.roombooking.database.entity.Room;
import com.illoy.roombooking.dto.request.RoomCreateEditRequest;
import com.illoy.roombooking.dto.response.RoomResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoomMapper {

    RoomResponse toResponse(Room room);

    @Mapping(target = "isActive", constant = "true")
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    Room toEntity(RoomCreateEditRequest request);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateEntity(RoomCreateEditRequest request, @MappingTarget Room room);
}
