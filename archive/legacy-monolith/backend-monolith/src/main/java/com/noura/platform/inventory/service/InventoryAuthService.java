package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.auth.InventoryAuthResponse;
import com.noura.platform.inventory.dto.auth.InventoryCurrentUserResponse;
import com.noura.platform.inventory.dto.auth.InventoryLoginRequest;
import com.noura.platform.inventory.dto.auth.InventoryRegisterRequest;

public interface InventoryAuthService {

    InventoryAuthResponse register(InventoryRegisterRequest request);

    InventoryAuthResponse login(InventoryLoginRequest request);

    InventoryCurrentUserResponse currentUser();
}
