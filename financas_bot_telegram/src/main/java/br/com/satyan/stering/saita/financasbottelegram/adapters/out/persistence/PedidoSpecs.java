package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.StatusPedido;
import br.com.satyan.stering.saita.financasbottelegram.domain.enums.TipoPagamento;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class PedidoSpecs {

    private PedidoSpecs() {}

    public static Specification<PedidoPagamentoEntity> doRequisitante(Long requisitanteId) {
        return (root, query, cb) -> cb.equal(root.get("requisitanteId"), requisitanteId);
    }

    public static Specification<PedidoPagamentoEntity> comStatus(StatusPedido status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<PedidoPagamentoEntity> comTipos(List<TipoPagamento> tipos) {
        return (root, query, cb) ->
                (tipos == null || tipos.isEmpty()) ? null : root.get("tipo").in(tipos);
    }

    public static Specification<PedidoPagamentoEntity> dataPedidoDesde(LocalDate de) {
        return (root, query, cb) ->
                de == null ? null : cb.greaterThanOrEqualTo(root.get("dataPedido"), de);
    }

    public static Specification<PedidoPagamentoEntity> dataPedidoAte(LocalDate ate) {
        return (root, query, cb) ->
                ate == null ? null : cb.lessThanOrEqualTo(root.get("dataPedido"), ate);
    }

    public static Specification<PedidoPagamentoEntity> comBusca(String busca) {
        return (root, query, cb) -> {
            if (busca == null || busca.isBlank()) return null;
            String like = "%" + busca.toLowerCase() + "%";
            Predicate likeDesc = cb.like(cb.lower(root.get("descricao")), like);
            try {
                BigDecimal valor = new BigDecimal(busca.replace(",", "."));
                return cb.or(likeDesc, cb.equal(root.get("valor"), valor));
            } catch (NumberFormatException e) {
                return likeDesc;
            }
        };
    }
}
