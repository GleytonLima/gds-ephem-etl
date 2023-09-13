package br.unb.sds.gdsephemetl.application;

import br.unb.sds.gdsephemetl.application.model.Configuracao;
import br.unb.sds.gdsephemetl.application.model.SignalApiResponse;
import br.unb.sds.gdsephemetl.application.model.Sinal;
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
class BuscarSinaisEphemWorkTest {

    @Mock
    private SinalRepository signalRepository;

    @Mock
    private ConfiguracaoRepository configuracaoRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private BuscarSinaisEphemWork buscarSinaisEphemWork;

    @Test
    void testProcessarWithEmptyConfiguracao() {
        when(configuracaoRepository.findById(BuscarSinaisEphemWork.ID_DEFAULT)).thenReturn(Optional.empty());

        buscarSinaisEphemWork.processar();

        verify(signalRepository, times(1)).deleteAll();
        verify(restTemplate, never()).getForObject(anyString(), any());
    }

    @Test
    void testProcessarWithApiResponse() {
        final var configuracao = new Configuracao();
        configuracao.setUrlDbRemoto("http://example.com/api/sinais");
        when(configuracaoRepository.findById(BuscarSinaisEphemWork.ID_DEFAULT)).thenReturn(Optional.of(configuracao));

        final var signalApiResponse = new SignalApiResponse();
        signalApiResponse.setEmbedded(new SignalApiResponse.Embedded());
        final var signals = new ArrayList<SignalApiResponse.SignalData>();
        final var signalData = new SignalApiResponse.SignalData();
        signalData.setSignalId(1L);
        signalData.setDados(new IntNode(1));
        signals.add(signalData);
        signalApiResponse.getEmbedded().setSignals(signals);
        when(restTemplate.getForObject("http://example.com/api/sinais?page=0&size=100", SignalApiResponse.class))
                .thenReturn(signalApiResponse);

        buscarSinaisEphemWork.processar();

        verify(signalRepository).deleteAll();
        verify(signalRepository).save(any(Sinal.class));
    }

    @Test
    void testProcessarWithEmptyApiResponse() {
        final var configuracao = new Configuracao();
        configuracao.setUrlDbRemoto("http://example.com/api/sinais");
        when(configuracaoRepository.findById(BuscarSinaisEphemWork.ID_DEFAULT)).thenReturn(Optional.of(configuracao));

        final var signalApiResponse = new SignalApiResponse();
        when(restTemplate.getForObject("http://example.com/api/sinais?page=0&size=100", SignalApiResponse.class))
                .thenReturn(signalApiResponse);

        buscarSinaisEphemWork.processar();
        verify(signalRepository, times(1)).deleteAll();
    }

    @Test
    void testProcessarWithException() {
        final var configuracao = new Configuracao();
        configuracao.setUrlDbRemoto("http://example.com/api/sinais");
        when(configuracaoRepository.findById(BuscarSinaisEphemWork.ID_DEFAULT)).thenReturn(Optional.of(configuracao));

        when(restTemplate.getForObject("http://example.com/api/sinais?page=0&size=100", SignalApiResponse.class))
                .thenThrow(new RuntimeException("Erro na chamada da API"));

        assertThrows(RuntimeException.class, () -> buscarSinaisEphemWork.processar());
        verify(signalRepository, never()).save(any(Sinal.class));
    }
}