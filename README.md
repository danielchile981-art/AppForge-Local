# AppForge Local — MVP

Gerador de projetos Android baseado em HTML/CSS/JavaScript + Capacitor.

## Recursos
- recebe uma descrição do aplicativo;
- gera os arquivos do projeto;
- integração com GitHub;
- GitHub Actions para compilar APK Android;
- funcionamento sem API de IA paga nesta versão.

## APK do AppForge
A compilação é feita automaticamente pelo workflow **Build AppForge APK**. Quando terminar, o APK debug fica disponível nos artefatos da execução do GitHub Actions.

## Observação
Esta versão usa um interpretador local baseado em regras/modelos. Uma evolução futura pode integrar um LLM local e OAuth Device Flow do GitHub.
