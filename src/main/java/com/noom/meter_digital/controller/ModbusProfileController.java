package com.noom.meter_digital.controller;

import com.noom.meter_digital.model.modbus.ModbusProfile;
import com.noom.meter_digital.service.ModbusProfileManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/modbus/profiles", "/api/modbus/profiles"})
public class ModbusProfileController {

    @Autowired
    private ModbusProfileManagerService profileManagerService;

    @GetMapping
    public List<ModbusProfile> getAllProfiles() {
        return profileManagerService.getAllProfiles();
    }

    @GetMapping("/{modelKey}")
    public ModbusProfile getProfileByModelKey(@PathVariable String modelKey) {
        ModbusProfile profile = profileManagerService.getProfile(modelKey);
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found for " + modelKey);
        }
        return profile;
    }

    @PostMapping
    public ModbusProfile saveOrUpdateProfile(@RequestBody ModbusProfile profile) {
        try {
            return profileManagerService.saveProfile(profile);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save profile JSON: " + e.getMessage());
        }
    }

    @DeleteMapping("/{modelKey}")
    public Map<String, Object> deleteProfile(@PathVariable String modelKey) {
        boolean deleted = profileManagerService.deleteProfile(modelKey);
        return Map.of(
                "success", deleted,
                "message", deleted ? "Profile " + modelKey + " deleted" : "Could not delete profile " + modelKey
        );
    }

    @PostMapping("/reload")
    public Map<String, Object> reloadProfiles() {
        profileManagerService.loadProfilesFromDisk();
        return Map.of(
                "success", true,
                "message", "Profiles reloaded from disk directory",
                "count", profileManagerService.getAllProfiles().size()
        );
    }
}
