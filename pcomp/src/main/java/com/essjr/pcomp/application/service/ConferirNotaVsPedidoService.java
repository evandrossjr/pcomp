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

        // 1) Conferir itens do pedido contra a NF
        for (var entry : pedidoAgg.entrySet()) {
            String chave = entry.getKey();
            ItemDoc p = entry.getValue();
            ItemDoc n = nfAgg.get(chave);

            if (n == null) {
                alertas.add(new AlertaItemDTO(
                        chave, "FALTA",
                        nz(p.quantidade()), BigDecimal.ZERO,
                        nz(p.valorUnitario()), BigDecimal.ZERO,
                        nz(p.valorTotal()), BigDecimal.ZERO,
                        "Item do pedido não encontrado na NF"
                ));
                continue;
            }

            BigDecimal qtdPedido = nz(p.quantidade());
            BigDecimal qtdNf = nz(n.quantidade());

            int cmpQtd = qtdNf.compareTo(qtdPedido);
            if (cmpQtd < 0) {
                alertas.add(new AlertaItemDTO(
                        chave, "FALTA",
                        qtdPedido, qtdNf,
                        nz(p.valorUnitario()), nz(n.valorUnitario()),
                        nz(p.valorTotal()), nz(n.valorTotal()),
                        "Quantidade menor na NF (após agrupar)"
                ));
            } else if (cmpQtd > 0) {
                alertas.add(new AlertaItemDTO(
                        chave, "SOBRA",
                        qtdPedido, qtdNf,
                        nz(p.valorUnitario()), nz(n.valorUnitario()),
                        nz(p.valorTotal()), nz(n.valorTotal()),
                        "Quantidade maior na NF (após agrupar)"
                ));
            }

            // 💡 Recomendado: comparar principalmente TOTAL (mais confiável em NF)
            if (diferente(nz(p.valorTotal()), nz(n.valorTotal()), tol)) {
                alertas.add(new AlertaItemDTO(
                        chave, "VALOR_DIVERGENTE",
                        qtdPedido, qtdNf,
                        nz(p.valorUnitario()), nz(n.valorUnitario()),
                        nz(p.valorTotal()), nz(n.valorTotal()),
                        "Total do item divergente (após agrupar)"
                ));
            }

            // Opcional: também alertar unitário médio divergente (informativo)
            if (diferente(nz(p.valorUnitario()), nz(n.valorUnitario()), tol)) {
                alertas.add(new AlertaItemDTO(
                        chave, "VALOR_DIVERGENTE",
                        qtdPedido, qtdNf,
                        nz(p.valorUnitario()), nz(n.valorUnitario()),
                        nz(p.valorTotal()), nz(n.valorTotal()),
                        "Valor unitário médio divergente (após agrupar)"
                ));
            }
        }

        // 2) Itens na NF que não existem no pedido
        for (var entry : nfAgg.entrySet()) {
            String chave = entry.getKey();
            if (!pedidoAgg.containsKey(chave)) {
                ItemDoc n = entry.getValue();
                alertas.add(new AlertaItemDTO(
                        chave, "ITEM_NAO_PEDIDO",
                        BigDecimal.ZERO, nz(n.quantidade()),
                        BigDecimal.ZERO, nz(n.valorUnitario()),
                        BigDecimal.ZERO, nz(n.valorTotal()),
                        "Item existe na NF mas não existe no pedido (após agrupar)"
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