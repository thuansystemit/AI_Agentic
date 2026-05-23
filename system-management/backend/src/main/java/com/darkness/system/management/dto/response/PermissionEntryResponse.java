package com.darkness.system.management.dto.response;

import com.darkness.system.management.domain.enums.Permission;

import java.util.UUID;

public record PermissionEntryResponse(UUID subjectId, String subjectName, Permission permission) {}
