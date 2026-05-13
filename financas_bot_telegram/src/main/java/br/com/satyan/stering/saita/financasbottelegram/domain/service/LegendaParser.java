package br.com.satyan.stering.saita.financasbottelegram.domain.service;

import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LegendaParser {

    private static final Map<String, TipoPagamento> PALAVRAS_CHAVE = new LinkedHashMap<>();
    static {
        PALAVRAS_CHAVE.put("boleto", TipoPagamento.BOLETO);
        PALAVRAS_CHAVE.put("pix", TipoPagamento.PIX);
        PALAVRAS_CHAVE.put("ted", TipoPagamento.TED);
        PALAVRAS_CHAVE.put("agendamento", TipoPagamento.AGENDAMENTO);
    }

    private LegendaParser() {}

    public static TipoPagamento parseTipo(String legenda) {
        if (legenda == null || legenda.isBlank()) return TipoPagamento.OUTRO;
        String alvo = legenda.toLowerCase();

        int posicaoMaisCedo = Integer.MAX_VALUE;
        TipoPagamento tipoEncontrado = TipoPagamento.OUTRO;

        for (Map.Entry<String, TipoPagamento> entry : PALAVRAS_CHAVE.entrySet()) {
            int pos = alvo.indexOf(entry.getKey());
            if (pos >= 0 && pos < posicaoMaisCedo) {
                posicaoMaisCedo = pos;
                tipoEncontrado = entry.getValue();
            }
        }

        return tipoEncontrado;
    }
}
