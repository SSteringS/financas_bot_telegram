package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.ComprovanteEntity;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ComprovanteJpaRepository extends JpaRepository<ComprovanteEntity, Long> {

    @Query("SELECT c.pedido.id FROM ComprovanteEntity c WHERE c.pedido.id IN :ids")
    Set<Long> findPedidoIdsByPedidoIdIn(@Param("ids") List<Long> ids);

    boolean existsByPedidoId(Long pedidoId);
}
