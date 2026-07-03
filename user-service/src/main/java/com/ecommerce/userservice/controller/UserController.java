package com.ecommerce.userservice.controller;

import com.ecommerce.common.context.UserContext;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.userservice.dto.AddressDto;
import com.ecommerce.userservice.dto.UserDto;
import com.ecommerce.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfile() {
        UUID userId = getAuthenticatedUserId();
        UserDto profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/addresses")
    public ResponseEntity<UserDto> addAddress(@Valid @RequestBody AddressDto addressDto) {
        UUID userId = getAuthenticatedUserId();
        UserDto updatedProfile = userService.addAddress(userId, addressDto);
        return ResponseEntity.ok(updatedProfile);
    }

    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<UserDto> deleteAddress(@PathVariable UUID addressId) {
        UUID userId = getAuthenticatedUserId();
        UserDto updatedProfile = userService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(updatedProfile);
    }

    private UUID getAuthenticatedUserId() {
        String userIdStr = UserContext.getCurrentContext().getUserId();
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new BadRequestException("Unauthenticated or missing User Context header");
        }
        try {
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid User ID format propagated from gateway");
        }
    }
}
