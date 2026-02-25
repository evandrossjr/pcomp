package com.essjr.pcomp.api.dto.request;


import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ConferenciaJsonRequest(
        @NotNull JsonNode pedidoJson,
        @NotNull List<JsonNode> nfsJson,
        BigDecimal toleranciaValor
) {}
