package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.PedidoPagamentoEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Query("""
           SELECT new br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.AgregadoStatus(
               p.status, COUNT(p), COALESCE(SUM(p.valor), 0))
           FROM PedidoPagamentoEntity p
           WHERE p.requisitanteId = :requisitanteId
             AND p.dataPedido >= :inicioMes
             AND p.dataPedido <= :fimMes
             AND LOWER(p.descricao) LIKE :buscaPattern
           GROUP BY p.status
           """)
    List<AgregadoStatus> agregarPorStatusNoIntervaloComBusca(
            @Param("requisitanteId") Long requisitanteId,
            @Param("inicioMes") LocalDate inicioMes,
            @Param("fimMes") LocalDate fimMes,
            @Param("buscaPattern") String buscaPattern);

    // ── Métodos V6: folha de pagamento ────────────────────────────────────────

    @Query("""
           SELECT p FROM PedidoPagamentoEntity p
           WHERE p.funcionarioId = :funcionarioId
             AND p.categoria = br.com.satyan.stering.saita.financasbottelegram.domain.enums.CategoriaPedido.VALE
             AND p.fechado = false
             AND p.dataPedido >= :inicio
             AND p.dataPedido <= :fim
           """)
    List<PedidoPagamentoEntity> findValesAbertos(
            @Param("funcionarioId") Long funcionarioId,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    boolean existsByFuncionarioIdAndMesReferencia(Long funcionarioId, LocalDate mesReferencia);

    @Modifying
    @Transactional
    @Query("UPDATE PedidoPagamentoEntity p SET p.fechado = true WHERE p.id IN :ids")
    void markAllClosed(@Param("ids") List<Long> ids);

    @Query("""
           SELECT p FROM PedidoPagamentoEntity p
           WHERE p.funcionarioId = :funcionarioId
             AND p.categoria = br.com.satyan.stering.saita.financasbottelegram.domain.enums.CategoriaPedido.FOLHA
           ORDER BY p.mesReferencia DESC
           """)
    List<PedidoPagamentoEntity> findFolhasByFuncionario(@Param("funcionarioId") Long funcionarioId);

    @Query("""
           SELECT p FROM PedidoPagamentoEntity p
           WHERE p.funcionarioId = :funcionarioId
             AND p.categoria = br.com.satyan.stering.saita.financasbottelegram.domain.enums.CategoriaPedido.VALE
             AND p.dataPedido >= :inicio
             AND p.dataPedido <= :fim
           ORDER BY p.dataPedido DESC
           """)
    List<PedidoPagamentoEntity> findValesByFuncionarioAndPeriodo(
            @Param("funcionarioId") Long funcionarioId,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);
}
