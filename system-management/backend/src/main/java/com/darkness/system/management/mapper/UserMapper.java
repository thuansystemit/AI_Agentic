package com.darkness.system.management.mapper;

import com.darkness.system.management.domain.User;
import com.darkness.system.management.dto.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
