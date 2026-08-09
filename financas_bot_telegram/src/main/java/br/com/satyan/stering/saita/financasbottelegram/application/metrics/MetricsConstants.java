package br.com.satyan.stering.saita.financasbottelegram.application.metrics;

public final class MetricsConstants {

    private MetricsConstants() {}

    // Nomes das métricas
    public static final String MENSAGEM_ENTRANTE_TIMER = "mensagem_entrante_processamento";
    public static final String NOTIFICACAO_ENVIO_TIMER = "notificacao_envio";
    public static final String NOTIFICACAO_FALHA_COUNTER = "notificacao_falha";

    // Chaves de dimensão
    public static final String TAG_TIPO = "tipo";
    public static final String TAG_CANAL = "canal";
    public static final String TAG_MOTIVO = "motivo";

    // Valores de tipo
    public static final String TIPO_PEDIDO = "PEDIDO";
    public static final String TIPO_COMPROVANTE = "COMPROVANTE";

    // Valores de motivo (notificacao_falha)
    public static final String MOTIVO_SEM_NOTIFICADOR = "SEM_NOTIFICADOR";
    public static final String MOTIVO_EXCEPTION = "EXCEPTION";
}
