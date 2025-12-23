package com.illoy.roombooking.mapper;

import com.illoy.roombooking.database.entity.User;
import com.illoy.roombooking.dto.request.RegisterRequest;
import com.illoy.roombooking.dto.response.UserResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "isActive", constant = "true")
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    User toEntity(RegisterRequest request);
}
