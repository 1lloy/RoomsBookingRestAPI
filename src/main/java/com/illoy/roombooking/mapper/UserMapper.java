package com.illoy.roombooking.mapper;

import com.illoy.roombooking.database.entity.User;
import com.illoy.roombooking.dto.request.RegisterRequest;
import com.illoy.roombooking.dto.request.UserEditRequest;
import com.illoy.roombooking.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "isActive", constant = "true")
    User toEntity(RegisterRequest request);

    void updateEntity(UserEditRequest request, @MappingTarget User user);
}
