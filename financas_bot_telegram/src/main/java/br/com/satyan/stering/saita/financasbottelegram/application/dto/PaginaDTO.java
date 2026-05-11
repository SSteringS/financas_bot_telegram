package br.com.satyan.stering.saita.financasbottelegram.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Wrapper genérico de página com paginação")
public record PaginaDTO<T>(
        @Schema(description = "Itens da página atual") List<T> items,
        @Schema(description = "Total de itens encontrados (em todas as páginas)", example = "142") long total,
        @Schema(description = "Página atual (0-indexed)", example = "0") int pagina,
        @Schema(description = "Tamanho de página solicitado", example = "20") int tamanho,
        @Schema(description = "Total de páginas", example = "8") int totalPaginas
) {}
