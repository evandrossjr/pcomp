package com.essjr.pcomp.application.service;


import org.springframework.stereotype.Service;
import com.essjr.pcomp.api.dto.response.AlertaItemDTO;
import com.essjr.pcomp.api.dto.response.RelatorioConferenciaDTO;
import com.essjr.pcomp.application.infrastructure.parser.OmieNfParser;
import com.essjr.pcomp.application.infrastructure.parser.OmiePedidoParser;
import com.essjr.pcomp.domain.model.DocumentoCompra;
import com.essjr.pcomp.domain.model.ItemDoc;
import com.essjr.pcomp.domain.service.ItemAggregator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;

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

    public RelatorioConferenciaDTO conferir(JsonNode pedidoJson,  List<JsonNode> nfsJson, BigDecimal toleranciaValor) {

        if (nfsJson == null || nfsJson.isEmpty()) {
            throw new IllegalArgumentException("É obrigatório informar pelo menos uma NF.");
        }

        DocumentoCompra pedido = pedidoParser.parse(pedidoJson);
        List<ItemDoc> itensNfTotal = new ArrayList<>();
        for (JsonNode nfJson : nfsJson) {
            DocumentoCompra nf = nfParser.parse(nfJson);
            itensNfTotal.addAll(nf.itens());
        }
        BigDecimal tol = (toleranciaValor == null) ? BigDecimal.ZERO : toleranciaValor;

        // mapas para avisos de cadastro
        var pedidoEanSkus = mapEanParaSkus(pedido.itens());
        var nfEanSkus     = mapEanParaSkus(itensNfTotal);
        var pedidoSkuEans = mapSkuParaEans(pedido.itens());
        var nfSkuEans     = mapSkuParaEans(itensNfTotal);

        // agrega (1 linha por produto) -> GARANTA que ItemAggregator usa EAN como chave primeiro
        Map<String, ItemDoc> nfAgg = ItemAggregator.agruparPorChave(itensNfTotal);
        Map<String, ItemDoc> pedidoAgg = ItemAggregator.agruparPorChave(pedido.itens());

        List<AlertaItemDTO> linhas = new ArrayList<>();

        // 1) produtos do pedido
        for (var entry : pedidoAgg.entrySet()) {
            String chave = entry.getKey();
            ItemDoc p = entry.getValue();
            ItemDoc n = nfAgg.get(chave);

            String aviso = avisoCadastroParaChave(chave, pedidoEanSkus, nfEanSkus, pedidoSkuEans, nfSkuEans);

            String skuPedido = skuPrincipalPedido(chave, pedidoEanSkus);
            String skuNf = skuPrincipalNf(chave, nfEanSkus);

            BigDecimal qtdPedido = nz(p.quantidade());
            BigDecimal totalPedido = nz(p.valorTotal());

            // não existe na NF => FALTA
            if (n == null) {
                linhas.add(new AlertaItemDTO(
                        chave,
                        descSafe(p.descricao()),
                        skuPedido,
                        "-",                 // skuNf
                        "FALTA",
                        "-",                 // sem preço
                        aviso,
                        qtdPedido,
                        BigDecimal.ZERO,
                        totalPedido,
                        BigDecimal.ZERO
                ));
                continue;
            }

            BigDecimal qtdNf = nz(n.quantidade());
            BigDecimal totalNf = nz(n.valorTotal());

            // status qtd
            String statusQtd = "OK";
            int cmp = qtdNf.compareTo(qtdPedido);
            if (cmp < 0) statusQtd = "FALTA";
            else if (cmp > 0) statusQtd = "SOBRA";

            // status preço (só OK / PRECO_DIFERENTE)
            String statusPreco = diferente(totalPedido, totalNf, tol) ? "PRECO_DIFERENTE" : "OK";

            linhas.add(new AlertaItemDTO(
                    chave,
                    descSafe(p.descricao()),   // prefiro descrição do pedido
                    skuPedido,
                    skuNf,
                    statusQtd,
                    statusPreco,
                    aviso,
                    qtdPedido,
                    qtdNf,
                    totalPedido,
                    totalNf
            ));
        }

        // 2) itens da NF sem pedido
        for (var entry : nfAgg.entrySet()) {
            String chave = entry.getKey();
            if (pedidoAgg.containsKey(chave)) continue;

            ItemDoc n = entry.getValue();

            String skuNf = skuPrincipalNf(chave, nfEanSkus);

            linhas.add(new AlertaItemDTO(
                    chave,
                    descSafe(n.descricao()),
                    "-",                      // skuPedido
                    skuNf,
                    "ITEM_SEM_PEDIDO",
                    "",                       // sem preço
                    "OK",                     // você pediu sem aviso de preço; aqui fica OK
                    BigDecimal.ZERO,
                    nz(n.quantidade()),
                    BigDecimal.ZERO,
                    nz(n.valorTotal())
            ));
        }

        List<String> idsNf = new ArrayList<>();

        for (JsonNode nfJson : nfsJson) {
            DocumentoCompra nf = nfParser.parse(nfJson);
            itensNfTotal.addAll(nf.itens());
            idsNf.add(nf.id());
        }

        String nfIdConcat = String.join(", ", idsNf);

        String status = linhas.isEmpty() ? "OK" : "ALERTA";
        return new RelatorioConferenciaDTO(
                pedido.id(),
                nfIdConcat,
                status,
                pedidoAgg.size(),
                nfAgg.size(),
                linhas
        );
    }

    // ---------- helpers cadastro / sku-ean ----------

    private Map<String, Set<String>> mapEanParaSkus(List<ItemDoc> itens) {
        Map<String, Set<String>> m = new HashMap<>();
        for (ItemDoc i : itens) {
            String ean = (i.ean() == null || i.ean().isBlank()) ? null : i.ean().trim();
            String sku = (i.sku() == null || i.sku().isBlank()) ? null : normalizarSku(i.sku());
            if (ean == null || sku == null) continue;
            m.computeIfAbsent(ean, k -> new HashSet<>()).add(sku);
        }
        return m;
    }

    private Map<String, Set<String>> mapSkuParaEans(List<ItemDoc> itens) {
        Map<String, Set<String>> m = new HashMap<>();
        for (ItemDoc i : itens) {
            String sku = (i.sku() == null || i.sku().isBlank()) ? null : normalizarSku(i.sku());
            String ean = (i.ean() == null || i.ean().isBlank()) ? null : i.ean().trim();
            if (ean == null || sku == null) continue;
            m.computeIfAbsent(sku, k -> new HashSet<>()).add(ean);
        }
        return m;
    }

    private String avisoCadastroParaChave(String chaveProduto,
                                          Map<String, Set<String>> pedidoEanSkus,
                                          Map<String, Set<String>> nfEanSkus,
                                          Map<String, Set<String>> pedidoSkuEans,
                                          Map<String, Set<String>> nfSkuEans) {

        if (chaveProduto.startsWith("EAN:")) {
            String ean = chaveProduto.substring(4);
            Set<String> skusPedido = pedidoEanSkus.getOrDefault(ean, Set.of());
            Set<String> skusNf = nfEanSkus.getOrDefault(ean, Set.of());

            Set<String> uniao = new HashSet<>(skusPedido);
            uniao.addAll(skusNf);

            return (uniao.size() > 1) ? "CODIGO_DIFERENTE_MESMO_EAN" : "OK";
        }

        if (chaveProduto.startsWith("SKU:")) {
            String sku = chaveProduto.substring(4);
            Set<String> eansPedido = pedidoSkuEans.getOrDefault(sku, Set.of());
            Set<String> eansNf = nfSkuEans.getOrDefault(sku, Set.of());

            Set<String> uniao = new HashSet<>(eansPedido);
            uniao.addAll(eansNf);

            return (uniao.size() > 1) ? "EAN_DIFERENTE_MESMO_CODIGO" : "OK";
        }

        return "OK";
    }

    private String skuPrincipalPedido(String chaveProduto, Map<String, Set<String>> pedidoEanSkus) {
        if (chaveProduto.startsWith("EAN:")) {
            String ean = chaveProduto.substring(4);
            return escolherUm(pedidoEanSkus.get(ean));
        }
        if (chaveProduto.startsWith("SKU:")) return chaveProduto.substring(4);
        return "-";
    }

    private String skuPrincipalNf(String chaveProduto, Map<String, Set<String>> nfEanSkus) {
        if (chaveProduto.startsWith("EAN:")) {
            String ean = chaveProduto.substring(4);
            return escolherUm(nfEanSkus.get(ean));
        }
        if (chaveProduto.startsWith("SKU:")) return chaveProduto.substring(4);
        return "-";
    }

    private String escolherUm(Set<String> set) {
        if (set == null || set.isEmpty()) return "-";
        return set.stream().sorted().findFirst().orElse("-");
    }

    private String normalizarSku(String sku) {
        String s = sku.trim();
        return s.replaceFirst("^0+(?!$)", "");
    }

    private String descSafe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private boolean diferente(BigDecimal a, BigDecimal b, BigDecimal tol) {
        return nz(a).subtract(nz(b)).abs().compareTo(tol) > 0;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}