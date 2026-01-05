package br.com.satyan.stering.saita.financasbottelegram.adapters.out.persistence;

import br.com.satyan.stering.saita.financasbottelegram.application.port.out.ComprovanteRepositoryPort;
import br.com.satyan.stering.saita.financasbottelegram.domain.entity.Comprovante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public class ComprovanteRepositoryAdapter implements ComprovanteRepositoryPort {

    private final ComprovanteJpaRepository jpaRepository;

    public ComprovanteRepositoryAdapter(ComprovanteJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Comprovante save(Comprovante comprovante) {
        return jpaRepository.save(comprovante);
    }
}

