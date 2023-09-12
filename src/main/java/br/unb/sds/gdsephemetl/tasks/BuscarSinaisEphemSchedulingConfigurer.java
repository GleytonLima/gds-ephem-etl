package br.unb.sds.gdsephemetl.tasks;

import br.unb.sds.gdsephemetl.application.BuscarSinaisEphemWork;
import br.unb.sds.gdsephemetl.application.ConfiguracaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BuscarSinaisEphemSchedulingConfigurer implements SchedulingConfigurer {
    public static final int INTERVALO_EM_SEGUNDOS_DEFAULT = 360;
    private final ConfiguracaoRepository configuracaoRepository;
    private final BuscarSinaisEphemWork work;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());

        taskRegistrar.addTriggerTask(
                work::processar,
                triggerContext -> {
                    final var nextExecutionTime = new GregorianCalendar();
                    final var lastActualExecutionTime = triggerContext.lastActualExecutionTime();
                    nextExecutionTime.setTime(lastActualExecutionTime != null ? lastActualExecutionTime : new Date());
                    try {
                        final var configuracao = configuracaoRepository.findById(1L);

                        var intervaloEmSegundos = INTERVALO_EM_SEGUNDOS_DEFAULT;
                        if (configuracao.isPresent()) {
                            intervaloEmSegundos = configuracao.get().getIntervaloEtlEmSegundos();
                        }
                        nextExecutionTime.add(Calendar.SECOND, intervaloEmSegundos);
                        return nextExecutionTime.getTime();
                    } catch (Exception e) {
                        log.error("Não foi possível buscar o intervalo de execução da ETL. Usando intervalo padrão de {} segundos.", INTERVALO_EM_SEGUNDOS_DEFAULT, e);
                        nextExecutionTime.add(Calendar.SECOND, INTERVALO_EM_SEGUNDOS_DEFAULT);
                        return nextExecutionTime.getTime();
                    }
                }
        );
    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskExecutor() {
        return Executors.newScheduledThreadPool(100);
    }
}
