package com.essjr.pcomp.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record ValidarJsonRequest (@NotNull(message = "pedidoJson é obrigatório")
                                  JsonNode pedidoJson,

                                  @NotNull(message = "nfJson é obrigatório")
                                  JsonNode nfJson){}
