# Media Car — player de vídeo para multimídia (Android 6.0 ou mais novo)

## O que tem

- **Pastas grandes:** lista de pastas e vídeos com itens enormes. O botão "Tamanho" troca entre Médio, Grande e Enorme.
  Mostra a memória interna e os pendrives que o Android deixar o app ler.
- **Player:** toque na tela para mostrar/esconder os controles. Toque duplo na esquerda volta 10 s, na direita avança 10 s.
  Os vídeos seguintes da mesma pasta tocam em sequência. Botão para ajustar ou esticar a imagem.
- **Equalizador de 15 bandas** (25 Hz a 16 kHz, de -12 a +12 dB), com pré-amp e 8 predefinições.
  É um equalizador por software, então funciona com qualquer número de bandas, independente do aparelho.
  Toque duas vezes numa banda para zerá-la.
- **Retoma sozinho:** ao abrir o app, se o último vídeo estava tocando, ele volta a tocar de onde parou.
  Se o carro acabou de ligar e o pendrive ainda não está pronto, o app espera até ~2 minutos.
  O progresso é gravado a cada 3 segundos, então vale mesmo se o carro desligar de repente.
  Se você pausou antes de sair, ele abre na lista de pastas com o botão **Continuar**.
- Visual escuro com vidro translúcido, inspirado no iOS. A tela fica sempre ligada e em tela cheia, na horizontal.

## Limites que você precisa saber

- **Não foi testado em carro nem em multimídia.** O código foi escrito sem poder compilar nem rodar.
- **Formatos:** MP4, MKV, MOV, WebM, TS, M4V, 3GP e FLV costumam funcionar. **AVI e WMV podem não abrir**,
  e MKV com áudio DTS/AC3 pode ficar sem som, dependendo dos decodificadores do aparelho.
  Quando o vídeo não abre, o app avisa na tela.
- **Pendrive:** o Android da multimídia precisa liberar a leitura dele para o app. Se ele não aparecer na lista,
  toque em "Atualizar lista" depois de espetar o pendrive. Alguns aparelhos mostram o pendrive apenas em
  caminhos que o app não consegue ler.
- O app **pausa** quando você sai para outro app (rádio, navegação) e volta a tocar quando você volta,
  se estava tocando.

## Arquivos

- `MainActivity.kt` — tela cheia, permissão de arquivos, pausa/retoma ao sair/voltar
- `App.kt` — telas e retomada automática do último vídeo
- `BrowserScreen.kt` — lista de pastas grande
- `PlayerScreen.kt` — vídeo e controles
- `EqualizerScreen.kt` / `EqProcessor.kt` — equalizador de 15 bandas
- `PlayerController.kt` — player (ExoPlayer/Media3) e lista da pasta
- `Files.kt`, `Prefs.kt`, `Ui.kt` — arquivos, memória do app e componentes visuais

Passo a passo para gerar o APK: veja `GITHUB.md`.
