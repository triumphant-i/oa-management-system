import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Home from '../views/Home.vue'
import ApprovalCenter from '../views/ApprovalCenter.vue'
import Attendance from '../views/Attendance.vue'
import AttendanceHistory from '../views/AttendanceHistory.vue'
import Notice from '../views/Notice.vue'
import Meeting from '../views/Meeting.vue'
import MeetingDetail from '../views/MeetingDetail.vue'
import NoticeDetail from '../views/NoticeDetail.vue'
import MeetingReserve from '../views/MeetingReserve.vue'
import MeetingControl from '../views/MeetingControl.vue'
import EmployeeManage from '../views/EmployeeManage.vue'
import Department from '../views/Department.vue'
import Apply from '../views/Apply.vue'
import Profile from '../views/Profile.vue'
import ApprovalDetail from '../views/ApprovalDetail.vue'
import ApplyDetail from '../views/ApplyDetail.vue'
import MyApplications from '../views/MyApplications.vue'

import Custom from '../views/Custom.vue'
import Message from '../views/Message.vue'

const routes = [
  { path: '/login', name: 'Login', component: Login },
  { path: '/', name: 'Home', component: Home },
  { path: '/approval', name: 'Approval', component: ApprovalCenter },
  { path: '/attendance', name: 'Attendance', component: Attendance },
  { path: '/attendance/history', name: 'AttendanceHistory', component: AttendanceHistory },
  { path: '/notice', name: 'Notice', component: Notice },
  { path: '/notice/detail/:id', name: 'NoticeDetail', component: NoticeDetail },
  { path: '/employee', name: 'Employee', component: EmployeeManage },
  { path: '/employee/manage', name: 'EmployeeManage', component: EmployeeManage },
  { path: '/department', name: 'Department', component: Department },
  { path: '/apply', name: 'Apply', component: Apply },
  { path: '/profile', name: 'Profile', component: Profile },
  { path: '/approval/detail/:id', name: 'ApprovalDetail', component: ApprovalDetail },
  { path: '/apply/detail/:id', name: 'ApplyDetail', component: ApplyDetail },
  { path: '/my-applications', name: 'MyApplications', component: MyApplications },
  { path: '/meeting', name: 'Meeting', component: Meeting },
  { path: '/meeting/detail/:id', name: 'MeetingDetail', component: MeetingDetail },
  { path: '/meeting/reserve/:id?', name: 'MeetingReserve', component: MeetingReserve },
  { path: '/meeting/control/:id', name: 'MeetingControl', component: MeetingControl },
  { path: '/custom', name: 'Custom', component: Custom },
  
  { path: '/message', name: 'Message', component: Message }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from) => {
  const isLogin = localStorage.getItem('isLogin')
  if (to.path !== '/login' && !isLogin) {
    return '/login'
  }
  if (to.path === '/login' && isLogin) {
    return '/'
  }
})

export default router