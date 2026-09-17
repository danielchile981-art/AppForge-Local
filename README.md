# S25 Preset Cam

Aplicativo Android gratuito com câmera própria e presets de fotos e vídeos criados para o Galaxy S25 Ultra.

O app inclui ícone próprio, capa de abertura animada, abas de Fotos e Vídeos, entrada suave dos presets e animações de captura e gravação.

## Presets de foto

- Câmera padrão: dia a dia, paisagem ampla, retrato, assunto distante e selfie
- Foto Pro: RAW coringa, movimento, paisagem 50 MP, noite à mão, noite com tripé, Lua e astrofotografia
- A resolução, a lente e os controles manuais são ajustados para a opção compatível mais próxima exposta pela API Camera2
- As fotos são salvas em `Galeria > Pictures > S25 Preset Cam`

## Presets incluídos

- Dia a dia: 4K 30 fps, 1x, automático
- Cinema: 4K 24 fps, ISO 100, 1/50, 5600 K
- Movimento: 4K 60 fps, ISO 200, 1/120, 5600 K
- Noite: 4K 30 fps, ISO 800, 1/60, 4200 K
- Paisagem ampla: 4K 30 fps, 0,6x
- Retrato / Produto: 4K 30 fps, 2x, ISO 200, 1/60
- Show perto: 4K 30 fps, 3x, ISO 800, 1/60
- Show longe: 4K 30 fps, 5x, ISO 800, 1/60
- Entrevista: 4K 30 fps, 2x, ISO 400, 1/60
- Câmera lenta: FHD 120 fps, ISO 200, 1/240
- Vlog frontal: 4K 30 fps

## Funcionamento

O aplicativo usa a API Camera2 do Android. Antes de gravar, consulta as capacidades expostas pelo aparelho. Quando uma combinação de lente, resolução ou FPS não estiver disponível, escolhe a alternativa compatível mais próxima e mostra o ajuste na tela.

As gravações ficam em `Galeria > Movies > S25 Preset Cam`.

## Limitações

- Não utiliza o processamento proprietário dos aplicativos Câmera Samsung e Expert RAW.
- A troca entre sensores físicos depende do que o firmware disponibiliza à API Camera2.
- Samsung Log, Vídeo Retrato, Superestável e Hyperlapse continuam exclusivos do aplicativo Samsung.
- A primeira versão precisa ser testada no aparelho real para validar todas as combinações do S25 Ultra.

## APK

Depois que o build terminar, baixe `release/S25-Preset-Cam-v1.1.apk`. O mesmo arquivo também fica disponível na aba **Actions**, no artefato `S25-Preset-Cam-APK`.

## Compilação

Requisitos: Java 17, Android SDK 35 e Gradle 8.11.1.

```bash
gradle :app:assembleDebug
```

O APK será criado em `app/build/outputs/apk/debug/app-debug.apk`.
