// forced light theme — warm color scheme
export const isDark = ref(false)

export function toggleDark() {
  isDark.value = !isDark.value
}

export const preferredDark = ref(false)
