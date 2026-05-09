# Copilot Instructions

## Progetto

**discord-taunt-bot** è un bot Discord scritto in **Java 21** con il framework **Quarkus 3.x**.
Riproduce audio taunt nei canali vocali Discord e gestisce eventi KotH (King of the Hill).

## Stack tecnologico

- **Java 21** — usa `var`, switch expressions, pattern matching dove appropriato
- **Quarkus 3.x** — CDI, MicroProfile Config, scheduling
- **JDA 5.x** — Discord Java API
- **LavaPlayer** — riproduzione audio
- **Concentus** — codec Opus per registrazione audio

## Package structure

```
it.simonecelia.discordtauntbot
├── config/       → AppConfig (configurazione app + segreti da secret.properties)
├── enums/        → enum del dominio (es. KothTimesEnum)
├── manager/      → GuildMusicManager
├── scheduler/    → KoTHScheduler (Quarkus @Scheduled)
└── service/
    ├── audio/    → AudioPlayerService, AudioPlayerSendHandler, TrackScheduler
    │   ├── recorder/ → AudioReceiveHandler, AudioRecorderRingBufferService
    │   └── tts/      → TTSSenderService
    ├── business/ → DiscordTauntBot (entry point eventi JDA), DiscordTauntBotBaseLogger
    └── text/     → TextSenderService
```

## Convenzioni di codice

- **DI**: `@ApplicationScoped` + `@Inject` (CDI standard)
- **Logging**: `io.quarkus.logging.Log` (statico), con `Log.infof(...)`, `Log.error(...)`
- **Config**: `@ConfigProperty` per le proprietà da `application.properties`; i segreti vengono letti da `secret.properties` a runtime in `AppConfig.onStartup()`
- **Stile**: spazi intorno alle parentesi nelle chiamate a metodo, es. `method ( arg )` — rispetta lo stile esistente
- **Locale variable**: preferire `var` quando il tipo è ovvio dal contesto

## Build & Run

```bash
# build
mvn clean install package
# oppure
./mvnw clean install package

# run
java -jar target/quarkus-app/quarkus-run.jar

# dev mode (live reload)
./mvnw quarkus:dev -Dquarkus.banner.enabled=true -Dquarkus.analytics.disabled=true -Dquarkus.test.continuous-testing=disabled
```

## Configurazione

- `src/main/resources/application.properties` — proprietà pubbliche
- `src/main/resources/secret.properties` — token e credenziali (NON committare)
- Chiavi principali: `admin.id`, `channel.id`, `voice.channel.id`, `guild.id`, `koth.enabled`, `verbose`

## Note importanti

- `secret.properties` è escluso da git (contiene `discord.bot.token` e credenziali JDownloader)
- Il bot risponde ai messaggi Discord tramite `DiscordTauntBot.onMessageReceived()` con uno switch expression
- I comandi iniziano con `/` (es. `/tts`, `/stop`, `/tauntlist`, `/koth`, `/kill`)
- `/kill` e `/verbose` sono riservati all'admin (verificato tramite `admin.id`)
