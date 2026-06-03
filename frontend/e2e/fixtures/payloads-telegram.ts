/**
 * payloads-telegram.ts — factories de Updates sintéticos do Telegram Bot API.
 *
 * Produz objetos compatíveis com https://core.telegram.org/bots/api#update
 * para uso nos testes E2E de webhook.
 *
 * MVP — 2 factories exportadas:
 *   - telegramUpdateTextoPuro  (message.text)
 *   - telegramUpdateSticker    (message.sticker)
 *
 * TODO Fase 1.1: adicionar telegramUpdateFotoLegenda após decisão de mock (ADR 00XX)
 *   Bloqueado por: decisão sobre como o back trata download de mídia em E2E
 *   (§3 da spec qa-suite-e2e-fase1.md — opções: WireMock, flag skip-media, stub @Profile)
 */

// ── Contador de update_id (único por execução) ────────────────────────────────

let _updateCounter = Date.now();

function nextUpdateId(): number {
  return _updateCounter++;
}

function nextMessageId(): number {
  return Math.floor(Math.random() * 900_000) + 100_000;
}

function nowUnix(): number {
  return Math.floor(Date.now() / 1000);
}

// ── Tipos Telegram (mínimo necessário) ───────────────────────────────────────

interface TelegramUser {
  id: number;
  is_bot: boolean;
  first_name: string;
}

interface TelegramChat {
  id: number;
  type: 'private' | 'group' | 'supergroup' | 'channel';
}

interface TelegramMessage {
  message_id: number;
  from: TelegramUser;
  chat: TelegramChat;
  date: number;
  text?: string;
  caption?: string;
  sticker?: TelegramSticker;
  photo?: TelegramPhotoSize[];
}

interface TelegramSticker {
  file_id: string;
  file_unique_id: string;
  type: 'regular' | 'mask' | 'custom_emoji';
  width: number;
  height: number;
  is_animated: boolean;
  is_video: boolean;
}

interface TelegramPhotoSize {
  file_id: string;
  file_unique_id: string;
  width: number;
  height: number;
  file_size?: number;
}

interface TelegramUpdate {
  update_id: number;
  message: TelegramMessage;
}

// ── Helpers internos ──────────────────────────────────────────────────────────

function baseMessage(fromUserId: number): TelegramMessage {
  return {
    message_id: nextMessageId(),
    from: { id: fromUserId, is_bot: false, first_name: 'E2E Test' },
    chat: { id: fromUserId, type: 'private' },
    date: nowUnix(),
  };
}

// ── Factories exportadas ──────────────────────────────────────────────────────

export interface TextoPuroParams {
  fromUserId: number;
  text: string;
}

/**
 * Update com mensagem de texto puro (sem foto, sem legenda).
 * Usado para testar o handler genérico — não deve criar pedido.
 */
export function telegramUpdateTextoPuro({
  fromUserId,
  text,
}: TextoPuroParams): TelegramUpdate {
  return {
    update_id: nextUpdateId(),
    message: {
      ...baseMessage(fromUserId),
      text,
    },
  };
}

export interface StickerParams {
  fromUserId: number;
}

/**
 * Update com sticker.
 * Usado para testar handler genérico (BE-15) — não deve criar pedido.
 */
export function telegramUpdateSticker({ fromUserId }: StickerParams): TelegramUpdate {
  return {
    update_id: nextUpdateId(),
    message: {
      ...baseMessage(fromUserId),
      sticker: {
        file_id: 'CAACAgIAAxkBAAE_e2e_sticker_fake',
        file_unique_id: 'e2e_sticker_unique',
        type: 'regular',
        width: 512,
        height: 512,
        is_animated: false,
        is_video: false,
      },
    },
  };
}

// TODO Fase 1.1: adicionar telegramUpdateFotoLegenda após decisão de mock (ADR 00XX)
// export function telegramUpdateFotoLegenda(...): TelegramUpdate { ... }
