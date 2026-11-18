import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '',
  timeout: 15000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('plu_token')
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

api.interceptors.response.use(
  res => res.data,
  err => {
    const status = err.response?.status
    const atLogin = location.hash.startsWith('#/login')

    if (status === 401) {
      if (atLogin) {
        // 登录页的 401 是密码错误，交由页面自行提示，避免误报"连接失败"
        return Promise.reject(err)
      }
      localStorage.removeItem('plu_token')
      location.hash = '#/login'
      ElMessage.error('登录已失效，请重新登录')
    } else if (!err.response) {
      ElMessage.error('无法连接到服务器')
    } else {
      ElMessage.error(err.response.data?.error || '请求失败')
    }
    return Promise.reject(err)
  }
)

export default api
