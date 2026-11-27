import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '仪表盘', icon: 'Odometer' } },
      { path: 'players', name: 'players', component: () => import('../views/Players.vue'), meta: { title: '玩家管理', icon: 'User' } },
      { path: 'console', name: 'console', component: () => import('../views/Console.vue'), meta: { title: '控制台', icon: 'Monitor' } },
      { path: 'plugins', name: 'plugins', component: () => import('../views/Plugins.vue'), meta: { title: '插件管理', icon: 'Box' } },
      { path: 'worlds', name: 'worlds', component: () => import('../views/Worlds.vue'), meta: { title: '世界管理', icon: 'Earth' } },
      { path: 'economy', name: 'economy', component: () => import('../views/Economy.vue'), meta: { title: '经济系统', icon: 'Money' } },
      { path: 'groups', name: 'groups', component: () => import('../views/Groups.vue'), meta: { title: '权限组', icon: 'UserFilled' } },
      { path: 'market', name: 'market', component: () => import('../views/Market.vue'), meta: { title: '玩家市场', icon: 'Shop' } },
      { path: 'claims', name: 'claims', component: () => import('../views/Claims.vue'), meta: { title: '领地管理', icon: 'MapLocation' } },
      { path: 'redeem', name: 'redeem', component: () => import('../views/Redeem.vue'), meta: { title: '兑换码', icon: 'Ticket' } },
      { path: 'recharge', name: 'recharge', component: () => import('../views/Recharge.vue'), meta: { title: '充值管理', icon: 'Wallet' } },
      { path: 'nicknames', name: 'nicknames', component: () => import('../views/Nicknames.vue'), meta: { title: '中文昵称', icon: 'EditPen' } },
      { path: 'menus', name: 'menus', component: () => import('../views/Menus.vue'), meta: { title: '自定义菜单', icon: 'Menu' } },
      { path: 'npcs', name: 'npcs', component: () => import('../views/Npcs.vue'), meta: { title: 'NPC 管理', icon: 'Avatar' } },
      { path: 'signs', name: 'signs', component: () => import('../views/Signs.vue'), meta: { title: '命令告示牌', icon: 'Postcard' } },
      { path: 'announcements', name: 'announcements', component: () => import('../views/Announcements.vue'), meta: { title: '公告与规则', icon: 'Notebook' } },
      { path: 'accounts', name: 'accounts', component: () => import('../views/Accounts.vue'), meta: { title: '登录账号', icon: 'UserFilled' } },
      { path: 'bans', name: 'bans', component: () => import('../views/Bans.vue'), meta: { title: '封禁管理', icon: 'Lock' } },
      { path: 'ipbans', name: 'ipbans', component: () => import('../views/IpBans.vue'), meta: { title: 'IP 封禁', icon: 'Key' } },
      { path: 'whitelist', name: 'whitelist', component: () => import('../views/Whitelist.vue'), meta: { title: '白名单', icon: 'CircleCheck' } },
      { path: 'chat', name: 'chat', component: () => import('../views/ChatLog.vue'), meta: { title: '聊天记录', icon: 'ChatDotRound' } },
      { path: 'warps', name: 'warps', component: () => import('../views/Warps.vue'), meta: { title: 'Warp 管理', icon: 'Position' } },
      { path: 'tasks', name: 'tasks', component: () => import('../views/Tasks.vue'), meta: { title: '定时任务', icon: 'Clock' } },
      { path: 'server', name: 'server', component: () => import('../views/Server.vue'), meta: { title: '服务器控制', icon: 'Setting' } }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('plu_token')
  if (to.name !== 'login' && !token) next({ name: 'login' })
  else if (to.name === 'login' && token) next({ name: 'dashboard' })
  else next()
})

export default router
