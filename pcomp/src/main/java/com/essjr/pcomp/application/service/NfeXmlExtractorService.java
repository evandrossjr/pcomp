package com.essjr.pcomp.application.service;

import com.essjr.pcomp.api.dto.response.LinhaXmlDTO;
import com.essjr.pcomp.api.dto.response.XmlExtracaoResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class NfeXmlExtractorService {

    public XmlExtracaoResponse extrair(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Envie pelo menos 1 XML.");
        }
        if (files.length > 20) {
            throw new IllegalArgumentException("Máximo de 20 arquivos por envio.");
        }

        List<LinhaXmlDTO> linhas = new ArrayList<>();

        int seq = 1;
        for (MultipartFile f : files) {
            if (f.isEmpty()) continue;

            try (InputStream in = f.getInputStream()) {
                Document doc = parseXmlSeguro(in);

                String numeroNf = firstText(doc, "ide", "nNF");
                String naturezaOperacao = firstText(doc, "ide", "natOp");
                String fornecedor = firstText(doc, "emit", "xNome");
                String dataEmissao = firstText(doc, "ide", "dhEmi");
                if (isBlank(dataEmissao)) dataEmissao = firstText(doc, "ide", "dEmi");

                NodeList dets = doc.getElementsByTagName("det");
                for (int i = 0; i < dets.getLength(); i++) {
                    Element det = (Element) dets.item(i);

                    // dentro do det -> prod -> campos
                    String ean = firstText(det, "prod", "cEAN");
                    String cProd = firstText(det, "prod", "cProd");
                    String xProd = firstText(det, "prod", "xProd");

                    String qtdStr = firstText(det, "prod", "qCom");
                    BigDecimal quantidade = parseBigDecimal(qtdStr);

                    String vUnCom = firstText(det, "prod", "vUnCom");
                    String vProd = firstText(det, "prod", "vProd");

                    String cfop = firstText(det, "prod", "CFOP");

String observacoes = firstText(doc, "infAdic", "infCpl");

                    if (isBlank(vUnCom)) vUnCom = firstText(det, "prod", "vUnTrib");

                    
                    BigDecimal valorUnit = parseBigDecimal(vUnCom);
                    BigDecimal vTotal = parseBigDecimal(vProd);

                    // calcula total se não vier
                    if (vTotal == null && valorUnit != null && quantidade != null) {
                        vTotal = valorUnit.multiply(quantidade);
                    }
                    
                    // IPI (pode não existir)
                    BigDecimal ipiUnit = BigDecimal.ZERO;

                    NodeList ipiList = det.getElementsByTagName("IPI");
                    if (ipiList.getLength() > 0) {
                        Element ipi = (Element) ipiList.item(0);

                        // tenta pegar direto
                        String vIPI = firstText(ipi, "vIPI");

                        // tenta dentro de IPITrib
                        if (isBlank(vIPI)) {
                            vIPI = firstText(ipi, "IPITrib", "vIPI");
                        }

                        BigDecimal totalIPI = parseBigDecimal(vIPI);

                        BigDecimal qtd = quantidade;

                        if (totalIPI != null && qtd != null && qtd.compareTo(BigDecimal.ZERO) > 0) {
                            ipiUnit = totalIPI.divide(qtd, 6, RoundingMode.HALF_UP);
                        }
                        System.out.println("Produto: " + xProd);
                        System.out.println("Qtd: " + qtdStr);
                        System.out.println("IPI total: " + vIPI);
                    }

                    BigDecimal valorUnitComIpi = valorUnit != null ? valorUnit.add(ipiUnit) : ipiUnit;
                    BigDecimal vTotalComIpi = vTotal != null ? vTotal.add(ipiUnit.multiply(quantidade != null ? quantidade : BigDecimal.ONE))
                                               : valorUnitComIpi.multiply(quantidade != null ? quantidade : BigDecimal.ONE);
                   
                    // normalizações leves
                    if ("SEM GTIN".equalsIgnoreCase(safe(ean))) ean = "";

                    linhas.add(new LinhaXmlDTO(
                            seq++,
                            safe(numeroNf),
                            safe(f.getOriginalFilename()),
                            safe(fornecedor),
                            safe(dataEmissao),
                            safe(ean),
                            safe(cProd),
                            safe(xProd),
                            quantidade,
                            valorUnitComIpi,
                            ipiUnit,
                            vTotalComIpi,
                            safe(cfop),
                            safe(naturezaOperacao)
                    ));
                }

            } catch (Exception e) {
                // Se quiser “continuar mesmo com falha”, você pode gerar uma linha de erro.
                // Por enquanto, vamos falhar para você perceber XML inválido.
                throw new IllegalArgumentException("Falha ao ler XML: " + f.getOriginalFilename() + " | " + e.getMessage(), e);
            }
        }

        return new XmlExtracaoResponse(files.length, linhas.size(), linhas);
    }

    private Document parseXmlSeguro(InputStream in) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        // proteção XXE
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);

        dbf.setNamespaceAware(true);

        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(in);
    }

    private String firstText(Document doc, String parentTag, String childTag) {
        NodeList parents = doc.getElementsByTagName(parentTag);
        for (int i = 0; i < parents.getLength(); i++) {
            Node p = parents.item(i);
            if (p instanceof Element el) {
                String v = firstText(el, childTag);
                if (!isBlank(v)) return v;
            }
        }
        return null;
    }

    private String firstText(Element scope, String parentTag, String childTag) {
        NodeList parents = scope.getElementsByTagName(parentTag);
        for (int i = 0; i < parents.getLength(); i++) {
            Node p = parents.item(i);
            if (p instanceof Element el) {
                String v = firstText(el, childTag);
                if (!isBlank(v)) return v;
            }
        }
        return null;
    }

    private String firstText(Element scope, String childTag) {
        NodeList children = scope.getElementsByTagName(childTag);
        if (children.getLength() == 0) return null;
        String v = children.item(0).getTextContent();
        return isBlank(v) ? null : v.trim();
    }

    private BigDecimal parseBigDecimal(String s) {
        if (isBlank(s)) return null;
        try {
            String t = s.trim().replace(",", ".");
            return new BigDecimal(t);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
