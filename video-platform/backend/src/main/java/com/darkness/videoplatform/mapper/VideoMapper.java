package com.darkness.videoplatform.mapper;

import com.darkness.videoplatform.dto.VideoResponse;
import com.darkness.videoplatform.entity.User;
import com.darkness.videoplatform.entity.Video;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VideoMapper {

    @Mapping(target = "streamUrl", expression = "java(\"/api/videos/\" + video.getId() + \"/stream\")")
    @Mapping(target = "owner", source = "user")
    VideoResponse toResponse(Video video);

    VideoResponse.VideoOwner toVideoOwner(User user);
}
