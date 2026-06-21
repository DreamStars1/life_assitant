/** IndexedDB 离线存储 — 待办/日程/生活记录缓存 */

const DB_NAME = 'life-assistant'
const DB_VERSION = 1
const STORES = ['todos', 'events', 'lifelogs'] as const

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      STORES.forEach(s => {
        if (!db.objectStoreNames.contains(s)) {
          db.createObjectStore(s, { keyPath: 'id' })
        }
      })
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

export async function cachePut(store: string, data: any) {
  const db = await openDB()
  const tx = db.transaction(store, 'readwrite')
  tx.objectStore(store).put(data)
  await new Promise(r => { tx.oncomplete = r })
  db.close()
}

export async function cacheGetAll(store: string): Promise<any[]> {
  const db = await openDB()
  const tx = db.transaction(store, 'readonly')
  const result = await new Promise<any[]>((resolve, reject) => {
    const req = tx.objectStore(store).getAll()
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
  db.close()
  return result
}

export async function cacheDelete(store: string, id: string) {
  const db = await openDB()
  const tx = db.transaction(store, 'readwrite')
  tx.objectStore(store).delete(id)
  await new Promise(r => { tx.oncomplete = r })
  db.close()
}
