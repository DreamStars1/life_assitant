import { ref, computed } from 'vue'

export type CalendarView = 'month' | 'week' | 'day'

export function useCalendar() {
  const currentDate = ref(new Date())
  const view = ref<CalendarView>('month')

  const year = computed(() => currentDate.value.getFullYear())
  const month = computed(() => currentDate.value.getMonth() + 1)
  const dateStr = computed(() => currentDate.value.toISOString().split('T')[0])

  function prev() {
    if (view.value === 'month') {
      currentDate.value = new Date(year.value, month.value - 2, 1)
    } else if (view.value === 'week') {
      currentDate.value = new Date(currentDate.value.getTime() - 7 * 86400000)
    } else {
      currentDate.value = new Date(currentDate.value.getTime() - 86400000)
    }
  }

  function next() {
    if (view.value === 'month') {
      currentDate.value = new Date(year.value, month.value, 1)
    } else if (view.value === 'week') {
      currentDate.value = new Date(currentDate.value.getTime() + 7 * 86400000)
    } else {
      currentDate.value = new Date(currentDate.value.getTime() + 86400000)
    }
  }

  return { currentDate, view, year, month, dateStr, prev, next }
}
