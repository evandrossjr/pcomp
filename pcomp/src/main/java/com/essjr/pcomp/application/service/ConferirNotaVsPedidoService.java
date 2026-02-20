package com.essjr.pcomp.application.service;

import com.essjr.pcomp.api.dto.response.AlertaItemDTO;
import com.essjr.pcomp.api.dto.response.RelatorioConferenciaDTO;
import com.essjr.pcomp.application.infrastructure.parser.OmieNfParser;
import com.essjr.pcomp.application.infrastructure.parser.OmiePedidoParser;
import com.essjr.pcomp.domain.model.DocumentoCompra;
import com.essjr.pcomp.domain.model.ItemDoc;
import com.essjr.pcomp.domain.service.ItemAggregator;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;


@Service
public class ConferirNotaVsPedidoService {

    private final ObjectMapper objectMapper;
    private final OmiePedidoParser pedidoParser;
    private final OmieNfParser nfParser;

    public ConferirNotaVsPedidoService(ObjectMapper objectMapper, OmiePedidoParser pedidoParser, OmieNfParser nfParser) {
        this.objectMapper = objectMapper;
        this.pedidoParser = pedidoParser;
        this.nfParser = nfParser;
    }

    public RelatorioConferenciaDTO conferir(
            JsonNode pedidoJson,
            JsonNode nfJson,
            BigDecimal toleranciaValor) {

        DocumentoCompra pedido = pedidoParser.parse(pedidoJson);
        DocumentoCompra nf = nfParser.parse(nfJson);

        BigDecimal tol = (toleranciaValor == null) ? BigDecimal.ZERO : toleranciaValor;


        // ✅ AGREGA (soma duplicados)
        Map<String, ItemDoc> pedidoAgg = ItemAggregator.agruparPorChave(pedido.itens());
        Map<String, ItemDoc> nfAgg = ItemAggregator.agruparPorChave(nf.itens());

        List<AlertaItemDTO> alertas = new ArrayList<>();

        // 1) Itens do pedido (existem no pedido)
        for (var entry : pedidoAgg.entrySet()) {
            String chave = entry.getKey();
            ItemDoc p = entry.getValue();
            ItemDoc n = nfAgg.get(chave);

            BigDecimal qtdPedido = nz(p.quantidade());
            BigDecimal totalPedido = nz(p.valorTotal());

            // Se não existe na NF => FALTA (qtd NF = 0)
            if (n == null) {
                alertas.add(new AlertaItemDTO(
                        chave,
                        p.descricao(),
                        "FALTA",
                        "-",             // sem NF, não tem como comparar preço (mas você pode deixar OK)
                        qtdPedido,
                        BigDecimal.ZERO,
                        totalPedido,
                        BigDecimal.ZERO
                ));
                continue;
            }

            BigDecimal qtdNf = nz(n.quantidade());
            BigDecimal totalNf = nz(n.valorTotal());

            // Status Quantidade
            String statusQtd = "OK";
            int cmpQtd = qtdNf.compareTo(qtdPedido);
            if (cmpQtd < 0) statusQtd = "FALTA";
            else if (cmpQtd > 0) statusQtd = "SOBRA";

            // Status Preço (você quer apenas OK ou PRECO_DIFERENTE)
            String statusPreco = diferente(totalPedido, totalNf, tol) ? "PRECO_DIFERENTE" : "OK";

            alertas.add(new AlertaItemDTO(
                    chave,
                    p.descricao(),
                    statusQtd,
                    statusPreco,
                    qtdPedido,
                    qtdNf,
                    totalPedido,
                    totalNf
            ));
        }

// 2) Itens na NF que não estão no pedido => ITEM_SEM_PEDIDO (sem aviso de preço)
        for (var entry : nfAgg.entrySet()) {
            String chave = entry.getKey();
            if (!pedidoAgg.containsKey(chave)) {
                ItemDoc n = entry.getValue();

                alertas.add(new AlertaItemDTO(
                        chave,
                        n.descricao(),   // 👈 descrição da NF
                        "ITEM_SEM_PEDIDO",
                        "",                   // sem status de preço (como você pediu)
                        BigDecimal.ZERO,
                        nz(n.quantidade()),
                        BigDecimal.ZERO,
                        nz(n.valorTotal())
                ));
            }
        }

        String status = alertas.isEmpty() ? "OK" : "ALERTA";
        return new RelatorioConferenciaDTO(
                pedido.id(), nf.id(), status,
                pedidoAgg.size(), nfAgg.size(),   // agora conta itens após agrupar
                alertas
        );
    }

    private boolean diferente(BigDecimal a, BigDecimal b, BigDecimal tol) {
        return a.subtract(b).abs().compareTo(tol) > 0;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}