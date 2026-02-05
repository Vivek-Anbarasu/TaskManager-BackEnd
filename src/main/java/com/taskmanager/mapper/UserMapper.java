package com.taskmanager.mapper;

import com.taskmanager.api.dto.UserRegistrationRequest;
import com.taskmanager.domain.model.UserInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created_date", ignore = true)
    @Mapping(target = "created_by", ignore = true)
    @Mapping(target = "last_modified_date", ignore = true)
    @Mapping(target = "last_modified_by", ignore = true)
    UserInfo toUserInfo(UserRegistrationRequest request);
}
