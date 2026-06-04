package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.AdiantamentoEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.AdiantamentoMapper;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.AdiantamentoRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Adiantamento;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter de persistência para {@link Adiantamento}.
 *
 * <p>Implementa {@link AdiantamentoRepositoryPortOut} (port de saída) usando Spring Data JPA.
 * Única classe que conhece {@link AdiantamentoJpaRepository} — domínio e use cases
 * dependem apenas do port (DIP).
 */
@Component
public class AdiantamentoRepositoryAdapter implements AdiantamentoRepositoryPortOut {

    private final AdiantamentoJpaRepository jpaRepository;
    private final AdiantamentoMapper mapper;

    public AdiantamentoRepositoryAdapter(
            AdiantamentoJpaRepository jpaRepository,
            AdiantamentoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Adiantamento save(Adiantamento adiantamento) {
        AdiantamentoEntity entity = mapper.toEntity(adiantamento);
        AdiantamentoEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Adiantamento> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Adiantamento> findAtivosParaFuncionario(Long funcionarioId) {
        return jpaRepository.findByFuncionarioIdAndAtivoTrue(funcionarioId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Adiantamento> findAtivosParaFechamento(Long funcionarioId) {
        // Retorna todos os adiantamentos ativos — o FecharMesUseCase filtra por parcelas restantes
        return jpaRepository.findByFuncionarioIdAndAtivoTrue(funcionarioId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
