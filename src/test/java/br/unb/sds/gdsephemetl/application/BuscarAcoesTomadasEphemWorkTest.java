package br.unb.sds.gdsephemetl.application;

import br.unb.sds.gdsephemetl.application.model.AcaoTomada;
import br.unb.sds.gdsephemetl.application.model.Configuracao;
import br.unb.sds.gdsephemetl.application.model.ModelApiResponse;
import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuscarAcoesTomadasEphemWorkTest {

    @Mock
    private AcaoTomadaRepository repository;

    @Mock
    private ConfiguracaoRepository configuracaoRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private BuscarAcoesTomadasEphemWork work;

    @Test
    void testProcessarWithEmptyConfiguracao() {
        when(configuracaoRepository.findById(BuscarSinaisEphemWork.ID_DEFAULT)).thenReturn(Optional.empty());

        work.processar();

        verify(repository, times(1)).deleteAll();
        verify(restTemplate, never()).getForObject(anyString(), any());
    }

    @Test
    void testProcessarWithApiResponse() {
        final var configuracao = new Configuracao();
        configuracao.setDominioRemoto("http://example.com");
        when(configuracaoRepository.findById(BuscarSinaisEphemWork.ID_DEFAULT)).thenReturn(Optional.of(configuracao));

        final var ModelApiResponse = new ModelApiResponse();
        ModelApiResponse.setEmbedded(new ModelApiResponse.Embedded());
        final var models = new ArrayList<ModelApiResponse.ModelData>();
        final var modelData = new ModelApiResponse.ModelData();
        modelData.setAttributes(new IntNode(1));
        models.add(modelData);
        ModelApiResponse.getEmbedded().setModels(models);
        when(restTemplate.getForObject("http://example.com/api-integracao/v1/models/eoc.signal.actions.taken?page=0&size=100", ModelApiResponse.class))
                .thenReturn(ModelApiResponse);

        work.processar();

        verify(repository).deleteAll();
        verify(repository).save(any(AcaoTomada.class));
    }

    @Test
    void testProcessarWithEmptyApiResponse() {
        final var configuracao = new Configuracao();
        configuracao.setDominioRemoto("http://example.com");
        when(configuracaoRepository.findById(BuscarSinaisEphemWork.ID_DEFAULT)).thenReturn(Optional.of(configuracao));

        final var ModelApiResponse = new ModelApiResponse();
        when(restTemplate.getForObject("http://example.com/api-integracao/v1/models/eoc.signal.actions.taken?page=0&size=100", ModelApiResponse.class))
                .thenReturn(ModelApiResponse);

        work.processar();
        verify(repository, times(1)).deleteAll();
    }

    @Test
    void testProcessarWithException() {
        final var configuracao = new Configuracao();
        configuracao.setDominioRemoto("http://example.com");
        when(configuracaoRepository.findById(BuscarSinaisEphemWork.ID_DEFAULT)).thenReturn(Optional.of(configuracao));

        when(restTemplate.getForObject("http://example.com/api-integracao/v1/models/eoc.signal.actions.taken?page=0&size=100", ModelApiResponse.class))
                .thenThrow(new RuntimeException("Erro na chamada da API"));

        assertThrows(RuntimeException.class, () -> work.processar());
        verify(repository, never()).save(any(AcaoTomada.class));
    }
}