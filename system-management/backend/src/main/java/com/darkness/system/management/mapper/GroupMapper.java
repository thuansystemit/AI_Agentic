package com.darkness.system.management.mapper;

import com.darkness.system.management.domain.Group;
import com.darkness.system.management.dto.response.GroupResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GroupMapper {
    GroupResponse toResponse(Group group);
}
