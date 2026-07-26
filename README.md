# 2Fast (Android)

Autenticador TOTP local para Android. Os segredos ficam em um arquivo `.2fa` criptografado que você controla — sem conta, sem nuvem obrigatória e sem depender de terceiros.

Compatível com o formato de arquivo do [2fast](https://github.com/2fast-team/2fast) (desktop/Uno).

## Recursos

- Arquivo único `.2fa` criptografado (AES + PBKDF2)
- Criar, abrir e gerenciar vários arquivos
- Códigos TOTP (RFC 6238) com SHA-1, SHA-256 e SHA-512
- Importar contas via URI `otpauth://` ou leitura de QR code
- Toque para copiar o código
- Opção de ocultar códigos na tela
- Bloqueio da sessão
- Idiomas: português, inglês, espanhol, chinês (simplificado) e russo
- Funcionamento offline

## Requisitos

- Android 8.0 (API 26) ou superior
- Câmera (opcional) para escanear QR codes

## Como usar

1. Crie um novo arquivo `.2fa` ou abra um existente (incluindo backups do 2fast desktop).
2. Desbloqueie com a senha do arquivo.
3. Adicione contas manualmente, colando uma URI `otpauth://` ou lendo um QR code.
4. Toque em um código para copiá-lo. Use o menu para ocultar códigos ou bloquear o app.

O arquivo pode ser guardado onde você quiser (armazenamento interno, pasta compartilhada, etc.) e aberto de novo depois — útil para backup e para usar o mesmo cofre no desktop.

## Compilar

### Pré-requisitos

- JDK 17
- Android SDK com platform `android-37` (ou superior conforme o `compileSdk` do projeto)
- Android Studio (recomendado) ou linha de comando com Gradle Wrapper

### Debug

```bash
./gradlew assembleDebug
```

O APK fica em `app/build/outputs/apk/debug/`.

### Release

Defina as variáveis de ambiente do keystore e rode:

```bash
export KEYSTORE_FILE=/caminho/para/keystore.jks
export KEYSTORE_PASSWORD=...
export KEY_ALIAS=...
export KEY_PASSWORD=...

./gradlew assembleRelease
```

## Estrutura

| Pacote | Função |
|--------|--------|
| `crypto` | Derivação de chave (PBKDF2) e criptografia AES compatível com o 2fast |
| `serialization` | Leitura/gravação do JSON `.2fa` com campos criptografados |
| `otp` | Geração TOTP (e Steam OTP) |
| `data` | Sessão, preferências e cofre de senhas (Android Keystore) |
| `ui` | Telas do aplicativo |

## Segurança

- Dados das contas ficam no arquivo `.2fa`, criptografados com chave derivada da senha.
- Senhas lembradas no aparelho são protegidas com AES-GCM via Android Keystore.
- Backup automático do app está desabilitado (`allowBackup="false"`).
- A segurança do cofre depende da força da senha do arquivo e de quem tem acesso ao dispositivo.

## CI

Workflows em `.github/workflows/` montam o APK debug (e release quando configurado) no push/PR para `main`.

## Licença

Distribuído sob a [GNU General Public License v3.0](LICENSE).

## Créditos

Formato `.2fa` e ideia original do projeto [2fast-team/2fast](https://github.com/2fast-team/2fast).
