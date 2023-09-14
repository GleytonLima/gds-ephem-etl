package br.unb.sds.gdsephemetl.tasks;

import br.unb.sds.gdsephemetl.application.BuscarAcoesTomadasEphemWork;
import br.unb.sds.gdsephemetl.application.BuscarFontesEphemWork;
import br.unb.sds.gdsephemetl.application.BuscarRecomendacaoTecnicaEphemWork;
import br.unb.sds.gdsephemetl.application.BuscarSinaisEphemWork;
import br.unb.sds.gdsephemetl.application.model.Configuracao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BuscarSinaisEphemSchedulingConfigurerTest {
    @Mock
    private BuscarSinaisEphemWork buscarSinaisEphemWork;
    @Mock
    private BuscarFontesEphemWork buscarFontesEphemWork;
    @Mock
    private BuscarRecomendacaoTecnicaEphemWork buscarRecomendacaoTecnicaEphemWork;
    @Mock
    private BuscarAcoesTomadasEphemWork buscarAcoesTomadasEphemWork;
    @InjectMocks
    private BuscarSinaisEphemSchedulingConfigurer schedulingConfigurer;


    @Test
    void testConfigureTasks() {
        final var configuracao = new Configuracao();
        configuracao.setIntervaloEtlEmSegundos(1);
        ScheduledTaskRegistrar taskRegistrar = new ScheduledTaskRegistrar();
        schedulingConfigurer.configureTasks(taskRegistrar);

        taskRegistrar.getTriggerTaskList().get(0).getRunnable().run();
        taskRegistrar.getTriggerTaskList().get(1).getRunnable().run();
        taskRegistrar.getTriggerTaskList().get(2).getRunnable().run();
        taskRegistrar.getTriggerTaskList().get(3).getRunnable().run();

        verify(buscarSinaisEphemWork, times(1)).processar();
        verify(buscarFontesEphemWork, times(1)).processar();
        verify(buscarAcoesTomadasEphemWork, times(1)).processar();
        verify(buscarRecomendacaoTecnicaEphemWork, times(1)).processar();
    }
}