package com.essjr.pcomp.application.infrastructure.parser;


import com.essjr.pcomp.domain.model.DocumentoCompra;
import com.essjr.pcomp.domain.model.ItemDoc;
import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class OmieNfParser {

    public DocumentoCompra parse(JsonNode nfJson) {
        String id = nfJson.path("cabec").path("cNumeroNFe").asText(null);

        List<ItemDoc> itens = new ArrayList<>();
        for (JsonNode item : nfJson.path("itensRecebimento")) {
            JsonNode cab = item.path("itensCabec");

            // se existir flag de ignorar
            if ("S".equalsIgnoreCase(cab.path("cIgnorarItem").asText("N"))) continue;

            String sku = text(cab, "cCodigoProduto");
            String ean = text(cab, "cEAN");
            String desc = text(cab, "cDescricaoProduto");

            BigDecimal qtd = dec(cab, "nQtdeNFe");
            BigDecimal unit = dec(cab, "nPrecoUnit");
            BigDecimal total = dec(cab, "vTotalItem");

            if (total == null && qtd != null && unit != null) total = unit.multiply(qtd);

            itens.add(new ItemDoc(sku, ean, desc, qtd, unit, total));
        }

        return new DocumentoCompra(id, itens);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static BigDecimal dec(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.decimalValue();
        String s = v.asText();
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        if (s.contains(",")) s = s.replace(".", "").replace(",", ".");
        return new BigDecimal(s);
    }
}
