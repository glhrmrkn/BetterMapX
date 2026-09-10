# BetterMapX 0.3.0-alpha

Mapa independente para **Minecraft Java 1.21.1 / Fabric / Java 21**, com minimapa, mapa aberto, radar de Pokémon, waypoints, camadas de altura e overlays.

## Destaques da 0.3.0-alpha

### Spawn Areas

Nova camada opcional em **Ajustes > Spawn Areas**:

- ON/OFF global;
- exibição separada no mapa cheio e no minimapa;
- busca de Pokémon a partir dos `spawn_pool_world` ativos;
- apenas um Pokémon selecionado por vez;
- botão para remover a seleção;
- área válida em rosa/roxo translúcido com contorno vermelho;
- cache de máscara por tile para não recalcular por frame;
- usa bioma, dimensão e faixa de Y presentes nas regras de spawn;
- o Atlas agora persiste o bioma de cada coluna junto com cor e altura, mantendo o overlay útil em chunks já mapeados.

**Importante:** esta versão mostra a **geografia estaticamente elegível** para spawn. Condições dinâmicas como horário, clima, luz, estruturas, blocos próximos e estado momentâneo do servidor não são inventadas pelo mapa. Em singleplayer/integrated server os datapacks ativos podem ser lidos diretamente. Em servidores remotos, regras que não são enviadas ao cliente podem ficar indisponíveis.

### Premium Pokémon Glow

O glow foi refeito em vez de usar retângulos rígidos:

| Categoria | Identidade |
| --- | --- |
| Rare | ciano/azul intenso |
| Ultra Rare | violeta elétrico |
| Legendary | dourado/âmbar forte |
| Mythical | magenta/rosa cósmico |
| Shiny | ciano-branco prismático, independente da raridade |

- aura radial suave de 64×64, sem ring rígido;
- 24 frames pré-gerados por estilo;
- breathing contínuo com fases diferentes por UUID;
- Ultra Rare usa bloom em múltiplas camadas;
- Legendary/Mythical usam bloom ainda maior + pontos luminosos animados;
- Shiny adiciona uma aura própria sem substituir a raridade base;
- intensidade configurável;
- animação de entrada/saída por spring;
- posição visual dos marcadores é suavizada entre updates para reduzir movimento “quadrado”.

Um Legendary Shiny, por exemplo, mantém o dourado de Legendary e recebe também a assinatura Shiny.

### UI / movimento

- pan e zoom do mapa aberto usam smoothing em vez de saltos secos;
- a abertura do mapa cheio não escala mais um retângulo inteiro como uma folha de papel;
- botões possuem hover contínuo e micro-acento luminoso;
- o blur vanilla da tela do Minecraft continua desativado no mapa aberto.

## Instalação

1. Minecraft 1.21.1.
2. Fabric Loader 0.16.10 ou superior.
3. Fabric API 0.116.1+1.21.1 ou superior compatível.
4. Java 21.
5. Remova versões antigas do BetterMapX da pasta `mods`.
6. Copie `bettermapx-0.3.0-alpha.jar` para `mods`.

O mod é client-side. Cobblemon é necessário para as funções de Pokémon. Spawn Areas depende dos dados de spawn que estejam acessíveis no lado onde o BetterMapX está rodando.

## Controles

Os atalhos são configuráveis em **Opções > Controles > BetterMapX**. O mapa aberto suporta arraste, scroll para zoom, PageUp/PageDown para camadas e clique direito para waypoints.

## Configuração

Arquivo:

```text
config/bettermapx.properties
```

Opções novas relevantes:

```properties
animatedGlow=true
glowRare=true
glowUltraRare=true
glowLegendary=true
glowMythical=true
glowShiny=true
glowIntensity=1.18
spawnAreas=false
spawnAreasFullMap=true
spawnAreasMinimap=false
spawnAreaPokemon=
```

## Dados do mapa

Cada chunk continua usando 16×16 texels: **1 bloco = 1 texel**. O tile persistido agora contém:

- cor do terreno;
- altura;
- biome ID por coluna.

O codec foi atualizado para v2, mas mantém leitura dos tiles v1. Tiles antigos sem biome são atualizados quando o chunk é amostrado novamente.

## Integração externa

A API `dev.bettermapx.OverlayApi` permanece disponível para módulos como PokeHunterX. Spawn Areas é uma camada nativa separada; não remove a possibilidade de providers externos.

## Compilar

Com JDK 21:

```powershell
.\gradlew.bat clean build
```

Linux/macOS:

```bash
./gradlew clean build
```

JAR esperado:

```text
build/libs/bettermapx-0.3.0-alpha.jar
```

Versões do projeto: Gradle 8.10.2, Loom 1.8.13, Yarn 1.21.1+build.3 e Fabric API 0.116.1+1.21.1.

Consulte `VALIDATION.md` para saber exatamente o que foi e não foi confirmado nesta entrega.

## 0.3.1-alpha interaction cleanup

The full map now keeps Pokemon markers visually clean: marker icons are always visible, but Pokemon text/details are hover-only. The player marker uses a cyan/dark high-contrast locator. Rarity/Shiny glows use compact fixed-size animated light fields rather than expanding circles. Right-click map coordinates can invoke Teleportar; the server remains responsible for command permission enforcement.
