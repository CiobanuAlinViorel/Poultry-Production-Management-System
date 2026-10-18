import Dexie, { type EntityTable } from 'dexie';

export interface QueueItem {
  id?: number;
  method: string;
  url: string;
  body?: unknown;
  idempotencyKey: string;
  createdAt: string;
}

export interface RefCacheItem { key: string; value: unknown; updatedAt: string }
export interface Draft { id: string; module: string; value: unknown; updatedAt: string }

export const db = new Dexie('ppms') as Dexie & {
  queue: EntityTable<QueueItem, 'id'>;
  refCache: EntityTable<RefCacheItem, 'key'>;
  drafts: EntityTable<Draft, 'id'>;
};

db.version(1).stores({
  queue: '++id, createdAt',
  refCache: 'key, updatedAt',
  drafts: 'id, module, updatedAt',
});
