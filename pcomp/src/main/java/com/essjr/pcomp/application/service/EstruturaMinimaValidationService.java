package com.essjr.pcomp.application.service;


import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
public class EstruturaMinimaValidationService {

    public void validarPedido(tools.jackson.databind.@NotNull(message = "pedidoJson é obrigatório") JsonNode pedido) {
        if (pedido == null || pedido.isNull() || pedido.isMissingNode()) {
            throw new IllegalArgumentException("pedido é obrigatório");
        }
        if (!pedido.hasNonNull("cabecalho_consulta")) {
            throw new IllegalArgumentException("pedido inválido: campo 'cabecalho_consulta' ausente");
        }
        if (!pedido.has("produtos_consulta") || !pedido.get("produtos_consulta").isArray()) {
            throw new IllegalArgumentException("pedido inválido: 'produtos_consulta' deve ser um array");
        }
    }

    public void validarNf(@NotNull(message = "nfJson é obrigatório") JsonNode nf) {
        if (nf == null || nf.isNull() || nf.isMissingNode()) {
            throw new IllegalArgumentException("nf é obrigatório");
        }
        if (!nf.hasNonNull("cabec")) {
            throw new IllegalArgumentException("nf inválida: campo 'cabec' ausente");
        }
        if (!nf.has("itensRecebimento") || !nf.get("itensRecebimento").isArray()) {
            throw new IllegalArgumentException("nf inválida: 'itensRecebimento' deve ser um array");
        }
    }
}