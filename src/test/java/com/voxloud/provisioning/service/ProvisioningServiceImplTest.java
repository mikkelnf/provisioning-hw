package com.voxloud.provisioning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voxloud.provisioning.configuration.properties.ProvisioningProperties;
import com.voxloud.provisioning.entity.Device;
import com.voxloud.provisioning.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProvisioningServiceImplTest {

    @Mock private ProvisioningProperties provisioningProperties;
    @Mock private DeviceRepository deviceRepository;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private ProvisioningServiceImpl provisioningService;

    @BeforeEach
    void setUp() {
        when(provisioningProperties.getDomain()).thenReturn("sip.voxloud.com");
        when(provisioningProperties.getPort()).thenReturn("5060");
        when(provisioningProperties.getCodecs()).thenReturn(new String[]{"G711", "G729"});
    }

    @Test
    void shouldReturnMergedConfigForDeskModel_WhenOverrideFragmentIsNull() {
        Device device = createDevice(Device.DeviceModel.DESK, null);
        when(deviceRepository.findByMacAddress("aa-bb-cc-dd-ee-ff")).thenReturn(Optional.of(device));

        String result = provisioningService.getProvisioningFile("aa-bb-cc-dd-ee-ff");

        String expected = "username=john\npassword=doe\ndomain=sip.voxloud.com\nport=5060\ncodecs=G711,G729";

        assertEquals(parseConfigToMap(expected), parseConfigToMap(result));
    }

    private Map<String, String> parseConfigToMap(String config) {
        return Arrays.stream(config.split("\n"))
                .map(line -> line.split("=", 2))
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));
    }

    @Test
    void shouldReturnMergedConfigForDeskModel_WhenOverrideFragmentIsNotNull() {
        Device device = createDevice(Device.DeviceModel.DESK, "port=5070\nextraParam=value");
        when(deviceRepository.findByMacAddress("aa-bb-cc-dd-ee-ff")).thenReturn(Optional.of(device));

        String result = provisioningService.getProvisioningFile("aa-bb-cc-dd-ee-ff");

        String expected = "username=john\npassword=doe\ndomain=sip.voxloud.com\nport=5070\ncodecs=G711,G729\nextraParam=value";
        assertEquals(parseConfigToMap(expected), parseConfigToMap(result));
    }

    @Test
    void shouldReturnBaseConfigForConferenceModel_WhenOverrideFragmentIsNull() throws JsonProcessingException {
        Device device = createDevice(Device.DeviceModel.CONFERENCE, null);
        when(deviceRepository.findByMacAddress("aa-bb-cc-dd-ee-ff")).thenReturn(Optional.of(device));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"username\":\"john\",\"password\":\"doe\",\"domain\":\"sip.voxloud.com\",\"port\":5060,\"codecs\":[\"G711\",\"G729\"]}");

        String result = provisioningService.getProvisioningFile("aa-bb-cc-dd-ee-ff");

        String expected = "{\"username\":\"john\",\"password\":\"doe\",\"domain\":\"sip.voxloud.com\",\"port\":5060,\"codecs\":[\"G711\",\"G729\"]}";
        assertEquals(expected, result);
    }

    @Test
    void shouldReturnMergedConfigForConferenceModel_WhenOverrideFragmentIsNotNull() throws IOException {
        String overrideJson = "{\"port\":5070,\"extraParam\":\"value\"}";
        Device device = createDevice(Device.DeviceModel.CONFERENCE, overrideJson);
        when(deviceRepository.findByMacAddress("aa-bb-cc-dd-ee-ff")).thenReturn(Optional.of(device));
        doReturn(Map.of("port", 5070, "extraParam", "value"))
                .when(objectMapper)
                .readValue(eq(overrideJson), any(TypeReference.class));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"username\":\"john\",\"password\":\"doe\",\"domain\":\"sip.voxloud.com\",\"port\":5070,\"codecs\":[\"G711\",\"G729\"],\"extraParam\":\"value\"}");

        String result = provisioningService.getProvisioningFile("aa-bb-cc-dd-ee-ff");

        String expected = "{\"username\":\"john\",\"password\":\"doe\",\"domain\":\"sip.voxloud.com\",\"port\":5070,\"codecs\":[\"G711\",\"G729\"],\"extraParam\":\"value\"}";
        assertEquals(expected, result);
    }

    private Device createDevice(Device.DeviceModel model, String overrideFragment) {
        Device device = new Device();
        device.setMacAddress("aa-bb-cc-dd-ee-ff");
        device.setUsername("john");
        device.setPassword("doe");
        device.setModel(model);
        device.setOverrideFragment(overrideFragment);
        return device;
    }
}