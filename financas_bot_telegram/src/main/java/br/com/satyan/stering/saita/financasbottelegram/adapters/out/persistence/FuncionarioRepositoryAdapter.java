package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.entity.FuncionarioEntity;
import br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence.mapper.FuncionarioMapper;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.FuncionarioRepositoryPortOut;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Funcionario;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter de persistência para {@link Funcionario}.
 *
 * <p>Implementa {@link FuncionarioRepositoryPortOut} (port de saída) usando Spring Data JPA.
 * Única classe que conhece {@link FuncionarioJpaRepository} — domínio e use cases
 * dependem apenas do port (DIP).
 */
@Component
public class FuncionarioRepositoryAdapter implements FuncionarioRepositoryPortOut {

    private final FuncionarioJpaRepository jpaRepository;
    private final FuncionarioMapper mapper;

    public FuncionarioRepositoryAdapter(
            FuncionarioJpaRepository jpaRepository,
            FuncionarioMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Funcionario save(Funcionario funcionario) {
        FuncionarioEntity entity = mapper.toEntity(funcionario);
        FuncionarioEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Funcionario> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Funcionario> findAtivoById(Long id) {
        return jpaRepository.findByIdAndAtivoTrue(id).map(mapper::toDomain);
    }

    @Override
    public List<Funcionario> findAllAtivos() {
        return jpaRepository.findAllByAtivoTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        // Soft delete: set ativo=false
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setAtivo(false);
            jpaRepository.save(entity);
        });
    }
}
