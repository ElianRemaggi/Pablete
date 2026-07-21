package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.ConfiguracionInstitucionalRequest;
import com.instituto.siga.dto.response.ConfiguracionInstitucionalResponse;

public interface ConfiguracionInstitucionalService {

    ConfiguracionInstitucionalResponse obtener();

    ConfiguracionInstitucionalResponse actualizar(ConfiguracionInstitucionalRequest request);
}
