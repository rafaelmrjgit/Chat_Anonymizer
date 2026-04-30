# Chat Anonymizer 🛡️

[English (US)](#english) | [Português (BR)](#português)

---
## English

### 📝 Description

Chat Anonymizer is an Android application designed to protect user privacy when sharing WhatsApp conversations for analysis. It allows users to import .txt chat files and replaces all sensitive information (names, phone numbers, emails, and timestamps) with generic identifiers. The resulting text is perfectly prepared for safe analysis by external Artificial Intelligence (AI) tools or research purposes. This app does not access WhatsApp directly or read chats automatically. Currently, it does not process media (images, audio, video).

### ✨ Key Features

- Deep anonymization: replaces participant names and numbers with consistent labels (e.g., "Person 1", "Person 2").
- Identifier hierarchy: follows a strict priority (Phone number > LID > Username) to ensure unique mapping without using display names.
- Privacy first: 100% local processing.
- System message removal: filters out WhatsApp system notifications (e.g., "messages are encrypted", "user joined").
- Customization: toggleable options for masking URLs and removing timestamps.
- Export: saves files with UTF-8 BOM encoding for character display in any text editor or browser.

### 🚀 How to Use

1. Export: in WhatsApp, export your chat (without media) as a .txt file.
2. Import: open Chat Anonymizer and tap "Import Chat".
3. Process: the app automatically detects participants and normalizes the content.
4. Save/Share: export the clean text to your device or share it directly with your preferred AI app.

### ⚠️ Responsibilities
Users are responsible for reviewing the anonymized text and choosing an external AI service that complies with their own privacy requirements.

---
## Português

### 📝 Descrição

O Chat Anonymizer é um aplicativo Android desenvolvido para proteger a privacidade do usuário ao compartilhar conversas do WhatsApp para análise. Ele permite importar arquivos .txt de conversas exportadas e substitui todas as informações sensíveis (nomes, números de telefone, e-mails e registros de data/hora) por identificadores genéricos. O texto resultante é preparado para uma análise segura por ferramentas de Inteligência Artificial (IA) externas ou para fins de pesquisa. O app não acessa o WhatsApp diretamente nem lê conversas automaticamente. Atualmente, ele não processa mídias (imagens, áudio, vídeo).

### ✨ Funcionalidades Principais
- Anonimização profunda: substitui nomes e números de participantes por rótulos consistentes (ex: "Pessoa 1", "Pessoa 2").
- Hierarquia de identificadores: Segue uma prioridade rigorosa (número de telefone > LID > Username) para garantir o mapeamento único sem usar nomes de exibição.
- Privacidade total: processamento 100% local.
- Remoção de mensagens do sistema: filtra automaticamente notificações do WhatsApp (ex: "mensagens são criptografadas", "fulano entrou no grupo").
- Customização: opções para mascarar URLs e remover registros de data/hora (timestamps).
- Exportação: salva arquivos com codificação UTF-8 BOM para exibição de caracteres em qualquer editor de texto ou navegador.

### 🚀 Como Usar
1. Exportar: no WhatsApp, exporte sua conversa (sem mídia) como um arquivo .txt.
2. Importar: abra o Chat Anonymizer e toque em "Importar Conversa".
3. Processar: o app detecta automaticamente os participantes e normaliza o conteúdo.
4. Salvar/Compartilhar: exporte o texto limpo para seu dispositivo ou compartilhe diretamente com seu app de IA preferido.

### ⚠️ Responsabilidades
O usuário é responsável por revisar o texto anonimizado e escolher um serviço de IA externo que esteja de acordo com seus próprios requisitos de privacidade.
