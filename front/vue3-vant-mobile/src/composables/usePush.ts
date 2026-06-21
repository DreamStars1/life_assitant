import { subscribePush, unsubscribePush } from '@/api/modules/push'

export function usePush() {
  const isSupported = 'serviceWorker' in navigator && 'PushManager' in window
  const subscription = ref<PushSubscription | null>(null)
  const publicKey = import.meta.env.VITE_VAPID_PUBLIC_KEY || ''

  async function register() {
    if (!isSupported)
      return
    const registration = await navigator.serviceWorker.ready
    const sub = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(publicKey) as any,
    })
    subscription.value = sub
    await subscribePush({
      endpoint: sub.endpoint,
      p256dh: btoa(String.fromCharCode(...new Uint8Array(sub.getKey('p256dh')!))),
      auth: btoa(String.fromCharCode(...new Uint8Array(sub.getKey('auth')!))),
    })
  }

  async function unregister() {
    if (subscription.value) {
      await unsubscribePush({ endpoint: subscription.value.endpoint })
      await subscription.value.unsubscribe()
      subscription.value = null
    }
  }

  return { isSupported, subscription, register, unregister }
}

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - base64String.length % 4) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const rawData = window.atob(base64)
  return Uint8Array.from([...rawData].map(c => c.charCodeAt(0)))
}
