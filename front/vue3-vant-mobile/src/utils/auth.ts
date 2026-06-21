const TOKEN_KEY = 'access_token'

function isLogin() {
  return !!getToken()
}

function getToken() {
  const t = localStorage.getItem(TOKEN_KEY)
  return t && t !== 'null' && t !== 'undefined' ? t : null
}

function setToken(newToken: string) {
  localStorage.setItem(TOKEN_KEY, newToken)
}

function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export { isLogin, getToken, setToken, clearToken }
