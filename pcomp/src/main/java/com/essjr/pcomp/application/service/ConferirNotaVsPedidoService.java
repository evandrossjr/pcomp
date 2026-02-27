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
import java.util.stream.Collectors;

@Service
public class ConferirNotaVsPedidoService {

    private final OmiePedidoParser pedidoParser;
    private final OmieNfParser nfParser;

    public ConferirNotaVsPedidoService(OmiePedidoParser pedidoParser, OmieNfParser nfParser) {
        this.pedidoParser = pedidoParser;
        this.nfParser = nfParser;
    }

    public RelatorioConferenciaDTO conferir(JsonNode pedidoJson, List<JsonNode> nfsJson, BigDecimal toleranciaValor) {

        if (nfsJson == null || nfsJson.isEmpty()) {
            throw new IllegalArgumentException("É obrigatório informar pelo menos uma NF.");
        }

        BigDecimal tol = nz(toleranciaValor);

        DocumentoCompra pedido = pedidoParser.parse(pedidoJson);

        // 1) Parseia TODAS as NFs e junta itens
        ParseNfsResult nfs = parseNfs(nfsJson);
        List<ItemDoc> itensNfTotal = nfs.itensNfTotal();
        String nfIdConcat = nfs.nfIdConcat();

        // 2) Mapas para avisos de cadastro e SKUs principais
        var pedidoEanSkus = mapEanParaSkus(pedido.itens());
        var nfEanSkus     = mapEanParaSkus(itensNfTotal);
        var pedidoSkuEans = mapSkuParaEans(pedido.itens());
        var nfSkuEans     = mapSkuParaEans(itensNfTotal);

        // 3) Agrega (1 linha por produto)
        Map<String, ItemDoc> pedidoAgg = ItemAggregator.agruparPorChave(pedido.itens());
        Map<String, ItemDoc> nfAgg     = ItemAggregator.agruparPorChave(itensNfTotal);

        // 4) Totais gerais
        BigDecimal totalPedidoGeral = somarTotais(pedidoAgg.values());
        BigDecimal totalNfGeral     = somarTotais(nfAgg.values());

        // 5) Monta linhas (na regra: pedido primeiro, depois NF sem pedido)
        List<AlertaItemDTO> linhas = new ArrayList<>();

        // 5.1) Itens do pedido (comparação principal)
        for (var entry : pedidoAgg.entrySet()) {
            String chave = entry.getKey();
            ItemDoc p = entry.getValue();
            ItemDoc n = nfAgg.get(chave);

            String aviso = avisoCadastroParaChave(chave, pedidoEanSkus, nfEanSkus, pedidoSkuEans, nfSkuEans);

            String skuPedido = skuPrincipalPedido(chave, pedidoEanSkus);
            String skuNf = skuPrincipalNf(chave, nfEanSkus);

            BigDecimal qtdPedido = nz(p.quantidade());
            BigDecimal unitPedido = nz(p.valorUnitario());
            BigDecimal totalPedido = nz(p.valorTotal());

            // Se não existe na NF => falta total
            if (n == null) {
                BigDecimal qtdNf = BigDecimal.ZERO;

                String stQtd = statusQtd(qtdPedido, qtdNf);
                BigDecimal diffQtd = qtdDiferenca(qtdPedido, qtdNf);

                linhas.add(new AlertaItemDTO(
                        chave,
                        descSafe(p.descricao()),
                        skuPedido,
                        "-",
                        stQtd,
                        diffQtd,
                        unitPedido,
                        BigDecimal.ZERO,
                        "- (SEM MUDANÇA)",      // você pode preferir "-" aqui
                        aviso,
                        qtdPedido,
                        qtdNf,
                        totalPedido,
                        BigDecimal.ZERO
                ));
                continue;
            }

            // Existe na NF
            BigDecimal qtdNf = nz(n.quantidade());
            BigDecimal unitNf = nz(n.valorUnitario());
            BigDecimal totalNf = nz(n.valorTotal());

            String stQtd = statusQtd(qtdPedido, qtdNf);
            BigDecimal diffQtd = qtdDiferenca(qtdPedido, qtdNf);

            // Status preço pelo UNITÁRIO (como você pediu)
            String stPrecoTexto = statusPrecoTexto(unitPedido, unitNf, tol);

            linhas.add(new AlertaItemDTO(
                    chave,
                    descSafe(prioridadeDesc(p.descricao(), n.descricao())),
                    skuPedido,
                    skuNf,
                    stQtd,
                    diffQtd,
                    unitPedido,
                    unitNf,
                    stPrecoTexto,
                    aviso,
                    qtdPedido,
                    qtdNf,
                    totalPedido,
                    totalNf
            ));
        }

        // 5.2) Itens na NF que não estão no pedido
        for (var entry : nfAgg.entrySet()) {
            String chave = entry.getKey();
            if (pedidoAgg.containsKey(chave)) continue;

            ItemDoc n = entry.getValue();

            String skuNf = skuPrincipalNf(chave, nfEanSkus);
            BigDecimal qtdNf = nz(n.quantidade());
            BigDecimal unitNf = nz(n.valorUnitario());
            BigDecimal totalNf = nz(n.valorTotal());

            linhas.add(new AlertaItemDTO(
                    chave,
                    descSafe(n.descricao()),
                    "-",
                    skuNf,
                    "ITEM_SEM_PEDIDO",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    unitNf,
                    "",                 // sem status de preço como você pediu
                    "OK",
                    BigDecimal.ZERO,
                    qtdNf,
                    BigDecimal.ZERO,
                    totalNf
            ));
        }

        // 6) Status geral
        String status = linhas.isEmpty() ? "OK" : "ALERTA";

        return new RelatorioConferenciaDTO(
                pedido.id(),
                nfIdConcat,
                status,
                pedidoAgg.size(),
                nfAgg.size(),
                totalPedidoGeral,
                totalNfGeral,
                linhas
        );
    }

    // ------------------------ parsing NFs ------------------------

    private ParseNfsResult parseNfs(List<JsonNode> nfsJson) {
        List<ItemDoc> itensNfTotal = new ArrayList<>();
        List<String> idsNf = new ArrayList<>();

        for (JsonNode nfJson : nfsJson) {
            DocumentoCompra nf = nfParser.parse(nfJson);
            idsNf.add(nf.id());
            itensNfTotal.addAll(nf.itens());
        }

        String nfIdConcat = idsNf.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));

        if (nfIdConcat.isBlank()) nfIdConcat = "MULTI_NF";

        return new ParseNfsResult(itensNfTotal, nfIdConcat);
    }

    private record ParseNfsResult(List<ItemDoc> itensNfTotal, String nfIdConcat) {}

    // ------------------------ totais ------------------------

    private BigDecimal somarTotais(Collection<ItemDoc> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemDoc i : itens) total = total.add(nz(i.valorTotal()));
        return total;
    }

    // ------------------------ helpers cadastro / sku-ean ------------------------

    private Map<String, Set<String>> mapEanParaSkus(List<ItemDoc> itens) {
        Map<String, Set<String>> m = new HashMap<>();
        for (ItemDoc i : itens) {
            String ean = normEan(i.ean());
            String sku = normSku(i.sku());
            if (ean == null || sku == null) continue;
            m.computeIfAbsent(ean, k -> new HashSet<>()).add(sku);
        }
        return m;
    }

    private Map<String, Set<String>> mapSkuParaEans(List<ItemDoc> itens) {
        Map<String, Set<String>> m = new HashMap<>();
        for (ItemDoc i : itens) {
            String sku = normSku(i.sku());
            String ean = normEan(i.ean());
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

    private String normSku(String sku) {
        if (sku == null) return null;
        String s = sku.trim();
        if (s.isEmpty()) return null;
        // remove zeros à esquerda
        return s.replaceFirst("^0+(?!$)", "");
    }

    private String normEan(String ean) {
        if (ean == null) return null;
        String s = ean.trim();
        if (s.isEmpty()) return null;
        return s;
    }

    // ------------------------ regras negócio ------------------------

    private String statusQtd(BigDecimal qtdPedido, BigDecimal qtdNf) {
        int cmp = nz(qtdNf).compareTo(nz(qtdPedido));
        if (cmp == 0) return "OK";
        if (cmp > 0) return "SOBRA";
        // cmp < 0
        return nz(qtdNf).compareTo(BigDecimal.ZERO) > 0 ? "FALTA_PARCIAL" : "FALTA";
    }

    private BigDecimal qtdDiferenca(BigDecimal qtdPedido, BigDecimal qtdNf) {
        // positivo = pendente | negativo = a mais
        return nz(qtdPedido).subtract(nz(qtdNf));
    }

    private String statusPrecoTexto(BigDecimal unitPedido, BigDecimal unitNf, BigDecimal tol) {
        BigDecimal a = nz(unitPedido);
        BigDecimal b = nz(unitNf);

        BigDecimal diff = b.subtract(a); // NF - Pedido

        // tolerância
        if (diff.abs().compareTo(nz(tol)) <= 0) {
            return "R$ 0";
        }

        // se menor, prefixo com - colado (sem espaço) pra evitar Excel "inventar fórmula"
        if (diff.compareTo(BigDecimal.ZERO) < 0) {
            return "-R$ " + diff.abs().toPlainString();
        }

        // se maior
        return "R$ " + diff.toPlainString();
    }

    // ------------------------ util ------------------------

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String descSafe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private String prioridadeDesc(String descPedido, String descNf) {
        // usa pedido primeiro, se vazio usa NF
        if (descPedido != null && !descPedido.isBlank()) return descPedido;
        return descNf;
    }
}