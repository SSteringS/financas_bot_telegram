package br.com.satyan.stering.saita.financasbottelegram.adapter.in.telegram.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.com.satyan.stering.saita.financasbottelegram.adapters.in.telegram.service.MediaGroupInMemoryService;
import br.com.satyan.stering.saita.financasbottelegram.domain.model.TelegramMediaGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MediaGroupInMemoryServiceTest {

  private MediaGroupInMemoryService service;

  @BeforeEach
  void setUp() {
    service = new MediaGroupInMemoryService();
  }

  @Test
  @DisplayName("Deve adicionar novo grupo e preencher fileIdPedido")
  void deveAdicionarNovoGrupo() {
    TelegramMediaGroup group = service.addAndGetGroup("group1", "filePedido", "cat", "origem");
    assertEquals("filePedido", group.getFileIdPedido());
    assertEquals("cat", group.getCategoria());
    assertEquals("origem", group.getOrigem());
    assertNull(group.getFileIdComprovante());
  }

  @Test
  @DisplayName("Deve atualizar grupo existente e preencher fileIdComprovante")
  void deveAtualizarGrupoExistente() {
    service.addAndGetGroup("group1", "filePedido", "cat", "origem");
    TelegramMediaGroup group = service.addAndGetGroup("group1", "fileComprovante", null, null);
    assertEquals("filePedido", group.getFileIdPedido());
    assertEquals("fileComprovante", group.getFileIdComprovante());
    assertEquals("cat", group.getCategoria());
    assertEquals("origem", group.getOrigem());
  }

  @Test
  @DisplayName("Deve remover grupo da memória")
  void deveRemoverGrupo() {
    service.addAndGetGroup("group1", "filePedido", "cat", "origem");
    service.removeGroup("group1");
    // Não há método para buscar diretamente, mas podemos adicionar novamente e verificar se é novo
    TelegramMediaGroup group = service.addAndGetGroup("group1", "novoFile", "novaCat", "novaOrigem");
    assertEquals("novoFile", group.getFileIdPedido());
    assertEquals("novaCat", group.getCategoria());
    assertEquals("novaOrigem", group.getOrigem());
    assertNull(group.getFileIdComprovante());
  }
}