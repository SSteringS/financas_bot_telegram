package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoPagamentoJpaRepository
        extends JpaRepository<PedidoPagamentoEntity, Long>,
                JpaSpecificationExecutor<PedidoPagamentoEntity> {

    @Query("""
           SELECT new br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.AgregadoStatus(
               p.status, COUNT(p), COALESCE(SUM(p.valor), 0))
           FROM PedidoPagamentoEntity p
           WHERE p.requisitanteId = :requisitanteId
             AND p.dataPedido >= :inicioMes
             AND p.dataPedido <= :fimMes
           GROUP BY p.status
           """)
    List<AgregadoStatus> agregarPorStatusNoIntervalo(
            @Param("requisitanteId") Long requisitanteId,
            @Param("inicioMes") LocalDate inicioMes,
            @Param("fimMes") LocalDate fimMes);
}
