package com.essjr.pcomp.api.dto.request;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

public record ConferenciaMultiNfRequest(
        JsonNode pedidoJson,
        List<JsonNode> nfsJson,
        BigDecimal toleranciaValor
) {
}
