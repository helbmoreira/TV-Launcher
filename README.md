# TV Launcher — Liquid Glass edition

Esta versão atualiza o visual e adiciona controle local dos aplicativos exibidos.

## Alterações
- Visual inspirado em Liquid Glass: cartões translúcidos, contornos claros e fundo azul-escuro.
- Animação de foco/ampliação para navegação por controle remoto.
- Pressione e segure um aplicativo para ocultá-lo da grade ou abrir as informações do app.
- Novo botão de grade no canto superior direito para gerenciar aplicativos visíveis. Desmarque um app para ocultá-lo; marque para mostrá-lo novamente.
- Aplicativos ocultos continuam instalados. A preferência é salva localmente no dispositivo.

## Compilar
Abra o projeto no Android Studio ou execute no ambiente com Gradle e Android SDK:
`gradle assembleDebug`

O APK debug será gerado em:
`app/build/outputs/apk/debug/app-debug.apk`

## Instalar por ADB
`adb install -r app/build/outputs/apk/debug/app-debug.apk`

A instalação atual usa o mesmo applicationId (`com.example.tvlauncher`). Se o APK anterior tiver a mesma assinatura, `-r` atualiza sem apagar os dados. Se a assinatura for diferente, o Android pode exigir desinstalação antes; isso pode apagar preferências locais.
