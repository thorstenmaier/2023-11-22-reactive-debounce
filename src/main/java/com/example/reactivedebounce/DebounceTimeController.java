package com.example.reactivedebounce;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@RestController
public class DebounceTimeController {

    private Function<Flux<AlarmUpdate>, Flux<AlarmUpdate>> debounceStrategy = USE_FIRST_EVENT_AND_THEN_FILTER;

    private static final Function<Flux<AlarmUpdate>, Flux<AlarmUpdate>> PAUSE_BETWEEN_ELEMENTS = flux -> flux.delayElements(Duration.ofSeconds(5));
    private static final Function<Flux<AlarmUpdate>, Flux<AlarmUpdate>> WAIT_UNTIL_NOTHING_CHANGES = flux -> flux.sampleTimeout(item -> Mono.delay(Duration.ofSeconds(5)));
    private static final Function<Flux<AlarmUpdate>, Flux<AlarmUpdate>> USE_FIRST_EVENT_AND_THEN_FILTER = flux -> {
      AtomicLong lastEventTs = new AtomicLong();
      return flux.filter(alarmUpdate -> {
          long now = System.currentTimeMillis();
          long oldTs = lastEventTs.getAndSet(now);
          return now - oldTs > 5000;
      });
    };

    private Map<Long, Sinks.Many<AlarmUpdate>> alarmUpdates = new HashMap<>();

    @GetMapping("/alarmUpdate")
    public void alarmUpdate(@RequestParam("routerId") Long routerId) {
        Sinks.Many<AlarmUpdate> alarmUpdate = getAlarmUpdateForRouter(routerId);
        alarmUpdate.tryEmitNext(new AlarmUpdate(routerId, new Date()));
    }

    private Sinks.Many<AlarmUpdate> getAlarmUpdateForRouter(Long routerId) {
        Sinks.Many<AlarmUpdate> alarmUpdate = alarmUpdates.get(routerId);
        if (alarmUpdate == null) {
            alarmUpdate = Sinks.many().multicast().onBackpressureBuffer();
            debounceStrategy.apply(alarmUpdate.asFlux()).subscribe(this::startProcess);
            alarmUpdates.put(routerId, alarmUpdate);
        }
        return alarmUpdate;
    }

    public void startProcess(AlarmUpdate alarmUpdate) {
        System.out.println(alarmUpdate);
    }

    record AlarmUpdate(Long routerId, Date ts) {
    }
}
