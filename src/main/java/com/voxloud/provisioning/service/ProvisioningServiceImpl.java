package com.voxloud.provisioning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voxloud.provisioning.configuration.properties.ProvisioningProperties;
import com.voxloud.provisioning.constant.ExceptionConstants;
import com.voxloud.provisioning.entity.Device;
import com.voxloud.provisioning.exception.BusinessException;
import com.voxloud.provisioning.exception.TechnicalException;
import com.voxloud.provisioning.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProvisioningServiceImpl implements ProvisioningService {
    private final ProvisioningProperties provisioningProperties;
    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;

    public String getProvisioningFile(String macAddress) {
        Device device = deviceRepository.findByMacAddress(macAddress)
                .orElseThrow(() -> new BusinessException(ExceptionConstants.ERR_CODE_40000, ExceptionConstants.ERR_DESCRIPTION_40000));

        Map<String, Object> baseConfig = getBaseConfig(device);

        if (!ObjectUtils.isEmpty(device.getOverrideFragment())) {
            try {
                Map<String, Object> overrides = parseOverrideFragment(device.getOverrideFragment(), device.getModel());
                baseConfig.putAll(overrides);
            } catch (IOException e) {
                throw new TechnicalException("Failed to parse override fragment");
            }
        }

        return formatResponse(device.getModel(), baseConfig);
    }

    private Map<String, Object> getBaseConfig(Device device) {
        Map<String, Object> config = new HashMap<>();
        config.put("username", device.getUsername());
        config.put("password", device.getPassword());
        config.put("domain", provisioningProperties.getDomain());
        config.put("port", provisioningProperties.getPort());
        config.put("codecs", provisioningProperties.getCodecs());
        return config;
    }

    private Map<String, Object> parseOverrideFragment(String overrideFragment, Device.DeviceModel model) throws IOException {
        if (Device.DeviceModel.DESK.equals(model)) {
            Properties properties = new Properties();
            try (StringReader reader = new StringReader(overrideFragment)) {
                properties.load(reader);
            }
            return propertiesToMap(properties);
        } else if (Device.DeviceModel.CONFERENCE.equals(model)) {
            return objectMapper.readValue(overrideFragment, new TypeReference<Map<String, Object>>() {});
        }
        throw new BusinessException(ExceptionConstants.ERR_CODE_40001, ExceptionConstants.ERR_DESCRIPTION_40001);
    }

    private Map<String, Object> propertiesToMap(Properties properties) {
        Map<String, Object> map = new HashMap<>();
        properties.forEach((key, value) -> map.put(key.toString(), value));
        return map;
    }

    private String formatResponse(Device.DeviceModel model, Map<String, Object> response) {
        if (Device.DeviceModel.DESK.equals(model)) {

            return response.entrySet().stream()
                    .map(entry -> {
                        String value = entry.getValue() instanceof String[]
                                ? String.join(",", (String[]) entry.getValue())
                                : String.valueOf(entry.getValue());
                        return entry.getKey().concat("=").concat(value);
                    })
                    .collect(Collectors.joining("\n"));
        } else {
            try {
                return objectMapper.writeValueAsString(response);
            } catch (JsonProcessingException e) {
                throw new TechnicalException("Failed to serialize JSON response");
            }
        }
    }
}
