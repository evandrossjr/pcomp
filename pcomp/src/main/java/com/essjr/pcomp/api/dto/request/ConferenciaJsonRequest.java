package com.essjr.pcomp.api.dto.request;


import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConferenciaJsonRequest(
        @NotNull JsonNode pedidoJson,
        @NotNull JsonNode nfJson,
        BigDecimal toleranciaValor
) {}
