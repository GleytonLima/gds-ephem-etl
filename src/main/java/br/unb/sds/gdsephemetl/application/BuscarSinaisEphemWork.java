package br.unb.sds.gdsephemetl.application;

import br.unb.sds.gdsephemetl.application.model.SignalApiResponse;
import br.unb.sds.gdsephemetl.application.model.Sinal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuscarSinaisEphemWork {
    public static final long ID_DEFAULT = 1L;
    private final SinalRepository signalRepository;
    private final ConfiguracaoRepository configuracaoRepository;

    private final RestTemplate restTemplate;

    @Transactional
    public void processar() {
        signalRepository.deleteAll();

        int page = 0;
        int size = 100;
        boolean hasMoreSignals = true;

        final var configuracao = configuracaoRepository.findById(ID_DEFAULT);
        if (configuracao.isEmpty()) {
            log.error("URL do banco de dados remoto não configurada.");
            return;
        }
        while (hasMoreSignals) {
            try {
                final var urlRemoteDb = configuracao.get().getDominioRemoto();
                final var url = urlRemoteDb + "/api-integracao/v1/signals?page=" + page + "&size=" + size;
                log.info("Iniciando requisicao em: {}", url);
                log.info("Buscando sinais da página {}...", page);
                final var response = restTemplate.getForObject(url, SignalApiResponse.class);
                log.info("Buscando sinais da página {} finalizada", page);
                if (response != null && response.getEmbedded() != null && !response.getEmbedded().getSignals().isEmpty()) {
                    for (SignalApiResponse.SignalData signalData : response.getEmbedded().getSignals()) {
                        final var sinal = new Sinal();
                        sinal.setSignalId(signalData.getSignalId());
                        sinal.setDados(signalData.getDados());
                        signalRepository.save(sinal);
                    }
                    log.info("{} sinais salvos com sucesso.", response.getEmbedded().getSignals().size());
                    page++;
                } else {
                    log.info("Dados retornados vazios. Encerrando busca.");
                    hasMoreSignals = false;
                }
            } catch (Exception e) {
                log.error("Erro ao buscar sinais da página " + page + ".", e);
                throw e;
            }
        }
        log.info("Busca de sinais finalizada.");
    }
}
