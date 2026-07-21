package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.CarreraRequest;
import com.instituto.siga.dto.response.CarreraResponse;

import java.util.List;

public interface CarreraService {

    List<CarreraResponse> listar();

    CarreraResponse crear(CarreraRequest request);

    CarreraResponse actualizar(Long id, CarreraRequest request);

    void darDeBaja(Long id);
}
